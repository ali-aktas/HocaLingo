package com.hocalingo.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.hocalingo.app.core.common.DebugHelper
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * HocaLingo Application Class
 *
 * ✅ Hilt integration
 * ✅ WorkManager initialization
 * ✅ RevenueCat SDK initialization
 */
@HiltAndroidApp
class HocaLingoApplication : Application() {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // WorkManager initialization
        initializeWorkManager()

        // ✅ RevenueCat initialization
        initializeRevenueCat()
    }

    private fun initializeWorkManager() {
        try {
            WorkManager.initialize(
                this,
                Configuration.Builder()
                    .setWorkerFactory(workerFactory)
                    .build()
            )
        } catch (e: IllegalStateException) {
            // WorkManager already initialized
        } catch (e: Exception) {
            try {
                WorkManager.initialize(
                    this,
                    Configuration.Builder().build()
                )
            } catch (ignored: Exception) {
                // If all fails, app will work without notifications
            }
        }
    }

    /**
     * RevenueCat SDK'sını başlatır
     *
     * ⚠️ ÖNEMLI: API Key güvenliği için BuildConfig kullanılmalı
     * Şimdilik direkt kod içinde ama production'da BuildConfig'e taşınmalı!
     */
    private fun initializeRevenueCat() {
        try {
            DebugHelper.log("🚀 Initializing RevenueCat SDK...")

            // ✅ RevenueCat API Key
            val apiKey = "goog_hUrMIAPlvIzmWpBMQMiZiaUTHMs"

            // Debug mode için log level ayarla
            if (BuildConfig.DEBUG) {
                Purchases.logLevel = LogLevel.DEBUG
            }

            // RevenueCat configure
            Purchases.configure(
                PurchasesConfiguration.Builder(this, apiKey)
                    .build()
            )

            DebugHelper.logSuccess("✅ RevenueCat SDK initialized successfully!")

        } catch (e: Exception) {
            DebugHelper.logError("❌ Failed to initialize RevenueCat", e)
        }
    }
}