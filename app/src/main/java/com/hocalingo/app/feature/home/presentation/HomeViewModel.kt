package com.hocalingo.app.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.feature.home.domain.HomeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Home ViewModel - v2.1
 * ✅ App launch tracking eklendi (streak için)
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

    init {
        loadDashboardData()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.LoadDashboardData -> loadDashboardData()
            HomeEvent.RefreshData -> refreshData()
            HomeEvent.StartStudy -> handleStartStudy()
            HomeEvent.NavigateToPackageSelection -> handleNavigateToPackageSelection()
            HomeEvent.NavigateToAIAssistant -> handleNavigateToAIAssistant()
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

                        _effect.emit(HomeEffect.ShowError("Dashboard verileri yüklenemedi"))
                    }
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Bilinmeyen hata"
                )
                _effect.emit(HomeEffect.ShowError("Beklenmeyen hata oluştu"))
            }
        }
    }

    private fun refreshData() {
        loadDashboardData()
    }

    private fun handleStartStudy() {
        viewModelScope.launch {
            val progress = _uiState.value.dailyGoalProgress

            if (progress.todayAvailableCards > 0) {
                _effect.emit(HomeEffect.NavigateToStudy)
            } else {
                val message = if (progress.isDailyGoalComplete) {
                    "🎉 Bugünkü hedefi tamamladın! Yarın yeni kartlar seni bekliyor."
                } else {
                    "📚 Çalışacak kart yok. Yeni kartlar seçmek için paket seçimine git."
                }
                _effect.emit(HomeEffect.ShowMessage(message))
            }
        }
    }

    private fun handleNavigateToPackageSelection() {
        viewModelScope.launch {
            _effect.emit(HomeEffect.NavigateToPackageSelection)
        }
    }

    private fun handleNavigateToAIAssistant() {
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