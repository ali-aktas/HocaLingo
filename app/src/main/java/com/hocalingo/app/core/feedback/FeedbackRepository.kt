package com.hocalingo.app.core.feedback

import com.hocalingo.app.core.base.Result

/**
 * FeedbackRepository
 * ==================
 * Repository interface for submitting user feedback
 *
 * Responsibilities:
 * - Submit feedback to Firebase
 * - Track feedback metrics
 * - Handle errors gracefully
 *
 * Package: app/src/main/java/com/hocalingo/app/core/feedback/
 */
interface FeedbackRepository {

    /**
     * Submit user feedback to backend
     *
     * @param feedbackData Complete feedback information
     * @return Result<Unit> Success or error
     */
    suspend fun submitFeedback(feedbackData: FeedbackData): Result<Unit>

    /**
     * Track satisfaction response (for analytics)
     * Called even if user doesn't submit full feedback
     *
     * @param userId User ID
     * @param satisfactionEmoji Selected emoji
     */
    suspend fun trackSatisfactionResponse(
        userId: String,
        satisfactionEmoji: String
    ): Result<Unit>

    /**
     * Track native store rating click
     * Called when user clicks "Rate" button
     *
     * @param userId User ID
     */
    suspend fun trackStoreRatingClick(userId: String): Result<Unit>
}