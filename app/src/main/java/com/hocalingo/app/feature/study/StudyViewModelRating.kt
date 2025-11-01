package com.hocalingo.app.feature.study

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.feedback.FeedbackCategory
import com.hocalingo.app.core.feedback.FeedbackData
import com.hocalingo.app.core.feedback.SatisfactionLevel
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * StudyViewModelRating.kt
 * =======================
 * Rating logic extension for StudyViewModel
 * ✅ Session completion rating check
 * ✅ Satisfaction dialog handling
 * ✅ Feedback form submission
 * ✅ Native store rating trigger
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/study/
 *
 * NOT: Bu dosya StudyViewModel'e extension fonksiyonlar olarak eklenecek
 */

// ========== RATING LOGIC ==========

/**
 * Check if we should show rating prompt after session
 */
fun StudyViewModel.checkAndShowRatingPrompt(wordsStudied: Int, correctAnswers: Int) {
    viewModelScope.launch {
        try {
            val accuracy = if (wordsStudied > 0) {
                (correctAnswers.toFloat() / wordsStudied.toFloat()) * 100f
            } else 0f

            val shouldShow = getRatingManager().shouldShowRatingPrompt(
                wordsStudiedInSession = wordsStudied,
                accuracyPercentage = accuracy
            )

            if (shouldShow) {
                DebugHelper.log("✨ Showing rating prompt after session")
                // Wait a bit for session complete animation
                kotlinx.coroutines.delay(1500)
                updateUiState { it.copy(showSatisfactionDialog = true) }
                getRatingManager().markPromptShown()
            }

            // Always increment session counter
            getRatingManager().incrementStudySession()

        } catch (e: Exception) {
            DebugHelper.logError("Rating check error", e)
        }
    }
}

/**
 * Show satisfaction dialog
 */
fun StudyViewModel.showSatisfactionDialog() {
    updateUiState { it.copy(showSatisfactionDialog = true) }
}

/**
 * Dismiss satisfaction dialog
 */
fun StudyViewModel.dismissSatisfactionDialog() {
    updateUiState {
        it.copy(
            showSatisfactionDialog = false,
            selectedSatisfactionLevel = null
        )
    }
    // Mark as declined
    viewModelScope.launch {
        getRatingManager().markUserDeclined()
    }
}

/**
 * Handle satisfaction level selection
 */
fun StudyViewModel.handleSatisfactionSelected(level: SatisfactionLevel) {
    viewModelScope.launch {
        try {
            // Track satisfaction
            val userId = getCurrentUserId()
            getFeedbackRepository().trackSatisfactionResponse(userId, level.emoji)

            updateUiState {
                it.copy(
                    showSatisfactionDialog = false,
                    selectedSatisfactionLevel = level
                )
            }

            if (level.isPositive) {
                // Positive: Launch native store rating
                DebugHelper.log("Positive feedback - launching store rating")
                getFeedbackRepository().trackStoreRatingClick(userId)
                emitEffect(StudyEffect.LaunchNativeStoreRating)
            } else {
                // Negative: Show feedback form
                DebugHelper.log("Negative feedback - showing feedback form")
                updateUiState { it.copy(showFeedbackDialog = true) }
            }

        } catch (e: Exception) {
            DebugHelper.logError("Satisfaction handling error", e)
        }
    }
}

/**
 * Dismiss feedback dialog
 */
fun StudyViewModel.dismissFeedbackDialog() {
    updateUiState {
        it.copy(
            showFeedbackDialog = false,
            selectedSatisfactionLevel = null
        )
    }
}

/**
 * Submit feedback form
 */
fun StudyViewModel.submitFeedback(
    category: FeedbackCategory,
    message: String,
    email: String?
) {
    viewModelScope.launch {
        try {
            // ✅ Arka planda Firebase'e göndermeyi dene (sessizce)
            val userId = getCurrentUserId()
            val satisfactionLevel = getUiState().selectedSatisfactionLevel
                ?: SatisfactionLevel.NEUTRAL

            val feedbackData = FeedbackData(
                userId = userId,
                satisfactionLevel = satisfactionLevel,
                category = category,
                message = message,
                contactEmail = email,
                appVersion = getAppVersion(),
                deviceInfo = getDeviceInfo()
            )

            // Arka planda gönder, sonucu önemseme
            launch {
                try {
                    getFeedbackRepository().submitFeedback(feedbackData)
                    DebugHelper.logSuccess("Feedback sent successfully")
                } catch (e: Exception) {
                    DebugHelper.log("Feedback send failed (silent): ${e.message}")
                }
            }

            // ✅ Kullanıcıya hemen başarılı mesajı göster
            kotlinx.coroutines.delay(500) // Kısa delay (gerçekçi olsun)

            updateUiState {
                it.copy(
                    showFeedbackDialog = false,
                    selectedSatisfactionLevel = null
                )
            }

            emitEffect(StudyEffect.ShowMessage("Geri bildiriminiz için teşekkürler! 💙"))

        } catch (e: Exception) {
            DebugHelper.logError("Feedback UI error", e)
            // Yine de başarılı gibi göster
            updateUiState {
                it.copy(
                    showFeedbackDialog = false,
                    selectedSatisfactionLevel = null
                )
            }
            emitEffect(StudyEffect.ShowMessage("Geri bildiriminiz için teşekkürler! 💙"))
        }
    }
}

// ========== HELPER FUNCTIONS ==========

/**
 * Get current user ID from Firebase
 */
private fun getCurrentUserId(): String {
    return FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"
}

/**
 * Get app version
 */
private fun getAppVersion(): String {
    return "1.0.0" // TODO: Get from BuildConfig
}

/**
 * Get device info
 */
private fun getDeviceInfo(): String {
    return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
}

// ========== ACCESSOR FUNCTIONS ==========
// NOT: Bu fonksiyonlar StudyViewModel'e eklenmeli (private field'lara erişim için)

/**
 * StudyViewModel içine eklenecek accessor fonksiyonlar:
 *
 * internal fun getRatingManager() = ratingManager
 * internal fun getFeedbackRepository() = feedbackRepository
 * internal fun updateUiState(update: (StudyUiState) -> StudyUiState) {
 *     _uiState.update(update)
 * }
 * internal fun emitEffect(effect: StudyEffect) {
 *     viewModelScope.launch { _effect.emit(effect) }
 * }
 * internal fun getUiState() = _uiState.value
 */