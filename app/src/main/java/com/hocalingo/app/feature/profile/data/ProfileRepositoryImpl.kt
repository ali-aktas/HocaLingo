package com.hocalingo.app.feature.profile.data

import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.common.ThemeMode
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.core.common.base.toAppError
import com.hocalingo.app.core.database.HocaLingoDatabase
import com.hocalingo.app.core.database.entities.StudyDirection as DbStudyDirection
import com.hocalingo.app.feature.profile.domain.ProfileRepository
import com.hocalingo.app.feature.profile.domain.UserPreferences
import com.hocalingo.app.feature.profile.domain.UserStats
import com.hocalingo.app.feature.profile.domain.WordSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Profile Repository Implementation
 * ✅ Selected words with pagination
 * ✅ User statistics calculation
 * ✅ User preferences management
 */
@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val database: HocaLingoDatabase,
    private val userPreferencesManager: UserPreferencesManager
) : ProfileRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override suspend fun getSelectedWordsPreview(): Result<List<WordSummary>> = withContext(Dispatchers.IO) {
        try {
            // Get first 5 selected words for preview
            val conceptsWithProgress = database.combinedDataDao().getSelectedWordsWithProgress(limit = 5)

            val wordSummaries = conceptsWithProgress.map { conceptData ->
                WordSummary(
                    id = conceptData.id,
                    english = conceptData.english,
                    turkish = conceptData.turkish,
                    level = conceptData.level,
                    isMastered = conceptData.isMastered ?: false,
                    packageName = getPackageDisplayName(conceptData.packageId)
                )
            }

            Result.Success(wordSummaries)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun getSelectedWords(offset: Int, limit: Int): Result<List<WordSummary>> = withContext(Dispatchers.IO) {
        try {
            // Get selected words with pagination
            val conceptsWithProgress = database.combinedDataDao()
                .getSelectedWordsWithProgressPaginated(offset = offset, limit = limit)

            val wordSummaries = conceptsWithProgress.map { conceptData ->
                WordSummary(
                    id = conceptData.id,
                    english = conceptData.english,
                    turkish = conceptData.turkish,
                    level = conceptData.level,
                    isMastered = conceptData.isMastered ?: false,
                    packageName = getPackageDisplayName(conceptData.packageId)
                )
            }

            Result.Success(wordSummaries)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun getTotalSelectedWordsCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val count = database.combinedDataDao().getTotalSelectedWordsCount()
            Result.Success(count)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun getUserStats(): Result<UserStats> = withContext(Dispatchers.IO) {
        try {
            // Get total selected words
            val totalWords = database.combinedDataDao().getTotalSelectedWordsCount()

            // Get mastered words count
            val masteredCount = database.combinedDataDao().getMasteredWordsCount(DbStudyDirection.EN_TO_TR) +
                    database.combinedDataDao().getMasteredWordsCount(DbStudyDirection.TR_TO_EN)

            // Get today's words studied from DailyStats
            val today = dateFormat.format(Date())
            val todayStats = database.dailyStatsDao().getStatsByDate("user_1", today)
            val wordsStudiedToday = todayStats?.wordsStudied ?: 0
            val currentStreak = todayStats?.streakCount ?: 0

            // Get this week's study time (in minutes)
            val weekStart = getStartOfWeek()
            val studyTimeMs = database.studySessionDao().getTotalStudyTimeSince(weekStart)
            val studyTimeMinutes = (studyTimeMs / (1000 * 60)).toInt()

            // Calculate average accuracy from recent sessions
            val averageAccuracy = database.studySessionDao().getAverageAccuracySince(weekStart)

            val userStats = UserStats(
                totalWordsSelected = totalWords,
                wordsStudiedToday = wordsStudiedToday,
                masteredWordsCount = masteredCount,
                currentStreak = currentStreak,
                studyTimeThisWeek = studyTimeMinutes,
                averageAccuracy = averageAccuracy
            )

            Result.Success(userStats)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun getUserPreferences(): Result<UserPreferences> = try {
        val themeMode = userPreferencesManager.getThemeMode().first()
        val studyDirection = userPreferencesManager.getStudyDirection().first()
        val notificationsEnabled = userPreferencesManager.areNotificationsEnabled().first()
        val dailyGoal = userPreferencesManager.getDailyGoal().first()
        val soundEnabled = userPreferencesManager.isSoundEnabled().first()

        val preferences = UserPreferences(
            themeMode = themeMode,
            studyDirection = studyDirection,
            notificationsEnabled = notificationsEnabled,
            dailyGoal = dailyGoal,
            soundEnabled = soundEnabled
        )

        Result.Success(preferences)
    } catch (e: Exception) {
        Result.Error(e.toAppError())
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode): Result<Unit> {
        return userPreferencesManager.setThemeMode(themeMode)
    }

    override suspend fun updateStudyDirection(direction: StudyDirection): Result<Unit> {
        return userPreferencesManager.setStudyDirection(direction)
    }

    override suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit> {
        return userPreferencesManager.setNotificationsEnabled(enabled)
    }

    // Private helper methods

    private fun getPackageDisplayName(packageId: String?): String? {
        return when {
            packageId == null -> null
            packageId.contains("a1") -> "A1 Temel"
            packageId.contains("a2") -> "A2 Temel"
            packageId.contains("b1") -> "B1 Orta"
            packageId.contains("b2") -> "B2 Orta-Üst"
            packageId.contains("test") -> "Test Paketi"
            else -> "Kelime Paketi"
        }
    }

    private fun getStartOfWeek(): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}