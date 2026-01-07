package com.hocalingo.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.SoundEffectManager
import com.hocalingo.app.core.common.TrialOfferDataStore
import com.hocalingo.app.core.crash.CrashlyticsManager
import com.hocalingo.app.core.analytics.AnalyticsManager
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
 * Home ViewModel - v2.1
 * ✅ App launch tracking eklendi (streak için)
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val trialOfferDataStore: TrialOfferDataStore,
    private val soundEffectManager: SoundEffectManager,
    private val crashlyticsManager: CrashlyticsManager,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

    init {
        loadDashboardData()
        checkPremiumPush()

        // ✅ Analytics: Ekran görüntüleme
        analyticsManager.logScreenView("home_screen", "HomeViewModel")
    }

    private fun checkPremiumPush() {
        viewModelScope.launch {
            val shouldShow = trialOfferDataStore.shouldShowTrialOffer()
            if (shouldShow) {
                trialOfferDataStore.markFirstShown()
                _uiState.update { it.copy(showPremiumPush = true) }

                // ✅ Analytics: Premium push gösterildi
                analyticsManager.logEvent("premium_push_shown")
            }
        }
    }

    fun dismissPremiumPush() {
        viewModelScope.launch {
            trialOfferDataStore.markFirstDismissed()
            _uiState.update { it.copy(showPremiumPush = false) }

            // ✅ Analytics: Premium push kapatıldı
            analyticsManager.logEvent("premium_push_dismissed")
        }
    }

    fun onPremiumPurchaseSuccess() {
        viewModelScope.launch {
            trialOfferDataStore.resetAfterPurchase()
            _uiState.update { it.copy(showPremiumPush = false) }

            // ✅ Analytics zaten SubscriptionViewModel'de loglanıyor
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.LoadDashboardData -> loadDashboardData()
            HomeEvent.RefreshData -> refreshData()
            HomeEvent.StartStudy -> handleStartStudy()
            HomeEvent.NavigateToPackageSelection -> handleNavigateToPackageSelection()
            HomeEvent.NavigateToAIAssistant -> handleNavigateToAIAssistant()
            HomeEvent.DismissPremiumPush -> dismissPremiumPush()
            HomeEvent.PremiumPurchaseSuccess -> onPremiumPurchaseSuccess()
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // ✅ STEP 1: App launch tracking (streak için)
                homeRepository.trackAppLaunch()

                // STEP 2: Paralel olarak tüm data'ları fetch et
                val userNameResult = homeRepository.getUserName()
                val streakDaysResult = homeRepository.getStreakDays()
                val dailyGoalResult = homeRepository.getDailyGoalProgress()
                val monthlyStatsResult = homeRepository.getMonthlyStats()

                // STEP 3: Sonuçları kontrol et ve state'i güncelle
                when {
                    userNameResult is Result.Success &&
                            streakDaysResult is Result.Success &&
                            dailyGoalResult is Result.Success &&
                            monthlyStatsResult is Result.Success -> {

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            userName = userNameResult.data,
                            streakDays = streakDaysResult.data,
                            dailyGoalProgress = dailyGoalResult.data,
                            monthlyStats = monthlyStatsResult.data,
                            error = null
                        )

                        // ✅ Analytics: Dashboard yüklendi
                        analyticsManager.logEvent("dashboard_loaded",
                            "streak_days" to streakDaysResult.data,
                            "daily_progress" to dailyGoalResult.data.todayCompletedCards
                        )
                    }

                    else -> {
                        // Hata durumunda fallback data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            userName = "Ali",
                            streakDays = 0,
                            dailyGoalProgress = DailyGoalProgress(),
                            monthlyStats = MonthlyStats(),
                            error = "Veriler yüklenirken hata oluştu"
                        )

                        // ✅ Crashlytics: Partial failure
                        crashlyticsManager.log("Dashboard partial load failure")

                        _effect.emit(HomeEffect.ShowError("Dashboard verileri yüklenemedi"))
                    }
                }

            } catch (e: Exception) {
                // ✅ Crashlytics: Hata kaydet
                crashlyticsManager.logError("Dashboard loading failed", e)

                // ✅ Analytics: Hata
                analyticsManager.logError("dashboard_load_error", e.message ?: "Unknown error")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Bilinmeyen hata"
                )
                _effect.emit(HomeEffect.ShowError("Beklenmeyen hata oluştu"))
            }
        }
    }

    private fun refreshData() {
        // ✅ Analytics: Refresh
        analyticsManager.logEvent("home_refresh")
        loadDashboardData()
    }

    private fun handleStartStudy() {
        // ✅ Play click sound
        soundEffectManager.playClickSound()

        viewModelScope.launch {
            val progress = _uiState.value.dailyGoalProgress

            if (progress.todayAvailableCards > 0) {
                // ✅ Analytics: Çalışma başlatıldı
                analyticsManager.logEvent("study_started",
                    "available_cards" to progress.todayAvailableCards
                )

                _effect.emit(HomeEffect.NavigateToStudy)
            } else {
                val message = if (progress.isDailyGoalComplete) {
                    // ✅ Analytics: Günlük hedef tamamlandı
                    analyticsManager.logDailyGoalCompleted(
                        streakDays = _uiState.value.streakDays,
                        wordsLearnedToday = progress.todayCompletedCards
                    )

                    "🎉 Bugünkü hedefi tamamladın! Yarın yeni kartlar seni bekliyor."
                } else {
                    "📚 Çalışacak kart yok. Yeni kartlar seçmek için paket seçimine git."
                }
                _effect.emit(HomeEffect.ShowMessage(message))
            }
        }
    }

    private fun handleNavigateToPackageSelection() {
        // ✅ Play click sound
        soundEffectManager.playClickSound()

        // ✅ Analytics: Paket seçimine gitti
        analyticsManager.logEvent("navigate_to_package_selection")

        viewModelScope.launch {
            _effect.emit(HomeEffect.NavigateToPackageSelection)
        }
    }

    private fun handleNavigateToAIAssistant() {
        // ✅ Play click sound
        soundEffectManager.playClickSound()

        // ✅ Analytics: AI Assistant'a gitti
        analyticsManager.logEvent("navigate_to_ai_assistant")

        viewModelScope.launch {
            _effect.emit(HomeEffect.NavigateToAIAssistant)
        }
    }

    /**
     * Public method to refresh specific data without full reload
     */
    fun refreshStreakData() {
        viewModelScope.launch {
            val streakResult = homeRepository.getStreakDays()
            if (streakResult is Result.Success) {
                _uiState.value = _uiState.value.copy(
                    streakDays = streakResult.data
                )
            }
        }
    }

    /**
     * Public method to refresh daily goal progress
     */
    fun refreshDailyGoalProgress() {
        viewModelScope.launch {
            val progressResult = homeRepository.getDailyGoalProgress()
            if (progressResult is Result.Success) {
                _uiState.value = _uiState.value.copy(
                    dailyGoalProgress = progressResult.data
                )
            }
        }
    }

}