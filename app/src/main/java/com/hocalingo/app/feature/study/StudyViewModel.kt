package com.hocalingo.app.feature.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.nativead.NativeAd
import com.hocalingo.app.core.ads.AdMobManager
import com.hocalingo.app.core.ads.AdState
import com.hocalingo.app.core.ads.NativeAdLoader
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.common.SpacedRepetitionAlgorithm
import com.hocalingo.app.core.common.TextToSpeechManager
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.feedback.RatingManager
import com.hocalingo.app.core.feedback.FeedbackRepository
import com.hocalingo.app.database.entities.ConceptEntity
import com.hocalingo.app.database.entities.StudyDirection
import com.hocalingo.app.database.entities.SessionType
import com.hocalingo.app.database.entities.WordProgressEntity
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
 * Package: app/src/main/java/com/hocalingo/app/feature/study/
 *
 * ✅ FIXED: Index artırma mantığı düzeltildi
 * ✅ FIXED: Native ad her 10 kelimede doğru gösteriliyor
 * ✅ FIXED: Rewarded ad sonrası kelime atlanmıyor
 * ✅ FIXED: wordsCompletedCount kaldırıldı, currentQueueIndex kullanılıyor
 */
@HiltViewModel
class StudyViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val preferencesManager: UserPreferencesManager,
    private val textToSpeechManager: TextToSpeechManager,
    private val ratingManager: RatingManager,
    private val feedbackRepository: FeedbackRepository,
    private val adMobManager: AdMobManager,
    private val nativeAdLoader: NativeAdLoader
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<StudyEffect>()
    val effect: SharedFlow<StudyEffect> = _effect.asSharedFlow()

    val nativeAdState: StateFlow<NativeAd?> = nativeAdLoader.studyScreenAd

    // Study session tracking
    private var currentSessionId: Long? = null
    private var sessionStartTime: Long = 0
    private var studyQueue: List<ConceptEntity> = emptyList()
    private var currentQueueIndex: Int = 0

    init {
        loadInitialData()
        trackTtsState()

        viewModelScope.launch {
            nativeAdLoader.loadStudyScreenAd()
            // ✅ YENİ SATIR - BURAYA EKLE
            adMobManager.loadStudyRewardedAd()
        }
    }

    // ========== INITIALIZATION ==========

    private fun trackTtsState() {
        viewModelScope.launch {
            textToSpeechManager.isSpeaking.collectLatest { isSpeaking ->
                _uiState.update { it.copy(isSpeaking = isSpeaking) }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }

                val userStudyDirection = preferencesManager.getStudyDirection().first()
                val dailyGoal = preferencesManager.getDailyGoal().first()
                val ttsEnabled: Boolean = preferencesManager.isSoundEnabled().first()

                val entityStudyDirection = when (userStudyDirection) {
                    com.hocalingo.app.core.common.StudyDirection.EN_TO_TR ->
                        StudyDirection.EN_TO_TR
                    com.hocalingo.app.core.common.StudyDirection.TR_TO_EN ->
                        StudyDirection.TR_TO_EN
                    com.hocalingo.app.core.common.StudyDirection.MIXED ->
                        StudyDirection.EN_TO_TR
                }

                _uiState.update {
                    it.copy(
                        studyDirection = entityStudyDirection,
                        dailyGoal = dailyGoal,
                        isTtsEnabled = ttsEnabled
                    )
                }

                loadDailyProgress()
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

    // ========== STUDY QUEUE MANAGEMENT ==========

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

                        startNewSession()

                        studyRepository.getStudyQueue(direction, limit = 20)
                            .collectLatest { queueData ->
                                DebugHelper.log("Study queue received: ${queueData.size} words")

                                if (queueData.isEmpty()) {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            isQueueEmpty = true,
                                            showEmptyQueueMessage = true,
                                            currentConcept = null
                                        )
                                    }
                                    return@collectLatest
                                }

                                val concepts = mutableListOf<ConceptEntity>()
                                for (timingData in queueData) {
                                    when (val result =
                                        studyRepository.getConceptById(timingData.id)) {
                                        is Result.Success -> {
                                            result.data?.let { concepts.add(it) }
                                        }
                                        is Result.Error -> {
                                            DebugHelper.logError(
                                                "Failed to get concept ${timingData.id}",
                                                result.error
                                            )
                                        }
                                    }
                                }

                                studyQueue = concepts
                                currentQueueIndex = 0

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
                    }

                    is Result.Error -> {
                        DebugHelper.logError(
                            "Has words to study check failed",
                            hasWordsResult.error
                        )
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Çalışma kontrolü sırasında hata: ${hasWordsResult.error.message}"
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                DebugHelper.logError("Study queue loading error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Çalışma kuyruğu yüklenemedi: ${e.message}"
                    )
                }
            }
        }
    }

    // ========== WORD MANAGEMENT ==========



    /**
     * ✅ UNCHANGED: Load next word from queue
     */
    private fun loadNextWord() {
        DebugHelper.log("🔵 loadNextWord: index=$currentQueueIndex, size=${studyQueue.size}")

        if (currentQueueIndex >= studyQueue.size) {
            DebugHelper.log("🏁 Queue completed")
            completeSession()
            return
        }

        val currentConcept = studyQueue[currentQueueIndex]
        DebugHelper.log("Loading word ${currentQueueIndex + 1}/${studyQueue.size}: ${currentConcept.english}")

        viewModelScope.launch {
            try {
                val direction = _uiState.value.studyDirection

                val progressResult =
                    studyRepository.getCurrentProgress(currentConcept.id, direction)
                val currentProgress = when (progressResult) {
                    is Result.Success -> progressResult.data
                    is Result.Error -> null
                } ?: createDefaultProgress(currentConcept.id, direction)

                val (hardTimeText, mediumTimeText, easyTimeText) = SpacedRepetitionAlgorithm.getButtonPreviews(
                    currentProgress
                )

                DebugHelper.log("🔍 Updating state: concept=${currentConcept.english}, isLoading=false")

                _uiState.update {
                    it.copy(
                        currentConcept = currentConcept,
                        currentCardIndex = currentQueueIndex,
                        remainingCards = studyQueue.size - currentQueueIndex,
                        isCardFlipped = false,
                        isLoading = false, // ✅ FIXED: Hide loading screen
                        error = null,      // ✅ FIXED: Clear any errors
                        easyTimeText = easyTimeText,
                        mediumTimeText = mediumTimeText,
                        hardTimeText = hardTimeText
                    )
                }

                DebugHelper.log("🔍 State updated successfully")
                DebugHelper.log("Word loaded: ${currentConcept.english} -> ${currentConcept.turkish}")

            } catch (e: Exception) {
                DebugHelper.logError("🔴 Load word exception", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Kelime yüklenemedi: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * ✅ FIXED: User response handling with corrected index logic
     */
    private fun handleUserResponse(quality: Int) {
        val concept = _uiState.value.currentConcept ?: return
        val direction = _uiState.value.studyDirection

        viewModelScope.launch {
            try {
                DebugHelper.log("🎯 handleUserResponse: quality=$quality, word=${concept.english}")

                // 1. Update word progress
                when (val updateResult =
                    studyRepository.updateWordProgress(concept.id, direction, quality)) {
                    is Result.Success -> {
                        val updatedProgress = updateResult.data
                        DebugHelper.log("✅ Progress updated: reps=${updatedProgress.repetitions}, interval=${updatedProgress.intervalDays}d")

                        // 2. Update session stats
                        val isCorrect = quality >= SpacedRepetitionAlgorithm.QUALITY_MEDIUM
                        _uiState.update {
                            it.copy(
                                sessionWordsCount = it.sessionWordsCount + 1,
                                correctAnswers = if (isCorrect) it.correctAnswers + 1 else it.correctAnswers
                            )
                        }

                        // 3. Increment AdMob study word counter
                        adMobManager.incrementStudyWordCount()

                        // 4. INCREMENT INDEX (single place - always)
                        currentQueueIndex++
                        DebugHelper.log("🔵 Index incremented: $currentQueueIndex / ${studyQueue.size}")

                        // 5. Check rewarded ad (25 words)
                        val shouldShowAd = adMobManager.shouldShowStudyRewardedAd()
                        val isAdLoaded = adMobManager.studyRewardedAdState.value is AdState.Loaded

                        DebugHelper.log("🔍 Rewarded Ad Check: shouldShow=$shouldShowAd, isLoaded=$isAdLoaded")

                        if (shouldShowAd) {
                            if (!isAdLoaded) {
                                DebugHelper.logError("⚠️ Rewarded ad not loaded - skipping")
                                adMobManager.resetStudyWordCount()
                                adMobManager.loadStudyRewardedAd()
                                // Continue to next checks
                            } else {
                                DebugHelper.log("🎯 Showing rewarded ad")
                                _uiState.update { it.copy(currentConcept = null, isLoading = true) }
                                _effect.emit(StudyEffect.ShowStudyRewardedAd)
                                return@launch
                            }
                        }

                        // 6. Check queue completion
                        if (currentQueueIndex >= studyQueue.size) {
                            DebugHelper.log("✅ All words completed!")
                            _uiState.update {
                                it.copy(
                                    currentConcept = null,
                                    showEmptyQueueMessage = true,
                                    isCardFlipped = false
                                )
                            }
                            completeSession()
                            return@launch
                        }

                        // 7. Check native ad (every 10 words)
                        if (currentQueueIndex > 0 && currentQueueIndex % 10 == 0) {
                            DebugHelper.log("🎯 10 words completed - showing native ad")
                            _uiState.update { it.copy(showNativeAd = true) }
                            nativeAdLoader.loadStudyScreenAd()
                            return@launch
                        }

                        // 8. Load next word
                        loadNextWord()
                    }

                    is Result.Error -> {
                        DebugHelper.logError("Progress update error", updateResult.error)
                        _effect.emit(StudyEffect.ShowMessage("Kelime kaydedilemedi"))
                    }
                }

            } catch (e: Exception) {
                DebugHelper.logError("Handle response error", e)
            }
        }
    }

    fun continueAfterAd() {
        viewModelScope.launch {
            DebugHelper.log("🔍 continueAfterAd called")

            // Önce loading'i kapat
            _uiState.update { it.copy(isLoading = false) }

            if (currentQueueIndex < studyQueue.size) {
                loadNextWord()
                // Sonraki için preload
                adMobManager.loadStudyRewardedAd()
            } else {
                _uiState.update {
                    it.copy(
                        currentConcept = null,
                        showEmptyQueueMessage = true,
                        isCardFlipped = false,
                        isLoading = false
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
            val (textToSpeak, language) = when (_uiState.value.studyDirection) {
                StudyDirection.EN_TO_TR -> concept.english to "en"
                StudyDirection.TR_TO_EN -> concept.turkish to "tr"
            }

            if (textToSpeak.isNotEmpty()) {
                textToSpeechManager.speak(textToSpeak, language)
                DebugHelper.log("TTS: Speaking '$textToSpeak' in $language")
            }
        }
    }

    private fun stopTts() {
        textToSpeechManager.stop()
        DebugHelper.log("TTS: Stopped")
    }

    // ========== SESSION MANAGEMENT ==========

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

                checkAndShowRatingPrompt(wordsStudied, correctAnswers)
            }
        }
    }

    private fun endCurrentSession() {
        completeSession()
        _effect.tryEmit(StudyEffect.NavigateToHome)
    }

    private fun loadDailyProgress() {
        _uiState.update {
            it.copy(
                wordsStudiedToday = 0,
                dailyProgressPercentage = 0f
            )
        }
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

            StudyEvent.CloseNativeAd -> {
                DebugHelper.log("🔵 CloseNativeAd: hiding ad and loading next word")
                _uiState.update { it.copy(showNativeAd = false) }
                loadNextWord()
            }

            // ========== RATING EVENTS ==========
            StudyEvent.ShowSatisfactionDialog -> showSatisfactionDialog()
            StudyEvent.DismissSatisfactionDialog -> dismissSatisfactionDialog()
            is StudyEvent.SatisfactionSelected -> handleSatisfactionSelected(event.level)
            StudyEvent.DismissFeedbackDialog -> dismissFeedbackDialog()
            is StudyEvent.SubmitFeedback -> submitFeedback(
                event.category,
                event.message,
                event.email
            )

            StudyEvent.ContinueAfterAd -> continueAfterAd()
        }
    }

    // ========== HELPER METHODS ==========

    private fun createDefaultProgress(conceptId: Int, direction: StudyDirection) =
        WordProgressEntity(
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

    // ========== RATING ACCESSOR FUNCTIONS ==========

    internal fun getRatingManager() = ratingManager

    internal fun getFeedbackRepository() = feedbackRepository

    internal fun updateUiState(update: (StudyUiState) -> StudyUiState) {
        _uiState.update(update)
    }

    internal fun emitEffect(effect: StudyEffect) {
        viewModelScope.launch { _effect.emit(effect) }
    }

    internal fun getUiState() = _uiState.value

    // ========== ADMOB ACCESSOR ==========
    fun getAdMobManager() = adMobManager

    // ========== CLEANUP ==========

    override fun onCleared() {
        super.onCleared()
        textToSpeechManager.stop()
        DebugHelper.log("StudyViewModel cleared")
    }
}