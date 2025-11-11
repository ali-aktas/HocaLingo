package com.hocalingo.app.feature.ai.models

/**
 * StoryType - Story Classification Enum
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/ai/models/
 *
 * Defines different types of content that can be generated.
 * Each type has display name and emoji for UI.
 *
 * Usage:
 * - User selects type in StoryCreatorDialog
 * - Sent to Gemini API for appropriate prompt
 * - Stored in database for filtering
 */
enum class StoryType(
    val displayName: String,
    val icon: String,
    val dbValue: String
) {
    /**
     * Creative narrative story
     * Most popular type
     */
    STORY(
        displayName = "Hikaye",
        icon = "📖",
        dbValue = "story"
    ),

    /**
     * Motivational/inspirational content
     * Short and uplifting
     */
    MOTIVATION(
        displayName = "Motivasyon",
        icon = "💪",
        dbValue = "motivation"
    ),

    /**
     * Conversational dialogue
     * Practice real-world scenarios
     */
    DIALOGUE(
        displayName = "Diyalog",
        icon = "💬",
        dbValue = "dialogue"
    ),

    /**
     * Educational article/blog post
     * More formal tone
     */
    ARTICLE(
        displayName = "Makale",
        icon = "📰",
        dbValue = "article"
    );

    companion object {
        /**
         * Convert database string to enum
         */
        fun fromDbValue(value: String): StoryType {
            return entries.find { it.dbValue == value } ?: STORY
        }
    }
}