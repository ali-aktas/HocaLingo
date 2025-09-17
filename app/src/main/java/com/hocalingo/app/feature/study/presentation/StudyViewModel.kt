package com.hocalingo.app.feature.study.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.common.SpacedRepetitionAlgorithm
import com.hocalingo.app.core.common.TextToSpeechManager
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.core.database.entities.ConceptEntity
import com.hocalingo.app.core.database.entities.StudyDirection
import com.hocalingo.app.core.database.entities.SessionType
import com.hocalingo.app.feature.study.domain.StudyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * StudyViewModel - Simplified Study Screen Logic
 *
 * Manages:
 * - Study queue and current word
 * - SM-2 spaced repetition algorithm
 * - Card flip animations
 * - Daily progress tracking (simplified)
 * - TTS integration
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val preferencesManager: UserPreferencesManager,
    private val textToSpeechManager: TextToSpeechManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<StudyEffect>()
    val effect: SharedFlow<StudyEffect> = _effect.asSharedFlow()

    // Study session tracking
    private var currentSessionId: Long? = null
    private var sessionStartTime: Long = 0L
    private var studyQueue: List<ConceptEntity> = emptyList()
    private var currentQueueIndex: Int = 0

    init {
        DebugHelper.log("=== StudyViewModel BAŞLATILIYOR ===")
        loadInitialData()
    }

    /**
     * Handle user events - Simplified
     */
    fun onEvent(event: StudyEvent) {
        DebugHelper.log("StudyEvent: $event")

        when (event) {
            StudyEvent.FlipCard -> flipCard()
            StudyEvent.ResetCard -> resetCard()

            StudyEvent.EasyButtonPressed -> handleUserResponse(SpacedRepetitionAlgorithm.QUALITY_EASY)
            StudyEvent.MediumButtonPressed -> handleUserResponse(SpacedRepetitionAlgorithm.QUALITY_MEDIUM)
            StudyEvent.HardButtonPressed -> handleUserResponse(SpacedRepetitionAlgorithm.QUALITY_HARD)

            StudyEvent.PlayPronunciation -> playPronunciation()
            StudyEvent.StopTts -> stopTts()

            StudyEvent.LoadStudyQueue -> loadStudyQueue()
            StudyEvent.EndSession -> endCurrentSession()
            StudyEvent.RetryLoading -> retryLoading()

            StudyEvent.NavigateToWordSelection -> navigateToWordSelection()
            StudyEvent.NavigateBack -> navigateBack()

            // Settings events removed - moved to profile
        }
    }

    /**
     * Load initial data and start study session
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Get user preferences
                val studyDirection = preferencesManager.getStudyDirection().first()
                val dailyGoal = preferencesManager.getDailyGoal().first()
                val ttsEnabled = preferencesManager.isSoundEnabled().first()

                DebugHelper.log("User preferences - Direction: $studyDirection, Goal: $dailyGoal, TTS: $ttsEnabled")

                _uiState.update {
                    it.copy(
                        studyDirection = StudyDirection.valueOf(studyDirection),
                        dailyGoal = dailyGoal,
                        isTtsEnabled = ttsEnabled
                    )
                }

                // Load today's progress - Simplified
                loadDailyProgress()

                // Load study queue
                loadStudyQueue()

            } catch (e: Exception) {
                DebugHelper.logError("StudyViewModel initialization error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Çalışma verileri yüklenirken hata oluştu: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Load study queue based on SM-2 algorithm
     */
    private fun loadStudyQueue() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val direction = _uiState.value.studyDirection
                DebugHelper.log("Loading study queue for direction: $direction")

                // Check if there are words to study
                when (val hasWordsResult = studyRepository.hasWordsToStudy(direction)) {
                    is Result.Success -> {
                        if (!hasWordsResult.data) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isQueueEmpty = true,
                                    showEmptyQueueMessage = true,
                                    error = null
                                )
                            }
                            return@launch
                        }
                    }
                    is Result.Error -> {
                        throw Exception("Failed to check words availability: ${hasWordsResult.error.message}")
                    }
                }

                // Collect study queue from Flow
                studyRepository.getStudyQueue(direction, limit = 20).collectLatest { queueData ->
                    DebugHelper.log("Study queue received: ${queueData.size} words")

                    if (queueData.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isQueueEmpty = true,
                                showEmptyQueueMessage = true,
                                error = null
                            )
                        }
                        return@collectLatest
                    }

                    // Convert ConceptWithTimingData to ConceptEntity
                    val concepts = mutableListOf<ConceptEntity>()
                    for (timingData in queueData) {
                        when (val result = studyRepository.getConceptById(timingData.id)) {
                            is Result.Success -> {
                                result.data?.let { concepts.add(it) }
                            }
                            is Result.Error -> {
                                DebugHelper.logError("Failed to get concept ${timingData.id}", result.error)
                            }
                        }
                    }

                    studyQueue = concepts
                    currentQueueIndex = 0

                    DebugHelper.log("Final study queue: ${studyQueue.size} words")

                    if (studyQueue.isNotEmpty()) {
                        startNewSession()
                        loadNextWord()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isQueueEmpty = true,
                                showEmptyQueueMessage = true,
                                error = null
                            )
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalWordsInQueue = studyQueue.size,
                            hasWordsToStudy = studyQueue.isNotEmpty()
                        )
                    }
                }

            } catch (e: Exception) {
                DebugHelper.logError("Study queue loading error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Çalışma kuyruğu yüklenirken hata: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Start new study session
     */
    private suspend fun startNewSession() {
        when (val result = studyRepository.startStudySession(SessionType.MIXED)) {
            is Result.Success -> {
                currentSessionId = result.data
                sessionStartTime = System.currentTimeMillis()
                DebugHelper.log("Started new study session: $currentSessionId")
            }
            is Result.Error -> {
                DebugHelper.logError("Session start error", result.error)
            }
        }
    }

    /**
     * Load next word from study queue
     */
    private fun loadNextWord() {
        if (currentQueueIndex >= studyQueue.size) {
            // Queue completed
            completeSession()
            return
        }

        val currentConcept = studyQueue[currentQueueIndex]
        DebugHelper.log("Loading word ${currentQueueIndex + 1}/${studyQueue.size}: ${currentConcept.english}")

        viewModelScope.launch {
            try {
                val direction = _uiState.value.studyDirection

                // Get current progress for button text calculation
                val progressResult = studyRepository.getCurrentProgress(currentConcept.id, direction)
                val currentProgress = when (progressResult) {
                    is Result.Success -> progressResult.data
                    is Result.Error -> null
                } ?: createDefaultProgress(currentConcept.id, direction)

                // Calculate SM-2 timing texts
                val easyProgress = SpacedRepetitionAlgorithm.calculateNextReview(currentProgress, SpacedRepetitionAlgorithm.QUALITY_EASY)
                val mediumProgress = SpacedRepetitionAlgorithm.calculateNextReview(currentProgress, SpacedRepetitionAlgorithm.QUALITY_MEDIUM)
                val hardProgress = SpacedRepetitionAlgorithm.calculateNextReview(currentProgress, SpacedRepetitionAlgorithm.QUALITY_HARD)

                val easyTimeText = formatNextReviewTime(easyProgress.nextReviewAt)
                val mediumTimeText = formatNextReviewTime(mediumProgress.nextReviewAt)
                val hardTimeText = formatNextReviewTime(hardProgress.nextReviewAt)

                _uiState.update {
                    it.copy(
                        currentConcept = currentConcept,
                        currentWordIndex = currentQueueIndex,
                        isCardFlipped = false,
                        easyTimeText = easyTimeText,
                        mediumTimeText = mediumTimeText,
                        hardTimeText = hardTimeText,
                        isLoading = false,
                        error = null
                    )
                }

            } catch (e: Exception) {
                DebugHelper.logError("Load next word error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Kelime yüklenirken hata: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Handle user response to current word
     */
    private fun handleUserResponse(quality: Int) {
        viewModelScope.launch {
            try {
                val concept = _uiState.value.currentConcept ?: return@launch
                val direction = _uiState.value.studyDirection

                DebugHelper.log("User response: quality=$quality for concept=${concept.english}")

                // Update progress in database (this will increment daily progress if word is completed)
                when (val result = studyRepository.updateWordProgress(concept.id, direction, quality)) {
                    is Result.Success -> {
                        // Update session statistics
                        val currentState = _uiState.value
                        val isCorrect = quality >= SpacedRepetitionAlgorithm.QUALITY_MEDIUM

                        _uiState.update {
                            it.copy(
                                sessionWordsCount = currentState.sessionWordsCount + 1,
                                correctAnswers = if (isCorrect) currentState.correctAnswers + 1 else currentState.correctAnswers
                            )
                        }

                        // Reload daily progress
                        loadDailyProgress()

                        // Provide haptic feedback
                        val hapticType = when (quality) {
                            SpacedRepetitionAlgorithm.QUALITY_EASY -> HapticType.SUCCESS
                            SpacedRepetitionAlgorithm.QUALITY_MEDIUM -> HapticType.MEDIUM
                            else -> HapticType.ERROR
                        }
                        _effect.emit(StudyEffect.HapticFeedback(hapticType))

                        // Move to next word after a short delay
                        kotlinx.coroutines.delay(500)
                        nextWord()

                        DebugHelper.log("Progress updated successfully, moving to next word")
                    }
                    is Result.Error -> {
                        DebugHelper.logError("Update progress error", result.error)
                        _effect.emit(StudyEffect.ShowMessage("İlerleme kaydedilemedi"))
                    }
                }

            } catch (e: Exception) {
                DebugHelper.logError("Handle user response error", e)
                _effect.emit(StudyEffect.ShowMessage("Yanıt işlenirken hata oluştu"))
            }
        }
    }

    /**
     * Move to next word in queue
     */
    private fun nextWord() {
        currentQueueIndex++
        loadNextWord()
    }

    /**
     * Flip card animation
     */
    private fun flipCard() {
        _uiState.update { it.copy(isCardFlipped = !it.isCardFlipped) }

        viewModelScope.launch {
            _effect.emit(StudyEffect.HapticFeedback(HapticType.LIGHT))
        }
    }

    /**
     * Reset card to front side
     */
    private fun resetCard() {
        _uiState.update { it.copy(isCardFlipped = false) }
    }

    /**
     * Play pronunciation using TTS
     */
    private fun playPronunciation() {
        viewModelScope.launch {
            try {
                val concept = _uiState.value.currentConcept ?: return@launch
                val direction = _uiState.value.studyDirection

                val textToSpeak = when (direction) {
                    StudyDirection.EN_TO_TR -> concept.english
                    StudyDirection.TR_TO_EN -> concept.turkish
                }

                val language = when (direction) {
                    StudyDirection.EN_TO_TR -> "en"
                    StudyDirection.TR_TO_EN -> "tr"
                }

                _effect.emit(StudyEffect.SpeakText(textToSpeak, language))
                DebugHelper.log("TTS request: '$textToSpeak' in $language")

            } catch (e: Exception) {
                DebugHelper.logError("Play pronunciation error", e)
                _effect.emit(StudyEffect.ShowMessage("Sesli okuma başlatılamadı"))
            }
        }
    }

    /**
     * Stop TTS
     */
    private fun stopTts() {
        viewModelScope.launch {
            textToSpeechManager.stop()
            _uiState.update { it.copy(isSpeaking = false) }
        }
    }

    /**
     * Complete current session
     */
    private fun completeSession() {
        viewModelScope.launch {
            try {
                currentSessionId?.let { sessionId ->
                    val currentState = _uiState.value
                    studyRepository.endStudySession(
                        sessionId = sessionId,
                        wordsStudied = currentState.sessionWordsCount,
                        correctAnswers = currentState.correctAnswers
                    )
                }

                val stats = SessionStats(
                    wordsStudied = _uiState.value.sessionWordsCount,
                    correctAnswers = _uiState.value.correctAnswers,
                    timeSpentMs = System.currentTimeMillis() - sessionStartTime,
                    newWordsLearned = 0, // Could be calculated if needed
                    wordsReviewed = _uiState.value.sessionWordsCount
                )

                _effect.emit(StudyEffect.ShowSessionComplete(stats))
                DebugHelper.log("Session completed: $stats")

            } catch (e: Exception) {
                DebugHelper.logError("Complete session error", e)
            }
        }
    }

    /**
     * End current session manually
     */
    private fun endCurrentSession() {
        completeSession()
    }

    /**
     * Retry loading after error
     */
    private fun retryLoading() {
        loadInitialData()
    }

    /**
     * Navigate to word selection
     */
    private fun navigateToWordSelection() {
        viewModelScope.launch {
            _effect.emit(StudyEffect.NavigateToWordSelection)
        }
    }

    /**
     * Navigate back to home
     */
    private fun navigateBack() {
        viewModelScope.launch {
            _effect.emit(StudyEffect.NavigateToHome)
        }
    }

    /**
     * Load daily progress - Simplified version
     */
    private fun loadDailyProgress() {
        viewModelScope.launch {
            try {
                // Get today's statistics
                when (val result = studyRepository.getTodaySessionStats()) {
                    is Result.Success -> {
                        val stats = result.data
                        _uiState.update {
                            it.copy(
                                wordsStudiedToday = stats.cardsCompleted,
                                dailyGoal = stats.dailyGoal,
                                dailyProgressPercentage = stats.progressPercentage
                            )
                        }
                        DebugHelper.log("Daily progress loaded: ${stats.cardsCompleted}/${stats.dailyGoal}")
                    }
                    is Result.Error -> {
                        DebugHelper.logError("Load daily progress error", result.error)
                    }
                }

            } catch (e: Exception) {
                DebugHelper.logError("Load daily progress exception", e)
            }
        }
    }

    /**
     * Helper functions
     */
    private fun createDefaultProgress(conceptId: Int, direction: StudyDirection) =
        com.hocalingo.app.core.database.entities.WordProgressEntity(
            conceptId = conceptId,
            direction = direction,
            repetitions = 0,
            intervalDays = 0f,
            easeFactor = 2.5f,
            nextReviewAt = System.currentTimeMillis(),
            lastReviewAt = null,
            isSelected = true,
            isMastered = false
        )

    private fun formatNextReviewTime(nextReviewAt: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = nextReviewAt - now

        return when {
            diffMs < 60 * 60 * 1000 -> "1 saat içinde"
            diffMs < 24 * 60 * 60 * 1000 -> "${(diffMs / (60 * 60 * 1000)).toInt()} saat sonra"
            diffMs < 7 * 24 * 60 * 60 * 1000 -> "${(diffMs / (24 * 60 * 60 * 1000)).toInt()} gün sonra"
            else -> "${(diffMs / (7 * 24 * 60 * 60 * 1000)).toInt()} hafta sonra"
        }
    }
}