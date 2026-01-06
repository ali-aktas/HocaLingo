package com.hocalingo.app.di

import android.content.Context
import com.hocalingo.app.core.common.SoundEffectManager
import com.hocalingo.app.core.common.TextToSpeechManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Audio Module - Dependency Injection for Audio Features
 *
 * Provides:
 * - SoundEffectManager: Short sound effects (card flip, etc.)
 * - TextToSpeechManager: Already provided elsewhere, just documenting
 *
 * Scope: Singleton (app-wide single instance)
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    /**
     * Provide SoundEffectManager
     *
     * Singleton ensures:
     * - Single MediaPlayer instance (memory efficient)
     * - Pre-loaded sounds ready to play
     * - Consistent state across app
     */
    @Provides
    @Singleton
    fun provideSoundEffectManager(
        @ApplicationContext context: Context
    ): SoundEffectManager {
        return SoundEffectManager(context)
    }
}