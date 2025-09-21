package com.hocalingo.app.feature.profile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.common.ThemeMode
import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.feature.profile.domain.ProfileRepository
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
 * Profile ViewModel - Modern Profile Management
 * ✅ User stats and selected words
 * ✅ Settings management (theme, notifications, study direction)
 * ✅ Performance optimized data loading
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    init {
        loadProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.LoadProfile -> loadProfile()
            ProfileEvent.RefreshData -> refreshData()
            ProfileEvent.ViewAllWords -> handleViewAllWords()
            is ProfileEvent.UpdateThemeMode -> updateThemeMode(event.themeMode)
            is ProfileEvent.UpdateStudyDirection -> updateStudyDirection(event.direction)
            is ProfileEvent.UpdateNotifications -> updateNotifications(event.enabled)
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load all profile data in parallel
                val selectedWordsResult = profileRepository.getSelectedWordsPreview()
                val totalWordsResult = profileRepository.getTotalSelectedWordsCount()
                val userStatsResult = profileRepository.getUserStats()
                val userPreferencesResult = profileRepository.getUserPreferences()

                // Check all results
                if (selectedWordsResult is Result.Success &&
                    totalWordsResult is Result.Success &&
                    userStatsResult is Result.Success &&
                    userPreferencesResult is Result.Success) {

                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            selectedWordsPreview = selectedWordsResult.data,
                            totalWordsCount = totalWordsResult.data,
                            userStats = userStatsResult.data,
                            themeMode = userPreferencesResult.data.themeMode,
                            studyDirection = userPreferencesResult.data.studyDirection,
                            notificationsEnabled = userPreferencesResult.data.notificationsEnabled,
                            soundEnabled = userPreferencesResult.data.soundEnabled,
                            dailyGoal = userPreferencesResult.data.dailyGoal,
                            error = null
                        )
                    }
                } else {
                    // Handle individual errors but still show what we can
                    val errorMessages = mutableListOf<String>()

                    val selectedWords = (selectedWordsResult as? Result.Success)?.data ?: emptyList()
                    val totalWords = (totalWordsResult as? Result.Success)?.data ?: 0
                    val userStats = (userStatsResult as? Result.Success)?.data ?: ProfileUiState().userStats
                    val preferences = (userPreferencesResult as? Result.Success)?.data

                    if (selectedWordsResult is Result.Error) errorMessages.add("Kelimeler yüklenemedi")
                    if (userStatsResult is Result.Error) errorMessages.add("İstatistikler yüklenemedi")
                    if (userPreferencesResult is Result.Error) errorMessages.add("Ayarlar yüklenemedi")

                    _uiState.update { currentState ->
                        currentState.copy(
                            isLoading = false,
                            selectedWordsPreview = selectedWords,
                            totalWordsCount = totalWords,
                            userStats = userStats,
                            themeMode = preferences?.themeMode ?: ThemeMode.SYSTEM,
                            studyDirection = preferences?.studyDirection ?: StudyDirection.EN_TO_TR,
                            notificationsEnabled = preferences?.notificationsEnabled ?: true,
                            soundEnabled = preferences?.soundEnabled ?: true,
                            dailyGoal = preferences?.dailyGoal ?: 20,
                            error = if (errorMessages.isNotEmpty()) errorMessages.joinToString(", ") else null
                        )
                    }

                    if (errorMessages.isNotEmpty()) {
                        _effect.emit(ProfileEffect.ShowError("Bazı veriler yüklenemedi"))
                    }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Profil yüklenirken hata oluştu: ${e.message}"
                    )
                }
                _effect.emit(ProfileEffect.ShowError("Beklenmeyen hata oluştu"))
            }
        }
    }

    private fun refreshData() {
        loadProfile()
    }

    private fun handleViewAllWords() {
        viewModelScope.launch {
            _effect.emit(ProfileEffect.NavigateToWordsList)
        }
    }

    private fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            _uiState.update { it.copy(themeMode = themeMode) }

            when (val result = profileRepository.updateThemeMode(themeMode)) {
                is Result.Success -> {
                    _effect.emit(ProfileEffect.ShowMessage("Tema değiştirildi"))
                }
                is Result.Error -> {
                    // Revert UI state
                    loadProfile()
                    _effect.emit(ProfileEffect.ShowError("Tema değiştirilemedi"))
                }
            }
        }
    }

    private fun updateStudyDirection(direction: StudyDirection) {
        viewModelScope.launch {
            _uiState.update { it.copy(studyDirection = direction) }

            when (val result = profileRepository.updateStudyDirection(direction)) {
                is Result.Success -> {
                    _effect.emit(ProfileEffect.ShowMessage("Çalışma yönü değiştirildi"))
                }
                is Result.Error -> {
                    // Revert UI state
                    loadProfile()
                    _effect.emit(ProfileEffect.ShowError("Çalışma yönü değiştirilemedi"))
                }
            }
        }
    }

    private fun updateNotifications(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(notificationsEnabled = enabled) }

            when (val result = profileRepository.updateNotificationsEnabled(enabled)) {
                is Result.Success -> {
                    val message = if (enabled) "Bildirimler açıldı" else "Bildirimler kapatıldı"
                    _effect.emit(ProfileEffect.ShowMessage(message))
                }
                is Result.Error -> {
                    // Revert UI state
                    loadProfile()
                    _effect.emit(ProfileEffect.ShowError("Bildirim ayarı değiştirilemedi"))
                }
            }
        }
    }

    /**
     * Public method to refresh specific data sections
     */
    fun refreshUserStats() {
        viewModelScope.launch {
            val userStatsResult = profileRepository.getUserStats()
            if (userStatsResult is Result.Success) {
                _uiState.update { it.copy(userStats = userStatsResult.data) }
            }
        }
    }

    /**
     * Public method to refresh selected words
     */
    fun refreshSelectedWords() {
        viewModelScope.launch {
            val selectedWordsResult = profileRepository.getSelectedWordsPreview()
            val totalWordsResult = profileRepository.getTotalSelectedWordsCount()

            if (selectedWordsResult is Result.Success && totalWordsResult is Result.Success) {
                _uiState.update {
                    it.copy(
                        selectedWordsPreview = selectedWordsResult.data,
                        totalWordsCount = totalWordsResult.data
                    )
                }
            }
        }
    }
}