package com.hocalingo.app.data.models

import kotlinx.serialization.Serializable

/**
 * ExampleJson - Örnek cümle verisi
 *
 * Package: data/models/
 *
 * ✅ Her kelimenin bir örnek cümlesi var
 * ✅ Hem İngilizce hem Türkçe versiyonu
 *
 * Örnek:
 * ```json
 * "example": {
 *   "en": "Don't abandon your dreams",
 *   "tr": "Hayallerini terk etme"
 * }
 * ```
 */
@Serializable
data class ExampleJson(
    val en: String,                             // İngilizce örnek cümle
    val tr: String                              // Türkçe örnek cümle
)