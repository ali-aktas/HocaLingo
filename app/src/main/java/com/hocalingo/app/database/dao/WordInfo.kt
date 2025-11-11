package com.hocalingo.app.database.dao

/**
 * WordInfo - Data class for word selection queries
 *
 * Package: database/dao/
 *
 * Used by:
 * - WordProgressDao (query result)
 * - StoryRepository (word selection)
 *
 * Represents minimal word info for AI story generation.
 */
data class WordInfo(
    val id: Int,
    val english: String,
    val turkish: String
)