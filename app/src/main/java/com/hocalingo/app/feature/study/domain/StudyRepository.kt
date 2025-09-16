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
 * - Word progress updates
 * - Study session tracking
 * - Basic statistics
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
     */
    suspend fun getTodayWordsStudied(): Result<Int>
}