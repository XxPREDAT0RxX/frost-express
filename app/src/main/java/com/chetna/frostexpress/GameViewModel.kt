package com.chetna.frostexpress

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
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
    var soundOn by mutableStateOf(prefs.getBoolean("sound", true)); private set

    fun toggleSound() {
        soundOn = !soundOn
        audio.enabled = soundOn
        prefs.edit().putBoolean("sound", soundOn).apply()
        if (soundOn) { audio.startMusic(); audio.play(R.raw.sfx_click) } else audio.pauseMusic()
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
            (1.0 + Balance.PERM_SPEED * permLevel(PermType.SPEED))

    val coinsPerMeter: Double
        get() = (1.0 + Balance.WAGON_BONUS * level(UpgradeType.WAGON)) * milestoneMult(UpgradeType.WAGON) *
            (1.0 + Balance.HEAT_BONUS * level(UpgradeType.HEAT)) * milestoneMult(UpgradeType.HEAT) *
            (1.0 + Balance.FOOD_BONUS * level(UpgradeType.FOOD)) * milestoneMult(UpgradeType.FOOD) *
            Balance.TIER_INCOME.pow(tierIndex) *
            (1.0 + Balance.CORE_BONUS * cores) *
            (1.0 + Balance.PERM_INCOME * permLevel(PermType.INCOME))

    val boostMultiplier: Double
        get() = Balance.BOOST_BASE_MULT + Balance.BOOST_MULT_PER_LEVEL * level(UpgradeType.BOOSTER) +
            Balance.PERM_BOOST * permLevel(PermType.BOOST)

    val isBoosting: Boolean get() = boostTimeLeft > 0.0
    val incomeMultiplier: Double
        get() = (if (isBoosting) boostMultiplier else 1.0) * (if (adBoostLeft > 0.0) Balance.AD_BOOST_MULT else 1.0)
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
        load()
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
            audio.play(R.raw.sfx_buy)
        }
    }

    /** Each Boost tap costs 10% of the meter and grants 3 seconds of income multiplier. */
    fun boost() {
        if (boostLocked || boostMeter < Balance.BOOST_COST_PER_TAP - 0.005) return
        audio.play(R.raw.sfx_boost, 0.7f)
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

        val last = prefs.getLong("last", 0L)
        if (last > 0L) {
            val secs = ((System.currentTimeMillis() - last) / 1000).coerceIn(0L, Balance.OFFLINE_CAP_SEC)
            val meters = speed * secs * offlineRate
            val gain = meters * coinsPerMeter
            if (gain >= 1.0) {
                runDistance += meters
                coins += gain
                offlineCoins = gain
                offlineMeters = meters
            }
        }
    }

    private fun tick(dt: Double) {
        val meters = speed * dt
        runDistance += meters
        coins += meters * coinsPerMeter * incomeMultiplier
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
