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
 * StudyViewModel - FIXED VERSION
 *
 * FIXES APPLIED:
 * ✅ Fixed formatNextReviewTime() to show correct intervals (5 min, 30 min, etc.)
 * ✅ Now uses SpacedRepetitionAlgorithm.getTimeUntilReview() for consistency
 * ✅ Proper debug logging for button timing texts
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
    private var sessionStartTime: Long = 0
    private var studyQueue: List<ConceptEntity> = emptyList()
    private var currentQueueIndex: Int = 0

    init {
        loadInitialData()
    }

    /**
     * HYBRID: Reload study queue for learning cards
     * Called when current queue is completed but learning cards remain
     */
    private fun reloadStudyQueue() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val direction = _uiState.value.studyDirection
                DebugHelper.log("🔄 Reloading study queue for direction: $direction")

                // Collect study queue from Flow
                studyRepository.getStudyQueue(direction, limit = 20).collectLatest { queueData ->
                    DebugHelper.log("🔄 Reloaded queue received: ${queueData.size} words")

                    if (queueData.isEmpty()) {
                        DebugHelper.log("🔄 Reloaded queue is empty, completing session")
                        completeSession()
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

                    // Update queue and reset index
                    studyQueue = concepts
                    currentQueueIndex = 0

                    DebugHelper.log("🔄 Queue reloaded: ${studyQueue.size} words, starting from index 0")

                    if (studyQueue.isNotEmpty()) {
                        loadNextWord() // Continue with first word in new queue
                    } else {
                        completeSession()
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            totalWordsInQueue = studyQueue.size
                        )
                    }
                }

            } catch (e: Exception) {
                DebugHelper.logError("Reload study queue error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Queue yeniden yüklenirken hata: ${e.message}"
                    )
                }
                completeSession() // Fallback
            }
        }
    }

    /**
     * Handle UI events
     */
    fun onEvent(event: StudyEvent) {
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
            StudyEvent.RetryLoading -> loadStudyQueue()
            StudyEvent.NavigateToWordSelection -> navigateToWordSelection()
            StudyEvent.NavigateBack -> navigateBack()
        }
    }

    /**
     * Load initial data on ViewModel creation
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Get user preferences
                val studyDirection = preferencesManager.getStudyDirection().first()
                val dailyGoal = preferencesManager.getDailyGoal().first()
                // FIXED: Use isSoundEnabled() instead of getSoundEnabled() + explicit Flow<Boolean> type
                val ttsEnabled: Boolean = preferencesManager.isSoundEnabled().first()

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
     * Load next word from study queue - HYBRID VERSION
     *
     * MAJOR FIX: In hybrid system, session should reload queue when learning cards exist
     * Session only completes when NO learning cards remain
     */
    private fun loadNextWord() {
        if (currentQueueIndex >= studyQueue.size) {
            // Current queue completed - check if we should reload for learning cards
            DebugHelper.log("🔄 Current queue completed ($currentQueueIndex/${studyQueue.size}), checking for learning cards...")

            viewModelScope.launch {
                try {
                    val direction = _uiState.value.studyDirection

                    // Check if there are learning cards that need to continue in session
                    when (val result = studyRepository.getLearningCardsCount(direction)) {
                        is Result.Success -> {
                            val learningCount = result.data
                            DebugHelper.log("📚 Learning cards remaining: $learningCount")

                            if (learningCount > 0) {
                                // HYBRID: There are learning cards, reload the queue
                                DebugHelper.log("🔄 Reloading queue for learning cards...")
                                reloadStudyQueue()
                            } else {
                                // No learning cards left, session can complete
                                DebugHelper.log("🏁 No learning cards remaining, completing session")
                                completeSession()
                            }
                        }
                        is Result.Error -> {
                            DebugHelper.logError("Error checking learning cards", result.error)
                            completeSession() // Fallback to complete session
                        }
                    }
                } catch (e: Exception) {
                    DebugHelper.logError("Error checking learning cards", e)
                    completeSession() // Fallback to complete session
                }
            }
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

                // Calculate SM-2 timing texts using HYBRID method
                // Note: We pass a dummy session position since this is just for preview
                val easyProgress = SpacedRepetitionAlgorithm.calculateNextReview(currentProgress, SpacedRepetitionAlgorithm.QUALITY_EASY, 100)
                val mediumProgress = SpacedRepetitionAlgorithm.calculateNextReview(currentProgress, SpacedRepetitionAlgorithm.QUALITY_MEDIUM, 100)
                val hardProgress = SpacedRepetitionAlgorithm.calculateNextReview(currentProgress, SpacedRepetitionAlgorithm.QUALITY_HARD, 100)

                // 🔥 FIX: Use SpacedRepetitionAlgorithm.getTimeUntilReview() instead of broken formatNextReviewTime()
                val easyTimeText = SpacedRepetitionAlgorithm.getTimeUntilReview(easyProgress.nextReviewAt)
                val mediumTimeText = SpacedRepetitionAlgorithm.getTimeUntilReview(mediumProgress.nextReviewAt)
                val hardTimeText = SpacedRepetitionAlgorithm.getTimeUntilReview(hardProgress.nextReviewAt)

                // 🐛 DEBUG: Log button timing texts for verification
                DebugHelper.log("Button timing texts - Easy: '$easyTimeText', Medium: '$mediumTimeText', Hard: '$hardTimeText'")
                DebugHelper.log("Progress intervals - Easy: ${easyProgress.intervalDays}, Medium: ${mediumProgress.intervalDays}, Hard: ${hardProgress.intervalDays}")

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
     * FIXED: Added proper debug logging for quality values
     */
    private fun handleUserResponse(quality: Int) {
        viewModelScope.launch {
            try {
                val concept = _uiState.value.currentConcept ?: return@launch
                val direction = _uiState.value.studyDirection

                DebugHelper.log("🔥 USER RESPONSE: quality=$quality for concept='${concept.english}' (1=Hard, 2=Medium, 3=Easy)")

                // Update progress in database (this will increment daily progress if word is completed)
                when (val result = studyRepository.updateWordProgress(concept.id, direction, quality)) {
                    is Result.Success -> {
                        val updatedProgress = result.data
                        DebugHelper.log("✅ Updated progress: repetitions=${updatedProgress.repetitions}, nextReviewAt=${updatedProgress.nextReviewAt}, intervalDays=${updatedProgress.intervalDays}")

                        // Update session statistics
                        val currentState = _uiState.value
                        val isCorrect = quality >= SpacedRepetitionAlgorithm.QUALITY_MEDIUM

                        _uiState.update {
                            it.copy(
                                sessionWordsCount = currentState.sessionWordsCount + 1,
                                correctAnswers = if (isCorrect) currentState.correctAnswers + 1 else currentState.correctAnswers
                            )
                        }

                        // Move to next word
                        currentQueueIndex++
                        loadNextWord()

                        // Reload daily progress
                        loadDailyProgress()

                        // Show feedback effect
                        val feedbackMessage = when (quality) {
                            SpacedRepetitionAlgorithm.QUALITY_EASY -> "Harika! 🎉"
                            SpacedRepetitionAlgorithm.QUALITY_MEDIUM -> "İyi! 👍"
                            SpacedRepetitionAlgorithm.QUALITY_HARD -> "Tekrar göreceğiz 📚"
                            else -> "Devam!"
                        }
                        _effect.tryEmit(StudyEffect.ShowMessage(feedbackMessage))

                    }
                    is Result.Error -> {
                        DebugHelper.logError("❌ Update progress error", result.error)
                        _effect.tryEmit(StudyEffect.ShowMessage("Kelime kaydedilirken hata oluştu"))
                    }
                }

            } catch (e: Exception) {
                DebugHelper.logError("Handle user response error", e)
                _effect.tryEmit(StudyEffect.ShowMessage("Bir hata oluştu: ${e.message}"))
            }
        }
    }

    /**
     * Load daily progress statistics
     */
    private fun loadDailyProgress() {
        viewModelScope.launch {
            try {
                when (val result = studyRepository.getTodaySessionStats()) {
                    is Result.Success -> {
                        val stats = result.data
                        _uiState.update {
                            it.copy(
                                wordsStudiedToday = stats.wordsStudied,
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

    // ========== CARD ACTIONS ==========

    private fun flipCard() {
        _uiState.update { it.copy(isCardFlipped = !it.isCardFlipped) }
    }

    private fun resetCard() {
        _uiState.update { it.copy(isCardFlipped = false) }
    }

    // ========== TTS ACTIONS ==========

    private fun playPronunciation() {
        val concept = _uiState.value.currentConcept
        if (concept != null && _uiState.value.isTtsEnabled) {
            val textToSpeak = when (_uiState.value.studyDirection) {
                StudyDirection.EN_TO_TR -> concept.english
                StudyDirection.TR_TO_EN -> concept.turkish
            }
            _effect.tryEmit(StudyEffect.SpeakText(textToSpeak))
        }
    }

    private fun stopTts() {
        textToSpeechManager.stop()
    }

    // ========== SESSION MANAGEMENT ==========

    private fun completeSession() {
        viewModelScope.launch {
            currentSessionId?.let { sessionId ->
                val wordsStudied = _uiState.value.sessionWordsCount
                val correctAnswers = _uiState.value.correctAnswers

                studyRepository.endStudySession(sessionId, wordsStudied, correctAnswers)
                DebugHelper.log("Session completed: $wordsStudied words studied, $correctAnswers correct")

                val sessionStats = SessionStats(
                    wordsStudied = wordsStudied,
                    correctAnswers = correctAnswers,
                    timeSpentMs = System.currentTimeMillis() - sessionStartTime,
                    newWordsLearned = wordsStudied, // Simplified
                    wordsReviewed = 0 // Simplified
                )

                _effect.tryEmit(StudyEffect.ShowSessionComplete(sessionStats))
            }
        }
    }

    private fun endCurrentSession() {
        completeSession()
        _effect.tryEmit(StudyEffect.NavigateToHome)
    }

    // ========== NAVIGATION ==========

    private fun navigateToWordSelection() {
        _effect.tryEmit(StudyEffect.NavigateToWordSelection)
    }

    private fun navigateBack() {
        _effect.tryEmit(StudyEffect.NavigateToHome)
    }

    // ========== HELPER FUNCTIONS ==========

    /**
     * Create default progress for new word - HYBRID VERSION
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
            isMastered = false,
            learningPhase = true, // Start in learning phase
            sessionPosition = 1   // Default session position
        )

    /**
     * REMOVED: formatNextReviewTime() - Now using SpacedRepetitionAlgorithm.getTimeUntilReview()
     *
     * OLD BROKEN CODE WAS:
     * private fun formatNextReviewTime(nextReviewAt: Long): String {
     *     val now = System.currentTimeMillis()
     *     val diffMs = nextReviewAt - now
     *     return when {
     *         diffMs < 60 * 60 * 1000 -> "1 saat içinde"  // ❌ ALWAYS SHOWED THIS!
     *         ...
     *     }
     * }
     */
}