package com.hocalingo.app.feature.profile

import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.common.ThemeMode

/**
 * Profile Repository Interface
 * Handles user profile data, selected words, and user statistics
 */
interface ProfileRepository {

    /**
     * Get user's selected words for preview (first 5)
     */
    suspend fun getSelectedWordsPreview(): Result<List<WordSummary>>

    /**
     * Get user's selected words with pagination
     * @param offset Starting position for pagination
     * @param limit Number of words to fetch (default 20)
     */
    suspend fun getSelectedWords(offset: Int = 0, limit: Int = 20): Result<List<WordSummary>>

    /**
     * Get total count of selected words
     */
    suspend fun getTotalSelectedWordsCount(): Result<Int>

    /**
     * Get user statistics summary
     */
    suspend fun getUserStats(): Result<UserStats>

    /**
     * Get user preferences for profile settings
     */
    suspend fun getUserPreferences(): Result<UserPreferences>

    /**
     * Update theme mode
     */
    suspend fun updateThemeMode(themeMode: ThemeMode): Result<Unit>

    /**
     * Update study direction
     */
    suspend fun updateStudyDirection(direction: StudyDirection): Result<Unit>

    /**
     * Update notifications setting
     */
    suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit>
}

/**
 * Word Summary for profile display
 * Lightweight version of ConceptEntity for list display
 */
data class WordSummary(
    val id: Int,
    val english: String,
    val turkish: String,
    val level: String,
    val isMastered: Boolean = false,
    val packageName: String? = null
)

/**
 * User Statistics for profile overview
 */
data class UserStats(
    val totalWordsSelected: Int,
    val wordsStudiedToday: Int,
    val masteredWordsCount: Int,
    val currentStreak: Int,
    val studyTimeThisWeek: Int, // minutes
    val averageAccuracy: Float // 0.0 - 1.0
)

/**
 * User Preferences for profile settings
 */
data class UserPreferences(
    val themeMode: ThemeMode,
    val studyDirection: StudyDirection,
    val notificationsEnabled: Boolean,
    val dailyGoal: Int,
    val soundEnabled: Boolean
)