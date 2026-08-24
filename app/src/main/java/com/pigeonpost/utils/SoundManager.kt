package com.pigeonpost.utils

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sound effects for the PigeonPost app.
 * Plays medieval/pigeon-themed sounds for various events.
 *
 * Note: The raw audio resource files (pigeon_coo.mp3, wing_flaps.mp3, etc.)
 * must be added to app/src/main/res/raw/ to enable actual sound playback.
 * Currently references are commented out to allow compilation without audio files.
 */
@Singleton
class SoundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var mediaPlayer: MediaPlayer? = null

    /**
     * Play pigeon cooing sound (when sending a message).
     */
    fun playPigeonCoo() {
        playSound(SOUND_PIGEON_COO)
    }

    /**
     * Play wing flapping sound (during pigeon flight).
     */
    fun playWingFlaps() {
        playSound(SOUND_WING_FLAPS)
    }

    /**
     * Play delivery chime sound (when message is delivered).
     */
    fun playDeliveryChime() {
        playSound(SOUND_DELIVERY_CHIME)
    }

    /**
     * Play sad tone sound (when pigeon dies).
     */
    fun playDeathSound() {
        playSound(SOUND_PIGEON_DEATH)
    }

    /**
     * Play a scroll unrolling sound (when opening a conversation).
     */
    fun playScrollOpen() {
        playSound(SOUND_SCROLL_OPEN)
    }

    private fun playSound(soundId: Int) {
        try {
            release()
            // Only play if the resource ID is valid (actual audio files present)
            if (soundId != 0) {
                mediaPlayer = MediaPlayer.create(context, soundId)
                mediaPlayer?.setOnCompletionListener { mp ->
                    mp.release()
                    mediaPlayer = null
                }
                mediaPlayer?.start()
            }
        } catch (e: Exception) {
            // Silently fail if sound resources are not available
            mediaPlayer = null
        }
    }

    /**
     * Release the media player resources.
     */
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    companion object {
        // Resource IDs for sound effects
        // These reference R.raw resources that would need actual audio files:
        // - res/raw/pigeon_coo.mp3
        // - res/raw/wing_flaps.mp3
        // - res/raw/delivery_chime.mp3
        // - res/raw/pigeon_death.mp3
        // - res/raw/scroll_open.mp3
        // Using 0 as placeholder since actual files are not present
        const val SOUND_PIGEON_COO = 0
        const val SOUND_WING_FLAPS = 0
        const val SOUND_DELIVERY_CHIME = 0
        const val SOUND_PIGEON_DEATH = 0
        const val SOUND_SCROLL_OPEN = 0
    }
}
