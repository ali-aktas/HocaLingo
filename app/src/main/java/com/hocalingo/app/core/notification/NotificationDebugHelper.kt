package com.hocalingo.app.core.notification

import android.content.Context
import android.os.Build
import android.util.Log
import com.hocalingo.app.core.common.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Notification Debug Helper - v2.0 (AlarmManager based)
 * ✅ Updated for AlarmManager instead of WorkManager
 * ✅ Helps diagnose notification issues
 * ✅ Shows next scheduled time
 * ✅ Checks AlarmManager permissions
 *
 * Package: app/src/main/java/com/hocalingo/app/core/notification/
 *
 * Usage: Call from Profile screen or test activity
 */
@Singleton
class NotificationDebugHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: UserPreferencesManager,
    private val notificationScheduler: NotificationScheduler,
    private val alarmScheduler: AlarmScheduler
) {

    companion object {
        private const val TAG = "NotificationDebug"
    }

    /**
     * Print comprehensive notification debug info
     */
    suspend fun printDebugInfo() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "🔔 NOTIFICATION DEBUG INFO (AlarmManager)")
        Log.d(TAG, "========================================")

        // 1. User Preferences
        printUserPreferences()

        // 2. AlarmManager State
        printAlarmManagerState()

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

    private fun printAlarmManagerState() {
        Log.d(TAG, "\n⏰ ALARMMANAGER STATE:")

        // Check if exact alarms can be scheduled
        val canSchedule = alarmScheduler.canScheduleAlarm()
        Log.d(TAG, "  - Can schedule exact alarms: $canSchedule")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!canSchedule) {
                Log.d(TAG, "  ⚠️ WARNING: Cannot schedule exact alarms!")
                Log.d(TAG, "  → User needs to grant SCHEDULE_EXACT_ALARM permission")
                Log.d(TAG, "  → Go to: Settings > Apps > HocaLingo > Alarms & reminders")
            } else {
                Log.d(TAG, "  ✅ Exact alarm permission granted")
            }
        } else {
            Log.d(TAG, "  ✅ Android < 12, no special permission needed")
        }
    }

    private suspend fun printNextScheduledTime() {
        Log.d(TAG, "\n📅 NEXT SCHEDULED TIME:")

        val nextTime = notificationScheduler.getNextNotificationTime()

        if (nextTime == null) {
            Log.d(TAG, "  ❌ No notification scheduled!")
            Log.d(TAG, "  → Notifications might be disabled")
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
            Log.d(TAG, "  💡 Note: AlarmManager provides exact timing (not flexible like WorkManager)")
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

        // Check 2: AlarmManager permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmScheduler.canScheduleAlarm()) {
                issues.add("SCHEDULE_EXACT_ALARM izni verilmemiş")
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
            "✅ Bildirimler düzgün çalışmalı\n💡 AlarmManager ile tam zamanında gelecek"
        } else {
            "⚠️ Sorunlar:\n" + issues.joinToString("\n") { "• $it" }
        }
    }

    /**
     * Force trigger notification for testing
     * This manually triggers the alarm receiver
     */
    fun triggerTestNotification() {
        Log.d(TAG, "🧪 TRIGGERING TEST NOTIFICATION")
        Log.d(TAG, "  Note: Manually sending broadcast to receiver...")

        try {
            val intent = android.content.Intent(context, DailyNotificationReceiver::class.java).apply {
                action = DailyNotificationReceiver.ACTION_DAILY_NOTIFICATION
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "  ✅ Test broadcast sent")
        } catch (e: Exception) {
            Log.e(TAG, "  ❌ Error triggering test: ${e.message}")
        }
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