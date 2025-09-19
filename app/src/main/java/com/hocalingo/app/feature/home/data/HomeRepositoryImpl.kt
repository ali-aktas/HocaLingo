package com.hocalingo.app.feature.home.data

import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.feature.home.domain.HomeDashboardData
import com.hocalingo.app.feature.home.domain.HomeRepository
import com.hocalingo.app.feature.home.presentation.DailyGoalProgress
import com.hocalingo.app.feature.home.presentation.TodayWord
import com.hocalingo.app.feature.home.presentation.WeeklyStats
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of HomeRepository with mock data
 * TODO: Replace with real data source integration
 */
@Singleton
class HomeRepositoryImpl @Inject constructor() : HomeRepository {

    // Simulated data storage
    private var streakDays = 7
    private var dailyProgress = DailyGoalProgress(
        targetWords = 10,
        completedWords = 3,
        targetMinutes = 15,
        completedMinutes = 8
    )

    override suspend fun getDashboardData(): HomeDashboardData {
        // Simulate network/database delay
        delay(500)

        DebugHelper.log("Loading dashboard data from repository")

        return HomeDashboardData(
            streakDays = streakDays,
            dailyGoalProgress = dailyProgress,
            todayWords = getMockTodayWords(),
            weeklyStats = getMockWeeklyStats()
        )
    }

    override suspend fun markWordAsStudied(wordId: String) {
        DebugHelper.log("Marking word as studied: $wordId")

        // Update daily progress
        dailyProgress = dailyProgress.copy(
            completedWords = dailyProgress.completedWords + 1,
            completedMinutes = dailyProgress.completedMinutes + 2 // Assume 2 minutes per word
        )

        delay(200) // Simulate processing time
    }

    override suspend fun updateDailyGoal(targetWords: Int, targetMinutes: Int) {
        DebugHelper.log("Updating daily goal: $targetWords words, $targetMinutes minutes")

        dailyProgress = dailyProgress.copy(
            targetWords = targetWords,
            targetMinutes = targetMinutes
        )

        delay(200)
    }

    private fun getMockTodayWords(): List<TodayWord> {
        return listOf(
            TodayWord(
                id = "word_1",
                english = "Beautiful",
                turkish = "Güzel",
                level = "A1",
                isLearned = false
            ),
            TodayWord(
                id = "word_2",
                english = "Adventure",
                turkish = "Macera",
                level = "A2",
                isLearned = true
            ),
            TodayWord(
                id = "word_3",
                english = "Consequence",
                turkish = "Sonuç",
                level = "B1",
                isLearned = false
            ),
            TodayWord(
                id = "word_4",
                english = "Magnificent",
                turkish = "Muhteşem",
                level = "B2",
                isLearned = false
            ),
            TodayWord(
                id = "word_5",
                english = "Perseverance",
                turkish = "Sebat",
                level = "C1",
                isLearned = false
            )
        )
    }

    private fun getMockWeeklyStats(): WeeklyStats {
        return WeeklyStats(
            totalWords = 47,
            totalMinutes = 125,
            accuracyRate = 0.87f, // 87%
            daysActive = 5
        )
    }
}