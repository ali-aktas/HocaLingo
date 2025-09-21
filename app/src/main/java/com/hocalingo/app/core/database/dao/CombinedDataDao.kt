package com.hocalingo.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.hocalingo.app.core.database.entities.StudyDirection
import com.hocalingo.app.core.database.entities.SelectionStatus

/**
 * Combined Data DAO - Enhanced for Profile Feature
 * ✅ Selected words queries for profile
 * ✅ Pagination support
 * ✅ User statistics support
 */
@Dao
interface CombinedDataDao {

    /**
     * Study queue management - existing
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
            COALESCE(wp.next_review_at, :currentTime) as nextReviewAt,
            COALESCE(wp.repetitions, 0) as repetitions
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        LEFT JOIN word_progress wp ON c.id = wp.concept_id AND wp.direction = :direction
        WHERE us.status = 'SELECTED' 
        AND (
            wp.learning_phase = 1 
            OR (wp.learning_phase = 0 AND wp.next_review_at <= :currentTime)
            OR wp.concept_id IS NULL
        )
        ORDER BY 
            CASE WHEN wp.learning_phase = 1 THEN wp.session_position ELSE wp.next_review_at END ASC
        LIMIT :limit
    """)
    suspend fun getStudyQueue(
        direction: StudyDirection,
        currentTime: Long,
        limit: Int = 50
    ): List<ConceptWithTimingData>

    /**
     * ✅ NEW: Selected words with progress for Profile - Preview (5 words)
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
            c.package_id as packageId,
            us.status as selectionStatus,
            MAX(wp.repetitions) as repetitions,
            MAX(CASE WHEN wp.is_mastered = 1 THEN 1 ELSE 0 END) as isMastered,
            MAX(wp.next_review_at) as nextReviewAt
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        LEFT JOIN word_progress wp ON c.id = wp.concept_id
        WHERE us.status = 'SELECTED'
        GROUP BY c.id
        ORDER BY c.id ASC
        LIMIT :limit
    """)
    suspend fun getSelectedWordsWithProgress(limit: Int = 5): List<ConceptWithProgressData>

    /**
     * ✅ NEW: Selected words with progress for Profile - Paginated
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
            c.package_id as packageId,
            us.status as selectionStatus,
            MAX(wp.repetitions) as repetitions,
            MAX(CASE WHEN wp.is_mastered = 1 THEN 1 ELSE 0 END) as isMastered,
            MAX(wp.next_review_at) as nextReviewAt
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        LEFT JOIN word_progress wp ON c.id = wp.concept_id
        WHERE us.status = 'SELECTED'
        GROUP BY c.id
        ORDER BY c.id ASC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getSelectedWordsWithProgressPaginated(
        offset: Int,
        limit: Int = 20
    ): List<ConceptWithProgressData>

    /**
     * Learning phase management - existing
     */
    @Query("""
        SELECT COUNT(*)
        FROM concepts c
        INNER JOIN user_selections us ON c.id = us.concept_id
        LEFT JOIN word_progress wp ON c.id = wp.concept_id AND wp.direction = :direction
        WHERE us.status = 'SELECTED' 
        AND (wp.learning_phase = 1 OR wp.concept_id IS NULL)
    """)
    suspend fun getLearningCardsCount(direction: StudyDirection): Int

    @Query("""
        SELECT COALESCE(MAX(wp.session_position), 0)
        FROM word_progress wp
        WHERE wp.direction = :direction AND wp.learning_phase = 1
    """)
    suspend fun getMaxSessionPosition(direction: StudyDirection): Int

    /**
     * Review phase management - existing
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
     * Statistics methods - existing
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
 * Data class for concept with progress information - Enhanced for Profile
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
    val packageId: String? = null, // ✅ Added for profile display
    val selectionStatus: SelectionStatus?,
    val repetitions: Int?,
    val nextReviewAt: Long?,
    val isMastered: Boolean?
)