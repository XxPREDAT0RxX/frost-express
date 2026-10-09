package com.chetna.frostexpress

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
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
    }

    private fun shot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { FrostTheme { content() } }
        compose.mainClock.advanceTimeBy(900)
        compose.waitForIdle()
        captureScreenRoboImage("build/screens/$name.png") // the whole screen, dialogs included
    }

    private fun vm() = GameViewModel(ApplicationProvider.getApplicationContext<Application>())

    private fun runAll(size: String, screen: String) {
        when (screen) {
            "loading" -> shot("${size}_1_loading") { LoadingScreen(0.6f) }
            "game" -> { playing(); val v = vm(); shot("${size}_2_game") { GameScreen(v) } }
            "upgrades" -> { playing(); val v = vm(); shot("${size}_3_upgrades") { GameScreen(v, initialSheet = 1) } }
            "device" -> { playing(); val v = vm(); shot("${size}_4_device") { GameScreen(v, initialDetail = UpgradeType.ENGINE) } }
            "welcome" -> { prefs { }; val v = vm(); shot("${size}_5_welcome") { GameScreen(v) } }
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

    // big phone
    @Test @Config(sdk = [34], qualifiers = "w412dp-h915dp-port-xxhdpi") fun big_game() = runAll("big", "game")
    @Test @Config(sdk = [34], qualifiers = "w412dp-h915dp-port-xxhdpi") fun big_upgrades() = runAll("big", "upgrades")
    @Test @Config(sdk = [34], qualifiers = "w412dp-h915dp-port-xxhdpi") fun big_daily() = runAll("big", "daily")

    // tablet
    @Test @Config(sdk = [34], qualifiers = "w800dp-h1280dp-port-xhdpi") fun tablet_game() = runAll("tablet", "game")
    @Test @Config(sdk = [34], qualifiers = "w800dp-h1280dp-port-xhdpi") fun tablet_device() = runAll("tablet", "device")
}
