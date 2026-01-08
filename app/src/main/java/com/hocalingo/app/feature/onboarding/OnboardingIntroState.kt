package com.hocalingo.app.feature.onboarding

/**
 * State management for onboarding intro
 *
 * Package: feature/onboarding/
 * Updated for 5-page onboarding flow
 */

// UI State
data class OnboardingIntroUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 5  // 5 sayfalık sistem
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