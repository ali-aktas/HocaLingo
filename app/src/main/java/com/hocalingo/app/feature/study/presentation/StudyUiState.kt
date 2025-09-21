package com.hocalingo.app.feature.study.presentation

import com.hocalingo.app.core.database.entities.ConceptEntity
import com.hocalingo.app.core.database.entities.StudyDirection

/**
 * UI State for Study Screen
 *
 * Simplified version focused on:
 * - Card display and flip state
 * - Daily progress tracking (not accuracy)
 * - Study queue management
 * - Error and loading states
 */
data class StudyUiState(
    // Loading & Error States
    val isLoading: Boolean = false,
    val error: String? = null,

    // Study Content
    val currentConcept: ConceptEntity? = null,
    val isCardFlipped: Boolean = false,
    val studyDirection: StudyDirection = StudyDirection.EN_TO_TR,

    // Queue Management
    val totalWordsInQueue: Int = 0,
    val currentWordIndex: Int = 0,
    val hasWordsToStudy: Boolean = false,
    val isQueueEmpty: Boolean = false,
    val showEmptyQueueMessage: Boolean = false,

    // Daily Progress (Simplified - Only Daily Goal)
    val wordsStudiedToday: Int = 0,
    val dailyGoal: Int = 20,
    val dailyProgressPercentage: Float = 0f,

    // Session Info (Simple)
    val sessionWordsCount: Int = 0,
    val correctAnswers: Int = 0,

    // Button Timing Text (SM-2 Algorithm Results)
    val easyTimeText: String = "",
    val mediumTimeText: String = "",
    val hardTimeText: String = "",

    // TTS State
    val isTtsEnabled: Boolean = true,
    val isSpeaking: Boolean = false
) {

    /**
     * Progress percentage for current study queue
     * This shows how many words completed in current session
     */
    val progressPercentage: Float
        get() = if (totalWordsInQueue > 0) {
            (currentWordIndex.toFloat() / totalWordsInQueue.toFloat()) * 100f
        } else 0f

    /**
     * Daily progress calculation - only based on daily goal
     * When a card is moved to next day, this progress increases
     */
    val dailyProgressCalculated: Float
        get() = if (dailyGoal > 0) {
            (wordsStudiedToday.toFloat() / dailyGoal.toFloat() * 100f).coerceAtMost(100f)
        } else 0f

    /**
     * Text to show on front of card based on study direction
     */
    val frontText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.english ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.turkish ?: ""
        }

    /**
     * Text to show on back of card based on study direction
     */
    val backText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.turkish ?: ""
            StudyDirection.TR_TO_EN -> currentConcept?.english ?: ""
        }

    /**
     * FIXED: Front example sentence (ön yüzde gösterilecek)
     */
    val frontExampleText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.exampleEn ?: "" // İngilizce ön yüz → İngilizce örnek
            StudyDirection.TR_TO_EN -> currentConcept?.exampleTr ?: "" // Türkçe ön yüz → Türkçe örnek
        }

    /**
     * FIXED: Back example sentence (arka yüzde gösterilecek)
     */
    val backExampleText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.exampleTr ?: "" // Türkçe arka yüz → Türkçe örnek
            StudyDirection.TR_TO_EN -> currentConcept?.exampleEn ?: "" // İngilizce arka yüz → İngilizce örnek
        }

    /**
     * NEW: TTS için doğru dil ve metin
     */
    val pronunciationText: String
        get() = when (studyDirection) {
            StudyDirection.EN_TO_TR -> currentConcept?.english ?: "" // İngilizce kelimeyi oku
            StudyDirection.TR_TO_EN -> currentConcept?.turkish ?: "" // Türkçe kelimeyi oku
        }

    /**
     * NEW: TTS butonunu ne zaman göster
     */
    val shouldShowTtsButton: Boolean
        get() = pronunciationText.isNotEmpty() && isTtsEnabled
}

/**
 * User Events for Study Screen - Simplified
 */
sealed interface StudyEvent {
    // Card Interaction
    data object FlipCard : StudyEvent
    data object ResetCard : StudyEvent

    // Study Response Buttons (SM-2 Algorithm)
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

    // Removed: Settings and direction toggle (moved to profile)
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
 * Session completion statistics - Simplified
 */
data class SessionStats(
    val wordsStudied: Int,
    val correctAnswers: Int,
    val timeSpentMs: Long,
    val newWordsLearned: Int,
    val wordsReviewed: Int
) {
    /**
     * Simple accuracy calculation
     */
    val accuracy: Float
        get() = if (wordsStudied > 0) {
            (correctAnswers.toFloat() / wordsStudied.toFloat()) * 100f
        } else 0f
}