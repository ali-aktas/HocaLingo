package com.hocalingo.app.data

import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.database.HocaLingoDatabase
import com.hocalingo.app.database.entities.ConceptEntity
import com.hocalingo.app.database.entities.WordPackageEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * WordPackageRepository
 *
 * Package: app/src/main/java/com/hocalingo/app/data/
 *
 * Kelime paketi indirme ve yönetimi için repository
 * - Firebase DataSource ile koordinasyon
 * - Database işlemleri
 * - Progress tracking relay
 * - Package download status check (NEW)
 */
@Singleton
class WordPackageRepository @Inject constructor(
    private val firebaseDataSource: FirebaseWordPackageDataSource,
    private val database: HocaLingoDatabase
) {

    /**
     * Check if package already downloaded
     */
    suspend fun isPackageDownloaded(packageId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val existingPackage = database.wordPackageDao().getPackageById(packageId)
            val conceptCount = if (existingPackage != null) {
                database.conceptDao().getConceptsByPackage(packageId).size
            } else {
                0
            }

            val isDownloaded = existingPackage != null && conceptCount > 0
            DebugHelper.log("Package check: $packageId -> downloaded=$isDownloaded (concepts=$conceptCount)")

            Result.Success(isDownloaded)

        } catch (e: Exception) {
            DebugHelper.logError("Package check hatası", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * ✨ NEW: Get package download status for badge display
     *
     * Compares Firebase word IDs with local downloaded word IDs to determine status.
     *
     * @param packageId The package to check
     * @return PackageDownloadStatus indicating what badge to show
     */
    suspend fun getPackageDownloadStatus(packageId: String): PackageDownloadStatus = withContext(Dispatchers.IO) {
        try {
            DebugHelper.log("📊 Checking download status for: $packageId")

            // 1. Get Firebase metadata (includes wordIds array)
            val metadataResult = firebaseDataSource.getPackageMetadata(packageId)
            if (metadataResult !is Result.Success) {
                DebugHelper.logError("Metadata alınamadı, default NotDownloaded dönülüyor")
                return@withContext PackageDownloadStatus.NotDownloaded
            }

            val metadata = metadataResult.data
            val firebaseWordIds = metadata.wordIds?.toSet() ?: emptySet()

            if (firebaseWordIds.isEmpty()) {
                DebugHelper.logError("⚠️ Firebase'de wordIds bulunamadı! Firestore'a wordIds array'i eklemen gerekiyor.")
                return@withContext PackageDownloadStatus.NotDownloaded
            }

            DebugHelper.log("Firebase word IDs: ${firebaseWordIds.size} kelime")

            // 2. Get local downloaded word IDs
            val localConcepts = database.conceptDao().getConceptsByPackage(packageId)
            val localWordIds = localConcepts.map { it.id }.toSet()

            DebugHelper.log("Local downloaded IDs: ${localWordIds.size} kelime")

            // 3. Compare and determine status
            return@withContext when {
                localWordIds.isEmpty() -> {
                    // No words downloaded
                    DebugHelper.log("Status: NOT_DOWNLOADED")
                    PackageDownloadStatus.NotDownloaded
                }

                firebaseWordIds == localWordIds -> {
                    // All words downloaded
                    DebugHelper.log("Status: FULLY_DOWNLOADED")
                    PackageDownloadStatus.FullyDownloaded
                }

                else -> {
                    // Some words not downloaded (can be old or new)
                    val missingWordIds = firebaseWordIds - localWordIds
                    val newWordsCount = missingWordIds.size
                    DebugHelper.log("Status: HAS_NEW_WORDS ($newWordsCount kelime eksik)")
                    PackageDownloadStatus.HasNewWords(newWordsCount)
                }
            }

        } catch (e: Exception) {
            DebugHelper.logError("Download status kontrolü hatası", e)
            // Return NotDownloaded as fallback
            PackageDownloadStatus.NotDownloaded
        }
    }

    /**
     * Download package with progress tracking
     * Returns Flow of download states
     */
    fun downloadPackageWithProgress(packageId: String): Flow<WordPackageDownloadState> = flow {
        try {
            DebugHelper.log("=== PAKET İNDİRME BAŞLADI: $packageId ===")

            // 1. Check if already exists
            when (val checkResult = isPackageDownloaded(packageId)) {
                is Result.Success -> {
                    if (checkResult.data) {
                        DebugHelper.log("Paket zaten var!")
                        val conceptCount = database.conceptDao().getConceptsByPackage(packageId).size
                        emit(WordPackageDownloadState.Success(conceptCount))
                        return@flow
                    }
                }
                is Result.Error -> {
                    emit(WordPackageDownloadState.Error("Paket kontrolü başarısız"))
                    return@flow
                }
            }

            // 2. Get metadata from Firestore
            val metadata = when (val metadataResult = firebaseDataSource.getPackageMetadata(packageId)) {
                is Result.Success -> metadataResult.data
                is Result.Error -> {
                    emit(WordPackageDownloadState.Error("Paket bilgileri alınamadı"))
                    return@flow
                }
            }

            // 3. Download and parse JSON
            emit(WordPackageDownloadState.Downloading(0, metadata.totalWords.toLong(), 0))

            val wordPackage = when (val downloadResult = firebaseDataSource.downloadAndParsePackage(
                packageId = packageId,
                storageUrl = metadata.storageUrl
            )) {
                is Result.Success -> downloadResult.data
                is Result.Error -> {
                    emit(WordPackageDownloadState.Error("İndirme başarısız: ${downloadResult.error.message}"))
                    return@flow
                }
            }

            // 4. Parsing done
            emit(WordPackageDownloadState.Parsing)

            // 5. Convert to database entities
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

            // 6. Save to database with progress
            DebugHelper.log("Database'e kaydediliyor...")

            database.wordPackageDao().insertPackage(packageEntity)
            emit(WordPackageDownloadState.Saving(0, concepts.size, 0))

            // Insert concepts in batches for better performance
            val batchSize = 10
            concepts.chunked(batchSize).forEachIndexed { index, batch ->
                database.conceptDao().insertConcepts(batch)

                val currentWord = minOf((index + 1) * batchSize, concepts.size)
                val progress = (currentWord * 100) / concepts.size

                emit(WordPackageDownloadState.Saving(
                    currentWord = currentWord,
                    totalWords = concepts.size,
                    progress = progress
                ))
            }

            // 7. Validation
            val savedConceptCount = database.conceptDao().getConceptsByPackage(packageId).size
            val savedPackage = database.wordPackageDao().getPackageById(packageId)

            if (savedPackage != null && savedConceptCount > 0) {
                DebugHelper.logSuccess("✅ YÜKLEME BAŞARILI: $savedConceptCount kelime")
                emit(WordPackageDownloadState.Success(savedConceptCount))
            } else {
                throw Exception("Kaydetme doğrulaması başarısız: Package=${savedPackage != null}, Concepts=$savedConceptCount")
            }

        } catch (e: Exception) {
            DebugHelper.logError("💥 DOWNLOAD REPOSITORY HATASI", e)
            emit(WordPackageDownloadState.Error(
                error = e.message ?: "Bilinmeyen hata",
                throwable = e
            ))
        }
    }

    /**
     * Get all downloaded packages
     */
    suspend fun getDownloadedPackages(): Result<List<WordPackageEntity>> = withContext(Dispatchers.IO) {
        try {
            val packages = database.wordPackageDao().getActivePackages()
            Result.Success(packages)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Delete package and its concepts
     */
    suspend fun deletePackage(packageId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            DebugHelper.log("Paket siliniyor: $packageId")

            val packageEntity = database.wordPackageDao().getPackageById(packageId)
            if (packageEntity != null) {
                database.conceptDao().deleteConceptsByPackage(packageId)
                database.wordPackageDao().deletePackage(packageEntity)
                DebugHelper.logSuccess("Paket silindi")
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            DebugHelper.logError("Paket silme hatası", e)
            Result.Error(AppError.Unknown(e))
        }
    }
}