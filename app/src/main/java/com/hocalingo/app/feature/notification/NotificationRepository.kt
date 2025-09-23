package com.hocalingo.app.feature.notification

import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.feature.profile.domain.WordSummary

/**
 * Notification Repository Interface
 * Handles word selection for daily notifications
 */
interface NotificationRepository {

    /**
     * Get a word for daily notification
     * Priority: Overdue words > Random selected words
     */
    suspend fun getWordForNotification(): Result<WordSummary?>

    /**
     * Check if notifications are enabled in user preferences
     */
    suspend fun areNotificationsEnabled(): Boolean

    /**
     * Record that a notification was sent for analytics
     */
    suspend fun recordNotificationSent(wordId: Int): Result<Unit>

    /**
     * Get notification statistics (for future analytics)
     */
    suspend fun getNotificationStats(): Result<NotificationStats>
}

/**
 * Notification statistics for analytics
 */
data class NotificationStats(
    val totalNotificationsSent: Int,
    val notificationsClickedCount: Int,
    val lastNotificationSentAt: Long?,
    val averageResponseTime: Long? // Time from notification to app open
)