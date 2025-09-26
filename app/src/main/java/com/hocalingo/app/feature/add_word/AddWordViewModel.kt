package com.hocalingo.app.feature.addword.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.database.HocaLingoDatabase
import com.hocalingo.app.database.entities.ConceptEntity
import com.hocalingo.app.database.entities.SelectionStatus
import com.hocalingo.app.database.entities.StudyDirection
import com.hocalingo.app.database.entities.UserSelectionEntity
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
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Add Word ViewModel - Real Implementation
 * Handles custom word creation with SM-2 integration
 */
@HiltViewModel
class AddWordViewModel @Inject constructor(
    private val database: HocaLingoDatabase,
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddWordUiState())
    val uiState: StateFlow<AddWordUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<AddWordEffect>()
    val effect: SharedFlow<AddWordEffect> = _effect.asSharedFlow()

    init {
        loadUserWordsCount()
    }

    fun onEvent(event: AddWordEvent) {
        when (event) {
            is AddWordEvent.EnglishWordChanged -> updateEnglishWord(event.value)
            is AddWordEvent.TurkishWordChanged -> updateTurkishWord(event.value)
            is AddWordEvent.EnglishExampleChanged -> updateEnglishExample(event.value)
            is AddWordEvent.TurkishExampleChanged -> updateTurkishExample(event.value)
            AddWordEvent.SubmitWord -> submitWord()
            AddWordEvent.ClearForm -> clearForm()
            AddWordEvent.DismissError -> dismissError()
            AddWordEvent.DismissSuccess -> dismissSuccess()
            AddWordEvent.NavigateBack -> navigateBack()
            AddWordEvent.NavigateToStudy -> navigateToStudy()
        }
    }

    private fun updateEnglishWord(value: String) {
        _uiState.update {
            it.copy(
                englishWord = value,
                englishWordError = null
            )
        }
        validateForm()
    }

    private fun updateTurkishWord(value: String) {
        _uiState.update {
            it.copy(
                turkishWord = value,
                turkishWordError = null
            )
        }
        validateForm()
    }

    private fun updateEnglishExample(value: String) {
        _uiState.update {
            it.copy(
                englishExample = value,
                englishExampleError = null
            )
        }
        validateForm()
    }

    private fun updateTurkishExample(value: String) {
        _uiState.update {
            it.copy(
                turkishExample = value,
                turkishExampleError = null
            )
        }
        validateForm()
    }

    private fun validateForm() {
        val currentState = _uiState.value
        val formData = WordFormData(
            english = currentState.englishWord.trim(),
            turkish = currentState.turkishWord.trim(),
            exampleEn = currentState.englishExample.trim().takeIf { it.isNotBlank() },
            exampleTr = currentState.turkishExample.trim().takeIf { it.isNotBlank() }
        )

        val validationResult = formData.validate()

        _uiState.update {
            it.copy(
                englishWordError = validationResult.getErrorForField(FormField.ENGLISH_WORD),
                turkishWordError = validationResult.getErrorForField(FormField.TURKISH_WORD),
                englishExampleError = validationResult.getErrorForField(FormField.ENGLISH_EXAMPLE),
                turkishExampleError = validationResult.getErrorForField(FormField.TURKISH_EXAMPLE),
                isFormValid = validationResult.isValid
            )
        }
    }

    private fun submitWord() {
        val currentState = _uiState.value

        if (!currentState.canSubmit) {
            DebugHelper.log("Form validation failed")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                val formData = WordFormData(
                    english = currentState.englishWord.trim(),
                    turkish = currentState.turkishWord.trim(),
                    exampleEn = currentState.englishExample.trim().takeIf { it.isNotBlank() },
                    exampleTr = currentState.turkishExample.trim().takeIf { it.isNotBlank() }
                )

                // Check for duplicates
                val existingConcepts = database.conceptDao().getUserAddedConcepts()
                val duplicateExists = existingConcepts.any {
                    it.english.equals(formData.english, ignoreCase = true) ||
                            it.turkish.equals(formData.turkish, ignoreCase = true)
                }

                if (duplicateExists) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Bu kelime zaten eklenmiş!"
                        )
                    }
                    return@launch
                }

                // Generate unique ID for user words
                val newConceptId = generateUserWordId()

                // Get user's current level for categorization
                val userLevel = preferencesManager.getCurrentLevel().first()

                // Create ConceptEntity
                val conceptEntity = ConceptEntity(
                    id = newConceptId,
                    english = formData.english,
                    turkish = formData.turkish,
                    exampleEn = formData.exampleEn,
                    exampleTr = formData.exampleTr,
                    pronunciation = null, // TTS will handle English pronunciation
                    level = "CUSTOM",
                    category = "user_added",
                    reversible = true,
                    userAdded = true, // ✅ Key field
                    packageId = "user_custom"
                )

                // Insert concept
                database.conceptDao().insertConcept(conceptEntity)
                DebugHelper.log("✅ Custom word created: ${conceptEntity.english} -> ${conceptEntity.turkish}")

                // Auto-select the word for study
                val userSelectionEntity = UserSelectionEntity(
                    conceptId = newConceptId,
                    status = SelectionStatus.SELECTED,
                    selectedAt = System.currentTimeMillis(),
                    packageLevel = "CUSTOM"
                )

                database.userSelectionDao().insertSelection(userSelectionEntity)
                DebugHelper.log("✅ Word auto-selected for study")

                // Create initial progress entities for both directions
                createInitialWordProgress(newConceptId)

                // Update UI state
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showSuccessAnimation = true,
                        userWordsCount = it.userWordsCount + 1
                    )
                }

                // Show success and navigate after delay
                _effect.emit(AddWordEffect.ShowMessage("Kelime başarıyla eklendi! 🎉"))

                // Auto-clear form after successful submission
                kotlinx.coroutines.delay(1500)
                clearForm()

            } catch (e: Exception) {
                DebugHelper.logError("Add word error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Kelime eklenirken hata oluştu: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Generate unique ID for user words (starting from 100000 to avoid conflicts)
     */
    private suspend fun generateUserWordId(): Int {
        val existingUserWords = database.conceptDao().getUserAddedConcepts()
        val maxUserWordId = existingUserWords.maxOfOrNull { it.id } ?: 99999
        return maxUserWordId + 1
    }

    /**
     * Create initial WordProgress entities for both study directions
     */
    private suspend fun createInitialWordProgress(conceptId: Int) {
        val currentTime = System.currentTimeMillis()

        // EN_TO_TR direction
        val progressEnToTr = WordProgressEntity(
            conceptId = conceptId,
            direction = StudyDirection.EN_TO_TR,
            repetitions = 0,
            intervalDays = 0f,
            easeFactor = 2.5f, // SM-2 default
            nextReviewAt = currentTime, // Available immediately for study
            lastReviewAt = null,
            isSelected = true,
            isMastered = false,
            learningPhase = true, // Start in learning phase
            sessionPosition = 1, // First position in session
            createdAt = currentTime,
            updatedAt = currentTime
        )

        // TR_TO_EN direction
        val progressTrToEn = WordProgressEntity(
            conceptId = conceptId,
            direction = StudyDirection.TR_TO_EN,
            repetitions = 0,
            intervalDays = 0f,
            easeFactor = 2.5f,
            nextReviewAt = currentTime,
            lastReviewAt = null,
            isSelected = true,
            isMastered = false,
            learningPhase = true,
            sessionPosition = 1,
            createdAt = currentTime,
            updatedAt = currentTime
        )

        // Insert both progress entities
        database.wordProgressDao().insertProgressList(listOf(progressEnToTr, progressTrToEn))
        DebugHelper.log("✅ Initial progress created for both directions")
    }

    private fun clearForm() {
        _uiState.update {
            AddWordUiState(userWordsCount = it.userWordsCount)
        }
        _effect.tryEmit(AddWordEffect.ClearFormFields)
    }

    private fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun dismissSuccess() {
        _uiState.update { it.copy(showSuccessAnimation = false) }
    }

    private fun navigateBack() {
        _effect.tryEmit(AddWordEffect.NavigateBack)
    }

    private fun navigateToStudy() {
        _effect.tryEmit(AddWordEffect.NavigateToStudy)
    }

    private fun loadUserWordsCount() {
        viewModelScope.launch {
            try {
                val userWords = database.conceptDao().getUserAddedConcepts()
                _uiState.update {
                    it.copy(userWordsCount = userWords.size)
                }
                DebugHelper.log("User words count loaded: ${userWords.size}")
            } catch (e: Exception) {
                DebugHelper.logError("Load user words count error", e)
            }
        }
    }
}