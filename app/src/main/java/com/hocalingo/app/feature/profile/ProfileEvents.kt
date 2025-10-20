package com.hocalingo.app.feature.profile

import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.common.ThemeMode

/**
 * Profile Events - Enhanced with BottomSheet Support
 * ✅ All user interactions
 * ✅ Notification support
 * ✅ BottomSheet management for selected words
 * ✅ Pagination support
 */
sealed interface ProfileEvent {
    // Navigation Events
    data object ViewAllWords : ProfileEvent
    data object Refresh : ProfileEvent

    // BottomSheet Events
    data object ShowWordsBottomSheet : ProfileEvent
    data object HideWordsBottomSheet : ProfileEvent
    data object LoadMoreWords : ProfileEvent
    data object RefreshAllWords : ProfileEvent

    // Settings Events
    data class UpdateThemeMode(val themeMode: ThemeMode) : ProfileEvent
    data class UpdateStudyDirection(val direction: StudyDirection) : ProfileEvent
    data class UpdateNotifications(val enabled: Boolean) : ProfileEvent
    data class UpdateNotificationTime(val hour: Int) : ProfileEvent
    data class UpdateDailyGoal(val goal: Int) : ProfileEvent

    // Legal & Support Events
    data object OpenPrivacyPolicy : ProfileEvent
    data object OpenTermsOfService : ProfileEvent
    data object OpenPlayStore : ProfileEvent
    data object OpenSupport : ProfileEvent

}

/**
 * Profile Effects - Enhanced with BottomSheet Support
 * ✅ Navigation effects
 * ✅ User feedback effects
 * ✅ Notification-specific effects
 * ✅ BottomSheet effects
 */
sealed interface ProfileEffect {
    // Navigation Effects
    data object NavigateToWordsList : ProfileEffect

    // User Feedback Effects
    data class ShowMessage(val message: String) : ProfileEffect
    data class ShowError(val error: String) : ProfileEffect

    // Notification Effects
    data object RequestNotificationPermission : ProfileEffect
    data object ShowNotificationPermissionDialog : ProfileEffect
    data class ShowNotificationScheduled(val time: String) : ProfileEffect

    // BottomSheet Effects
    data object ShowWordsBottomSheet : ProfileEffect
    data object HideWordsBottomSheet : ProfileEffect
    data class ShowWordsLoadError(val error: String) : ProfileEffect

    // Legal & Support Effects
    data class OpenUrl(val url: String) : ProfileEffect

}