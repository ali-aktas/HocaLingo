package com.hocalingo.app.database.dao

import androidx.room.*
import com.hocalingo.app.database.entities.StoryEntity
import com.hocalingo.app.database.entities.StoryQuotaEntity
import kotlinx.coroutines.flow.Flow

/**
 * StoryDao - Database Access Object for AI Stories
 *
 * Package: app/src/main/java/com/hocalingo/app/database/dao/
 *
 * Provides all database operations for:
 * - Story CRUD operations
 * - Daily quota tracking
 * - History retrieval
 * - Cleanup operations
 */
@Dao
interface StoryDao {

    // ==================== STORY OPERATIONS ====================

    /**
     * Get all stories ordered by creation date (newest first)
     * Limited to last 30 stories for performance
     */
    @Query("SELECT * FROM generated_stories ORDER BY created_at DESC LIMIT 30")
    fun getAllStories(): Flow<List<StoryEntity>>

    /**
     * Get a single story by ID
     */
    @Query("SELECT * FROM generated_stories WHERE id = :storyId")
    suspend fun getStoryById(storyId: String): StoryEntity?

    /**
     * Get stories filtered by type
     */
    @Query("SELECT * FROM generated_stories WHERE type = :type ORDER BY created_at DESC")
    fun getStoriesByType(type: String): Flow<List<StoryEntity>>

    /**
     * Get favorite stories
     */
    @Query("SELECT * FROM generated_stories WHERE is_favorite = 1 ORDER BY created_at DESC")
    fun getFavoriteStories(): Flow<List<StoryEntity>>

    /**
     * Get total story count
     */
    @Query("SELECT COUNT(*) FROM generated_stories")
    suspend fun getStoryCount(): Int

    /**
     * Insert a new story
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: StoryEntity)

    /**
     * Update existing story (for favorite toggle, etc.)
     */
    @Update
    suspend fun updateStory(story: StoryEntity)

    /**
     * Delete a story
     */
    @Delete
    suspend fun deleteStory(story: StoryEntity)

    /**
     * Delete stories older than timestamp
     * For automatic cleanup (e.g., older than 30 days)
     */
    @Query("DELETE FROM generated_stories WHERE created_at < :timestamp")
    suspend fun deleteOldStories(timestamp: Long)

    /**
     * Delete all stories (for user data cleanup)
     */
    @Query("DELETE FROM generated_stories")
    suspend fun deleteAllStories()

    // ==================== QUOTA OPERATIONS ====================

    /**
     * Get today's quota
     */
    @Query("SELECT * FROM story_quota WHERE date = :date")
    suspend fun getQuotaForDate(date: String): StoryQuotaEntity?

    /**
     * Insert or update quota
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuota(quota: StoryQuotaEntity)

    /**
     * Increment quota count for today
     */
    @Query("UPDATE story_quota SET count = count + 1 WHERE date = :date")
    suspend fun incrementQuota(date: String)

    /**
     * Delete old quota records (cleanup)
     */
    @Query("DELETE FROM story_quota WHERE date < :date")
    suspend fun deleteOldQuotas(date: String)
}