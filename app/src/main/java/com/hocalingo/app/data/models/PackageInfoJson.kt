package com.hocalingo.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * PackageInfoJson - JSON dosyalarındaki package_info alanı
 *
 * Package: data/models/
 *
 * ✅ Master plandaki JSON formatına tam uyumlu
 * ✅ 1600 kelimelik paketlerin metadata'sını parse eder
 *
 * Örnek:
 * ```json
 * "package_info": {
 *   "id": "en_tr_b1_001",
 *   "version": "1.0.0",
 *   "level": "B1",
 *   "language_pair": "en_tr",
 *   "total_words": 100,
 *   "updated_at": "2025-10-24",
 *   "description": "B1 level English-Turkish vocabulary package 1",
 *   "attribution": "HocaLingo vocabulary database"
 * }
 * ```
 */
@Serializable
data class PackageInfoJson(
    val id: String,                             // "en_tr_a1_001"

    val version: String,                        // "1.0.0"

    val level: String,                          // "A1", "B1", etc.

    @SerialName("language_pair")
    val languagePair: String,                   // "en_tr"

    @SerialName("total_words")
    val totalWords: Int,                        // 100

    @SerialName("updated_at")
    val updatedAt: String,                      // "2025-10-24"

    val description: String,                     // Package açıklaması

    val attribution: String                      // "HocaLingo vocabulary database"
)