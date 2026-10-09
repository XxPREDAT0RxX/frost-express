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
    // Guided tutorial, first launch only: 0 welcome, 1 open upgrades, 2 buy Engine, 3 tap Boost, 4 done.
    var tutorialStage by mutableIntStateOf(if (prefs.getBoolean("tut", false)) 4 else prefs.getInt("tstage", 0)); private set
    val tutorialDone: Boolean get() = tutorialStage >= 4

    fun tutorialAdvance(from: Int) {
        if (tutorialStage != from) return
        tutorialStage = from + 1
        prefs.edit().putInt("tstage", tutorialStage).apply()
        if (tutorialStage >= 4) {
            prefs.edit().putBoolean("tut", true).apply()
            showToast("Tutorial done! New upgrades unlock as you level up.")
        }
    }

    // Upgrades unlock one after another (see UpgradeSpec.unlockType). Unlocks are kept after evolving.
    val unlockedUpgrades = mutableStateListOf<String>()
    fun isUnlocked(spec: UpgradeSpec): Boolean = spec.unlockType == null || spec.type.name in unlockedUpgrades

    private fun checkUnlocks() {
        for (spec in UPGRADES) {
            if (isUnlocked(spec)) continue
            val req = spec.unlockType ?: continue
            if (carOf(spec.type) >= maxCars && level(spec.type) == 0) continue // that car needs a later train
            if (level(spec.type) > 0) {
                unlockedUpgrades.add(spec.type.name) // old saves: already owned
            } else if (level(req) >= spec.unlockLevel) {
                unlockedUpgrades.add(spec.type.name)
                showToast("New upgrade unlocked: ${spec.title}")
                audio.play(R.raw.sfx_buy)
            }
        }
        if (tutorialStage == 2 && level(UpgradeType.ENGINE) >= 1) tutorialAdvance(2)
        if (tutorialStage == 3 && boostTaps >= 1) tutorialAdvance(3)
    }

    // ---- Google Play sign-in and cloud save ----
    private var cloud: CloudSaveProvider = NoCloud
    var cloudSignedIn by mutableStateOf(false); private set
    var cloudStatus by mutableStateOf(""); private set
    /** A cloud save that is further along than this device: waits for the player's choice. */
    var cloudConflict by mutableStateOf<String?>(null); private set

    fun attachCloud(provider: CloudSaveProvider) {
        cloud = provider
        if (Balance.PLAY_GAMES_ENABLED) {
            provider.init()
            provider.checkSignedIn { ok ->
                cloudSignedIn = ok
                if (ok) syncFromCloud()
            }
        }
    }

    fun cloudSignIn() {
        if (!Balance.PLAY_GAMES_ENABLED) {
            cloudStatus = "Google Play sign-in is not set up in this build yet."
            return
        }
        cloud.signIn { ok ->
            cloudSignedIn = ok
            cloudStatus = if (ok) "Signed in. Progress is saved to your Google account." else "Sign-in failed. Try again."
            if (ok) syncFromCloud()
        }
    }

    private fun progressScore(): Double = evolutions * 1e9 + totalDistance

    private fun syncFromCloud() {
        cloud.load { json ->
            if (json == null) {
                cloudSaveNow()
            } else {
                val cloudScore = try {
                    org.json.JSONObject(json).let { it.optInt("evo", 0) * 1e9 + (it.optString("totalDist", "0").toDoubleOrNull() ?: 0.0) }
                } catch (e: Exception) { 0.0 }
                if (cloudScore > progressScore() + 1.0) cloudConflict = json else cloudSaveNow()
            }
        }
    }

    fun describeCloud(json: String): String = try {
        val o = org.json.JSONObject(json)
        "${o.optInt("evo", 0)} evolutions, ${fmtDistance(o.optString("totalDist", "0").toDoubleOrNull() ?: 0.0)} travelled"
    } catch (e: Exception) { "unknown" }

    fun describeLocal(): String = "$evolutions evolutions, ${fmtDistance(totalDistance)} travelled"

    fun useCloudSave() {
        cloudConflict?.let { importProgress(it) }
        cloudConflict = null
    }

    fun keepLocalSave() {
        cloudConflict = null
        cloudSaveNow()
    }

    fun cloudSaveNow() {
        if (cloudSignedIn) {
            cloud.save(exportProgress())
            cloudStatus = "Saved to the cloud."
        }
    }

    private fun exportProgress(): String = org.json.JSONObject().apply {
        put("coins", coins.toString()); put("dist", runDistance.toString())
        put("cores", cores); put("evo", evolutions); put("spent", coresSpent)
        put("levels", levels.joinToString(",")); put("perm", permLevels.joinToString(","))
        put("totalDist", totalDistance.toString()); put("taps", boostTaps); put("purch", purchases)
        put("streak", dailyStreak); put("dailyDay", lastDailyDay)
        put("ach", unlocked.joinToString(",")); put("unl", unlockedUpgrades.joinToString(","))
        put("livery", liveryIndex); put("tstage", tutorialStage); put("gems", gems); put("inv", inventory.joinToString(",")); put("xp", xp); put("plvl", playerLevel); put("jq", journeyClaimed.joinToString(",")); put("devinst", devicesInstalled)
    }.toString()

    private fun importProgress(json: String) {
        try {
            val o = org.json.JSONObject(json)
            fun ints(key: String) = o.optString(key, "").split(",").mapNotNull { it.toIntOrNull() }
            coins = o.optString("coins", "0").toDoubleOrNull() ?: 0.0
            runDistance = o.optString("dist", "0").toDoubleOrNull() ?: 0.0
            cores = o.optInt("cores", 0); evolutions = o.optInt("evo", 0); coresSpent = o.optInt("spent", 0)
            val lv = ints("levels"); for (i in levels.indices) levels[i] = lv.getOrElse(i) { 0 }
            val pl = ints("perm"); for (i in permLevels.indices) permLevels[i] = pl.getOrElse(i) { 0 }
            totalDistance = o.optString("totalDist", "0").toDoubleOrNull() ?: 0.0
            boostTaps = o.optInt("taps", 0); purchases = o.optInt("purch", 0)
            dailyStreak = o.optInt("streak", 0); lastDailyDay = o.optLong("dailyDay", -1L)
            unlocked.clear(); unlocked.addAll(o.optString("ach", "").split(",").filter { it.isNotEmpty() })
            unlockedUpgrades.clear(); unlockedUpgrades.addAll(o.optString("unl", "").split(",").filter { it.isNotEmpty() })
            liveryIndex = o.optInt("livery", 0)
            gems = max(gems, o.optInt("gems", 0))
            if (o.optInt("plvl", 1) > playerLevel) { playerLevel = o.optInt("plvl", 1); xp = o.optInt("xp", 0) }
            val inv = o.optString("inv", "").split(",").mapNotNull { it.toIntOrNull() }
            for (i in inventory.indices) inventory[i] = max(inventory[i], inv.getOrElse(i) { 0 })
            tutorialStage = o.optInt("tstage", 4)
            o.optString("jq", "").split(",").filter { it.isNotEmpty() && it !in journeyClaimed }.let { journeyClaimed.addAll(it) }
            devicesInstalled = max(devicesInstalled, o.optInt("devinst", 0))
            dailyReady = lastDailyDay != today()
            save()
            showToast("Cloud save loaded")
        } catch (e: Exception) {
            showToast("Could not read the cloud save")
        }
    }
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
    /** Screenshot tests only: where the next station appears (screen dp), NaN = the normal place. */
    internal var debugStationAt = Float.NaN

    /** Screenshot tests only: jump to a time of day and bring in a station. */
    internal fun debugScene(time: Double, station: Float) {
        dayTime = time
        debugStationAt = station
        stationSerial++
    }
    private var nextStationIn = Balance.STATION_MIN_GAP

    private fun showToast(text: String) {
        toast = text
        toastLeft = 3.5
    }

    // Snow storm: its own random timer, independent of the other events.
    var stormLeft by mutableDoubleStateOf(0.0); private set
    private var stormTotal = 0.0
    private var nextStormIn = 60.0 + Math.random() * 60.0

    /** 0..1, fades in and out so the storm builds up smoothly. */
    val stormLevel: Float
        get() = if (stormLeft <= 0.0) 0f
        else (min(stormLeft, stormTotal - stormLeft) / Balance.STORM_FADE_SEC).coerceIn(0.0, 1.0).toFloat()

    // Day and night cycle
    var dayTime by mutableDoubleStateOf(0.0); private set
    private val cycleSec = Balance.DAY_SEC + Balance.DUSK_SEC + Balance.NIGHT_SEC + Balance.DAWN_SEC

    /** 0 (bright) .. 0.42 (darkest), used to tint the scene. */
    val darkness: Float
        get() {
            val t = dayTime
            val d = Balance.DAY_SEC
            val du = Balance.DUSK_SEC
            val n = Balance.NIGHT_SEC
            val v = when {
                t < d -> 0.0
                t < d + du -> (t - d) / du
                t < d + du + n -> 1.0
                else -> 1.0 - (t - d - du - n) / Balance.DAWN_SEC
            }
            return (v * 0.42).toFloat()
        }

    /** Current phase name and seconds until the next phase. */
    fun dayPhase(): Pair<String, Int> {
        val t = dayTime
        val d = Balance.DAY_SEC
        val du = Balance.DUSK_SEC
        val n = Balance.NIGHT_SEC
        return when {
            t < d -> "Day" to (d - t).toInt() + 1
            t < d + du -> "Dusk" to (d + du - t).toInt() + 1
            t < d + du + n -> "Night" to (d + du + n - t).toInt() + 1
            else -> "Dawn" to (cycleSec - t).toInt() + 1
        }
    }

    private val eventSpeedMult: Double
        get() = if (stormLeft > 0.0) 1.0 - (1.0 - Balance.STORM_SPEED) * (1.0 - min(0.9, effSum(Eff.STORM))) else 1.0
    private val eventIncomeMult: Double
        get() = if (event == GameEvent.AURORA) Balance.AURORA_INCOME + effSum(Eff.AURORA) else 1.0

    private fun startRandomEvent() {
        event = if ((0..1).random() == 0) GameEvent.AURORA else GameEvent.CRATE
        eventLeft = when (event) {
            GameEvent.AURORA -> Balance.AURORA_SEC
            else -> 120.0 // the scene ends it earlier, when the crate has passed the train
        }
        if (event == GameEvent.CRATE) crateSerial++
        showToast(
            when (event) {
                GameEvent.AURORA -> "Aurora! Income x${fmt(Balance.AURORA_INCOME, 1)} for ${Balance.AURORA_SEC.toInt()}s"
                else -> "Supply crate ahead! Tap it before it passes"
            }
        )
    }

    /** Increases when a supply crate appears beside the track (the scene then scrolls it past the train). */
    var crateSerial by mutableIntStateOf(0); private set

    /** The crate scrolled away without being tapped. */
    fun missCrate() {
        if (event != GameEvent.CRATE) return
        event = GameEvent.NONE
        eventLeft = 0.0
        showToast("The supply crate slipped away...")
    }

    fun collectCrate() {
        if (event != GameEvent.CRATE) return
        val gain = max(50.0, baseCoinsPerSec * 30.0) * (1.0 + effSum(Eff.CRATE))
        coins += gain
        cratesCollected++
        event = GameEvent.NONE
        eventLeft = 0.0
        audio.play(R.raw.sfx_buy)
        showToast("Supply crate: +${fmt(gain)} coins")
    }

    // ---- passengers: they get on and off at every station; more on board = a bigger (random) coin bonus ----
    var passengers by mutableIntStateOf(prefs.getInt("pax", 0)); private set
    /** Income multiplier from the passengers on board, rolled again at every station. */
    var paxMult by mutableDoubleStateOf(prefs.getFloat("paxMult", 1f).toDouble()); private set
    /** Seats: four per car. */
    val seats: Int get() = 4 * carsNow

    /**
     * Called by the scene when the train stops at a station. Returns (got off, got on) for the animation.
     * Fares: the station pays more for every passenger who boards.
     */
    fun collectStation(shelter: Boolean = false, waiting: Int = 6): IntArray {
        val off = if (passengers > 0) (0..(passengers + 1) / 2).random() else 0
        val left = passengers - off
        val on = (1..max(1, waiting)).random().coerceAtMost(max(0, seats - left))
        passengers = left + on
        // every passenger adds 2% to 6% income, rolled again at each stop
        paxMult = 1.0 + passengers * (0.02 + Math.random() * 0.04)
        val gain = max(30.0, baseCoinsPerSec * Balance.STATION_BONUS_SEC) * (1.0 + effSum(Eff.STATION)) *
            (if (shopTicketLeft > 0.0) 3.0 else 1.0) * (if (shelter) 2.0 else 1.0) * (1.0 + 0.15 * on)
        coins += gain
        stationsVisited++
        if (shelter) stormsSheltered++
        audio.play(R.raw.sfx_brake, 0.8f)
        showToast(
            if (shelter) "Storm shelter: +${fmt(gain)} coins · $on aboard, $off off"
            else "Station: +${fmt(gain)} coins · $on aboard, $off off · passengers x${fmt(paxMult, 2)}"
        )
        return intArrayOf(off, on)
    }

    /** The scene tells the game which part of the frozen world the train is passing. */
    fun enterZone(name: String) { showToast("Now passing: $name") }

    private fun tickEvents(dt: Double) {
        dayTime = (dayTime + dt) % cycleSec
        if (stormLeft > 0.0) {
            stormLeft -= dt
        } else {
            nextStormIn -= dt
            if (nextStormIn <= 0.0) {
                nextStormIn = Balance.STORM_MIN_GAP + Math.random() * (Balance.STORM_MAX_GAP - Balance.STORM_MIN_GAP)
                stormTotal = Balance.STORM_MIN_SEC + Math.random() * (Balance.STORM_MAX_SEC - Balance.STORM_MIN_SEC)
                stormLeft = stormTotal
                stormStationSerial++
                showToast("Snow storm! Speed -25% for ${stormTotal.toInt()}s")
            }
        }
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
        nextStationIn -= dt
        if (nextStationIn <= 0.0) {
            nextStationIn = Balance.STATION_MIN_GAP + Math.random() * (Balance.STATION_MAX_GAP - Balance.STATION_MIN_GAP)
            stationSerial++
        }
    }

    // Display settings
    var fpsCap by mutableIntStateOf(prefs.getInt("fps", 60)); private set
    /** Graphics quality: 0 Low (30 fps, no effects, light snow), 1 Medium (60 fps, effects, no post-processing), 2 High (everything). */
    var gfx by mutableIntStateOf(prefs.getInt("gfx", if (prefs.getBoolean("postfx", true)) 2 else 1)); private set
    /** Light bloom, colour grading and vignette: only on High. */
    val postFx: Boolean get() = gfx >= 2
    fun changeFps(v: Int) { fpsCap = v; prefs.edit().putInt("fps", v).apply() }
    fun changeGfx(v: Int) {
        gfx = v.coerceIn(0, 2)
        val fps = when (gfx) { 0 -> 30; 1 -> 60; else -> max(60, prefs.getInt("hifps", 60)) }
        fpsCap = fps
        prefs.edit().putInt("gfx", gfx).putInt("fps", fps).apply()
    }
    /** Frame rate on High (60, 90 or 120). */
    fun changeHighFps(v: Int) {
        prefs.edit().putInt("hifps", v).apply()
        if (gfx == 2) changeFps(v)
    }

    var liveryIndex by mutableIntStateOf(prefs.getInt("livery", 0)); private set
    fun liveryUnlocked(i: Int): Boolean = i <= evolutions
    fun chooseLivery(i: Int) {
        if (!liveryUnlocked(i)) return
        liveryIndex = i
        prefs.edit().putInt("livery", i).apply()
    }

    private fun today(): Long = java.time.LocalDate.now().toEpochDay()
    val dailyNextStreak: Int get() = if (lastDailyDay >= 0 && lastDailyDay == today() - 1) dailyStreak + 1 else 1
    fun dailyCoins(streak: Int): Double =
        max(200.0, baseCoinsPerSec * 900.0) * (1.0 + 0.25 * (min(streak, 7) - 1))
    fun dailyGivesCore(streak: Int): Boolean = streak % 7 == 0
    fun dailyGems(streak: Int): Int = if (streak % 7 == 0) Balance.DAILY_GEMS * 5 else Balance.DAILY_GEMS

    /** Time until tomorrow's daily gift, like "5h 12m". */
    val nextGiftIn: String
        get() {
            val now = java.time.LocalDateTime.now()
            val m = java.time.Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).toMinutes()
            return "${m / 60}h ${m % 60}m"
        }

    fun giftHint() {
        showToast(if (dailyReady) "Your daily gift is ready!" else "Next daily gift in $nextGiftIn")
    }

    fun claimDaily() {
        if (!dailyReady) return
        val streak = dailyNextStreak
        coins += dailyCoins(streak)
        if (dailyGivesCore(streak)) cores += 1
        gems += dailyGems(streak)
        dailyStreak = streak
        lastDailyDay = today()
        dailyReady = false
        audio.play(R.raw.sfx_buy)
        addXp(20)
    }

    fun finishTutorial() {
        tutorialStage = 4
        prefs.edit().putBoolean("tut", true).putInt("tstage", 4).apply()
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

    // ---- player level: XP from achievements, daily quests, evolving and the daily reward ----
    var xp by mutableIntStateOf(prefs.getInt("xp", 0)); private set          // XP inside the current level
    var playerLevel by mutableIntStateOf(prefs.getInt("plvl", 1)); private set
    val xpNeeded: Int get() = xpForLevel(playerLevel)
    var levelUpSerial by mutableIntStateOf(0); private set
    var levelUpGems by mutableIntStateOf(0); private set
    fun levelGems(level: Int): Int = 5 + level

    fun addXp(amount: Int) {
        xp += amount
        while (xp >= xpForLevel(playerLevel)) {
            xp -= xpForLevel(playerLevel)
            playerLevel++
            levelUpGems = levelGems(playerLevel)
            gems += levelUpGems
            levelUpSerial++
            audio.play(R.raw.sfx_evolve, 0.7f)
        }
        save()
    }

    // lifetime counters for achievements and quests
    var cratesCollected by mutableIntStateOf(prefs.getInt("crates", 0)); private set
    var stationsVisited by mutableIntStateOf(prefs.getInt("stations", 0)); private set
    var stormsSheltered by mutableIntStateOf(prefs.getInt("storms", 0)); private set
    var boostersUsed by mutableIntStateOf(prefs.getInt("bused", 0)); private set

    /** Cars the train has right now (a car appears once its first upgrade is unlocked). */
    val carsNow: Int
        get() {
            var n = 2
            for (i in 2 until CAR_LAYOUT.size) {
                val first = CAR_LAYOUT[i][0]
                if (UPGRADES.firstOrNull { it.type == first }?.let { isUnlocked(it) } == true) n = i + 1
            }
            return n
        }

    // ---- daily quests ----
    private fun statValue(s: QuestStat): Int = when (s) {
        QuestStat.UPGRADES -> purchases
        QuestStat.BOOST_TAPS -> boostTaps
        QuestStat.CRATES -> cratesCollected
        QuestStat.STATIONS -> stationsVisited
        QuestStat.BOOSTERS -> boostersUsed
        QuestStat.STORMS -> stormsSheltered
        QuestStat.DISTANCE -> totalDistance.coerceAtMost(2e9).toInt()
        QuestStat.DEVICES -> devicesInstalled
        QuestStat.EVOLVES -> evolutions
        QuestStat.LEVEL -> playerLevel
        QuestStat.SURVIVORS -> level(UpgradeType.SURVIVORS)
    }
    var devicesInstalled by mutableIntStateOf(prefs.getInt("devinst", 0)); private set

    // ---- journey quests: lifetime goals, each claimed once ----
    val journeyClaimed = mutableStateListOf<String>().apply {
        prefs.getString("jq", "")?.split(",")?.filter { it.isNotEmpty() }?.let { addAll(it) }
    }
    fun journeyProgress(q: Quest): Int = statValue(q.stat).coerceIn(0, q.goal)
    fun claimJourney(q: Quest) {
        if (q.id in journeyClaimed || journeyProgress(q) < q.goal) return
        journeyClaimed.add(q.id)
        gems += q.gems
        audio.play(R.raw.sfx_buy)
        showToast("Journey quest done: +${q.xp} XP, +${q.gems} gems")
        addXp(q.xp)
    }
    private var questDay = prefs.getLong("qDay", -1L)
    private var questBase = prefs.getString("qBase", "")?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    val questClaimed = mutableStateListOf(false, false, false, false).apply {
        prefs.getString("qClaim", "")?.split(",")?.forEachIndexed { i, v -> if (i < size) this[i] = v == "1" }
    }

    /** Today's three quests (the same for the whole day). */
    val todaysQuests: List<Quest>
        get() {
            val r = java.util.Random(today() * 7919L)
            return QUEST_POOL.shuffled(r).distinctBy { it.stat }.take(4)
        }

    private fun refreshQuestDay() {
        if (questDay == today() && questBase.size < QuestStat.values().size) {
            // an update added new kinds of quests: start those from now, keep today's progress and claims
            questBase = questBase + QuestStat.values().drop(questBase.size).map { statValue(it) }
            return
        }
        if (questDay == today()) return
        questDay = today()
        questBase = QuestStat.values().map { statValue(it) }
        for (i in questClaimed.indices) questClaimed[i] = false
    }

    fun questProgress(q: Quest): Int {
        refreshQuestDay()
        return (statValue(q.stat) - questBase.getOrElse(q.stat.ordinal) { statValue(q.stat) }).coerceIn(0, q.goal)
    }

    val questsReady: Int get() = todaysQuests.indices.count { !questClaimed[it] && questProgress(todaysQuests[it]) >= todaysQuests[it].goal } +
        JOURNEY_QUESTS.count { it.id !in journeyClaimed && journeyProgress(it) >= it.goal }

    fun claimQuest(i: Int) {
        val q = todaysQuests.getOrNull(i) ?: return
        if (questClaimed[i] || questProgress(q) < q.goal) return
        questClaimed[i] = true
        gems += q.gems
        audio.play(R.raw.sfx_buy)
        showToast("Quest done: +${q.xp} XP, +${q.gems} gems")
        addXp(q.xp)
    }

    private fun achievementMet(id: String): Boolean = when (id) {
        "km10" -> totalDistance >= 10_000.0
        "km1000" -> totalDistance >= 1_000_000.0
        "upg200" -> purchases >= 200
        "upg1000" -> purchases >= 1000
        "crate1" -> cratesCollected >= 1
        "crate25" -> cratesCollected >= 25
        "station10" -> stationsVisited >= 10
        "station100" -> stationsVisited >= 100
        "storm1" -> stormsSheltered >= 1
        "storm20" -> stormsSheltered >= 20
        "booster1" -> boostersUsed >= 1
        "booster25" -> boostersUsed >= 25
        "surv1" -> level(UpgradeType.SURVIVORS) >= 1
        "surv4" -> level(UpgradeType.SURVIVORS) >= 4
        "cars6" -> carsNow >= 6
        "cars11" -> carsNow >= 11
        "rich2" -> coins >= 1e9
        "gems100" -> gems >= 100
        "streak7" -> dailyStreak >= 7
        "evo5" -> evolutions >= 5
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
            if (a.cores > 0) cores += a.cores else coins += max(100.0, baseCoinsPerSec * 600.0)
            achievementPopup = "${a.title}  ·  +${a.xp} XP"
            popupLeft = 3.5
            audio.play(R.raw.sfx_buy)
            addXp(a.xp)
        }
    }

    /** Wipes all progress (settings stay). */
    fun resetAll() {
        liveryIndex = 0
        prefs.edit().clear().putInt("gems", gems).putString("inv", inventory.joinToString(",")).putInt("fps", fpsCap).putInt("gfx", gfx).putInt("hifps", prefs.getInt("hifps", 60)).putBoolean("sound", soundOn).putFloat("mvol", musicVolume).putFloat("svol", sfxVolume).apply()
        coins = 0.0; runDistance = 0.0; cores = 0; evolutions = 0; coresSpent = 0
        for (i in levels.indices) levels[i] = 0
        for (i in permLevels.indices) permLevels[i] = 0
        totalDistance = 0.0; boostTaps = 0; purchases = 0
        dailyStreak = 0; lastDailyDay = -1L; dailyReady = true
        unlocked.clear(); achievementPopup = null
        event = GameEvent.NONE; eventLeft = 0.0; adBoostLeft = 0.0; toast = null
        stormLeft = 0.0
        shopIncomeLeft = 0.0; shopSpeedLeft = 0.0; shopTicketLeft = 0.0; shopFuelLeft = 0.0
        for (i in rushLeft.indices) rushLeft[i] = 0.0
        passengers = 0; paxMult = 1.0
        boostMeter = 1.0; boostTimeLeft = 0.0; boostLocked = false
        offlineCoins = 0.0; offlineMeters = 0.0
        tutorialStage = 0
        unlockedUpgrades.clear()
        xp = 0; playerLevel = 1; cratesCollected = 0; stationsVisited = 0; stormsSheltered = 0; boostersUsed = 0
        devicesInstalled = 0; journeyClaimed.clear()
        questDay = -1L
        save()
    }
    var soundOn by mutableStateOf(prefs.getBoolean("sound", true)); private set
    var vibrationOn by mutableStateOf(prefs.getBoolean("vibrate", true).also { Haptics.enabled = it }); private set
    fun toggleVibration() { vibrationOn = !vibrationOn; Haptics.enabled = vibrationOn; prefs.edit().putBoolean("vibrate", vibrationOn).apply(); if (vibrationOn) Haptics.confirm() }
    /** Reminders while the game is closed (daily gift ready, idle earnings full). */
    var remindersOn by mutableStateOf(prefs.getBoolean("remind", true)); private set
    fun toggleReminders() { remindersOn = !remindersOn; prefs.edit().putBoolean("remind", remindersOn).apply() }
    private var hornIn = 40.0
    private var wolfIn = 25.0
    private var birdIn = 4.0
    private var buyCombo = 0
    private var lastBuyAt = 0L
    private val birds = listOf(R.raw.sfx_bird1, R.raw.sfx_bird2, R.raw.sfx_bird3)
    private var ambienceIn = 0.0

    /** Nature sounds: wind in a storm, crickets (and now and then a far-away wolf) at night, birds by day. */
    private fun tickAmbience(dt: Double) {
        ambienceIn -= dt
        if (ambienceIn > 0.0) return
        ambienceIn = 1.0
        val phase = dayPhase().first
        val night = phase == "Night" || phase == "Dusk"
        audio.setAmbience(
            when {
                stormLevel > 0.15f -> R.raw.amb_storm
                night -> R.raw.amb_night
                else -> R.raw.amb_day
            }
        )
        // birds sing now and then (never non-stop), at random times, pitches and sides
        if (!night && stormLevel <= 0.15f) {
            birdIn -= 1.0
            if (birdIn <= 0.0) {
                birdIn = 6.0 + Math.random() * 18.0
                audio.play(birds.random(), 0.25f + Math.random().toFloat() * 0.3f, 0.88f + Math.random().toFloat() * 0.3f, Math.random().toFloat() * 1.6f - 0.8f)
            }
        }
        if (phase == "Night" && stormLevel <= 0.15f) {
            wolfIn -= 1.0
            if (wolfIn <= 0.0) {
                wolfIn = 35.0 + Math.random() * 50.0
                audio.play(R.raw.sfx_wolf, 0.5f)
            }
        }
    }

    fun toggleSound() {
        soundOn = !soundOn
        audio.enabled = soundOn
        prefs.edit().putBoolean("sound", soundOn).apply()
        if (soundOn) { audio.startMusic(); ambienceIn = 0.0 } else { audio.pauseMusic(); audio.pauseAmbience() }
    }

    /** The app went to the background or came back: no sound at all while it is in the background. */
    fun setForeground(on: Boolean) {
        audio.foreground = on
        Haptics.foreground = on
        if (on) { audio.startMusic(); ambienceIn = 0.0 }
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

    private val extraSpecs: List<UpgradeSpec> = UPGRADES.filter { it.eff != null }
    /** Total bonus from all generic upgrades of one kind (per level x level). */
    private fun effSum(e: Eff): Double {
        var sum = 0.0
        for (sp in extraSpecs) if (sp.eff == e) sum += sp.per * levels[sp.type.ordinal]
        return sum
    }

    val tierIndex: Int get() = min(evolutions, TIERS.lastIndex)

    /** Speed from upgrades and weather, without shop boosts or station stops. */
    val cruiseSpeed: Double
        get() = (Balance.BASE_SPEED + Balance.SPEED_PER_ENGINE * level(UpgradeType.ENGINE)) *
            milestoneMult(UpgradeType.ENGINE) *
            (1.0 + Balance.PLOW_BONUS * level(UpgradeType.PLOW)) *
            (1.0 + Balance.TIER_SPEED_BONUS * tierIndex) *
            (1.0 + Balance.PERM_SPEED * permLevel(PermType.SPEED)) * (1.0 + effSum(Eff.SPEED)) * eventSpeedMult

    val coinsPerMeter: Double
        get() = (1.0 + Balance.WAGON_BONUS * level(UpgradeType.WAGON)) * milestoneMult(UpgradeType.WAGON) *
            (1.0 + Balance.HEAT_BONUS * level(UpgradeType.HEAT)) * milestoneMult(UpgradeType.HEAT) *
            (1.0 + Balance.FOOD_BONUS * level(UpgradeType.FOOD)) * milestoneMult(UpgradeType.FOOD) *
            Balance.TIER_INCOME.pow(tierIndex) *
            (1.0 + Balance.CORE_BONUS * cores) *
            (1.0 + Balance.PERM_INCOME * permLevel(PermType.INCOME)) *
            (1.0 + Balance.PERM_CREW * permLevel(PermType.CREW)) *
            (1.0 + effSum(Eff.INCOME)) *
            (1.0 + Balance.SURVIVOR_BONUS * level(UpgradeType.SURVIVORS))

    val boostMultiplier: Double
        get() = Balance.BOOST_BASE_MULT + Balance.BOOST_MULT_PER_LEVEL * level(UpgradeType.BOOSTER) +
            Balance.PERM_BOOST * permLevel(PermType.BOOST) + effSum(Eff.BOOSTMULT)

    val isBoosting: Boolean get() = boostTimeLeft > 0.0
    val incomeMultiplier: Double
        get() = (if (isBoosting) boostMultiplier else 1.0) * (if (adBoostLeft > 0.0) Balance.AD_BOOST_MULT else 1.0) * eventIncomeMult * nightMult * shopIncomeMult * paxMult
    private val nightMult: Double get() = if (darkness > 0.2f) 1.0 + effSum(Eff.NIGHT) else 1.0
    /** Visual travel speed for the scene (station stops are applied by the scene itself). */
    val travelSpeed: Double get() = cruiseSpeed * shopSpeedMult
    val speed: Double get() = travelSpeed * moveFactor
    val coinsPerSec: Double get() = speed * coinsPerMeter * incomeMultiplier
    /** Steady income without boosts, shop items or stops; used to size rewards and shop prices. */
    private val baseCoinsPerSec: Double get() = cruiseSpeed * coinsPerMeter

    // ---- train stops at a station during storms (driven by the scene) ----
    private var moveFactorRaw = 1.0
    private var moveFactorAt = 0L
    /** 0 = standing at a station, 1 = full speed. Falls back to 1 if the scene stops reporting (app in background). */
    val moveFactor: Double
        get() = if (System.currentTimeMillis() - moveFactorAt > 2500L) 1.0 else moveFactorRaw
    fun setMoveFactor(f: Float) {
        moveFactorRaw = f.toDouble().coerceIn(0.0, 1.0)
        moveFactorAt = System.currentTimeMillis()
    }
    /** Increases when a storm starts; the scene then brings in a shelter station and stops the train there. */
    var stormStationSerial by mutableIntStateOf(0); private set

    // ---- shop ----
    var shopIncomeLeft by mutableDoubleStateOf(prefs.getFloat("shopInc", 0f).toDouble()); private set
    var shopSpeedLeft by mutableDoubleStateOf(prefs.getFloat("shopSpd", 0f).toDouble()); private set
    var shopTicketLeft by mutableDoubleStateOf(prefs.getFloat("shopTix", 0f).toDouble()); private set
    /** Endless Fuel: BOOST taps use no fuel while this runs. */
    var shopFuelLeft by mutableDoubleStateOf(prefs.getFloat("shopFuel", 0f).toDouble()); private set
    /** Gold Rush x5 / x10 and Jackpot x100 timers. */
    val rushLeft = mutableStateListOf(prefs.getFloat("rush5", 0f).toDouble(), prefs.getFloat("rush10", 0f).toDouble(), prefs.getFloat("rush100", 0f).toDouble())
    private fun rushIndex(item: ShopItem): Int = when (item) { ShopItem.RUSH5 -> 0; ShopItem.RUSH10 -> 1; ShopItem.RUSH100 -> 2; else -> -1 }
    val endlessFuel: Boolean get() = shopFuelLeft > 0.0
    /** Timed boosters that are running right now. */
    val activeBoosters: List<ShopItem> get() = TIMED_ITEMS.filter { shopLeft(it) > 0.0 }
    /** Stop a running booster early (what is left is lost). */
    fun cancelBooster(item: ShopItem) {
        when (item) {
            ShopItem.COCOA -> shopIncomeLeft = 0.0
            ShopItem.TURBO -> shopSpeedLeft = 0.0
            ShopItem.TICKET -> shopTicketLeft = 0.0
            ShopItem.TANK -> shopFuelLeft = 0.0
            ShopItem.RUSH5, ShopItem.RUSH10, ShopItem.RUSH100 -> rushLeft[rushIndex(item)] = 0.0
            else -> return
        }
        showToast("${item.title} stopped")
        save()
    }
    val shopSpeedMult: Double get() = if (shopSpeedLeft > 0.0) 1.5 else 1.0
    /** The strongest coin booster running right now (Hot Cocoa x2, Gold Rush x5/x10, Jackpot x100). */
    private val shopIncomeMult: Double
        get() = listOf(ShopItem.COCOA, ShopItem.RUSH5, ShopItem.RUSH10, ShopItem.RUSH100).filter { shopLeft(it) > 0.0 }.maxOfOrNull { coinBoost(it) } ?: 1.0
    fun shopCost(item: ShopItem): Int = item.gems
    fun shopLeft(item: ShopItem): Double = when (item) {
        ShopItem.COCOA -> shopIncomeLeft
        ShopItem.TURBO -> shopSpeedLeft
        ShopItem.TICKET -> shopTicketLeft
        ShopItem.TANK -> shopFuelLeft
        ShopItem.RUSH5, ShopItem.RUSH10, ShopItem.RUSH100 -> rushLeft[rushIndex(item)]
        else -> 0.0
    }
    fun shopAvailable(item: ShopItem): Boolean = when (item) {
        ShopItem.CLEAR -> stormLeft > 0.0
        else -> shopLeft(item) + item.durationSec <= Balance.SHOP_MAX_SEC &&
            (shopLeft(item) > 0.0 || activeBoosters.size < Balance.MAX_ACTIVE_BOOSTERS)
    }
    /** Why a booster cannot be used right now, or null. */
    fun whyNotUse(item: ShopItem): String? = when {
        owned(item) <= 0 -> null
        item == ShopItem.CLEAR && stormLeft <= 0.0 -> "Use it during a snow storm"
        item != ShopItem.CLEAR && shopLeft(item) <= 0.0 && activeBoosters.size >= Balance.MAX_ACTIVE_BOOSTERS -> "${Balance.MAX_ACTIVE_BOOSTERS} boosters are already running"
        item != ShopItem.CLEAR && shopLeft(item) + item.durationSec > Balance.SHOP_MAX_SEC -> "Already stacked to 30 minutes"
        else -> null
    }
    /** Boosters you own but have not used yet (bought in the shop with gems). */
    val inventory = mutableStateListOf<Int>().apply {
        val saved = prefs.getString("inv", "")?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
        ShopItem.values().indices.forEach { add(saved.getOrElse(it) { 0 }) }
    }
    fun owned(item: ShopItem): Int = inventory[item.ordinal]
    val inventoryCount: Int get() = inventory.sum()

    /** The last booster used, for the on-screen pop-up. Increases with every use. */
    var usedSerial by mutableIntStateOf(0); private set
    var lastUsed by mutableStateOf<ShopItem?>(null); private set

    /** Shop: pay gems, the booster goes to the inventory. */
    fun buyShop(item: ShopItem) {
        val cost = shopCost(item)
        if (gems < cost) return
        gems -= cost
        inventory[item.ordinal] = owned(item) + 1
        audio.play(R.raw.sfx_buy)
        showToast("${item.title} added to your bag")
        save()
    }

    /** Can this booster be used right now (timed boosters stack up to 30 minutes, the others need a reason)? */
    fun canUse(item: ShopItem): Boolean = owned(item) > 0 && shopAvailable(item)

    /** Inventory: use one booster. */
    fun useItem(item: ShopItem) {
        if (!canUse(item)) return
        inventory[item.ordinal] = owned(item) - 1
        when (item) {
            ShopItem.COCOA -> shopIncomeLeft += item.durationSec
            ShopItem.TURBO -> shopSpeedLeft += item.durationSec
            ShopItem.TICKET -> shopTicketLeft += item.durationSec
            ShopItem.TANK -> { shopFuelLeft += item.durationSec; boostMeter = 1.0; boostLocked = false }
            ShopItem.CLEAR -> { stormLeft = min(stormLeft, Balance.STORM_FADE_SEC) }
            ShopItem.RUSH5, ShopItem.RUSH10, ShopItem.RUSH100 -> rushLeft[rushIndex(item)] += item.durationSec
        }
        lastUsed = item
        usedSerial++
        boostersUsed++
        audio.play(R.raw.sfx_evolve, 0.6f)
        save()
    }

    // ---- gems: premium currency for the shop ----
    var gems by mutableIntStateOf(prefs.getInt("gems", 0)); private set
    private var gemTradeDay = prefs.getLong("gemDay", -1L)
    var gemsTradedToday by mutableIntStateOf(if (gemTradeDay == java.time.LocalDate.now().toEpochDay()) prefs.getInt("gemTraded", 0) else 0); private set
    val gemTradeLeft: Int get() = Balance.GEM_TRADE_PER_DAY - (if (gemTradeDay == today()) gemsTradedToday else 0)
    fun gemTradeCost(t: GemTrade): Double = floor(max(250.0 * t.gems, baseCoinsPerSec * t.incomeSec))
    fun tradeForGems(t: GemTrade) {
        val cost = gemTradeCost(t)
        if (coins < cost || gemTradeLeft < t.gems) return
        if (gemTradeDay != today()) { gemTradeDay = today(); gemsTradedToday = 0 }
        coins -= cost
        gemsTradedToday += t.gems
        gems += t.gems
        audio.play(R.raw.sfx_buy)
        showToast("+${t.gems} gems")
        save()
    }

    /** Google Play purchases; NoPurchases until Balance.IAP_ENABLED is switched on. */
    var store: PurchaseProvider = NoPurchases
    var storeVersion by mutableIntStateOf(0); private set
    fun onStorePrices() { storeVersion++ }
    fun gemPackPrice(p: GemPack): String? { storeVersion; return store.price(p.productId) }
    fun buyGemPack(p: GemPack) {
        if (!Balance.IAP_ENABLED) {
            showToast("Gem packs open after the game launches on Google Play")
            return
        }
        store.purchase(p.productId)
    }
    /** Called by the store once Google Play confirms a purchase. */
    fun grantProduct(productId: String, quantity: Int) {
        val pack = GemPack.values().firstOrNull { it.productId == productId } ?: return
        gems += pack.gems * quantity.coerceAtLeast(1)
        audio.play(R.raw.sfx_buy)
        showToast("+${pack.gems * quantity.coerceAtLeast(1)} gems. Thank you!")
        save()
        cloudSaveNow()
    }
    private val rechargePerSec: Double
        get() = Balance.BOOST_RECHARGE_PER_SEC * (1.0 + Balance.COAL_BONUS * level(UpgradeType.COAL)) * (1.0 + effSum(Eff.RECHARGE))
    /** Idle earnings count for at most this many seconds. */
    val offlineCapSec: Long get() = Balance.OFFLINE_CAP_SEC + Balance.OFFLINE_CAP_PER_PERM * permLevel(PermType.OFFLINE)
    var offlineAwaySec = 0L; private set
    var offlineCountedSec = 0L; private set
    private val offlineRate: Double
        get() = min(1.0, Balance.OFFLINE_BASE + Balance.CONDUCTOR_BONUS * level(UpgradeType.CONDUCTOR) +
            Balance.PERM_OFFLINE * permLevel(PermType.OFFLINE) + effSum(Eff.OFFLINE))

    /** Upgrade levels bought in this run; evolving needs [evolveLevels] of them. */
    val runLevels: Int get() = levels.sum()
    val evolveLevels: Int get() = Balance.evolveLevels(evolutions)
    val canEvolve: Boolean get() = runLevels >= evolveLevels
    val coreGain: Int get() = max(1, runLevels / 50)
    /** Cars this train tier can have; more unlock with every evolve. */
    val maxCars: Int get() = Balance.maxCars(evolutions)
    fun blockedByTier(spec: UpgradeSpec): Boolean = carOf(spec.type) >= maxCars && !isUnlocked(spec)

    fun upgradeCost(spec: UpgradeSpec): Double =
        floor(spec.baseCost * spec.growth.pow(level(spec.type)))

    /** Cost of buying n levels at once (geometric sum). */
    fun bulkCost(spec: UpgradeSpec, n: Int): Double =
        floor(spec.baseCost * spec.growth.pow(level(spec.type)) * (spec.growth.pow(n) - 1.0) / (spec.growth - 1.0))

    /** How many levels a buy press would purchase right now (at least 1 so the price is always shown). */
    fun buyCount(spec: UpgradeSpec): Int {
        val room = spec.maxLevel - level(spec.type)
        if (room <= 0) return 0
        if (level(spec.type) == 0) return 1 // a device is bought (installed) first, then upgraded
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
        UiSound.audio = audio
        load()
        if (!prefs.contains("devinst")) devicesInstalled = levels.count { it > 0 } // older saves
        dailyReady = lastDailyDay != today()
        startLoop()
    }

    // ---- actions ----
    fun buy(spec: UpgradeSpec) {
        if (!isUnlocked(spec)) return
        val lvl = level(spec.type)
        if (lvl >= spec.maxLevel) return
        val n = buyCount(spec)
        val cost = bulkCost(spec, n)
        if (n > 0 && coins >= cost) {
            coins -= cost
            levels[spec.type.ordinal] = lvl + n
            purchases += n
            if (lvl == 0) devicesInstalled++
            if (lvl == 0) {
                audio.play(R.raw.sfx_install, 0.8f)
            } else {
                // quick repeated upgrades climb in pitch, like a little combo
                val now = System.currentTimeMillis()
                buyCombo = if (now - lastBuyAt < 900L) min(buyCombo + 1, 8) else 0
                lastBuyAt = now
                audio.play(R.raw.sfx_upgrade, 0.75f, 1f + buyCombo * 0.05f)
            }
        }
    }

    /** Each Boost tap costs 10% of the meter and grants 3 seconds of income multiplier. */
    fun boost() {
        if (boostLocked || boostMeter < Balance.BOOST_COST_PER_TAP - 0.005) return
        if (boostTimeLeft <= 0.0) audio.play(R.raw.sfx_boost, 0.55f) // only when a boost starts, not on every tap
        boostTaps++
        if (!endlessFuel) boostMeter = max(0.0, boostMeter - Balance.BOOST_COST_PER_TAP)
        val extraLen = effSum(Eff.BOOSTLEN)
        boostTimeLeft = min(Balance.BOOST_MAX_SEC + extraLen * 4.0, boostTimeLeft + Balance.BOOST_SEC_PER_TAP + extraLen)
        if (!endlessFuel && boostMeter < Balance.BOOST_COST_PER_TAP - 0.005) boostLocked = true // locked until fully recharged
    }

    /** Increases with every evolve. */
    var evolveSerial by mutableIntStateOf(0); private set
    /** The "New train!" pop-up is waiting to be seen. */
    var newTrainPending by mutableStateOf(false); private set
    fun dismissNewTrain() { newTrainPending = false }

    fun evolve() {
        if (!canEvolve) return
        evolveSerial++
        newTrainPending = true
        audio.play(R.raw.sfx_evolve)
        cores += coreGain
        evolutions++
        for (i in levels.indices) levels[i] = 0 // temporary upgrades reset; permanent ones stay
        unlockedUpgrades.clear()                // the unlock chain (and the extra cars) start again
        coins = startCoins
        runDistance = 0.0
        boostMeter = 1.0
        boostTimeLeft = 0.0
        boostLocked = false
        addXp(100) // also saves
    }

    // ---- rewarded ads: free gems, a mystery booster or a short coin rush (limited per day) ----
    var adReady by mutableStateOf(false); private set
    fun onAdReady(ready: Boolean) { adReady = ready }
    private var adDay = prefs.getLong("adDay", -1L)
    val adCounts = mutableStateListOf<Int>().apply {
        val saved = if (adDay == java.time.LocalDate.now().toEpochDay()) prefs.getString("adCnt", "")?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList() else emptyList()
        AdReward.values().indices.forEach { add(saved.getOrElse(it) { 0 }) }
    }
    fun adsLeft(r: AdReward): Int {
        if (adDay != today()) return r.perDay
        return (r.perDay - adCounts[r.ordinal]).coerceAtLeast(0)
    }
    fun watchAd(r: AdReward) {
        if (!Balance.ADS_ENABLED || adsLeft(r) <= 0) return
        if (!adReady) { showToast("The ad is still loading, try again in a moment"); return }
        ads.showRewarded { grantAd(r) }
    }
    private fun grantAd(r: AdReward) {
        if (adDay != today()) { adDay = today(); for (i in adCounts.indices) adCounts[i] = 0 }
        adCounts[r.ordinal] = adCounts[r.ordinal] + 1
        when (r) {
            AdReward.GEMS -> { gems += Balance.AD_GEMS; showToast("+${Balance.AD_GEMS} gems. Thanks for watching!") }
            AdReward.BOOSTER -> {
                val item = listOf(ShopItem.COCOA, ShopItem.TURBO, ShopItem.TICKET, ShopItem.TANK).random()
                inventory[item.ordinal] = owned(item) + 1
                showToast("${item.title} added to your bag!")
            }
            AdReward.COINS -> { adBoostLeft += Balance.AD_BOOST_SEC; showToast("Coin rush! Coins x2 for 2 minutes") }
        }
        audio.play(R.raw.sfx_buy)
        save()
    }

    /** Old name kept for the footer button. */
    fun watchAdBoost() = watchAd(AdReward.COINS)

    /** Watch an ad to double what the train earned while you were away. */
    fun doubleOfflineWithAd() {
        if (!Balance.ADS_ENABLED || offlineCoins <= 0.0) return
        if (!adReady) { showToast("The ad is still loading, try again in a moment"); return }
        val extra = offlineCoins
        ads.showRewarded {
            coins += extra
            showToast("Doubled! +${fmt(extra)} coins")
            dismissOffline()
            save()
        }
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
            .putString("unl", unlockedUpgrades.joinToString(","))
            .putInt("gems", gems)
            .putString("inv", inventory.joinToString(","))
            .putInt("xp", xp).putInt("plvl", playerLevel)
            .putInt("crates", cratesCollected).putInt("stations", stationsVisited).putInt("storms", stormsSheltered).putInt("bused", boostersUsed).putInt("devinst", devicesInstalled).putString("jq", journeyClaimed.joinToString(","))
            .putLong("qDay", questDay).putString("qBase", questBase.joinToString(",")).putString("qClaim", questClaimed.joinToString(",") { if (it) "1" else "0" })
            .putLong("adDay", adDay)
            .putString("adCnt", adCounts.joinToString(","))
            .putLong("gemDay", gemTradeDay)
            .putInt("gemTraded", gemsTradedToday)
            .putFloat("shopInc", shopIncomeLeft.toFloat())
            .putFloat("shopSpd", shopSpeedLeft.toFloat())
            .putFloat("shopTix", shopTicketLeft.toFloat())
            .putFloat("shopFuel", shopFuelLeft.toFloat())
            .putFloat("rush5", rushLeft[0].toFloat()).putFloat("rush10", rushLeft[1].toFloat()).putFloat("rush100", rushLeft[2].toFloat())
            .putInt("pax", passengers).putFloat("paxMult", paxMult.toFloat())
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
        unlockedUpgrades.clear()
        prefs.getString("unl", "")?.split(",")?.filter { it.isNotEmpty() }?.let { unlockedUpgrades.addAll(it) }
        unlocked.clear()
        prefs.getString("ach", "")?.split(",")?.filter { it.isNotEmpty() }?.let { unlocked.addAll(it) }

        val last = prefs.getLong("last", 0L)
        if (last > 0L) {
            val away = ((System.currentTimeMillis() - last) / 1000).coerceAtLeast(0L)
            val secs = away.coerceAtMost(offlineCapSec)
            offlineAwaySec = away
            offlineCountedSec = secs
            val meters = cruiseSpeed * secs * offlineRate
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
        checkUnlocks()
        refreshQuestDay()
        tickEvents(dt)
        if (adBoostLeft > 0.0) adBoostLeft = max(0.0, adBoostLeft - dt)
        // a soft train horn now and then
        hornIn -= dt
        if (hornIn <= 0.0) {
            hornIn = 70.0 + Math.random() * 90.0
            audio.play(R.raw.sfx_horn, 0.85f)
        }
        tickAmbience(dt)
        if (shopIncomeLeft > 0.0) shopIncomeLeft = max(0.0, shopIncomeLeft - dt)
        if (shopSpeedLeft > 0.0) shopSpeedLeft = max(0.0, shopSpeedLeft - dt)
        if (shopTicketLeft > 0.0) shopTicketLeft = max(0.0, shopTicketLeft - dt)
        for (i in rushLeft.indices) if (rushLeft[i] > 0.0) rushLeft[i] = max(0.0, rushLeft[i] - dt)
        if (shopFuelLeft > 0.0) { shopFuelLeft = max(0.0, shopFuelLeft - dt); boostMeter = 1.0; boostLocked = false }
        if (boostTimeLeft > 0.0) boostTimeLeft = max(0.0, boostTimeLeft - dt)
        if (boostMeter < 1.0) boostMeter = min(1.0, boostMeter + rechargePerSec * dt)
        if (boostLocked && boostMeter >= 1.0) boostLocked = false
    }

    private fun startLoop() {
        viewModelScope.launch {
            var lastTick = System.nanoTime()
            var sinceSave = 0.0
            var sinceCloud = 0.0
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
                sinceCloud += dt
                if (sinceCloud >= 60.0) {
                    cloudSaveNow()
                    sinceCloud = 0.0
                }
            }
        }
    }
}
