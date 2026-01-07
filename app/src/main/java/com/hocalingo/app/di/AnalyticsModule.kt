package com.hocalingo.app.di

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * AnalyticsModule - Analytics & Crashlytics Dependency Injection
 *
 * Package: app/src/main/java/com/hocalingo/app/di/
 *
 * 🎯 Ne İşe Yarar?
 * - FirebaseAnalytics'i Hilt container'a ekler
 * - Tüm app'te tek instance kullanılır (Singleton)
 *
 * 📝 NOT:
 * - CrashlyticsManager zaten @Inject constructor kullanıyor
 * - FirebaseAnalytics'i manuel provide etmemiz gerekiyor
 *
 * 💡 Kullanım:
 * ```kotlin
 * @HiltViewModel
 * class MyViewModel @Inject constructor(
 *     private val analyticsManager: AnalyticsManager,
 *     private val crashlyticsManager: CrashlyticsManager
 * ) : ViewModel() {
 *     // Kullanıma hazır!
 * }
 * ```
 */
@Module
@InstallIn(SingletonComponent::class)
object AnalyticsModule {

    /**
     * FirebaseAnalytics instance sağla
     *
     * Singleton olarak tüm app'te aynı instance kullanılır
     */
    @Provides
    @Singleton
    fun provideFirebaseAnalytics(
        @ApplicationContext context: Context
    ): FirebaseAnalytics {
        return Firebase.analytics
    }
}