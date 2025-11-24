package com.hocalingo.app.feature.study

import com.hocalingo.app.core.feedback.FeedbackCategory
import com.hocalingo.app.core.feedback.SatisfactionLevel
import com.hocalingo.app.database.entities.ConceptEntity
import com.hocalingo.app.database.entities.StudyDirection

/**
 * StudyUiState - Complete State with AdMob Support
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/study/
 *
 * ✅ Fixed to include all necessary properties
 * ✅ AdMob support added
 */
data class StudyUiState(
    // ========== LOADING & ERROR ==========
    val isLoading: Boolean = false,
    val error: String? = null,

    val showRewardedAdDialog: Boolean = false,

    // ========== CURRENT CONCEPT ==========
    val currentConcept: ConceptEntity? = null,
    val isCardFlipped: Boolean = false,

    // ========== STUDY DIRECTION ==========
    val studyDirection: StudyDirection = StudyDirection.EN_TO_TR,

    // ========== QUEUE MANAGEMENT ==========
    val studyQueue: List<ConceptEntity> = emptyList(),
    val currentCardIndex: Int = 0,
    val remainingCards: Int = 0,
    val totalWordsInQueue: Int = 0,
    val hasWordsToStudy: Boolean = false,
    val isQueueEmpty: Boolean = false,
    val showEmptyQueueMessage: Boolean = false,

    // ========== SESSION STATS ==========
    val sessionWordsCount: Int = 0,
    val correctAnswers: Int = 0,
    val wordsStudiedToday: Int = 0,
    val dailyGoal: Int = 10,
    val dailyProgressPercentage: Float = 0f,

    // ========== TTS & SOUND ==========
    val isTtsEnabled: Boolean = true,
    val isSpeaking: Boolean = false,

    // ========== BUTTON TIMING TEXTS (SM-2 Algorithm) ==========
    val easyTimeText: String = "",
    val mediumTimeText: String = "",
    val hardTimeText: String = "",

    // ========== RATING ==========
    val showSatisfactionDialog: Boolean = false,
    val showFeedbackDialog: Boolean = false,
    val selectedSatisfactionLevel: SatisfactionLevel? = null,

    // ========== ADMOB ==========
    val showNativeAd: Boolean = false,
    val wordsCompletedCount: Int = 0
) {
    /**
     * Current word index (1-based for display)
     */
    val currentWordIndex: Int
        get() = currentCardIndex

    /**
     * Progress percentage
     */
    val progressPercentage: Float
        get() = if (totalWordsInQueue > 0) {
            (currentCardIndex.toFloat() / totalWordsInQueue.toFloat()) * 100f
        } else 0f

    /**
     * Front text based on study direction
     */
    val frontText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.english ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.turkish ?: ""
        }

    /**
     * Back text based on study direction
     */
    val backText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.turkish ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.english ?: ""
        }

    /**
     * Should show TTS button
     */
    val shouldShowTtsButton: Boolean
        get() = isTtsEnabled && currentConcept != null

    /**
     * TTS button should be on front side (EN→TR) or back side (TR→EN)
     * EN→TR: true (front = English word)
     * TR→EN: false (back = English word)
     */
    val showTtsOnFrontSide: Boolean
        get() = studyDirection == StudyDirection.EN_TO_TR

    /**
     * Front example sentence
     */
    val frontExampleText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.exampleEn ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.exampleTr ?: ""
        }

    /**
     * Back example sentence
     */
    val backExampleText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.exampleTr ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.exampleEn ?: ""
        }
}

/**
 * User Events for Study Screen
 */
sealed interface StudyEvent {
    // ========== CARD INTERACTION ==========
    data object FlipCard : StudyEvent
    data object ResetCard : StudyEvent
    data object CloseNativeAd : StudyEvent

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

    // ========== ADMOB EVENTS ==========
    data object ContinueAfterAd : StudyEvent
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

    // ========== ADMOB EFFECTS ==========
    data object ShowStudyRewardedAd : StudyEffect
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