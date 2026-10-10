package com.chetna.frostexpress

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.temporal.IsoFields

/*
 * Long-term goals around the idle loop: weekly events with their own currency (Snowflakes) and shop,
 * the monthly Frost Pass, the lucky wheel, the collection album, survivor stories, golden devices,
 * the journey to the Warm City, personal records, friend gift codes and train customisation.
 * Everything here is data plus small, saved state; GameViewModel turns the rewards into coins, gems and items.
 */

// ---------------- rewards ----------------

/** Something the player receives. [secs] coin rewards are worth that many seconds of steady income. */
sealed class Reward {
    data class Coins(val secs: Double) : Reward()
    /** [base] is the design value; players get [n] after the gem-earning rate is applied. */
    data class Gems(val base: Int) : Reward() { val n: Int get() = earnedGems(base) }
    data class Item(val item: ShopItem, val n: Int = 1) : Reward()
    data class Flakes(val n: Int) : Reward()
    data class Polish(val n: Int) : Reward()
    data class Paint(val livery: Int) : Reward()
    data class Passengers(val n: Int) : Reward()
}

fun Reward.label(): String = when (this) {
    is Reward.Coins -> "Coins (${(secs / 60).toInt().coerceAtLeast(1)} min)"
    is Reward.Gems -> "$n gems"
    is Reward.Item -> if (n > 1) "${item.title} x$n" else item.title
    is Reward.Flakes -> "$n snowflakes"
    is Reward.Polish -> if (n > 1) "$n Gold Polish" else "Gold Polish"
    is Reward.Passengers -> "$n passengers"
    is Reward.Paint -> "${LIVERIES.getOrNull(livery)?.name ?: "New"} paint"
}

// ---------------- weekly events ----------------

/** A week-long event. The game changes a little while it runs; snowflakes buy its exclusive paint. */
class WeeklyEvent(val id: Int, val name: String, val desc: String, val color: Long, val livery: Int)

val WEEKLY_EVENTS = listOf(
    WeeklyEvent(0, "Blizzard Week", "Snow storms come twice as often and shelters pay triple. Every storm gives snowflakes.", 0xFF9FC5EA, 5),
    WeeklyEvent(1, "Aurora Festival", "Auroras glow twice as often. Every aurora gives snowflakes.", 0xFFB9A2EE, 6),
    WeeklyEvent(2, "Supply Drop Week", "Supply crates fall twice as often. Every crate gives snowflakes.", 0xFFFFC98A, 7),
    WeeklyEvent(3, "Wolf Moon", "Wolf packs roam the snow. Chase them off for snowflakes.", 0xFF8FA3B5, 8),
)

/** The event that runs this week (ISO weeks, so it changes every Monday). */
fun currentEvent(): WeeklyEvent {
    val d = LocalDate.now()
    val week = d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + d.get(IsoFields.WEEK_BASED_YEAR) * 53
    return WEEKLY_EVENTS[week % WEEKLY_EVENTS.size]
}

fun eventWeekKey(): Int {
    val d = LocalDate.now()
    return d.get(IsoFields.WEEK_BASED_YEAR) * 100 + d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
}

/** Days left in this week's event, like "3d 5h". */
fun eventTimeLeft(): String {
    val now = java.time.LocalDateTime.now()
    val end = now.toLocalDate().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY)).atStartOfDay()
    val m = java.time.Duration.between(now, end).toMinutes()
    return "${m / 1440}d ${(m % 1440) / 60}h"
}

/** The snowflake shop: the week's exclusive paint plus things that are always there. */
class FlakeOffer(val title: String, val cost: Int, val reward: Reward)

fun flakeShop(ev: WeeklyEvent): List<FlakeOffer> = listOf(
    FlakeOffer("${LIVERIES[ev.livery].name} paint (this week only)", 400, Reward.Paint(ev.livery)),
    FlakeOffer("Gold Polish", 250, Reward.Polish(1)),
    FlakeOffer("Gold Rush x5", 160, Reward.Item(ShopItem.RUSH5)),
    FlakeOffer("6 gems", 120, Reward.Gems(15)),
    FlakeOffer("Turbo Coal", 60, Reward.Item(ShopItem.TURBO)),
    FlakeOffer("Hot Cocoa Rush", 60, Reward.Item(ShopItem.COCOA)),
)

// ---------------- Frost Pass (monthly) ----------------

const val PASS_TIERS = 30
const val PASS_STARS_PER_TIER = 100
const val PASS_PREMIUM_GEMS = 1000

fun passSeasonKey(): Int = LocalDate.now().let { it.year * 12 + it.monthValue }
fun passSeasonName(): String = LocalDate.now().month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " Frost Pass"
fun passDaysLeft(): Int = LocalDate.now().let { it.lengthOfMonth() - it.dayOfMonth + 1 }

fun passFree(tier: Int): Reward = when {
    tier == PASS_TIERS -> Reward.Gems(50)
    tier % 10 == 0 -> Reward.Polish(1)
    tier % 5 == 0 -> Reward.Gems(10)
    tier % 3 == 0 -> Reward.Item(listOf(ShopItem.COCOA, ShopItem.TURBO, ShopItem.TICKET)[tier % 3])
    tier % 2 == 0 -> Reward.Flakes(30)
    else -> Reward.Coins(300.0 + tier * 30.0)
}

fun passPremium(tier: Int): Reward = when {
    tier == PASS_TIERS -> Reward.Paint(9)
    tier % 10 == 0 -> Reward.Item(ShopItem.RUSH10)
    tier % 5 == 0 -> Reward.Gems(25)
    tier % 3 == 0 -> Reward.Item(ShopItem.RUSH5)
    tier % 2 == 0 -> Reward.Polish(1)
    else -> Reward.Gems(8)
}

// ---------------- lucky wheel ----------------

class WheelSlice(val reward: Reward, val weight: Int, val color: Long)

val WHEEL = listOf(
    WheelSlice(Reward.Coins(600.0), 22, 0xFFFFE49A),
    WheelSlice(Reward.Gems(5), 18, 0xFFFFC9DE),
    WheelSlice(Reward.Item(ShopItem.TURBO), 14, 0xFFFFD9B8),
    WheelSlice(Reward.Flakes(40), 14, 0xFFCDE7F8),
    WheelSlice(Reward.Coins(1800.0), 12, 0xFFFFF0C8),
    WheelSlice(Reward.Gems(20), 8, 0xFFF49AB2),
    WheelSlice(Reward.Polish(1), 6, 0xFFFFE08A),
    WheelSlice(Reward.Item(ShopItem.RUSH5), 6, 0xFFDCCBFA),
)
const val WHEEL_GEM_SPIN = 10
const val WHEEL_AD_SPINS = 2

fun rollWheel(): Int {
    var r = (0 until WHEEL.sumOf { it.weight }).random()
    for ((i, s) in WHEEL.withIndex()) { if (r < s.weight) return i; r -= s.weight }
    return 0
}

// ---------------- collection album ----------------

/** Six keepsakes hidden in each region; a full set gives a permanent coin bonus. */
val COLLECTIBLE_NAMES = listOf(
    listOf("Pine Cone Charm", "Carved Owl", "Lost Mitten", "Ranger Badge", "Birch Whistle", "Frozen Acorn"),
    listOf("Old Photograph", "Toy Train", "Street Sign Plate", "Shop Bell", "Snow Globe", "Love Letter"),
    listOf("Ice Crystal", "Skate Blade", "Frozen Fish", "Polar Compass", "Sled Bell", "Glacier Pearl"),
    listOf("Brass Gear", "Turbine Bolt", "Worker Helmet", "Old Fuse", "Copper Coil", "Oil Lamp"),
    listOf("Ship in a Bottle", "Anchor Pin", "Seashell", "Captain's Log", "Lighthouse Key", "Sea Glass"),
    listOf("Campfire Mug", "Hand-knit Scarf", "Harmonica", "Survivor Map", "Tin Star", "Warm Socks"),
)
const val SET_BONUS = 0.05 // +5% coins per completed region set

// ---------------- survivor stories ----------------

/** One chapter: a short story beat and a goal counted from when the chapter starts. */
class Chapter(val text: String, val goal: String, val stat: QuestStat, val amount: Int, val reward: Reward)

class Story(val who: String, val chapters: List<Chapter>)

val STORIES = listOf(
    Story("Mira, the mechanic", listOf(
        Chapter("Mira was found asleep under a snowed-in tractor. She wants to fix the boiler valves first.", "Buy 15 upgrade levels", QuestStat.UPGRADES, 15, Reward.Gems(10)),
        Chapter("\"This train needs more hands,\" she says, and asks you to stop at stations so she can trade for parts.", "Stop at 5 stations", QuestStat.STATIONS, 5, Reward.Polish(1)),
        Chapter("She built a tiny radio. It picks up a warm voice from the south: the Warm City is real.", "Travel 10 km", QuestStat.DISTANCE, 10_000, Reward.Gems(25)),
    )),
    Story("Old Tomas", listOf(
        Chapter("Tomas walked out of a buried town with only his violin. He plays for the passengers.", "Get 15 passengers aboard", QuestStat.PASSENGERS, 15, Reward.Gems(10)),
        Chapter("He remembers supply depots along the line. \"Watch the snow for crates,\" he winks.", "Catch 3 supply crates", QuestStat.CRATES, 3, Reward.Item(ShopItem.RUSH5)),
        Chapter("He finally plays a song for his wife's photo, which you found in the snow.", "Find 3 keepsakes", QuestStat.COLLECTIBLES, 3, Reward.Gems(30)),
    )),
    Story("Noor and her dog", listOf(
        Chapter("Noor and her husky Pip were tracking the train for days. Pip hates storms.", "Wait out 2 snow storms", QuestStat.STORMS, 2, Reward.Gems(12)),
        Chapter("Pip howls back at a wolf pack. Noor wants to keep them away from the passengers.", "Chase off 10 wolves", QuestStat.WOLVES, 10, Reward.Polish(1)),
        Chapter("Pip curls up by the boiler every night now. Noor says it is the first home in a year.", "Tap BOOST 100 times", QuestStat.BOOST_TAPS, 100, Reward.Gems(30)),
    )),
    Story("Kai, the scout", listOf(
        Chapter("Kai has a map of the old rail network, half of it washed out by ice.", "Play 3 mini-games", QuestStat.MINIGAMES, 3, Reward.Gems(12)),
        Chapter("He marks safe routes as you pass each region of the frozen world.", "Travel 25 km", QuestStat.DISTANCE, 25_000, Reward.Item(ShopItem.RUSH10)),
        Chapter("Kai pins the last route: straight through to the Warm City. \"We can make it.\"", "Evolve your train once", QuestStat.EVOLVES, 1, Reward.Gems(40)),
    )),
    Story("Ivo, the pilot", listOf(
        Chapter("Ivo crash-landed his little plane in the snow. He still wears his goggles everywhere.", "Travel 15 km", QuestStat.DISTANCE, 15_000, Reward.Gems(15)),
        Chapter("From the roof he spots crates the others miss.", "Catch 4 supply crates", QuestStat.CRATES, 4, Reward.Polish(1)),
        Chapter("He swears he will fly everyone to the Warm City one day. For now, the train is faster.", "Stop at 20 stations", QuestStat.STATIONS, 20, Reward.Gems(35)),
    )),
    Story("Rosa, the cook", listOf(
        Chapter("Rosa ran a soup kitchen in the Buried Town. The galley is hers now.", "Get 25 passengers aboard", QuestStat.PASSENGERS, 25, Reward.Item(ShopItem.COCOA, 2)),
        Chapter("\"A full train is a happy train,\" she says, and asks for more seats.", "Buy 60 upgrade levels", QuestStat.UPGRADES, 60, Reward.Gems(20)),
        Chapter("Her stew brings strangers to the platform just for the smell.", "Get 60 passengers aboard", QuestStat.PASSENGERS, 60, Reward.Gems(35)),
    )),
    Story("Ben, the teacher", listOf(
        Chapter("Ben teaches the children letters on the frosted windows.", "Play 5 mini-games", QuestStat.MINIGAMES, 5, Reward.Gems(15)),
        Chapter("He starts a little museum of the old world in the luggage rack.", "Find 6 keepsakes", QuestStat.COLLECTIBLES, 6, Reward.Polish(1)),
        Chapter("The children's drawings now cover a whole car. They draw the Warm City in green.", "Find 12 keepsakes", QuestStat.COLLECTIBLES, 12, Reward.Gems(40)),
    )),
    Story("Lena, the skater", listOf(
        Chapter("Lena skated across the Ice Fields to catch the train. Nobody knows how.", "Wait out 3 snow storms", QuestStat.STORMS, 3, Reward.Gems(15)),
        Chapter("She races the wolves along the line and laughs about it.", "Chase off 20 wolves", QuestStat.WOLVES, 20, Reward.Item(ShopItem.RUSH5)),
        Chapter("She teaches Pip to skate. Pip does not enjoy it.", "Tap BOOST 300 times", QuestStat.BOOST_TAPS, 300, Reward.Gems(35)),
    )),
    Story("Omar, the sailor", listOf(
        Chapter("Omar's ship froze in the harbour years ago. He tells stories of warm seas.", "Travel 40 km", QuestStat.DISTANCE, 40_000, Reward.Gems(20)),
        Chapter("He reads the stars at night and keeps the train on course.", "Stop at 40 stations", QuestStat.STATIONS, 40, Reward.Polish(1)),
        Chapter("\"Land ho!\" he shouts at every station, just for fun.", "Evolve your train once", QuestStat.EVOLVES, 1, Reward.Gems(45)),
    )),
    Story("Little Yuki", listOf(
        Chapter("Yuki was found asleep in the Survivor Camps, hugging a toy bear.", "Install 5 devices", QuestStat.DEVICES, 5, Reward.Gems(15)),
        Chapter("She names every device on the train. The boiler is called Mr Toasty.", "Buy 100 upgrade levels", QuestStat.UPGRADES, 100, Reward.Item(ShopItem.RUSH10)),
        Chapter("She asks if the Warm City has snow too. Just a little, everyone hopes.", "Travel 80 km", QuestStat.DISTANCE, 80_000, Reward.Gems(50)),
    )),
)

// ---------------- the journey to the Warm City ----------------

/** Milestones along the line (total km travelled), the last one is the Warm City. */
class Leg(val name: String, val km: Int, val gemsBase: Int) { val gems: Int get() = earnedGems(gemsBase) }

val JOURNEY = listOf(
    Leg("Frostbite Junction", 1, 5), Leg("Pinewatch", 3, 5), Leg("Old Mill Halt", 8, 8), Leg("Icebridge", 20, 10),
    Leg("Lantern Town", 50, 12), Leg("Windmere", 100, 15), Leg("Harbour Light", 200, 20), Leg("Camp Ember", 400, 25),
    Leg("Glacier Gate", 700, 30), Leg("Southern Pass", 1000, 40), Leg("Thaw Valley", 1500, 50), Leg("The Warm City", 2000, 150),
    // Chapter 2: the Warm City asks the train to go back north and bring everyone else home
    Leg("Snowline Depot", 3000, 60), Leg("The Old Capital", 4500, 70), Leg("Frozen Sea Rim", 6500, 80),
    Leg("Aurora Peak", 9000, 100), Leg("The Last Depot", 12000, 120), Leg("Home Station", 16000, 300),
)
/** Index of the Warm City in [JOURNEY]; reaching it ends chapter 1. */
const val WARM_CITY = 11
/** Coins bonus forever after reaching the Warm City. */
const val WARM_CITY_BONUS = 0.25
/** Google Play product for the premium Frost Pass (one month). */
const val PASS_PRODUCT = "frost_pass_premium"

// ---------------- customisation ----------------

class Horn(val name: String, val res: Int, val gems: Int)

val HORNS = listOf(
    Horn("Soft whistle", R.raw.sfx_horn, 0),
    Horn("Deep diesel", R.raw.sfx_horn2, 30),
    Horn("Steam chorus", R.raw.sfx_horn3, 60),
)

/** Decorations inside the cars, bought once and shown in every car. */
enum class CarDecor(val title: String, val desc: String, val gems: Int) {
    LIGHTS("String Lights", "Warm bulbs along the walls, lovely at night", 25),
    RUG("Aisle Rug", "A cosy runner down the middle of each car", 20),
    LANTERNS("Window Lanterns", "Little lanterns that glow in every window", 30),
}

// ---------------- friend gift codes ----------------

/**
 * Offline gift codes: a code carries the sender's id and the day, signed so it cannot be made up.
 * Each code works once per phone, not on the phone that made it, and only on the day it was made or the next.
 */
object GiftCodes {
    private const val KEY = "frost-express-gift-v1"
    private const val ALPHA = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private fun sig(s: String): String {
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(KEY.toByteArray(), "HmacSHA256"))
        val b = mac.doFinal(s.toByteArray())
        return (0 until 4).map { ALPHA[(b[it].toInt() and 0xFF) % ALPHA.length] }.joinToString("")
    }

    /** A code like FROST-7KQ2-1844-ABCD: player id, day number, signature. */
    fun make(playerId: String, day: Long): String {
        val d = (day % 10000).toString().padStart(4, '0')
        return "FROST-$playerId-$d-${sig("$playerId$d")}"
    }

    /** Returns the sender's id when the code is genuine and recent, else null. */
    fun check(code: String, today: Long): String? {
        val parts = code.trim().uppercase().split("-")
        if (parts.size != 4 || parts[0] != "FROST") return null
        val (_, id, d, s) = parts
        if (sig("$id$d") != s) return null
        val day = d.toIntOrNull() ?: return null
        val t = (today % 10000).toInt()
        if (day != t && day != (t + 9999) % 10000) return null
        return id
    }

    fun newPlayerId(): String = (0 until 4).map { ALPHA.random() }.joinToString("")
}

// ---------------- the saved state of all of the above ----------------

class MetaState(private val prefs: SharedPreferences) {
    private fun ints(key: String) = prefs.getString(key, "")?.split(",")?.mapNotNull { it.toIntOrNull() } ?: emptyList()
    private fun strs(key: String) = prefs.getString(key, "")?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()

    // event
    var flakes by mutableIntStateOf(prefs.getInt("m_flakes", 0)); private set
    val boughtOffers = mutableStateListOf<String>().apply { addAll(strs("m_offers_${eventWeekKey()}")) }
    fun addFlakes(n: Int) { flakes += n; prefs.edit().putInt("m_flakes", flakes).apply() }
    fun spendFlakes(n: Int): Boolean { if (flakes < n) return false; flakes -= n; prefs.edit().putInt("m_flakes", flakes).apply(); return true }
    fun markOffer(title: String) { boughtOffers.add(title); prefs.edit().putString("m_offers_${eventWeekKey()}", boughtOffers.joinToString(",")).apply() }

    // pass
    private var passSeason = prefs.getInt("m_pass_season", 0)
    var passStars by mutableIntStateOf(if (passSeason == passSeasonKey()) prefs.getInt("m_pass_stars", 0) else 0); private set
    var passPremium by mutableStateOf(passSeason == passSeasonKey() && prefs.getBoolean("m_pass_premium", false)); private set
    val passClaimedFree = mutableStateListOf<Int>().apply { if (passSeason == passSeasonKey()) addAll(ints("m_pass_free")) }
    val passClaimedPremium = mutableStateListOf<Int>().apply { if (passSeason == passSeasonKey()) addAll(ints("m_pass_prem")) }
    val passTier: Int get() = (passStars / PASS_STARS_PER_TIER).coerceAtMost(PASS_TIERS)
    private fun checkSeason() {
        if (passSeason == passSeasonKey()) return
        passSeason = passSeasonKey(); passStars = 0; passPremium = false
        passClaimedFree.clear(); passClaimedPremium.clear(); savePass()
    }
    fun addStars(n: Int) { checkSeason(); passStars += n; savePass() }
    fun unlockPremium() { checkSeason(); passPremium = true; savePass() }
    fun claimPass(tier: Int, premium: Boolean): Boolean {
        checkSeason()
        if (tier > passTier) return false
        val list = if (premium) passClaimedPremium else passClaimedFree
        if (tier in list || (premium && !passPremium)) return false
        list.add(tier); savePass(); return true
    }
    val passReady: Int get() = (1..passTier).count { it !in passClaimedFree } + (if (passPremium) (1..passTier).count { it !in passClaimedPremium } else 0)
    private fun savePass() {
        prefs.edit().putInt("m_pass_season", passSeason).putInt("m_pass_stars", passStars).putBoolean("m_pass_premium", passPremium)
            .putString("m_pass_free", passClaimedFree.joinToString(",")).putString("m_pass_prem", passClaimedPremium.joinToString(",")).apply()
    }

    // wheel
    private var wheelDay = prefs.getLong("m_wheel_day", -1L)
    var wheelFreeUsed by mutableStateOf(wheelDay == LocalDate.now().toEpochDay() && prefs.getBoolean("m_wheel_free", false)); private set
    var wheelAdsUsed by mutableIntStateOf(if (wheelDay == LocalDate.now().toEpochDay()) prefs.getInt("m_wheel_ads", 0) else 0); private set
    private fun wheelToday() {
        val t = LocalDate.now().toEpochDay()
        if (wheelDay != t) { wheelDay = t; wheelFreeUsed = false; wheelAdsUsed = 0 }
    }
    val wheelFreeReady: Boolean get() { wheelToday(); return !wheelFreeUsed }
    fun useWheel(kind: Int) { // 0 free, 1 ad, 2 gems
        wheelToday()
        if (kind == 0) wheelFreeUsed = true
        if (kind == 1) wheelAdsUsed++
        prefs.edit().putLong("m_wheel_day", wheelDay).putBoolean("m_wheel_free", wheelFreeUsed).putInt("m_wheel_ads", wheelAdsUsed).apply()
    }

    // album: "region:index" keys, postcards by region
    val found = mutableStateListOf<String>().apply { addAll(strs("m_found")) }
    val postcards = mutableStateListOf<Int>().apply { addAll(ints("m_cards")) }
    fun hasItem(region: Int, i: Int) = "$region:$i" in found
    fun addItem(region: Int, i: Int) { found.add("$region:$i"); prefs.edit().putString("m_found", found.joinToString(",")).apply() }
    fun addPostcard(region: Int): Boolean {
        if (region in postcards) return false
        postcards.add(region); prefs.edit().putString("m_cards", postcards.joinToString(",")).apply(); return true
    }
    val setsDone: Int get() = COLLECTIBLE_NAMES.indices.count { r -> (0 until 6).all { hasItem(r, it) } }

    // stories: chapter reached per survivor and the stat value when that chapter started
    val storyStep = mutableStateListOf<Int>().apply { val s = ints("m_story"); STORIES.indices.forEach { add(s.getOrElse(it) { 0 }) } }
    val storyBase = mutableStateListOf<Int>().apply { val s = ints("m_story_base"); STORIES.indices.forEach { add(s.getOrElse(it) { -1 }) } }
    fun saveStories() { prefs.edit().putString("m_story", storyStep.joinToString(",")).putString("m_story_base", storyBase.joinToString(",")).apply() }

    // golden devices and polish
    var polish by mutableIntStateOf(prefs.getInt("m_polish", 0)); private set
    val golden = mutableStateListOf<String>().apply { addAll(strs("m_golden")) }
    fun addPolish(n: Int) { polish += n; prefs.edit().putInt("m_polish", polish).apply() }
    fun makeGolden(t: UpgradeType): Boolean {
        if (polish <= 0 || t.name in golden) return false
        polish--; golden.add(t.name)
        prefs.edit().putInt("m_polish", polish).putString("m_golden", golden.joinToString(",")).apply(); return true
    }

    // journey legs reached
    val legsDone = mutableStateListOf<Int>().apply { addAll(ints("m_legs")) }
    fun markLeg(i: Int) { legsDone.add(i); prefs.edit().putString("m_legs", legsDone.joinToString(",")).apply() }

    // customisation
    val paints = mutableStateListOf<Int>().apply { addAll(ints("m_paints")) }
    fun addPaint(i: Int) { if (i !in paints) { paints.add(i); prefs.edit().putString("m_paints", paints.joinToString(",")).apply() } }
    var horn by mutableIntStateOf(prefs.getInt("m_horn", 0)); private set
    val horns = mutableStateListOf(0).apply { addAll(ints("m_horns").filter { it != 0 }) }
    fun buyHorn(i: Int) { if (i !in horns) horns.add(i); horn = i; prefs.edit().putString("m_horns", horns.joinToString(",")).putInt("m_horn", i).apply() }
    fun chooseHorn(i: Int) { if (i in horns) { horn = i; prefs.edit().putInt("m_horn", i).apply() } }
    val decor = mutableStateListOf<String>().apply { addAll(strs("m_decor")) }
    fun addDecor(d: CarDecor) { if (d.name !in decor) { decor.add(d.name); prefs.edit().putString("m_decor", decor.joinToString(",")).apply() } }

    // records
    var bestRun by mutableStateOf(prefs.getFloat("m_best_run", 0f).toDouble()); private set
    var bestSpeed by mutableStateOf(prefs.getFloat("m_best_speed", 0f).toDouble()); private set
    var bestPassengers by mutableIntStateOf(prefs.getInt("m_best_pax", 0)); private set
    var bestCombo by mutableIntStateOf(prefs.getInt("m_best_combo", 0)); private set
    var wolvesChased by mutableIntStateOf(prefs.getInt("m_wolves", 0)); private set
    var minigamesPlayed by mutableIntStateOf(prefs.getInt("m_minigames", 0)); private set
    fun record(run: Double, speed: Double, pax: Int) {
        var dirty = false
        if (run > bestRun + 1.0) { bestRun = run; dirty = true }
        if (speed > bestSpeed + 0.01) { bestSpeed = speed; dirty = true }
        if (pax > bestPassengers) { bestPassengers = pax; dirty = true }
        if (dirty) prefs.edit().putFloat("m_best_run", bestRun.toFloat()).putFloat("m_best_speed", bestSpeed.toFloat()).putInt("m_best_pax", bestPassengers).apply()
    }
    fun recordCombo(c: Int) { if (c > bestCombo) { bestCombo = c; prefs.edit().putInt("m_best_combo", c).apply() } }
    fun addWolves(n: Int) { wolvesChased += n; prefs.edit().putInt("m_wolves", wolvesChased).apply() }
    fun addMinigame() { minigamesPlayed++; prefs.edit().putInt("m_minigames", minigamesPlayed).apply() }

    // friends
    val playerId: String = prefs.getString("m_pid", null) ?: GiftCodes.newPlayerId().also { prefs.edit().putString("m_pid", it).apply() }
    val redeemed = mutableStateListOf<String>().apply { addAll(strs("m_redeemed")) }
    private var redeemDay = prefs.getLong("m_redeem_day", -1L)
    var redeemedToday by mutableIntStateOf(if (redeemDay == LocalDate.now().toEpochDay()) prefs.getInt("m_redeem_n", 0) else 0); private set
    private var sentDay = prefs.getLong("m_sent_day", -1L)
    val sentToday: Boolean get() = sentDay == LocalDate.now().toEpochDay()
    fun markSent() { sentDay = LocalDate.now().toEpochDay(); prefs.edit().putLong("m_sent_day", sentDay).apply() }
    fun markRedeemed(code: String) {
        val t = LocalDate.now().toEpochDay()
        if (redeemDay != t) { redeemDay = t; redeemedToday = 0 }
        redeemedToday++; redeemed.add(code.trim().uppercase())
        prefs.edit().putLong("m_redeem_day", redeemDay).putInt("m_redeem_n", redeemedToday).putString("m_redeemed", redeemed.takeLast(200).joinToString(",")).apply()
    }

    fun reset() {
        // a full reset of progress keeps purchases and cosmetics, but clears journey and stories
        legsDone.clear(); storyStep.indices.forEach { storyStep[it] = 0; storyBase[it] = -1 }
        prefs.edit().remove("m_legs").remove("m_story").remove("m_story_base").apply()
    }
}
