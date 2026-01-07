package com.hocalingo.app.core.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.logEvent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AnalyticsManager - Kullanıcı Davranışı Analiz Yöneticisi
 *
 * Package: app/src/main/java/com/hocalingo/app/core/analytics/
 *
 * 🎯 Ne İşe Yarar?
 * - Kullanıcı hangi ekranları açıyor? → Track ediyoruz
 * - Hangi butonlara tıklanıyor? → Görüyoruz
 * - Kelime öğrenme süreleri → Ölçüyoruz
 * - Premium satışları → Takip ediyoruz
 *
 * 📊 Firebase Console'da Göreceğin Şeyler:
 * - En çok kullanılan özellikler
 * - Kullanıcı akışları (hangi sırayla ekran geçişleri)
 * - Funnel analizi (kaç kişi premium'a geçiyor)
 * - Retention (kullanıcılar geri dönüyor mu?)
 *
 * 💡 Kullanım Örnekleri:
 * ```kotlin
 * // Ekran görüntüleme
 * analyticsManager.logScreenView("home_screen")
 *
 * // Buton tıklama
 * analyticsManager.logEvent("button_click", "start_learning")
 *
 * // Kelime öğrenme
 * analyticsManager.logWordLearned(wordId = "word_123", timeSpentSeconds = 45)
 *
 * // Premium satış
 * analyticsManager.logPurchase(productId = "premium_monthly", price = 29.99)
 * ```
 */
@Singleton
class AnalyticsManager @Inject constructor(
    private val analytics: FirebaseAnalytics
) {

    /**
     * Ekran görüntüleme event'i
     *
     * Her ekran açıldığında çağır
     * Firebase otomatik olarak ekran akışlarını oluşturur
     */
    fun logScreenView(screenName: String, screenClass: String? = null) {
        analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            screenClass?.let {
                param(FirebaseAnalytics.Param.SCREEN_CLASS, it)
            }
        }
    }

    /**
     * Genel event loglama
     *
     * Özel eventler için kullan
     */
    fun logEvent(eventName: String, vararg params: Pair<String, Any>) {
        analytics.logEvent(eventName) {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> param(key, value)
                    is Long -> param(key, value)
                    is Double -> param(key, value)
                    is Int -> param(key, value.toLong())
                    is Float -> param(key, value.toDouble())
                    else -> param(key, value.toString())
                }
            }
        }
    }

    /**
     * Kullanıcı özelliği ayarla
     *
     * Demografik segmentasyon için
     * Örnek: Premium kullanıcılar ayrı analiz edilir
     */
    fun setUserProperty(propertyName: String, propertyValue: String) {
        analytics.setUserProperty(propertyName, propertyValue)
    }

    /**
     * Kullanıcı ID'si ata
     *
     * ⚠️ Privacy: Firebase User ID kullan (email değil!)
     */
    fun setUserId(userId: String) {
        analytics.setUserId(userId)
    }

    // ==========================================
    // 🎯 APP'E ÖZEL EVENTLER - HOCALINGO
    // ==========================================

    /**
     * Kelime öğrenildi eventi
     *
     * Hangi kelimeler öğreniliyor? Ne kadar sürede?
     */
    fun logWordLearned(wordId: String, categoryId: String, timeSpentSeconds: Int) {
        logEvent("word_learned",
            "word_id" to wordId,
            "category_id" to categoryId,
            "time_spent_seconds" to timeSpentSeconds
        )
    }

    /**
     * Test tamamlandı
     *
     * Başarı oranları ve süreleri
     */
    fun logTestCompleted(
        testType: String,
        score: Int,
        totalQuestions: Int,
        durationSeconds: Int
    ) {
        logEvent("test_completed",
            "test_type" to testType,
            "score" to score,
            "total_questions" to totalQuestions,
            "duration_seconds" to durationSeconds,
            "success_rate" to (score.toDouble() / totalQuestions * 100)
        )
    }

    /**
     * Kategori tamamlandı
     *
     * Hangi kategoriler bitiriliyor?
     */
    fun logCategoryCompleted(categoryId: String, wordCount: Int, durationDays: Int) {
        logEvent("category_completed",
            "category_id" to categoryId,
            "word_count" to wordCount,
            "duration_days" to durationDays
        )
    }

    /**
     * Günlük hedef tamamlandı
     *
     * Streak analizi için
     */
    fun logDailyGoalCompleted(streakDays: Int, wordsLearnedToday: Int) {
        logEvent("daily_goal_completed",
            "streak_days" to streakDays,
            "words_learned_today" to wordsLearnedToday
        )
    }

    /**
     * Premium satın alındı
     *
     * 💰 En önemli event! Revenue tracking
     */
    fun logPurchase(productId: String, price: Double, currency: String = "TRY") {
        analytics.logEvent(FirebaseAnalytics.Event.PURCHASE) {
            param(FirebaseAnalytics.Param.ITEM_ID, productId)
            param(FirebaseAnalytics.Param.CURRENCY, currency)
            param(FirebaseAnalytics.Param.VALUE, price)
        }
    }

    /**
     * Premium deneme başladı
     *
     * Trial conversion tracking
     */
    fun logTrialStarted(trialType: String) {
        logEvent("trial_started",
            "trial_type" to trialType
        )
    }

    /**
     * Reklam izlendi
     *
     * Ad revenue tracking
     */
    fun logAdWatched(adType: String, rewardEarned: Boolean = false) {
        logEvent("ad_watched",
            "ad_type" to adType,
            "reward_earned" to if (rewardEarned) "true" else "false"
        )
    }

    /**
     * Paylaşım yapıldı
     *
     * Viral growth tracking
     */
    fun logShare(contentType: String, method: String) {
        analytics.logEvent(FirebaseAnalytics.Event.SHARE) {
            param(FirebaseAnalytics.Param.CONTENT_TYPE, contentType)
            param(FirebaseAnalytics.Param.METHOD, method)
        }
    }

    /**
     * Bildirim açıldı
     *
     * Notification effectiveness
     */
    fun logNotificationOpened(notificationType: String) {
        logEvent("notification_opened",
            "notification_type" to notificationType
        )
    }

    /**
     * Hata oluştu (fatal olmayan)
     *
     * Analytics'te error tracking
     */
    fun logError(errorType: String, errorMessage: String) {
        logEvent("error_occurred",
            "error_type" to errorType,
            "error_message" to errorMessage
        )
    }
}