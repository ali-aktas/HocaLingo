package com.hocalingo.app.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.hocalingo.app.MainActivity
import com.hocalingo.app.R
import com.hocalingo.app.feature.profile.WordSummary
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * HocaLingo Notification Manager
 * ✅ Daily word reminders
 * ✅ Permission handling
 * ✅ Android 13+ compatibility
 * ✅ Deep linking to study screen
 */
@Singleton
class HocaLingoNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "hocalingo_daily_words"
        const val CHANNEL_NAME = "Günlük Kelime Hatırlatmaları"
        const val NOTIFICATION_ID = 1001

        // Deep link extras
        const val EXTRA_WORD_ID = "word_id"
        const val EXTRA_FROM_NOTIFICATION = "from_notification"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    /**
     * Create notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Günlük kelime çalışması için hatırlatmalar"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Show daily word notification
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun showDailyWordNotification(word: WordSummary) {
        if (!hasNotificationPermission()) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_WORD_ID, word.id)
            putExtra(EXTRA_FROM_NOTIFICATION, true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.hocalingo_plane) // ✅ Senin beyaz vektör ikonun
            .setLargeIcon(
                BitmapFactory.decodeResource(
                    context.resources,
                    R.mipmap.ic_launcher
                )
            ) // ✅ YENİ EKLENEN - uygulama ikonu
            .setContentTitle(getNotificationTitle(word))
            .setContentText(getNotificationText(word))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(getNotificationBigText(word))
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * Check if notification permission is granted
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            notificationManager.areNotificationsEnabled()
        }
    }

    /**
     * Cancel all notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * Generate engaging notification titles
     */
    private fun getNotificationTitle(word: WordSummary): String {
        val titles = listOf(
            "🔥 Kelime zamanı!",
            "⏰ ${word.english} seni bekliyor!",
            "🚀 Hızlı tekrar?",
            "📚 Günlük doz!",
            "✨ Kelime pratiği!"
        )
        return titles.random()
    }

    /**
     * Generate notification content text
     */
    private fun getNotificationText(word: WordSummary): String {
        val templates = listOf(
            "'${word.english}' kelimesini hatırlıyor musun?",
            "${word.english} → ${word.turkish}",
            "'${word.english}' kelimesini öğrenmek üzeresin!",
            "Bugün '${word.english}' çalışma zamanı!",
            "${word.english} kelimesi tekrar edilmeyi bekliyor!"
        )
        return templates.random()
    }

    /**
     * Generate expanded notification text
     */
    private fun getNotificationBigText(word: WordSummary): String {
        val motivationalMessages = listOf(
            "Merhaba! '${word.english}' kelimesini tekrar etme zamanı. Sadece 2 dakika ayır ve kelime dağarcığını güçlendir! 💪",
            "'${word.english}' → '${word.turkish}' \n\nHızlı bir tekrarla bugünü kelime ile taçlandır! 🎯",
            "Günlük kelime rutinin seni bekliyor! '${word.english}' ile başlayalım. ⭐",
            "'${word.english}' kelimesini öğrenmeye devam edelim. Her tekrar seni hedefe yaklaştırıyor! 🚀"
        )
        return motivationalMessages.random()
    }
}