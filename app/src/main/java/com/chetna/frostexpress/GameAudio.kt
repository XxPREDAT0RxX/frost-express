package com.chetna.frostexpress

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/** Plays short sound effects (SoundPool), the background music and the looping nature ambience (MediaPlayer). */
class GameAudio(private val ctx: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = HashMap<Int, Int>()
    private var music: MediaPlayer? = null

    var enabled = true
        set(v) {
            field = v
            if (!v) pauseAmbience()
        }

    /** False while the app is in the background: nothing at all may play then. */
    var foreground = true
        set(v) {
            field = v
            if (!v) {
                pauseMusic()
                pauseAmbience()
                pool.autoPause()
            } else {
                pool.autoResume()
            }
        }

    var sfxVolume = 1f
        set(v) {
            field = v
            applyAmbienceVolume()
        }
    var musicVolume = 0.35f
        set(v) {
            field = v
            music?.setVolume(v, v)
        }

    init {
        listOf(
            R.raw.sfx_buy, R.raw.sfx_coin, R.raw.sfx_click,
            R.raw.sfx_evolve, R.raw.sfx_boost, R.raw.sfx_deny, R.raw.sfx_horn,
            R.raw.sfx_wolf, R.raw.sfx_brake, R.raw.sfx_pass,
            R.raw.sfx_install, R.raw.sfx_upgrade, R.raw.sfx_bird1, R.raw.sfx_bird2, R.raw.sfx_bird3,
            R.raw.sfx_horn2, R.raw.sfx_horn3,
        ).forEach { ids[it] = pool.load(ctx, it, 1) }
    }

    /** [rate] changes the pitch (0.5..2), used to give repeated sounds some variety. [pan] -1 left .. 1 right. */
    fun play(res: Int, volume: Float = 1f, rate: Float = 1f, pan: Float = 0f) {
        Haptics.forSound(res)
        if (!enabled || !foreground) return
        val v = (volume * sfxVolume).coerceIn(0f, 1f)
        val l = v * (1f - pan.coerceIn(0f, 1f))
        val r = v * (1f + pan.coerceIn(-1f, 0f))
        ids[res]?.let { pool.play(it, l, r, 1, 0, rate.coerceIn(0.5f, 2f)) }
    }

    // Five tracks played in random order, one after another (never the same one twice in a row).
    private val tracks = listOf(R.raw.music_1, R.raw.music_2, R.raw.music_3, R.raw.music_4, R.raw.music_5)
    private var current = -1

    private fun playNext() {
        music?.release()
        music = null
        if (!enabled || !foreground) return
        var next = (tracks.indices).random()
        if (next == current && tracks.size > 1) next = (next + 1) % tracks.size
        current = next
        music = MediaPlayer.create(ctx, tracks[next])?.apply {
            setVolume(musicVolume, musicVolume)
            setOnCompletionListener { if (enabled && foreground) playNext() }
            start()
        }
    }

    fun startMusic() {
        if (!enabled || !foreground) return
        if (music == null) playNext() else music?.start()
    }

    fun pauseMusic() {
        music?.let { if (it.isPlaying) it.pause() }
    }

    // ---- nature ambience: a breeze by day, crickets at night, wind in a storm ----
    // A change cross-fades over a few seconds: the old loop fades out while the new one fades in.
    private var ambience: MediaPlayer? = null
    private var ambienceRes = 0
    private var fadingOut: MediaPlayer? = null
    private var mix = 1f           // 0 = all old loop, 1 = all new loop
    private val ambVol: Float get() = (sfxVolume * 0.55f).coerceIn(0f, 1f)
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val fadeStep = object : Runnable {
        override fun run() {
            mix = (mix + 0.05f / 3f).coerceAtMost(1f) // 3 second cross-fade
            applyAmbienceVolume()
            if (mix < 1f) handler.postDelayed(this, 50L)
            else { fadingOut?.release(); fadingOut = null }
        }
    }

    private fun applyAmbienceVolume() {
        val v = ambVol
        ambience?.setVolume(v * mix, v * mix)
        fadingOut?.setVolume(v * (1f - mix), v * (1f - mix))
    }

    /** Switches the looping ambience (0 = none). Called often; only does work when the choice changes. */
    fun setAmbience(res: Int) {
        if (!enabled || !foreground) { pauseAmbience(); return }
        if (res == ambienceRes && ambience != null) {
            ambience?.let { if (!it.isPlaying) it.start() }
            fadingOut?.let { if (!it.isPlaying && mix < 1f) it.start() }
            return
        }
        // the current loop becomes the one fading out
        fadingOut?.release()
        fadingOut = ambience
        ambience = null
        ambienceRes = res
        mix = 0f
        if (res != 0) {
            ambience = MediaPlayer.create(ctx, res)?.apply {
                isLooping = true
                setVolume(0f, 0f)
                start()
            }
        }
        handler.removeCallbacks(fadeStep)
        handler.post(fadeStep)
    }

    fun pauseAmbience() {
        ambience?.let { if (it.isPlaying) it.pause() }
        fadingOut?.let { if (it.isPlaying) it.pause() }
    }

    fun release() {
        music?.release()
        music = null
        handler.removeCallbacks(fadeStep)
        ambience?.release()
        ambience = null
        fadingOut?.release()
        fadingOut = null
        pool.release()
    }
}

/** The soft click for buttons and other UI elements (not for taps on the game scene). */
object UiSound {
    var audio: GameAudio? = null
    fun click() { audio?.play(R.raw.sfx_click, 0.45f) }
}

/**
 * Light vibrations for taps, purchases and big moments. Uses the view's haptic feedback (no permission needed,
 * follows the phone's touch-feedback setting) and can be switched off in Settings.
 */
object Haptics {
    var view: android.view.View? = null
    var enabled = true
    var foreground = true

    private fun feel(constant: Int) {
        if (!enabled || !foreground) return
        view?.performHapticFeedback(constant)
    }

    /** A soft tick for buttons. */
    fun tick() = feel(android.view.HapticFeedbackConstants.CLOCK_TICK)
    /** A firmer tap: upgrades, BOOST. */
    fun tap() = feel(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
    /** A confirm buzz: installing, claiming, rewards. */
    fun confirm() = feel(if (android.os.Build.VERSION.SDK_INT >= 30) android.view.HapticFeedbackConstants.CONFIRM else android.view.HapticFeedbackConstants.VIRTUAL_KEY)
    /** A long buzz: evolving, level up. */
    fun big() = feel(android.view.HapticFeedbackConstants.LONG_PRESS)

    /** Pick the feeling that goes with a sound effect. */
    fun forSound(res: Int) {
        when (res) {
            R.raw.sfx_click -> tick()
            R.raw.sfx_upgrade, R.raw.sfx_boost, R.raw.sfx_coin -> tap()
            R.raw.sfx_buy, R.raw.sfx_install -> confirm()
            R.raw.sfx_evolve -> big()
        }
    }
}
