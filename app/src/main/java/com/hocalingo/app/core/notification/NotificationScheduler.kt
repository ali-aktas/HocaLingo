package com.hocalingo.app.core.notification

import android.content.Context
import android.util.Log
import com.hocalingo.app.core.common.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification Scheduler - v2.0 (AlarmManager Based)
 * ✅ Now uses AlarmManager instead of WorkManager
 * ✅ Guarantees exact time execution
 * ✅ Simpler, more reliable approach
 * ✅ Backwards compatible API - no changes needed in ViewModels
 *
 * Package: app/src/main/java/com/hocalingo/app/core/notification/
 *
 * MIGRATION NOTE:
 * - Removed WorkManager dependency
 * - Now delegates to AlarmScheduler for actual scheduling
 * - Maintains same public API for seamless migration
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: UserPreferencesManager,
    private val alarmScheduler: AlarmScheduler
) {

    companion object {
        private const val TAG = "NotificationScheduler"
    }

    /**
     * Schedule daily notifications
     * Now delegates to AlarmScheduler
     */
    suspend fun scheduleDailyNotifications() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "🔔 SCHEDULING DAILY NOTIFICATIONS (AlarmManager)")
        Log.d(TAG, "========================================")

        val (isEnabled, preferredHour) = userPreferencesManager.getStudyReminderSettings().first()

        Log.d(TAG, "User Preferences:")
        Log.d(TAG, "  - Enabled: $isEnabled")
        Log.d(TAG, "  - Preferred Hour: $preferredHour:00")

        if (!isEnabled) {
            Log.d(TAG, "❌ Notifications disabled, cancelling any existing alarms")
            cancelDailyNotifications()
            return
        }

        // Delegate to AlarmScheduler
        alarmScheduler.scheduleNextAlarm()

        Log.d(TAG, "✅ Notification scheduling completed")
        Log.d(TAG, "========================================")
    }

    /**
     * Cancel all scheduled notifications
     */
    fun cancelDailyNotifications() {
        Log.d(TAG, "🚫 Cancelling all scheduled notifications")
        alarmScheduler.cancelAlarm()
        Log.d(TAG, "✅ All notifications cancelled")
    }

    /**
     * Update notification schedule (when user changes time preference)
     */
    suspend fun updateNotificationSchedule() {
        Log.d(TAG, "🔄 Updating notification schedule")
        scheduleDailyNotifications() // Will replace existing alarm
    }

    /**
     * Check if notifications are currently scheduled
     */
    suspend fun areNotificationsScheduled(): Boolean {
        val (isEnabled, _) = userPreferencesManager.getStudyReminderSettings().first()
        val canSchedule = alarmScheduler.canScheduleAlarm()

        val scheduled = isEnabled && canSchedule

        Log.d(TAG, "🔍 Checking if notifications are scheduled:")
        Log.d(TAG, "  - Enabled in preferences: $isEnabled")
        Log.d(TAG, "  - Can schedule alarms: $canSchedule")
        Log.d(TAG, "  - Result: $scheduled")

        return scheduled
    }

    /**
     * Get next scheduled notification time (for UI display)
     */
    suspend fun getNextNotificationTime(): Long? {
        val (isEnabled, _) = userPreferencesManager.getStudyReminderSettings().first()

        if (!isEnabled) {
            Log.d(TAG, "⚠️ Notifications disabled, no next time")
            return null
        }

        val nextTime = alarmScheduler.getNextAlarmTime()

        if (nextTime != null) {
            Log.d(TAG, "📅 Next notification: ${formatTimestamp(nextTime)}")
        } else {
            Log.d(TAG, "⚠️ Could not calculate next notification time")
        }

        return nextTime
    }

    /**
     * Get first notification time as formatted string
     * Used for UI display
     */
    suspend fun getFirstNotificationTime(preferredHour: Int): String {
        val nextTime = alarmScheduler.getNextAlarmTime()
        return if (nextTime != null) {
            formatTimestamp(nextTime)
        } else {
            "Hesaplanamadı"
        }
    }

    /**
     * Check if exact alarms can be scheduled (Android 12+ permission check)
     */
    fun canScheduleExactAlarms(): Boolean {
        return alarmScheduler.canScheduleAlarm()
    }

    // Helper function for logging
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatDuration(millis: Long): String {
        if (millis < 0) return "geçmiş"

        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days gün ${hours % 24} saat"
            hours > 0 -> "$hours saat ${minutes % 60} dakika"
            minutes > 0 -> "$minutes dakika"
            else -> "$seconds saniye"
        }
    }
}