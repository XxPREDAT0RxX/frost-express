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
            ambience?.setVolume(ambVol, ambVol)
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
        ).forEach { ids[it] = pool.load(ctx, it, 1) }
    }

    /** [rate] changes the pitch (0.5..2), used to give repeated sounds some variety. [pan] -1 left .. 1 right. */
    fun play(res: Int, volume: Float = 1f, rate: Float = 1f, pan: Float = 0f) {
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

    // ---- nature ambience: birds by day, crickets at night, wind in a storm ----
    private var ambience: MediaPlayer? = null
    private var ambienceRes = 0
    private val ambVol: Float get() = (sfxVolume * 0.55f).coerceIn(0f, 1f)

    /** Switches the looping ambience (0 = none). Called often; only does work when the choice changes. */
    fun setAmbience(res: Int) {
        if (!enabled || !foreground) { pauseAmbience(); return }
        if (res == ambienceRes && ambience != null) {
            ambience?.let { if (!it.isPlaying) it.start() }
            return
        }
        ambience?.release()
        ambience = null
        ambienceRes = res
        if (res == 0) return
        ambience = MediaPlayer.create(ctx, res)?.apply {
            isLooping = true
            setVolume(ambVol, ambVol)
            start()
        }
    }

    fun pauseAmbience() {
        ambience?.let { if (it.isPlaying) it.pause() }
    }

    fun release() {
        music?.release()
        music = null
        ambience?.release()
        ambience = null
        pool.release()
    }
}

/** The soft click for buttons and other UI elements (not for taps on the game scene). */
object UiSound {
    var audio: GameAudio? = null
    fun click() { audio?.play(R.raw.sfx_click, 0.45f) }
}
