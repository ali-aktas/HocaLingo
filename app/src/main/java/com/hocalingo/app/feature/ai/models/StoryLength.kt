package com.hocalingo.app.feature.ai.models

/**
 * StoryLength - Story Length Classification Enum
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/ai/models/
 *
 * Determines the length of generated content:
 * - SHORT: ~150 words (quick read, 1-2 minutes)
 * - MEDIUM: ~300 words (standard length, 3-4 minutes)
 * - LONG: ~500 words (deep dive, 5-7 minutes)
 *
 * Word count is approximate - AI may vary slightly.
 */
enum class StoryLength(
    val displayName: String,
    val icon: String,
    val dbValue: String,
    val targetWordCount: Int,
    val estimatedReadTime: String
) {
    /**
     * Short story/article
     * Perfect for quick practice
     */
    SHORT(
        displayName = "Kısa",
        icon = "📄",
        dbValue = "short",
        targetWordCount = 150,
        estimatedReadTime = "1-2 dk"
    ),

    /**
     * Medium length content
     * Most popular choice
     * Good balance of depth and brevity
     */
    MEDIUM(
        displayName = "Orta",
        icon = "📃",
        dbValue = "medium",
        targetWordCount = 300,
        estimatedReadTime = "3-4 dk"
    ),

    /**
     * Long form content
     * Premium feature (may require upgrade)
     * Deep practice with extensive context
     */
    LONG(
        displayName = "Uzun",
        icon = "📚",
        dbValue = "long",
        targetWordCount = 500,
        estimatedReadTime = "5-7 dk"
    );

    companion object {
        /**
         * Convert database string to enum
         */
        fun fromDbValue(value: String): StoryLength {
            return entries.find { it.dbValue == value } ?: MEDIUM
        }
    }
}