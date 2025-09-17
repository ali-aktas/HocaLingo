package com.hocalingo.app.feature.study.domain

import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.core.database.dao.ConceptWithTimingData
import com.hocalingo.app.core.database.entities.ConceptEntity
import com.hocalingo.app.core.database.entities.StudyDirection
import com.hocalingo.app.core.database.entities.SessionType
import com.hocalingo.app.core.database.entities.WordProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Study functionality
 *
 * Handles:
 * - Study queue management with SM-2 algorithm
 * - Word progress updates and daily tracking
 * - Study session tracking
 * - Simplified statistics (daily goal focused)
 */
interface StudyRepository {

    /**
     * Get study queue for current session
     * Returns words that need to be reviewed based on SM-2 timing
     */
    fun getStudyQueue(
        direction: StudyDirection,
        limit: Int = 50
    ): Flow<List<ConceptWithTimingData>>

    /**
     * Get current progress for a specific concept and direction
     */
    suspend fun getCurrentProgress(
        conceptId: Int,
        direction: StudyDirection
    ): Result<WordProgressEntity?>

    /**
     * Get concept entity by ID
     */
    suspend fun getConceptById(conceptId: Int): Result<ConceptEntity?>

    /**
     * Update word progress after user response
     * This will move cards to future dates based on SM-2 algorithm
     * And increment daily progress when a card is completed
     */
    suspend fun updateWordProgress(
        conceptId: Int,
        direction: StudyDirection,
        quality: Int
    ): Result<WordProgressEntity>

    /**
     * Check if there are words available to study
     */
    suspend fun hasWordsToStudy(direction: StudyDirection): Result<Boolean>

    /**
     * Start a new study session
     */
    suspend fun startStudySession(sessionType: SessionType): Result<Long>

    /**
     * End current study session
     */
    suspend fun endStudySession(
        sessionId: Long,
        wordsStudied: Int,
        correctAnswers: Int
    ): Result<Unit>

    /**
     * Get today's words studied count
     * This is the main metric for daily progress tracking
     */
    suspend fun getTodayWordsStudied(): Result<Int>

    /**
     * Get daily completed words count
     * Words that have been moved to future dates (completed for today)
     */
    suspend fun getDailyCompletedWords(): Result<Int>

    /**
     * Update daily progress when a card is completed
     * Called when a word is moved to next review date
     */
    suspend fun incrementDailyProgress(): Result<Unit>

    /**
     * Get user's daily goal
     */
    suspend fun getDailyGoal(): Result<Int>

    /**
     * Check if daily goal is reached
     */
    suspend fun isDailyGoalReached(): Result<Boolean>

    /**
     * Get study session statistics for today
     */
    suspend fun getTodaySessionStats(): Result<TodayStats>
}

/**
 * Today's study statistics - simplified
 */
data class TodayStats(
    val wordsStudied: Int,
    val cardsCompleted: Int,
    val dailyGoal: Int,
    val progressPercentage: Float,
    val sessionCount: Int
) {
    val isGoalReached: Boolean
        get() = cardsCompleted >= dailyGoal

    val remainingWords: Int
        get() = (dailyGoal - cardsCompleted).coerceAtLeast(0)
}