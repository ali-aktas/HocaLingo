package com.hocalingo.app.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * StoryEntity - AI-Generated Stories Database Table
 *
 * Package: app/src/main/java/com/hocalingo/app/database/entities/
 *
 * Stores user-generated stories created with their learned vocabulary.
 * Integrated with Room Database version 4.
 *
 * Features:
 * - Unique story ID
 * - Story content and metadata
 * - Used words tracking (JSON array)
 * - Type, difficulty, length classifications
 * - Timestamp for sorting/cleanup
 * - Favorite marking
 */
@Entity(
    tableName = "generated_stories",
    indices = [
        Index(value = ["created_at"]),
        Index(value = ["type"]),
        Index(value = ["is_favorite"])
    ]
)
data class StoryEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    /**
     * JSON array of word IDs used in the story
     * Example: "[1, 5, 12, 23, 45]"
     * Used for highlighting and tracking
     */
    @ColumnInfo(name = "used_words")
    val usedWords: String,

    /**
     * User's input topic/theme
     * Can be null if generated without specific topic
     */
    @ColumnInfo(name = "topic")
    val topic: String?,

    /**
     * Story type: "story", "motivation", "dialogue", "article"
     */
    @ColumnInfo(name = "type")
    val type: String,

    /**
     * Difficulty level: "easy", "medium", "hard"
     */
    @ColumnInfo(name = "difficulty")
    val difficulty: String,

    /**
     * Length classification: "short", "medium", "long"
     */
    @ColumnInfo(name = "length")
    val length: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    /**
     * Premium flag - for future analytics
     * All stories are premium feature
     */
    @ColumnInfo(name = "is_premium")
    val isPremium: Boolean = true
)