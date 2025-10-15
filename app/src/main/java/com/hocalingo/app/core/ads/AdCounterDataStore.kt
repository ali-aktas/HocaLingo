package com.hocalingo.app.core.ads

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hocalingo.app.core.common.DebugHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AdCounterDataStore - Reklam Counter Yönetimi
 *
 * Package: app/src/main/java/com/hocalingo/app/core/ads/
 *
 * Reklam gösterim sayaçlarını ve timing'lerini DataStore'da saklar.
 * - App launch counter (3-4 açılışta 1 reklam)
 * - Study word counter (25 kelimede 1 reklam)
 * - Son gösterilen reklam zamanları (cooldown mantığı için)
 */
@Singleton
class AdCounterDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val Context.adCounterDataStore: DataStore<Preferences> by preferencesDataStore(
        name = "ad_counter_prefs"
    )

    private val dataStore = context.adCounterDataStore

    companion object {
        // App launch counter
        private val APP_LAUNCH_COUNT = intPreferencesKey("ad_app_launch_count")
        private val LAST_LAUNCH_AD_TIME = longPreferencesKey("ad_last_launch_ad_time")

        // Study word counter
        private val STUDY_WORD_COUNT = intPreferencesKey("ad_study_word_count")
        private val LAST_STUDY_AD_TIME = longPreferencesKey("ad_last_study_ad_time")

        // Ad thresholds
        const val APP_LAUNCH_THRESHOLD = 4 // Her 4 açılışta 1 reklam
        const val STUDY_WORD_THRESHOLD = 25 // Her 25 kelimede 1 reklam
        const val COOLDOWN_DURATION_MS = 60_000L // 1 dakika cooldown
    }

    /**
     * ============================================
     * APP LAUNCH COUNTER
     * ============================================
     */

    /**
     * Get app launch count
     */
    suspend fun getAppLaunchCount(): Int {
        return dataStore.data.map { preferences ->
            preferences[APP_LAUNCH_COUNT] ?: 0
        }.first()
    }

    /**
     * Increment app launch count
     */
    suspend fun incrementAppLaunchCount() {
        dataStore.edit { preferences ->
            val currentCount = preferences[APP_LAUNCH_COUNT] ?: 0
            val newCount = currentCount + 1
            preferences[APP_LAUNCH_COUNT] = newCount
            DebugHelper.log("📱 App launch count: $newCount")
        }
    }

    /**
     * Reset app launch count (reklam gösterildikten sonra)
     */
    suspend fun resetAppLaunchCount() {
        dataStore.edit { preferences ->
            preferences[APP_LAUNCH_COUNT] = 0
            preferences[LAST_LAUNCH_AD_TIME] = System.currentTimeMillis()
            DebugHelper.log("🔄 App launch count reset")
        }
    }

    /**
     * Check if should show app launch ad
     */
    suspend fun shouldShowAppLaunchAd(): Boolean {
        val count = getAppLaunchCount()
        val lastAdTime = getLastLaunchAdTime()
        val currentTime = System.currentTimeMillis()

        val reachedThreshold = count >= APP_LAUNCH_THRESHOLD
        val cooldownPassed = (currentTime - lastAdTime) > COOLDOWN_DURATION_MS

        DebugHelper.log("📱 Launch Ad Check: count=$count, threshold=$APP_LAUNCH_THRESHOLD, cooldown=$cooldownPassed")

        return reachedThreshold && cooldownPassed
    }

    /**
     * Get last launch ad shown time
     */
    private suspend fun getLastLaunchAdTime(): Long {
        return dataStore.data.map { preferences ->
            preferences[LAST_LAUNCH_AD_TIME] ?: 0L
        }.first()
    }

    /**
     * ============================================
     * STUDY WORD COUNTER
     * ============================================
     */

    /**
     * Get study word count
     */
    suspend fun getStudyWordCount(): Int {
        return dataStore.data.map { preferences ->
            preferences[STUDY_WORD_COUNT] ?: 0
        }.first()
    }

    /**
     * Increment study word count
     */
    suspend fun incrementStudyWordCount() {
        dataStore.edit { preferences ->
            val currentCount = preferences[STUDY_WORD_COUNT] ?: 0
            val newCount = currentCount + 1
            preferences[STUDY_WORD_COUNT] = newCount
            DebugHelper.log("📚 Study word count: $newCount")
        }
    }

    /**
     * Reset study word count (reklam gösterildikten sonra)
     */
    suspend fun resetStudyWordCount() {
        dataStore.edit { preferences ->
            preferences[STUDY_WORD_COUNT] = 0
            preferences[LAST_STUDY_AD_TIME] = System.currentTimeMillis()
            DebugHelper.log("🔄 Study word count reset")
        }
    }

    /**
     * Check if should show study rewarded ad
     */
    suspend fun shouldShowStudyRewardedAd(): Boolean {
        val count = getStudyWordCount()
        val lastAdTime = getLastStudyAdTime()
        val currentTime = System.currentTimeMillis()

        val reachedThreshold = count >= STUDY_WORD_THRESHOLD
        val cooldownPassed = (currentTime - lastAdTime) > COOLDOWN_DURATION_MS

        DebugHelper.log("📚 Study Ad Check: count=$count, threshold=$STUDY_WORD_THRESHOLD, cooldown=$cooldownPassed")

        return reachedThreshold && cooldownPassed
    }

    /**
     * Get last study ad shown time
     */
    private suspend fun getLastStudyAdTime(): Long {
        return dataStore.data.map { preferences ->
            preferences[LAST_STUDY_AD_TIME] ?: 0L
        }.first()
    }

    /**
     * ============================================
     * GENERAL UTILITIES
     * ============================================
     */

    /**
     * Clear all ad counters (logout, testing vs.)
     */
    suspend fun clearAllCounters() {
        dataStore.edit { preferences ->
            preferences.clear()
            DebugHelper.log("🗑️ All ad counters cleared")
        }
    }

    /**
     * Get debug info
     */
    suspend fun getDebugInfo(): String {
        val launchCount = getAppLaunchCount()
        val studyCount = getStudyWordCount()
        val lastLaunchAd = getLastLaunchAdTime()
        val lastStudyAd = getLastStudyAdTime()

        return """
            📊 Ad Counter Debug Info:
            - App Launch Count: $launchCount / $APP_LAUNCH_THRESHOLD
            - Study Word Count: $studyCount / $STUDY_WORD_THRESHOLD
            - Last Launch Ad: ${if (lastLaunchAd > 0) "${(System.currentTimeMillis() - lastLaunchAd) / 1000}s ago" else "Never"}
            - Last Study Ad: ${if (lastStudyAd > 0) "${(System.currentTimeMillis() - lastStudyAd) / 1000}s ago" else "Never"}
        """.trimIndent()
    }
}