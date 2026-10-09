package com.chetna.frostexpress

import android.os.Build
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
            snapshotFlow { vm.fpsCap }.collect { fps ->
                val lp = window.attributes
                lp.preferredRefreshRate = fps.toFloat()
                window.attributes = lp
            }
        }
        vm.attachCloud(if (Balance.PLAY_GAMES_ENABLED) PlayGamesCloud(this) else NoCloud)
        setContent {
            FrostTheme {
                GameScreen(vm)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    // Immersive mode: system bars stay hidden and appear only briefly on a swipe from the edge.
    private fun hideSystemBars() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    // A crisp click on every touch, anywhere on the screen.
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) vm.audio.play(R.raw.sfx_click, 0.7f)
        return super.dispatchTouchEvent(ev)
    }

    override fun onStart() {
        super.onStart()
        vm.audio.startMusic()
    }

    override fun onStop() {
        super.onStop()
        vm.audio.pauseMusic()
        vm.save()
        vm.cloudSaveNow()
    }
}
