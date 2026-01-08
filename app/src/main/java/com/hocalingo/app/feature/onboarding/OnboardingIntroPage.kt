package com.hocalingo.app.feature.onboarding

import androidx.annotation.DrawableRes

/**
 * Data model for onboarding intro pages
 *
 * Package: feature/onboarding/
 * Simplified structure: Only title and image
 */
data class OnboardingIntroPage(
    val title: String,
    @DrawableRes val imageRes: Int
)