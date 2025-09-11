package com.hocalingo.app.core.common

import android.util.Log

/**
 * Debug Helper for HocaLingo
 * Centralized logging for debugging
 * YENİ DOSYA - core/common klasörüne eklenecek
 */
object DebugHelper {
    private const val TAG = "HocaLingo"
    private const val DEBUG_MODE = true // BuildConfig.DEBUG olarak değiştirilecek

    fun log(message: String, tag: String = TAG) {
        if (DEBUG_MODE) {
            Log.d(tag, message)
        }
    }

    fun logError(message: String, throwable: Throwable? = null, tag: String = TAG) {
        if (DEBUG_MODE) {
            Log.e(tag, message, throwable)
        }
    }

    fun logWarning(message: String, tag: String = TAG) {
        if (DEBUG_MODE) {
            Log.w(tag, message)
        }
    }

    fun logDatabase(message: String) {
        log("DB: $message", "$TAG-DB")
    }

    fun logAuth(message: String) {
        log("AUTH: $message", "$TAG-AUTH")
    }

    fun logNavigation(message: String) {
        log("NAV: $message", "$TAG-NAV")
    }

    fun logWordSelection(message: String) {
        log("WORDS: $message", "$TAG-WORDS")
    }
}