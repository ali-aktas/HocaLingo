package com.hocalingo.app.feature.home.domain

import com.hocalingo.app.feature.home.presentation.DailyGoalProgress
import com.hocalingo.app.feature.home.presentation.TodayWord
import com.hocalingo.app.feature.home.presentation.WeeklyStats

/**
 * Repository interface for Home dashboard data
 */
interface HomeRepository {
    suspend fun getDashboardData(): HomeDashboardData
    suspend fun markWordAsStudied(wordId: String)
    suspend fun updateDailyGoal(targetWords: Int, targetMinutes: Int)
}

/**
 * Dashboard data container
 */
data class HomeDashboardData(
    val streakDays: Int,
    val dailyGoalProgress: DailyGoalProgress,
    val todayWords: List<TodayWord>,
    val weeklyStats: WeeklyStats
)