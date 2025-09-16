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
 * StudyViewModel - Main Study Screen Logic
 *
 * Manages:
 * - Study queue and current word
 * - SM-2 spaced repetition algorithm
 * - Card flip animations
 * - Progress tracking
 * - TTS integration
 * - Session statistics
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
     * Handle user events
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

            is StudyEvent.ToggleStudyDirection -> changeStudyDirection(event.direction)
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

                // Load today's progress
                loadTodayStats()

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
                } ?: SpacedRepetitionAlgorithm.createInitialProgress(currentConcept.id, direction)

                // Calculate button texts with timing
                val easyTiming = SpacedRepetitionAlgorithm.calculateNextReview(
                    currentProgress, SpacedRepetitionAlgorithm.QUALITY_EASY
                )
                val mediumTiming = SpacedRepetitionAlgorithm.calculateNextReview(
                    currentProgress, SpacedRepetitionAlgorithm.QUALITY_MEDIUM
                )
                val hardTiming = SpacedRepetitionAlgorithm.calculateNextReview(
                    currentProgress, SpacedRepetitionAlgorithm.QUALITY_HARD
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentConcept = currentConcept,
                        isCardFlipped = false,
                        currentWordIndex = currentQueueIndex,
                        totalWordsInQueue = studyQueue.size,
                        isQueueEmpty = false,
                        showEmptyQueueMessage = false,
                        error = null,
                        // Update button texts with timing
                        easyButtonText = "Kolay",
                        easyTimeText = SpacedRepetitionAlgorithm.getTimeUntilReview(easyTiming.nextReviewAt),
                        mediumButtonText = "Orta",
                        mediumTimeText = SpacedRepetitionAlgorithm.getTimeUntilReview(mediumTiming.nextReviewAt),
                        hardButtonText = "Zor",
                        hardTimeText = SpacedRepetitionAlgorithm.getTimeUntilReview(hardTiming.nextReviewAt)
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
                val currentConcept = _uiState.value.currentConcept ?: return@launch
                val direction = _uiState.value.studyDirection

                DebugHelper.log("Handling response for word ${currentConcept.english}: quality=$quality")

                // Update progress in database
                when (val result = studyRepository.updateWordProgress(currentConcept.id, direction, quality)) {
                    is Result.Success -> {
                        // Update session statistics
                        val sessionWords = _uiState.value.sessionWordsCount + 1
                        val correctAnswers = _uiState.value.correctAnswers + if (quality >= SpacedRepetitionAlgorithm.QUALITY_MEDIUM) 1 else 0

                        _uiState.update {
                            it.copy(
                                sessionWordsCount = sessionWords,
                                correctAnswers = correctAnswers
                            )
                        }

                        DebugHelper.log("Response recorded successfully. Session: $correctAnswers/$sessionWords")

                        // Provide haptic and audio feedback
                        when (quality) {
                            SpacedRepetitionAlgorithm.QUALITY_EASY -> {
                                _effect.emit(StudyEffect.HapticFeedback(HapticType.SUCCESS))
                                _effect.emit(StudyEffect.PlaySound(StudySoundType.CORRECT))
                            }
                            SpacedRepetitionAlgorithm.QUALITY_MEDIUM -> {
                                _effect.emit(StudyEffect.HapticFeedback(HapticType.LIGHT))
                            }
                            SpacedRepetitionAlgorithm.QUALITY_HARD -> {
                                _effect.emit(StudyEffect.HapticFeedback(HapticType.ERROR))
                                _effect.emit(StudyEffect.PlaySound(StudySoundType.INCORRECT))
                            }
                        }

                        // Move to next word
                        currentQueueIndex++
                        loadNextWord()
                    }
                    is Result.Error -> {
                        DebugHelper.logError("Response handling error", result.error)
                        _effect.emit(StudyEffect.ShowMessage("Cevap kaydedilirken hata oluştu"))
                    }
                }

            } catch (e: Exception) {
                DebugHelper.logError("Response handling exception", e)
                _effect.emit(StudyEffect.ShowMessage("Cevap kaydedilirken hata oluştu"))
            }
        }
    }

    /**
     * Flip the study card
     */
    private fun flipCard() {
        _uiState.update { it.copy(isCardFlipped = !it.isCardFlipped) }

        viewModelScope.launch {
            _effect.emit(StudyEffect.PlaySound(StudySoundType.CARD_FLIP))
            _effect.emit(StudyEffect.HapticFeedback(HapticType.LIGHT))
        }

        DebugHelper.log("Card flipped: ${_uiState.value.isCardFlipped}")
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
        val currentConcept = _uiState.value.currentConcept ?: return
        val isTtsEnabled = _uiState.value.isTtsEnabled

        if (!isTtsEnabled) {
            DebugHelper.log("TTS is disabled")
            return
        }

        val direction = _uiState.value.studyDirection
        val textToSpeak = when (direction) {
            StudyDirection.EN_TO_TR -> currentConcept.english
            StudyDirection.TR_TO_EN -> currentConcept.turkish
        }

        viewModelScope.launch {
            _effect.emit(StudyEffect.SpeakText(textToSpeak, if (direction == StudyDirection.EN_TO_TR) "en" else "tr"))
            _effect.emit(StudyEffect.HapticFeedback(HapticType.LIGHT))
        }

        DebugHelper.log("Playing pronunciation: $textToSpeak")
    }

    /**
     * Stop TTS
     */
    private fun stopTts() {
        textToSpeechManager.stop()
        _uiState.update { it.copy(isTtsSpeaking = false) }
    }

    /**
     * Complete current study session
     */
    private fun completeSession() {
        viewModelScope.launch {
            try {
                val sessionWords = _uiState.value.sessionWordsCount
                val correctAnswers = _uiState.value.correctAnswers

                currentSessionId?.let { sessionId ->
                    studyRepository.endStudySession(
                        sessionId = sessionId,
                        wordsStudied = sessionWords,
                        correctAnswers = correctAnswers
                    )

                    DebugHelper.log("Session completed: $correctAnswers/$sessionWords correct")
                }

                // Update UI to show completion
                val sessionStats = SessionStats(
                    wordsStudied = sessionWords,
                    correctAnswers = correctAnswers,
                    accuracy = if (sessionWords > 0) {
                        (correctAnswers.toFloat() / sessionWords) * 100
                    } else 0f,
                    timeSpentMs = System.currentTimeMillis() - sessionStartTime,
                    newWordsLearned = sessionWords - correctAnswers, // Simplified
                    wordsReviewed = correctAnswers
                )

                _uiState.update {
                    it.copy(showCompletionDialog = true)
                }

                _effect.emit(StudyEffect.ShowSessionComplete(sessionStats))
                _effect.emit(StudyEffect.HapticFeedback(HapticType.SUCCESS))

                // Reload today's stats
                loadTodayStats()

            } catch (e: Exception) {
                DebugHelper.logError("Complete session error", e)
                _effect.emit(StudyEffect.ShowMessage("Oturum tamamlanırken hata oluştu"))
            }
        }
    }

    /**
     * End current session early
     */
    private fun endCurrentSession() {
        completeSession()
    }

    /**
     * Retry loading after error
     */
    private fun retryLoading() {
        loadStudyQueue()
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
     * Change study direction
     */
    private fun changeStudyDirection(direction: StudyDirection) {
        viewModelScope.launch {
            try {
                // Save to preferences
                preferencesManager.setStudyDirection(direction.name)

                _uiState.update { it.copy(studyDirection = direction) }

                // Reload study queue with new direction
                loadStudyQueue()

                DebugHelper.log("Study direction changed to: $direction")

            } catch (e: Exception) {
                DebugHelper.logError("Change study direction error", e)
                _effect.emit(StudyEffect.ShowMessage("Yön değiştirilemedi"))
            }
        }
    }

    /**
     * Load today's statistics
     */
    private fun loadTodayStats() {
        viewModelScope.launch {
            try {
                when (val result = studyRepository.getTodayWordsStudied()) {
                    is Result.Success -> {
                        val wordsStudied = result.data
                        _uiState.update {
                            it.copy(wordsStudiedToday = wordsStudied)
                        }
                        DebugHelper.log("Today's words studied: $wordsStudied")
                    }
                    is Result.Error -> {
                        DebugHelper.logError("Load today stats error", result.error)
                    }
                }

            } catch (e: Exception) {
                DebugHelper.logError("Load today stats exception", e)
            }
        }
    }
}