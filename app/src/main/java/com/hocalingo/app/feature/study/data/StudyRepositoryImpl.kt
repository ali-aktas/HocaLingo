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
import com.hocalingo.app.feature.study.domain.DailyStudyStats
import com.hocalingo.app.feature.study.domain.StudyRepository
import com.hocalingo.app.feature.study.domain.StudyWordCounts
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of StudyRepository
 *
 * Handles all study-related data operations:
 * - Study queue management with SM-2 algorithm
 * - Progress tracking and updates
 * - Session management
 * - Statistics calculation
 */
@Singleton
class StudyRepositoryImpl @Inject constructor(
    private val database: HocaLingoDatabase,
    private val preferencesManager: UserPreferencesManager
) : StudyRepository {

    override fun getStudyQueue(
        direction: StudyDirection,
        limit: Int
    ): Flow<List<ConceptWithTimingData>> {
        val currentTime = System.currentTimeMillis()

        return database.combinedDataDao().getStudyQueue(direction, currentTime, limit)
            .onEach { studyQueue -> // 1. Önce onEach ile yan etki (loglama) yapılır
                DebugHelper.log("Study queue loaded: ${studyQueue.size} words for direction $direction")
            }
            .map { studyQueue -> // 2. Sonra map ile dönüştürme (sıralama) yapılır
                // Sort by priority using SM-2 algorithm
                studyQueue.sortedByDescending { conceptData ->
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
            }
    }

    override suspend fun getCurrentProgress(
        conceptId: Int,
        direction: StudyDirection
    ): Result<WordProgressEntity?> = try {
        val progress = database.wordProgressDao()
            .getProgressByConceptAndDirection(conceptId, direction)
        DebugHelper.log("Retrieved progress for concept $conceptId: ${if (progress != null) "found" else "not found"}")
        Result.Success(progress)
    } catch (e: Exception) {
        DebugHelper.logError("Get current progress error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getConceptWithProgress(
        conceptId: Int,
        direction: StudyDirection
    ): Result<ConceptEntity?> = try {
        val concept = database.conceptDao().getConceptById(conceptId)
        DebugHelper.log("Retrieved concept: ${concept?.english ?: "null"}")
        Result.Success(concept)
    } catch (e: Exception) {
        DebugHelper.logError("Get concept with progress error", e)
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

    override suspend fun getOverdueWords(
        direction: StudyDirection
    ): Result<List<ConceptWithTimingData>> = try {
        val currentTime = System.currentTimeMillis()

        val overdueWords = database.combinedDataDao().getStudyQueue(direction, currentTime, 100)
            .filter  { it.nextReviewAt <= currentTime }


        DebugHelper.log("Found ${overdueWords.size} overdue words")
        Result.Success(overdueWords)

    } catch (e: Exception) {
        DebugHelper.logError("Get overdue words error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getNewWords(
        direction: StudyDirection,
        limit: Int
    ): Result<List<ConceptEntity>> = try {
        val newProgressWords = database.wordProgressDao().getNewWordsForStudy(limit)

        val concepts = newProgressWords.mapNotNull { progress ->
            database.conceptDao().getConceptById(progress.conceptId)
        }

        DebugHelper.log("Found ${concepts.size} new words for study")
        Result.Success(concepts)

    } catch (e: Exception) {
        DebugHelper.logError("Get new words error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun startStudySession(
        sessionType: SessionType
    ): Result<Long> = try {
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
        DebugHelper.log("Ended study session: $sessionId")
        Result.Success(Unit)

    } catch (e: Exception) {
        DebugHelper.logError("End study session error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getTodayStats(): Result<DailyStudyStats> = try {
        val currentTime = System.currentTimeMillis()
        val startOfDay = currentTime - (currentTime % (24 * 60 * 60 * 1000))

        val wordsStudied = database.studySessionDao().getTotalWordsStudiedSince(startOfDay)
        val studyTime = database.studySessionDao().getTotalStudyTimeSince(startOfDay)
        val accuracy = database.studySessionDao().getAverageAccuracySince(startOfDay)
        val dailyGoal = preferencesManager.getDailyGoal().first()

        val stats = DailyStudyStats(
            wordsStudied = wordsStudied,
            correctAnswers = (wordsStudied * accuracy).toInt(),
            totalAnswers = wordsStudied,
            studyTimeMs = studyTime,
            streakCount = 0, // TODO: Implement streak calculation
            goalAchieved = wordsStudied >= dailyGoal,
            accuracy = accuracy
        )

        DebugHelper.log("Today's stats: $stats")
        Result.Success(stats)

    } catch (e: Exception) {
        DebugHelper.logError("Get today stats error", e)
        Result.Error(AppError.Unknown(e))
    }

    override fun getDailyGoal(): Flow<Int> {
        return preferencesManager.getDailyGoal()
    }

    override suspend fun updateDailyGoal(goal: Int): Result<Unit> = try {
        preferencesManager.setDailyGoal(goal)
        DebugHelper.log("Daily goal updated to: $goal")
        Result.Success(Unit)

    } catch (e: Exception) {
        DebugHelper.logError("Update daily goal error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun getStudyWordCounts(
        direction: StudyDirection
    ): Result<StudyWordCounts> = try {
        val currentTime = System.currentTimeMillis()

        val newWords = database.wordProgressDao().getNewWordsCount()
        val reviewWords = database.wordProgressDao().getWordsForReviewCount(currentTime)
        val masteredWords = database.wordProgressDao().getMasteredWordsCount()

        // Overdue words are part of review words but past due
        val allReviewWords = database.wordProgressDao().getWordsForReview(currentTime, 1000)
        val overdueWords = allReviewWords.count { it.nextReviewAt < currentTime }

        val counts = StudyWordCounts(
            newWords = newWords,
            reviewWords = reviewWords - overdueWords,
            overdueWords = overdueWords,
            masteredWords = masteredWords
        )

        DebugHelper.log("Study word counts: $counts")
        Result.Success(counts)

    } catch (e: Exception) {
        DebugHelper.logError("Get study word counts error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun markWordAsMastered(
        conceptId: Int,
        direction: StudyDirection
    ): Result<Unit> = try {
        val currentProgress = database.wordProgressDao()
            .getProgressByConceptAndDirection(conceptId, direction)
            ?: return Result.Error(AppError.NotFound)

        val masteredProgress = currentProgress.copy(
            isMastered = true,
            nextReviewAt = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000), // 1 year from now
            updatedAt = System.currentTimeMillis()
        )

        database.wordProgressDao().updateProgress(masteredProgress)
        DebugHelper.log("Word marked as mastered: $conceptId")
        Result.Success(Unit)

    } catch (e: Exception) {
        DebugHelper.logError("Mark word as mastered error", e)
        Result.Error(AppError.Unknown(e))
    }

    override suspend fun resetWordProgress(
        conceptId: Int,
        direction: StudyDirection
    ): Result<Unit> = try {
        val resetProgress = SpacedRepetitionAlgorithm.createInitialProgress(conceptId, direction)
        database.wordProgressDao().insertProgress(resetProgress)

        DebugHelper.log("Word progress reset: $conceptId")
        Result.Success(Unit)

    } catch (e: Exception) {
        DebugHelper.logError("Reset word progress error", e)
        Result.Error(AppError.Unknown(e))
    }
}