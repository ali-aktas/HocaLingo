package com.hocalingo.app.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.feature.notification.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Daily Notification Receiver
 * ✅ BroadcastReceiver that triggers when AlarmManager fires
 * ✅ Handles BOOT_COMPLETED to reschedule alarms after reboot
 * ✅ Dagger Hilt injected for dependency management
 *
 * Package: app/src/main/java/com/hocalingo/app/core/notification/
 */
@AndroidEntryPoint
class DailyNotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var notificationManager: HocaLingoNotificationManager

    @Inject
    lateinit var alarmScheduler: AlarmScheduler

    companion object {
        private const val TAG = "NotificationReceiver"
        const val ACTION_DAILY_NOTIFICATION = "com.hocalingo.app.ACTION_DAILY_NOTIFICATION"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "========================================")
        Log.d(TAG, "📬 BROADCAST RECEIVER TRIGGERED")
        Log.d(TAG, "========================================")
        Log.d(TAG, "Action: ${intent.action}")

        when (intent.action) {
            ACTION_DAILY_NOTIFICATION -> {
                // Daily notification triggered
                handleDailyNotification()
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Device rebooted, reschedule alarms
                Log.d(TAG, "📱 Device rebooted - Rescheduling notifications")
                rescheduleNotifications()
            }
        }
    }

    /**
     * Handle daily notification trigger
     */
    private fun handleDailyNotification() {
        Log.d(TAG, "🔔 Processing daily notification...")

        // Use goAsync for background work
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                // Check if notifications are enabled
                val notificationsEnabled = notificationRepository.areNotificationsEnabled()
                Log.d(TAG, "Notifications enabled: $notificationsEnabled")

                if (!notificationsEnabled) {
                    Log.d(TAG, "❌ Notifications disabled, skipping")
                    pendingResult.finish()
                    return@launch
                }

                // Get a word for notification
                Log.d(TAG, "📚 Fetching word for notification...")
                when (val result = notificationRepository.getWordForNotification()) {
                    is Result.Success -> {
                        val word = result.data

                        if (word == null) {
                            Log.d(TAG, "⚠️ No word available for notification")
                            pendingResult.finish()
                            return@launch
                        }

                        Log.d(TAG, "✅ Word selected:")
                        Log.d(TAG, "  - ID: ${word.id}")
                        Log.d(TAG, "  - English: ${word.english}")
                        Log.d(TAG, "  - Turkish: ${word.turkish}")

                        // Send notification
                        Log.d(TAG, "📤 Sending notification...")
                        try {
                            notificationManager.showDailyWordNotification(word)
                            Log.d(TAG, "✅ Notification sent successfully")

                            // Record analytics
                            notificationRepository.recordNotificationSent(word.id)
                            Log.d(TAG, "✅ Analytics recorded")

                        } catch (e: SecurityException) {
                            Log.e(TAG, "❌ SecurityException: ${e.message}")
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error sending notification: ${e.message}")
                            e.printStackTrace()
                        }

                        // Schedule next alarm
                        Log.d(TAG, "⏰ Scheduling next alarm...")
                        alarmScheduler.scheduleNextAlarm()
                    }

                    is Result.Error -> {
                        Log.e(TAG, "❌ Error fetching word: ${result.error}")
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception in receiver: ${e.message}")
                e.printStackTrace()
            } finally {
                Log.d(TAG, "========================================")
                pendingResult.finish()
            }
        }
    }

    /**
     * Reschedule notifications after device reboot
     */
    private fun rescheduleNotifications() {
        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        scope.launch {
            try {
                val notificationsEnabled = notificationRepository.areNotificationsEnabled()
                if (notificationsEnabled) {
                    Log.d(TAG, "🔄 Rescheduling alarms after boot...")
                    alarmScheduler.scheduleNextAlarm()
                    Log.d(TAG, "✅ Alarms rescheduled")
                } else {
                    Log.d(TAG, "⏸️ Notifications disabled, not rescheduling")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error rescheduling: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }
}