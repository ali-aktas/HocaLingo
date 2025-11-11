package com.hocalingo.app.feature.ai.models

import com.hocalingo.app.database.entities.StoryEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.util.UUID

/**
 * GeneratedStory - UI Domain Model
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/ai/models/
 *
 * Clean domain model for UI layer.
 * Converts between StoryEntity (database) and UI representation.
 *
 * Benefits:
 * - Type-safe enums instead of strings
 * - Parsed word list instead of JSON
 * - Clean separation of concerns
 * - Easy to test and maintain
 */
@Serializable
data class GeneratedStory(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val usedWords: List<Int>, // Parsed word IDs
    val topic: String?,
    val type: StoryType,
    val difficulty: StoryDifficulty,
    val length: StoryLength,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isPremium: Boolean = true
) {
    /**
     * Convert to database entity
     */
    fun toEntity(): StoryEntity {
        return StoryEntity(
            id = id,
            title = title,
            content = content,
            usedWords = Json.encodeToString(serializer<List<Int>>(), usedWords),
            topic = topic,
            type = type.dbValue,
            difficulty = difficulty.dbValue,
            length = length.dbValue,
            createdAt = createdAt,
            isFavorite = isFavorite,
            isPremium = isPremium
        )
    }

    companion object {
        /**
         * Convert from database entity
         */
        fun fromEntity(entity: StoryEntity): GeneratedStory {
            return GeneratedStory(
                id = entity.id,
                title = entity.title,
                content = entity.content,
                usedWords = try {
                    Json.decodeFromString<List<Int>>(entity.usedWords)
                } catch (e: Exception) {
                    emptyList()
                },
                topic = entity.topic,
                type = StoryType.fromDbValue(entity.type),
                difficulty = StoryDifficulty.fromDbValue(entity.difficulty),
                length = StoryLength.fromDbValue(entity.length),
                createdAt = entity.createdAt,
                isFavorite = entity.isFavorite,
                isPremium = entity.isPremium
            )
        }
    }
}