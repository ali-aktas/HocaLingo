package com.hocalingo.app.database

import android.content.Context
import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.data.models.VocabularyPackageJson
import com.hocalingo.app.data.models.toConceptEntity
import com.hocalingo.app.database.entities.WordPackageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LocalPackageLoader - Assets'teki 1600 kelimeyi yükleyen sistem
 *
 * Package: database/
 *
 * ✅ İlk açılışta 16 JSON dosyasını okur
 * ✅ Database'e concepts + packages olarak kaydeder
 * ✅ Duplicate kontrolü otomatik (Room OnConflictStrategy.REPLACE)
 * ✅ Progress tracking için log'lar içerir
 *
 * KULLANIM:
 * ```kotlin
 * // SplashViewModel'de
 * localPackageLoader.loadBundledPackagesIfNeeded()
 * ```
 */
@Singleton
class LocalPackageLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: HocaLingoDatabase
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }

    /**
     * İlk açılışta assets'ten 16 paketi yükle
     *
     * Kontrol: Eğer database'de zaten 16+ paket varsa atlıyor
     *
     * @return Result<Int> - Yüklenen toplam kelime sayısı
     */
    suspend fun loadBundledPackagesIfNeeded(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // Kontrol: Zaten yüklenmiş mi?
            val existingPackages = database.wordPackageDao().getActivePackages()
            val existingPackageCount = existingPackages.size

            if (existingPackageCount >= 16) {
                val totalWords = database.conceptDao().getConceptCount()
                DebugHelper.log("📦 Paketler zaten yüklü ($existingPackageCount paket, $totalWords kelime)")
                return@withContext Result.Success(totalWords)
            }

            DebugHelper.log("🚀 Assets'ten 16 paket yükleniyor...")
            DebugHelper.log("📊 Mevcut: $existingPackageCount paket")

            // 16 JSON dosyasının listesi
            val packageFiles = listOf(
                "en_tr_a1_001.json", "en_tr_a1_002.json", "en_tr_a1_003.json",
                "en_tr_a2_001.json", "en_tr_a2_002.json", "en_tr_a2_003.json",
                "en_tr_b1_001.json", "en_tr_b1_002.json", "en_tr_b1_003.json",
                "en_tr_b2_001.json", "en_tr_b2_002.json", "en_tr_b2_003.json",
                "en_tr_c1_001.json", "en_tr_c1_002.json",
                "en_tr_c2_001.json", "en_tr_c2_002.json"
            )

            var successCount = 0
            var totalWords = 0
            val errors = mutableListOf<String>()

            // Her dosyayı sırayla yükle
            packageFiles.forEach { fileName ->
                try {
                    // JSON dosyasını parse et
                    val packageData = loadPackageFromAssets(fileName)

                    // Zaten yüklü mü kontrol et
                    val existingPackage = database.wordPackageDao()
                        .getPackageById(packageData.packageInfo.id)

                    if (existingPackage != null) {
                        DebugHelper.log("⏭️  $fileName zaten yüklü, atlanıyor")
                        return@forEach
                    }

                    // 1. Convert to ConceptEntity
                    val concepts = packageData.words.map { wordJson ->
                        wordJson.toConceptEntity(packageData.packageInfo.id)
                    }

                    // 2. Insert concepts (duplicate kontrolü otomatik)
                    database.conceptDao().insertConcepts(concepts)

                    // 3. Insert package record
                    database.wordPackageDao().insertPackage(
                        WordPackageEntity(
                            packageId = packageData.packageInfo.id,
                            version = packageData.packageInfo.version,
                            level = packageData.packageInfo.level,
                            languagePair = packageData.packageInfo.languagePair,
                            totalWords = packageData.packageInfo.totalWords,
                            downloadedAt = System.currentTimeMillis(),
                            isActive = true,
                            description = packageData.packageInfo.description
                        )
                    )

                    totalWords += concepts.size
                    successCount++

                    DebugHelper.logSuccess("✅ $fileName yüklendi (${concepts.size} kelime)")

                } catch (e: Exception) {
                    val errorMsg = "❌ $fileName yüklenemedi: ${e.message}"
                    DebugHelper.logError(errorMsg, e)
                    errors.add(errorMsg)
                }
            }

            // Özet log
            DebugHelper.log("📊 ═══════════════════════════════════")
            DebugHelper.logSuccess("🎉 YÜKLEME TAMAMLANDI!")
            DebugHelper.log("✅ Başarılı: $successCount paket")
            DebugHelper.log("📝 Toplam Kelime: $totalWords")

            if (errors.isNotEmpty()) {
                DebugHelper.log("⚠️  Hatalar: ${errors.size}")
                errors.forEach { DebugHelper.logError(it) }
            }

            DebugHelper.log("═══════════════════════════════════")

            Result.Success(totalWords)

        } catch (e: Exception) {
            DebugHelper.logError("💥 LocalPackageLoader kritik hata", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Assets klasöründen JSON dosyasını oku ve parse et
     *
     * @param fileName Dosya adı (örn: "en_tr_a1_001.json")
     * @return Parse edilmiş VocabularyPackageJson
     * @throws Exception JSON parse hatası veya dosya bulunamadı
     */
    private fun loadPackageFromAssets(fileName: String): VocabularyPackageJson {
        val jsonString = context.assets
            .open("vocabulary/$fileName")
            .bufferedReader()
            .use { it.readText() }

        return json.decodeFromString(jsonString)
    }

    /**
     * Belirli bir seviyenin paketlerini yükle (opsiyonel)
     *
     * Örnek: Sadece A1 paketlerini yükle
     *
     * @param level "A1", "B1", etc.
     * @return Yüklenen kelime sayısı
     */
    suspend fun loadPackagesByLevel(level: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val levelPackages = when (level) {
                "A1" -> listOf("en_tr_a1_001.json", "en_tr_a1_002.json", "en_tr_a1_003.json")
                "A2" -> listOf("en_tr_a2_001.json", "en_tr_a2_002.json", "en_tr_a2_003.json")
                "B1" -> listOf("en_tr_b1_001.json", "en_tr_b1_002.json", "en_tr_b1_003.json")
                "B2" -> listOf("en_tr_b2_001.json", "en_tr_b2_002.json", "en_tr_b2_003.json")
                "C1" -> listOf("en_tr_c1_001.json", "en_tr_c1_002.json")
                "C2" -> listOf("en_tr_c2_001.json", "en_tr_c2_002.json")
                else -> return@withContext Result.Error(AppError.Unknown(Exception("Geçersiz level: $level")))
            }

            var totalWords = 0

            levelPackages.forEach { fileName ->
                try {
                    val packageData = loadPackageFromAssets(fileName)
                    val concepts = packageData.words.map { it.toConceptEntity(packageData.packageInfo.id) }

                    database.conceptDao().insertConcepts(concepts)
                    database.wordPackageDao().insertPackage(
                        WordPackageEntity(
                            packageId = packageData.packageInfo.id,
                            version = packageData.packageInfo.version,
                            level = packageData.packageInfo.level,
                            languagePair = packageData.packageInfo.languagePair,
                            totalWords = packageData.packageInfo.totalWords,
                            downloadedAt = System.currentTimeMillis(),
                            isActive = true,
                            description = packageData.packageInfo.description
                        )
                    )

                    totalWords += concepts.size
                } catch (e: Exception) {
                    DebugHelper.logError("$fileName yüklenemedi", e)
                }
            }

            Result.Success(totalWords)

        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }
}