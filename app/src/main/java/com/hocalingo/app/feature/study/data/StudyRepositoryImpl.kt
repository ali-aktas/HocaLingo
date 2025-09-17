package com.hocalingo.app.feature.study.data

import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.common.SpacedRepetitionAlgorithm
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.core.common.base.AppError
import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.core.database.HocaLingoDatabase
import com.hocalingo.app.core.database.dao.ConceptWithTimingData
import com.hocalingo.app.core.database.entities.ConceptEntity
import com.hocalingo.app.core.database.entities.SessionType
import com.hocalingo.app.core.database.entities.StudyDirection
import com.hocalingo.app.core.database.entities.StudySessionEntity
import com.hocalingo.app.core.database.entities.WordProgressEntity
import com.hocalingo.app.feature.study.domain.StudyRepository
import com.hocalingo.app.feature.study.domain.TodayStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StudyRepository
 *
 * Handles all study-related data operations using DAO layer
 * with simplified daily progress tracking
 */
@Singleton
class StudyRepositoryImpl @Inject constructor(
    private val database: HocaLingoDatabase,
    private val preferencesManager: UserPreferencesManager
) : StudyRepository {

    override fun getStudyQueue(
        direction: StudyDirection,
        limit: Int
    ): Flow<List<ConceptWithTimingData>> = flow {
        try {
            val currentTime = System.currentTimeMillis()

            // Call DAO suspend function
            val studyQueue = database.combinedDataDao().getStudyQueue(direction, currentTime, limit)

            DebugHelper.log("Study queue loaded: ${studyQueue.size} words for direction $direction")

            // Sort by priority using SM-2 algorithm
            val sortedQueue = studyQueue.sortedByDescending { conceptData ->
                val mockProgress = WordProgressEntity(
                    conceptId = conceptData.id,
                    direction = direction,
                    repetitions = conceptData.repetitions,
                    intervalDays = 0f,
                    easeFactor = 2.5f,
                    nextReviewAt = conceptData.nextReviewAt,
                    lastReviewAt = null,
                    isSelected = true,
                    isMastered = false
                )
                SpacedRepetitionAlgorithm.getStudyPriority(mockProgress, currentTime)
            }

            emit(sortedQueue)

        } catch (e: Exception) {
            DebugHelper.logError("Study queue flow error", e)
            emit(emptyList())
        }
    }.catch { e ->
        DebugHelper.logError("Study queue flow catch", e)
        emit(emptyList())
    }

    override suspend fun getCurrentProgress(
        conceptId: Int,
        direction: StudyDirection
    ): Result<WordProgressEntity?> = try {
        val progress = database.wordProgressDao().getProgressByConceptAndDirection(conceptId, direction)
        Result.Success(progress)
    } catch (e: Exception) {
        DebugHelper.logError("Get current progress error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getConceptById(conceptId: Int): Result<ConceptEntity?> = try {
        val concept = database.conceptDao().getConceptById(conceptId)
        Result.Success(concept)
    } catch (e: Exception) {
        DebugHelper.logError("Get concept by id error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun updateWordProgress(
        conceptId: Int,
        direction: StudyDirection,
        quality: Int
    ): Result<WordProgressEntity> = try {

        // Get current progress or create new
        val currentProgress = database.wordProgressDao().getProgressByConceptAndDirection(conceptId, direction)
            ?: WordProgressEntity(
                conceptId = conceptId,
                direction = direction,
                repetitions = 0,
                intervalDays = 0f,
                easeFactor = 2.5f,
                nextReviewAt = System.currentTimeMillis(),
                lastReviewAt = null,
                isSelected = true,
                isMastered = false
            )

        // Calculate new progress using SM-2
        val newProgress = SpacedRepetitionAlgorithm.calculateNextReview(currentProgress, quality)

        // Update in database
        database.wordProgressDao().insertProgress(newProgress)

        // If word is moved to future (successful completion), increment daily progress
        val now = System.currentTimeMillis()
        val nextReview = newProgress.nextReviewAt
        val oneDay = 24 * 60 * 60 * 1000 // 24 hours in milliseconds

        if (nextReview > now + oneDay) {
            // Word is moved to tomorrow or later = completed for today
            incrementDailyProgress()
            DebugHelper.log("Word completed for today, daily progress incremented")
        }

        DebugHelper.log("Updated progress for concept $conceptId: quality=$quality, nextReview=$nextReview")
        Result.Success(newProgress)

    } catch (e: Exception) {
        DebugHelper.logError("Update word progress error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun hasWordsToStudy(direction: StudyDirection): Result<Boolean> = try {
        val currentTime = System.currentTimeMillis()

        // Check for overdue words
        val overdueCount = database.combinedDataDao().getOverdueWordsCount(direction, currentTime)
        val hasOverdue = overdueCount > 0

        // Check for new words (never studied)
        val newWordsCount = database.combinedDataDao().getNewWordsCount(direction)
        val hasNew = newWordsCount > 0

        val hasWordsToStudy = hasOverdue || hasNew

        DebugHelper.log("Has words to study: $hasWordsToStudy (overdue: $hasOverdue, new: $hasNew)")
        Result.Success(hasWordsToStudy)

    } catch (e: Exception) {
        DebugHelper.logError("Check has words to study error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun startStudySession(sessionType: SessionType): Result<Long> = try {
        val sessionEntity = StudySessionEntity(
            startedAt = System.currentTimeMillis(),
            sessionType = sessionType
        )

        val sessionId = database.studySessionDao().insertSession(sessionEntity)
        DebugHelper.log("Started new study session: $sessionId")
        Result.Success(sessionId)

    } catch (e: Exception) {
        DebugHelper.logError("Start study session error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun endStudySession(
        sessionId: Long,
        wordsStudied: Int,
        correctAnswers: Int
    ): Result<Unit> = try {
        val session = StudySessionEntity(
            id = sessionId,
            startedAt = 0, // Will be preserved from existing record
            endedAt = System.currentTimeMillis(),
            wordsStudied = wordsStudied,
            correctAnswers = correctAnswers,
            sessionType = SessionType.MIXED,
            totalDurationMs = System.currentTimeMillis()
        )

        database.studySessionDao().updateSession(session)
        DebugHelper.log("Ended study session: $sessionId with $correctAnswers/$wordsStudied correct")
        Result.Success(Unit)

    } catch (e: Exception) {
        DebugHelper.logError("End study session error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getTodayWordsStudied(): Result<Int> = try {
        val today = System.currentTimeMillis()
        val startOfDay = today - (today % (24 * 60 * 60 * 1000))

        val todaySessions = database.studySessionDao().getSessionsBetweenDates(startOfDay, today)
        val wordsStudied = todaySessions.sumOf { it.wordsStudied ?: 0 }

        DebugHelper.log("Words studied today: $wordsStudied")
        Result.Success(wordsStudied)

    } catch (e: Exception) {
        DebugHelper.logError("Get today words studied error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getDailyCompletedWords(): Result<Int> = try {
        val today = System.currentTimeMillis()
        val startOfDay = today - (today % (24 * 60 * 60 * 1000))
        val endOfDay = startOfDay + (24 * 60 * 60 * 1000)

        // Count words that were updated today and moved to future dates
        val completedCount = database.wordProgressDao()
            .getWordsCompletedToday(startOfDay, endOfDay)

        DebugHelper.log("Daily completed words: $completedCount")
        Result.Success(completedCount)

    } catch (e: Exception) {
        DebugHelper.logError("Get daily completed words error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun incrementDailyProgress(): Result<Unit> = try {
        // This could store daily progress in a separate table
        // For now, we calculate it dynamically from completed cards

        DebugHelper.log("Daily progress incremented (calculated dynamically)")
        Result.Success(Unit)

    } catch (e: Exception) {
        DebugHelper.logError("Increment daily progress error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getDailyGoal(): Result<Int> = try {
        val dailyGoal = preferencesManager.getDailyGoal().first()
        Result.Success(dailyGoal)
    } catch (e: Exception) {
        DebugHelper.logError("Get daily goal error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun isDailyGoalReached(): Result<Boolean> = try {
        val completedWordsResult = getDailyCompletedWords()
        val dailyGoalResult = getDailyGoal()

        if (completedWordsResult is Result.Success && dailyGoalResult is Result.Success) {
            val isReached = completedWordsResult.data >= dailyGoalResult.data
            DebugHelper.log("Daily goal reached: $isReached (${completedWordsResult.data}/${dailyGoalResult.data})")
            Result.Success(isReached)
        } else {
            Result.Success(false)
        }

    } catch (e: Exception) {
        DebugHelper.logError("Check daily goal reached error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getTodaySessionStats(): Result<TodayStats> = try {
        val wordsStudiedResult = getTodayWordsStudied()
        val completedWordsResult = getDailyCompletedWords()
        val dailyGoalResult = getDailyGoal()

        val wordsStudied = if (wordsStudiedResult is Result.Success) wordsStudiedResult.data else 0
        val completed = if (completedWordsResult is Result.Success) completedWordsResult.data else 0
        val goal = if (dailyGoalResult is Result.Success) dailyGoalResult.data else 20

        val progressPercentage = if (goal > 0) {
            (completed.toFloat() / goal.toFloat() * 100f).coerceAtMost(100f)
        } else 0f

        // Count today's sessions
        val today = System.currentTimeMillis()
        val startOfDay = today - (today % (24 * 60 * 60 * 1000))
        val sessions = database.studySessionDao().getSessionsBetweenDates(startOfDay, today)

        val stats = TodayStats(
            wordsStudied = wordsStudied,
            cardsCompleted = completed,
            dailyGoal = goal,
            progressPercentage = progressPercentage,
            sessionCount = sessions.size
        )

        DebugHelper.log("Today stats: $stats")
        Result.Success(stats)

    } catch (e: Exception) {
        DebugHelper.logError("Get today session stats error", e)
        Result.Error(AppError.Unknown(e))
    }
}