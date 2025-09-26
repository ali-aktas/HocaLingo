package com.hocalingo.app.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hocalingo.app.database.entities.StudyDirection
import com.hocalingo.app.database.entities.WordProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WordProgressDao {

    @Query("SELECT * FROM word_progress WHERE concept_id = :conceptId")
    suspend fun getProgressByConceptId(conceptId: Int): List<WordProgressEntity>

    @Query("SELECT * FROM word_progress WHERE concept_id = :conceptId AND direction = :direction")
    suspend fun getProgressByConceptAndDirection(
        conceptId: Int,
        direction: StudyDirection
    ): WordProgressEntity?

    // Alias for StudyRepositoryImpl compatibility
    suspend fun getProgress(conceptId: Int, direction: StudyDirection): WordProgressEntity? =
        getProgressByConceptAndDirection(conceptId, direction)

    @Query("""
        SELECT * FROM word_progress 
        WHERE is_selected = 1 AND next_review_at <= :currentTime
        ORDER BY next_review_at ASC
        LIMIT :limit
    """)
    suspend fun getWordsForReview(currentTime: Long, limit: Int = 20): List<WordProgressEntity>

    @Query("""
        SELECT * FROM word_progress 
        WHERE is_selected = 1 AND repetitions = 0
        ORDER BY created_at ASC
        LIMIT :limit
    """)
    suspend fun getNewWordsForStudy(limit: Int = 5): List<WordProgressEntity>

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_selected = 1 AND next_review_at <= :currentTime")
    suspend fun getWordsForReviewCount(currentTime: Long): Int

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_selected = 1 AND repetitions = 0")
    suspend fun getNewWordsCount(): Int

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_selected = 1 AND is_mastered = 1")
    suspend fun getMasteredWordsCount(): Int

    @Query("SELECT COUNT(*) FROM word_progress WHERE is_selected = 1")
    suspend fun getTotalSelectedWordsCount(): Int

    // NEW: Daily progress tracking methods
    @Query("""
        SELECT COUNT(*) FROM word_progress 
        WHERE last_review_at >= :startOfDay 
        AND last_review_at < :endOfDay
        AND next_review_at > :endOfDay
    """)
    suspend fun getWordsCompletedToday(startOfDay: Long, endOfDay: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgress(progress: WordProgressEntity)

    // Alias for StudyRepositoryImpl compatibility
    suspend fun upsertProgress(progress: WordProgressEntity) = insertProgress(progress)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressList(progressList: List<WordProgressEntity>)

    @Update
    suspend fun updateProgress(progress: WordProgressEntity)

    @Delete
    suspend fun deleteProgress(progress: WordProgressEntity)

    @Query("DELETE FROM word_progress WHERE concept_id = :conceptId")
    suspend fun deleteProgressByConceptId(conceptId: Int)

    @Query("""
        SELECT * FROM word_progress 
        WHERE is_selected = 1 AND next_review_at <= :currentTime
        ORDER BY next_review_at ASC
    """)
    fun getWordsForReviewFlow(currentTime: Long): Flow<List<WordProgressEntity>>
}