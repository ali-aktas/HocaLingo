package com.hocalingo.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.R
import com.hocalingo.app.core.common.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Onboarding Intro Screen
 *
 * Package: feature/onboarding/
 * Manages 5-page modern intro flow
 */
@HiltViewModel
class OnboardingIntroViewModel @Inject constructor(
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingIntroUiState())
    val state: StateFlow<OnboardingIntroUiState> = _state.asStateFlow()

    private val _effect = MutableSharedFlow<OnboardingIntroEffect>()
    val effect = _effect.asSharedFlow()

    fun onEvent(event: OnboardingIntroEvent) {
        when (event) {
            OnboardingIntroEvent.OnNextClick -> handleNextClick()
            OnboardingIntroEvent.OnGetStartedClick -> handleGetStartedClick()
            is OnboardingIntroEvent.OnPageChanged -> updateCurrentPage(event.page)
        }
    }

    private fun handleNextClick() {
        val currentPage = _state.value.currentPage
        val nextPage = currentPage + 1

        if (nextPage < _state.value.totalPages) {
            viewModelScope.launch {
                _effect.emit(OnboardingIntroEffect.ScrollToPage(nextPage))
            }
        }
    }

    private fun handleGetStartedClick() {
        viewModelScope.launch {
            // ✅ Onboarding tamamlandı olarak işaretle
            preferencesManager.setOnboardingCompleted(true)

            _effect.emit(OnboardingIntroEffect.NavigateToPackageSelection)
        }
    }

    private fun updateCurrentPage(page: Int) {
        _state.value = _state.value.copy(currentPage = page)
    }

    companion object {
        /**
         * Get onboarding intro pages data
         *
         * NOTE: Texts are placeholders - update with actual content
         */
        fun getOnboardingPages(): List<OnboardingIntroPage> {
            return listOf(
                OnboardingIntroPage(
                    title = "Öğreneceğin kelimeleri kendin seç",
                    imageRes = R.drawable.onboarding_1
                ),
                OnboardingIntroPage(
                    title = "Çalışma destene yeni kelimeler ekle",
                    imageRes = R.drawable.onboarding_2
                ),
                OnboardingIntroPage(
                    title = "Hatırlamaya çalış, kartı çevir, devam et",
                    imageRes = R.drawable.onboarding_3
                ),
                OnboardingIntroPage(
                    title = "Yapay zeka, senin kelimelerinle bağlamsal yazılar üretsin",
                    imageRes = R.drawable.onboarding_4
                ),
                OnboardingIntroPage(
                    title = "HocaLingo ile gerçekten öğrendiğini hisset!",
                    imageRes = R.drawable.hocalingo_cards
                )
            )
        }
    }
}