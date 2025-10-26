package com.hocalingo.app.data.models

import com.hocalingo.app.database.entities.ConceptEntity
import kotlinx.serialization.Serializable

/**
 * WordJson - JSON dosyalarındaki kelime verisi
 *
 * Package: data/models/
 *
 * ✅ 1600 kelimelik JSON dosyalarının word formatı
 * ✅ ConceptEntity'ye dönüştürme extension'ı içerir
 *
 * Örnek:
 * ```json
 * {
 *   "id": 3000,
 *   "english": "abandon",
 *   "turkish": "terk etmek",
 *   "example": {
 *     "en": "Don't abandon your dreams",
 *     "tr": "Hayallerini terk etme"
 *   },
 *   "pronunciation": "əˈbændən",
 *   "level": "B1",
 *   "category": "verbs",
 *   "reversible": true,
 *   "userAdded": false
 * }
 * ```
 */
@Serializable
data class WordJson(
    val id: Int,                                // Unique ID (1000-6199)

    val english: String,                        // İngilizce kelime

    val turkish: String,                        // Türkçe karşılık

    val example: ExampleJson,                   // Örnek cümle

    val pronunciation: String,                  // IPA telaffuz

    val level: String,                          // A1, A2, B1, B2, C1, C2

    val category: String,                       // verbs, nouns, adjectives, etc.

    val reversible: Boolean,                    // Çift yönlü çalışılabilir mi?

    val userAdded: Boolean                      // Kullanıcı ekledi mi?
)

/**
 * Extension: JSON → ConceptEntity dönüşümü
 *
 * @param packageId Hangi paketten geldiği (örn: "en_tr_a1_001")
 * @return Database'e kaydedilecek ConceptEntity
 */
fun WordJson.toConceptEntity(packageId: String) = ConceptEntity(
    id = id,
    english = english,
    turkish = turkish,
    exampleEn = example.en,
    exampleTr = example.tr,
    pronunciation = pronunciation,
    level = level,
    category = category,
    reversible = reversible,
    userAdded = userAdded,
    packageId = packageId,                      // ✅ Paket tracking
    createdAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)