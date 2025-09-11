package com.hocalingo.app.core.common

import android.util.Log

/**
 * Debug Helper for HocaLingo - FULL VERSION
 * Centralized logging for debugging
 */
object DebugHelper {
    private const val TAG = "HocaLingo"
    private const val DEBUG_MODE = true // BuildConfig.DEBUG olarak değiştirilecek

    fun log(message: String, tag: String = TAG) {
        if (DEBUG_MODE) {
            Log.d(tag, "🟢 $message")
        }
    }

    fun logError(message: String, throwable: Throwable? = null, tag: String = TAG) {
        if (DEBUG_MODE) {
            Log.e(tag, "🔴 ERROR: $message", throwable)
        }
    }

    fun logWarning(message: String, tag: String = TAG) {
        if (DEBUG_MODE) {
            Log.w(tag, "🟡 WARNING: $message")
        }
    }

    fun logDatabase(message: String) {
        log("📊 DB: $message", "$TAG-DB")
    }

    fun logAuth(message: String) {
        log("🔐 AUTH: $message", "$TAG-AUTH")
    }

    fun logNavigation(message: String) {
        log("🧭 NAV: $message", "$TAG-NAV")
    }

    fun logWordSelection(message: String) {
        log("🎯 WORDS: $message", "$TAG-WORDS")
    }

    fun logSuccess(message: String) {
        log("✅ SUCCESS: $message")
    }

    fun logInfo(message: String) {
        log("ℹ️ INFO: $message")
    }
}