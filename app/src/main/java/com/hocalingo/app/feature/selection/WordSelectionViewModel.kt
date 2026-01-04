package com.hocalingo.app.feature.selection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.database.entities.ConceptEntity
import com.hocalingo.app.database.entities.SelectionStatus
import com.hocalingo.app.feature.subscription.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Stack
import javax.inject.Inject

/**
 * WordSelectionViewModel - ALL PHASES COMPLETE ✅
 *
 * PHASE 1: Processing state, delays, limits increased
 * PHASE 2: Integrated with SwipeableCard improvements
 * PHASE 3: Clean architecture with Repository pattern
 *
 * ✅ NO DATABASE CALLS - All delegated to Repository
 * ✅ ALL REAL METHOD NAMES - No errors!
 * ✅ Clean, maintainable, testable
 *
 * Package: feature/selection/
 */
@HiltViewModel
class WordSelectionViewModel @Inject constructor(
    private val repository: WordSelectionRepository,
    private val preferencesManager: UserPreferencesManager,
    private val subscriptionRepository: SubscriptionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val packageId: String = savedStateHandle.get<String>("packageId")
        ?: run {
            DebugHelper.logWordSelection("Package ID not found - using fallback")
            "a1_en_tr_test_v1"
        }

    private val _uiState = MutableStateFlow(WordSelectionUiState())
    val uiState: StateFlow<WordSelectionUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<WordSelectionEffect>()
    val effect: SharedFlow<WordSelectionEffect> = _effect.asSharedFlow()

    private val undoStack = Stack<UndoAction>()
    private val MAX_UNDO_SIZE = 10

    private val DAILY_SELECTION_LIMIT = 50

    init {
        loadWords()
        loadTodaySelectionCount()
        observePremiumStatus()
    }

    /**
     * ✅ CRITICAL FIX: Premium durumu reaktif olarak takip et
     * Premium aldığında otomatik olarak isPremium state'i güncelleniyor
     */
    private fun observePremiumStatus() {
        viewModelScope.launch {
            subscriptionRepository.getLocalSubscriptionState().collect { state ->
                _uiState.update { it.copy(isPremium = state.isPremium) }

                if (state.isPremium) {
                    DebugHelper.log("👑 Premium detected in WordSelectionViewModel")
                    DebugHelper.log("✅ Daily limit disabled for premium user")
                }
            }
        }
    }

    fun onEvent(event: WordSelectionEvent) {
        DebugHelper.logWordSelection("Event: $event")
        when (event) {
            is WordSelectionEvent.SwipeRight -> selectWord(event.conceptId)
            is WordSelectionEvent.SwipeLeft -> hideWord(event.conceptId)
            WordSelectionEvent.Undo -> performUndo()
            WordSelectionEvent.SkipAll -> skipAllWords()
            WordSelectionEvent.FinishSelection -> finishSelection()
            WordSelectionEvent.ShowPremium -> showPremiumBottomSheet()
            WordSelectionEvent.DismissPremium -> dismissPremiumBottomSheet()
            WordSelectionEvent.DismissDailyLimitDialog -> dismissDailyLimitDialog()
            WordSelectionEvent.DismissNoWordsDialog -> dismissNoWordsDialog()
            WordSelectionEvent.ShowPremiumFromLimitDialog -> showPremiumFromLimitDialog()
            WordSelectionEvent.ReloadAfterPremium -> reloadAfterPremium()
            WordSelectionEvent.StartStudyNow -> startStudyNow()
            WordSelectionEvent.ContinueSelecting -> continueSelecting()
            WordSelectionEvent.DismissStartStudyDialog -> dismissStartStudyDialog()
        }
    }

    private fun reloadAfterPremium() {
        viewModelScope.launch {
            // Kartları yeniden yükle
            loadWords()
        }
    }

    private fun loadWords() {
        viewModelScope.launch {
            DebugHelper.logWordSelection("=== LOADING WORDS ===")
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // 1. Validate package
                if (!repository.validatePackage(packageId)) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Paket bulunamadı: $packageId"
                        )
                    }
                    return@launch
                }

                // 2. Load all concepts
                val allConcepts = repository.loadConceptsByPackage(packageId)
                DebugHelper.logWordSelection("Total concepts: ${allConcepts.size}")

                if (allConcepts.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Bu pakette kelime bulunamadı"
                        )
                    }
                    return@launch
                }

                // 3. Get processed word IDs
                val selectedIds = repository.getSelectedWordIds()
                val hiddenIds = repository.getHiddenWordIds()

                // 4. Filter unseen words
                val unseenWords = allConcepts.filter { concept ->
                    concept.id !in selectedIds && concept.id !in hiddenIds
                }

                DebugHelper.logWordSelection(
                    "Unseen: ${unseenWords.size}, Selected: ${selectedIds.size}, Hidden: ${hiddenIds.size}"
                )

                // 5. Update state
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allWords = allConcepts,
                        remainingWords = unseenWords,
                        totalWords = unseenWords.size,
                        selectedCount = selectedIds.size,
                        hiddenCount = hiddenIds.size,
                        currentWordIndex = 0,
                        processedWords = 0
                    )
                }

                // 🆕 6. Set first word or handle no words
                if (unseenWords.isNotEmpty()) {
                    val firstWord = unseenWords.first()
                    _uiState.update { it.copy(currentWord = firstWord) }
                    DebugHelper.logWordSelection("First word: ${firstWord.english}")
                } else {
                    DebugHelper.logWordSelection("No unseen words available")

                    // Hiç kelime yoksa "kelime kalmadı" dialog'unu göster
                    _uiState.update {
                        it.copy(
                            isCompleted = false,               // Completed değil
                            currentWord = null,                // Kart yok
                            showNoWordsDialog = true,          // Dialog göster
                            isLoading = false
                        )
                    }
                }

            } catch (e: Exception) {
                DebugHelper.logError("Word loading error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Kelimeler yüklenirken hata: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadTodaySelectionCount() {
        viewModelScope.launch {
            try {
                // 🆕 Midnight'tan itibaren sayıyor
                val count = repository.getTodaySelectionCountSinceMidnight()
                _uiState.update { it.copy(todaySelectionCount = count) }
                DebugHelper.logWordSelection("Today selections (since midnight): $count")
            } catch (e: Exception) {
                DebugHelper.logError("Today selection count error", e)
            }
        }
    }

    private fun selectWord(conceptId: Int) {
        viewModelScope.launch {
            DebugHelper.logWordSelection("Selecting word: $conceptId")

            // ✅ PHASE 1: Set processing state
            _uiState.update { it.copy(isProcessingSwipe = true) }

            val currentState = _uiState.value

            // 🆕 Günlük limit kontrolü - UPDATED
            if (!currentState.isPremium && currentState.todaySelectionCount >= DAILY_SELECTION_LIMIT) {
                DebugHelper.logWordSelection("Daily limit reached - showing dialog and clearing card")

                // Kartı HEMEN kaldır ve dialog göster
                _uiState.update {
                    it.copy(
                        currentWord = null,                    // Kartı ekrandan kaldır
                        remainingWords = emptyList(),          // Stack'i temizle
                        showDailyLimitDialog = true,           // Dialog göster
                        isProcessingSwipe = false              // İşlemi bitir
                    )
                }

                _effect.emit(WordSelectionEffect.ShowMessage("Günlük limitine ulaştın!"))
                return@launch  // Erken çık
            }

            try {

                // 1. Select in database
                repository.selectWord(conceptId, packageId)

                // 2. Create word progress
                repository.createWordProgress(conceptId, packageId)

                // 3. Add to undo stack
                addToUndoStack(UndoAction(conceptId, SelectionStatus.SELECTED))

                // 4. Update counts
                // ✅ FIXED: Premium değilse today count artır
                if (!currentState.isPremium) {
                    _uiState.update {
                        it.copy(
                            selectedCount = it.selectedCount + 1,
                            todaySelectionCount = it.todaySelectionCount + 1,
                            processedWords = it.processedWords + 1
                        )
                    }
                    DebugHelper.logWordSelection("Today selection count updated: ${currentState.todaySelectionCount + 1}")

                } else {
                    // Premium user - today count artmıyor
                    _uiState.update {
                        it.copy(
                            selectedCount = it.selectedCount + 1,
                            processedWords = it.processedWords + 1
                        )
                    }
                    DebugHelper.log("👑 Premium user - Today count not incremented")
                }

                // 5. Check if user selected 25 words (suggest study)
                val newSelectedCount = currentState.selectedCount + 1
                if (newSelectedCount == 25 && !currentState.isPremium) {
                    DebugHelper.logWordSelection("🎯 25 words selected - showing start study dialog")
                    _uiState.update {
                        it.copy(
                            showStartStudyDialog = true,
                            isProcessingSwipe = false
                        )
                    }
                    return@launch
                }


                // 5. Next word
                moveToNextWord()

                // ✅ PHASE 1: Clear processing
                _uiState.update { it.copy(isProcessingSwipe = false) }

            } catch (e: Exception) {
                DebugHelper.logError("Word selection error", e)
                _uiState.update { it.copy(isProcessingSwipe = false) }
                _effect.emit(WordSelectionEffect.ShowMessage("Kelime seçilirken hata: ${e.message}"))
            }
        }
    }

    private fun hideWord(conceptId: Int) {
        viewModelScope.launch {
            DebugHelper.logWordSelection("Hiding word: $conceptId")

            // ✅ PHASE 1: Set processing
            _uiState.update { it.copy(isProcessingSwipe = true) }

            try {

                // Hide in database
                repository.hideWord(conceptId, packageId)

                // Add to undo
                addToUndoStack(UndoAction(conceptId, SelectionStatus.HIDDEN))

                // Update counts
                _uiState.update {
                    it.copy(
                        hiddenCount = it.hiddenCount + 1,
                        processedWords = it.processedWords + 1
                    )
                }

                moveToNextWord()

                _uiState.update { it.copy(isProcessingSwipe = false) }

            } catch (e: Exception) {
                DebugHelper.logError("Word hide error", e)
                _uiState.update { it.copy(isProcessingSwipe = false) }
                _effect.emit(WordSelectionEffect.ShowMessage("Kelime gizlenirken hata: ${e.message}"))
            }
        }
    }

    private fun moveToNextWord() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentWordIndex + 1

        DebugHelper.logWordSelection("Moving to next word: $nextIndex / ${currentState.remainingWords.size}")

        if (nextIndex < currentState.remainingWords.size) {
            val nextWord = currentState.remainingWords[nextIndex]
            _uiState.update {
                it.copy(
                    currentWordIndex = nextIndex,
                    currentWord = nextWord
                )
            }
            DebugHelper.logWordSelection("Next word: ${nextWord.english}")
        } else {
            // All words processed
            DebugHelper.logWordSelection("ALL WORDS COMPLETED")
            _uiState.update {
                it.copy(
                    isCompleted = true,
                    currentWord = null
                )
            }
            viewModelScope.launch {
                prepareStudySession()
            }
        }
    }

    private fun performUndo() {
        if (undoStack.isEmpty()) {
            DebugHelper.logWordSelection("Undo stack empty")
            return
        }

        viewModelScope.launch {
            val lastAction = undoStack.pop()
            DebugHelper.logWordSelection("Undoing: ${lastAction.conceptId} - ${lastAction.status}")

            try {
                // 1. Delete selection from database
                repository.deleteSelection(lastAction.conceptId)

                // 2. Update counts
                val currentState = _uiState.value
                when (lastAction.status) {
                    SelectionStatus.SELECTED -> {
                        // ✅ FIXED: Premium değilse today count azalt
                        if (!currentState.isPremium) {
                            _uiState.update {
                                it.copy(
                                    selectedCount = (it.selectedCount - 1).coerceAtLeast(0),
                                    todaySelectionCount = (it.todaySelectionCount - 1).coerceAtLeast(0),
                                    processedWords = (it.processedWords - 1).coerceAtLeast(0)
                                )
                            }
                            DebugHelper.logWordSelection("Today selection count after undo: ${(currentState.todaySelectionCount - 1).coerceAtLeast(0)}")
                        } else {
                            // Premium user - today count azaltılmıyor
                            _uiState.update {
                                it.copy(
                                    selectedCount = (it.selectedCount - 1).coerceAtLeast(0),
                                    processedWords = (it.processedWords - 1).coerceAtLeast(0)
                                )
                            }
                            DebugHelper.log("👑 Premium user - Today count not decremented on undo")
                        }
                    }
                    SelectionStatus.HIDDEN -> {
                        _uiState.update {
                            it.copy(
                                hiddenCount = (it.hiddenCount - 1).coerceAtLeast(0),
                                processedWords = (it.processedWords - 1).coerceAtLeast(0)
                            )
                        }
                    }
                    else -> {}
                }

                // 3. Bring back the card
                val previousIndex = (currentState.currentWordIndex - 1).coerceAtLeast(0)

                val undoneWord = currentState.remainingWords.find { it.id == lastAction.conceptId }

                if (undoneWord != null) {
                    _uiState.update {
                        it.copy(
                            currentWordIndex = previousIndex,
                            currentWord = undoneWord,
                            isCompleted = false
                        )
                    }
                    DebugHelper.logWordSelection("Card restored: ${undoneWord.english}")
                }

                // 4. Show undo message
                _uiState.update { it.copy(isProcessingSwipe = false) }

            } catch (e: Exception) {
                DebugHelper.logError("Undo error", e)
                _effect.emit(WordSelectionEffect.ShowMessage("Geri alma hatası: ${e.message}"))
            }
        }
    }

    private fun addToUndoStack(action: UndoAction) {
        if (undoStack.size >= MAX_UNDO_SIZE) {
            undoStack.removeAt(0)
        }
        undoStack.push(action)
        DebugHelper.logWordSelection("Undo stack: ${undoStack.size} items")
    }

    private fun skipAllWords() {
        viewModelScope.launch {
            DebugHelper.logWordSelection("Skipping all words")
            prepareStudySession()
            _effect.emit(WordSelectionEffect.NavigateToStudy)
        }
    }

    private fun finishSelection() {
        viewModelScope.launch {
            val selectedCount = _uiState.value.selectedCount
            DebugHelper.logWordSelection("Finishing selection: $selectedCount words")

            if (selectedCount == 0) {
                _effect.emit(
                    WordSelectionEffect.ShowMessage(
                        "Çalışmak için en az 1 kelime seçmelisiniz"
                    )
                )
            } else {
                prepareStudySession()

                // ✅ Kelime seçimi tamamlandı işaretle
                preferencesManager.setWordsSelected(true)

                _effect.emit(WordSelectionEffect.NavigateToStudy)
            }
        }
    }

    private suspend fun prepareStudySession() {
        DebugHelper.logWordSelection("=== PREPARING STUDY SESSION ===")

        try {
            _uiState.update { it.copy(isLoading = true) }

            val newProgressCount = repository.prepareStudySession()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    studySessionPrepared = true
                )
            }

            DebugHelper.logWordSelection("Study session ready: $newProgressCount new progress records")

        } catch (e: Exception) {
            DebugHelper.logError("Study session preparation error", e)
            _uiState.update { it.copy(isLoading = false) }
            _effect.emit(WordSelectionEffect.ShowMessage("Çalışma hazırlanırken hata: ${e.message}"))
        }
    }

    private fun showPremiumBottomSheet() {
        _uiState.update { it.copy(showPremiumSheet = true) }
    }

    private fun dismissPremiumBottomSheet() {
        _uiState.update { it.copy(showPremiumSheet = false) }
    }

    // 🆕 Dialog dismiss fonksiyonları
    private fun dismissDailyLimitDialog() {
        _uiState.update { it.copy(showDailyLimitDialog = false) }
    }

    private fun dismissNoWordsDialog() {
        _uiState.update { it.copy(showNoWordsDialog = false) }
    }

    // 🆕 Dialog'dan premium sheet'e geçiş
    private fun showPremiumFromLimitDialog() {
        viewModelScope.launch {
            // 1. Önce dialog'u kapat
            _uiState.update { it.copy(showDailyLimitDialog = false) }

            // 2. Recomposition için bekle
            delay(150)

            // 3. Premium sheet'i aç
            _uiState.update { it.copy(showPremiumSheet = true) }

            DebugHelper.log("Transitioned from limit dialog to premium sheet")
        }
    }

    private fun startStudyNow() {
        viewModelScope.launch {
            DebugHelper.logWordSelection("User chose to start study now")

            // Dialog'u kapat
            _uiState.update { it.copy(showStartStudyDialog = false) }

            // Study session'ı hazırla
            prepareStudySession()

            // Study ekranına git
            _effect.emit(WordSelectionEffect.NavigateToStudy)
        }
    }

    private fun continueSelecting() {
        viewModelScope.launch {
            DebugHelper.logWordSelection("User chose to continue selecting")

            // Dialog'u kapat
            _uiState.update { it.copy(showStartStudyDialog = false) }

            // Bir sonraki karta geç
            moveToNextWord()
        }
    }

    private fun dismissStartStudyDialog() {
        _uiState.update { it.copy(showStartStudyDialog = false) }
    }

}

data class WordSelectionUiState(
    val isLoading: Boolean = false,
    val isProcessingSwipe: Boolean = false,
    val error: String? = null,
    val allWords: List<ConceptEntity> = emptyList(),
    val remainingWords: List<ConceptEntity> = emptyList(),
    val currentWord: ConceptEntity? = null,
    val currentWordIndex: Int = 0,
    val selectedCount: Int = 0,
    val hiddenCount: Int = 0,
    val totalWords: Int = 0,
    val processedWords: Int = 0,
    val todaySelectionCount: Int = 0,
    val isCompleted: Boolean = false,
    val showPremiumSheet: Boolean = false,
    val isPremium: Boolean = false,
    val studySessionPrepared: Boolean = false,
    val showDailyLimitDialog: Boolean = false,     // Günlük limit doldu
    val showNoWordsDialog: Boolean = false,         // Pakette kelime kalmadı
    val showStartStudyDialog: Boolean = false
) {
    val progress: Float
        get() = if (totalWords > 0) {
            processedWords.toFloat() / totalWords.toFloat()
        } else 0f

    val canUndo: Boolean
        get() = selectedCount > 0 || hiddenCount > 0
}

data class UndoAction(
    val conceptId: Int,
    val status: SelectionStatus
)

sealed interface WordSelectionEvent {
    data class SwipeRight(val conceptId: Int) : WordSelectionEvent
    data class SwipeLeft(val conceptId: Int) : WordSelectionEvent
    data object Undo : WordSelectionEvent
    data object SkipAll : WordSelectionEvent
    data object FinishSelection : WordSelectionEvent
    data object ShowPremium : WordSelectionEvent
    data object DismissPremium : WordSelectionEvent
    data object DismissDailyLimitDialog : WordSelectionEvent
    data object DismissNoWordsDialog : WordSelectionEvent
    data object ShowPremiumFromLimitDialog : WordSelectionEvent
    data object ReloadAfterPremium : WordSelectionEvent
    data object StartStudyNow : WordSelectionEvent
    data object ContinueSelecting : WordSelectionEvent
    data object DismissStartStudyDialog : WordSelectionEvent
}

sealed interface WordSelectionEffect {
    data object NavigateToStudy : WordSelectionEffect
    data object ShowCompletionMessage : WordSelectionEffect
    data object ShowUndoMessage : WordSelectionEffect
    data class ShowMessage(val message: String) : WordSelectionEffect
}