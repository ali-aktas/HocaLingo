package com.hocalingo.app.core.database

import android.content.Context
import com.hocalingo.app.core.common.base.AppError
import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.core.database.HocaLingoDatabase
import com.hocalingo.app.core.database.entities.ConceptEntity
import com.hocalingo.app.core.database.entities.WordPackageEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JsonLoader
 * Loads word data from JSON files in assets and imports to Room database
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
     * Load test words from assets/test_words.json
     */
    suspend fun loadTestWords(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            println("HocaLingo: Test kelimeler yükleniyor...")

            // Check if already loaded
            val existingPackage = database.wordPackageDao().getPackageById("a1_en_tr_test_v1")
            if (existingPackage != null) {
                println("HocaLingo: Test paketi zaten yüklü")
                val conceptCount = database.conceptDao().getConceptsByPackage("a1_en_tr_test_v1").size
                return@withContext Result.Success(conceptCount)
            }

            val jsonString = context.assets.open("test_words.json")
                .bufferedReader()
                .use { it.readText() }

            println("HocaLingo: JSON okundu, parse ediliyor...")

            val wordPackage = json.decodeFromString<WordPackageJson>(jsonString)

            println("HocaLingo: ${wordPackage.words.size} kelime parse edildi")

            // Convert JSON to entities
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

            // Insert into database
            database.wordPackageDao().insertPackage(packageEntity)
            database.conceptDao().insertConcepts(concepts)

            println("HocaLingo: ${concepts.size} kelime database'e kaydedildi")

            Result.Success(concepts.size)
        } catch (e: Exception) {
            println("HocaLingo: Test kelime yükleme hatası: ${e.message}")
            e.printStackTrace()
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Load words from a specific JSON file
     */
    suspend fun loadWordsFromAssets(fileName: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val wordPackage = json.decodeFromString<WordPackageJson>(jsonString)

            // Check if package already exists
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
     * Load multiple word packages from assets
     */
    suspend fun loadMultiplePackages(fileNames: List<String>): Result<Map<String, Int>> = withContext(Dispatchers.IO) {
        try {
            val results = mutableMapOf<String, Int>()

            for (fileName in fileNames) {
                when (val result = loadWordsFromAssets(fileName)) {
                    is Result.Success -> results[fileName] = result.data
                    is Result.Error -> return@withContext result
                }
            }

            Result.Success(results)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Check if test data is already loaded
     */
    suspend fun isTestDataLoaded(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val packageInfo = database.wordPackageDao().getPackageById("a1_en_tr_test_v1")
            Result.Success(packageInfo != null)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }


    /**
     * Get all available JSON files in assets
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
     * Clear all word data (for development/testing)
     */
    suspend fun clearAllWords(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get all packages
            val packages = database.wordPackageDao().getActivePackages()

            // Delete concepts for each package
            packages.forEach { pkg ->
                database.conceptDao().deleteConceptsByPackage(pkg.packageId)
            }

            // Delete packages
            packages.forEach { pkg ->
                database.wordPackageDao().deletePackage(pkg)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Validate JSON structure before loading
     */
    suspend fun validateJsonFile(fileName: String): Result<WordPackageInfo> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val wordPackage = json.decodeFromString<WordPackageJson>(jsonString)

            // Basic validation
            if (wordPackage.words.isEmpty()) {
                return@withContext Result.Error(AppError.ValidationError)
            }

            if (wordPackage.packageInfo.totalWords != wordPackage.words.size) {
                return@withContext Result.Error(AppError.ValidationError)
            }

            // Check for duplicate IDs
            val ids = wordPackage.words.map { it.id }
            if (ids.size != ids.distinct().size) {
                return@withContext Result.Error(AppError.DuplicateWord)
            }

            Result.Success(wordPackage.packageInfo)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Get loading progress callback
     */
    suspend fun loadWordsWithProgress(
        fileName: String,
        onProgress: (current: Int, total: Int) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open(fileName).bufferedReader().use { it.readText() }
            val wordPackage = json.decodeFromString<WordPackageJson>(jsonString)

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

                // Report progress every 10 items
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
 * JSON Data Classes for serialization
 */
@Serializable
data class WordPackageJson(
    val packageInfo: WordPackageInfo,
    val words: List<WordJson>
)

@Serializable
data class WordPackageInfo(
    val id: String,
    val version: String,
    val level: String,
    val languagePair: String,
    val totalWords: Int,
    val updatedAt: String,
    val description: String? = null,
    val attribution: String? = null
)

@Serializable
data class WordJson(
    val id: Int,
    val english: String,
    val turkish: String,
    val example: ExampleJson? = null,
    val pronunciation: String? = null,
    val level: String,
    val category: String,
    val reversible: Boolean = true,
    val userAdded: Boolean = false
)

@Serializable
data class ExampleJson(
    val en: String,
    val tr: String
)