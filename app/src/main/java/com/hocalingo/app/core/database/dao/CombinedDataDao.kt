package com.hocalingo.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.hocalingo.app.core.database.entities.SelectionStatus
import com.hocalingo.app.core.database.entities.StudyDirection

@Dao
interface CombinedDataDao {

    /**
     * HYBRID STUDY QUEUE - Learning + Review Cards
     *
     * LEARNING CARDS: Always shown (stay in session)
     * REVIEW CARDS: Only shown when time is due
     *
     * Priority Order:
     * 1. Learning cards by session_position (ASC)
     * 2. Review cards by next_review_at (ASC)
     */
    @Query("""
        SELECT 
            c.id,
            c.english,
            c.turkish,
            c.example_en as exampleEn,
            c.example_tr as exampleTr,
            c.pronunciation,
            c.level,
            c.category,
            wp.next_review_at as nextReviewAt,
            wp.repetitions
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        INNER JOIN word_progress wp ON c.id = wp.concept_id
        WHERE us.status = 'SELECTED' 
        AND wp.direction = :direction
        AND wp.is_mastered = 0
        AND (
            (wp.learning_phase = 1) OR                                    -- Learning cards: always include
            (wp.learning_phase = 0 AND wp.next_review_at <= :currentTime) -- Review cards: only if due
        )
        ORDER BY 
            wp.learning_phase DESC,           -- Learning cards first (1 before 0)
            wp.session_position ASC,          -- Learning cards by session position
            wp.next_review_at ASC             -- Review cards by due time
        LIMIT :limit
    """)
    suspend fun getStudyQueue(
        direction: StudyDirection,
        currentTime: Long,
        limit: Int = 20
    ): List<ConceptWithTimingData>

    /**
     * Count words available for study - HYBRID VERSION
     */
    @Query("""
        SELECT COUNT(*)
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        INNER JOIN word_progress wp ON c.id = wp.concept_id
        WHERE us.status = 'SELECTED' 
        AND wp.direction = :direction
        AND wp.is_mastered = 0
        AND (
            (wp.learning_phase = 1) OR                                    -- Learning cards
            (wp.learning_phase = 0 AND wp.next_review_at <= :currentTime) -- Due review cards
        )
    """)
    suspend fun getStudyWordsCount(direction: StudyDirection, currentTime: Long): Int

    /**
     * Get learning cards count (cards in current session)
     */
    @Query("""
        SELECT COUNT(*)
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        INNER JOIN word_progress wp ON c.id = wp.concept_id
        WHERE us.status = 'SELECTED' 
        AND wp.direction = :direction
        AND wp.learning_phase = 1
        AND wp.is_mastered = 0
    """)
    suspend fun getLearningCardsCount(direction: StudyDirection): Int

    /**
     * Get review cards count (time-based cards that are due)
     */
    @Query("""
        SELECT COUNT(*)
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        INNER JOIN word_progress wp ON c.id = wp.concept_id
        WHERE us.status = 'SELECTED' 
        AND wp.direction = :direction
        AND wp.learning_phase = 0
        AND wp.next_review_at <= :currentTime
        AND wp.is_mastered = 0
    """)
    suspend fun getOverdueReviewCardsCount(direction: StudyDirection, currentTime: Long): Int

    /**
     * Get max session position for learning cards
     * Used to add new cards to end of session queue
     */
    @Query("""
        SELECT COALESCE(MAX(wp.session_position), 0)
        FROM word_progress wp
        WHERE wp.learning_phase = 1
        AND wp.direction = :direction
    """)
    suspend fun getMaxSessionPosition(direction: StudyDirection): Int

    /**
     * LEGACY: Count methods for compatibility
     * These now check both learning and review cards
     */
    @Query("""
        SELECT COUNT(*)
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        INNER JOIN word_progress wp ON c.id = wp.concept_id
        WHERE us.status = 'SELECTED' 
        AND wp.direction = :direction
        AND wp.next_review_at <= :currentTime
        AND wp.is_mastered = 0
    """)
    suspend fun getOverdueWordsCount(direction: StudyDirection, currentTime: Long): Int

    @Query("""
        SELECT COUNT(*)
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        LEFT JOIN word_progress wp ON c.id = wp.concept_id AND wp.direction = :direction
        WHERE us.status = 'SELECTED' 
        AND (wp.concept_id IS NULL OR wp.repetitions = 0)
    """)
    suspend fun getNewWordsCount(direction: StudyDirection): Int

    /**
     * Statistics methods
     */
    @Query("""
        SELECT COUNT(*)
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        WHERE us.status = 'SELECTED'
    """)
    suspend fun getTotalSelectedWordsCount(): Int

    @Query("""
        SELECT COUNT(*)
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        INNER JOIN word_progress wp ON c.id = wp.concept_id
        WHERE us.status = 'SELECTED' 
        AND wp.direction = :direction
        AND wp.is_mastered = 1
    """)
    suspend fun getMasteredWordsCount(direction: StudyDirection): Int

    /**
     * Get words that graduated today (moved from learning to review phase)
     * These count towards daily progress
     */
    @Query("""
        SELECT COUNT(*)
        FROM word_progress wp
        WHERE wp.learning_phase = 0
        AND wp.repetitions >= 2
        AND wp.last_review_at >= :startOfDay 
        AND wp.last_review_at < :endOfDay
        AND wp.direction = :direction
    """)
    suspend fun getGraduatedWordsToday(
        direction: StudyDirection,
        startOfDay: Long,
        endOfDay: Long
    ): Int

    /**
     * Debug method: Get all learning cards with their positions
     */
    @Query("""
        SELECT 
            c.id,
            c.english,
            c.turkish,
            c.example_en as exampleEn,
            c.example_tr as exampleTr,
            c.pronunciation,
            c.level,
            c.category,
            us.status as selectionStatus,
            wp.repetitions,
            wp.next_review_at as nextReviewAt,
            wp.is_mastered as isMastered
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        INNER JOIN word_progress wp ON c.id = wp.concept_id
        WHERE wp.learning_phase = 1
        AND wp.direction = :direction
        ORDER BY wp.session_position ASC
    """)
    suspend fun getAllLearningCards(direction: StudyDirection): List<ConceptWithProgressData>

    @Query("""
        SELECT 
            c.id,
            c.english,
            c.turkish,
            c.example_en as exampleEn,
            c.example_tr as exampleTr,
            c.pronunciation,
            c.level,
            c.category,
            us.status as selectionStatus,
            wp.repetitions,
            wp.next_review_at as nextReviewAt,
            wp.is_mastered as isMastered
        FROM concepts c
        LEFT JOIN user_selections us ON c.id = us.concept_id
        LEFT JOIN word_progress wp ON c.id = wp.concept_id AND wp.direction = :direction
        WHERE c.id = :conceptId
    """)
    suspend fun getConceptWithProgress(conceptId: Int, direction: StudyDirection): ConceptWithProgressData?
}

/**
 * Data class for concept with timing information - UNCHANGED
 */
data class ConceptWithTimingData(
    val id: Int,
    val english: String,
    val turkish: String,
    val exampleEn: String?,
    val exampleTr: String?,
    val pronunciation: String?,
    val level: String,
    val category: String,
    val nextReviewAt: Long,
    val repetitions: Int
)

/**
 * Data class for concept with progress information - UNCHANGED
 */
data class ConceptWithProgressData(
    val id: Int,
    val english: String,
    val turkish: String,
    val exampleEn: String?,
    val exampleTr: String?,
    val pronunciation: String?,
    val level: String,
    val category: String,
    val selectionStatus: SelectionStatus?,
    val repetitions: Int?,
    val nextReviewAt: Long?,
    val isMastered: Boolean?
)