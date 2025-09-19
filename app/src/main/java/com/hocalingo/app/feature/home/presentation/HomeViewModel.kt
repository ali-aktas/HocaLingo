package com.hocalingo.app.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.feature.auth.data.AuthRepository
import com.hocalingo.app.feature.home.domain.HomeRepository
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
 * Home Dashboard ViewModel
 * Central hub functionality with modern Android practices
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homeRepository: HomeRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<HomeEffect>()
    val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

    init {
        DebugHelper.log("HomeViewModel initialized")
        loadDashboardData()
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.LoadDashboardData -> loadDashboardData()
            HomeEvent.StartStudy -> startStudy()
            HomeEvent.AddWord -> addWord()
            HomeEvent.ViewProgress -> viewProgress()
            is HomeEvent.QuickWordStudy -> quickWordStudy(event.wordId)
            HomeEvent.RefreshData -> refreshData()
        }
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }

                // Get current user info
                val currentUser = authRepository.getCurrentUser()
                val userName = currentUser?.displayName ?: "Öğrenci"

                // Load dashboard data from repository
                val dashboardData = homeRepository.getDashboardData()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userName = userName,
                        streakDays = dashboardData.streakDays,
                        dailyGoalProgress = dashboardData.dailyGoalProgress,
                        todayWords = dashboardData.todayWords,
                        weeklyStats = dashboardData.weeklyStats,
                        quickActions = getQuickActions()
                    )
                }

                DebugHelper.log("Dashboard data loaded for user: $userName")

            } catch (e: Exception) {
                DebugHelper.log("Error loading dashboard data: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Dashboard verileri yüklenirken hata oluştu"
                    )
                }
            }
        }
    }

    private fun refreshData() {
        DebugHelper.log("Refreshing dashboard data")
        loadDashboardData()
    }

    private fun startStudy() {
        viewModelScope.launch {
            DebugHelper.log("Navigating to study")
            _effect.emit(HomeEffect.NavigateToStudy)
        }
    }

    private fun addWord() {
        viewModelScope.launch {
            DebugHelper.log("Navigating to add word")
            _effect.emit(HomeEffect.NavigateToAddWord)
        }
    }

    private fun viewProgress() {
        viewModelScope.launch {
            DebugHelper.log("Navigating to profile/progress")
            _effect.emit(HomeEffect.NavigateToProfile)
        }
    }

    private fun quickWordStudy(wordId: String) {
        viewModelScope.launch {
            try {
                DebugHelper.log("Quick study for word: $wordId")
                // Mark word as studied quickly
                homeRepository.markWordAsStudied(wordId)

                // Update daily progress
                _uiState.update { currentState ->
                    val updatedProgress = currentState.dailyGoalProgress.copy(
                        completedWords = currentState.dailyGoalProgress.completedWords + 1
                    )
                    currentState.copy(dailyGoalProgress = updatedProgress)
                }

                _effect.emit(HomeEffect.ShowMessage("Kelime işaretlendi! 🎉"))

            } catch (e: Exception) {
                DebugHelper.log("Error in quick word study: ${e.message}")
                _effect.emit(HomeEffect.ShowMessage("Hata oluştu"))
            }
        }
    }

    private fun getQuickActions(): List<QuickAction> {
        return listOf(
            QuickAction(
                id = "study",
                title = "Çalışmaya Başla",
                description = "Bugünkü kelimelerini öğren",
                icon = "study",
                action = QuickActionType.START_STUDY
            ),
            QuickAction(
                id = "add_word",
                title = "Kelime Ekle",
                description = "Kendi kelimelerini ekle",
                icon = "add",
                action = QuickActionType.ADD_WORD
            ),
            QuickAction(
                id = "progress",
                title = "İlerleme",
                description = "İstatistiklerini görüntüle",
                icon = "chart",
                action = QuickActionType.VIEW_PROGRESS
            ),
            QuickAction(
                id = "challenge",
                title = "Günlük Meydan Okuma",
                description = "Bugünkü zorluğu tamamla",
                icon = "trophy",
                action = QuickActionType.DAILY_CHALLENGE
            )
        )
    }
}