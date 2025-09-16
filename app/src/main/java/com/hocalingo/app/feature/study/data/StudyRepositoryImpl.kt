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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.catch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StudyRepository
 *
 * Handles all study-related data operations using DAO layer
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

            DebugHelper.log("Study queue sorted by priority: ${sortedQueue.size} words")
            emit(sortedQueue)

        } catch (e: Exception) {
            DebugHelper.logError("Study queue loading error", e)
            emit(emptyList())
        }
    }.catch { e ->
        DebugHelper.logError("Flow error in getStudyQueue", e)
        emit(emptyList())
    }

    override suspend fun getCurrentProgress(
        conceptId: Int,
        direction: StudyDirection
    ): Result<WordProgressEntity?> = try {
        val progress = database.wordProgressDao()
            .getProgressByConceptAndDirection(conceptId, direction)
        DebugHelper.log("Retrieved progress for concept $conceptId: ${progress != null}")
        Result.Success(progress)
    } catch (e: Exception) {
        DebugHelper.logError("Get current progress error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getConceptById(conceptId: Int): Result<ConceptEntity?> = try {
        val concept = database.conceptDao().getConceptById(conceptId)
        DebugHelper.log("Retrieved concept $conceptId: ${concept?.english ?: "null"}")
        Result.Success(concept)
    } catch (e: Exception) {
        DebugHelper.logError("Get concept error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun updateWordProgress(
        conceptId: Int,
        direction: StudyDirection,
        quality: Int
    ): Result<WordProgressEntity> = try {
        DebugHelper.log("Updating word progress: concept=$conceptId, direction=$direction, quality=$quality")

        // Get current progress or create new one
        val currentProgress = database.wordProgressDao()
            .getProgressByConceptAndDirection(conceptId, direction)
            ?: SpacedRepetitionAlgorithm.createInitialProgress(conceptId, direction)

        // Calculate new progress using SM-2 algorithm
        val updatedProgress = SpacedRepetitionAlgorithm.calculateNextReview(currentProgress, quality)

        // Save to database
        database.wordProgressDao().insertProgress(updatedProgress)

        DebugHelper.log("Progress updated successfully - next review: ${SpacedRepetitionAlgorithm.getTimeUntilReview(updatedProgress.nextReviewAt)}")
        Result.Success(updatedProgress)

    } catch (e: Exception) {
        DebugHelper.logError("Update word progress error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun hasWordsToStudy(direction: StudyDirection): Result<Boolean> = try {
        val currentTime = System.currentTimeMillis()

        // Check overdue words
        val overdueWords = database.wordProgressDao().getWordsForReview(currentTime, 1)
        val hasOverdue = overdueWords.isNotEmpty()

        // Check new words
        val newWords = database.wordProgressDao().getNewWordsForStudy(1)
        val hasNew = newWords.isNotEmpty()

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
            totalDurationMs = System.currentTimeMillis() // Will be calculated properly in real implementation
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
        val startOfDay = today - (today % (24 * 60 * 60 * 1000)) // Start of today

        // Use correct DAO method name: getSessionsBetweenDates
        val todaySessions = database.studySessionDao().getSessionsBetweenDates(startOfDay, today)
        val wordsStudied = todaySessions.sumOf { it.wordsStudied ?: 0 }

        DebugHelper.log("Words studied today: $wordsStudied")
        Result.Success(wordsStudied)

    } catch (e: Exception) {
        DebugHelper.logError("Get today words studied error", e)
        Result.Error(AppError.Unknown(e))
    }
}