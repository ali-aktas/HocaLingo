package com.hocalingo.app.feature.onboarding

import androidx.annotation.DrawableRes

/**
 * Data model for onboarding intro pages
 *
 * Package: feature/onboarding/
 */
data class OnboardingIntroPage(
    val title: String,
    val description: String,
    @DrawableRes val imageRes: Int,
    val buttonText: String,
    val backgroundColor: Long
)