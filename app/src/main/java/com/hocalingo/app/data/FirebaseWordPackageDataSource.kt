package com.hocalingo.app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.DebugHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FirebaseWordPackageDataSource
 *
 * Package: app/src/main/java/com/hocalingo/app/data/
 *
 * Firebase Storage'dan kelime paketlerini indiren DataSource
 * - Firestore'dan metadata okuma
 * - Storage'dan JSON indirme
 * - Progress tracking
 * - Parse işlemi
 */
@Singleton
class FirebaseWordPackageDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    companion object {
        private const val PACKAGES_COLLECTION = "wordPackages"
        private const val STORAGE_BASE_PATH = "word_packages"
    }

    /**
     * Get package metadata from Firestore
     *
     * ✨ UPDATED: wordIds field'ını da okuyor
     */
    suspend fun getPackageMetadata(packageId: String): Result<PackageMetadata> = withContext(Dispatchers.IO) {
        try {
            DebugHelper.log("Firestore'dan metadata çekiliyor: $packageId")

            val document = firestore.collection(PACKAGES_COLLECTION)
                .document(packageId)
                .get()
                .await()

            if (!document.exists()) {
                return@withContext Result.Error(AppError.NotFound)
            }

            // ✨ YENİ: wordIds array'ini oku (List<Long> olarak gelir, Int'e çeviriyoruz)
            @Suppress("UNCHECKED_CAST")
            val wordIdsRaw = document.get("wordIds") as? List<Long>
            val wordIds = wordIdsRaw?.map { it.toInt() }

            val metadata = PackageMetadata(
                packageId = document.getString("packageId") ?: packageId,
                version = document.getString("version") ?: "1.0.0",
                level = document.getString("level") ?: "A1",
                languagePair = document.getString("languagePair") ?: "en_tr",
                totalWords = document.getLong("totalWords")?.toInt() ?: 0,
                wordIds = wordIds,  // ✨ YENİ: wordIds eklendi
                storageUrl = document.getString("storageUrl") ?: "",
                requiresPremium = document.getBoolean("requiresPremium") ?: false,
                description = document.getString("description")
            )

            DebugHelper.logSuccess("Metadata alındı: ${metadata.totalWords} kelime, wordIds: ${wordIds?.size ?: 0}")
            Result.Success(metadata)

        } catch (e: Exception) {
            DebugHelper.logError("Metadata çekme hatası", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Download package JSON from Firebase Storage with progress tracking
     */
    fun downloadPackageJson(
        packageId: String,
        storageUrl: String
    ): Flow<WordPackageDownloadState> = callbackFlow {
        try {
            DebugHelper.log("Firebase Storage'dan indirme başlıyor: $packageId")
            trySend(WordPackageDownloadState.Idle)

            // Storage reference
            val storageRef = if (storageUrl.startsWith("gs://")) {
                storage.getReferenceFromUrl(storageUrl)
            } else {
                storage.reference.child("$STORAGE_BASE_PATH/$packageId.json")
            }

            // Get metadata to know file size
            val metadata = storageRef.metadata.await()
            val totalBytes = metadata.sizeBytes

            DebugHelper.log("Dosya boyutu: $totalBytes bytes")

            // Download with progress tracking
            val localFile = createTempFile("word_package_", ".json")

            storageRef.getFile(localFile)
                .addOnProgressListener { taskSnapshot ->
                    val bytesDownloaded = taskSnapshot.bytesTransferred
                    val progress = ((bytesDownloaded * 100) / totalBytes).toInt()

                    trySend(WordPackageDownloadState.Downloading(
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes,
                        progress = progress.coerceIn(0, 100)
                    ))
                }
                .addOnSuccessListener {
                    DebugHelper.logSuccess("İndirme tamamlandı!")

                    // Parse JSON
                    trySend(WordPackageDownloadState.Parsing)

                    try {
                        val jsonString = localFile.readText()
                        val wordPackage = json.decodeFromString<FirebaseWordsJson>(jsonString)

                        DebugHelper.logSuccess("Parse başarılı: ${wordPackage.words.size} kelime")

                        // Clean up temp file
                        localFile.delete()

                        // Success state will be handled by repository after DB insert
                        close()

                    } catch (parseError: Exception) {
                        DebugHelper.logError("Parse hatası", parseError)
                        trySend(WordPackageDownloadState.Error(
                            error = "JSON işleme hatası: ${parseError.message}",
                            throwable = parseError
                        ))
                        close(parseError)
                    }
                }
                .addOnFailureListener { exception ->
                    DebugHelper.logError("İndirme hatası", exception)
                    localFile.delete()

                    trySend(WordPackageDownloadState.Error(
                        error = "İndirme başarısız: ${exception.message}",
                        throwable = exception
                    ))
                    close(exception)
                }

            awaitClose {
                DebugHelper.log("Download flow kapatılıyor")
            }

        } catch (e: Exception) {
            DebugHelper.logError("Download flow hatası", e)
            trySend(WordPackageDownloadState.Error(
                error = "Beklenmeyen hata: ${e.message}",
                throwable = e
            ))
            close(e)
        }
    }

    /**
     * Download and parse package JSON - Synchronous version
     */
    suspend fun downloadAndParsePackage(
        packageId: String,
        storageUrl: String
    ): Result<FirebaseWordsJson> = withContext(Dispatchers.IO) {
        try {
            DebugHelper.log("Senkron indirme başlıyor: $packageId")

            val storageRef = if (storageUrl.startsWith("gs://")) {
                storage.getReferenceFromUrl(storageUrl)
            } else {
                storage.reference.child("$STORAGE_BASE_PATH/$packageId.json")
            }

            // Download bytes
            val maxDownloadSize = 5L * 1024 * 1024 // 5MB max
            val bytes = storageRef.getBytes(maxDownloadSize).await()

            DebugHelper.log("İndirme tamamlandı: ${bytes.size} bytes")

            // Parse JSON
            val jsonString = String(bytes, Charsets.UTF_8)
            val wordPackage = json.decodeFromString<FirebaseWordsJson>(jsonString)

            DebugHelper.logSuccess("Parse başarılı: ${wordPackage.words.size} kelime")

            Result.Success(wordPackage)

        } catch (e: Exception) {
            DebugHelper.logError("Senkron indirme hatası", e)
            Result.Error(AppError.Unknown(e))
        }
    }
}

/**
 * Package metadata from Firestore
 */
data class PackageMetadata(
    val packageId: String,
    val version: String,
    val level: String,
    val languagePair: String,
    val totalWords: Int,
    val wordIds: List<Int>? = null,
    val storageUrl: String,
    val requiresPremium: Boolean,
    val description: String?
)

/**
 * JSON structure from Firebase Storage
 */
@Serializable
data class FirebaseWordsJson(
    @SerialName("package_info")
    val packageInfo: FirebasePackageInfo,
    val words: List<FirebaseWordJson>
)

@Serializable
data class FirebasePackageInfo(
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
data class FirebaseWordJson(
    val id: Int,
    val english: String,
    val turkish: String,
    val example: FirebaseExampleJson? = null,
    val pronunciation: String? = null,
    val level: String,
    val category: String,
    val reversible: Boolean = true,
    val userAdded: Boolean = false
)

@Serializable
data class FirebaseExampleJson(
    val en: String,
    val tr: String
)