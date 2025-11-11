package com.hocalingo.app.feature.ai.models

/**
 * StoryDifficulty - Story Difficulty Level Enum
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/ai/models/
 *
 * Determines which words will be selected from user's vocabulary:
 * - EASY: Recently learned words (interval < 90 days)
 * - MEDIUM: Moderately practiced words (interval < 30 days)
 * - HARD: Freshly learned words (interval < 15 days)
 *
 * Higher difficulty = more challenging vocabulary = better practice
 */
enum class StoryDifficulty(
    val displayName: String,
    val icon: String,
    val dbValue: String,
    val maxIntervalDays: Int
) {
    /**
     * Easy - Well-known words
     * Interval < 90 days
     * Most comfortable for reading
     */
    EASY(
        displayName = "Kolay",
        icon = "🟢",
        dbValue = "easy",
        maxIntervalDays = 90
    ),

    /**
     * Medium - Recently practiced words
     * Interval < 30 days
     * Good balance of challenge and readability
     */
    MEDIUM(
        displayName = "Orta",
        icon = "🟡",
        dbValue = "medium",
        maxIntervalDays = 30
    ),

    /**
     * Hard - Newly learned words
     * Interval < 15 days
     * Maximum challenge and reinforcement
     */
    HARD(
        displayName = "Zor",
        icon = "🔴",
        dbValue = "hard",
        maxIntervalDays = 15
    );

    companion object {
        /**
         * Convert database string to enum
         */
        fun fromDbValue(value: String): StoryDifficulty {
            return entries.find { it.dbValue == value } ?: MEDIUM
        }
    }
}