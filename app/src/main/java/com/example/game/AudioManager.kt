package com.example.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.ToneGenerator
import android.media.AudioManager as AndroidAudioManager
import android.util.Log
import java.io.IOException

/**
 * Manages game sound effects (jump, point, hit).
 * If physical assets are unavailable in the assets folder, it triggers synthesized
 * retro-arcade beeps via the Android ToneGenerator to preserve complete, enjoyable gameplay feedback.
 *
 * ASSET INSTRUCTIONS:
 * Place your "jump.wav", "point.wav", and "hit.wav" audio files inside:
 * [project_root]/app/src/main/assets/ or [project_root]/app/src/main/res/raw/
 * Currently, this manager checks the main "assets/" resource bucket.
 */
class AudioManager(private val context: Context) {
    private var soundPool: SoundPool? = null
    private var jumpSoundId: Int = -1
    private var pointSoundId: Int = -1
    private var hitSoundId: Int = -1

    private var toneGenerator: ToneGenerator? = null

    init {
        // Build high-performance audio properties suitable for arcade games
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attributes)
            .build()

        // Construct ToneGenerator synthetic fallback for running container simulations
        try {
            toneGenerator = ToneGenerator(AndroidAudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            Log.e("AudioManager", "Failed to initialize ToneGenerator synthesizer: ${e.message}")
        }

        loadAssets()
    }

    /**
     * Attempts to resolve custom audio clips from standard asset pools.
     */
    private fun loadAssets() {
        soundPool?.let { pool ->
            try {
                val assetManager = context.assets
                // Check if files exist to avoid throwing IOException silently
                jumpSoundId = pool.load(assetManager.openFd("jump.wav"), 1)
                pointSoundId = pool.load(assetManager.openFd("point.wav"), 1)
                hitSoundId = pool.load(assetManager.openFd("hit.wav"), 1)
                Log.d("AudioManager", "Loaded audio wave files (jump.wav, point.wav, hit.wav) from assets!")
            } catch (e: IOException) {
                Log.w("AudioManager", "Audio files (jump.wav, point.wav, hit.wav) not found " +
                        "inside 'app/src/main/assets/'. Using synthesized arcade tone fallbacks instead.")
            } catch (e: Exception) {
                Log.e("AudioManager", "Sound loader encountered exception: ${e.message}")
            }
        }
    }

    /**
     * Plays bird flapping sound.
     */
    fun playJump() {
        if (jumpSoundId != -1) {
            soundPool?.play(jumpSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } else {
            // Retro synth beep: short, cheerful upward chirp
            playToneFallback(ToneGenerator.TONE_PROP_BEEP, 60)
        }
    }

    /**
     * Plays score threshold point reward sound.
     */
    fun playPoint() {
        if (pointSoundId != -1) {
            soundPool?.play(pointSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } else {
            // Retro synth beep: high-pitched positive CDMA confirmation pip
            playToneFallback(ToneGenerator.TONE_CDMA_PIP, 80)
        }
    }

    /**
     * Plays collision obstacle hit sound.
     */
    fun playHit() {
        if (hitSoundId != -1) {
            soundPool?.play(hitSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
        } else {
            // Retro synthesizer: low buzz/buzzing alarm indicating strike
            playToneFallback(ToneGenerator.TONE_SUP_ERROR, 200)
        }
    }

    /**
     * Generates real-time synthetic wave triggers.
     */
    private fun playToneFallback(toneType: Int, durationMs: Int) {
        try {
            toneGenerator?.startTone(toneType, durationMs)
        } catch (e: Exception) {
            Log.e("AudioManager", "Tone generator failed to synth beep: ${e.message}")
        }
    }

    /**
     * Call when component lifecycle terminates to prevent audio thread leak.
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        toneGenerator?.release()
        toneGenerator = null
    }
}
