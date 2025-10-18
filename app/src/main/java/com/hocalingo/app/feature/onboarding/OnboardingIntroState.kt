package com.hocalingo.app.feature.onboarding

/**
 * State management for onboarding intro
 *
 * Package: feature/onboarding/
 */

// UI State
data class OnboardingIntroUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 3
)

// Events - User actions
sealed interface OnboardingIntroEvent {
    data object OnNextClick : OnboardingIntroEvent
    data object OnGetStartedClick : OnboardingIntroEvent
    data class OnPageChanged(val page: Int) : OnboardingIntroEvent
}

// Effects - One-time UI effects
sealed interface OnboardingIntroEffect {
    data object NavigateToPackageSelection : OnboardingIntroEffect
    data class ScrollToPage(val page: Int) : OnboardingIntroEffect
}