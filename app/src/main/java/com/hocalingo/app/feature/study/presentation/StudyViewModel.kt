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
 * StudyViewModel - COMPLETION FIXED VERSION
 * ✅ Fixed last card staying on screen issue
 * ✅ Immediate completion state display when session ends
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
     * ✅ FIXED: Handle user response and immediate completion state
     */
    private fun handleUserResponse(quality: Int) {
        val concept = _uiState.value.currentConcept ?: return
        val direction = _uiState.value.studyDirection

        viewModelScope.launch {
            try {
                // Update word progress
                val result = studyRepository.updateWordProgress(concept.id, direction, quality)

                when (result) {
                    is Result.Success -> {
                        DebugHelper.log("Word progress updated: ${concept.english} with quality $quality")

                        // Update session stats
                        _uiState.update {
                            it.copy(
                                sessionWordsCount = it.sessionWordsCount + 1,
                                correctAnswers = if (quality >= 2) it.correctAnswers + 1 else it.correctAnswers,
                                isCardFlipped = false
                            )
                        }

                        // ✅ CRITICAL FIX: Move to next immediately, check completion afterwards
                        currentQueueIndex++

                        // ✅ FIXED: Check if this was the last card BEFORE trying to load next
                        if (currentQueueIndex >= studyQueue.size) {
                            DebugHelper.log("🏁 Last card completed, checking for more learning cards...")

                            // Check if there are more learning cards
                            val learningCardsResult = studyRepository.getLearningCardsCount(direction)
                            when (learningCardsResult) {
                                is Result.Success -> {
                                    if (learningCardsResult.data > 0) {
                                        DebugHelper.log("🔄 Learning cards remain, reloading queue...")
                                        reloadStudyQueue()
                                    } else {
                                        DebugHelper.log("🎉 No more learning cards, showing completion!")
                                        // ✅ IMMEDIATE: Show completion state right away
                                        _uiState.update {
                                            it.copy(
                                                currentConcept = null,
                                                showEmptyQueueMessage = true,
                                                isCardFlipped = false
                                            )
                                        }
                                        completeSession()
                                    }
                                }
                                is Result.Error -> {
                                    DebugHelper.logError("Error checking learning cards", learningCardsResult.error)
                                    // ✅ FALLBACK: Show completion on error
                                    _uiState.update {
                                        it.copy(
                                            currentConcept = null,
                                            showEmptyQueueMessage = true,
                                            isCardFlipped = false
                                        )
                                    }
                                    completeSession()
                                }
                            }
                        } else {
                            // Load next word in current queue
                            loadNextWord()
                        }
                    }
                    is Result.Error -> {
                        DebugHelper.logError("Word progress update error", result.error)
                        _effect.tryEmit(StudyEffect.ShowMessage("Kelime güncellenirken hata oluştu"))
                    }
                }
            } catch (e: Exception) {
                DebugHelper.logError("Handle user response exception", e)
            }
        }
    }

    /**
     * Load next word from study queue
     */
    private fun loadNextWord() {
        if (currentQueueIndex >= studyQueue.size) {
            DebugHelper.log("🏁 Queue completed")
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

                /// Calculate button timing texts - simplified
                val easyTimeText = "Kolay"
                val mediumTimeText = "Orta"
                val hardTimeText = "Zor"

                _uiState.update {
                    it.copy(
                        currentConcept = currentConcept,
                        currentWordIndex = currentQueueIndex,
                        isCardFlipped = false,
                        easyTimeText = easyTimeText,
                        mediumTimeText = mediumTimeText,
                        hardTimeText = hardTimeText
                    )
                }

                DebugHelper.log("Word loaded: ${currentConcept.english} -> ${currentConcept.turkish}")

            } catch (e: Exception) {
                DebugHelper.logError("Load word exception", e)
            }
        }
    }

    /**
     * HYBRID: Reload study queue for learning cards
     */
    private fun reloadStudyQueue() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val direction = _uiState.value.studyDirection
                DebugHelper.log("🔄 Reloading study queue for direction: $direction")

                studyRepository.getStudyQueue(direction, limit = 20).collectLatest { queueData ->
                    DebugHelper.log("🔄 Reloaded queue received: ${queueData.size} words")

                    if (queueData.isEmpty()) {
                        DebugHelper.log("🔄 Reloaded queue is empty, showing completion")
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                currentConcept = null,
                                showEmptyQueueMessage = true,
                                isCardFlipped = false
                            )
                        }
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
                        _uiState.update {
                            it.copy(
                                currentConcept = null,
                                showEmptyQueueMessage = true,
                                isCardFlipped = false
                            )
                        }
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
                        currentConcept = null,
                        showEmptyQueueMessage = true,
                        isCardFlipped = false,
                        error = "Queue yeniden yüklenirken hata: ${e.message}"
                    )
                }
                completeSession()
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
                    newWordsLearned = wordsStudied,
                    wordsReviewed = 0
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

    // ========== EVENT HANDLING ==========

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

    // ========== INITIALIZATION ==========

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                // Get user preferences
                val studyDirection = preferencesManager.getStudyDirection().first()
                val dailyGoal = preferencesManager.getDailyGoal().first()
                val ttsEnabled: Boolean = preferencesManager.isSoundEnabled().first()

                _uiState.update {
                    it.copy(
                        studyDirection = StudyDirection.valueOf(studyDirection),
                        dailyGoal = dailyGoal,
                        isTtsEnabled = ttsEnabled
                    )
                }

                // Load today's progress
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

    private fun loadStudyQueue() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val direction = _uiState.value.studyDirection
                DebugHelper.log("Loading study queue for direction: $direction")

                when (val hasWordsResult = studyRepository.hasWordsToStudy(direction)) {
                    is Result.Success -> {
                        if (!hasWordsResult.data) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isQueueEmpty = true,
                                    showEmptyQueueMessage = true,
                                    currentConcept = null,
                                    error = null
                                )
                            }
                            return@launch
                        }
                    }
                    is Result.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Kelime kontrolü hatası: ${hasWordsResult.error}"
                            )
                        }
                        return@launch
                    }
                }

                // Start new session
                startNewSession()

                // Load study queue
                studyRepository.getStudyQueue(direction, limit = 20).collectLatest { queueData ->
                    DebugHelper.log("Study queue received: ${queueData.size} words")

                    if (queueData.isEmpty()) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isQueueEmpty = true,
                                showEmptyQueueMessage = true,
                                currentConcept = null,
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

                    // Update queue
                    studyQueue = concepts
                    currentQueueIndex = 0

                    DebugHelper.log("Queue loaded: ${studyQueue.size} words")

                    if (studyQueue.isNotEmpty()) {
                        loadNextWord()
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isQueueEmpty = true,
                                showEmptyQueueMessage = true,
                                currentConcept = null,
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

    private fun loadDailyProgress() {
        // Simplified daily progress loading - no external dependency
        _uiState.update {
            it.copy(
                wordsStudiedToday = 0, // Will be updated as session progresses
                dailyProgressPercentage = 0f
            )
        }
    }

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
            learningPhase = true,
            sessionPosition = 1
        )
}