package com.hocalingo.app.core.feedback

/**
 * Rating & Feedback Models
 * ========================
 * Data structures for the two-step rating system
 *
 * Flow:
 * 1. User sees SatisfactionDialog (emoji selection)
 * 2. Based on emoji:
 *    - Positive (😍😊) → Native Store Rating
 *    - Negative (😐😞) → Feedback Form
 *
 * Package: app/src/main/java/com/hocalingo/app/core/feedback/
 */

/**
 * User satisfaction level - First step dialog
 * Maps to emoji selection in UI
 */
enum class SatisfactionLevel(
    val emoji: String,
    val displayName: String,
    val isPositive: Boolean
) {
    VERY_HAPPY("😍", "Çok iyi", true),
    HAPPY("😊", "İyi", true),
    NEUTRAL("😐", "Orta", false),
    UNHAPPY("😞", "Kötü", false);

    companion object {
        fun fromEmoji(emoji: String): SatisfactionLevel? {
            return values().find { it.emoji == emoji }
        }
    }
}

/**
 * Feedback category for negative users
 */
enum class FeedbackCategory(
    val icon: String,
    val displayName: String
) {
    BUG("🐛", "Hata Bildirimi"),
    FEATURE("💡", "Öneri"),
    CONTENT("📚", "İçerik Talebi"),
    OTHER("💬", "Diğer");

    companion object {
        fun getDefault() = OTHER
    }
}

/**
 * Feedback form data - Sent to Firebase
 */
data class FeedbackData(
    val userId: String,
    val satisfactionLevel: SatisfactionLevel,
    val category: FeedbackCategory,
    val message: String,
    val contactEmail: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String,
    val deviceInfo: String
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "userId" to userId,
        "satisfactionLevel" to satisfactionLevel.name,
        "category" to category.name,
        "message" to message,
        "contactEmail" to contactEmail,
        "timestamp" to timestamp,
        "appVersion" to appVersion,
        "deviceInfo" to deviceInfo
    )
}

/**
 * Rating prompt trigger conditions
 * Used by RatingManager to decide when to show prompt
 */
data class RatingPromptConditions(
    val minStudySessions: Int = 3,
    val minWordsStudiedInSession: Int = 5,
    val minAccuracyPercentage: Float = 60f,
    val cooldownDays: Int = 90,
    val maxPromptsPerYear: Int = 2
) {
    companion object {
        fun getDefault() = RatingPromptConditions()
    }
}

/**
 * Rating prompt state - Stored in SharedPreferences
 */
data class RatingPromptState(
    val totalPromptsShown: Int = 0,
    val lastPromptTimestamp: Long = 0L,
    val lastPromptYear: Int = 0,
    val hasUserRatedBefore: Boolean = false,
    val userOptedOut: Boolean = false
) {
    /**
     * Check if enough time passed since last prompt
     */
    fun canShowPrompt(cooldownDays: Int): Boolean {
        if (userOptedOut || hasUserRatedBefore) return false

        val now = System.currentTimeMillis()
        val daysSinceLastPrompt = (now - lastPromptTimestamp) / (1000 * 60 * 60 * 24)

        return daysSinceLastPrompt >= cooldownDays
    }

    /**
     * Check if within yearly limit (Apple: 3, We use: 2)
     */
    fun canShowPromptThisYear(maxPromptsPerYear: Int): Boolean {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        // New year, reset counter
        if (currentYear > lastPromptYear) return true

        // Same year, check limit
        return totalPromptsShown < maxPromptsPerYear
    }

    /**
     * Update after showing prompt
     */
    fun afterPromptShown(): RatingPromptState {
        val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)

        return copy(
            totalPromptsShown = if (currentYear == lastPromptYear) totalPromptsShown + 1 else 1,
            lastPromptTimestamp = System.currentTimeMillis(),
            lastPromptYear = currentYear
        )
    }
}

/**
 * Rating metrics for analytics
 */
data class RatingMetrics(
    val promptsShown: Int = 0,
    val satisfactionVeryHappy: Int = 0,
    val satisfactionHappy: Int = 0,
    val satisfactionNeutral: Int = 0,
    val satisfactionUnhappy: Int = 0,
    val storeRatingClicked: Int = 0,
    val feedbackFormsSubmitted: Int = 0
) {
    val totalResponses: Int
        get() = satisfactionVeryHappy + satisfactionHappy + satisfactionNeutral + satisfactionUnhappy

    val positiveResponseRate: Float
        get() = if (totalResponses > 0) {
            (satisfactionVeryHappy + satisfactionHappy).toFloat() / totalResponses.toFloat()
        } else 0f

    val storeClickConversionRate: Float
        get() = if (totalResponses > 0) {
            storeRatingClicked.toFloat() / totalResponses.toFloat()
        } else 0f
}