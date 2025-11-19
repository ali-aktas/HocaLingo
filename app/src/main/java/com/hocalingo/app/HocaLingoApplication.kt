package com.hocalingo.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import com.hocalingo.app.BuildConfig
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
     */
    private fun initializeRevenueCat() {
        try {
            DebugHelper.log("🚀 Initializing RevenueCat SDK...")

            val apiKey = BuildConfig.REVENUECAT_API_KEY

            // 🧪 TEST: API key doğru mu?
            DebugHelper.log("🔑 API Key length: ${apiKey.length}")

            if (apiKey.isBlank()) {
                DebugHelper.logError("❌ RevenueCat API key is empty!")
                return
            }

            if (BuildConfig.DEBUG) {
                Purchases.logLevel = LogLevel.DEBUG
            }

            Purchases.configure(
                PurchasesConfiguration.Builder(this, apiKey).build()
            )

            DebugHelper.logSuccess("✅ RevenueCat SDK initialized successfully!")

        } catch (e: Exception) {
            DebugHelper.logError("❌ Failed to initialize RevenueCat", e)
        }
    }
}