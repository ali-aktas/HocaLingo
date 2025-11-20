package com.hocalingo.app.feature.ai

import com.hocalingo.app.core.base.Result
import com.hocalingo.app.feature.ai.models.GeneratedStory
import com.hocalingo.app.feature.ai.models.StoryDifficulty
import com.hocalingo.app.feature.ai.models.StoryLength
import com.hocalingo.app.feature.ai.models.StoryType
import kotlinx.coroutines.flow.Flow

/**
 * StoryRepository - AI Story Feature Repository Interface
 *
 * Package: feature/ai/
 *
 * Business logic for AI story generation, storage, and retrieval.
 * Clean architecture: ViewModel -> Repository -> (API + Database)
 */
interface StoryRepository {

    /**
     * Generate a new story using Gemini API
     *
     * Process:
     * 1. Check daily quota
     * 2. Select learned words from database
     * 3. Call Gemini API with prompt
     * 4. Save to database
     * 5. Update quota
     *
     * @param topic User's desired topic (nullable)
     * @param type Story type (story, motivation, dialogue, article)
     * @param difficulty Word difficulty (easy, medium, hard)
     * @param length Story length (short, medium, long)
     * @return Result with generated story or error
     */
    suspend fun generateStory(
        topic: String?,
        type: StoryType,
        difficulty: StoryDifficulty,
        length: StoryLength
    ): Result<GeneratedStory>

    /**
     * Get all saved stories (Flow for real-time updates)
     * Ordered by creation date (newest first)
     * Limited to last 30 stories
     */
    fun getAllStories(): Flow<List<GeneratedStory>>

    /**
     * Get story by ID
     */
    suspend fun getStoryById(storyId: String): Result<GeneratedStory>

    /**
     * Toggle favorite status
     */
    suspend fun toggleFavorite(storyId: String): Result<Unit>

    /**
     * Delete a story
     */
    suspend fun deleteStory(storyId: String): Result<Unit>

    /**
     * Check daily quota
     * @return Remaining stories (0-2)
     */
    suspend fun checkDailyQuota(): Result<Int>

    /**
     * Get quota info (for UI display)
     * @return Pair(used, total) - e.g. (1, 2) = "1/2 kalan"
     */
    suspend fun getQuotaInfo(): Result<Pair<Int, Int>>

    /**
     * Get English words for a story (for highlighting)
     * @param wordIds List of word IDs used in story
     * @return List of English words
     */
    suspend fun getEnglishWordsForStory(wordIds: List<Int>): Result<List<String>>
}