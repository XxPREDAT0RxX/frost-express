package com.chetna.frostexpress

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
 * Records short gameplay clips for the store trailer: the real game, frame by frame, with the clock
 * stepped 1/24 s at a time. Not part of the normal build; the "Trailer" workflow runs it and
 * store_assets/make_trailer.py turns app/build/trailer/<clip>_NNNN.png into the video.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w360dp-h780dp-port-xxhdpi")
class TrailerTest {

    @get:Rule
    val compose = createComposeRule()

    private fun prefs(setup: android.content.SharedPreferences.Editor.() -> Unit) {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.getSharedPreferences("frost_express", Context.MODE_PRIVATE).edit().clear().apply {
            putBoolean("tut", true); putInt("simTour", 5); putInt("tstage", 4)
            putLong("dailyDay", java.time.LocalDate.now().toEpochDay()); putLong("specialDay", java.time.LocalDate.now().toEpochDay())
            putBoolean("sound", false)
            // no pop-ups in the middle of a shot: every feature already introduced, every award already won
            putString("featSeen", Feature.values().joinToString(",") { it.name })
            putString("ach", ACHIEVEMENTS.joinToString(",") { it.id })
            setup()
        }.commit()
    }

    private fun vm() = GameViewModel(ApplicationProvider.getApplicationContext<Application>())

    private fun clip(name: String, seconds: Float, content: @Composable () -> Unit) {
        val fps = 24
        compose.mainClock.autoAdvance = false
        compose.setContent { FrostTheme { content() } }
        compose.mainClock.advanceTimeBy(900)
        compose.waitForIdle()
        val n = (seconds * fps).toInt()
        var t = 0.0
        for (i in 0 until n) {
            val next = (i + 1) * 1000.0 / fps
            compose.mainClock.advanceTimeBy((next - t).toLong().coerceAtLeast(1))
            t += (next - t).toLong().coerceAtLeast(1)
            compose.waitForIdle()
            captureScreenRoboImage("build/trailer/%s_%04d.png".format(name, i))
        }
    }

    private val full = "30,20,15,12,10,8,8,6,6,6,5,5,5,4,4,4,3,3,3,3,2,2,2,2,2,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1"

    @Test fun c1_interior() {
        prefs { putString("coins", "90000000"); putInt("evo", 10); putInt("plvl", 5); putInt("pax", 24); putString("levels", full); putString("unl", UPGRADES.joinToString(",") { it.type.name }) }
        val v = vm(); clip("c1_interior", 4.5f) { GameScreen(v) }
    }

    @Test fun c2_station() {
        debugStationType = 1
        prefs { putString("coins", "9000000"); putInt("evo", 3); putInt("cores", 6); putString("levels", "20,10,8,6,4,3,5,5,6,6,4,4,3,3,2,2,1,1"); putString("unl", UPGRADES.take(18).joinToString(",") { it.type.name }) }
        val v = vm(); v.debugScene(10.0, 330f)
        clip("c2_station", 5.5f) { GameScreen(v) }
        debugStationType = -1
    }

    @Test fun c3_village() {
        prefs { putInt("country", 5); putString("totalDist", "1700"); putString("levels", "12,6,5,3,2,1,0,0,1,1") }
        val v = vm(); clip("c3_village", 4f) { GameScreen(v) }
    }

    @Test fun c4_desert() {
        prefs { putString("totalDist", "9500"); putString("levels", "12,6,5,3,2,1,0,0,1,1") }
        val v = vm(); clip("c4_desert", 3f) { GameScreen(v) }
    }

    @Test fun c5_night() {
        prefs { putInt("country", 5); putString("totalDist", "1700"); putString("levels", "12,6,5,3,2,1,0,0,1,1") }
        val v = vm(); v.debugScene(Balance.DAY_SEC + Balance.DUSK_SEC + Balance.NIGHT_SEC / 2, Float.NaN)
        clip("c5_night", 3f) { GameScreen(v) }
    }

    @Test fun c6_window() {
        prefs { putString("levels", "12,6,5,3,2,1,0,0,1,1") }
        val v = vm(); v.enterRegion(0, "the Green Meadow", 22)
        clip("c6_window", 4f) { Box(Modifier.fillMaxSize()) { WindowView(v) } }
    }

    @Test fun c7_window_snow() {
        prefs { putString("levels", "12,6,5,3,2,1,0,0,1,1") }
        val v = vm(); v.enterRegion(5, "the Snowfields", -18)
        clip("c7_window_snow", 3f) { Box(Modifier.fillMaxSize()) { WindowView(v) } }
    }

    @Test fun c8_zoomout() {
        prefs { putString("coins", "9000000"); putInt("evo", 3); putString("levels", "20,10,8,6,4,3,5,5,6,6") }
        val v = vm(); val link = SceneLink().apply { zoom = 0.6f }
        clip("c8_zoomout", 3f) { Box(Modifier.fillMaxSize()) { TrainScene(v, Modifier.fillMaxSize(), link = link) } }
    }
}
