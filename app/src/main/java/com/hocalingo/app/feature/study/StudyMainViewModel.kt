package com.hocalingo.app.feature.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.SoundEffectManager
import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.feature.profile.ProfileRepository
import com.hocalingo.app.feature.profile.WordSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * StudyMainViewModel - Study Hub Screen ViewModel
 *
 * Package: feature/study/
 *
 * Responsibilities:
 * - Manage selected words preview & bottomsheet
 * - Handle study direction changes
 * - Provide navigation effects
 * - Sync with UserPreferencesManager
 *
 * Features moved from ProfileScreen:
 * - Selected words display
 * - Study direction toggle
 */
@HiltViewModel
class StudyMainViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val userPreferencesManager: UserPreferencesManager,
    private val soundEffectManager: SoundEffectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyMainUiState())
    val uiState: StateFlow<StudyMainUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<StudyMainEffect>()
    val effect: SharedFlow<StudyMainEffect> = _effect.asSharedFlow()

    // Pagination constants
    private companion object {
        const val WORDS_PER_PAGE = 20
    }

    init {
        loadStudyMainData()
        observeStudyDirection()
    }

    /**
     * Observe study direction changes from preferences
     * Reactive to changes made elsewhere in the app
     */
    private fun observeStudyDirection() {
        viewModelScope.launch {
            userPreferencesManager.getStudyDirection().collectLatest { direction ->
                _uiState.update { it.copy(studyDirection = direction) }
            }
        }
    }

    /**
     * Load initial data for StudyMainScreen
     */
    private fun loadStudyMainData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Load selected words preview (first 5)
                val wordsResult = profileRepository.getSelectedWordsPreview()
                val words = when (wordsResult) {
                    is Result.Success -> wordsResult.data
                    is Result.Error -> emptyList()
                }

                // Load total count
                val countResult = profileRepository.getTotalSelectedWordsCount()
                val totalCount = when (countResult) {
                    is Result.Success -> countResult.data
                    is Result.Error -> 0
                }

                // Load study direction
                val studyDirection = userPreferencesManager.getStudyDirection().first()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedWordsPreview = words,
                        totalWordsCount = totalCount,
                        studyDirection = studyDirection
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Veriler yüklenemedi"
                    )
                }
            }
        }
    }

    fun onEvent(event: StudyMainEvent) {
        when (event) {
            StudyMainEvent.StartStudy -> navigateToStudy()
            StudyMainEvent.AddWord -> navigateToAddWord()
            StudyMainEvent.Refresh -> loadStudyMainData()

            // BottomSheet Events
            StudyMainEvent.ShowWordsBottomSheet -> showWordsBottomSheet()
            StudyMainEvent.HideWordsBottomSheet -> hideWordsBottomSheet()
            StudyMainEvent.LoadMoreWords -> loadMoreWords()
            StudyMainEvent.RefreshAllWords -> refreshAllWords()

            // Study Direction
            is StudyMainEvent.UpdateStudyDirection -> updateStudyDirection(event.direction)
        }
    }

    // ========== NAVIGATION ==========

    private fun navigateToStudy() {
        // ✅ Play click sound
        soundEffectManager.playClickSound()

        viewModelScope.launch {
            _effect.emit(StudyMainEffect.NavigateToStudy)
        }
    }

    private fun navigateToAddWord() {
        // ✅ Play click sound
        soundEffectManager.playClickSound()

        viewModelScope.launch {
            _effect.emit(StudyMainEffect.NavigateToAddWord)
        }
    }

    // ========== STUDY DIRECTION ==========

    private fun updateStudyDirection(direction: StudyDirection) {
        viewModelScope.launch {
            userPreferencesManager.setStudyDirection(direction)
            _uiState.update { it.copy(studyDirection = direction) }
            _effect.emit(StudyMainEffect.ShowMessage("Çalışma yönü güncellendi"))
        }
    }

    // ========== BOTTOMSHEET MANAGEMENT ==========

    private fun showWordsBottomSheet() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    showWordsBottomSheet = true,
                    currentWordsPage = 0,
                    allSelectedWords = emptyList(),
                    hasMoreWords = true
                )
            }

            // Load first page
            loadWordsPage(0)
        }
    }

    private fun hideWordsBottomSheet() {
        _uiState.update {
            it.copy(
                showWordsBottomSheet = false,
                allSelectedWords = emptyList(),
                wordsLoadingError = null,
                isLoadingAllWords = false,
                currentWordsPage = 0
            )
        }
    }

    private fun loadMoreWords() {
        val currentState = _uiState.value
        if (currentState.canLoadMoreWords) {
            val nextPage = currentState.currentWordsPage + 1
            loadWordsPage(nextPage)
        }
    }

    private fun refreshAllWords() {
        _uiState.update {
            it.copy(
                allSelectedWords = emptyList(),
                currentWordsPage = 0,
                hasMoreWords = true,
                wordsLoadingError = null
            )
        }
        loadWordsPage(0)
    }

    private fun loadWordsPage(page: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingAllWords = true, wordsLoadingError = null) }

            val offset = page * WORDS_PER_PAGE
            when (val result = profileRepository.getSelectedWords(offset, WORDS_PER_PAGE)) {
                is Result.Success -> {
                    val newWords = result.data
                    val hasMore = newWords.size == WORDS_PER_PAGE

                    _uiState.update { currentState ->
                        val updatedWords = if (page == 0) {
                            newWords // First page
                        } else {
                            currentState.allSelectedWords + newWords // Append
                        }

                        currentState.copy(
                            allSelectedWords = updatedWords,
                            isLoadingAllWords = false,
                            currentWordsPage = page,
                            hasMoreWords = hasMore,
                            wordsLoadingError = null
                        )
                    }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingAllWords = false,
                            wordsLoadingError = "Kelimeler yüklenemedi",
                            hasMoreWords = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * StudyMainUiState - UI State for StudyMainScreen
 */
data class StudyMainUiState(
    val isLoading: Boolean = false,
    val error: String? = null,

    // Selected words preview (5 words max)
    val selectedWordsPreview: List<WordSummary> = emptyList(),
    val totalWordsCount: Int = 0,

    // BottomSheet state for all selected words
    val showWordsBottomSheet: Boolean = false,
    val allSelectedWords: List<WordSummary> = emptyList(),
    val isLoadingAllWords: Boolean = false,
    val wordsLoadingError: String? = null,
    val currentWordsPage: Int = 0,
    val hasMoreWords: Boolean = true,

    // Study direction
    val studyDirection: StudyDirection = StudyDirection.EN_TO_TR
) {
    /**
     * Check if we can load more words
     */
    val canLoadMoreWords: Boolean get() = hasMoreWords && !isLoadingAllWords

    /**
     * Study direction display text
     */
    val studyDirectionText: String get() = when (studyDirection) {
        StudyDirection.EN_TO_TR -> "İngilizce → Türkçe"
        StudyDirection.TR_TO_EN -> "Türkçe → İngilizce"
        StudyDirection.MIXED -> "Karışık"
    }
}

/**
 * StudyMainEvent - User actions on StudyMainScreen
 */
sealed interface StudyMainEvent {
    // Navigation
    data object StartStudy : StudyMainEvent
    data object AddWord : StudyMainEvent
    data object Refresh : StudyMainEvent

    // BottomSheet
    data object ShowWordsBottomSheet : StudyMainEvent
    data object HideWordsBottomSheet : StudyMainEvent
    data object LoadMoreWords : StudyMainEvent
    data object RefreshAllWords : StudyMainEvent

    // Study Direction
    data class UpdateStudyDirection(val direction: StudyDirection) : StudyMainEvent
}

/**
 * StudyMainEffect - One-time side effects
 */
sealed interface StudyMainEffect {
    // Navigation
    data object NavigateToStudy : StudyMainEffect
    data object NavigateToAddWord : StudyMainEffect

    // User Feedback
    data class ShowMessage(val message: String) : StudyMainEffect
}