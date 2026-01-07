package com.hocalingo.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.common.ThemeMode
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.core.notification.HocaLingoNotificationManager
import com.hocalingo.app.core.notification.NotificationDebugHelper
import com.hocalingo.app.core.notification.NotificationScheduler
import com.hocalingo.app.core.crash.CrashlyticsManager
import com.hocalingo.app.core.analytics.AnalyticsManager
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
    private val notificationManager: HocaLingoNotificationManager,
    private val userPreferencesManager: UserPreferencesManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val analyticsManager: AnalyticsManager
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

        // ✅ Analytics: Ekran görüntüleme
        analyticsManager.logScreenView("profile_screen", "ProfileViewModel")
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

            // Legal & Support Events
            ProfileEvent.OpenPrivacyPolicy -> openUrl("https://sites.google.com/view/hocalingoprivacypolicy/ana-sayfa")
            ProfileEvent.OpenTermsOfService -> openUrl("https://sites.google.com/view/hocalingo-kullanicisozlesmesi/ana-sayfa")
            ProfileEvent.OpenPlayStore -> openUrl("https://play.google.com/store/apps/details?id=com.hocalingo.app")
            ProfileEvent.OpenSupport -> openUrl("mailto:aliaktasofficial@gmail.com")

        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load all profile data in parallel
                val selectedWordsResult = profileRepository.getSelectedWordsPreview()
                val totalWordsResult = profileRepository.getTotalSelectedWordsCount()
                val statsResult = profileRepository.getUserStats()
                val preferencesResult = profileRepository.getUserPreferences()

                // ✅ YENİ: Bildirim saatini yükle
                val (notificationsEnabled, notificationHour) = userPreferencesManager.getStudyReminderSettings().first()

                // Update UI state with loaded data
                val words = when (selectedWordsResult) {
                    is Result.Success -> selectedWordsResult.data
                    is Result.Error -> emptyList()
                }

                val totalWords = when (totalWordsResult) {
                    is Result.Success -> totalWordsResult.data
                    is Result.Error -> 0
                }

                val stats = when (statsResult) {
                    is Result.Success -> statsResult.data
                    is Result.Error -> UserStats(0, 0, 0, 0, 0, 0.0f)
                }

                val prefs = when (preferencesResult) {
                    is Result.Success -> preferencesResult.data
                    is Result.Error -> UserPreferences(
                        ThemeMode.SYSTEM,
                        StudyDirection.EN_TO_TR,
                        false,
                        20,
                        true
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedWordsPreview = words,
                        totalWordsCount = totalWords,
                        userStats = stats,
                        themeMode = prefs.themeMode,
                        studyDirection = prefs.studyDirection,
                        notificationsEnabled = notificationsEnabled, // ✅ Preferences'tan
                        notificationHour = notificationHour, // ✅ YENİ: Preferences'tan
                        soundEnabled = prefs.soundEnabled,
                        dailyGoal = prefs.dailyGoal
                    )
                }

                // ✅ Analytics: Profil yüklendi
                analyticsManager.logEvent("profile_loaded",
                    "total_words" to totalWords,
                    "notifications_enabled" to notificationsEnabled
                )

            } catch (e: Exception) {
                // ✅ Crashlytics: Hata kaydet
                crashlyticsManager.logError("Profile loading failed", e)

                // ✅ Analytics: Hata
                analyticsManager.logError("profile_load_error", e.message ?: "Unknown error")

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Profil yüklenemedi"
                    )
                }
            }
        }
    }

    private fun refreshData() {
        // ✅ Analytics: Refresh
        analyticsManager.logEvent("profile_refresh")
        loadProfile()
    }

    // BottomSheet Management Functions
    private fun handleViewAllWords() {
        // ✅ Analytics: Kelime listesi açıldı
        analyticsManager.logEvent("view_all_words_clicked")
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
                    // ✅ Crashlytics: Hata kaydet
                    crashlyticsManager.logError("Words loading failed (page $page)", result.error)

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
                    // ✅ Analytics: Tema değiştirildi
                    analyticsManager.logEvent("theme_changed", "theme" to themeMode.name)

                    _effect.emit(ProfileEffect.ShowMessage("Tema değiştirildi"))
                }
                is Result.Error -> {
                    // ✅ Crashlytics: Hata kaydet
                    crashlyticsManager.logError("Theme update failed", result.error)

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
                    // ✅ Analytics: Çalışma yönü değiştirildi
                    analyticsManager.logEvent("study_direction_changed", "direction" to direction.name)

                    _effect.emit(ProfileEffect.ShowMessage("Çalışma yönü değiştirildi"))
                }
                is Result.Error -> {
                    // ✅ Crashlytics: Hata kaydet
                    crashlyticsManager.logError("Study direction update failed", result.error)

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
                    // ✅ Analytics: Bildirim durumu değişti
                    analyticsManager.logEvent("notifications_toggled", "enabled" to enabled)

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
                    // ✅ Crashlytics: Hata kaydet
                    crashlyticsManager.logError("Notification toggle failed", result.error)

                    // Revert UI state
                    loadProfile()
                    _effect.emit(ProfileEffect.ShowError("Bildirim ayarı değiştirilemedi"))
                }
            }
        }
    }

    private fun updateNotificationTime(hour: Int) {
        viewModelScope.launch {
            // ✅ YENİ: UI state'i güncelle
            _uiState.update { it.copy(notificationHour = hour) }

            // ✅ YENİ: Preferences'a kaydet
            val result = userPreferencesManager.setStudyReminder(
                enabled = _uiState.value.notificationsEnabled,
                hour = hour
            )

            when (result) {
                is Result.Success -> {
                    // ✅ Analytics: Bildirim saati değişti
                    analyticsManager.logEvent("notification_time_changed", "hour" to hour)

                    // Update WorkManager schedule with new time
                    notificationScheduler.updateNotificationSchedule()

                    val timeFormat = String.format("%02d:00", hour)
                    _effect.emit(ProfileEffect.ShowMessage("Bildirim saati $timeFormat olarak ayarlandı"))
                }
                is Result.Error -> {
                    // ✅ Crashlytics: Hata kaydet
                    crashlyticsManager.logError("Notification time update failed", result.error)

                    // Revert UI state
                    loadProfile()
                    _effect.emit(ProfileEffect.ShowError("Bildirim saati değiştirilemedi"))
                }
            }
        }
    }

    private fun updateDailyGoal(goal: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(dailyGoal = goal) }

            // ✅ Analytics: Günlük hedef değişti
            analyticsManager.logEvent("daily_goal_changed", "goal" to goal)

            _effect.emit(ProfileEffect.ShowMessage("Günlük hedef güncellendi"))
        }
    }

    /**
     * Handle permission denial
     */
    fun onPermissionDenied() {
        viewModelScope.launch {
            _uiState.update { it.copy(notificationsEnabled = false) }

            // ✅ Analytics: İzin reddedildi
            analyticsManager.logEvent("notification_permission_denied")

            _effect.emit(ProfileEffect.ShowError("Bildirim izni verilmedi"))
        }
    }

    /**
     * Open URL in browser
     */
    private fun openUrl(url: String) {
        viewModelScope.launch {
            // ✅ Analytics: Link açıldı
            when {
                url.contains("privacy") -> analyticsManager.logEvent("privacy_policy_opened")
                url.contains("terms") -> analyticsManager.logEvent("terms_opened")
                url.contains("play.google") -> analyticsManager.logEvent("play_store_opened")
                url.contains("mailto") -> analyticsManager.logEvent("support_email_opened")
            }

            _effect.emit(ProfileEffect.OpenUrl(url))
        }
    }

}