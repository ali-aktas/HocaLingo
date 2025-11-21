package com.hocalingo.app.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.feature.ai.models.GeneratedStory
import com.hocalingo.app.feature.ai.models.StoryDifficulty
import com.hocalingo.app.feature.ai.models.StoryLength
import com.hocalingo.app.feature.ai.models.StoryType
import com.hocalingo.app.feature.subscription.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AIViewModel - AI Story Feature ViewModel
 *
 * Package: feature/ai/
 *
 * Manages:
 * - Story generation with Gemini API
 * - Story history and favorites
 * - Daily quota tracking
 * - Premium status
 * - Creator dialog state
 * - History sheet state
 *
 * Architecture: MVVM + MVI (State + Events + Effects)
 */
@HiltViewModel
class AIViewModel @Inject constructor(
    private val generateStoryUseCase: GenerateStoryUseCase,
    private val getStoryHistoryUseCase: GetStoryHistoryUseCase,
    private val checkDailyQuotaUseCase: CheckDailyQuotaUseCase,
    private val storyRepository: StoryRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(AIUiState())
    val uiState: StateFlow<AIUiState> = _uiState.asStateFlow()

    // Side Effects (one-time events)
    private val _effect = MutableSharedFlow<AIEffect>()
    val effect: SharedFlow<AIEffect> = _effect.asSharedFlow()

    init {
        DebugHelper.log("🤖 AIViewModel initialized")
        loadStoryHistory()
        loadQuotaInfo()
        observePremiumStatus()
    }

    /**
     * ✅ Premium status tracking - Reactive
     * Updates isPremium when subscription changes
     */
    private fun observePremiumStatus() {
        viewModelScope.launch {
            subscriptionRepository.getLocalSubscriptionState().collect { state ->
                _uiState.update { it.copy(isPremium = state.isPremium) }
                DebugHelper.log("👑 Premium status: ${state.isPremium}")
            }
        }
    }

    /**
     * Handle all user events
     */
    fun onEvent(event: AIEvent) {
        DebugHelper.log("📥 Event: $event")
        when (event) {
            AIEvent.OpenCreatorDialog -> openCreatorDialog()
            AIEvent.CloseCreatorDialog -> closeCreatorDialog()
            is AIEvent.GenerateStory -> generateStory(
                topic = event.topic,
                type = event.type,
                difficulty = event.difficulty,
                length = event.length
            )
            AIEvent.ShowHistory -> showHistory()
            AIEvent.CloseHistory -> closeHistory()
            is AIEvent.OpenStoryDetail -> openStoryDetail(event.storyId)
            is AIEvent.DeleteStory -> deleteStory(event.storyId)
            is AIEvent.ToggleFavorite -> toggleFavorite(event.storyId)
            AIEvent.CloseStoryDetail -> closeStoryDetail()
            AIEvent.DismissError -> dismissError()
            AIEvent.RefreshQuota -> loadQuotaInfo()
        }
    }

    /**
     * Open story creator dialog
     */
    private fun openCreatorDialog() {
        _uiState.update { it.copy(showCreatorDialog = true) }
    }

    /**
     * Close story creator dialog
     */
    private fun closeCreatorDialog() {
        _uiState.update {
            it.copy(
                showCreatorDialog = false,
                isGenerating = false,
                generationError = null
            )
        }
    }

    /**
     * Generate story - Main business logic
     */
    private fun generateStory(
        topic: String?,
        type: StoryType,
        difficulty: StoryDifficulty,
        length: StoryLength
    ) {
        viewModelScope.launch {
            DebugHelper.log("🚀 Starting story generation...")
            DebugHelper.log("   Topic: ${topic ?: "None"}")
            DebugHelper.log("   Type: ${type.displayName}")
            DebugHelper.log("   Difficulty: ${difficulty.displayName}")
            DebugHelper.log("   Length: ${length.displayName}")

            // 1. Show generating state
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    generationError = null
                )
            }

            // 2. Call use case
            when (val result = generateStoryUseCase(topic, type, difficulty, length)) {
                is Result.Success -> {
                    DebugHelper.logSuccess("✅ Story generated successfully!")

                    // Update state
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            showCreatorDialog = false,
                            currentStory = result.data
                        )
                    }

                    // Send effects
                    _effect.emit(AIEffect.ShowSuccessAnimation)
                    _effect.emit(AIEffect.NavigateToDetail(result.data.id))
                    _effect.emit(AIEffect.ShowMessage("🎉 Hikaye oluşturuldu!"))

                    // Refresh quota
                    loadQuotaInfo()
                    loadStoryHistory()
                }

                is Result.Error -> {
                    DebugHelper.logError("❌ Story generation failed", result.error)

                    val errorMessage = when (result.error) {
                        is AppError.QuotaExceeded -> "Günlük hikaye limitine ulaştınız. Yarın tekrar deneyin!"
                        is AppError.NoWordsAvailable -> "Yeterli kelime bulunamadı. Daha fazla kelime öğrenin!"
                        is AppError.Timeout -> "İstek zaman aşımına uğradı. Lütfen tekrar deneyin."
                        is AppError.ApiError -> "API hatası: ${result.error.message}"
                        else -> "Bir hata oluştu. Lütfen tekrar deneyin."
                    }

                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            generationError = errorMessage
                        )
                    }

                    _effect.emit(AIEffect.ShowError(errorMessage))
                }
            }
        }
    }

    /**
     * Load story history - Flow based
     */
    private fun loadStoryHistory() {
        viewModelScope.launch {
            getStoryHistoryUseCase().collect { stories ->
                _uiState.update {
                    it.copy(
                        stories = stories,
                        isLoadingHistory = false
                    )
                }
                DebugHelper.log("📚 Loaded ${stories.size} stories")
            }
        }
    }

    /**
     * Load quota info
     */
    private fun loadQuotaInfo() {
        viewModelScope.launch {
            when (val result = checkDailyQuotaUseCase()) {
                is Result.Success -> {
                    val (used, total) = result.data
                    _uiState.update {
                        it.copy(
                            quotaUsed = used,
                            quotaTotal = total
                        )
                    }
                    DebugHelper.log("📊 Quota: $used/$total")
                }
                is Result.Error -> {
                    DebugHelper.logError("Failed to load quota", result.error)
                }
            }
        }
    }

    /**
     * Show history bottom sheet
     */
    private fun showHistory() {
        _uiState.update {
            it.copy(
                showHistorySheet = true,
                isLoadingHistory = true
            )
        }
        loadStoryHistory()
    }

    /**
     * Close history bottom sheet
     */
    private fun closeHistory() {
        _uiState.update { it.copy(showHistorySheet = false) }
    }

    /**
     * Open story detail
     */
    private fun openStoryDetail(storyId: String) {
        viewModelScope.launch {
            DebugHelper.log("📖 Opening story detail: $storyId")

            // 1. Story'yi yükle
            when (val storyResult = storyRepository.getStoryById(storyId)) {
                is Result.Success -> {
                    val story = storyResult.data
                    DebugHelper.log("✅ Story loaded: ${story.title}")

                    // 2. Story'deki concept ID'lerden İngilizce kelimeleri yükle
                    when (val wordsResult = storyRepository.getEnglishWordsForStory(story.usedWords)) {
                        is Result.Success -> {
                            val englishWords = wordsResult.data
                            DebugHelper.log("📚 Loaded ${englishWords.size} words for highlighting")

                            // State güncelle
                            _uiState.update {
                                it.copy(
                                    currentStory = story,
                                    selectedStoryWords = englishWords
                                )
                            }

                            // Navigate
                            _effect.emit(AIEffect.NavigateToDetail(storyId))
                        }
                        is Result.Error -> {
                            // Kelimeler yüklenemedi ama hikayeyi göster (graceful degradation)
                            DebugHelper.logError("⚠️ Failed to load words, showing story without highlights", wordsResult.error)

                            _uiState.update {
                                it.copy(
                                    currentStory = story,
                                    selectedStoryWords = emptyList()
                                )
                            }

                            _effect.emit(AIEffect.NavigateToDetail(storyId))
                        }
                    }
                }
                is Result.Error -> {
                    DebugHelper.logError("❌ Failed to load story", storyResult.error)
                    _effect.emit(AIEffect.ShowError("Hikaye yüklenemedi"))
                }
            }
        }
    }

    /**
     * Close story detail and cleanup state
     */
    private fun closeStoryDetail() {
        DebugHelper.log("🔙 Closing story detail")
        _uiState.update {
            it.copy(
                currentStory = null,
                selectedStoryWords = emptyList()
            )
        }
    }

    /**
     * Delete story
     */
    private fun deleteStory(storyId: String) {
        viewModelScope.launch {
            when (storyRepository.deleteStory(storyId)) {
                is Result.Success -> {
                    _effect.emit(AIEffect.ShowMessage("Hikaye silindi"))
                    loadStoryHistory()
                }
                is Result.Error -> {
                    _effect.emit(AIEffect.ShowError("Hikaye silinemedi"))
                }
            }
        }
    }

    /**
     * Toggle favorite status
     */
    private fun toggleFavorite(storyId: String) {
        viewModelScope.launch {
            when (storyRepository.toggleFavorite(storyId)) {
                is Result.Success -> {
                    loadStoryHistory()
                }
                is Result.Error -> {
                    _effect.emit(AIEffect.ShowError("Favori durumu değiştirilemedi"))
                }
            }
        }
    }

    /**
     * Dismiss error message
     */
    private fun dismissError() {
        _uiState.update { it.copy(generationError = null) }
    }
}

// ==================== UI STATE ====================

/**
 * AIUiState - Complete UI state for AI feature
 */
data class AIUiState(
    // Premium status
    val isPremium: Boolean = false,

    // Story history
    val stories: List<GeneratedStory> = emptyList(),
    val isLoadingHistory: Boolean = false,

    // Current story (for detail screen)
    val currentStory: GeneratedStory? = null,
    val selectedStoryWords: List<String> = emptyList(),

    // Generation state
    val isGenerating: Boolean = false,
    val generationError: String? = null,

    // Quota tracking
    val quotaUsed: Int = 0,
    val quotaTotal: Int = 2,

    // Dialog states
    val showCreatorDialog: Boolean = false,
    val showHistorySheet: Boolean = false
) {
    // Computed properties
    val quotaRemaining: Int get() = quotaTotal - quotaUsed
    val hasQuotaRemaining: Boolean get() = quotaRemaining > 0
    val quotaText: String get() = "$quotaRemaining/$quotaTotal kalan"
}

// ==================== USER EVENTS ====================

/**
 * AIEvent - All possible user actions
 */
sealed interface AIEvent {
    // Dialog actions
    data object OpenCreatorDialog : AIEvent
    data object CloseCreatorDialog : AIEvent

    // Generation
    data class GenerateStory(
        val topic: String?,
        val type: StoryType,
        val difficulty: StoryDifficulty,
        val length: StoryLength
    ) : AIEvent

    // History
    data object ShowHistory : AIEvent
    data object CloseHistory : AIEvent

    // Story actions
    data class OpenStoryDetail(val storyId: String) : AIEvent
    data class DeleteStory(val storyId: String) : AIEvent
    data class ToggleFavorite(val storyId: String) : AIEvent
    data object DismissError : AIEvent
    data object CloseStoryDetail : AIEvent
    data object RefreshQuota : AIEvent
}

// ==================== SIDE EFFECTS ====================

/**
 * AIEffect - One-time side effects for UI
 */
sealed interface AIEffect {
    // Messages
    data class ShowMessage(val message: String) : AIEffect
    data class ShowError(val message: String) : AIEffect

    // Navigation
    data class NavigateToDetail(val storyId: String) : AIEffect

    // Animations
    data object ShowGeneratingAnimation : AIEffect
    data object ShowSuccessAnimation : AIEffect
}