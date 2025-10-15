package com.hocalingo.app.feature.study

import com.hocalingo.app.database.entities.ConceptEntity
import com.hocalingo.app.database.entities.StudyDirection
import com.hocalingo.app.core.feedback.SatisfactionLevel
import com.hocalingo.app.core.feedback.FeedbackCategory

/**
 * StudyUiState.kt - REFACTORED
 * =============================
 * State, Events, Effects separated from ViewModel
 * ✅ Mevcut tüm özellikler korundu
 * ✅ Rating özellikleri eklendi
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/study/
 */

/**
 * Main UI State for Study Screen
 */
data class StudyUiState(
    // ========== LOADING & ERROR ==========
    val isLoading: Boolean = false,
    val error: String? = null,

    // ========== CURRENT STUDY CARD ==========
    val currentConcept: ConceptEntity? = null,
    val isCardFlipped: Boolean = false,
    val currentCardIndex: Int = 0,
    val currentWordIndex: Int = 0,
    val remainingCards: Int = 0,

    // ========== QUEUE MANAGEMENT ==========
    val totalWordsInQueue: Int = 0,
    val hasWordsToStudy: Boolean = false,

    // ========== SESSION INFO ==========
    val sessionWordsCount: Int = 0,
    val correctAnswers: Int = 0,
    val studyDirection: StudyDirection = StudyDirection.EN_TO_TR,

    // ========== BUTTON TIMING TEXT (SM-2 Algorithm) ==========
    val easyTimeText: String = "",
    val mediumTimeText: String = "",
    val hardTimeText: String = "",

    // ========== PROGRESS ==========
    val wordsStudiedToday: Int = 0,
    val dailyGoal: Int = 20,
    val dailyProgressPercentage: Float = 0f,

    // ========== QUEUE STATE ==========
    val isQueueEmpty: Boolean = false,
    val showEmptyQueueMessage: Boolean = false,

    // ========== TTS ==========
    val isTtsEnabled: Boolean = true,
    val isSpeaking: Boolean = false,

    // ========== RATING PROMPT STATE ==========
    val showSatisfactionDialog: Boolean = false,
    val showFeedbackDialog: Boolean = false,
    val selectedSatisfactionLevel: SatisfactionLevel? = null
) {
    /**
     * Front side text based on study direction
     */
    val frontText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.english ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.turkish ?: ""
        }

    /**
     * Back side text based on study direction
     */
    val backText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.turkish ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.english ?: ""
        }

    /**
     * Example sentence for current card
     */
    val exampleText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.exampleTr ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.exampleEn ?: ""
        }

    /**
     * TTS için doğru dil ve metin
     */
    val pronunciationText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.english ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.turkish ?: ""
        }

    /**
     * TTS butonunu ne zaman göster
     */
    val shouldShowTtsButton: Boolean
        get() = pronunciationText.isNotEmpty() && isTtsEnabled
}

/**
 * User Events for Study Screen
 */
sealed interface StudyEvent {
    // ========== CARD INTERACTION ==========
    data object FlipCard : StudyEvent
    data object ResetCard : StudyEvent

    // ========== STUDY RESPONSE BUTTONS (SM-2 Algorithm) ==========
    data object EasyButtonPressed : StudyEvent
    data object MediumButtonPressed : StudyEvent
    data object HardButtonPressed : StudyEvent

    // ========== TTS CONTROLS ==========
    data object PlayPronunciation : StudyEvent
    data object StopTts : StudyEvent

    // ========== SESSION MANAGEMENT ==========
    data object LoadStudyQueue : StudyEvent
    data object EndSession : StudyEvent
    data object RetryLoading : StudyEvent

    // ========== NAVIGATION ==========
    data object NavigateToWordSelection : StudyEvent
    data object NavigateBack : StudyEvent

    // ========== RATING EVENTS ==========
    data object ShowSatisfactionDialog : StudyEvent
    data object DismissSatisfactionDialog : StudyEvent
    data class SatisfactionSelected(val level: SatisfactionLevel) : StudyEvent
    data object DismissFeedbackDialog : StudyEvent
    data class SubmitFeedback(
        val category: FeedbackCategory,
        val message: String,
        val email: String?
    ) : StudyEvent
}

/**
 * One-time UI Effects for Study Screen
 */
sealed interface StudyEffect {
    // ========== NAVIGATION EFFECTS ==========
    data object NavigateToHome : StudyEffect
    data object NavigateToWordSelection : StudyEffect

    // ========== UI FEEDBACK ==========
    data class ShowMessage(val message: String) : StudyEffect
    data class PlaySound(val soundType: StudySoundType) : StudyEffect
    data class ShowSessionComplete(val stats: SessionStats) : StudyEffect

    // ========== TTS EFFECTS ==========
    data class SpeakText(val text: String, val language: String = "en") : StudyEffect

    // ========== HAPTIC FEEDBACK ==========
    data class HapticFeedback(val type: HapticType) : StudyEffect

    // ========== RATING EFFECTS ==========
    data object LaunchNativeStoreRating : StudyEffect
}

/**
 * Sound types for audio feedback
 */
enum class StudySoundType {
    CORRECT,
    INCORRECT,
    CARD_FLIP,
    SESSION_COMPLETE,
    LEVEL_UP
}

/**
 * Haptic feedback types
 */
enum class HapticType {
    LIGHT,
    MEDIUM,
    HEAVY,
    SUCCESS,
    ERROR
}

/**
 * Session completion statistics
 */
data class SessionStats(
    val wordsStudied: Int,
    val correctAnswers: Int,
    val timeSpentMs: Long,
    val newWordsLearned: Int,
    val wordsReviewed: Int
) {
    val accuracy: Float
        get() = if (wordsStudied > 0) {
            (correctAnswers.toFloat() / wordsStudied.toFloat()) * 100f
        } else 0f
}