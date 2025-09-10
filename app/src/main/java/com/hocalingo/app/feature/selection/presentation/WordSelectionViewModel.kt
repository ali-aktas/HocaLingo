package com.hocalingo.app.feature.selection.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.database.HocaLingoDatabase
import com.hocalingo.app.core.database.entities.ConceptEntity
import com.hocalingo.app.core.database.entities.SelectionStatus
import com.hocalingo.app.core.database.entities.UserSelectionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
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

@HiltViewModel
class WordSelectionViewModel @Inject constructor(
    private val database: HocaLingoDatabase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Package ID from navigation args or default
    private val packageId: String = savedStateHandle.get<String>("packageId") ?: "a1_en_tr_test_v1"

    private val _uiState = MutableStateFlow(WordSelectionUiState())
    val uiState: StateFlow<WordSelectionUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<WordSelectionEffect>()
    val effect: SharedFlow<WordSelectionEffect> = _effect.asSharedFlow()

    // Undo stack for last 5 actions
    private val undoStack = Stack<UndoAction>()
    private val MAX_UNDO_SIZE = 5

    // Daily selection limit
    private val DAILY_SELECTION_LIMIT = 25
    private val DAILY_SELECTION_LIMIT_PREMIUM = 100 // Premium için

    init {
        loadWords()
        loadTodaySelectionCount()
    }

    fun onEvent(event: WordSelectionEvent) {
        when (event) {
            is WordSelectionEvent.SwipeRight -> selectWord(event.conceptId)
            is WordSelectionEvent.SwipeLeft -> hideWord(event.conceptId)
            WordSelectionEvent.Undo -> performUndo()
            WordSelectionEvent.SkipAll -> skipAllWords()
            WordSelectionEvent.FinishSelection -> finishSelection()
            WordSelectionEvent.ShowPremium -> showPremiumBottomSheet()
            WordSelectionEvent.DismissPremium -> dismissPremiumBottomSheet()
        }
    }

    private fun loadWords() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Package'a ait kelimeleri getir
                val concepts = database.conceptDao().getConceptsByPackage(packageId)

                // Daha önce seçilmiş/gizlenmiş kelimeleri filtrele
                val selections = database.userSelectionDao()
                    .getSelectionsByStatus(SelectionStatus.SELECTED) +
                        database.userSelectionDao()
                            .getSelectionsByStatus(SelectionStatus.HIDDEN)

                val selectedIds = selections.map { it.conceptId }.toSet()

                // Henüz seçilmemiş kelimeleri al
                val unseenWords = concepts.filter { it.id !in selectedIds }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allWords = unseenWords,
                        remainingWords = unseenWords,
                        currentWordIndex = 0,
                        totalWords = concepts.size,
                        processedWords = selectedIds.size
                    )
                }

                // İlk kelimeyi göster
                if (unseenWords.isNotEmpty()) {
                    _uiState.update { it.copy(currentWord = unseenWords.first()) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Kelimeler yüklenirken hata oluştu"
                    )
                }
            }
        }
    }

    private fun loadTodaySelectionCount() {
        viewModelScope.launch {
            // Bugünkü seçim sayısını hesapla (gerçek implementasyonda tarih bazlı olacak)
            val todaySelections = database.userSelectionDao()
                .getSelectionCountByStatus(SelectionStatus.SELECTED)

            _uiState.update {
                it.copy(todaySelectionCount = todaySelections)
            }
        }
    }

    private fun selectWord(conceptId: Int) {
        viewModelScope.launch {
            val currentState = _uiState.value

            // Limit kontrolü
            if (currentState.todaySelectionCount >= DAILY_SELECTION_LIMIT && !currentState.isPremium) {
                _uiState.update { it.copy(showPremiumSheet = true) }
                return@launch
            }

            // Kelimeyi seç
            database.userSelectionDao().selectWord(conceptId, packageId)

            // Undo stack'e ekle
            addToUndoStack(UndoAction(conceptId, SelectionStatus.SELECTED))

            // State güncelle
            _uiState.update {
                it.copy(
                    selectedCount = it.selectedCount + 1,
                    todaySelectionCount = it.todaySelectionCount + 1
                )
            }

            // Sonraki kelimeye geç
            moveToNextWord()
        }
    }

    private fun hideWord(conceptId: Int) {
        viewModelScope.launch {
            // Kelimeyi gizle
            database.userSelectionDao().hideWord(conceptId, packageId)

            // Undo stack'e ekle
            addToUndoStack(UndoAction(conceptId, SelectionStatus.HIDDEN))

            // State güncelle
            _uiState.update {
                it.copy(hiddenCount = it.hiddenCount + 1)
            }

            // Sonraki kelimeye geç
            moveToNextWord()
        }
    }

    private fun moveToNextWord() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentWordIndex + 1

        if (nextIndex < currentState.remainingWords.size) {
            _uiState.update {
                it.copy(
                    currentWordIndex = nextIndex,
                    currentWord = it.remainingWords[nextIndex]
                )
            }
        } else {
            // Tüm kelimeler işlendi
            _uiState.update {
                it.copy(
                    isCompleted = true,
                    currentWord = null
                )
            }

            viewModelScope.launch {
                _effect.emit(WordSelectionEffect.ShowCompletionMessage)
            }
        }
    }

    private fun performUndo() {
        if (undoStack.isEmpty()) return

        viewModelScope.launch {
            val lastAction = undoStack.pop()

            // Veritabanından seçimi sil
            database.userSelectionDao().deleteSelectionByConceptId(lastAction.conceptId)

            // State'i güncelle
            when (lastAction.status) {
                SelectionStatus.SELECTED -> {
                    _uiState.update {
                        it.copy(
                            selectedCount = it.selectedCount - 1,
                            todaySelectionCount = it.todaySelectionCount - 1
                        )
                    }
                }
                SelectionStatus.HIDDEN -> {
                    _uiState.update {
                        it.copy(hiddenCount = it.hiddenCount - 1)
                    }
                }
                else -> {}
            }

            // Önceki kelimeye dön
            val currentState = _uiState.value
            if (currentState.currentWordIndex > 0) {
                val prevIndex = currentState.currentWordIndex - 1
                _uiState.update {
                    it.copy(
                        currentWordIndex = prevIndex,
                        currentWord = it.remainingWords[prevIndex],
                        isCompleted = false
                    )
                }
            }

            _effect.emit(WordSelectionEffect.ShowUndoMessage)
        }
    }

    private fun addToUndoStack(action: UndoAction) {
        if (undoStack.size >= MAX_UNDO_SIZE) {
            undoStack.removeAt(0) // En eski işlemi sil
        }
        undoStack.push(action)
    }

    private fun skipAllWords() {
        viewModelScope.launch {
            _effect.emit(WordSelectionEffect.NavigateToStudy)
        }
    }

    private fun finishSelection() {
        viewModelScope.launch {
            val selectedCount = _uiState.value.selectedCount

            if (selectedCount == 0) {
                _effect.emit(
                    WordSelectionEffect.ShowMessage(
                        "Çalışmak için en az 1 kelime seçmelisiniz"
                    )
                )
            } else {
                _effect.emit(WordSelectionEffect.NavigateToStudy)
            }
        }
    }

    private fun showPremiumBottomSheet() {
        _uiState.update { it.copy(showPremiumSheet = true) }
    }

    private fun dismissPremiumBottomSheet() {
        _uiState.update { it.copy(showPremiumSheet = false) }
    }
}

// UI State
data class WordSelectionUiState(
    val isLoading: Boolean = false,
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
    val isPremium: Boolean = false
) {
    val progress: Float
        get() = if (totalWords > 0) {
            (processedWords + currentWordIndex).toFloat() / totalWords.toFloat()
        } else 0f

    val canUndo: Boolean
        get() = selectedCount > 0 || hiddenCount > 0
}

// Undo Action Model
data class UndoAction(
    val conceptId: Int,
    val status: SelectionStatus
)

// Events
sealed interface WordSelectionEvent {
    data class SwipeRight(val conceptId: Int) : WordSelectionEvent
    data class SwipeLeft(val conceptId: Int) : WordSelectionEvent
    data object Undo : WordSelectionEvent
    data object SkipAll : WordSelectionEvent
    data object FinishSelection : WordSelectionEvent
    data object ShowPremium : WordSelectionEvent
    data object DismissPremium : WordSelectionEvent
}

// Effects
sealed interface WordSelectionEffect {
    data object NavigateToStudy : WordSelectionEffect
    data object ShowCompletionMessage : WordSelectionEffect
    data object ShowUndoMessage : WordSelectionEffect
    data class ShowMessage(val message: String) : WordSelectionEffect
}