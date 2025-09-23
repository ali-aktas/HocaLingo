package com.hocalingo.app.feature.profile.presentation

import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.common.ThemeMode

/**
 * Profile Events - Complete & Clean
 * ✅ All user interactions
 * ✅ Notification support
 * ✅ No duplicate declarations
 */
sealed interface ProfileEvent {
    // Navigation Events
    data object ViewAllWords : ProfileEvent
    data object Refresh : ProfileEvent

    // Settings Events
    data class UpdateThemeMode(val themeMode: ThemeMode) : ProfileEvent
    data class UpdateStudyDirection(val direction: StudyDirection) : ProfileEvent
    data class UpdateNotifications(val enabled: Boolean) : ProfileEvent
    data class UpdateNotificationTime(val hour: Int) : ProfileEvent
    data class UpdateDailyGoal(val goal: Int) : ProfileEvent
}

/**
 * Profile Effects - Complete & Clean
 * ✅ Navigation effects
 * ✅ User feedback effects
 * ✅ Notification-specific effects
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
}