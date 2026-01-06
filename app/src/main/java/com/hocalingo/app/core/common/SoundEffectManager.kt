package com.hocalingo.app.core.common

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.hocalingo.app.R
import com.hocalingo.app.core.common.DebugHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sound Effect Manager for HocaLingo
 *
 * Manages short audio feedback sounds:
 * - Card flip sound
 * - Success/error sounds (future)
 * - Achievement sounds (future)
 *
 * Performance:
 * - Sounds are pre-loaded in memory (~50KB)
 * - Playback is asynchronous (doesn't block UI)
 * - Proper cleanup to prevent memory leaks
 */
@Singleton
class SoundEffectManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var cardFlipPlayer: MediaPlayer? = null
    private var isInitialized = false

    // Sound state tracking
    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    init {
        DebugHelper.log("=== SoundEffectManager BAŞLATILIYOR ===")
        initializeSounds()
    }

    /**
     * Initialize and pre-load sound effects
     */
    private fun initializeSounds() {
        try {
            // Card flip sound
            cardFlipPlayer = MediaPlayer.create(context, R.raw.card_flip)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setVolume(0.7f, 0.7f) // 70% volume for subtle effect
                setOnCompletionListener {
                    // Reset to start for next play
                    seekTo(0)
                }
            }

            isInitialized = cardFlipPlayer != null

            if (isInitialized) {
                DebugHelper.log("✅ Sound effects initialized successfully")
            } else {
                DebugHelper.logError("❌ Failed to initialize sound effects")
            }

        } catch (e: Exception) {
            DebugHelper.logError("Sound initialization exception", e)
            isInitialized = false
        }
    }

    /**
     * Play card flip sound
     * Called when user taps on study card
     */
    fun playCardFlip() {
        if (!isInitialized || !_isSoundEnabled.value) {
            DebugHelper.log("Sound disabled or not initialized")
            return
        }

        try {
            cardFlipPlayer?.let { player ->
                if (player.isPlaying) {
                    player.seekTo(0) // Restart if already playing
                }
                player.start()
                DebugHelper.log("🔊 Card flip sound played")
            }
        } catch (e: Exception) {
            DebugHelper.logError("Error playing card flip sound", e)
        }
    }

    /**
     * Enable or disable sound effects
     * Synced with user preferences
     */
    fun setSoundEnabled(enabled: Boolean) {
        _isSoundEnabled.value = enabled
        DebugHelper.log("Sound effects ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Cleanup resources
     * Must be called when app is destroyed
     */
    fun cleanup() {
        try {
            cardFlipPlayer?.release()
            cardFlipPlayer = null
            isInitialized = false
            DebugHelper.log("Sound effects cleanup completed")
        } catch (e: Exception) {
            DebugHelper.logError("Sound cleanup exception", e)
        }
    }
}