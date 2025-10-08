package com.hocalingo.app.database

import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.base.AppError
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.data.WordPackageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * JsonLoader - FIREBASE VERSION
 *
 * Package: app/src/main/java/com/hocalingo/app/database/
 *
 * ÖNEMLI: Bu sınıf artık Firebase Storage'dan veri çeker!
 * Assets klasörü tamamen kaldırıldı.
 *
 * Backward compatibility için aynı metodlar korundu:
 * - loadTestWords() -> Firebase'den a1_en_tr_test_v1 paketini indirir
 * - isTestDataLoaded() -> Paketin indirilip indirilmediğini kontrol eder
 *
 * Eski kullanımlar sorunsuz çalışacak, sadece veri kaynağı değişti.
 */
@Singleton
class JsonLoader @Inject constructor(
    private val database: HocaLingoDatabase,
    private val packageRepository: WordPackageRepository
) {

    companion object {
        // Test paketi ID'si - Firebase'de aynı ID ile kayıtlı olmalı
        private const val TEST_PACKAGE_ID = "a1_en_tr_test_v1"
    }

    /**
     * Load test words from Firebase Storage
     *
     * ÖNCEKİ KULLANIM: Assets'ten yüklüyordu
     * YENİ KULLANIM: Firebase Storage'dan indirir
     *
     * Backward compatible - mevcut kod değişmeden çalışır
     */
    suspend fun loadTestWords(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            DebugHelper.logDatabase("=== FIREBASE'DEN TEST PAKETİ İNDİRİLİYOR ===")
            DebugHelper.logDatabase("Package ID: $TEST_PACKAGE_ID")

            // Duplicate check
            when (val checkResult = packageRepository.isPackageDownloaded(TEST_PACKAGE_ID)) {
                is Result.Success -> {
                    if (checkResult.data) {
                        val conceptCount = database.conceptDao().getConceptsByPackage(TEST_PACKAGE_ID).size
                        DebugHelper.logDatabase("Paket zaten var: $conceptCount kelime")
                        return@withContext Result.Success(conceptCount)
                    }
                }
                is Result.Error -> {
                    DebugHelper.logError("Paket kontrolü başarısız", checkResult.error)
                }
            }

            // Download from Firebase - synchronous version for compatibility
            // Not using Flow here to keep the same signature
            DebugHelper.logDatabase("Firebase Storage'dan indiriliyor...")

            // Use a simple synchronous approach
            var finalResult: Result<Int> = Result.Error(AppError.Unknown(Exception("Download not completed")))

            packageRepository.downloadPackageWithProgress(TEST_PACKAGE_ID).collect { state ->
                when (state) {
                    is com.hocalingo.app.data.WordPackageDownloadState.Success -> {
                        DebugHelper.logSuccess("✅ Firebase'den yükleme başarılı: ${state.wordCount} kelime")
                        finalResult = Result.Success(state.wordCount)
                    }
                    is com.hocalingo.app.data.WordPackageDownloadState.Error -> {
                        DebugHelper.logError("💥 Firebase yükleme hatası: ${state.error}")
                        finalResult = Result.Error(AppError.Unknown(state.throwable ?: Exception(state.error)))
                    }
                    else -> {
                        // Progress states - log for debugging
                        DebugHelper.logDatabase("Download state: $state")
                    }
                }
            }

            finalResult

        } catch (e: Exception) {
            DebugHelper.logError("YÜKLEME HATASI", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Check if test data loaded from database
     *
     * Backward compatible - aynı şekilde çalışır
     */
    suspend fun isTestDataLoaded(): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val packageInfo = database.wordPackageDao().getPackageById(TEST_PACKAGE_ID)
            val conceptCount = if (packageInfo != null) {
                database.conceptDao().getConceptsByPackage(TEST_PACKAGE_ID).size
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
     * Load any package from Firebase by ID
     *
     * YENİ METOD - Generic package loading
     */
    suspend fun loadPackageById(packageId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            DebugHelper.logDatabase("=== PAKET İNDİRİLİYOR: $packageId ===")

            // Check if already exists
            when (val checkResult = packageRepository.isPackageDownloaded(packageId)) {
                is Result.Success -> {
                    if (checkResult.data) {
                        val conceptCount = database.conceptDao().getConceptsByPackage(packageId).size
                        DebugHelper.logDatabase("Paket zaten var: $conceptCount kelime")
                        return@withContext Result.Success(conceptCount)
                    }
                }
                is Result.Error -> {
                    DebugHelper.logError("Paket kontrolü başarısız", checkResult.error)
                }
            }

            // Download from Firebase
            var finalResult: Result<Int> = Result.Error(AppError.Unknown(Exception("Download not completed")))

            packageRepository.downloadPackageWithProgress(packageId).collect { state ->
                when (state) {
                    is com.hocalingo.app.data.WordPackageDownloadState.Success -> {
                        DebugHelper.logSuccess("✅ Yükleme başarılı: ${state.wordCount} kelime")
                        finalResult = Result.Success(state.wordCount)
                    }
                    is com.hocalingo.app.data.WordPackageDownloadState.Error -> {
                        DebugHelper.logError("💥 Yükleme hatası: ${state.error}")
                        finalResult = Result.Error(AppError.Unknown(state.throwable ?: Exception(state.error)))
                    }
                    else -> {
                        DebugHelper.logDatabase("Download state: $state")
                    }
                }
            }

            finalResult

        } catch (e: Exception) {
            DebugHelper.logError("YÜKLEME HATASI", e)
            Result.Error(AppError.Unknown(e))
        }
    }

    /**
     * Clear all words from database
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
     * Get downloaded package count
     */
    suspend fun getDownloadedPackageCount(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val packages = database.wordPackageDao().getActivePackages()
            Result.Success(packages.size)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }
}

/**
 * DEPRECATION NOTICE:
 *
 * Aşağıdaki data class'lar artık kullanılmıyor.
 * Firebase data class'ları (FirebaseWordsJson vb.) kullanılıyor.
 *
 * Backward compatibility için bırakıldı ama kullanmayın!
 */

@Deprecated(
    message = "Use FirebaseWordsJson instead",
    replaceWith = ReplaceWith("FirebaseWordsJson", "com.hocalingo.app.data.FirebaseWordsJson"),
    level = DeprecationLevel.WARNING
)
data class TestWordsJson(
    val packageInfo: Any,
    val words: List<Any>
)

@Deprecated(
    message = "Use FirebasePackageInfo instead",
    level = DeprecationLevel.WARNING
)
data class TestPackageInfo(val id: String)

@Deprecated(
    message = "Use FirebaseWordJson instead",
    level = DeprecationLevel.WARNING
)
data class TestWordJson(val id: Int)

@Deprecated(
    message = "Use FirebaseExampleJson instead",
    level = DeprecationLevel.WARNING
)
data class TestExampleJson(val en: String, val tr: String)