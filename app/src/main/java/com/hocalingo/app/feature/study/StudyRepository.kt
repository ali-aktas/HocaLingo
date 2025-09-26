package com.hocalingo.app.feature.study

import com.hocalingo.app.core.base.Result
import com.hocalingo.app.database.dao.ConceptWithTimingData
import com.hocalingo.app.database.entities.ConceptEntity
import com.hocalingo.app.database.entities.StudyDirection
import com.hocalingo.app.database.entities.SessionType
import com.hocalingo.app.database.entities.WordProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Study functionality - HYBRID VERSION
 *
 * UPDATED FOR HYBRID LEARNING SYSTEM:
 * ✅ Session-based learning + time-based review
 * ✅ Learning cards count method added
 * ✅ Graduate-based daily progress tracking
 *
 * Handles:
 * - Study queue management with hybrid SM-2 algorithm
 * - Learning/Review phase management
 * - Word progress updates and daily tracking
 * - Study session tracking
 * - Simplified statistics (daily goal focused)
 */
interface StudyRepository {

    /**
     * Get study queue for current session - HYBRID VERSION
     * Returns learning cards (always) + due review cards (time-based)
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
     * Update word progress after user response - HYBRID VERSION
     * Uses learning/review phases and graduation-based daily progress
     */
    suspend fun updateWordProgress(
        conceptId: Int,
        direction: StudyDirection,
        quality: Int
    ): Result<WordProgressEntity>

    /**
     * Check if there are words available to study - HYBRID VERSION
     * Checks both learning cards and due review cards
     */
    suspend fun hasWordsToStudy(direction: StudyDirection): Result<Boolean>

    /**
     * HYBRID: Get learning cards count for a direction
     * Used to determine if session should continue or complete
     * Learning cards always stay in session until graduation
     */
    suspend fun getLearningCardsCount(direction: StudyDirection): Result<Int>

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
     * Get daily completed words count - HYBRID VERSION
     * Words that graduated from learning to review phase today
     */
    suspend fun getDailyCompletedWords(): Result<Int>

    /**
     * Update daily progress when a card graduates
     * Called when a word moves from learning to review phase
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