package com.hocalingo.app.feature.notification.data

import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.core.common.base.toAppError
import com.hocalingo.app.core.database.HocaLingoDatabase
import com.hocalingo.app.feature.notification.NotificationRepository
import com.hocalingo.app.feature.notification.NotificationStats
import com.hocalingo.app.feature.profile.domain.WordSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification Repository Implementation
 * ✅ Smart word selection for notifications
 * ✅ Overdue words priority
 * ✅ Random fallback from selected words
 * ✅ Fixed property names and null handling
 */
@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val database: HocaLingoDatabase,
    private val userPreferencesManager: UserPreferencesManager
) : NotificationRepository {

    override suspend fun getWordForNotification(): Result<WordSummary?> = withContext(Dispatchers.IO) {
        try {
            // 1. First, try to get overdue words (due for review)
            val overdueWord = getOverdueWordForNotification()
            if (overdueWord != null) {
                return@withContext Result.Success(overdueWord)
            }

            // 2. Fallback: Get random selected word
            val randomWord = getRandomSelectedWord()
            if (randomWord != null) {
                return@withContext Result.Success(randomWord)
            }

            // 3. No words available
            Result.Success(null)

        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun areNotificationsEnabled(): Boolean {
        return try {
            userPreferencesManager.areNotificationsEnabled().first()
        } catch (e: Exception) {
            false // Default to false if error
        }
    }

    override suspend fun recordNotificationSent(wordId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // For now, we'll just log this
            // In future versions, we can add a notifications_log table
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    override suspend fun getNotificationStats(): Result<NotificationStats> = withContext(Dispatchers.IO) {
        try {
            // Placeholder implementation
            Result.Success(
                NotificationStats(
                    totalNotificationsSent = 0,
                    notificationsClickedCount = 0,
                    lastNotificationSentAt = null,
                    averageResponseTime = null
                )
            )
        } catch (e: Exception) {
            Result.Error(e.toAppError())
        }
    }

    /**
     * Get overdue word for notification
     * Priority for spaced repetition system
     */
    private suspend fun getOverdueWordForNotification(): WordSummary? {
        return try {
            val currentTime = System.currentTimeMillis()
            val studyDirection = userPreferencesManager.getStudyDirection().first()

            // Convert to database enum
            val dbDirection = when (studyDirection) {
                StudyDirection.EN_TO_TR -> com.hocalingo.app.core.database.entities.StudyDirection.EN_TO_TR
                StudyDirection.TR_TO_EN -> com.hocalingo.app.core.database.entities.StudyDirection.TR_TO_EN
                StudyDirection.MIXED -> com.hocalingo.app.core.database.entities.StudyDirection.EN_TO_TR // Default fallback for mixed
            }

            // Get overdue words from study queue
            val overdueWords = database.combinedDataDao().getStudyQueue(
                direction = dbDirection,
                currentTime = currentTime,
                limit = 10 // Get multiple options
            )

            if (overdueWords.isNotEmpty()) {
                val selectedWord = overdueWords.random() // Randomize to avoid always same word
                WordSummary(
                    id = selectedWord.id,
                    english = selectedWord.english,
                    turkish = selectedWord.turkish,
                    level = selectedWord.level,
                    isMastered = false, // Overdue words are not mastered
                    packageName = null
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get random selected word as fallback
     */
    private suspend fun getRandomSelectedWord(): WordSummary? {
        return try {
            // Get a random selection from user's selected words
            val selectedWords = database.combinedDataDao().getSelectedWordsWithProgress(limit = 20)

            if (selectedWords.isNotEmpty()) {
                val selectedWord = selectedWords.random()
                WordSummary(
                    id = selectedWord.id,
                    english = selectedWord.english,
                    turkish = selectedWord.turkish,
                    level = selectedWord.level,
                    isMastered = selectedWord.isMastered ?: false,
                    packageName = getPackageDisplayName(selectedWord.packageId)
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get package display name (helper method)
     * ✅ Fixed: Using 'description' property and proper null handling
     */
    private suspend fun getPackageDisplayName(packageId: String?): String? {
        return try {
            if (packageId == null) return null
            val packageEntity = database.wordPackageDao().getPackageById(packageId)
            packageEntity?.description ?: packageEntity?.level // Fallback to level if no description
        } catch (e: Exception) {
            null
        }
    }
}