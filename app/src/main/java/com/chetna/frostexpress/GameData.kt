package com.chetna.frostexpress

import java.util.Locale
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** Train tiers. Each evolve unlocks the next tier. */
data class TrainTier(val name: String, val body: Long, val accent: Long)

val TIERS = listOf(
    TrainTier("Snow Pup", 0xFFD95D39, 0xFFF0A202),
    TrainTier("Frost Runner", 0xFF2E86AB, 0xFFF6F5AE),
    TrainTier("Ice Breaker", 0xFF6C4AB6, 0xFFFFD166),
    TrainTier("Aurora Express", 0xFF1B998B, 0xFFFFFFFF),
    TrainTier("Cryo Bullet", 0xFFE63946, 0xFFF1FAEE),
    TrainTier("Eternal Engine", 0xFF222831, 0xFFFFC83D),
)

/** Drawing family used for an upgrade's device picture. */
enum class Gadget { GEAR, WHEEL, FLAME, LAMP, STEAM, CRATE, FAN, COIL, BELL, TANK, DIAL, SHIELD, COIN, CLOCK, ANTENNA, CUP, PLOW, COALPILE, ENGINE, BED }

/** What a generic (non-core) upgrade improves. */
enum class Eff { SPEED, INCOME, RECHARGE, BOOSTMULT, BOOSTLEN, OFFLINE, STATION, CRATE, STORM, NIGHT, AURORA }

private val EXTRA_DEFS = listOf(
    // id, title, gadget, effect, per level, max level. The order is the unlock order AND the car layout (5 per car).
    Triple("STEAM_WHISTLE", "Steam Whistle", Gadget.BELL) to Triple(Eff.INCOME, 0.02, 12),
    Triple("HEADLAMP", "Headlamp", Gadget.LAMP) to Triple(Eff.NIGHT, 0.05, 12),
    Triple("SNOW_CHAINS", "Snow Chains", Gadget.WHEEL) to Triple(Eff.SPEED, 0.03, 12),
    Triple("WATER_TANK", "Water Tank", Gadget.TANK) to Triple(Eff.RECHARGE, 0.04, 12),
    Triple("COAL_TENDER", "Coal Tender", Gadget.COALPILE) to Triple(Eff.INCOME, 0.03, 12),
    Triple("PRESSURE_GAUGE", "Pressure Gauge", Gadget.DIAL) to Triple(Eff.SPEED, 0.02, 12),
    Triple("ROOF_FAN", "Roof Fan", Gadget.FAN) to Triple(Eff.INCOME, 0.02, 12),
    Triple("BRASS_PISTONS", "Brass Pistons", Gadget.GEAR) to Triple(Eff.SPEED, 0.03, 12),
    Triple("TICKET_OFFICE", "Ticket Office", Gadget.COIN) to Triple(Eff.INCOME, 0.03, 12),
    Triple("SAMOVAR", "Samovar", Gadget.CUP) to Triple(Eff.INCOME, 0.02, 12),
    Triple("FROST_SHIELD", "Frost Shield", Gadget.SHIELD) to Triple(Eff.STORM, 0.05, 12),
    Triple("RADIO_MAST", "Radio Mast", Gadget.ANTENNA) to Triple(Eff.STATION, 0.06, 12),
    Triple("SPARK_COIL", "Spark Coil", Gadget.COIL) to Triple(Eff.BOOSTMULT, 0.05, 12),
    Triple("FLYWHEEL", "Flywheel", Gadget.WHEEL) to Triple(Eff.SPEED, 0.03, 12),
    Triple("STEAM_DOME", "Steam Dome", Gadget.STEAM) to Triple(Eff.SPEED, 0.03, 12),
    Triple("SPARE_CRATES", "Spare Crates", Gadget.CRATE) to Triple(Eff.CRATE, 0.06, 12),
    Triple("POCKET_WATCH", "Pocket Watch", Gadget.CLOCK) to Triple(Eff.BOOSTLEN, 0.15, 10),
    Triple("WIND_DEFLECTOR", "Wind Deflector", Gadget.PLOW) to Triple(Eff.SPEED, 0.02, 12),
    Triple("INSULATION", "Insulation Layer", Gadget.SHIELD) to Triple(Eff.INCOME, 0.02, 12),
    Triple("STOVE_PIPE", "Stove Pipe", Gadget.STEAM) to Triple(Eff.INCOME, 0.03, 12),
    Triple("CARGO_NET", "Cargo Net", Gadget.CRATE) to Triple(Eff.INCOME, 0.02, 12),
    Triple("LANTERN_RACK", "Lantern Rack", Gadget.LAMP) to Triple(Eff.NIGHT, 0.06, 12),
    Triple("COOLING_FAN", "Cooling Fan", Gadget.FAN) to Triple(Eff.RECHARGE, 0.04, 12),
    Triple("BOILER_RIVETS", "Boiler Rivets", Gadget.GEAR) to Triple(Eff.SPEED, 0.02, 12),
    Triple("WOOL_BLANKETS", "Wool Blankets", Gadget.BED) to Triple(Eff.OFFLINE, 0.02, 10),
    Triple("TEA_BREWER", "Tea Brewer", Gadget.CUP) to Triple(Eff.OFFLINE, 0.02, 10),
    Triple("SIGNAL_BELL", "Signal Bell", Gadget.BELL) to Triple(Eff.STATION, 0.05, 12),
    Triple("GOLD_COUPLER", "Gold Coupler", Gadget.COIN) to Triple(Eff.INCOME, 0.03, 12),
    Triple("TURBO_VALVE", "Turbo Valve", Gadget.STEAM) to Triple(Eff.BOOSTMULT, 0.05, 12),
    Triple("ICE_CUTTER", "Ice Cutter", Gadget.PLOW) to Triple(Eff.SPEED, 0.03, 12),
    Triple("GREASE_PUMP", "Grease Pump", Gadget.TANK) to Triple(Eff.SPEED, 0.02, 12),
    Triple("FROST_RADAR", "Frost Radar", Gadget.ANTENNA) to Triple(Eff.STORM, 0.05, 12),
    Triple("FUEL_INJECTOR", "Fuel Injector", Gadget.FLAME) to Triple(Eff.RECHARGE, 0.04, 12),
    Triple("CABIN_HEATER", "Cabin Heater", Gadget.FLAME) to Triple(Eff.INCOME, 0.03, 12),
    Triple("CARGO_CRANE", "Cargo Crane", Gadget.CRATE) to Triple(Eff.CRATE, 0.08, 12),
    Triple("LOUNGE", "Passenger Lounge", Gadget.CUP) to Triple(Eff.INCOME, 0.03, 12),
    Triple("NIGHT_WATCH", "Night Watch", Gadget.CLOCK) to Triple(Eff.NIGHT, 0.06, 12),
    Triple("BANK_SAFE", "Bank Safe", Gadget.COIN) to Triple(Eff.OFFLINE, 0.02, 10),
    Triple("COMPASS", "Compass", Gadget.DIAL) to Triple(Eff.STATION, 0.04, 12),
    Triple("GOVERNOR", "Governor", Gadget.GEAR) to Triple(Eff.SPEED, 0.03, 12),
    Triple("MAGNET_COUPLER", "Magnet Coupler", Gadget.COIL) to Triple(Eff.INCOME, 0.03, 12),
    Triple("TIMETABLE", "Timetable Board", Gadget.CLOCK) to Triple(Eff.STATION, 0.05, 12),
    Triple("AURORA_PRISM", "Aurora Prism", Gadget.COIL) to Triple(Eff.AURORA, 0.05, 10),
    Triple("TRACTION_SAND", "Traction Sand", Gadget.WHEEL) to Triple(Eff.SPEED, 0.02, 12),
    Triple("CHEFS_OVEN", "Chef's Oven", Gadget.FLAME) to Triple(Eff.INCOME, 0.03, 12),
    Triple("BOGIE_SPRINGS", "Bogie Springs", Gadget.GEAR) to Triple(Eff.SPEED, 0.02, 12),
    Triple("POLAR_BEACON", "Polar Beacon", Gadget.LAMP) to Triple(Eff.BOOSTLEN, 0.15, 10),
)

enum class UpgradeType { ENGINE, WAGON, HEAT, FOOD, PLOW, COAL, BOOSTER, CONDUCTOR,
    SNOW_CHAINS, BRASS_PISTONS, STEAM_WHISTLE, HEADLAMP, WATER_TANK, PRESSURE_GAUGE, COAL_TENDER, FROST_SHIELD,
    TICKET_OFFICE, SAMOVAR, ROOF_FAN, RADIO_MAST, POCKET_WATCH, SPARK_COIL, FLYWHEEL, STEAM_DOME,
    WIND_DEFLECTOR, SPARE_CRATES, INSULATION, STOVE_PIPE, LANTERN_RACK, COOLING_FAN, SIGNAL_BELL, CARGO_NET,
    BOILER_RIVETS, WOOL_BLANKETS, TEA_BREWER, TIMETABLE, GOLD_COUPLER, AURORA_PRISM, FROST_RADAR, NIGHT_WATCH,
    TURBO_VALVE, ICE_CUTTER, GREASE_PUMP, FUEL_INJECTOR, CABIN_HEATER, CARGO_CRANE, LOUNGE, BANK_SAFE,
    COMPASS, GOVERNOR, MAGNET_COUPLER, POLAR_BEACON,
    TRACTION_SAND, CHEFS_OVEN, BOGIE_SPRINGS }

data class UpgradeSpec(
    val type: UpgradeType,
    val title: String,
    val effect: String,
    val baseCost: Double,
    val growth: Double,
    val maxLevel: Int = Int.MAX_VALUE,
    /** This upgrade unlocks once [unlockType] reaches [unlockLevel] (null = available from the start). */
    val unlockType: UpgradeType? = null,
    val unlockLevel: Int = 0,
    val gadget: Gadget = Gadget.GEAR,
    /** Generic upgrades: what they improve and by how much per level (null = one of the core eight). */
    val eff: Eff? = null,
    val per: Double = 0.0,
)

fun effText(e: Eff, per: Double): String {
    val pct = (per * 100).let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format(Locale.US, "%.0f", it) }
    return when (e) {
        Eff.SPEED -> "+$pct% train speed"
        Eff.INCOME -> "+$pct% coins"
        Eff.RECHARGE -> "+$pct% faster boost recharge"
        Eff.BOOSTMULT -> "+${String.format(Locale.US, "%.2f", per)} boost multiplier"
        Eff.BOOSTLEN -> "+${String.format(Locale.US, "%.2f", per)} s of boost per tap"
        Eff.OFFLINE -> "+$pct% offline earnings"
        Eff.STATION -> "+$pct% station bonus"
        Eff.CRATE -> "+$pct% supply crate reward"
        Eff.STORM -> "-$pct% of storm slowdown"
        Eff.NIGHT -> "+$pct% coins at night"
        Eff.AURORA -> "+$pct% aurora bonus"
    }
}

/** To add an upgrade: add an UpgradeType, an entry here, and its formula in GameViewModel. */
private val CORE_UPGRADES = listOf(
    UpgradeSpec(UpgradeType.ENGINE, "Engine Power", "+0.5 m/s speed", 20.0, 1.25, gadget = Gadget.ENGINE),
    UpgradeSpec(UpgradeType.WAGON, "Cargo Wagon", "+30% coins per metre", 50.0, 1.7, 40, UpgradeType.ENGINE, 5, gadget = Gadget.CRATE),
    UpgradeSpec(UpgradeType.HEAT, "Heat Generator", "+6% income, warm passengers pay more", 150.0, 1.45, Int.MAX_VALUE, UpgradeType.WAGON, 3, gadget = Gadget.FLAME),
    UpgradeSpec(UpgradeType.FOOD, "Food Car", "+5% income", 400.0, 1.5, Int.MAX_VALUE, UpgradeType.HEAT, 5, gadget = Gadget.CUP),
    UpgradeSpec(UpgradeType.PLOW, "Ice Plow", "+5% speed", 600.0, 1.35, 60, UpgradeType.FOOD, 3, gadget = Gadget.PLOW),
    UpgradeSpec(UpgradeType.COAL, "Coal Stoker", "+8% faster boost recharge", 300.0, 1.30, 30, UpgradeType.PLOW, 5, gadget = Gadget.COALPILE),
    UpgradeSpec(UpgradeType.BOOSTER, "Boost Core", "+0.2 boost multiplier", 800.0, 1.40, 20, UpgradeType.COAL, 5, gadget = Gadget.COIL),
    UpgradeSpec(UpgradeType.CONDUCTOR, "Night Conductor", "+5% offline earnings", 1000.0, 1.40, 15, UpgradeType.BOOSTER, 3, gadget = Gadget.BED),
)

private val EXTRA_UPGRADES: List<UpgradeSpec> by lazy {
    var prev = UpgradeType.CONDUCTOR
    EXTRA_DEFS.mapIndexed { i, (a, b) ->
        val (id, title, gadget) = a
        val (eff, per, max) = b
        val type = UpgradeType.valueOf(id)
        val spec = UpgradeSpec(type, title, effText(eff, per), Math.floor(2500.0 * Math.pow(1.55, i.toDouble())), 1.55, minOf(max, 10), prev, 3, gadget, eff, per)
        prev = type
        spec
    }
}

val UPGRADES: List<UpgradeSpec> = CORE_UPGRADES + EXTRA_UPGRADES

/** Five upgrades per car: car 0 is the engine car, car 1 the utility car, every later car holds five more devices. */
val CAR_LAYOUT: List<List<UpgradeType>> by lazy {
    val extras = EXTRA_UPGRADES.map { it.type }
    listOf(
        listOf(UpgradeType.ENGINE, UpgradeType.COAL, UpgradeType.BOOSTER, UpgradeType.CONDUCTOR, UpgradeType.PLOW),
        listOf(UpgradeType.HEAT, UpgradeType.FOOD, UpgradeType.WAGON, extras[0], extras[1]),
    ) + extras.drop(2).chunked(5)
}

/** Which car an upgrade's device sits in. */
fun carOf(t: UpgradeType): Int = CAR_LAYOUT.indexOfFirst { t in it }.coerceAtLeast(0)

/** Shop boosters, bought with coins. Prices scale with your steady income ([costSec] seconds of it). */
enum class ShopItem(val title: String, val desc: String, val costSec: Double, val durationSec: Double) {
    COCOA("Hot Cocoa Rush", "Coins x2 for 5 minutes", 120.0, 300.0),
    TURBO("Turbo Coal", "Train speed x1.5 for 5 minutes", 100.0, 300.0),
    TICKET("Golden Ticket", "Stations pay x3 for 10 minutes", 90.0, 600.0),
    TANK("Full Boost Tank", "Refill the boost meter right now", 25.0, 0.0),
    CLEAR("Storm Breaker", "End the current snow storm", 40.0, 0.0),
}

/** Permanent upgrades: bought with Frost Cores (earned by evolving) and kept forever, even after evolving. */
enum class PermType { INCOME, SPEED, START, OFFLINE, BOOST, CREW }

data class PermSpec(
    val type: PermType,
    val title: String,
    val effect: String,
    val baseCost: Int,
    val growth: Double,
    val maxLevel: Int,
)

val PERMANENT = listOf(
    PermSpec(PermType.INCOME, "Eternal Furnace", "+20% coins from distance", 1, 1.7, 30),
    PermSpec(PermType.SPEED, "Permafrost Rails", "+10% train speed", 2, 1.7, 30),
    PermSpec(PermType.START, "Head Start", "Start each run with more coins", 2, 1.8, 10),
    PermSpec(PermType.BOOST, "Deep Freeze Core", "+0.25 boost multiplier", 3, 1.8, 8),
    PermSpec(PermType.OFFLINE, "Hibernation", "+5% offline earnings", 3, 1.8, 10),
    PermSpec(PermType.CREW, "Second Worker", "A second worker joins: +25% coins", 4, 1.0, 1),
)

/** Achievements. Reward: Frost Cores if [cores] > 0, otherwise a bundle of coins. */
data class Achievement(val id: String, val title: String, val desc: String, val cores: Int = 0)

val ACHIEVEMENTS = listOf(
    Achievement("steps", "First Steps", "Travel 100 m"),
    Achievement("km1", "One Kilometre", "Travel 1 km in total"),
    Achievement("km100", "Long Haul", "Travel 100 km in total", cores = 1),
    Achievement("upg1", "Tinkerer", "Buy your first upgrade"),
    Achievement("upg50", "Mechanic", "Buy 50 upgrades"),
    Achievement("boost10", "Boost Fan", "Tap BOOST 10 times"),
    Achievement("boost200", "Turbo Finger", "Tap BOOST 200 times"),
    Achievement("wagon5", "Heavy Load", "Own 5 cargo wagons"),
    Achievement("heat10", "Warm and Cosy", "Heat Generator level 10"),
    Achievement("rich", "Millionaire", "Hold 1M coins"),
    Achievement("streak3", "Regular", "Reach a 3 day login streak"),
    Achievement("evo1", "Evolution", "Evolve for the first time", cores = 1),
    Achievement("evo3", "Frost Veteran", "Evolve 3 times", cores = 2),
)

/** Train liveries (paint jobs). Livery i unlocks after i evolutions. Index 0 uses the tier's own colors. */
data class Livery(val name: String, val body: Long, val accent: Long)

val LIVERIES = listOf(
    Livery("Classic", 0, 0),
    Livery("Sunset", 0xFFE8590C, 0xFFFFD43B),
    Livery("Midnight", 0xFF1C2541, 0xFF5BC0BE),
    Livery("Aurora", 0xFF1B998B, 0xFFC3F0CA),
    Livery("Rose", 0xFFD6336C, 0xFFFFE3EC),
)

/** Random events that happen while the train runs. */
enum class GameEvent { NONE, AURORA, CRATE }

object Balance {
    const val BASE_SPEED = 2.0            // metres per second
    const val SPEED_PER_ENGINE = 0.5
    const val PLOW_BONUS = 0.05
    const val WAGON_BONUS = 0.3
    const val HEAT_BONUS = 0.06
    const val FOOD_BONUS = 0.05
    const val TIER_SPEED_BONUS = 0.2      // +20% speed per tier
    const val TIER_INCOME = 1.6           // coins x1.6 per tier
    val MILESTONES = listOf(25, 50, 100) // each doubles that upgrade's effect
    const val CORE_BONUS = 0.10           // each core = +10% coins (permanent)

    /** Evolve needs this many upgrade levels bought in the current run (grows with every evolve). */
    fun evolveLevels(evolutions: Int): Int = 100 + 120 * evolutions + 10 * evolutions * (evolutions - 1)
    /** Cars (5 upgrades each) available on a train tier: 4 at first, 2 more per evolve. */
    fun maxCars(evolutions: Int): Int = minOf(4 + 2 * evolutions, 11)
    const val SHOP_MAX_SEC = 1800.0       // timed shop boosters stack up to 30 minutes
    const val STATION_STOP_SEC = 8.0      // storm shelter: the train waits this long at the station

    const val EVOLVE_BASE_DISTANCE = 20000.0 // metres for the first evolve (about 15 to 25 minutes), x4 after each evolve
    const val EVOLVE_GROWTH = 4.0

    // Permanent upgrades
    const val PERM_INCOME = 0.20
    const val PERM_SPEED = 0.10
    const val PERM_BOOST = 0.25
    const val PERM_OFFLINE = 0.05
    const val PERM_CREW = 0.25
    const val START_COINS_BASE = 50.0     // Head Start: 50 x 3^level coins
    const val START_COINS_GROWTH = 3.0

    // Random events and stations
    const val EVENT_MIN_GAP = 80.0        // seconds between events: 80..160
    const val EVENT_MAX_GAP = 160.0
    const val STORM_MIN_GAP = 100.0       // snow storms come on their own timer: every 100..240 s
    const val STORM_MAX_GAP = 240.0
    const val STORM_MIN_SEC = 20.0        // and last 20..40 s
    const val STORM_MAX_SEC = 40.0
    const val STORM_FADE_SEC = 3.0
    // Day and night cycle (seconds)
    const val DAY_SEC = 70.0
    const val DUSK_SEC = 20.0
    const val NIGHT_SEC = 40.0
    const val DAWN_SEC = 20.0
    const val STORM_SPEED = 0.75
    const val AURORA_SEC = 30.0
    const val AURORA_INCOME = 1.5
    const val CRATE_SEC = 12.0
    const val STATION_EVERY_M = 400.0     // a station passes every 400 m
    const val STATION_BONUS_SEC = 45.0    // bonus = 45 seconds of income

    // Boost
    const val BOOST_BASE_MULT = 2.0
    const val BOOST_MULT_PER_LEVEL = 0.2
    const val BOOST_COST_PER_TAP = 0.1      // 10% of the meter per tap
    const val BOOST_SEC_PER_TAP = 3.0
    const val BOOST_MAX_SEC = 10.0
    const val BOOST_RECHARGE_PER_SEC = 0.04 // empty to full in about 25 sec
    const val COAL_BONUS = 0.08

    const val IAP_ENABLED = false
    const val PLAY_GAMES_ENABLED = false  // set to true after the Play Games setup in the README

    // Rewarded ad boost (hidden while ADS_ENABLED is false)
    const val ADS_ENABLED = false
    const val AD_BOOST_MULT = 2.0
    const val AD_BOOST_SEC = 120.0

    // Offline
    const val OFFLINE_CAP_SEC = 8 * 3600L
    const val OFFLINE_BASE = 0.25
    const val CONDUCTOR_BONUS = 0.05
}

private val SUFFIXES = listOf("", "K", "M", "B", "T", "Qa", "Qi", "Sx", "Sp", "Oc", "No", "Dc")

fun fmt(v: Double, decimals: Int = 0): String {
    if (v < 1000.0) return String.format(Locale.US, "%.${decimals}f", v)
    val idx = (floor(log10(v)) / 3).toInt().coerceAtMost(SUFFIXES.size - 1)
    val scaled = v / 10.0.pow(idx * 3)
    return String.format(Locale.US, "%.2f%s", scaled, SUFFIXES[idx])
}

fun fmtDistance(meters: Double): String =
    if (meters < 1000.0) String.format(Locale.US, "%.0f m", meters) else fmt(meters / 1000.0) + " km"
