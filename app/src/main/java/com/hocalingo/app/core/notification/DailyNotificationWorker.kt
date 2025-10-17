package com.hocalingo.app.core.notification

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hocalingo.app.feature.notification.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Daily Notification Worker - DEPRECATED
 * ⚠️ This class is now DEPRECATED and replaced by AlarmManager approach
 * ⚠️ Keeping it for backwards compatibility but it's no longer used
 *
 * NEW APPROACH:
 * - DailyNotificationReceiver (BroadcastReceiver)
 * - AlarmScheduler (AlarmManager wrapper)
 *
 * WHY CHANGED?
 * - WorkManager's PeriodicWorkRequest has 15-minute minimum interval
 * - WorkManager doesn't guarantee exact timing
 * - AlarmManager provides exact alarm scheduling
 *
 * Package: app/src/main/java/com/hocalingo/app/core/notification/
 */
@Deprecated(
    message = "Use AlarmManager with DailyNotificationReceiver instead",
    replaceWith = ReplaceWith("DailyNotificationReceiver"),
    level = DeprecationLevel.WARNING
)
@HiltWorker
class DailyNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val notificationRepository: NotificationRepository,
    private val notificationManager: HocaLingoNotificationManager
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "daily_notification_work"
        const val WORKER_TAG = "daily_notification"
        private const val TAG = "DailyNotificationWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "========================================")
        Log.d(TAG, "⚠️ DEPRECATED WORKER EXECUTED")
        Log.d(TAG, "========================================")
        Log.d(TAG, "This worker is deprecated. Use AlarmManager instead.")
        Log.d(TAG, "Run time: ${formatTimestamp(System.currentTimeMillis())}")

        return try {
            // Check if notifications are enabled
            val notificationsEnabled = notificationRepository.areNotificationsEnabled()
            Log.d(TAG, "Notifications enabled in preferences: $notificationsEnabled")

            if (!notificationsEnabled) {
                Log.d(TAG, "❌ Notifications disabled, skipping")
                return Result.success()
            }

            // Get a word for notification
            Log.d(TAG, "📚 Fetching word for notification...")
            when (val result = notificationRepository.getWordForNotification()) {
                is com.hocalingo.app.core.base.Result.Success -> {
                    val word = result.data

                    if (word == null) {
                        Log.d(TAG, "⚠️ No word available for notification")
                        return Result.success()
                    }

                    Log.d(TAG, "✅ Word selected:")
                    Log.d(TAG, "  - ID: ${word.id}")
                    Log.d(TAG, "  - English: ${word.english}")
                    Log.d(TAG, "  - Turkish: ${word.turkish}")
                    Log.d(TAG, "  - Level: ${word.level}")

                    // Send notification
                    Log.d(TAG, "📤 Sending notification...")
                    try {
                        notificationManager.showDailyWordNotification(word)
                        Log.d(TAG, "✅ Notification sent successfully")

                        // Record that notification was sent
                        Log.d(TAG, "📊 Recording notification analytics...")
                        notificationRepository.recordNotificationSent(word.id)
                        Log.d(TAG, "✅ Analytics recorded")

                    } catch (e: SecurityException) {
                        Log.e(TAG, "❌ SecurityException: Missing notification permission")
                        Log.e(TAG, "  → User needs to grant POST_NOTIFICATIONS permission")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error sending notification: ${e.message}")
                        e.printStackTrace()
                    }
                }

                is com.hocalingo.app.core.base.Result.Error -> {
                    Log.e(TAG, "❌ Error fetching word: ${result.error}")
                }
            }

            Log.d(TAG, "========================================")
            Log.d(TAG, "✅ WORKER COMPLETED SUCCESSFULLY")
            Log.d(TAG, "========================================")

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "========================================")
            Log.e(TAG, "❌ WORKER FAILED WITH EXCEPTION")
            Log.e(TAG, "========================================")
            Log.e(TAG, "Error: ${e.message}")
            e.printStackTrace()

            // Return success anyway to avoid retry loops
            // Notification failures shouldn't retry
            Result.success()
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}