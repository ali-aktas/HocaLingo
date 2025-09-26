package com.hocalingo.app.feature.selection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.database.HocaLingoDatabase
import com.hocalingo.app.database.entities.ConceptEntity
import com.hocalingo.app.database.entities.SelectionStatus
import com.hocalingo.app.database.entities.StudyDirection
import com.hocalingo.app.database.entities.WordProgressEntity
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
 * WordSelectionViewModel - FIXED VERSION
 *
 * Fixed Issues:
 * 1. Removed StudySessionPreparer dependency (moved logic inline)
 * 2. Fixed Result.Success types
 * 3. Proper SavedStateHandle usage
 */
@HiltViewModel
class WordSelectionViewModel @Inject constructor(
    private val database: HocaLingoDatabase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // CRITICAL FIX: Proper SavedStateHandle parameter extraction
    private val packageId: String = savedStateHandle.get<String>("packageId")
        ?: run {
            DebugHelper.logWordSelection("Package ID not found in SavedStateHandle - using fallback")
            "a1_en_tr_test_v1" // Fallback to test package
        }

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
        DebugHelper.logWordSelection("Package ID (FIXED): $packageId")
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
            DebugHelper.logWordSelection("=== KELIME YÜKLEME BAŞLIYOR (FIXED) ===")
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // 1. Package validation - CRITICAL CHECK
                val packageInfo = database.wordPackageDao().getPackageById(packageId)
                DebugHelper.logWordSelection("Package info: $packageInfo")

                if (packageInfo == null) {
                    DebugHelper.logError("CRITICAL: Package not found in database: $packageId", null)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Paket veritabanında bulunamadı: $packageId"
                        )
                    }
                    return@launch
                }

                // 2. Load concepts for this package
                val allConcepts = database.conceptDao().getConceptsByPackage(packageId)
                DebugHelper.logWordSelection("Toplam ${allConcepts.size} kelime bulundu (packageId: $packageId)")

                if (allConcepts.isEmpty()) {
                    DebugHelper.logError("No concepts found for package: $packageId", null)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Bu pakette kelime bulunamadı: $packageId"
                        )
                    }
                    return@launch
                }

                // Debug: Log sample concepts
                allConcepts.take(3).forEach { concept ->
                    DebugHelper.logWordSelection("Sample: ${concept.id} - ${concept.english} -> ${concept.turkish}")
                }

                // 3. Check previously processed words
                val selectedSelections = database.userSelectionDao()
                    .getSelectionsByStatus(SelectionStatus.SELECTED)
                val hiddenSelections = database.userSelectionDao()
                    .getSelectionsByStatus(SelectionStatus.HIDDEN)

                val processedIds = (selectedSelections + hiddenSelections).map { it.conceptId }.toSet()
                DebugHelper.logWordSelection("Previously processed: ${processedIds.size} (Selected: ${selectedSelections.size}, Hidden: ${hiddenSelections.size})")

                // 4. Filter unprocessed words
                val unseenWords = allConcepts.filter { it.id !in processedIds }
                DebugHelper.logWordSelection("Unprocessed words: ${unseenWords.size}")

                // 5. Update state with loaded data
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

                // 6. Set current word
                if (unseenWords.isNotEmpty()) {
                    val firstWord = unseenWords.first()
                    _uiState.update { it.copy(currentWord = firstWord) }
                    DebugHelper.logWordSelection("First word set: ${firstWord.english} -> ${firstWord.turkish}")
                } else {
                    DebugHelper.logWordSelection("All words processed - showing completion")
                    _uiState.update {
                        it.copy(
                            isCompleted = true,
                            currentWord = null
                        )
                    }

                    // CRITICAL: Prepare study session when all words are processed
                    prepareStudySessionInline()
                }

                DebugHelper.logWordSelection("Word loading completed successfully")

            } catch (e: Exception) {
                DebugHelper.logError("Word loading CRITICAL ERROR", e)
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
                val todaySelections = database.userSelectionDao()
                    .getSelectionCountByStatus(SelectionStatus.SELECTED)

                DebugHelper.logWordSelection("Today selections: $todaySelections")

                _uiState.update {
                    it.copy(todaySelectionCount = todaySelections)
                }
            } catch (e: Exception) {
                DebugHelper.logError("Today selection count error", e)
            }
        }
    }

    private fun selectWord(conceptId: Int) {
        viewModelScope.launch {
            DebugHelper.logWordSelection("Selecting word: $conceptId")

            val currentState = _uiState.value

            // Premium limit check
            if (currentState.todaySelectionCount >= DAILY_SELECTION_LIMIT && !currentState.isPremium) {
                DebugHelper.logWordSelection("Daily limit reached")
                _uiState.update { it.copy(showPremiumSheet = true) }
                return@launch
            }

            try {
                // 1. Select word in database (UserSelectionEntity)
                database.userSelectionDao().selectWord(conceptId, packageId)
                DebugHelper.logWordSelection("Word selected in database: $conceptId")

                // 2. CRITICAL FIX: Create WordProgressEntity for immediate study availability
                createWordProgressForStudy(conceptId)

                // 3. Add to undo stack
                addToUndoStack(UndoAction(conceptId, SelectionStatus.SELECTED))

                // 4. Update state
                _uiState.update {
                    it.copy(
                        selectedCount = it.selectedCount + 1,
                        todaySelectionCount = it.todaySelectionCount + 1
                    )
                }

                DebugHelper.logWordSelection("State updated - Selected: ${currentState.selectedCount + 1}")

                // 5. Move to next word
                moveToNextWord()

            } catch (e: Exception) {
                DebugHelper.logError("Word selection error", e)
            }
        }
    }

    /**
     * HYBRID: Create WordProgressEntity when word is selected
     * New words start in learning phase with proper session positioning
     */
    private suspend fun createWordProgressForStudy(conceptId: Int) {
        try {
            // Check if WordProgressEntity already exists for both directions
            val progressEnToTr = database.wordProgressDao()
                .getProgressByConceptAndDirection(conceptId, StudyDirection.EN_TO_TR)
            val progressTrToEn = database.wordProgressDao()
                .getProgressByConceptAndDirection(conceptId, StudyDirection.TR_TO_EN)

            val currentTime = System.currentTimeMillis()
            val newProgressEntries = mutableListOf<WordProgressEntity>()

            // Get next session positions for both directions
            val maxPosEnToTr = database.combinedDataDao().getMaxSessionPosition(StudyDirection.EN_TO_TR)
            val maxPosTrToEn = database.combinedDataDao().getMaxSessionPosition(StudyDirection.TR_TO_EN)

            // Create EN_TO_TR progress if not exists
            if (progressEnToTr == null) {
                val enToTrProgress = WordProgressEntity(
                    conceptId = conceptId,
                    direction = StudyDirection.EN_TO_TR,
                    repetitions = 0,
                    intervalDays = 0f,
                    easeFactor = 2.5f, // SM-2 default
                    nextReviewAt = currentTime, // Available immediately for study
                    lastReviewAt = null,
                    isSelected = true,
                    isMastered = false,
                    learningPhase = true, // HYBRID: Start in learning phase
                    sessionPosition = maxPosEnToTr + 1, // HYBRID: Position in session queue
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
                newProgressEntries.add(enToTrProgress)
            }

            // Create TR_TO_EN progress if not exists
            if (progressTrToEn == null) {
                val trToEnProgress = WordProgressEntity(
                    conceptId = conceptId,
                    direction = StudyDirection.TR_TO_EN,
                    repetitions = 0,
                    intervalDays = 0f,
                    easeFactor = 2.5f, // SM-2 default
                    nextReviewAt = currentTime, // Available immediately for study
                    lastReviewAt = null,
                    isSelected = true,
                    isMastered = false,
                    learningPhase = true, // HYBRID: Start in learning phase
                    sessionPosition = maxPosTrToEn + 1, // HYBRID: Position in session queue
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
                newProgressEntries.add(trToEnProgress)
            }

            // Insert new progress entries
            if (newProgressEntries.isNotEmpty()) {
                database.wordProgressDao().insertProgressList(newProgressEntries)
                DebugHelper.logWordSelection("HYBRID: WordProgressEntity created for concept $conceptId (${newProgressEntries.size} directions) in learning phase")
            } else {
                DebugHelper.logWordSelection("WordProgressEntity already exists for concept $conceptId")
            }

        } catch (e: Exception) {
            DebugHelper.logError("WordProgressEntity creation error for concept $conceptId", e)
        }
    }

    private fun hideWord(conceptId: Int) {
        viewModelScope.launch {
            DebugHelper.logWordSelection("Hiding word: $conceptId")

            try {
                // Hide word in database
                database.userSelectionDao().hideWord(conceptId, packageId)
                DebugHelper.logWordSelection("Word hidden in database: $conceptId")

                // Add to undo stack
                addToUndoStack(UndoAction(conceptId, SelectionStatus.HIDDEN))

                // Update state
                _uiState.update {
                    it.copy(hiddenCount = it.hiddenCount + 1)
                }

                DebugHelper.logWordSelection("State updated - Hidden: ${_uiState.value.hiddenCount}")

                // Move to next word
                moveToNextWord()

            } catch (e: Exception) {
                DebugHelper.logError("Word hiding error", e)
            }
        }
    }

    private fun moveToNextWord() {
        val currentState = _uiState.value
        val nextIndex = currentState.currentWordIndex + 1

        DebugHelper.logWordSelection("Moving to next word - Index: $nextIndex / ${currentState.remainingWords.size}")

        if (nextIndex < currentState.remainingWords.size) {
            val nextWord = currentState.remainingWords[nextIndex]
            _uiState.update {
                it.copy(
                    currentWordIndex = nextIndex,
                    currentWord = nextWord
                )
            }
            DebugHelper.logWordSelection("Next word: ${nextWord.english} -> ${nextWord.turkish}")
        } else {
            // All words processed - CRITICAL: Prepare study session
            DebugHelper.logWordSelection("ALL WORDS PROCESSED!")
            _uiState.update {
                it.copy(
                    isCompleted = true,
                    currentWord = null
                )
            }

            viewModelScope.launch {
                _effect.emit(WordSelectionEffect.ShowCompletionMessage)
                // CRITICAL: Prepare study session
                prepareStudySessionInline()
            }
        }
    }

    private fun performUndo() {
        if (undoStack.isEmpty()) {
            DebugHelper.logWordSelection("Undo stack is empty")
            return
        }

        viewModelScope.launch {
            try {
                val lastAction = undoStack.pop()
                DebugHelper.logWordSelection("Performing undo: ${lastAction.conceptId} - ${lastAction.status}")

                // Remove selection from database
                database.userSelectionDao().deleteSelectionByConceptId(lastAction.conceptId)

                // Update state based on action type
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

                // Move back to previous word
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
                    DebugHelper.logWordSelection("Moved back to: ${prevWord.english}")
                }

                _effect.emit(WordSelectionEffect.ShowUndoMessage)

            } catch (e: Exception) {
                DebugHelper.logError("Undo error", e)
            }
        }
    }

    private fun addToUndoStack(action: UndoAction) {
        if (undoStack.size >= MAX_UNDO_SIZE) {
            undoStack.removeAt(0) // Remove oldest
        }
        undoStack.push(action)
        DebugHelper.logWordSelection("Added to undo stack: ${action.conceptId}")
    }

    private fun skipAllWords() {
        viewModelScope.launch {
            DebugHelper.logWordSelection("Skipping all words")
            prepareStudySessionInline()
            _effect.emit(WordSelectionEffect.NavigateToStudy)
        }
    }

    private fun finishSelection() {
        viewModelScope.launch {
            val selectedCount = _uiState.value.selectedCount
            DebugHelper.logWordSelection("Finishing selection - Selected: $selectedCount")

            if (selectedCount == 0) {
                _effect.emit(
                    WordSelectionEffect.ShowMessage(
                        "Çalışmak için en az 1 kelime seçmelisiniz"
                    )
                )
            } else {
                prepareStudySessionInline()
                _effect.emit(WordSelectionEffect.NavigateToStudy)
            }
        }
    }

    /**
     * CRITICAL: Prepare study session inline (without external dependency)
     * Creates WordProgressEntity records for selected words
     */
    private suspend fun prepareStudySessionInline() {
        DebugHelper.logWordSelection("=== PREPARING STUDY SESSION INLINE ===")

        try {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Get selected words
            val selectedSelections = database.userSelectionDao()
                .getSelectionsByStatus(SelectionStatus.SELECTED)

            if (selectedSelections.isEmpty()) {
                DebugHelper.logWordSelection("No selected words found")
                _uiState.update { it.copy(isLoading = false) }
                return
            }

            // 2. Create WordProgressEntity records for selected words
            val currentTime = System.currentTimeMillis()
            val newProgressEntries = mutableListOf<WordProgressEntity>()

            for (selection in selectedSelections) {
                val existingProgress = database.wordProgressDao()
                    .getProgressByConceptAndDirection(selection.conceptId, StudyDirection.EN_TO_TR)

                if (existingProgress == null) {
                    // Create new progress entry with SM-2 initial values
                    val progressEntity = WordProgressEntity(
                        conceptId = selection.conceptId,
                        direction = StudyDirection.EN_TO_TR,
                        repetitions = 0,
                        intervalDays = 1f, // Initial interval: 1 day
                        easeFactor = 2.5f, // SM-2 default ease factor
                        nextReviewAt = currentTime, // Available immediately
                        lastReviewAt = null,
                        isSelected = true,
                        isMastered = false,
                        createdAt = currentTime,
                        updatedAt = currentTime
                    )

                    newProgressEntries.add(progressEntity)
                }
            }

            // 3. Insert new progress entries
            if (newProgressEntries.isNotEmpty()) {
                database.wordProgressDao().insertProgressList(newProgressEntries)
                DebugHelper.logWordSelection("${newProgressEntries.size} new progress entries created")
            }

            DebugHelper.logWordSelection("Study session prepared successfully")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    studySessionPrepared = true
                )
            }

        } catch (e: Exception) {
            DebugHelper.logError("Study session preparation error", e)
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Study hazırlanırken hata: ${e.message}"
                )
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

// UI State with new field for study session preparation
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
    val isPremium: Boolean = false,
    val studySessionPrepared: Boolean = false
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

// Events - No changes needed
sealed interface WordSelectionEvent {
    data class SwipeRight(val conceptId: Int) : WordSelectionEvent
    data class SwipeLeft(val conceptId: Int) : WordSelectionEvent
    data object Undo : WordSelectionEvent
    data object SkipAll : WordSelectionEvent
    data object FinishSelection : WordSelectionEvent
    data object ShowPremium : WordSelectionEvent
    data object DismissPremium : WordSelectionEvent
}

// Effects - No changes needed
sealed interface WordSelectionEffect {
    data object NavigateToStudy : WordSelectionEffect
    data object ShowCompletionMessage : WordSelectionEffect
    data object ShowUndoMessage : WordSelectionEffect
    data class ShowMessage(val message: String) : WordSelectionEffect
}