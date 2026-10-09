package com.chetna.frostexpress

import android.os.Build
import java.io.File
import android.os.Bundle
import android.view.MotionEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.snapshotFlow
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create the ViewModel here, never lazily inside snapshotFlow (that runs in a read-only snapshot).
        val game = vm

        // Crash recovery: if the last run crashed (or died without a clean exit), start in safe mode
        // (no post-processing, 60 fps). A recorded crash is shown on screen so it can be reported.
        val prefs = getSharedPreferences("frost_express", MODE_PRIVATE)
        val crashFile = File(filesDir, FrostApp.CRASH_FILE)
        if (crashFile.exists() || prefs.getBoolean("running", false)) {
            prefs.edit().putBoolean("postfx", false).putInt("fps", 60).putBoolean("running", false).apply()
        }
        if (crashFile.exists()) {
            val report = try { crashFile.readText() } catch (e: Exception) { "Could not read the crash report." }
            enableEdgeToEdge()
            setContent {
                FrostTheme {
                    CrashScreen(
                        report,
                        onContinue = { crashFile.delete(); recreate() },
                        onReset = { crashFile.delete(); prefs.edit().clear().apply(); recreate() },
                    )
                }
            }
            crashMode = true
            return
        }
        enableEdgeToEdge()
        // Draw under the camera cutout as well, for a true full screen.
        if (Build.VERSION.SDK_INT >= 28) {
            val lp = window.attributes
            lp.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            window.attributes = lp
        }
        // Ask the display for the frame rate chosen in Settings (a hint; the device may cap it).
        lifecycleScope.launch {
            snapshotFlow { game.fpsCap }.collect { fps ->
                val lp = window.attributes
                lp.preferredRefreshRate = fps.toFloat()
                window.attributes = lp
            }
        }
        vm.attachCloud(if (Balance.PLAY_GAMES_ENABLED) PlayGamesCloud(this) else NoCloud)
        if (Balance.ADS_ENABLED) game.ads = AdMobRewarded(this, game::onAdReady)
        if (Balance.IAP_ENABLED) game.store = PlayBilling(this, game::onStorePrices, game::grantProduct)
        setContent {
            FrostTheme {
                GameScreen(vm)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !crashMode) hideSystemBars()
    }

    // Immersive mode: system bars stay hidden and appear only briefly on a swipe from the edge.
    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    // A crisp click on every touch, anywhere on the screen.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return super.dispatchTouchEvent(ev)
    }

    private var crashMode = false

    override fun onDestroy() {
        if (!crashMode) vm.store.release()
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        if (crashMode) return
        getSharedPreferences("frost_express", MODE_PRIVATE).edit().putBoolean("running", true).apply()
        vm.audio.startMusic()
    }

    override fun onStop() {
        super.onStop()
        if (crashMode) return
        getSharedPreferences("frost_express", MODE_PRIVATE).edit().putBoolean("running", false).apply()
        vm.audio.pauseMusic()
        vm.save()
        vm.cloudSaveNow()
    }
}
