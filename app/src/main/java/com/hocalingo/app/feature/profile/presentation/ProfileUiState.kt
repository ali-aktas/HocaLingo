package com.hocalingo.app.feature.profile.presentation

import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.common.ThemeMode
import com.hocalingo.app.feature.profile.domain.UserStats
import com.hocalingo.app.feature.profile.domain.WordSummary

/**
 * Profile UI State
 * Modern, clean state management for profile screen
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    // User identity
    val userName: String = "Ali",

    // Selected words preview (5 words max)
    val selectedWordsPreview: List<WordSummary> = emptyList(),
    val totalWordsCount: Int = 0,

    // User statistics
    val userStats: UserStats = UserStats(
        totalWordsSelected = 0,
        wordsStudiedToday = 0,
        masteredWordsCount = 0,
        currentStreak = 0,
        studyTimeThisWeek = 0,
        averageAccuracy = 0.0f
    ),

    // User preferences
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val studyDirection: StudyDirection = StudyDirection.EN_TO_TR,
    val notificationsEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val dailyGoal: Int = 20
) {
    /**
     * Formatted study time for display
     */
    val studyTimeFormatted: String get() {
        val hours = userStats.studyTimeThisWeek / 60
        val minutes = userStats.studyTimeThisWeek % 60
        return when {
            hours > 0 -> "${hours}sa ${minutes}dk"
            else -> "${minutes} dakika"
        }
    }

    /**
     * Accuracy percentage for display
     */
    val accuracyPercentage: Int get() {
        return (userStats.averageAccuracy * 100).toInt()
    }

    /**
     * Study direction display text
     */
    val studyDirectionText: String get() = when (studyDirection) {
        StudyDirection.EN_TO_TR -> "İngilizce → Türkçe"
        StudyDirection.TR_TO_EN -> "Türkçe → İngilizce"
        StudyDirection.MIXED -> "Karışık (Rastgele)"
    }

    /**
     * Theme mode display text
     */
    val themeModeText: String get() = when (themeMode) {
        ThemeMode.LIGHT -> "Açık Tema"
        ThemeMode.DARK -> "Koyu Tema"
        ThemeMode.SYSTEM -> "Sistem Teması"
    }
}
