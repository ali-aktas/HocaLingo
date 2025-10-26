package com.hocalingo.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * VocabularyPackageJson - Assets'teki JSON dosyalarını parse etmek için
 *
 * Package: data/models/
 *
 * ✅ Kotlinx Serialization kullanıyor
 * ✅ 16 JSON dosyasının formatına uygun
 * ✅ Master plan ile 100% uyumlu
 *
 * Örnek JSON yapısı:
 * ```json
 * {
 *   "package_info": { "id": "en_tr_a1_001", ... },
 *   "words": [ { "id": 1000, "english": "hello", ... } ]
 * }
 * ```
 */
@Serializable
data class VocabularyPackageJson(
    @SerialName("package_info")
    val packageInfo: PackageInfoJson,

    val words: List<WordJson>
)