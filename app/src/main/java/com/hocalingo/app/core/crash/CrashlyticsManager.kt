package com.hocalingo.app.core.crash

import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hocalingo.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CrashlyticsManager - Crash Raporlama Yöneticisi
 *
 * Package: app/src/main/java/com/hocalingo/app/core/crash/
 *
 * 🎯 Ne İşe Yarar?
 * - Crash'leri Firebase'e raporlar
 * - Kullanıcı bilgilerini loglar
 * - Custom log mesajları ekler
 * - Debug'da devre dışı (gereksiz trafik önlenir)
 *
 * 💡 Kullanım Örnekleri:
 * ```kotlin
 * // Hata loglama
 * crashlyticsManager.logError("API failed", exception)
 *
 * // Kullanıcı bilgisi
 * crashlyticsManager.setUserId("user123")
 *
 * // Özel bilgi
 * crashlyticsManager.setCustomKey("premium_user", true)
 * ```
 */
@Singleton
class CrashlyticsManager @Inject constructor() {

    private val crashlytics = FirebaseCrashlytics.getInstance()

    init {
        // Debug modda Crashlytics'i devre dışı bırak
        // Gereksiz raporlar göndermesin
        crashlytics.setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)

        // App versiyonunu kaydet
        setCustomKey("app_version", BuildConfig.VERSION_NAME)
        setCustomKey("version_code", BuildConfig.VERSION_CODE)
    }

    /**
     * Hata loglama - Fatal olmayan hatalar
     *
     * Örnek: API çağrısı başarısız oldu ama app çalışmaya devam ediyor
     */
    fun logError(message: String, throwable: Throwable? = null) {
        log(message)
        throwable?.let {
            crashlytics.recordException(it)
        }
    }

    /**
     * Basit log mesajı
     *
     * Crash olduğunda bu log'ları görebilirsin
     */
    fun log(message: String) {
        crashlytics.log(message)
    }

    /**
     * Kullanıcı ID'si kaydet
     *
     * Hangi kullanıcıda sorun çıktığını görmek için
     * ⚠️ Privacy: Anonim ID kullan (email değil!)
     */
    fun setUserId(userId: String) {
        crashlytics.setUserId(userId)
    }

    /**
     * Özel anahtar-değer çifti ekle
     *
     * Crash analizinde ek bilgi olarak görünür
     * Örnek: Premium kullanıcı mı? Hangi dil seçili?
     */
    fun setCustomKey(key: String, value: Any) {
        when (value) {
            is String -> crashlytics.setCustomKey(key, value)
            is Boolean -> crashlytics.setCustomKey(key, value)
            is Int -> crashlytics.setCustomKey(key, value)
            is Long -> crashlytics.setCustomKey(key, value)
            is Float -> crashlytics.setCustomKey(key, value)
            is Double -> crashlytics.setCustomKey(key, value)
            else -> crashlytics.setCustomKey(key, value.toString())
        }
    }

    /**
     * Test için manuel crash tetikle
     *
     * ⚠️ SADECE TEST AMAÇLI!
     * Gerçek üretimde kullanma!
     */
    fun forceCrash() {
        throw RuntimeException("Test crash from CrashlyticsManager")
    }

    /**
     * Kullanıcı bilgilerini temizle
     *
     * Logout olduğunda çağır
     */
    fun clearUserData() {
        setUserId("")
    }
}