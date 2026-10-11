package com.chetna.frostexpress

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the main screens on four phone sizes. CI runs this and publishes the pictures with each build,
 * so the layout can be checked on screens we do not own. Output: app/build/screens/<size>_<screen>.png
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun prefs(setup: android.content.SharedPreferences.Editor.() -> Unit) {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("frost_express", Context.MODE_PRIVATE).edit().clear().apply { setup() }.commit()
    }

    private fun playing() = prefs {
        putBoolean("tut", true)
        putInt("tstage", 4)
        putLong("dailyDay", java.time.LocalDate.now().toEpochDay())
        putString("coins", "48250")
        putInt("gems", 42)
        putString("levels", "12,6,5,3,2,1,0,0,1,1")
        putString("unl", "WAGON,HEAT,FOOD,PLOW,COAL,BOOSTER,CONDUCTOR,STEAM_WHISTLE,HEADLAMP")
        putInt("xp", 40)
        putInt("plvl", 3)
        putLong("specialDay", java.time.LocalDate.now().toEpochDay())
    }

    private fun shot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { FrostTheme { content() } }
        compose.mainClock.advanceTimeBy(900)
        compose.waitForIdle()
        captureScreenRoboImage("build/screens/$name.png") // the whole screen, dialogs included
    }

    private fun vm() = GameViewModel(ApplicationProvider.getApplicationContext<Application>())

    private fun runBiome(screen: String) = runAll("normal", "biome:$screen")
    private fun runAll(size: String, screen0: String) {
        val screen = if (screen0.startsWith("biome:")) screen0.removePrefix("biome:") else screen0

        when (screen) {
            "loading" -> shot("${size}_1_loading") { LoadingScreen(0.6f) }
            "game" -> { playing(); val v = vm(); shot("${size}_2_game") { GameScreen(v) } }
            "upgrades" -> { playing(); val v = vm(); shot("${size}_3_upgrades") { GameScreen(v, initialSheet = 1) } }
            "device" -> { playing(); val v = vm(); shot("${size}_4_device") { GameScreen(v, initialDetail = UpgradeType.ENGINE) } }
            "welcome" -> { prefs { }; val v = vm(); shot("${size}_5_welcome") { GameScreen(v) } }
            "evolve" -> { playing(); val v = vm(); shot("${size}_14_evolve") { GameScreen(v, initialSheet = 6) } }
            "events" -> { playing(); val v = vm(); shot("${size}_15_events") { GameScreen(v, initialSheet = 7) } }
            "journey" -> { prefs { putBoolean("tut", true); putInt("tstage", 4); putLong("dailyDay", java.time.LocalDate.now().toEpochDay()); putLong("specialDay", java.time.LocalDate.now().toEpochDay()); putString("coins", "48250"); putInt("gems", 42); putString("levels", "12,6,5,3,2,1,0,0,1,1"); putInt("plvl", 4); putInt("country", 1); putInt("cstation", 17); putInt("prestige", 1) }; val v = vm(); shot("${size}_16_journey") { GameScreen(v, initialSheet = 8) } }
            "journeyfull" -> { prefs { putBoolean("tut", true); putInt("tstage", 4); putLong("dailyDay", java.time.LocalDate.now().toEpochDay()); putLong("specialDay", java.time.LocalDate.now().toEpochDay()); putString("coins", "48250"); putInt("gems", 42); putString("levels", "12,6,5,3,2,1,0,0,1,1"); putInt("plvl", 4); putInt("country", 2); putInt("cstation", 30); putInt("prestige", 2) }; val v = vm(); shot("${size}_27_journeyfull") { GameScreen(v, initialSheet = 8) } }
            "profile" -> { playing(); val v = vm(); shot("${size}_17_profile") { GameScreen(v, initialSheet = 9) } }
            "shop" -> { playing(); val v = vm(); shot("${size}_11_shop") { GameScreen(v, initialSheet = 2) } }
            "gems" -> { playing(); val v = vm(); shot("${size}_12_gems") { GameScreen(v, initialSheet = 3) } }
            "bag" -> { playing(); prefs { putBoolean("tut", true); putInt("tstage", 4); putLong("dailyDay", java.time.LocalDate.now().toEpochDay()); putString("inv", "2,1,0,3,1"); putFloat("shopInc", 200f); putFloat("shopFuel", 90f) }; val v = vm(); shot("${size}_13_bag") { GameScreen(v, initialSheet = 4) } }
            "quests" -> { playing(); val v = vm(); shot("${size}_7_quests") { GameScreen(v, initialSheet = 5) } }
            "evolved" -> {
                playing()
                prefs {
                    putBoolean("tut", true); putInt("tstage", 4); putLong("dailyDay", java.time.LocalDate.now().toEpochDay())
                    putString("coins", "9000000"); putInt("evo", 3); putInt("cores", 6)
                    putString("levels", "20,10,8,6,4,3,5,5,6,6,4,4,3,3,2,2,1,1")
                    putString("unl", UPGRADES.take(18).joinToString(",") { it.type.name })
                }
                val v = vm()
                v.debugScene(Balance.DAY_SEC + Balance.DUSK_SEC + 20.0, 330f)
                shot("${size}_8_night_station") { GameScreen(v) }
            }
            "trains" -> shot("${size}_9_trains") {
                androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFE8F0F7))) {
                    for (row in 0 until 3) androidx.compose.foundation.layout.Row(androidx.compose.ui.Modifier.weight(1f)) {
                        for (c in 0 until 2) TrainPicture(row * 2 + c, androidx.compose.ui.Modifier.weight(1f).fillMaxHeight().padding(8.dp))
                    }
                }
            }
            "route" -> { playing(); prefs { putBoolean("tut", true); putInt("tstage", 4); putLong("dailyDay", java.time.LocalDate.now().toEpochDay()); putLong("specialDay", java.time.LocalDate.now().toEpochDay()); putInt("evo", 4); putInt("plvl", 3); putString("routeCh", "GOLD_LINE,FAST_TRACK,CHEAP_PARTS,NIGHT_SHIFT") }; val v = vm(); shot("${size}_20_route") { GameScreen(v) } }
            "feature" -> { prefs { putBoolean("tut", true); putInt("tstage", 4); putLong("dailyDay", java.time.LocalDate.now().toEpochDay()); putLong("specialDay", java.time.LocalDate.now().toEpochDay()); putInt("plvl", 2); putString("featSeen", "") }; val v = vm(); shot("${size}_21_feature") { GameScreen(v) } }
            "away" -> { prefs { putBoolean("tut", true); putInt("tstage", 4); putLong("dailyDay", java.time.LocalDate.now().toEpochDay()); putLong("specialDay", java.time.LocalDate.now().toEpochDay()); putString("levels", "12,6,5,3,2,1,0,0,1,1"); putLong("last", System.currentTimeMillis() - 2 * 3600_000L) }; val v = vm(); v.openParcel(); shot("${size}_22_away") { GameScreen(v) } }
            "meadow", "savanna", "desert", "rainforest" -> {
                val dists = mapOf("meadow" to "1700", "savanna" to "8200", "desert" to "9500", "rainforest" to "12100")
                val d = dists[screen] ?: "1700"
                prefs { putBoolean("tut", true); putInt("tstage", 4); putLong("dailyDay", java.time.LocalDate.now().toEpochDay()); putLong("specialDay", java.time.LocalDate.now().toEpochDay()); putString("totalDist", d); putString("levels", "12,6,5,3,2,1,0,0,1,1") }
                val v = vm()
                shot("${size}_25_$screen") { GameScreen(v) }
            }
            "bigtrain" -> {
                prefs {
                    putBoolean("tut", true); putInt("tstage", 4); putLong("dailyDay", java.time.LocalDate.now().toEpochDay()); putLong("specialDay", java.time.LocalDate.now().toEpochDay())
                    putString("coins", "90000000"); putInt("evo", 4); putInt("cores", 8); putInt("plvl", 5)
                    putString("levels", "30,20,15,12,10,8,8,6,6,6,5,5,5,4,4,4,3,3,3,3,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1")
                    putString("unl", UPGRADES.joinToString(",") { it.type.name })
                }
                val v = vm()
                shot("${size}_23_bigtrain") { GameScreen(v) }
            }
            "bigupgrades" -> {
                prefs {
                    putBoolean("tut", true); putInt("tstage", 4); putLong("dailyDay", java.time.LocalDate.now().toEpochDay()); putLong("specialDay", java.time.LocalDate.now().toEpochDay())
                    putString("coins", "900000"); putInt("evo", 4); putInt("plvl", 5)
                    putString("levels", "30,20,15,12,10,8,8,6,6,6,5,5,5,4,4,4,3,3,3,3,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1")
                    putString("unl", UPGRADES.joinToString(",") { it.type.name })
                }
                val v = vm()
                shot("${size}_24_bigupgrades") { GameScreen(v, initialSheet = 1) }
            }
            "window", "window_desert", "window_snow" -> {
                prefs { putBoolean("tut", true); putInt("tstage", 4); putLong("dailyDay", java.time.LocalDate.now().toEpochDay()); putLong("specialDay", java.time.LocalDate.now().toEpochDay()); putString("levels", "12,6,5,3,2,1,0,0,1,1") }
                val v = vm()
                when (screen) {
                    "window_desert" -> v.enterRegion(3, "the Red Desert", 39)
                    "window_snow" -> v.enterRegion(5, "the Snowfields", -18)
                    else -> v.enterRegion(0, "the Green Meadow", 22)
                }
                shot("${size}_26_$screen") { androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.fillMaxSize()) { WindowView(v) } }
            }
            "anims" -> shot("${size}_18_anims") { SceneAnimPreview() }
            "alltrains" -> shot("${size}_19_alltrains") {
                androidx.compose.foundation.layout.Column(androidx.compose.ui.Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFE8F0F7))) {
                    for (row in 0 until 5) androidx.compose.foundation.layout.Row(androidx.compose.ui.Modifier.weight(1f)) {
                        for (c in 0 until 5) TrainPicture(row * 5 + c, androidx.compose.ui.Modifier.weight(1f).fillMaxHeight().padding(3.dp))
                    }
                }
            }
            "newtrain" -> {
                playing()
                prefs { putBoolean("tut", true); putInt("tstage", 4); putLong("dailyDay", java.time.LocalDate.now().toEpochDay()); putString("levels", "60,40,30,20,10,5,0,0,0,0") }
                val v = vm()
                v.evolve()
                shot("${size}_10_newtrain") { GameScreen(v) }
            }
            "daily" -> { prefs { putBoolean("tut", true); putInt("tstage", 4); putInt("streak", 2); putLong("dailyDay", java.time.LocalDate.now().toEpochDay() - 1) }; val v = vm(); shot("${size}_6_daily") { GameScreen(v) } }
        }
    }

    // small phone
    @Test @Config(sdk = [34], qualifiers = "w320dp-h640dp-port-xhdpi") fun small_loading() = runAll("small", "loading")
    @Test @Config(sdk = [34], qualifiers = "w320dp-h640dp-port-xhdpi") fun small_game() = runAll("small", "game")
    @Test @Config(sdk = [34], qualifiers = "w320dp-h640dp-port-xhdpi") fun small_upgrades() = runAll("small", "upgrades")
    @Test @Config(sdk = [34], qualifiers = "w320dp-h640dp-port-xhdpi") fun small_device() = runAll("small", "device")
    @Test @Config(sdk = [34], qualifiers = "w320dp-h640dp-port-xhdpi") fun small_welcome() = runAll("small", "welcome")
    @Test @Config(sdk = [34], qualifiers = "w320dp-h640dp-port-xhdpi") fun small_daily() = runAll("small", "daily")

    // normal phone
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_loading() = runAll("normal", "loading")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_game() = runAll("normal", "game")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_upgrades() = runAll("normal", "upgrades")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_device() = runAll("normal", "device")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_welcome() = runAll("normal", "welcome")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_daily() = runAll("normal", "daily")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_quests() = runAll("normal", "quests")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_shop() = runAll("normal", "shop")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_evolve() = runAll("normal", "evolve")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_events() = runAll("normal", "events")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_profile() = runAll("normal", "profile")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_journey() = runAll("normal", "journey")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_gems() = runAll("normal", "gems")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_bag() = runAll("normal", "bag")
    @Test @Config(sdk = [34], qualifiers = "w320dp-h640dp-port-xhdpi") fun small_shop() = runAll("small", "shop")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_evolved() = runAll("normal", "evolved")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_trains() = runAll("normal", "trains")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_newtrain() = runAll("normal", "newtrain")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_route() = runAll("normal", "route")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_feature() = runAll("normal", "feature")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_away() = runAll("normal", "away")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_bigtrain() = runAll("normal", "bigtrain")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_bmeadow() = runBiome("meadow")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_bsavanna() = runBiome("savanna")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_bdesert() = runBiome("desert")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_brainforest() = runBiome("rainforest")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_bigupgrades() = runAll("normal", "bigupgrades")
    @Test @Config(sdk = [34], qualifiers = "w320dp-h640dp-port-xhdpi") fun small_bigtrain() = runAll("small", "bigtrain")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_journeyfull() = runAll("normal", "journeyfull")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_window() = runAll("normal", "window")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_window_desert() = runAll("normal", "window_desert")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_window_snow() = runAll("normal", "window_snow")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_anims() = runAll("normal", "anims")
    @Test @Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi") fun normal_alltrains() = runAll("normal", "alltrains")

    // big phone
    @Test @Config(sdk = [34], qualifiers = "w412dp-h915dp-port-xxhdpi") fun big_game() = runAll("big", "game")
    @Test @Config(sdk = [34], qualifiers = "w412dp-h915dp-port-xxhdpi") fun big_upgrades() = runAll("big", "upgrades")
    @Test @Config(sdk = [34], qualifiers = "w412dp-h915dp-port-xxhdpi") fun big_daily() = runAll("big", "daily")

    // tablet
    @Test @Config(sdk = [34], qualifiers = "w800dp-h1280dp-port-xhdpi") fun tablet_game() = runAll("tablet", "game")
    @Test @Config(sdk = [34], qualifiers = "w800dp-h1280dp-port-xhdpi") fun tablet_device() = runAll("tablet", "device")
}
