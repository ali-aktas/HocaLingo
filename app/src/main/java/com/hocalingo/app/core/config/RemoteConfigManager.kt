package com.hocalingo.app.core.config

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import com.hocalingo.app.R
import com.hocalingo.app.core.common.DebugHelper
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RemoteConfigManager - Firebase Remote Config Wrapper
 *
 * Package: app/src/main/java/com/hocalingo/app/core/config/
 *
 * Manages Firebase Remote Config for:
 * - API keys (Gemini)
 * - Feature flags
 * - Dynamic configuration
 *
 * Keys:
 * - "gemini_api_key" → API key for Gemini AI
 * - "daily_story_limit" → Max stories per day (default: 2)
 * - "enable_story_feature" → Feature toggle (default: true)
 *
 * Usage:
 * ```kotlin
 * val apiKey = remoteConfigManager.getGeminiApiKey()
 * val limit = remoteConfigManager.getDailyStoryLimit()
 * ```
 */
@Singleton
class RemoteConfigManager @Inject constructor() {

    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()

    companion object {
        // Remote Config Keys
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_DAILY_STORY_LIMIT = "daily_story_limit"
        private const val KEY_ENABLE_STORY_FEATURE = "enable_story_feature"

        // Default Values
        private const val DEFAULT_DAILY_LIMIT = 2
        private const val DEFAULT_FEATURE_ENABLED = true

        // Fetch interval
        private const val FETCH_INTERVAL_SECONDS = 3600L // 1 hour
    }

    init {
        setupRemoteConfig()
    }

    /**
     * Setup Firebase Remote Config
     * - Set defaults from XML
     * - Configure fetch interval
     */
    private fun setupRemoteConfig() {
        try {
            val configSettings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (com.hocalingo.app.BuildConfig.DEBUG) {
                    0 // Immediate fetch in debug
                } else {
                    FETCH_INTERVAL_SECONDS // 1 hour in production
                }
            }

            remoteConfig.setConfigSettingsAsync(configSettings)

            // Set default values (fallback)
            remoteConfig.setDefaultsAsync(
                mapOf(
                    KEY_GEMINI_API_KEY to "",
                    KEY_DAILY_STORY_LIMIT to DEFAULT_DAILY_LIMIT,
                    KEY_ENABLE_STORY_FEATURE to DEFAULT_FEATURE_ENABLED
                )
            )

            DebugHelper.log("🔧 Remote Config initialized")
        } catch (e: Exception) {
            DebugHelper.logError("Remote Config setup failed", e)
        }
    }

    /**
     * Fetch latest config from Firebase
     * Call this on app start or when needed
     */
    suspend fun fetchAndActivate(): Boolean {
        return try {
            DebugHelper.log("🔄 Fetching Remote Config...")
            val activated = remoteConfig.fetchAndActivate().await()
            if (activated) {
                DebugHelper.logSuccess("✅ Remote Config updated")
            } else {
                DebugHelper.log("ℹ️  Remote Config unchanged")
            }
            activated
        } catch (e: Exception) {
            DebugHelper.logError("Remote Config fetch failed", e)
            false
        }
    }

    /**
     * Get Gemini API key
     *
     * @return API key from Remote Config
     * @throws IllegalStateException if key is empty
     */
    fun getGeminiApiKey(): String {
        val key = remoteConfig.getString(KEY_GEMINI_API_KEY)

        if (key.isBlank()) {
            DebugHelper.logError("⚠️  Gemini API key is empty in Remote Config!")
            throw IllegalStateException("Gemini API key not configured. Please set '$KEY_GEMINI_API_KEY' in Firebase Remote Config.")
        }

        return key
    }

    /**
     * Get daily story generation limit
     * Default: 2 stories per day
     */
    fun getDailyStoryLimit(): Int {
        return remoteConfig.getLong(KEY_DAILY_STORY_LIMIT).toInt()
    }

    /**
     * Check if story feature is enabled
     * Useful for A/B testing or gradual rollout
     */
    fun isStoryFeatureEnabled(): Boolean {
        return remoteConfig.getBoolean(KEY_ENABLE_STORY_FEATURE)
    }

    /**
     * Get all config info (debug only)
     */
    fun getDebugInfo(): Map<String, Any> {
        return mapOf(
            "gemini_api_key_set" to getGeminiApiKey().isNotBlank(),
            "daily_story_limit" to getDailyStoryLimit(),
            "story_feature_enabled" to isStoryFeatureEnabled(),
            "last_fetch_status" to remoteConfig.info.lastFetchStatus,
            "last_fetch_time" to remoteConfig.info.fetchTimeMillis
        )
    }
}