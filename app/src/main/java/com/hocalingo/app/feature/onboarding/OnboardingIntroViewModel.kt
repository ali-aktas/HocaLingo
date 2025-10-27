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
 * Manages 3-page intro flow before package selection
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
         */
        fun getOnboardingPages(): List<OnboardingIntroPage> {
            return listOf(
                OnboardingIntroPage(
                    title = "Kelimeleri çerez gibi öğrenmeye hazır ol!",
                    description = "Önce hatırlamaya çalış, sonra arka yüzünü çevir. Doğru bildiysen yeşil butona tıkla!",
                    imageRes = R.drawable.onboarding_teacher_1,
                    buttonText = "Devam Et",
                    backgroundColor = 0xFFFB9322
                ),
                OnboardingIntroPage(
                    title = "Öğrenmek istediğin kelimeyi sağa kaydır!",
                    description = "Çalışmak istediğin kelimeyi seç ya da kendi kelimelerini ekle!",
                    imageRes = R.drawable.onboarding_teacher_2,
                    buttonText = "Devam Et",
                    backgroundColor = 0xFFFB9322
                ),
                OnboardingIntroPage(
                    title = "Yapay zeka ile kalıcı ve doğal öğrenme!",
                    description = "Hocalingo yapay zekası ile çalıştığın kelimelerden orijinal hikayeler ve motivasyon yazıları oluştur.",
                    imageRes = R.drawable.onboarding_teacher_3,
                    buttonText = "Öğrenmeye Başla",
                    backgroundColor = 0xFFFB9322
                )
            )
        }
    }
}