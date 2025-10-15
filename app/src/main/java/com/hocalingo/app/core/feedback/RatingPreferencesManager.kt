package com.hocalingo.app.core.feedback

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RatingPreferencesManager
 * ========================
 * Manages rating prompt timing and state persistence
 *
 * Responsibilities:
 * - Store prompt history (when, how many times)
 * - Track user opt-out
 * - Manage cooldown periods
 *
 * Package: app/src/main/java/com/hocalingo/app/core/feedback/
 */

private val Context.ratingDataStore by preferencesDataStore(name = "rating_preferences")

@Singleton
class RatingPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.ratingDataStore

    // Keys
    private object PreferencesKeys {
        val TOTAL_PROMPTS_SHOWN = intPreferencesKey("total_prompts_shown")
        val LAST_PROMPT_TIMESTAMP = longPreferencesKey("last_prompt_timestamp")
        val LAST_PROMPT_YEAR = intPreferencesKey("last_prompt_year")
        val HAS_USER_RATED = booleanPreferencesKey("has_user_rated")
        val USER_OPTED_OUT = booleanPreferencesKey("user_opted_out")
        val COMPLETED_STUDY_SESSIONS = intPreferencesKey("completed_study_sessions")
    }

    // ========== READ OPERATIONS ==========

    /**
     * Get current rating prompt state
     */
    val ratingPromptState: Flow<RatingPromptState> = dataStore.data.map { prefs ->
        RatingPromptState(
            totalPromptsShown = prefs[PreferencesKeys.TOTAL_PROMPTS_SHOWN] ?: 0,
            lastPromptTimestamp = prefs[PreferencesKeys.LAST_PROMPT_TIMESTAMP] ?: 0L,
            lastPromptYear = prefs[PreferencesKeys.LAST_PROMPT_YEAR] ?: 0,
            hasUserRatedBefore = prefs[PreferencesKeys.HAS_USER_RATED] ?: false,
            userOptedOut = prefs[PreferencesKeys.USER_OPTED_OUT] ?: false
        )
    }

    /**
     * Get completed study sessions count
     */
    val completedStudySessions: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PreferencesKeys.COMPLETED_STUDY_SESSIONS] ?: 0
    }

    // ========== WRITE OPERATIONS ==========

    /**
     * Increment study sessions count
     * Called after each successful study session
     */
    suspend fun incrementStudySessions() {
        dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.COMPLETED_STUDY_SESSIONS] ?: 0
            prefs[PreferencesKeys.COMPLETED_STUDY_SESSIONS] = current + 1
        }
    }

    /**
     * Mark that prompt was shown
     * Updates counters and timestamp
     */
    suspend fun markPromptShown() {
        dataStore.edit { prefs ->
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
            val lastYear = prefs[PreferencesKeys.LAST_PROMPT_YEAR] ?: 0
            val currentCount = prefs[PreferencesKeys.TOTAL_PROMPTS_SHOWN] ?: 0

            // Reset count if new year
            val newCount = if (currentYear > lastYear) 1 else currentCount + 1

            prefs[PreferencesKeys.TOTAL_PROMPTS_SHOWN] = newCount
            prefs[PreferencesKeys.LAST_PROMPT_TIMESTAMP] = System.currentTimeMillis()
            prefs[PreferencesKeys.LAST_PROMPT_YEAR] = currentYear
        }
    }

    /**
     * Mark that user has rated the app
     * Prevents future prompts
     */
    suspend fun markUserRated() {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.HAS_USER_RATED] = true
        }
    }

    /**
     * Mark that user opted out
     * User explicitly declined to rate
     */
    suspend fun markUserOptedOut() {
        dataStore.edit { prefs ->
            prefs[PreferencesKeys.USER_OPTED_OUT] = true
        }
    }

    /**
     * Reset all rating preferences
     * (For testing or app reset)
     */
    suspend fun resetRatingPreferences() {
        dataStore.edit { prefs ->
            prefs.remove(PreferencesKeys.TOTAL_PROMPTS_SHOWN)
            prefs.remove(PreferencesKeys.LAST_PROMPT_TIMESTAMP)
            prefs.remove(PreferencesKeys.LAST_PROMPT_YEAR)
            prefs.remove(PreferencesKeys.HAS_USER_RATED)
            prefs.remove(PreferencesKeys.USER_OPTED_OUT)
            prefs.remove(PreferencesKeys.COMPLETED_STUDY_SESSIONS)
        }
    }
}