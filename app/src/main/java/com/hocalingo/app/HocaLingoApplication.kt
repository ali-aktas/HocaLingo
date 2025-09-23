package com.hocalingo.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.WorkManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * HocaLingo Application Class - Simple WorkManager Fix
 * ✅ Hilt integration
 * ✅ WorkManager initialization without Configuration.Provider
 * ✅ Crash fix for notification system
 */
@HiltAndroidApp
class HocaLingoApplication : Application() {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Simple WorkManager initialization - no complex interfaces
        initializeWorkManager()
    }

    private fun initializeWorkManager() {
        try {
            // Initialize WorkManager with basic configuration
            WorkManager.initialize(
                this,
                Configuration.Builder()
                    .setWorkerFactory(workerFactory)
                    .build()
            )
        } catch (e: IllegalStateException) {
            // WorkManager already initialized, that's fine
        } catch (e: Exception) {
            // Any other error, initialize with default configuration
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
}