package com.hocalingo.app.core.feedback

import android.app.Activity
import android.content.Context
import com.google.android.play.core.review.ReviewManagerFactory
import com.hocalingo.app.core.common.DebugHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * RatingManager
 * ==============
 * Central manager for rating prompt logic
 *
 * Responsibilities:
 * - Decide when to show rating prompt
 * - Trigger native Google Play In-App Review API
 * - Manage timing and frequency
 *
 * Package: app/src/main/java/com/hocalingo/app/core/feedback/
 */
@Singleton
class RatingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: RatingPreferencesManager
) {

    private val reviewManager = ReviewManagerFactory.create(context)
    private val conditions = RatingPromptConditions.getDefault()

    /**
     * Check if we should show rating prompt
     *
     * Conditions:
     * - User completed minimum study sessions
     * - Cooldown period passed
     * - Within yearly limit
     * - User hasn't opted out
     *
     * @param wordsStudiedInSession Current session word count
     * @param accuracyPercentage Current session accuracy
     * @return true if should show prompt
     */
    suspend fun shouldShowRatingPrompt(
        wordsStudiedInSession: Int,
        accuracyPercentage: Float
    ): Boolean {
        try {
            // Get current state
            val state = preferencesManager.ratingPromptState.first()
            val sessionsCompleted = preferencesManager.completedStudySessions.first()

            // Check basic conditions
            if (sessionsCompleted < conditions.minStudySessions) {
                DebugHelper.log("Not enough sessions: $sessionsCompleted/${conditions.minStudySessions}")
                return false
            }

            if (wordsStudiedInSession < conditions.minWordsStudiedInSession) {
                DebugHelper.log("Not enough words in session: $wordsStudiedInSession/${conditions.minWordsStudiedInSession}")
                return false
            }

            if (accuracyPercentage < conditions.minAccuracyPercentage) {
                DebugHelper.log("Accuracy too low: $accuracyPercentage%/${conditions.minAccuracyPercentage}%")
                return false
            }

            // Check timing conditions
            if (!state.canShowPrompt(conditions.cooldownDays)) {
                DebugHelper.log("Cooldown not passed or user opted out")
                return false
            }

            if (!state.canShowPromptThisYear(conditions.maxPromptsPerYear)) {
                DebugHelper.log("Yearly limit reached: ${state.totalPromptsShown}/${conditions.maxPromptsPerYear}")
                return false
            }

            DebugHelper.log("✅ All conditions met - can show rating prompt")
            return true

        } catch (e: Exception) {
            DebugHelper.logError("Error checking rating conditions", e)
            return false
        }
    }

    /**
     * Mark that prompt was shown
     * Updates preferences and metrics
     */
    suspend fun markPromptShown() {
        preferencesManager.markPromptShown()
        DebugHelper.log("Rating prompt shown - counters updated")
    }

    /**
     * Launch native Google Play In-App Review flow
     *
     * Note: Google controls when/if the dialog actually shows
     * - Might not show immediately
     * - Limited to 3 times per year per user
     * - No callback on user action
     *
     * @param activity Current activity
     */
    suspend fun launchNativeRating(activity: Activity) {
        try {
            DebugHelper.log("Requesting Google Play review flow...")

            val request = reviewManager.requestReviewFlow()
            request.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val reviewInfo = task.result
                    val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                    flow.addOnCompleteListener {
                        DebugHelper.log("Review flow completed")
                    }
                } else {
                    DebugHelper.logError("Review flow request failed", task.exception)
                }
            }

            // Mark as rated (prevent future prompts)
            preferencesManager.markUserRated()

        } catch (e: Exception) {
            DebugHelper.logError("Failed to launch native rating", e)
        }
    }

    /**
     * Mark that user clicked "Not Now"
     * Resets cooldown timer
     */
    suspend fun markUserDeclined() {
        preferencesManager.markPromptShown()
        DebugHelper.log("User declined rating - cooldown reset")
    }

    /**
     * Mark that user opted out permanently
     * Prevents all future prompts
     */
    suspend fun markUserOptedOut() {
        preferencesManager.markUserOptedOut()
        DebugHelper.log("User opted out - no future prompts")
    }

    /**
     * Increment study session counter
     * Called after each successful study session
     */
    suspend fun incrementStudySession() {
        preferencesManager.incrementStudySessions()
        val count = preferencesManager.completedStudySessions.first()
        DebugHelper.log("Study session completed: $count total")
    }

    /**
     * Get current prompt state (for debugging)
     */
    suspend fun getPromptState(): RatingPromptState {
        return preferencesManager.ratingPromptState.first()
    }

    /**
     * Reset all rating data (for testing)
     */
    suspend fun resetForTesting() {
        preferencesManager.resetRatingPreferences()
        DebugHelper.log("Rating preferences reset")
    }
}