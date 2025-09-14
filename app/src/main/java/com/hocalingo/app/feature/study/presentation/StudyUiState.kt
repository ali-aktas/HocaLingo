package com.hocalingo.app.feature.study.presentation

import com.hocalingo.app.core.database.entities.ConceptEntity
import com.hocalingo.app.core.database.entities.StudyDirection

/**
 * UI State for Study Screen
 *
 * Manages current study session state including:
 * - Current word being studied
 * - Study queue and progress
 * - Card flip state
 * - Session statistics
 */
data class StudyUiState(
    // Loading and Error States
    val isLoading: Boolean = false,
    val error: String? = null,

    // Current Study Session
    val currentConcept: ConceptEntity? = null,
    val studyDirection: StudyDirection = StudyDirection.EN_TO_TR,
    val isCardFlipped: Boolean = false,

    // Study Queue Management
    val totalWordsInQueue: Int = 0,
    val currentWordIndex: Int = 0,
    val remainingWords: Int = 0,

    // Session Progress
    val wordsStudiedToday: Int = 0,
    val dailyGoal: Int = 20,
    val sessionWordsCount: Int = 0,
    val correctAnswers: Int = 0,

    // Button States with Dynamic Times
    val easyButtonText: String = "Kolay",
    val mediumButtonText: String = "Orta",
    val hardButtonText: String = "Zor",
    val easyTimeText: String = "",
    val mediumTimeText: String = "",
    val hardTimeText: String = "",

    // Empty States
    val isQueueEmpty: Boolean = false,
    val showCompletionDialog: Boolean = false,
    val showEmptyQueueMessage: Boolean = false,

    // TTS State
    val isTtsEnabled: Boolean = true,
    val isTtsSpeaking: Boolean = false,

    // Settings
    val showPronunciation: Boolean = true,
    val showExamples: Boolean = true
) {
    /**
     * Calculated properties for UI
     */
    val progressPercentage: Float
        get() = if (totalWordsInQueue > 0) {
            (currentWordIndex.toFloat() / totalWordsInQueue.toFloat()) * 100f
        } else 0f

    val dailyProgressPercentage: Float
        get() = if (dailyGoal > 0) {
            (wordsStudiedToday.toFloat() / dailyGoal.toFloat()) * 100f
        } else 0f

    val accuracyPercentage: Float
        get() = if (sessionWordsCount > 0) {
            (correctAnswers.toFloat() / sessionWordsCount.toFloat()) * 100f
        } else 0f

    val hasWordsToStudy: Boolean
        get() = !isQueueEmpty && currentConcept != null

    val showSessionStats: Boolean
        get() = sessionWordsCount > 0

    val canFlipCard: Boolean
        get() = currentConcept != null && !isLoading

    /**
     * Get display text based on study direction
     */
    val frontText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.english ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.turkish ?: ""
        }

    val backText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.turkish ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.english ?: ""
        }

    val exampleText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.exampleEn ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.exampleTr ?: ""
        }

    val pronunciationText: String
        get() = currentConcept?.pronunciation ?: ""
}

/**
 * User Events for Study Screen
 */
sealed interface StudyEvent {
    // Card Interaction
    data object FlipCard : StudyEvent
    data object ResetCard : StudyEvent

    // Study Response Buttons
    data object EasyButtonPressed : StudyEvent
    data object MediumButtonPressed : StudyEvent
    data object HardButtonPressed : StudyEvent

    // TTS Controls
    data object PlayPronunciation : StudyEvent
    data object StopTts : StudyEvent

    // Session Management
    data object LoadStudyQueue : StudyEvent
    data object EndSession : StudyEvent
    data object RetryLoading : StudyEvent

    // Navigation
    data object NavigateToWordSelection : StudyEvent
    data object NavigateBack : StudyEvent

    // Settings
    data class ToggleStudyDirection(val direction: StudyDirection) : StudyEvent
}

/**
 * One-time UI Effects for Study Screen
 */
sealed interface StudyEffect {
    // Navigation Effects
    data object NavigateToHome : StudyEffect
    data object NavigateToWordSelection : StudyEffect

    // UI Feedback
    data class ShowMessage(val message: String) : StudyEffect
    data class PlaySound(val soundType: StudySoundType) : StudyEffect
    data class ShowSessionComplete(val stats: SessionStats) : StudyEffect

    // TTS Effects
    data class SpeakText(val text: String, val language: String = "en") : StudyEffect

    // Haptic Feedback
    data class HapticFeedback(val type: HapticType) : StudyEffect
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
    val accuracy: Float,
    val timeSpentMs: Long,
    val newWordsLearned: Int,
    val wordsReviewed: Int
)