package com.hocalingo.app.core.database

import android.content.Context
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.common.base.AppError
import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.core.database.HocaLingoDatabase
import com.hocalingo.app.core.database.entities.ConceptEntity
import com.hocalingo.app.core.database.entities.WordPackageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JsonLoader - CLEAN VERSION
 * Serialization tamamen düzeltildi
 */
@Singleton
class JsonLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: HocaLingoDatabase
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Load test words - CLEAN VERSION
     */
    suspend fun loadTestWords(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            DebugHelper.logDatabase("=== CLEAN TEST KELİME YÜKLEMESI ===")

            // Duplicate check
            val existingPackage = database.wordPackageDao().getPackageById("a1_en_tr_test_v1")
            if (existingPackage != null) {
                val conceptCount = database.conceptDao().getConceptsByPackage("a1_en_tr_test_v1").size
                DebugHelper.logDatabase("Paket zaten var: $conceptCount kelime")
                return@withContext Result.Success(conceptCount)
            }

            // JSON okuma
            val jsonString = context.assets.open("test_words.json")
                .bufferedReader()
                .use { it.readText() }

            DebugHelper.logDatabase("JSON okundu: ${jsonString.length} karakter")
            DebugHelper.logDatabase("JSON preview: ${jsonString.take(200)}...")

            // Parse etme - DİKKATLİ
            val wordPackage = json.decodeFromString<TestWordsJson>(jsonString)
            DebugHelper.logDatabase("Parse başarılı: ${wordPackage.words.size} kelime")
            DebugHelper.logDatabase("Package ID: ${wordPackage.packageInfo.id}")

            // Database entities
            val packageEntity = WordPackageEntity(
                packageId = wordPackage.packageInfo.id,
                version = wordPackage.packageInfo.version,
                level = wordPackage.packageInfo.level,
                languagePair = wordPackage.packageInfo.languagePair,
                totalWords = wordPackage.packageInfo.totalWords,
                downloadedAt = System.currentTimeMillis(),
                isActive = true,
                description = wordPackage.packageInfo.description
            )

            val concepts = wordPackage.words.map { word ->
                ConceptEntity(
                    id = word.id,
                    english = word.english,
                    turkish = word.turkish,
                    exampleEn = word.example?.en,
                    exampleTr = word.example?.tr,
                    pronunciation = word.pronunciation,
                    level = word.level,
                    category = word.category,
                    reversible = word.reversible,
                    userAdded = word.userAdded,
                    packageId = wordPackage.packageInfo.id
                )
            }

            // Database'e kaydetme
            DebugHelper.logDatabase("Package kaydediliyor...")
            database.wordPackageDao().insertPackage(packageEntity)
            DebugHelper.logDatabase("Package kaydedildi!")

            DebugHelper.logDatabase("${concepts.size} concept kaydediliyor...")
            database.conceptDao().insertConcepts(concepts)
            DebugHelper.logDatabase("Concepts kaydedildi!")

            // Validation
            val savedConceptCount = database.conceptDao().getConceptsByPackage(wordPackage.packageInfo.id).size
            val savedPackage = database.wordPackageDao().getPackageById(wordPackage.packageInfo.id)

            DebugHelper.logDatabase("DOĞRULAMA:")
            DebugHelper.logDatabase("- Package: ${savedPackage != null}")
            DebugHelper.logDatabase("- Concepts: $savedConceptCount")

            if (savedPackage != null && savedConceptCount > 0) {
                DebugHelper.logSuccess("YÜKLEME BAŞARILI: $savedConceptCount kelime")
                Result.Success(savedConceptCount)
            } else {
                throw Exception("Kaydetme başarısız: Package=${savedPackage != null}, Concepts=$savedConceptCount")
            }

        } catch (e: Exception) {
            DebugHelper.logError("YÜKLEME HATASI", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Check if test data loaded
     */
    suspend fun isTestDataLoaded(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val packageInfo = database.wordPackageDao().getPackageById("a1_en_tr_test_v1")
            val conceptCount = if (packageInfo != null) {
                database.conceptDao().getConceptsByPackage("a1_en_tr_test_v1").size
            } else {
                0
            }

            val isLoaded = packageInfo != null && conceptCount > 0
            DebugHelper.logDatabase("Test loaded check: Package=${packageInfo != null}, Concepts=$conceptCount, Result=$isLoaded")

            Result.Success(isLoaded)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Load words from assets file
     */
    suspend fun loadWordsFromAssets(fileName: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val wordPackage = json.decodeFromString<TestWordsJson>(jsonString)

            // Duplicate check
            val existingPackage = database.wordPackageDao().getPackageById(wordPackage.packageInfo.id)
            if (existingPackage != null) {
                return@withContext Result.Error(AppError.DuplicateWord)
            }

            // Convert and insert
            val concepts = wordPackage.words.map { word ->
                ConceptEntity(
                    id = word.id,
                    english = word.english,
                    turkish = word.turkish,
                    exampleEn = word.example?.en,
                    exampleTr = word.example?.tr,
                    pronunciation = word.pronunciation,
                    level = word.level,
                    category = word.category,
                    reversible = word.reversible,
                    userAdded = word.userAdded,
                    packageId = wordPackage.packageInfo.id
                )
            }

            val packageEntity = WordPackageEntity(
                packageId = wordPackage.packageInfo.id,
                version = wordPackage.packageInfo.version,
                level = wordPackage.packageInfo.level,
                languagePair = wordPackage.packageInfo.languagePair,
                totalWords = wordPackage.packageInfo.totalWords,
                downloadedAt = System.currentTimeMillis(),
                isActive = true,
                description = wordPackage.packageInfo.description
            )

            database.wordPackageDao().insertPackage(packageEntity)
            database.conceptDao().insertConcepts(concepts)

            Result.Success(concepts.size)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Get available packages
     */
    suspend fun getAvailableWordPackages(): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val assetFiles = context.assets.list("") ?: emptyArray()
            val jsonFiles = assetFiles.filter { it.endsWith(".json") && it.contains("words") }
            Result.Success(jsonFiles)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Clear all words
     */
    suspend fun clearAllWords(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val packages = database.wordPackageDao().getActivePackages()
            packages.forEach { pkg ->
                database.conceptDao().deleteConceptsByPackage(pkg.packageId)
                database.wordPackageDao().deletePackage(pkg)
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Load with progress
     */
    suspend fun loadWordsWithProgress(
        fileName: String,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val wordPackage = json.decodeFromString<TestWordsJson>(jsonString)

            onProgress(0, wordPackage.words.size)

            val concepts = wordPackage.words.mapIndexed { index, word ->
                val concept = ConceptEntity(
                    id = word.id,
                    english = word.english,
                    turkish = word.turkish,
                    exampleEn = word.example?.en,
                    exampleTr = word.example?.tr,
                    pronunciation = word.pronunciation,
                    level = word.level,
                    category = word.category,
                    reversible = word.reversible,
                    userAdded = word.userAdded,
                    packageId = wordPackage.packageInfo.id
                )

                if (index % 10 == 0) {
                    onProgress(index, wordPackage.words.size)
                }

                concept
            }

            val packageEntity = WordPackageEntity(
                packageId = wordPackage.packageInfo.id,
                version = wordPackage.packageInfo.version,
                level = wordPackage.packageInfo.level,
                languagePair = wordPackage.packageInfo.languagePair,
                totalWords = wordPackage.packageInfo.totalWords,
                downloadedAt = System.currentTimeMillis(),
                isActive = true,
                description = wordPackage.packageInfo.description
            )

            database.wordPackageDao().insertPackage(packageEntity)
            database.conceptDao().insertConcepts(concepts)

            onProgress(concepts.size, concepts.size)
            Result.Success(concepts.size)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }
}

/**
 * JSON CLASSES - CLEAN VERSION with correct annotations
 */
@Serializable
data class TestWordsJson(
    @SerialName("package_info")
    val packageInfo: TestPackageInfo,
    val words: List<TestWordJson>
)

@Serializable
data class TestPackageInfo(
    val id: String,
    val version: String,
    val level: String,
    @SerialName("language_pair")
    val languagePair: String,
    @SerialName("total_words")
    val totalWords: Int,
    @SerialName("updated_at")
    val updatedAt: String,
    val description: String? = null,
    val attribution: String? = null
)

@Serializable
data class TestWordJson(
    val id: Int,
    val english: String,
    val turkish: String,
    val example: TestExampleJson? = null,
    val pronunciation: String? = null,
    val level: String,
    val category: String,
    val reversible: Boolean = true,
    val userAdded: Boolean = false
)

@Serializable
data class TestExampleJson(
    val en: String,
    val tr: String
)