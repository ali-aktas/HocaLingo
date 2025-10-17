package com.hocalingo.app.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.hocalingo.app.core.common.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Alarm Scheduler - AlarmManager wrapper
 * ✅ Schedules exact alarms for daily notifications
 * ✅ Handles Android 12+ SCHEDULE_EXACT_ALARM permission
 * ✅ Reschedules after boot
 * ✅ Much more reliable than WorkManager for time-specific tasks
 *
 * Package: app/src/main/java/com/hocalingo/app/core/notification/
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesManager: UserPreferencesManager
) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "AlarmScheduler"
        private const val REQUEST_CODE = 1001
    }

    /**
     * Schedule next alarm based on user preferences
     */
    suspend fun scheduleNextAlarm() {
        Log.d(TAG, "========================================")
        Log.d(TAG, "⏰ SCHEDULING NEXT ALARM")
        Log.d(TAG, "========================================")

        val (isEnabled, preferredHour) = userPreferencesManager.getStudyReminderSettings().first()

        Log.d(TAG, "User Preferences:")
        Log.d(TAG, "  - Enabled: $isEnabled")
        Log.d(TAG, "  - Preferred Hour: $preferredHour:00")

        if (!isEnabled) {
            Log.d(TAG, "❌ Notifications disabled, cancelling alarms")
            cancelAlarm()
            return
        }

        // Check if we can schedule exact alarms (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "❌ Cannot schedule exact alarms - Permission not granted")
                Log.e(TAG, "  → User needs to grant SCHEDULE_EXACT_ALARM in Settings")
                return
            }
        }

        // Calculate next alarm time
        val nextAlarmTime = calculateNextAlarmTime(preferredHour)
        val now = System.currentTimeMillis()
        val delay = nextAlarmTime - now

        Log.d(TAG, "⏰ Alarm Details:")
        Log.d(TAG, "  - Current time: ${formatTimestamp(now)}")
        Log.d(TAG, "  - Scheduled for: ${formatTimestamp(nextAlarmTime)}")
        Log.d(TAG, "  - Delay: ${formatDuration(delay)}")

        // Create pending intent for alarm
        val pendingIntent = createAlarmPendingIntent()

        // Schedule exact alarm
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0+: Use setExactAndAllowWhileIdle for better reliability
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextAlarmTime,
                    pendingIntent
                )
                Log.d(TAG, "✅ Exact alarm scheduled (setExactAndAllowWhileIdle)")
            } else {
                // Android 5.x: Use setExact
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextAlarmTime,
                    pendingIntent
                )
                Log.d(TAG, "✅ Exact alarm scheduled (setExact)")
            }

            Log.d(TAG, "========================================")

        } catch (e: SecurityException) {
            Log.e(TAG, "❌ SecurityException scheduling alarm: ${e.message}")
            Log.e(TAG, "  → This shouldn't happen if permission is granted")
        }
    }

    /**
     * Cancel scheduled alarm
     */
    fun cancelAlarm() {
        Log.d(TAG, "🚫 Cancelling scheduled alarm")

        val pendingIntent = createAlarmPendingIntent()
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()

        Log.d(TAG, "✅ Alarm cancelled")
    }

    /**
     * Check if alarm can be scheduled
     * Returns true if permission granted (Android 12+) or always true for older versions
     */
    fun canScheduleAlarm(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Get next scheduled alarm time (for UI display)
     */
    suspend fun getNextAlarmTime(): Long? {
        val (isEnabled, preferredHour) = userPreferencesManager.getStudyReminderSettings().first()

        if (!isEnabled) return null

        return calculateNextAlarmTime(preferredHour)
    }

    /**
     * Calculate next alarm time based on preferred hour
     * If time has passed today, schedule for tomorrow
     */
    private fun calculateNextAlarmTime(preferredHour: Int): Long {
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

        return targetTime
    }

    /**
     * Create PendingIntent for alarm
     */
    private fun createAlarmPendingIntent(): PendingIntent {
        val intent = Intent(context, DailyNotificationReceiver::class.java).apply {
            action = DailyNotificationReceiver.ACTION_DAILY_NOTIFICATION
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            flags
        )
    }

    // Helper functions for logging
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