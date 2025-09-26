package com.hocalingo.app.feature.home

/**
 * Updated UI State for Home Dashboard - v2.0
 * Modern gereksinimlere göre güncellenmiş home state yapısı
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val userName: String = "",
    val streakDays: Int = 0,
    val dailyGoalProgress: DailyGoalProgress = DailyGoalProgress(),
    val monthlyStats: MonthlyStats = MonthlyStats(),
    val error: String? = null
)

/**
 * Updated Daily Goal Progress - FIXED Logic
 * Günlük çalışılabilir kartlar ve deck bilgisi için ayrı mantık
 */
data class DailyGoalProgress(
    // Günlük kartlar (bugün çalışılabilir)
    val todayAvailableCards: Int = 0,       // Bugün çalışılabilir kart sayısı
    val todayCompletedCards: Int = 0,       // Bugün çalışılıp ertesi güne ertelenen

    // Deck bilgisi (sadece info, progress yok)
    val totalDeckCards: Int = 0,            // Deck'te toplam seçilmiş kart sayısı
    val masteredDeckCards: Int = 0          // Deck'te mastered olan kartlar
) {
    val todayProgress: Float get() =
        if (todayAvailableCards > 0) todayCompletedCards.toFloat() / todayAvailableCards else 0f

    val isDailyGoalComplete: Boolean get() =
        todayAvailableCards > 0 && todayCompletedCards >= todayAvailableCards

    // Deck info için sadece sayılar, progress yok
}

/**
 * Monthly Statistics - Bu Ay Verileri
 * Streak, aktif günler, disiplin puanı ve chart data
 */
data class MonthlyStats(
    val studyTimeMinutes: Int = 0,          // Bu ay toplam çalışma süresi (dakika)
    val activeDaysThisMonth: Int = 0,       // Bu ay aktif gün sayısı
    val disciplineScore: Int = 0,           // Disiplin puanı (0-100)
    val chartData: List<ChartDataPoint> = emptyList()  // 7 günlük trend data
) {
    val studyTimeFormatted: String get() {
        val hours = studyTimeMinutes / 60
        val minutes = studyTimeMinutes % 60
        return when {
            hours > 0 -> "${hours}.${(minutes * 100 / 60).toString().padStart(2, '0')} saat"
            else -> "$minutes dakika"
        }
    }
}

/**
 * Chart Data Point for Monthly Statistics Graph
 */
data class ChartDataPoint(
    val day: String,        // "1", "2", ..., "30"
    val value: Float        // 0.0 - 1.0 normalized value
)

/**
 * User Events - Updated for New Features
 */
sealed interface HomeEvent {
    data object LoadDashboardData : HomeEvent
    data object RefreshData : HomeEvent
    data object StartStudy : HomeEvent
    data object NavigateToPackageSelection : HomeEvent
    data object NavigateToAIAssistant : HomeEvent
}

/**
 * Navigation Effects - Updated Routes
 */
sealed interface HomeEffect {
    data object NavigateToStudy : HomeEffect
    data object NavigateToPackageSelection : HomeEffect
    data object NavigateToAIAssistant : HomeEffect
    data class ShowMessage(val message: String) : HomeEffect
    data class ShowError(val error: String) : HomeEffect
}