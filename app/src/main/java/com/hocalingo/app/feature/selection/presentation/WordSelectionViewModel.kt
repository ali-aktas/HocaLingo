package com.hocalingo.app.feature.selection.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.DebugHelper
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

/**
 * WordSelectionViewModel - DÜZELTME
 * Extensive debug logging ve state management düzeltmeleri
 */
@HiltViewModel
class WordSelectionViewModel @Inject constructor(
    private val database: HocaLingoDatabase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // DÜZELTME: Package ID route parametresinden doğru al
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
    private val DAILY_SELECTION_LIMIT_PREMIUM = 100

    init {
        DebugHelper.logWordSelection("=== WordSelectionViewModel BAŞLATILIYOR ===")
        DebugHelper.logWordSelection("Package ID: $packageId")
        loadWords()
        loadTodaySelectionCount()
    }

    fun onEvent(event: WordSelectionEvent) {
        DebugHelper.logWordSelection("Event received: $event")
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
            DebugHelper.logWordSelection("=== KELIME YÜKLEME BAŞLIYOR ===")
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // 1. Package'ın var olup olmadığını kontrol et
                val packageInfo = database.wordPackageDao().getPackageById(packageId)
                DebugHelper.logWordSelection("Package info: $packageInfo")

                if (packageInfo == null) {
                    DebugHelper.logWordSelection("ERROR: Package not found in database!")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Paket veritabanında bulunamadı: $packageId"
                        )
                    }
                    return@launch
                }

                // 2. Package'a ait tüm kelimeleri getir
                val allConcepts = database.conceptDao().getConceptsByPackage(packageId)
                DebugHelper.logWordSelection("Toplam ${allConcepts.size} kelime bulundu")

                // Debug: İlk 3 kelimeyi logla
                allConcepts.take(3).forEach { concept ->
                    DebugHelper.logWordSelection("Sample: ${concept.id} - ${concept.english} -> ${concept.turkish}")
                }

                if (allConcepts.isEmpty()) {
                    DebugHelper.logWordSelection("ERROR: No concepts found for package!")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Bu pakette kelime bulunamadı"
                        )
                    }
                    return@launch
                }

                // 3. Daha önce seçilmiş kelimeleri kontrol et
                val selectedSelections = database.userSelectionDao()
                    .getSelectionsByStatus(SelectionStatus.SELECTED)
                val hiddenSelections = database.userSelectionDao()
                    .getSelectionsByStatus(SelectionStatus.HIDDEN)

                val processedIds = (selectedSelections + hiddenSelections).map { it.conceptId }.toSet()
                DebugHelper.logWordSelection("Daha önce işlenmiş kelime sayısı: ${processedIds.size}")
                DebugHelper.logWordSelection("Seçili: ${selectedSelections.size}, Gizli: ${hiddenSelections.size}")

                // 4. Henüz işlenmemiş kelimeleri filtrele
                val unseenWords = allConcepts.filter { it.id !in processedIds }
                DebugHelper.logWordSelection("İşlenmemiş kelime sayısı: ${unseenWords.size}")

                // 5. State'i güncelle
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        allWords = allConcepts,
                        remainingWords = unseenWords,
                        currentWordIndex = 0,
                        totalWords = allConcepts.size,
                        processedWords = processedIds.size,
                        selectedCount = selectedSelections.size,
                        hiddenCount = hiddenSelections.size,
                        error = null
                    )
                }

                // 6. İlk kelimeyi set et
                if (unseenWords.isNotEmpty()) {
                    val firstWord = unseenWords.first()
                    _uiState.update { it.copy(currentWord = firstWord) }
                    DebugHelper.logWordSelection("İlk kelime set edildi: ${firstWord.english} -> ${firstWord.turkish}")
                } else {
                    DebugHelper.logWordSelection("Tüm kelimeler zaten işlenmiş - completion state")
                    _uiState.update {
                        it.copy(
                            isCompleted = true,
                            currentWord = null
                        )
                    }
                }

                DebugHelper.logWordSelection("=== KELIME YÜKLEME TAMAMLANDI ===")

            } catch (e: Exception) {
                DebugHelper.logError("Kelime yükleme HATASI", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Kelimeler yüklenirken hata oluştu: ${e.message}"
                    )
                }
            }
        }
    }

    private fun loadTodaySelectionCount() {
        viewModelScope.launch {
            try {
                // Bugünkü seçim sayısını hesapla
                val todaySelections = database.userSelectionDao()
                    .getSelectionCountByStatus(SelectionStatus.SELECTED)

                DebugHelper.logWordSelection("Bugün seçilen kelime sayısı: $todaySelections")

                _uiState.update {
                    it.copy(todaySelectionCount = todaySelections)
                }
            } catch (e: Exception) {
                DebugHelper.logError("Today selection count HATASI", e)
            }
        }
    }

    private fun selectWord(conceptId: Int) {
        viewModelScope.launch {
            DebugHelper.logWordSelection("Kelime seçiliyor: $conceptId")

            val currentState = _uiState.value

            // Limit kontrolü
            if (currentState.todaySelectionCount >= DAILY_SELECTION_LIMIT && !currentState.isPremium) {
                DebugHelper.logWordSelection("Günlük limit aşıldı")
                _uiState.update { it.copy(showPremiumSheet = true) }
                return@launch
            }

            try {
                // Kelimeyi seç
                database.userSelectionDao().selectWord(conceptId, packageId)
                DebugHelper.logWordSelection("Kelime veritabanında seçildi: $conceptId")

                // Undo stack'e ekle
                addToUndoStack(UndoAction(conceptId, SelectionStatus.SELECTED))

                // State güncelle
                _uiState.update {
                    it.copy(
                        selectedCount = it.selectedCount + 1,
                        todaySelectionCount = it.todaySelectionCount + 1
                    )
                }

                DebugHelper.logWordSelection("State güncellendi - Seçili: ${currentState.selectedCount + 1}")

                // Sonraki kelimeye geç
                moveToNextWord()

            } catch (e: Exception) {
                DebugHelper.logError("Kelime seçme HATASI", e)
            }
        }
    }

    private fun hideWord(conceptId: Int) {
        viewModelScope.launch {
            DebugHelper.logWordSelection("Kelime gizleniyor: $conceptId")

            try {
                // Kelimeyi gizle
                database.userSelectionDao().hideWord(conceptId, packageId)
                DebugHelper.logWordSelection("Kelime veritabanında gizlendi: $conceptId")

                // Undo stack'e ekle
                addToUndoStack(UndoAction(conceptId, SelectionStatus.HIDDEN))

                // State güncelle
                _uiState.update {
                    it.copy(hiddenCount = it.hiddenCount + 1)
                }

                DebugHelper.logWordSelection("State güncellendi - Gizli: ${_uiState.value.hiddenCount}")

                // Sonraki kelimeye geç
                moveToNextWord()

            } catch (e: Exception) {
                DebugHelper.logError("Kelime gizleme HATASI", e)
            }
        }
    }

    private fun moveToNextWord() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentWordIndex + 1

        DebugHelper.logWordSelection("Sonraki kelimeye geçiliyor - Index: $nextIndex / ${currentState.remainingWords.size}")

        if (nextIndex < currentState.remainingWords.size) {
            val nextWord = currentState.remainingWords[nextIndex]
            _uiState.update {
                it.copy(
                    currentWordIndex = nextIndex,
                    currentWord = nextWord
                )
            }
            DebugHelper.logWordSelection("Sonraki kelime: ${nextWord.english} -> ${nextWord.turkish}")
        } else {
            // Tüm kelimeler işlendi
            DebugHelper.logWordSelection("TÜM KELİMELER İŞLENDİ!")
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
        if (undoStack.isEmpty()) {
            DebugHelper.logWordSelection("Undo stack boş")
            return
        }

        viewModelScope.launch {
            try {
                val lastAction = undoStack.pop()
                DebugHelper.logWordSelection("Undo yapılıyor: ${lastAction.conceptId} - ${lastAction.status}")

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
                    val prevWord = currentState.remainingWords[prevIndex]
                    _uiState.update {
                        it.copy(
                            currentWordIndex = prevIndex,
                            currentWord = prevWord,
                            isCompleted = false
                        )
                    }
                    DebugHelper.logWordSelection("Önceki kelimeye dönüldü: ${prevWord.english}")
                }

                _effect.emit(WordSelectionEffect.ShowUndoMessage)

            } catch (e: Exception) {
                DebugHelper.logError("Undo HATASI", e)
            }
        }
    }

    private fun addToUndoStack(action: UndoAction) {
        if (undoStack.size >= MAX_UNDO_SIZE) {
            undoStack.removeAt(0) // En eski işlemi sil
        }
        undoStack.push(action)
        DebugHelper.logWordSelection("Undo stack'e eklendi: ${action.conceptId}")
    }

    private fun skipAllWords() {
        viewModelScope.launch {
            DebugHelper.logWordSelection("Tüm kelimeler atlanıyor")
            _effect.emit(WordSelectionEffect.NavigateToStudy)
        }
    }

    private fun finishSelection() {
        viewModelScope.launch {
            val selectedCount = _uiState.value.selectedCount
            DebugHelper.logWordSelection("Seçim bitiriliyor - Seçili kelime sayısı: $selectedCount")

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

// UI State - NO CHANGES NEEDED
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