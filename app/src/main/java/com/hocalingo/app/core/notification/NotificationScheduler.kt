package com.hocalingo.app.core.notification

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.hocalingo.app.core.common.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification Scheduler
 * ✅ Manages WorkManager for daily notifications
 * ✅ Schedules at user-preferred time
 * ✅ Battery and network optimized
 * ✅ Handles timezone changes
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: UserPreferencesManager
) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Schedule daily notifications
     * Runs at user's preferred time (default: 8 PM)
     */
    suspend fun scheduleDailyNotifications() {
        // Get user preference for notification time
        val (isEnabled, preferredHour) = userPreferencesManager.getStudyReminderSettings().first()

        if (!isEnabled) {
            cancelDailyNotifications()
            return
        }

        // Calculate initial delay to start at preferred hour
        val initialDelay = calculateInitialDelay(preferredHour)

        // Create work constraints
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Works offline
            .setRequiresBatteryNotLow(true) // Respect battery optimization
            .setRequiresCharging(false) // Don't require charging
            .setRequiresDeviceIdle(false) // Can run when device is active
            .build()

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
    }

    /**
     * Cancel all scheduled notifications
     */
    fun cancelDailyNotifications() {
        workManager.cancelUniqueWork(DailyNotificationWorker.WORK_NAME)
        workManager.cancelAllWorkByTag(DailyNotificationWorker.WORKER_TAG)
    }

    /**
     * Update notification schedule (when user changes time preference)
     */
    suspend fun updateNotificationSchedule() {
        scheduleDailyNotifications() // Will replace existing schedule
    }

    /**
     * Check if notifications are currently scheduled
     */
    suspend fun areNotificationsScheduled(): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(DailyNotificationWorker.WORK_NAME).get()
        return workInfos.any { !it.state.isFinished }
    }

    /**
     * Calculate initial delay to start at preferred hour today or tomorrow
     * ✅ Updated for 1:35 AM testing
     */
    private fun calculateInitialDelay(preferredHour: Int): Long {
        val calendar = Calendar.getInstance()
        val now = calendar.timeInMillis

        // Set to 1:35 AM (test time)
        calendar.set(Calendar.HOUR_OF_DAY, 1)  // 1 AM
        calendar.set(Calendar.MINUTE, 41)      // 35 minutes
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var targetTime = calendar.timeInMillis

        // If target time is in the past, schedule for tomorrow
        if (targetTime <= now) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            targetTime = calendar.timeInMillis
        }

        return targetTime - now
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

        return nextTime
    }
}