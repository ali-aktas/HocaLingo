package com.hocalingo.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.common.ThemeMode
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.notification.HocaLingoNotificationManager
import com.hocalingo.app.core.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Profile ViewModel - Enhanced with BottomSheet Management
 * ✅ Modern Profile Management
 * ✅ Notification toggle with WorkManager integration
 * ✅ Settings management (theme, notifications, study direction)
 * ✅ BottomSheet with pagination for selected words
 * ✅ Performance optimized data loading
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val notificationScheduler: NotificationScheduler,
    private val notificationManager: HocaLingoNotificationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<ProfileEffect>()
    val effect: SharedFlow<ProfileEffect> = _effect.asSharedFlow()

    // Pagination constants
    private companion object {
        const val WORDS_PER_PAGE = 20
    }

    init {
        loadProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            ProfileEvent.Refresh -> refreshData()
            ProfileEvent.ViewAllWords -> handleViewAllWords()

            // BottomSheet Events
            ProfileEvent.ShowWordsBottomSheet -> showWordsBottomSheet()
            ProfileEvent.HideWordsBottomSheet -> hideWordsBottomSheet()
            ProfileEvent.LoadMoreWords -> loadMoreWords()
            ProfileEvent.RefreshAllWords -> refreshAllWords()

            // Settings Events
            is ProfileEvent.UpdateThemeMode -> updateThemeMode(event.themeMode)
            is ProfileEvent.UpdateStudyDirection -> updateStudyDirection(event.direction)
            is ProfileEvent.UpdateNotifications -> updateNotifications(event.enabled)
            is ProfileEvent.UpdateNotificationTime -> updateNotificationTime(event.hour)
            is ProfileEvent.UpdateDailyGoal -> updateDailyGoal(event.goal)
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

    // BottomSheet Management Functions
    private fun handleViewAllWords() {
        showWordsBottomSheet()
    }

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

            // Load first page of words
            loadWordsPage(0)

            _effect.emit(ProfileEffect.ShowWordsBottomSheet)
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
                            newWords // First page, replace all
                        } else {
                            currentState.allSelectedWords + newWords // Append to existing
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
                    _effect.emit(ProfileEffect.ShowWordsLoadError("Kelimeler yüklenemedi"))
                }
            }
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
            // Check permission first if enabling
            if (enabled && !notificationManager.hasNotificationPermission()) {
                _effect.emit(ProfileEffect.RequestNotificationPermission)
                return@launch
            }

            _uiState.update { it.copy(notificationsEnabled = enabled) }

            when (val result = profileRepository.updateNotificationsEnabled(enabled)) {
                is Result.Success -> {
                    // Update WorkManager schedule
                    if (enabled) {
                        notificationScheduler.scheduleDailyNotifications()
                        val nextTime = notificationScheduler.getNextNotificationTime()
                        if (nextTime != null) {
                            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                            val timeString = timeFormat.format(Date(nextTime))
                            _effect.emit(ProfileEffect.ShowNotificationScheduled(timeString))
                        } else {
                            _effect.emit(ProfileEffect.ShowMessage("Bildirimler açıldı"))
                        }
                    } else {
                        notificationScheduler.cancelDailyNotifications()
                        _effect.emit(ProfileEffect.ShowMessage("Bildirimler kapatıldı"))
                    }
                }
                is Result.Error -> {
                    // Revert UI state
                    loadProfile()
                    _effect.emit(ProfileEffect.ShowError("Bildirim ayarı değiştirilemedi"))
                }
            }
        }
    }

    private fun updateNotificationTime(hour: Int) {
        viewModelScope.launch {
            // Update schedule with new time
            notificationScheduler.updateNotificationSchedule()

            val timeFormat = String.format("%02d:00", hour)
            _effect.emit(ProfileEffect.ShowMessage("Bildirim saati $timeFormat olarak ayarlandı"))
        }
    }

    private fun updateDailyGoal(goal: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(dailyGoal = goal) }
            _effect.emit(ProfileEffect.ShowMessage("Günlük hedef güncellendi"))
        }
    }

    /**
     * Handle permission denial
     */
    fun onPermissionDenied() {
        viewModelScope.launch {
            _uiState.update { it.copy(notificationsEnabled = false) }
            _effect.emit(ProfileEffect.ShowError("Bildirim izni verilmedi"))
        }
    }

}