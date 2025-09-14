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
                val direction = _uiState.value.studyDirection
                DebugHelper.log("Loading study queue for direction: $direction")

                // Get study word counts
                when (val countsResult = studyRepository.getStudyWordCounts(direction)) {
                    is Result.Success -> {
                        val counts = countsResult.data
                        DebugHelper.log("Study word counts: $counts")

                        if (!counts.hasWordsToStudy) {
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
                        throw Exception("Failed to get word counts: ${countsResult.error.message}")
                    }
                }

                // Get overdue words first (high priority)
                val overdueWordsResult = studyRepository.getOverdueWords(direction)
                val overdueWords = when (overdueWordsResult) {
                    is Result.Success -> overdueWordsResult.data.take(15) // Max 15 overdue
                    is Result.Error -> emptyList()
                }

                // Get new words (medium priority)
                val newWordsResult = studyRepository.getNewWords(direction, 5)
                val newWords = when (newWordsResult) {
                    is Result.Success -> newWordsResult.data
                    is Result.Error -> emptyList()
                }

                // Combine into study queue
                val overdueConceptIds = overdueWords.map { it.id }.toSet()
                studyQueue = overdueWords.mapNotNull { timingData ->
                    // Convert ConceptWithTimingData to ConceptEntity
                    studyRepository.getConceptWithProgress(timingData.id, direction).let { result ->
                        when (result) {
                            is Result.Success -> result.data
                            is Result.Error -> null
                        }
                    }
                } + newWords.filter { it.id !in overdueConceptIds }

                DebugHelper.log("Final study queue: ${studyQueue.size} words (${overdueWords.size} overdue + ${newWords.size} new)")

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
                    DebugHelper.log("Study queue is empty - showing empty state")
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

        // Calculate button texts with timing using repository
        viewModelScope.launch {
            try {
                val direction = _uiState.value.studyDirection

                // Get current progress using repository
                when (val progressResult = studyRepository.getCurrentProgress(currentConcept.id, direction)) {
                    is Result.Success -> {
                        val currentProgress = progressResult.data ?:
                        SpacedRepetitionAlgorithm.createInitialProgress(currentConcept.id, direction)

                        // Calculate what the next intervals would be for each button
                        val easyResult = SpacedRepetitionAlgorithm.calculateNextReview(
                            currentProgress, SpacedRepetitionAlgorithm.QUALITY_EASY
                        )
                        val mediumResult = SpacedRepetitionAlgorithm.calculateNextReview(
                            currentProgress, SpacedRepetitionAlgorithm.QUALITY_MEDIUM
                        )
                        val hardResult = SpacedRepetitionAlgorithm.calculateNextReview(
                            currentProgress, SpacedRepetitionAlgorithm.QUALITY_HARD
                        )

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentConcept = currentConcept,
                                currentWordIndex = currentQueueIndex,
                                totalWordsInQueue = studyQueue.size,
                                remainingWords = studyQueue.size - currentQueueIndex,
                                isCardFlipped = false,
                                easyTimeText = SpacedRepetitionAlgorithm.getTimeUntilReview(easyResult.nextReviewAt),
                                mediumTimeText = SpacedRepetitionAlgorithm.getTimeUntilReview(mediumResult.nextReviewAt),
                                hardTimeText = SpacedRepetitionAlgorithm.getTimeUntilReview(hardResult.nextReviewAt),
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        DebugHelper.logError("Failed to get current progress", progressResult.error)
                        // Fallback: still show the word but with default timing
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentConcept = currentConcept,
                                currentWordIndex = currentQueueIndex,
                                totalWordsInQueue = studyQueue.size,
                                remainingWords = studyQueue.size - currentQueueIndex,
                                isCardFlipped = false,
                                easyTimeText = "15 dakika sonra",
                                mediumTimeText = "30 dakika sonra",
                                hardTimeText = "5 dakika sonra",
                                error = null
                            )
                        }
                    }
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
     * Handle user response (Easy/Medium/Hard)
     */
    private fun handleUserResponse(quality: Int) {
        val currentState = _uiState.value
        val currentConcept = currentState.currentConcept ?: return

        DebugHelper.log("User response: quality=$quality for word=${currentConcept.english}")

        viewModelScope.launch {
            try {
                val direction = currentState.studyDirection

                // Update progress using repository
                when (val result = studyRepository.updateWordProgress(currentConcept.id, direction, quality)) {
                    is Result.Success -> {
                        DebugHelper.log("Progress updated successfully - next review: ${SpacedRepetitionAlgorithm.getTimeUntilReview(result.data.nextReviewAt)}")

                        // Update session statistics
                        val isCorrect = quality >= SpacedRepetitionAlgorithm.QUALITY_MEDIUM
                        _uiState.update {
                            it.copy(
                                sessionWordsCount = it.sessionWordsCount + 1,
                                correctAnswers = if (isCorrect) it.correctAnswers + 1 else it.correctAnswers
                            )
                        }

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
        _effect.emit(StudyEffect.PlaySound(StudySoundType.CARD_FLIP))
        _effect.emit(StudyEffect.HapticFeedback(HapticType.LIGHT))

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
        val direction = _uiState.value.studyDirection

        // For English pronunciation, use the English text
        val textToSpeak = when (direction) {
            StudyDirection.EN_TO_TR -> currentConcept.english
            StudyDirection.TR_TO_EN -> currentConcept.english // Still pronounce English
        }

        _uiState.update { it.copy(isTtsSpeaking = true) }

        // Use TextToSpeechManager directly
        textToSpeechManager.speakEnglishWord(textToSpeak)

        DebugHelper.log("Playing pronunciation: $textToSpeak")
    }

    /**
     * Stop TTS speaking
     */
    private fun stopTts() {
        textToSpeechManager.stop()
        _uiState.update { it.copy(isTtsSpeaking = false) }
    }

    /**
     * Load today's study statistics
     */
    private fun loadTodayStats() {
        viewModelScope.launch {
            when (val result = studyRepository.getTodayStats()) {
                is Result.Success -> {
                    val stats = result.data
                    DebugHelper.log("Today's stats: $stats")

                    _uiState.update {
                        it.copy(wordsStudiedToday = stats.wordsStudied)
                    }
                }
                is Result.Error -> {
                    DebugHelper.logError("Today stats loading error", result.error)
                }
            }
        }
    }

    /**
     * Complete current study session
     */
    private fun completeSession() {
        DebugHelper.log("Study session completed")

        val currentState = _uiState.value
        val sessionDuration = System.currentTimeMillis() - sessionStartTime

        val stats = SessionStats(
            wordsStudied = currentState.sessionWordsCount,
            correctAnswers = currentState.correctAnswers,
            accuracy = currentState.accuracyPercentage,
            timeSpentMs = sessionDuration,
            newWordsLearned = currentState.sessionWordsCount,
            wordsReviewed = currentState.sessionWordsCount
        )

        _uiState.update {
            it.copy(
                showCompletionDialog = true,
                isQueueEmpty = true
            )
        }

        _effect.emit(StudyEffect.ShowSessionComplete(stats))
        _effect.emit(StudyEffect.PlaySound(StudySoundType.SESSION_COMPLETE))

        viewModelScope.launch {
            endCurrentSession()
        }
    }

    /**
     * End current study session in database
     */
    private fun endCurrentSession() {
        currentSessionId?.let { sessionId ->
            viewModelScope.launch {
                val currentState = _uiState.value

                when (val result = studyRepository.endStudySession(
                    sessionId = sessionId,
                    wordsStudied = currentState.sessionWordsCount,
                    correctAnswers = currentState.correctAnswers
                )) {
                    is Result.Success -> {
                        DebugHelper.log("Session ended successfully: $sessionId")
                    }
                    is Result.Error -> {
                        DebugHelper.logError("Session end error", result.error)
                    }
                }

                currentSessionId = null
            }
        }
    }

    /**
     * Change study direction
     */
    private fun changeStudyDirection(direction: StudyDirection) {
        DebugHelper.log("Changing study direction to: $direction")

        viewModelScope.launch {
            preferencesManager.setStudyDirection(direction.name)
            loadStudyQueue() // Reload queue with new direction
        }
    }

    /**
     * Navigate to word selection screen
     */
    private fun navigateToWordSelection() {
        _effect.emit(StudyEffect.NavigateToWordSelection)
    }

    /**
     * Navigate back to home
     */
    private fun navigateBack() {
        viewModelScope.launch {
            endCurrentSession()
            _effect.emit(StudyEffect.NavigateToHome)
        }
    }

    /**
     * Retry loading after error
     */
    private fun retryLoading() {
        loadInitialData()
    }
}