package com.hocalingo.app.core.notification

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hocalingo.app.feature.notification.NotificationRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Daily Notification Worker
 * ✅ Background task for daily word notifications
 * ✅ Hilt integration for dependency injection
 * ✅ Coroutine-based for async operations
 * ✅ Robust error handling
 */
@HiltWorker
class DailyNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val notificationRepository: NotificationRepository,
    private val notificationManager: HocaLingoNotificationManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORKER_TAG = "daily_notification_worker"
        const val WORK_NAME = "hocalingo_daily_notification"
        private const val TAG = "DailyNotificationWorker"
    }

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        Log.d(TAG, "Daily notification worker started")

        return try {
            // 1. Check if notifications are enabled in settings
            val notificationsEnabled = notificationRepository.areNotificationsEnabled()
            if (!notificationsEnabled) {
                Log.d(TAG, "Notifications disabled in settings - skipping")
                return Result.success()
            }

            // 2. Check system notification permission
            if (!notificationManager.hasNotificationPermission()) {
                Log.d(TAG, "No notification permission - skipping")
                return Result.success()
            }

            // 3. Get a word for notification
            when (val wordResult = notificationRepository.getWordForNotification()) {
                is com.hocalingo.app.core.common.base.Result.Success -> {
                    val word = wordResult.data
                    if (word != null) {
                        // 4. Show notification
                        notificationManager.showDailyWordNotification(word)
                        Log.d(TAG, "Daily notification sent: ${word.english} → ${word.turkish}")

                        // 5. Track notification sent
                        notificationRepository.recordNotificationSent(word.id)

                        Result.success()
                    } else {
                        Log.d(TAG, "No words available for notification")
                        Result.success() // Not an error - user might not have selected words yet
                    }
                }
                is com.hocalingo.app.core.common.base.Result.Error -> {
                    Log.e(TAG, "Failed to get word for notification: ${wordResult.error}")
                    Result.failure() // Will trigger retry
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Daily notification worker failed", e)
            Result.failure() // Will trigger retry according to backoff policy
        }
    }
}