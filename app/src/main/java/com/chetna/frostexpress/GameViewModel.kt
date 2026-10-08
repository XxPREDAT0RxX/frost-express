package com.chetna.frostexpress

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("frost_express", Context.MODE_PRIVATE)

    var coins by mutableDoubleStateOf(0.0); private set
    var runDistance by mutableDoubleStateOf(0.0); private set
    var cores by mutableIntStateOf(0); private set
    var evolutions by mutableIntStateOf(0); private set
    var boostMeter by mutableDoubleStateOf(1.0); private set
    var boostTimeLeft by mutableDoubleStateOf(0.0); private set
    var boostLocked by mutableStateOf(false); private set
    var adBoostLeft by mutableDoubleStateOf(0.0); private set
    var ads: AdProvider = NoAds
    var offlineCoins by mutableDoubleStateOf(0.0); private set
    var offlineMeters by mutableDoubleStateOf(0.0); private set
    val levels = mutableStateListOf<Int>()

    // Permanent upgrades (kept across evolves) and the Frost Cores spent on them.
    val permLevels = mutableStateListOf<Int>()
    var coresSpent by mutableIntStateOf(0); private set
    val coresAvailable: Int get() = cores - coresSpent
    fun permLevel(t: PermType): Int = permLevels[t.ordinal]
    fun permCost(spec: PermSpec): Int =
        kotlin.math.ceil(spec.baseCost * spec.growth.pow(permLevel(spec.type))).toInt()
    fun buyPerm(spec: PermSpec) {
        if (permLevel(spec.type) >= spec.maxLevel) return
        val cost = permCost(spec)
        if (coresAvailable >= cost) {
            coresSpent += cost
            permLevels[spec.type.ordinal] = permLevel(spec.type) + 1
            audio.play(R.raw.sfx_buy)
        }
    }
    private val startCoins: Double
        get() = if (permLevel(PermType.START) == 0) 0.0
        else Balance.START_COINS_BASE * Balance.START_COINS_GROWTH.pow(permLevel(PermType.START) - 1)

    val audio = GameAudio(app)

    // ---- progress stats, daily reward, tutorial, achievements ----
    var totalDistance by mutableDoubleStateOf(0.0); private set
    var boostTaps by mutableIntStateOf(0); private set
    var purchases by mutableIntStateOf(0); private set
    var tutorialDone by mutableStateOf(prefs.getBoolean("tut", false)); private set
    var dailyStreak by mutableIntStateOf(prefs.getInt("streak", 0)); private set
    private var lastDailyDay = prefs.getLong("dailyDay", -1L)
    var dailyReady by mutableStateOf(false); private set
    val unlocked = mutableStateListOf<String>()
    var achievementPopup by mutableStateOf<String?>(null); private set
    private var popupLeft = 0.0
    var musicVolume by mutableFloatStateOf(prefs.getFloat("mvol", 0.35f)); private set
    var sfxVolume by mutableFloatStateOf(prefs.getFloat("svol", 1f)); private set

    // ---- random events, stations, toast messages ----
    var event by mutableStateOf(GameEvent.NONE); private set
    var eventLeft by mutableDoubleStateOf(0.0); private set
    private var nextEventIn = 60.0
    var toast by mutableStateOf<String?>(null); private set
    private var toastLeft = 0.0
    var stationSerial by mutableIntStateOf(0); private set
    private var nextStationAt = Balance.STATION_EVERY_M

    private fun showToast(text: String) {
        toast = text
        toastLeft = 3.5
    }

    private val eventSpeedMult: Double get() = if (event == GameEvent.BLIZZARD) Balance.BLIZZARD_SPEED else 1.0
    private val eventIncomeMult: Double get() = if (event == GameEvent.AURORA) Balance.AURORA_INCOME else 1.0

    private fun startRandomEvent() {
        event = when ((0..2).random()) {
            0 -> GameEvent.BLIZZARD
            1 -> GameEvent.AURORA
            else -> GameEvent.CRATE
        }
        eventLeft = when (event) {
            GameEvent.BLIZZARD -> Balance.BLIZZARD_SEC
            GameEvent.AURORA -> Balance.AURORA_SEC
            else -> Balance.CRATE_SEC
        }
        showToast(
            when (event) {
                GameEvent.BLIZZARD -> "Blizzard! Speed -25% for ${Balance.BLIZZARD_SEC.toInt()}s"
                GameEvent.AURORA -> "Aurora! Income x${fmt(Balance.AURORA_INCOME, 1)} for ${Balance.AURORA_SEC.toInt()}s"
                else -> "Supply crate spotted! Tap it"
            }
        )
    }

    fun collectCrate() {
        if (event != GameEvent.CRATE) return
        val gain = max(50.0, coinsPerSec * 60.0)
        coins += gain
        event = GameEvent.NONE
        eventLeft = 0.0
        audio.play(R.raw.sfx_buy)
        showToast("Supply crate: +${fmt(gain)} coins")
    }

    /** Called by the scene when a station reaches the train. */
    fun collectStation() {
        val gain = max(30.0, coinsPerSec * Balance.STATION_BONUS_SEC)
        coins += gain
        audio.play(R.raw.sfx_buy)
        showToast("Station: passengers paid +${fmt(gain)} coins")
    }

    private fun tickEvents(dt: Double) {
        if (toastLeft > 0.0) {
            toastLeft -= dt
            if (toastLeft <= 0.0) toast = null
        }
        if (event != GameEvent.NONE) {
            eventLeft -= dt
            if (eventLeft <= 0.0) event = GameEvent.NONE
        } else {
            nextEventIn -= dt
            if (nextEventIn <= 0.0) {
                nextEventIn = Balance.EVENT_MIN_GAP + Math.random() * (Balance.EVENT_MAX_GAP - Balance.EVENT_MIN_GAP)
                startRandomEvent()
            }
        }
        if (totalDistance >= nextStationAt) {
            nextStationAt = (floor(totalDistance / Balance.STATION_EVERY_M) + 1) * Balance.STATION_EVERY_M
            stationSerial++
        }
    }

    private fun today(): Long = java.time.LocalDate.now().toEpochDay()
    val dailyNextStreak: Int get() = if (lastDailyDay >= 0 && lastDailyDay == today() - 1) dailyStreak + 1 else 1
    fun dailyCoins(streak: Int): Double =
        max(200.0, coinsPerSec * 900.0) * (1.0 + 0.25 * (min(streak, 7) - 1))
    fun dailyGivesCore(streak: Int): Boolean = streak % 7 == 0

    fun claimDaily() {
        if (!dailyReady) return
        val streak = dailyNextStreak
        coins += dailyCoins(streak)
        if (dailyGivesCore(streak)) cores += 1
        dailyStreak = streak
        lastDailyDay = today()
        dailyReady = false
        audio.play(R.raw.sfx_buy)
        save()
    }

    fun finishTutorial() {
        tutorialDone = true
        prefs.edit().putBoolean("tut", true).apply()
    }

    fun changeMusicVolume(v: Float) {
        musicVolume = v
        audio.musicVolume = v
        prefs.edit().putFloat("mvol", v).apply()
    }

    fun changeSfxVolume(v: Float) {
        sfxVolume = v
        audio.sfxVolume = v
        prefs.edit().putFloat("svol", v).apply()
    }

    private fun achievementMet(id: String): Boolean = when (id) {
        "steps" -> totalDistance >= 100.0
        "km1" -> totalDistance >= 1000.0
        "km100" -> totalDistance >= 100_000.0
        "upg1" -> purchases >= 1
        "upg50" -> purchases >= 50
        "boost10" -> boostTaps >= 10
        "boost200" -> boostTaps >= 200
        "wagon5" -> level(UpgradeType.WAGON) >= 5
        "heat10" -> level(UpgradeType.HEAT) >= 10
        "rich" -> coins >= 1_000_000.0
        "streak3" -> dailyStreak >= 3
        "evo1" -> evolutions >= 1
        "evo3" -> evolutions >= 3
        else -> false
    }

    private fun checkAchievements() {
        for (a in ACHIEVEMENTS) {
            if (a.id in unlocked || !achievementMet(a.id)) continue
            unlocked.add(a.id)
            if (a.cores > 0) cores += a.cores else coins += max(100.0, coinsPerSec * 600.0)
            achievementPopup = a.title
            popupLeft = 3.5
            audio.play(R.raw.sfx_buy)
        }
    }

    /** Wipes all progress (settings stay). */
    fun resetAll() {
        prefs.edit().clear().putBoolean("sound", soundOn).putFloat("mvol", musicVolume).putFloat("svol", sfxVolume).apply()
        coins = 0.0; runDistance = 0.0; cores = 0; evolutions = 0; coresSpent = 0
        for (i in levels.indices) levels[i] = 0
        for (i in permLevels.indices) permLevels[i] = 0
        totalDistance = 0.0; boostTaps = 0; purchases = 0
        dailyStreak = 0; lastDailyDay = -1L; dailyReady = true
        unlocked.clear(); achievementPopup = null
        event = GameEvent.NONE; eventLeft = 0.0; nextStationAt = Balance.STATION_EVERY_M
        boostMeter = 1.0; boostTimeLeft = 0.0; boostLocked = false
        offlineCoins = 0.0; offlineMeters = 0.0
        tutorialDone = false
        save()
    }
    var soundOn by mutableStateOf(prefs.getBoolean("sound", true)); private set

    fun toggleSound() {
        soundOn = !soundOn
        audio.enabled = soundOn
        prefs.edit().putBoolean("sound", soundOn).apply()
        if (soundOn) audio.startMusic() else audio.pauseMusic()
    }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }

    /** Buy amount selector: 1, 10, 100, or 0 = MAX. */
    var buyAmount by mutableIntStateOf(1)

    /** Milestones: each one doubles that upgrade's effect (only for speed/income upgrades). */
    private val milestoneTypes = setOf(UpgradeType.ENGINE, UpgradeType.WAGON, UpgradeType.HEAT, UpgradeType.FOOD)
    fun milestoneMult(t: UpgradeType): Double {
        if (t !in milestoneTypes) return 1.0
        return 2.0.pow(Balance.MILESTONES.count { level(t) >= it })
    }
    fun nextMilestone(t: UpgradeType): Int? =
        if (t in milestoneTypes) Balance.MILESTONES.firstOrNull { level(t) < it } else null

    // ---- derived values ----
    fun level(t: UpgradeType): Int = levels[t.ordinal]

    val tierIndex: Int get() = min(evolutions, TIERS.lastIndex)

    val speed: Double
        get() = (Balance.BASE_SPEED + Balance.SPEED_PER_ENGINE * level(UpgradeType.ENGINE)) *
            milestoneMult(UpgradeType.ENGINE) *
            (1.0 + Balance.PLOW_BONUS * level(UpgradeType.PLOW)) *
            (1.0 + Balance.TIER_SPEED_BONUS * tierIndex) *
            (1.0 + Balance.PERM_SPEED * permLevel(PermType.SPEED)) * eventSpeedMult

    val coinsPerMeter: Double
        get() = (1.0 + Balance.WAGON_BONUS * level(UpgradeType.WAGON)) * milestoneMult(UpgradeType.WAGON) *
            (1.0 + Balance.HEAT_BONUS * level(UpgradeType.HEAT)) * milestoneMult(UpgradeType.HEAT) *
            (1.0 + Balance.FOOD_BONUS * level(UpgradeType.FOOD)) * milestoneMult(UpgradeType.FOOD) *
            Balance.TIER_INCOME.pow(tierIndex) *
            (1.0 + Balance.CORE_BONUS * cores) *
            (1.0 + Balance.PERM_INCOME * permLevel(PermType.INCOME)) *
            (1.0 + Balance.PERM_CREW * permLevel(PermType.CREW))

    val boostMultiplier: Double
        get() = Balance.BOOST_BASE_MULT + Balance.BOOST_MULT_PER_LEVEL * level(UpgradeType.BOOSTER) +
            Balance.PERM_BOOST * permLevel(PermType.BOOST)

    val isBoosting: Boolean get() = boostTimeLeft > 0.0
    val incomeMultiplier: Double
        get() = (if (isBoosting) boostMultiplier else 1.0) * (if (adBoostLeft > 0.0) Balance.AD_BOOST_MULT else 1.0) * eventIncomeMult
    val coinsPerSec: Double get() = speed * coinsPerMeter * incomeMultiplier
    private val rechargePerSec: Double
        get() = Balance.BOOST_RECHARGE_PER_SEC * (1.0 + Balance.COAL_BONUS * level(UpgradeType.COAL))
    private val offlineRate: Double
        get() = min(1.0, Balance.OFFLINE_BASE + Balance.CONDUCTOR_BONUS * level(UpgradeType.CONDUCTOR) +
            Balance.PERM_OFFLINE * permLevel(PermType.OFFLINE))

    val evolveDistance: Double get() = Balance.EVOLVE_BASE_DISTANCE * Balance.EVOLVE_GROWTH.pow(evolutions)
    val canEvolve: Boolean get() = runDistance >= evolveDistance
    val coreGain: Int get() = max(1, floor(sqrt(runDistance / 1000.0)).toInt())

    fun upgradeCost(spec: UpgradeSpec): Double =
        floor(spec.baseCost * spec.growth.pow(level(spec.type)))

    /** Cost of buying n levels at once (geometric sum). */
    fun bulkCost(spec: UpgradeSpec, n: Int): Double =
        floor(spec.baseCost * spec.growth.pow(level(spec.type)) * (spec.growth.pow(n) - 1.0) / (spec.growth - 1.0))

    /** How many levels a buy press would purchase right now (at least 1 so the price is always shown). */
    fun buyCount(spec: UpgradeSpec): Int {
        val room = spec.maxLevel - level(spec.type)
        if (room <= 0) return 0
        val want = if (buyAmount == 0) Int.MAX_VALUE else buyAmount
        var n = min(want, room)
        if (buyAmount == 0) {
            n = 0
            while (n < room && n < 1000 && bulkCost(spec, n + 1) <= coins) n++
            return max(1, n)
        }
        return n
    }

    init {
        audio.enabled = soundOn
        audio.musicVolume = musicVolume
        audio.sfxVolume = sfxVolume
        load()
        dailyReady = lastDailyDay != today()
        nextStationAt = (floor(totalDistance / Balance.STATION_EVERY_M) + 1) * Balance.STATION_EVERY_M
        startLoop()
    }

    // ---- actions ----
    fun buy(spec: UpgradeSpec) {
        val lvl = level(spec.type)
        if (lvl >= spec.maxLevel) return
        val n = buyCount(spec)
        val cost = bulkCost(spec, n)
        if (n > 0 && coins >= cost) {
            coins -= cost
            levels[spec.type.ordinal] = lvl + n
            purchases += n
            audio.play(R.raw.sfx_buy)
        }
    }

    /** Each Boost tap costs 10% of the meter and grants 3 seconds of income multiplier. */
    fun boost() {
        if (boostLocked || boostMeter < Balance.BOOST_COST_PER_TAP - 0.005) return
        audio.play(R.raw.sfx_boost, 0.7f)
        boostTaps++
        boostMeter = max(0.0, boostMeter - Balance.BOOST_COST_PER_TAP)
        boostTimeLeft = min(Balance.BOOST_MAX_SEC, boostTimeLeft + Balance.BOOST_SEC_PER_TAP)
        if (boostMeter < Balance.BOOST_COST_PER_TAP - 0.005) boostLocked = true // locked until fully recharged
    }

    fun evolve() {
        if (!canEvolve) return
        audio.play(R.raw.sfx_evolve)
        cores += coreGain
        evolutions++
        for (i in levels.indices) levels[i] = 0 // temporary upgrades reset; permanent ones stay
        coins = startCoins
        runDistance = 0.0
        boostMeter = 1.0
        boostTimeLeft = 0.0
        boostLocked = false
        save()
    }

    /** Watch a rewarded ad for a timed income multiplier. */
    fun watchAdBoost() {
        if (!Balance.ADS_ENABLED) return
        ads.showRewarded { adBoostLeft = Balance.AD_BOOST_SEC }
    }

    fun dismissOffline() {
        offlineCoins = 0.0
        offlineMeters = 0.0
    }

    fun save() {
        prefs.edit()
            .putString("coins", coins.toString())
            .putString("dist", runDistance.toString())
            .putInt("cores", cores)
            .putInt("evo", evolutions)
            .putString("levels", levels.joinToString(","))
            .putString("perm", permLevels.joinToString(","))
            .putInt("spent", coresSpent)
            .putString("totalDist", totalDistance.toString())
            .putInt("taps", boostTaps)
            .putInt("purch", purchases)
            .putInt("streak", dailyStreak)
            .putLong("dailyDay", lastDailyDay)
            .putString("ach", unlocked.joinToString(","))
            .putLong("last", System.currentTimeMillis())
            .apply()
    }

    // ---- internals ----
    private fun load() {
        coins = prefs.getString("coins", "0")?.toDoubleOrNull() ?: 0.0
        runDistance = prefs.getString("dist", "0")?.toDoubleOrNull() ?: 0.0
        cores = prefs.getInt("cores", 0)
        evolutions = prefs.getInt("evo", 0)
        val saved = prefs.getString("levels", "")?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        levels.clear()
        UpgradeType.values().indices.forEach { levels.add(saved.getOrElse(it) { 0 }) }
        val savedPerm = prefs.getString("perm", "")?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        PermType.values().indices.forEach { permLevels.add(savedPerm.getOrElse(it) { 0 }) }
        coresSpent = prefs.getInt("spent", 0)
        totalDistance = prefs.getString("totalDist", "0")?.toDoubleOrNull() ?: 0.0
        boostTaps = prefs.getInt("taps", 0)
        purchases = prefs.getInt("purch", 0)
        unlocked.clear()
        prefs.getString("ach", "")?.split(",")?.filter { it.isNotEmpty() }?.let { unlocked.addAll(it) }

        val last = prefs.getLong("last", 0L)
        if (last > 0L) {
            val secs = ((System.currentTimeMillis() - last) / 1000).coerceIn(0L, Balance.OFFLINE_CAP_SEC)
            val meters = speed * secs * offlineRate
            val gain = meters * coinsPerMeter
            if (gain >= 1.0) {
                runDistance += meters
                totalDistance += meters
                coins += gain
                offlineCoins = gain
                offlineMeters = meters
            }
        }
    }

    private fun tick(dt: Double) {
        val meters = speed * dt
        runDistance += meters
        totalDistance += meters
        coins += meters * coinsPerMeter * incomeMultiplier
        if (popupLeft > 0.0) {
            popupLeft -= dt
            if (popupLeft <= 0.0) achievementPopup = null
        }
        checkAchievements()
        tickEvents(dt)
        if (adBoostLeft > 0.0) adBoostLeft = max(0.0, adBoostLeft - dt)
        if (boostTimeLeft > 0.0) boostTimeLeft = max(0.0, boostTimeLeft - dt)
        if (boostMeter < 1.0) boostMeter = min(1.0, boostMeter + rechargePerSec * dt)
        if (boostLocked && boostMeter >= 1.0) boostLocked = false
    }

    private fun startLoop() {
        viewModelScope.launch {
            var lastTick = System.nanoTime()
            var sinceSave = 0.0
            while (true) {
                delay(100)
                val now = System.nanoTime()
                val dt = (now - lastTick) / 1e9
                lastTick = now
                tick(dt)
                sinceSave += dt
                if (sinceSave >= 5.0) {
                    save()
                    sinceSave = 0.0
                }
            }
        }
    }
}
