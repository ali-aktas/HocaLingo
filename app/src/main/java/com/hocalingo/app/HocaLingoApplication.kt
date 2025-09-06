package com.hocalingo.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class HocaLingoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        // Firebase.initializeApp(this) // Otomatik olarak initialize olur

        // Initialize other SDKs if needed
        // RevenueCat.configure(this, "your_api_key")
    }
}