package com.chetna.frostexpress

/**
 * The world journey: the Little Express rides a loop of 30 stations through one country,
 * then the player may "prestige" and travel on to the next country, which has its own
 * 30-station loop and its own themed lands. Each country re-uses the six biomes but in its
 * own order and with its own signature colour, so every country feels like a new place.
 */
class Country(
    val name: String,
    val flag: String,
    val tagline: String,
    /** Indices into ZONES (the six biomes) this country cycles through, in order. */
    val zones: List<Int>,
    val accent: Long,
)

/** Stations the line visits before a country's loop is complete and prestige opens. */
const val STATIONS_PER_COUNTRY = 30

/** Permanent coin bonus for every country left behind (prestige). */
const val PRESTIGE_BONUS = 0.15   // old per-country bonus, kept for saves made before the new prestige
const val PRESTIGE_MIN_EVO = 5

// Biome indices match ZONES in TrainScene: 0 meadow, 1 pine woods, 2 savanna, 3 desert, 4 rainforest, 5 snowfields.
val COUNTRIES = listOf(
    Country("Snowhaven", "❄️", "Where the Little Express first set out, through pines and deep snow.", listOf(5, 1, 0), 0xFF8FB8D8),
    Country("Greenvale", "🌿", "Rolling meadows and shady woods, gentle and green all year.", listOf(0, 1, 4), 0xFF7FB85C),
    Country("Sunlands", "☀️", "Golden savanna giving way to red desert under a wide warm sky.", listOf(2, 3, 0), 0xFFE0A94E),
    Country("Wildtropics", "🌴", "Steamy rainforest, tall palms and the odd acacia on the plains.", listOf(4, 2, 1), 0xFF3FA06A),
    Country("Frostpeaks", "🏔️", "The high north again: snowfields and lonely pines on the roof of the world.", listOf(5, 5, 1), 0xFFB8C8DA),
)

fun countryAt(index: Int): Country = COUNTRIES[Math.floorMod(index, COUNTRIES.size)]

/** The biome shown at a given step of the environment loop for a country. */
fun countryZoneAt(index: Int, step: Int): Int {
    val c = countryAt(index)
    return c.zones[Math.floorMod(step, c.zones.size)]
}
