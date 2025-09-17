package com.hocalingo.app.core.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.hocalingo.app.core.database.entities.SelectionStatus
import com.hocalingo.app.core.database.entities.StudyDirection

@Dao
interface CombinedDataDao {

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
        AND wp.next_review_at <= :currentTime
        AND wp.is_mastered = 0
        ORDER BY wp.next_review_at ASC
        LIMIT :limit
    """)
    suspend fun getStudyQueue(
        direction: StudyDirection,
        currentTime: Long,
        limit: Int = 20
    ): List<ConceptWithTimingData>

    // NEW: Count methods for hasWordsToStudy logic
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

    // NEW: Statistics methods
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
}

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