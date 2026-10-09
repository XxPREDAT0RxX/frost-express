package com.chetna.frostexpress

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/** Plays short sound effects (SoundPool) and the looping background music (MediaPlayer). */
class GameAudio(private val ctx: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
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
    var sfxVolume = 1f
    var musicVolume = 0.35f
        set(v) {
            field = v
            music?.setVolume(v, v)
        }

    init {
        listOf(
            R.raw.sfx_buy, R.raw.sfx_coin, R.raw.sfx_click,
            R.raw.sfx_evolve, R.raw.sfx_boost, R.raw.sfx_deny, R.raw.sfx_horn,
        ).forEach { ids[it] = pool.load(ctx, it, 1) }
    }

    fun play(res: Int, volume: Float = 1f) {
        if (!enabled) return
        ids[res]?.let { pool.play(it, volume * sfxVolume, volume * sfxVolume, 1, 0, 1f) }
    }

    // Five tracks played in random order, one after another (never the same one twice in a row).
    private val tracks = listOf(R.raw.music_1, R.raw.music_2, R.raw.music_3, R.raw.music_4, R.raw.music_5)
    private var current = -1

    private fun playNext() {
        music?.release()
        var next = (tracks.indices).random()
        if (next == current && tracks.size > 1) next = (next + 1) % tracks.size
        current = next
        music = MediaPlayer.create(ctx, tracks[next])?.apply {
            setVolume(musicVolume, musicVolume)
            setOnCompletionListener { if (enabled) playNext() }
            start()
        }
    }

    fun startMusic() {
        if (!enabled) return
        if (music == null) playNext() else music?.start()
    }

    fun pauseMusic() {
        music?.let { if (it.isPlaying) it.pause() }
    }

    fun release() {
        music?.release()
        music = null
        pool.release()
    }
}

/** The soft click for buttons and other UI elements (not for taps on the game scene). */
object UiSound {
    var audio: GameAudio? = null
    fun click() { audio?.play(R.raw.sfx_click, 0.45f) }
}
