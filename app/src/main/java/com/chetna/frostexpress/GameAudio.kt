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

    init {
        listOf(
            R.raw.sfx_buy, R.raw.sfx_coin, R.raw.sfx_click,
            R.raw.sfx_evolve, R.raw.sfx_boost, R.raw.sfx_deny,
        ).forEach { ids[it] = pool.load(ctx, it, 1) }
    }

    fun play(res: Int, volume: Float = 1f) {
        if (!enabled) return
        ids[res]?.let { pool.play(it, volume, volume, 1, 0, 1f) }
    }

    fun startMusic() {
        if (!enabled) return
        if (music == null) {
            music = MediaPlayer.create(ctx, R.raw.music_cozy)?.apply {
                isLooping = true
                setVolume(MUSIC_VOLUME, MUSIC_VOLUME)
            }
        }
        music?.start()
    }

    fun pauseMusic() {
        music?.let { if (it.isPlaying) it.pause() }
    }

    fun release() {
        music?.release()
        music = null
        pool.release()
    }

    private companion object {
        const val MUSIC_VOLUME = 0.35f
    }
}
