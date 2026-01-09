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
import com.hocalingo.app.feature.subscription.SubscriptionRepository
import com.hocalingo.app.core.common.SoundEffectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class StudyViewModel @Inject constructor(
    private val studyRepository: StudyRepository,
    private val preferencesManager: UserPreferencesManager,
    private val textToSpeechManager: TextToSpeechManager,
    private val ratingManager: RatingManager,
    private val feedbackRepository: FeedbackRepository,
    private val adMobManager: AdMobManager,
    private val nativeAdLoader: NativeAdLoader,
    private val subscriptionRepository: SubscriptionRepository,
    private val soundEffectManager: SoundEffectManager
) : ViewModel() {

    private val _uiState: MutableStateFlow<StudyUiState> = MutableStateFlow(StudyUiState())
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private val _effect: MutableSharedFlow<StudyEffect> = MutableSharedFlow()
    val effect: SharedFlow<StudyEffect> = _effect.asSharedFlow()

    val nativeAdState: StateFlow<NativeAd?> = nativeAdLoader.studyScreenAd

    val premiumAwareNativeAd: StateFlow<NativeAd?> = combine(
        subscriptionRepository.getLocalSubscriptionState(),
        nativeAdLoader.studyScreenAd
    ) { subscriptionState, nativeAd ->
        val result: NativeAd? = if (subscriptionState.isPremium) {
            null
        } else {
            nativeAd
        }
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = null
    )

    private var currentSessionId: Long? = null
    private var sessionStartTime: Long = 0L
    private var studyQueue: List<ConceptEntity> = emptyList()
    private var currentQueueIndex: Int = 0

    init {
        nativeAdLoader.loadStudyScreenAd()
        loadInitialData()
        trackTtsState()
        observePremiumStatus()

        viewModelScope.launch {
            nativeAdLoader.loadStudyScreenAd()
            adMobManager.loadStudyRewardedAd()
        }
    }

    private fun observePremiumStatus() {
        viewModelScope.launch {
            subscriptionRepository.getLocalSubscriptionState().collect { state ->
                if (state.isPremium) {
                    DebugHelper.log("👑 Premium detected in StudyViewModel - Clearing ads")
                    adMobManager.clearAdsForPremiumUser()
                    nativeAdLoader.clearAdsForPremiumUser()
                }
            }
        }
    }

    private fun trackTtsState() {
        viewModelScope.launch {
            textToSpeechManager.isSpeaking.collectLatest { isSpeaking ->
                _uiState.update { currentState ->
                    currentState.copy(isSpeaking = isSpeaking)
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.update { currentState ->
                    currentState.copy(isLoading = true)
                }

                val userStudyDirection: com.hocalingo.app.core.common.StudyDirection = preferencesManager.getStudyDirection().first()
                val dailyGoal: Int = preferencesManager.getDailyGoal().first()
                val ttsEnabled: Boolean = preferencesManager.isSoundEnabled().first()

                val entityStudyDirection: StudyDirection = when (userStudyDirection) {
                    com.hocalingo.app.core.common.StudyDirection.EN_TO_TR -> StudyDirection.EN_TO_TR
                    com.hocalingo.app.core.common.StudyDirection.TR_TO_EN -> StudyDirection.TR_TO_EN
                    com.hocalingo.app.core.common.StudyDirection.MIXED -> StudyDirection.EN_TO_TR
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        studyDirection = entityStudyDirection,
                        dailyGoal = dailyGoal,
                        isTtsEnabled = ttsEnabled
                    )
                }

                loadDailyProgress()
                loadStudyQueue()

            } catch (e: Exception) {
                DebugHelper.logError("StudyViewModel initialization error", e)
                _uiState.update { currentState ->
                    currentState.copy(
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
                _uiState.update { currentState ->
                    currentState.copy(isLoading = true, error = null)
                }

                val direction: StudyDirection = _uiState.value.studyDirection
                DebugHelper.log("Loading study queue for direction: $direction")

                val hasWordsResult: Result<Boolean> = studyRepository.hasWordsToStudy(direction)
                when (hasWordsResult) {
                    is Result.Success -> {
                        if (!hasWordsResult.data) {
                            _uiState.update { currentState ->
                                currentState.copy(
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

                        studyRepository.getStudyQueue(direction, limit = 70).collectLatest { queueData ->
                            DebugHelper.log("Study queue received: ${queueData.size} words")

                            if (queueData.isEmpty()) {
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        isLoading = false,
                                        isQueueEmpty = true,
                                        showEmptyQueueMessage = true,
                                        currentConcept = null
                                    )
                                }
                                return@collectLatest
                            }

                            val concepts: MutableList<ConceptEntity> = mutableListOf()
                            for (timingData in queueData) {
                                val conceptResult: Result<ConceptEntity?> = studyRepository.getConceptById(timingData.id)
                                when (conceptResult) {
                                    is Result.Success -> {
                                        val concept: ConceptEntity? = conceptResult.data
                                        if (concept != null) {
                                            concepts.add(concept)
                                        }
                                    }
                                    is Result.Error -> {
                                        DebugHelper.logError("Failed to get concept ${timingData.id}", conceptResult.error)
                                    }
                                }
                            }

                            studyQueue = concepts
                            currentQueueIndex = 0

                            if (studyQueue.isNotEmpty()) {
                                loadNextWord()
                            } else {
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        isLoading = false,
                                        isQueueEmpty = true,
                                        showEmptyQueueMessage = true,
                                        currentConcept = null,
                                        error = null
                                    )
                                }
                            }

                            _uiState.update { currentState ->
                                currentState.copy(
                                    isLoading = false,
                                    totalWordsInQueue = studyQueue.size,
                                    hasWordsToStudy = studyQueue.isNotEmpty()
                                )
                            }
                        }
                    }
                    is Result.Error -> {
                        DebugHelper.logError("Has words to study check failed", hasWordsResult.error)
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                error = "Çalışma kontrolü sırasında hata: ${hasWordsResult.error.message}"
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                DebugHelper.logError("Study queue loading error", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Çalışma kuyruğu yüklenemedi: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadNextWord() {
        DebugHelper.log("🔵 loadNextWord: index=$currentQueueIndex, size=${studyQueue.size}")

        if (currentQueueIndex >= studyQueue.size) {
            DebugHelper.log("🏁 Queue completed")
            completeSession()
            return
        }

        val currentConcept: ConceptEntity = studyQueue[currentQueueIndex]
        DebugHelper.log("Loading word ${currentQueueIndex + 1}/${studyQueue.size}: ${currentConcept.english}")

        viewModelScope.launch {
            try {
                val direction: StudyDirection = _uiState.value.studyDirection

                val progressResult: Result<WordProgressEntity?> = studyRepository.getCurrentProgress(currentConcept.id, direction)
                val currentProgress: WordProgressEntity = when (progressResult) {
                    is Result.Success -> progressResult.data
                    is Result.Error -> null
                } ?: createDefaultProgress(currentConcept.id, direction)

                val buttonPreviews: Triple<String, String, String> = SpacedRepetitionAlgorithm.getButtonPreviews(currentProgress)
                val hardTimeText: String = buttonPreviews.first
                val mediumTimeText: String = buttonPreviews.second
                val easyTimeText: String = buttonPreviews.third

                DebugHelper.log("🔍 Updating state: concept=${currentConcept.english}, isLoading=false")

                // ✅ STEP 1: Önce sadece front side'ı yükle (back text boş)
                _uiState.update { currentState ->
                    currentState.copy(
                        currentConcept = currentConcept,
                        currentCardIndex = currentQueueIndex,
                        remainingCards = studyQueue.size - currentQueueIndex,
                        isCardFlipped = false,
                        isLoading = false,
                        error = null,
                        easyTimeText = "", // Boş başlat
                        mediumTimeText = "", // Boş başlat
                        hardTimeText = "", // Boş başlat
                        backTextOverride = "",
                        isBackTextLoading = true // ✅ YENİ FLAG
                    )
                }

                viewModelScope.launch {
                    delay(150L)

                    // ✅ BackText'i hesapla
                    val calculatedBackText = when (direction) {
                        StudyDirection.EN_TO_TR -> currentConcept.turkish
                        StudyDirection.TR_TO_EN -> currentConcept.english
                    }

                    _uiState.update { currentState ->
                        currentState.copy(
                            backTextOverride = calculatedBackText,
                            easyTimeText = easyTimeText,
                            mediumTimeText = mediumTimeText,
                            hardTimeText = hardTimeText,
                            isBackTextLoading = false
                        )
                    }
                }

                viewModelScope.launch {
                    if (_uiState.value.isTtsEnabled) {
                        val currentDirection: StudyDirection = _uiState.value.studyDirection
                        if (currentDirection == StudyDirection.EN_TO_TR) {
                            delay(300L)
                            textToSpeechManager.speak(currentConcept.english, "en")
                            DebugHelper.log("🔊 Auto-TTS (EN→TR): Speaking '${currentConcept.english}'")
                        } else {
                            DebugHelper.log("🔇 Auto-TTS skipped (TR→EN): Will speak on flip")
                        }
                    }
                }

                DebugHelper.log("🔍 State updated successfully")
                DebugHelper.log("Word loaded: ${currentConcept.english} -> ${currentConcept.turkish}")

            } catch (e: Exception) {
                DebugHelper.logError("🔴 Load word exception", e)
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Kelime yüklenemedi: ${e.message}"
                    )
                }
            }
        }
    }

    private fun handleUserResponse(quality: Int) {
        _uiState.update { currentState ->
            currentState.copy(isCardFlipped = false)
        }

        val concept: ConceptEntity = _uiState.value.currentConcept ?: return
        val direction: StudyDirection = _uiState.value.studyDirection

        viewModelScope.launch {
            try {
                DebugHelper.log("🎯 handleUserResponse: quality=$quality, word=${concept.english}")

                val updateResult: Result<WordProgressEntity> = studyRepository.updateWordProgress(concept.id, direction, quality)
                when (updateResult) {
                    is Result.Success -> {
                        val updatedProgress: WordProgressEntity = updateResult.data
                        DebugHelper.log("✅ Progress updated: reps=${updatedProgress.repetitions}, interval=${updatedProgress.intervalDays}d")

                        val isCorrect: Boolean = quality >= SpacedRepetitionAlgorithm.QUALITY_MEDIUM
                        _uiState.update { currentState ->
                            currentState.copy(
                                sessionWordsCount = currentState.sessionWordsCount + 1,
                                correctAnswers = if (isCorrect) currentState.correctAnswers + 1 else currentState.correctAnswers
                            )
                        }

                        adMobManager.incrementStudyWordCount()
                        studyRepository.addCardStudyTime()

                        if (updatedProgress.learningPhase && updatedProgress.sessionPosition != null) {
                            DebugHelper.log("🔄 Learning phase word - reinserting with quality=$quality")
                            reinsertWordInQueue(currentQueueIndex, quality)
                        } else {
                            DebugHelper.log("🎓 Graduated/Review word - removing from current queue")
                        }

                        currentQueueIndex++
                        DebugHelper.log("🔵 Index incremented: $currentQueueIndex / ${studyQueue.size}")

                        val shouldShowAd: Boolean = adMobManager.shouldShowStudyRewardedAd()
                        val adState: AdState = adMobManager.studyRewardedAdState.value
                        val isAdLoaded: Boolean = adState is AdState.Loaded

                        DebugHelper.log("🔍 Rewarded Ad Check: shouldShow=$shouldShowAd, isLoaded=$isAdLoaded")

                        if (shouldShowAd) {
                            if (!isAdLoaded) {
                                DebugHelper.logError("⚠️ Rewarded ad not loaded - skipping")
                                adMobManager.loadStudyRewardedAd()
                            } else {
                                DebugHelper.log("🎯 Showing rewarded ad")
                                _uiState.update { currentState ->
                                    currentState.copy(currentConcept = null, isLoading = true)
                                }
                                _effect.emit(StudyEffect.ShowStudyRewardedAd)
                                return@launch
                            }
                        }

                        if (currentQueueIndex >= studyQueue.size) {
                            val learningWords: List<ConceptEntity> = studyQueue.filter { word ->
                                val progress: Result<WordProgressEntity?> = studyRepository.getCurrentProgress(word.id, direction)
                                val isLearning: Boolean = when (progress) {
                                    is Result.Success -> progress.data?.learningPhase == true
                                    is Result.Error -> false
                                }
                                isLearning
                            }

                            if (learningWords.isNotEmpty()) {
                                studyQueue = learningWords
                                currentQueueIndex = 0

                                DebugHelper.log("🔄 Queue filtered: ${studyQueue.size} learning words remain (${learningWords.map { it.english }})")

                                _uiState.update { currentState ->
                                    currentState.copy(
                                        totalWordsInQueue = studyQueue.size,
                                        remainingCards = studyQueue.size
                                    )
                                }

                                loadNextWord()
                            } else {
                                DebugHelper.log("✅ All words completed and graduated!")
                                _uiState.update { currentState ->
                                    currentState.copy(
                                        currentConcept = null,
                                        showEmptyQueueMessage = true,
                                        isCardFlipped = false
                                    )
                                }
                                completeSession()
                            }
                            return@launch
                        }

                        if (currentQueueIndex > 0 && currentQueueIndex % 12 == 0) {
                            val isPremium: Boolean = subscriptionRepository.isPremium()

                            if (!isPremium) {
                                DebugHelper.log("🎯 12 words completed - showing native ad")
                                _uiState.update { currentState ->
                                    currentState.copy(showNativeAd = true)
                                }
                                nativeAdLoader.loadStudyScreenAd()
                                return@launch
                            } else {
                                DebugHelper.log("👑 Premium user - skipping native ad at word 12")
                            }
                        }

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

            _uiState.update { currentState ->
                currentState.copy(isLoading = false)
            }

            if (currentQueueIndex >= studyQueue.size) {
                val direction: StudyDirection = _uiState.value.studyDirection
                val learningWords: List<ConceptEntity> = studyQueue.filter { word ->
                    val progress: Result<WordProgressEntity?> = studyRepository.getCurrentProgress(word.id, direction)
                    val isLearning: Boolean = when (progress) {
                        is Result.Success -> progress.data?.learningPhase == true
                        is Result.Error -> false
                    }
                    isLearning
                }

                if (learningWords.isNotEmpty()) {
                    studyQueue = learningWords
                    currentQueueIndex = 0
                    DebugHelper.log("🔄 Queue filtered after ad: ${studyQueue.size} learning words")

                    _uiState.update { currentState ->
                        currentState.copy(
                            totalWordsInQueue = studyQueue.size,
                            remainingCards = studyQueue.size
                        )
                    }

                    loadNextWord()
                    val currentAdState: AdState = adMobManager.studyRewardedAdState.value
                    if (currentAdState !is AdState.Loaded) {
                        adMobManager.loadStudyRewardedAd()
                    }
                    return@launch
                } else {
                    completeSession()
                    return@launch
                }
            }

            if (currentQueueIndex % 12 == 0 && currentQueueIndex > 0) {
                _uiState.update { currentState ->
                    currentState.copy(showNativeAd = true)
                }
                return@launch
            }

            loadNextWord()
            val currentAdState: AdState = adMobManager.studyRewardedAdState.value
            if (currentAdState !is AdState.Loaded) {
                adMobManager.loadStudyRewardedAd()
            }
        }
    }

    private fun flipCard() {
        soundEffectManager.playCardFlip()

        val wasFlipped: Boolean = _uiState.value.isCardFlipped
        _uiState.update { currentState ->
            currentState.copy(isCardFlipped = !currentState.isCardFlipped)
        }

        val direction: StudyDirection = _uiState.value.studyDirection
        val concept: ConceptEntity? = _uiState.value.currentConcept

        if (!wasFlipped && direction == StudyDirection.TR_TO_EN && concept != null) {
            if (_uiState.value.isTtsEnabled) {
                viewModelScope.launch {
                    delay(200L)
                    textToSpeechManager.speak(concept.english, "en")
                    DebugHelper.log("🔊 Flip-TTS (TR→EN): Speaking '${concept.english}'")
                }
            }
        }
    }

    private fun resetCard() {
        _uiState.update { currentState ->
            currentState.copy(isCardFlipped = false)
        }
    }

    private fun playPronunciation() {
        val concept: ConceptEntity? = _uiState.value.currentConcept
        if (concept != null && _uiState.value.isTtsEnabled) {
            textToSpeechManager.speak(concept.english, "en")
            DebugHelper.log("TTS: Speaking English word '${concept.english}'")
        }
    }

    private fun stopTts() {
        textToSpeechManager.stop()
        DebugHelper.log("TTS: Stopped")
    }

    private suspend fun startNewSession() {
        val result: Result<Long> = studyRepository.startStudySession(SessionType.MIXED)
        when (result) {
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
            val sessionId: Long? = currentSessionId
            if (sessionId != null) {
                val wordsStudied: Int = _uiState.value.sessionWordsCount
                val correctAnswers: Int = _uiState.value.correctAnswers

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
        _uiState.update { currentState ->
            currentState.copy(
                wordsStudiedToday = 0,
                dailyProgressPercentage = 0f
            )
        }
    }

    private fun navigateToWordSelection() {
        _effect.tryEmit(StudyEffect.NavigateToWordSelection)
    }

    private fun navigateBack() {
        _effect.tryEmit(StudyEffect.NavigateToHome)
    }

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
                _uiState.update { currentState ->
                    currentState.copy(showNativeAd = false)
                }
                loadNextWord()
            }
            StudyEvent.ShowSatisfactionDialog -> showSatisfactionDialog()
            StudyEvent.DismissSatisfactionDialog -> dismissSatisfactionDialog()
            is StudyEvent.SatisfactionSelected -> handleSatisfactionSelected(event.level)
            StudyEvent.DismissFeedbackDialog -> dismissFeedbackDialog()
            is StudyEvent.SubmitFeedback -> submitFeedback(event.category, event.message, event.email)
            StudyEvent.ContinueAfterAd -> continueAfterAd()
        }
    }

    private fun reinsertWordInQueue(currentIndex: Int, quality: Int) {
        if (currentIndex >= studyQueue.size) return

        val word: ConceptEntity = studyQueue[currentIndex]
        val mutableQueue: MutableList<ConceptEntity> = studyQueue.toMutableList()

        mutableQueue.removeAt(currentIndex)

        val remainingSize: Int = mutableQueue.size

        val offsetPercentage: Float = when (quality) {
            SpacedRepetitionAlgorithm.QUALITY_HARD -> 0.60f
            SpacedRepetitionAlgorithm.QUALITY_MEDIUM -> 0.80f
            SpacedRepetitionAlgorithm.QUALITY_EASY -> 1.0f
            else -> 1.0f
        }

        val offset: Int = (remainingSize * offsetPercentage).toInt()
        val newIndex: Int = offset.coerceIn(0, remainingSize)

        mutableQueue.add(newIndex, word)
        studyQueue = mutableQueue

        DebugHelper.log("🔄 Reordered: ${word.english} | Quality=$quality | Queue: $remainingSize cards | Offset: ${offsetPercentage * 100}% = $offset cards | Position: $currentIndex → $newIndex")
    }

    private fun createDefaultProgress(conceptId: Int, direction: StudyDirection): WordProgressEntity {
        return WordProgressEntity(
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

    internal fun getRatingManager(): RatingManager = ratingManager

    internal fun getFeedbackRepository(): FeedbackRepository = feedbackRepository

    internal fun updateUiState(update: (StudyUiState) -> StudyUiState) {
        _uiState.update(update)
    }

    internal fun emitEffect(effect: StudyEffect) {
        viewModelScope.launch {
            _effect.emit(effect)
        }
    }

    internal fun getUiState(): StudyUiState = _uiState.value

    fun getAdMobManager(): AdMobManager = adMobManager

    override fun onCleared() {
        super.onCleared()
        textToSpeechManager.stop()
        DebugHelper.log("StudyViewModel cleared")
    }
}