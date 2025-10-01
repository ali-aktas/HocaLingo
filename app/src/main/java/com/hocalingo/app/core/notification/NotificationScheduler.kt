package com.hocalingo.app.core.notification

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hocalingo.app.core.common.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification Scheduler
 * ✅ Manages WorkManager for daily notifications
 * ✅ Schedules at user-preferred time
 * ✅ Battery and network optimized
 * ✅ Handles timezone changes
 * ✅ DEBUG LOGS: Enhanced logging to diagnose issues
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: UserPreferencesManager
) {

    private val workManager = WorkManager.getInstance(context)

    companion object {
        private const val TAG = "NotificationScheduler"
    }

    /**
     * Schedule daily notifications
     * Runs at user's preferred time (default: 23:00)
     */
    suspend fun scheduleDailyNotifications() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "🔔 SCHEDULING DAILY NOTIFICATIONS")
        Log.d(TAG, "========================================")

        // Get user preference for notification time
        val (isEnabled, preferredHour) = userPreferencesManager.getStudyReminderSettings().first()

        Log.d(TAG, "User Preferences:")
        Log.d(TAG, "  - Enabled: $isEnabled")
        Log.d(TAG, "  - Preferred Hour: $preferredHour:00")

        if (!isEnabled) {
            Log.d(TAG, "❌ Notifications disabled, cancelling any existing schedules")
            cancelDailyNotifications()
            return
        }

        // Calculate initial delay to start at preferred hour
        val initialDelay = calculateInitialDelay(preferredHour)
        val initialDelayMinutes = initialDelay / (1000 * 60)

        Log.d(TAG, "⏰ Scheduling Details:")
        Log.d(TAG, "  - Initial delay: ${formatDuration(initialDelay)}")
        Log.d(TAG, "  - Initial delay (minutes): $initialDelayMinutes")
        Log.d(TAG, "  - First notification: ${getFirstNotificationTime(preferredHour)}")
        Log.d(TAG, "  - Repeat interval: 24 hours")
        Log.d(TAG, "  - Flex window: 30 minutes")

        // Create work constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Works offline
            .setRequiresBatteryNotLow(true) // Respect battery optimization
            .setRequiresCharging(false) // Don't require charging
            .setRequiresDeviceIdle(false) // Can run when device is active
            .build()

        Log.d(TAG, "📋 Work Constraints:")
        Log.d(TAG, "  - Network: Not required")
        Log.d(TAG, "  - Battery: Must not be low")
        Log.d(TAG, "  - Charging: Not required")
        Log.d(TAG, "  - Device idle: Not required")

        // Create periodic work request
        val notificationWorkRequest = PeriodicWorkRequestBuilder<DailyNotificationWorker>(
            repeatInterval = 24, // Every 24 hours
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 30, // 30 minutes flex window
            flexTimeIntervalUnit = TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .addTag(DailyNotificationWorker.WORKER_TAG)
            .build()

        // Schedule with replace policy to update timing if changed
        workManager.enqueueUniquePeriodicWork(
            DailyNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            notificationWorkRequest
        )

        Log.d(TAG, "✅ Notification work scheduled successfully")
        Log.d(TAG, "  - Work Name: ${DailyNotificationWorker.WORK_NAME}")
        Log.d(TAG, "  - Worker Tag: ${DailyNotificationWorker.WORKER_TAG}")
        Log.d(TAG, "  - Policy: REPLACE (will update if already exists)")
        Log.d(TAG, "========================================")
    }

    /**
     * Cancel all scheduled notifications
     */
    fun cancelDailyNotifications() {
        Log.d(TAG, "🚫 Cancelling all scheduled notifications")
        workManager.cancelUniqueWork(DailyNotificationWorker.WORK_NAME)
        workManager.cancelAllWorkByTag(DailyNotificationWorker.WORKER_TAG)
        Log.d(TAG, "✅ All notifications cancelled")
    }

    /**
     * Update notification schedule (when user changes time preference)
     */
    suspend fun updateNotificationSchedule() {
        Log.d(TAG, "🔄 Updating notification schedule")
        scheduleDailyNotifications() // Will replace existing schedule
    }

    /**
     * Check if notifications are currently scheduled
     */
    suspend fun areNotificationsScheduled(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(DailyNotificationWorker.WORK_NAME).get()
        val scheduled = workInfos.any { !it.state.isFinished }

        Log.d(TAG, "🔍 Checking if notifications are scheduled: $scheduled")
        if (workInfos.isNotEmpty()) {
            Log.d(TAG, "  - Work state: ${workInfos.first().state}")
        }

        return scheduled
    }

    /**
     * Calculate initial delay to start at preferred hour today or tomorrow
     * ✅ Uses actual preferredHour parameter properly
     */
    private fun calculateInitialDelay(preferredHour: Int): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Set target time to user's preferred hour today
        calendar.set(Calendar.HOUR_OF_DAY, preferredHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var targetTime = calendar.timeInMillis

        // If target time is in the past, schedule for tomorrow
        if (targetTime <= now) {
            Log.d(TAG, "  ⚠️ Target time is in the past, scheduling for tomorrow")
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            targetTime = calendar.timeInMillis
        }

        val delay = targetTime - now
        Log.d(TAG, "  - Current time: ${formatTimestamp(now)}")
        Log.d(TAG, "  - Target time: ${formatTimestamp(targetTime)}")
        Log.d(TAG, "  - Calculated delay: ${formatDuration(delay)}")

        return delay
    }

    /**
     * Get next scheduled notification time (for UI display)
     */
    suspend fun getNextNotificationTime(): Long? {
        val (isEnabled, preferredHour) = userPreferencesManager.getStudyReminderSettings().first()

        if (!isEnabled) return null

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, preferredHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var nextTime = calendar.timeInMillis

        // If time is in the past, get tomorrow's time
        if (nextTime <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            nextTime = calendar.timeInMillis
        }

        Log.d(TAG, "📅 Next notification time: ${formatTimestamp(nextTime)}")

        return nextTime
    }

    // Helper functions for logging
    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "$days gün ${hours % 24} saat ${minutes % 60} dakika"
            hours > 0 -> "$hours saat ${minutes % 60} dakika"
            minutes > 0 -> "$minutes dakika"
            else -> "$seconds saniye"
        }
    }

    private fun getFirstNotificationTime(preferredHour: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, preferredHour)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        return formatTimestamp(calendar.timeInMillis)
    }
}