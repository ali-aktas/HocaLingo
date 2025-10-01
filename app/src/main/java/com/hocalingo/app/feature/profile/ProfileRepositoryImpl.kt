package com.hocalingo.app.feature.profile

import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.common.ThemeMode
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.base.toAppError
import com.hocalingo.app.database.HocaLingoDatabase
import com.hocalingo.app.database.entities.StudyDirection as DbStudyDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Profile Repository Implementation - Fixed with Notification Support
 * ✅ Selected words with pagination
 * ✅ User statistics calculation
 * ✅ User preferences management
 * ✅ Notification settings save/load - FIXED!
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
            // Get various stats from database
            val totalSelected = database.combinedDataDao().getTotalSelectedWordsCount()
            val masteredCount = database.combinedDataDao().getMasteredWordsCount(DbStudyDirection.EN_TO_TR)

            // Mock data for now - can be enhanced later
            val userStats = UserStats(
                totalWordsSelected = totalSelected,
                wordsStudiedToday = 5, // TODO: Implement actual today's study count
                masteredWordsCount = masteredCount,
                currentStreak = database.dailyStatsDao()
                    .getStatsByDate("user_1", dateFormat.format(Date()))
                    ?.streakCount ?: 0,
                studyTimeThisWeek = 120, // TODO: Implement actual study time tracking
                averageAccuracy = 0.85f // TODO: Implement accuracy calculation
            )

            Result.Success(userStats)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun getUserPreferences(): Result<UserPreferences> = withContext(Dispatchers.IO) {
        try {
            // Get all user preferences
            val themeMode = userPreferencesManager.getThemeMode().first()
            val studyDirection = userPreferencesManager.getStudyDirection().first()
            val notificationsEnabled = userPreferencesManager.areNotificationsEnabled().first()
            val soundEnabled = userPreferencesManager.isSoundEnabled().first()
            val dailyGoal = 20 // TODO: Implement daily goal preference

            val preferences = UserPreferences(
                themeMode = themeMode,
                studyDirection = studyDirection,
                notificationsEnabled = notificationsEnabled,
                soundEnabled = soundEnabled,
                dailyGoal = dailyGoal
            )

            Result.Success(preferences)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            when (val result = userPreferencesManager.setThemeMode(themeMode)) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> result
            }
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun updateStudyDirection(direction: StudyDirection): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            when (val result = userPreferencesManager.setStudyDirection(direction)) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> result
            }
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    // ✅ FIXED: Missing method implementation!
    override suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            when (val result = userPreferencesManager.setNotificationsEnabled(enabled)) {
                is Result.Success -> Result.Success(Unit)
                is Result.Error -> result
            }
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Helper method to get package display name
     */
    private suspend fun getPackageDisplayName(packageId: String?): String? {
        return try {
            if (packageId == null) return null
            val packageEntity = database.wordPackageDao().getPackageById(packageId)
            packageEntity?.description ?: packageEntity?.level
        } catch (e: Exception) {
            null
        }
    }
}