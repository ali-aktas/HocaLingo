package com.hocalingo.app.feature.home.presentation

/**
 * UI State for Home Dashboard
 * Central hub için gerekli tüm data
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val streakDays: Int = 0,
    val dailyGoalProgress: DailyGoalProgress = DailyGoalProgress(),
    val todayWords: List<TodayWord> = emptyList(),
    val weeklyStats: WeeklyStats = WeeklyStats(),
    val quickActions: List<QuickAction> = emptyList(),
    val error: String? = null
)

/**
 * Daily goal progress tracking
 */
data class DailyGoalProgress(
    val targetWords: Int = 10,
    val completedWords: Int = 0,
    val targetMinutes: Int = 15,
    val completedMinutes: Int = 0
) {
    val wordsProgress: Float get() = if (targetWords > 0) completedWords.toFloat() / targetWords else 0f
    val minutesProgress: Float get() = if (targetMinutes > 0) completedMinutes.toFloat() / targetMinutes else 0f
    val isCompleted: Boolean get() = completedWords >= targetWords && completedMinutes >= targetMinutes
}

/**
 * Today's word for quick study
 */
data class TodayWord(
    val id: String,
    val english: String,
    val turkish: String,
    val level: String,
    val isLearned: Boolean = false
)

/**
 * Weekly statistics overview
 */
data class WeeklyStats(
    val totalWords: Int = 0,
    val totalMinutes: Int = 0,
    val accuracyRate: Float = 0f,
    val daysActive: Int = 0
)

/**
 * Quick action items for dashboard
 */
data class QuickAction(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val action: QuickActionType
)

enum class QuickActionType {
    START_STUDY,
    ADD_WORD,
    VIEW_PROGRESS,
    DAILY_CHALLENGE
}

/**
 * User events from Home screen
 */
sealed interface HomeEvent {
    data object LoadDashboardData : HomeEvent
    data object StartStudy : HomeEvent
    data object AddWord : HomeEvent
    data object ViewProgress : HomeEvent
    data class QuickWordStudy(val wordId: String) : HomeEvent
    data object RefreshData : HomeEvent
}

/**
 * One-time effects for navigation
 */
sealed interface HomeEffect {
    data object NavigateToStudy : HomeEffect
    data object NavigateToAddWord : HomeEffect
    data object NavigateToProfile : HomeEffect
    data class ShowMessage(val message: String) : HomeEffect
}