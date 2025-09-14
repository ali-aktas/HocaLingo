package com.hocalingo.app.feature.study.domain

import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.core.database.dao.ConceptWithTimingData
import com.hocalingo.app.core.database.entities.ConceptEntity
import com.hocalingo.app.core.database.entities.StudyDirection
import com.hocalingo.app.core.database.entities.StudySessionEntity
import com.hocalingo.app.core.database.entities.WordProgressEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Study functionality
 *
 * Handles:
 * - Study queue management
 * - Word progress updates with SM-2 algorithm
 * - Study session tracking
 * - Statistics and daily goals
 */
interface StudyRepository {

    /**
     * Get study queue for current session
     * Returns words that need to be reviewed based on SM-2 timing
     *
     * @param direction Study direction (EN_TO_TR, TR_TO_EN, or mixed)
     * @param limit Maximum number of words to return
     * @return Flow of study queue with timing data
     */
    fun getStudyQueue(
        direction: StudyDirection,
        limit: Int = 50
    ): Flow<List<ConceptWithTimingData>>

    /**
     * Get current progress for a specific concept and direction
     * Used for calculating next review intervals in UI
     *
     * @param conceptId ID of the concept
     * @param direction Study direction
     * @return Result with current WordProgressEntity or null if not found
     */
    suspend fun getCurrentProgress(
        conceptId: Int,
        direction: StudyDirection
    ): Result<WordProgressEntity?>

    /**
     * Get single concept with its current progress
     *
     * @param conceptId ID of the concept
     * @param direction Study direction
     * @return Result with concept and progress data
     */
    suspend fun getConceptWithProgress(
        conceptId: Int,
        direction: StudyDirection
    ): Result<ConceptEntity?>

    /**
     * Update word progress based on user response
     * Uses SM-2 algorithm to calculate next review time
     *
     * @param conceptId ID of the concept
     * @param direction Study direction
     * @param quality User response quality (1=Hard, 2=Medium, 3=Easy)
     * @return Result with updated progress
     */
    suspend fun updateWordProgress(
        conceptId: Int,
        direction: StudyDirection,
        quality: Int
    ): Result<WordProgressEntity>

    /**
     * Get words that need immediate review (overdue)
     * Priority sorted for efficient study sessions
     *
     * @param direction Study direction
     * @return List of overdue words with timing data
     */
    suspend fun getOverdueWords(direction: StudyDirection): Result<List<ConceptWithTimingData>>

    /**
     * Get new words ready for first-time study
     * Words that have been selected but never studied
     *
     * @param direction Study direction
     * @param limit Maximum number of new words
     * @return Result with list of new words
     */
    suspend fun getNewWords(
        direction: StudyDirection,
        limit: Int = 10
    ): Result<List<ConceptEntity>>

    /**
     * Start a new study session
     * Creates session record and initializes tracking
     *
     * @param sessionType Type of study session
     * @return Result with session ID
     */
    suspend fun startStudySession(
        sessionType: com.hocalingo.app.core.database.entities.SessionType
    ): Result<Long>

    /**
     * End current study session with statistics
     * Updates session record with final stats
     *
     * @param sessionId Session ID from startStudySession
     * @param wordsStudied Number of words studied
     * @param correctAnswers Number of correct responses
     * @return Result indicating success/failure
     */
    suspend fun endStudySession(
        sessionId: Long,
        wordsStudied: Int,
        correctAnswers: Int
    ): Result<Unit>

    /**
     * Get today's study statistics
     *
     * @return Result with daily stats
     */
    suspend fun getTodayStats(): Result<DailyStudyStats>

    /**
     * Get user's current daily goal
     *
     * @return Flow of daily goal value
     */
    fun getDailyGoal(): Flow<Int>

    /**
     * Update daily goal setting
     *
     * @param goal New daily goal value
     * @return Result indicating success/failure
     */
    suspend fun updateDailyGoal(goal: Int): Result<Unit>

    /**
     * Get count of words available for study
     * Separated by new vs review words
     *
     * @param direction Study direction
     * @return Result with word counts
     */
    suspend fun getStudyWordCounts(direction: StudyDirection): Result<StudyWordCounts>

    /**
     * Mark word as mastered
     * Removes from active study rotation
     *
     * @param conceptId ID of the concept
     * @param direction Study direction
     * @return Result indicating success/failure
     */
    suspend fun markWordAsMastered(
        conceptId: Int,
        direction: StudyDirection
    ): Result<Unit>

    /**
     * Reset word progress (for user request)
     * Brings word back to beginning of SM-2 cycle
     *
     * @param conceptId ID of the concept
     * @param direction Study direction
     * @return Result indicating success/failure
     */
    suspend fun resetWordProgress(
        conceptId: Int,
        direction: StudyDirection
    ): Result<Unit>
}

/**
 * Daily study statistics data class
 */
data class DailyStudyStats(
    val wordsStudied: Int = 0,
    val correctAnswers: Int = 0,
    val totalAnswers: Int = 0,
    val studyTimeMs: Long = 0,
    val streakCount: Int = 0,
    val goalAchieved: Boolean = false,
    val accuracy: Float = if (totalAnswers > 0) correctAnswers.toFloat() / totalAnswers.toFloat() else 0f
)

/**
 * Study word counts for queue status
 */
data class StudyWordCounts(
    val newWords: Int = 0,
    val reviewWords: Int = 0,
    val overdueWords: Int = 0,
    val masteredWords: Int = 0
) {
    val totalActiveWords: Int get() = newWords + reviewWords + overdueWords
    val hasWordsToStudy: Boolean get() = totalActiveWords > 0
}