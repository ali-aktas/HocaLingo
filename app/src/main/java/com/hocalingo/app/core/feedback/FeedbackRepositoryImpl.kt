package com.hocalingo.app.core.feedback

import com.google.firebase.firestore.FirebaseFirestore
import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.DebugHelper
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FeedbackRepositoryImpl
 * ======================
 * Firebase implementation of FeedbackRepository
 *
 * Collections structure:
 * - "feedback" → Full feedback forms
 * - "satisfaction_metrics" → Quick satisfaction tracking
 * - "rating_clicks" → Store rating button clicks
 *
 * Package: app/src/main/java/com/hocalingo/app/core/feedback/
 */
@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FeedbackRepository {

    companion object {
        private const val COLLECTION_FEEDBACK = "feedback"
        private const val COLLECTION_SATISFACTION = "satisfaction_metrics"
        private const val COLLECTION_RATING_CLICKS = "rating_clicks"
    }

    /**
     * Submit full feedback form to Firebase
     */
    override suspend fun submitFeedback(feedbackData: FeedbackData): Result<Unit> {
        return try {
            firestore.collection(COLLECTION_FEEDBACK)
                .add(feedbackData.toMap())
                .await()

            DebugHelper.log("Feedback submitted successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            DebugHelper.logError("Failed to submit feedback", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Track satisfaction emoji selection (analytics)
     */
    override suspend fun trackSatisfactionResponse(
        userId: String,
        satisfactionEmoji: String
    ): Result<Unit> {
        return try {
            val data = mapOf(
                "userId" to userId,
                "emoji" to satisfactionEmoji,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection(COLLECTION_SATISFACTION)
                .add(data)
                .await()

            DebugHelper.log("Satisfaction tracked: $satisfactionEmoji")
            Result.Success(Unit)
        } catch (e: Exception) {
            DebugHelper.logError("Failed to track satisfaction", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Track store rating button click (analytics)
     */
    override suspend fun trackStoreRatingClick(userId: String): Result<Unit> {
        return try {
            val data = mapOf(
                "userId" to userId,
                "timestamp" to System.currentTimeMillis()
            )

            firestore.collection(COLLECTION_RATING_CLICKS)
                .add(data)
                .await()

            DebugHelper.log("Store rating click tracked")
            Result.Success(Unit)
        } catch (e: Exception) {
            DebugHelper.logError("Failed to track rating click", e)
            Result.Error(AppError.Unknown(e))
        }
    }
}