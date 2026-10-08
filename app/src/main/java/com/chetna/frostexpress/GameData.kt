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

enum class UpgradeType { ENGINE, WAGON, HEAT, FOOD, PLOW, COAL, BOOSTER, CONDUCTOR }

data class UpgradeSpec(
    val type: UpgradeType,
    val title: String,
    val effect: String,
    val baseCost: Double,
    val growth: Double,
    val maxLevel: Int = Int.MAX_VALUE,
)

/** To add an upgrade: add an UpgradeType, an entry here, and its formula in GameViewModel. */
val UPGRADES = listOf(
    UpgradeSpec(UpgradeType.ENGINE, "Engine Power", "+0.5 m/s speed", 20.0, 1.18),
    UpgradeSpec(UpgradeType.WAGON, "Cargo Wagon", "+60% coins per metre", 50.0, 1.45, 40),
    UpgradeSpec(UpgradeType.HEAT, "Heat Generator", "+8% income, warm passengers pay more", 150.0, 1.30),
    UpgradeSpec(UpgradeType.FOOD, "Food Car", "+6% income", 400.0, 1.32),
    UpgradeSpec(UpgradeType.PLOW, "Ice Plow", "+5% speed", 600.0, 1.35, 60),
    UpgradeSpec(UpgradeType.COAL, "Coal Stoker", "+8% faster boost recharge", 300.0, 1.30, 30),
    UpgradeSpec(UpgradeType.BOOSTER, "Boost Core", "+0.2 boost multiplier", 800.0, 1.40, 20),
    UpgradeSpec(UpgradeType.CONDUCTOR, "Night Conductor", "+5% offline earnings", 1000.0, 1.40, 15),
)

/** Permanent upgrades: bought with Frost Cores (earned by evolving) and kept forever, even after evolving. */
enum class PermType { INCOME, SPEED, START, OFFLINE, BOOST }

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

object Balance {
    const val BASE_SPEED = 2.0            // metres per second
    const val SPEED_PER_ENGINE = 0.5
    const val PLOW_BONUS = 0.05
    const val WAGON_BONUS = 0.6
    const val HEAT_BONUS = 0.08
    const val FOOD_BONUS = 0.06
    const val TIER_SPEED_BONUS = 0.2      // +20% speed per tier
    const val TIER_INCOME = 3.0           // coins x3 per tier
    val MILESTONES = listOf(25, 50, 100, 200, 300) // each doubles that upgrade's effect
    const val CORE_BONUS = 0.25           // each core = +25% coins (permanent)

    const val EVOLVE_BASE_DISTANCE = 3000.0 // metres, x4 after each evolve
    const val EVOLVE_GROWTH = 4.0

    // Permanent upgrades
    const val PERM_INCOME = 0.20
    const val PERM_SPEED = 0.10
    const val PERM_BOOST = 0.25
    const val PERM_OFFLINE = 0.05
    const val START_COINS_BASE = 50.0     // Head Start: 50 x 3^level coins
    const val START_COINS_GROWTH = 3.0

    // Boost
    const val BOOST_BASE_MULT = 2.0
    const val BOOST_MULT_PER_LEVEL = 0.2
    const val BOOST_COST_PER_TAP = 0.1      // 10% of the meter per tap
    const val BOOST_SEC_PER_TAP = 3.0
    const val BOOST_MAX_SEC = 10.0
    const val BOOST_RECHARGE_PER_SEC = 0.04 // empty to full in about 25 sec
    const val COAL_BONUS = 0.08

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
