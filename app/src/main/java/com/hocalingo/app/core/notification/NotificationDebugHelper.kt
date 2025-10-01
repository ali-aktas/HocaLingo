package com.hocalingo.app.core.notification

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hocalingo.app.core.common.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification Debug Helper
 * ✅ Helps diagnose notification issues
 * ✅ Logs WorkManager state
 * ✅ Shows next scheduled time
 * ✅ Non-intrusive - only for debugging
 *
 * Usage: Call from Profile screen or test activity
 */
@Singleton
class NotificationDebugHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: UserPreferencesManager,
    private val notificationScheduler: NotificationScheduler
) {

    companion object {
        private const val TAG = "NotificationDebug"
    }

    /**
     * Print comprehensive notification debug info
     */
    suspend fun printDebugInfo() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "🔔 NOTIFICATION DEBUG INFO")
        Log.d(TAG, "========================================")

        // 1. User Preferences
        printUserPreferences()

        // 2. WorkManager State
        printWorkManagerState()

        // 3. Next Scheduled Time
        printNextScheduledTime()

        // 4. System Time
        printSystemInfo()

        Log.d(TAG, "========================================")
    }

    private suspend fun printUserPreferences() {
        Log.d(TAG, "📋 USER PREFERENCES:")

        val (enabled, hour) = userPreferencesManager.getStudyReminderSettings().first()
        Log.d(TAG, "  - Notifications Enabled: $enabled")
        Log.d(TAG, "  - Preferred Hour: $hour:00")

        val notificationsEnabled = userPreferencesManager.areNotificationsEnabled().first()
        Log.d(TAG, "  - General Notifications: $notificationsEnabled")
    }

    private suspend fun printWorkManagerState() {
        Log.d(TAG, "\n⚙️ WORKMANAGER STATE:")

        try {
            val workManager = WorkManager.getInstance(context)

            // Check by work name
            val workInfos = workManager
                .getWorkInfosForUniqueWork(DailyNotificationWorker.WORK_NAME)
                .get()

            if (workInfos.isEmpty()) {
                Log.d(TAG, "  ❌ No work scheduled!")
                Log.d(TAG, "  → Solution: Turn notifications ON in settings")
                return
            }

            workInfos.forEachIndexed { index, workInfo ->
                Log.d(TAG, "  Work #${index + 1}:")
                Log.d(TAG, "    - ID: ${workInfo.id}")
                Log.d(TAG, "    - State: ${workInfo.state}")
                Log.d(TAG, "    - Run Attempt: ${workInfo.runAttemptCount}")
                Log.d(TAG, "    - Tags: ${workInfo.tags}")

                when (workInfo.state) {
                    WorkInfo.State.ENQUEUED -> {
                        Log.d(TAG, "    ✅ Work is scheduled and waiting")
                        Log.d(TAG, "    ⏰ Next run: Check 'Next Scheduled Time' section below")
                        Log.d(TAG, "    💡 WorkManager will run at approximately your preferred time")
                        Log.d(TAG, "    💡 Exact time may vary ±30 minutes (flex window)")
                    }
                    WorkInfo.State.RUNNING -> {
                        Log.d(TAG, "    🏃 Work is currently running")
                    }
                    WorkInfo.State.SUCCEEDED -> {
                        Log.d(TAG, "    ✅ Last run succeeded")
                    }
                    WorkInfo.State.FAILED -> {
                        Log.d(TAG, "    ❌ Work failed")
                        Log.d(TAG, "    → Check worker logs for errors")
                    }
                    WorkInfo.State.BLOCKED -> {
                        Log.d(TAG, "    ⏸️ Work is blocked")
                        Log.d(TAG, "    → Check constraints (battery, network)")
                    }
                    WorkInfo.State.CANCELLED -> {
                        Log.d(TAG, "    ⛔ Work was cancelled")
                    }
                }

                // Print output data if available
                if (workInfo.outputData.keyValueMap.isNotEmpty()) {
                    Log.d(TAG, "    📤 Output Data:")
                    workInfo.outputData.keyValueMap.forEach { (key, value) ->
                        Log.d(TAG, "      - $key: $value")
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "  ❌ Error checking WorkManager: ${e.message}")
        }
    }

    private suspend fun printNextScheduledTime() {
        Log.d(TAG, "\n⏰ NEXT SCHEDULED TIME (Calculated):")

        val nextTime = notificationScheduler.getNextNotificationTime()

        if (nextTime == null) {
            Log.d(TAG, "  ❌ No scheduled time (notifications disabled?)")
            return
        }

        val now = System.currentTimeMillis()
        val timeUntil = nextTime - now

        Log.d(TAG, "  - Scheduled for: ${formatTimestamp(nextTime)}")
        Log.d(TAG, "  - Time until: ${formatDuration(timeUntil)}")

        if (timeUntil < 0) {
            Log.d(TAG, "  ⚠️ WARNING: Scheduled time is in the past!")
            Log.d(TAG, "  → This shouldn't happen. Try toggling notifications OFF/ON")
        } else {
            Log.d(TAG, "  ✅ Notification scheduled properly")
            Log.d(TAG, "  💡 Note: Actual notification may arrive ±30 minutes due to flex window")
        }
    }

    private fun printSystemInfo() {
        Log.d(TAG, "\n🕐 SYSTEM INFO:")

        val now = System.currentTimeMillis()
        Log.d(TAG, "  - Current Time: ${formatTimestamp(now)}")
        Log.d(TAG, "  - Timestamp: $now")
        Log.d(TAG, "  - Android Version: ${Build.VERSION.SDK_INT} (API ${Build.VERSION.SDK_INT})")

        val calendar = Calendar.getInstance()
        Log.d(TAG, "  - Current Day: ${calendar.get(Calendar.DAY_OF_MONTH)}")
        Log.d(TAG, "  - Current Hour: ${calendar.get(Calendar.HOUR_OF_DAY)}")
        Log.d(TAG, "  - Current Minute: ${calendar.get(Calendar.MINUTE)}")
    }

    /**
     * Quick check if notifications should work
     */
    suspend fun quickHealthCheck(): String {
        val issues = mutableListOf<String>()

        // Check 1: User preferences
        val (enabled, _) = userPreferencesManager.getStudyReminderSettings().first()
        if (!enabled) {
            issues.add("Bildirimler ayarlardan kapalı")
        }

        // Check 2: WorkManager
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager
            .getWorkInfosForUniqueWork(DailyNotificationWorker.WORK_NAME)
            .get()

        if (workInfos.isEmpty()) {
            issues.add("WorkManager'da bildirim zamanlanmamış")
        } else {
            val activeWork = workInfos.any {
                it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING
            }
            if (!activeWork) {
                issues.add("WorkManager'da aktif iş yok (state: ${workInfos.first().state})")
            }
        }

        // Check 3: Next scheduled time
        val nextTime = notificationScheduler.getNextNotificationTime()
        if (nextTime == null) {
            issues.add("Bir sonraki bildirim zamanı hesaplanamadı")
        } else if (nextTime < System.currentTimeMillis()) {
            issues.add("Sonraki bildirim zamanı geçmişte")
        }

        return if (issues.isEmpty()) {
            "✅ Bildirimler düzgün çalışmalı\n💡 Bildirimler ±30 dakika esneklik penceresi ile gelir"
        } else {
            "⚠️ Sorunlar:\n" + issues.joinToString("\n") { "• $it" }
        }
    }

    /**
     * Force trigger notification for testing
     * (You can call this manually from Profile screen)
     */
    suspend fun triggerTestNotification() {
        Log.d(TAG, "🧪 TRIGGERING TEST NOTIFICATION")
        // This would need to be implemented in the Worker
        // For now, just log
        Log.d(TAG, "  Note: Implement manual trigger in DailyNotificationWorker if needed")
    }

    // Helper functions
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
            minutes > 0 -> "$minutes dakika ${seconds % 60} saniye"
            else -> "$seconds saniye"
        }
    }
}