package com.chetna.frostexpress

/**
 * Progression helpers: features that open with the player level, what every evolve unlocks,
 * the run bonus picked after evolving, today's special and the "next goal" shown in the header.
 */

/** Side features open one by one so a new player is not flooded with buttons. */
enum class Feature(val title: String, val desc: String, val level: Int) {
    WHEEL("Lucky Wheel", "Spin it once a day for free. Win gems, boosters and coins.", 2),
    EVENTS("Events & Frost Pass", "A new event every week. Earn stars for Frost Pass rewards and snowflakes for the event shop.", 3),
    JOURNEY("Journey", "Ride the 30-station loop of each country, then prestige onward to the next land for +15% coins forever.", 4),
}

/** A bonus picked after evolving; it lasts until the next evolve. */
enum class RouteBonus(val title: String, val desc: String) {
    GOLD_LINE("Gold Line", "+40% coins"),
    FAST_TRACK("Fast Track", "+25% speed"),
    LONG_BOOST("Long Boost", "Boosts last 50% longer"),
    BUSY_STATIONS("Busy Stations", "Stations pay double"),
    CHEAP_PARTS("Cheap Parts", "Upgrades cost 20% less"),
    NIGHT_SHIFT("Night Shift", "+50% offline earnings"),
}

/** One special rule per day, so every day feels a little different. */
enum class DailySpecial(val title: String, val desc: String) {
    COIN_DAY("Coin Day", "+25% coins all day"),
    BUSY_DAY("Busy Platforms", "Stations pay +50%"),
    BOOST_DAY("Boost Day", "Boosts last 50% longer"),
    XP_DAY("Double XP", "All XP is doubled"),
    CRATE_DAY("Crate Day", "Supply crates give double coins"),
    TAILWIND("Tailwind", "+15% speed"),
}

fun todaysSpecial(): DailySpecial {
    val all = DailySpecial.values()
    return all[(java.time.LocalDate.now().toEpochDay() % all.size).toInt()]
}

/** What reaching a train tier unlocks. */
class EvolvePerk(val title: String, val desc: String, val milestone: Boolean = false)

const val PERK_ROUTE = 1
const val PERK_AUTOBUY = 2
const val PERK_LONG_NIGHTS = 3
const val PERK_MORE_ROUTES = 4
const val LONG_NIGHTS_SEC = 2 * 3600L
const val MILESTONE_INCOME = 0.10
const val BADGE_INCOME = 0.03

fun isMilestone(evo: Int): Boolean = evo > 0 && (evo % 5 == 0 || evo == TIERS.lastIndex)
fun milestoneGems(evo: Int): Int = 20 + 5 * evo

fun evolvePerk(evo: Int): EvolvePerk = when {
    evo == PERK_ROUTE -> EvolvePerk("Route Choice", "After every evolve, pick one of three bonuses for the run")
    evo == PERK_AUTOBUY -> EvolvePerk("Auto-Buyer", "Buys the cheapest upgrade for you every few seconds")
    evo == PERK_LONG_NIGHTS -> EvolvePerk("Long Nights", "Offline earnings count 2 hours longer")
    evo == PERK_MORE_ROUTES -> EvolvePerk("More Routes", "Pick from four route bonuses instead of three")
    isMilestone(evo) -> EvolvePerk("Milestone train", "+10% coins forever and ${milestoneGems(evo)} gems", milestone = true)
    else -> EvolvePerk("Train Badge", "+3% coins forever")
}

/** Permanent coin bonus from every milestone and badge reached so far. */
fun perkIncome(evolutions: Int): Double {
    var m = 1.0
    for (e in 1..evolutions) {
        if (e in PERK_ROUTE..PERK_MORE_ROUTES) continue
        m += if (isMilestone(e)) MILESTONE_INCOME else BADGE_INCOME
    }
    return m
}

/** The one thing the header suggests doing next; tapping it opens [sheet]. */
class Goal(val text: String, val progress: Float, val sheet: Int)
