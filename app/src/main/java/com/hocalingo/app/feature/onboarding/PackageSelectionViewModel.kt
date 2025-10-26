package com.hocalingo.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.data.PackageDownloadStatus
import com.hocalingo.app.data.WordPackageDownloadState
import com.hocalingo.app.data.WordPackageRepository
import com.hocalingo.app.database.JsonLoader
import com.hocalingo.app.database.MainDatabaseSeeder
import com.hocalingo.app.database.HocaLingoDatabase
import com.hocalingo.app.database.entities.WordPackageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PackageSelectionViewModel - FIREBASE VERSION + DOWNLOAD STATUS + DYNAMIC WORD COUNT
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/onboarding/
 *
 * ✅ Firebase entegrasyonu tamamlandı
 * ✅ Progress tracking eklendi (0-100%)
 * ✅ WordPackageRepository kullanılıyor
 * ✅ JsonLoader backward compatibility korundu
 * ✅ Package download status tracking (badge için)
 * ✨ YENİ: Dinamik kelime sayısı (database'den gerçek sayı)
 */
@HiltViewModel
class PackageSelectionViewModel @Inject constructor(
    private val jsonLoader: JsonLoader,
    private val database: HocaLingoDatabase,
    private val databaseSeeder: MainDatabaseSeeder,
    private val packageRepository: WordPackageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PackageSelectionUiState())
    val uiState: StateFlow<PackageSelectionUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<PackageSelectionEffect>()
    val effect: SharedFlow<PackageSelectionEffect> = _effect.asSharedFlow()

    init {
        DebugHelper.log("=== PackageSelectionViewModel BAŞLATILIYOR ===")
        loadPackages()
    }

    fun onEvent(event: PackageSelectionEvent) {
        DebugHelper.log("Event: $event")
        when (event) {
            is PackageSelectionEvent.SelectPackage -> selectPackage(event.packageId)
            is PackageSelectionEvent.DownloadPackage -> downloadPackage(event.packageId)
            PackageSelectionEvent.RetryLoading -> loadPackages()
        }
    }

    // PackageSelectionViewModel.kt
// loadPackages() metodunu TAM OLARAK BUNUNLA DEĞİŞTİR:

    private fun loadPackages() {
        viewModelScope.launch {
            DebugHelper.log("=== PACKAGES YÜKLENMEYE BAŞLIYOR ===")
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Paketleri oluştur (isDownloaded zaten içinde belirleniyor)
                val packages = createDefaultPackagesWithDynamicCounts()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        packages = packages
                    )
                }

                DebugHelper.log("✅ Packages yüklendi: ${packages.size} paket")
                packages.forEach { pkg ->
                    DebugHelper.log("  ${pkg.level}: ${pkg.wordCount} kelime (downloaded: ${pkg.isDownloaded})")
                }

            } catch (e: Exception) {
                DebugHelper.logError("Package loading hatası", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Paketler yüklenemedi: ${e.message}"
                    )
                }
            }

            DebugHelper.log("=== PACKAGES YÜKLEME TAMAMLANDI ===")
        }
    }

    private fun selectPackage(packageId: String) {
        DebugHelper.log("Paket seçildi: $packageId")
        _uiState.update { it.copy(selectedPackageId = packageId) }
    }

    private fun downloadPackage(packageId: String) {
        viewModelScope.launch {
            DebugHelper.log("=== PAKET İNDİRME BAŞLADI: $packageId ===")

            val selectedPackage = _uiState.value.packages.find { it.id == packageId }
            if (selectedPackage == null) {
                _effect.emit(
                    PackageSelectionEffect.ShowMessage(
                        "Lütfen önce bir paket seçin."
                    )
                )
                return@launch
            }

            if (selectedPackage.isDownloaded) {
                DebugHelper.log("Paket zaten indirilmiş, direkt açılıyor...")
                _effect.emit(PackageSelectionEffect.NavigateToWordSelection(packageId))
                return@launch
            }

            // Eğer ilk paket değilse ve test paketi indirilmemişse, uyar
            if (packageId != "a1_en_tr_test_v1") {
                val testPackageDownloaded = when (packageRepository.isPackageDownloaded("a1_en_tr_test_v1")) {
                    is Result.Success -> (packageRepository.isPackageDownloaded("a1_en_tr_test_v1") as Result.Success).data
                    else -> false
                }

                if (!testPackageDownloaded) {
                    _effect.emit(
                        PackageSelectionEffect.ShowMessage(
                            "Lütfen önce A1 paketi ile devam edin."
                        )
                    )
                    return@launch
                }
            }

            try {
                // Firebase'den indirme başlat (Flow kullanarak progress tracking)
                // PackageSelectionViewModel içindeki downloadPackage() metodunun
                // when bloğunu bununla değiştir (satır ~150 civarı)

                packageRepository.downloadPackageWithProgress(packageId).collect { state ->
                    val packages = _uiState.value.packages.toMutableList()
                    val index = packages.indexOfFirst { it.id == packageId }

                    if (index == -1) return@collect

                    when (state) {
                        is WordPackageDownloadState.Idle -> {
                            // Başlangıç durumu - hiçbir şey yapma
                        }

                        is WordPackageDownloadState.Downloading -> {
                            DebugHelper.log("⬇️  İndiriliyor: ${state.progress}%")
                            packages[index] = packages[index].copy(
                                downloadProgress = state.progress
                            )
                            _uiState.update { it.copy(packages = packages.toList()) }
                        }

                        is WordPackageDownloadState.Parsing -> {
                            DebugHelper.log("📝 Parse ediliyor...")
                            packages[index] = packages[index].copy(downloadProgress = 90)
                            _uiState.update { it.copy(packages = packages.toList()) }
                        }

                        is WordPackageDownloadState.Saving -> {
                            DebugHelper.log("💾 Kaydediliyor: ${state.currentWord}/${state.totalWords}")
                            packages[index] = packages[index].copy(
                                downloadProgress = 90 + (state.progress / 10)
                            )
                            _uiState.update { it.copy(packages = packages.toList()) }
                        }

                        is WordPackageDownloadState.Success -> {
                            DebugHelper.logSuccess("✅ İNDİRME BAŞARILI: ${state.wordCount} kelime")

                            _effect.emit(
                                PackageSelectionEffect.ShowMessage(
                                    "🎉 Paket başarıyla indirildi! ${state.wordCount} kelime eklendi."
                                )
                            )

                            _effect.emit(PackageSelectionEffect.ShowLoadingAnimation(packageId))
                        }

                        is WordPackageDownloadState.Error -> {
                            DebugHelper.logError("💥 DOWNLOAD BAŞARISIZ", state.throwable)

                            packages[index] = packages[index].copy(
                                isDownloaded = false,
                                downloadProgress = 0
                            )
                            _uiState.update { it.copy(packages = packages.toList()) }

                            _effect.emit(
                                PackageSelectionEffect.ShowMessage(
                                    "❌ İndirme başarısız: ${state.error}"
                                )
                            )
                        }
                    }
                }

            } catch (e: Exception) {
                DebugHelper.logError("Download exception", e)
                _effect.emit(
                    PackageSelectionEffect.ShowMessage("Beklenmeyen hata: ${e.message}")
                )
            }

            DebugHelper.log("=== PAKET İNDİRME BİTTİ ===")
        }
    }

    /**
     * ✨ DÜZELTİLMİŞ: Assets paketlerini de kontrol ediyor
     */
    private suspend fun createDefaultPackagesWithDynamicCounts(): List<PackageInfo> {
        return try {
            val levels = listOf(
                "A1" to "Başlangıç",
                "A2" to "Temel",
                "B1" to "Orta",
                "B2" to "Orta-İleri",
                "C1" to "İleri",
                "C2" to "Ustalaşma"
            )

            levels.map { (level, name) ->
                // 1. Database'den kelime sayısını al (level'a göre)
                val wordCount = try {
                    database.conceptDao().getConceptsByLevel(level).size
                } catch (e: Exception) {
                    DebugHelper.logError("Level sorgu hatası: $level", e)
                    0
                }

                // 2. Bu level için paket var mı kontrol et
                val isDownloaded = wordCount > 0  // ✅ Kelime varsa downloaded!

                // 3. Firebase test paketi için özel ID (geriye uyumluluk)
                val packageId = if (level == "A1") "a1_en_tr_test_v1" else "${level.lowercase()}_en_tr_v1"

                val description = when (level) {
                    "A1" -> "Günlük hayatta en çok kullanılan temel kelimeler"
                    "A2" -> "Basit cümleler kurmanızı sağlayacak kelimeler"
                    "B1" -> "Günlük konuşmalarda rahatça kullanabileceğiniz kelimeler"
                    "B2" -> "Karmaşık konuları anlayabileceğiniz kelimeler"
                    "C1" -> "Akademik ve profesyonel iletişim için kelimeler"
                    "C2" -> "Ana dili konuşanlar gibi akıcı iletişim"
                    else -> "Kelime paketi"
                }

                PackageInfo(
                    id = packageId,
                    level = level,
                    name = name,
                    description = description,
                    wordCount = wordCount,                      // ✅ Gerçek sayı
                    isDownloaded = isDownloaded,                // ✅ Kelime varsa true
                    downloadProgress = if (isDownloaded) 100 else 0,
                    color = getLevelColor(level)
                )
            }
        } catch (e: Exception) {
            DebugHelper.logError("createDefaultPackagesWithDynamicCounts hatası", e)
            createFallbackPackages()
        }
    }

    /**
     * Fallback: Database hatası durumunda sabit değerlerle paket listesi
     */
    private fun createFallbackPackages(): List<PackageInfo> {
        return listOf(
            PackageInfo(
                id = "a1_en_tr_test_v1",
                level = "A1",
                name = "Başlangıç",
                description = "Temel seviye kelimeler",
                wordCount = 50,
                isDownloaded = false,
                color = "#4CAF50"
            )
        )
    }

    /**
     * Seviyeye göre renk döndür
     */
    private fun getLevelColor(level: String): String {
        return when (level) {
            "A1" -> "#4CAF50" // Yeşil
            "A2" -> "#8BC34A" // Açık yeşil
            "B1" -> "#FFC107" // Sarı
            "B2" -> "#FF9800" // Turuncu
            "C1" -> "#FF5722" // Koyu turuncu
            "C2" -> "#F44336" // Kırmızı
            else -> "#9E9E9E" // Gri
        }
    }
}

// UI State
data class PackageSelectionUiState(
    val isLoading: Boolean = false,
    val packages: List<PackageInfo> = emptyList(),
    val selectedPackageId: String? = null,
    val error: String? = null
)

// Package Info Model
data class PackageInfo(
    val id: String,
    val level: String,
    val name: String,
    val description: String,
    val wordCount: Int,                 // ✅ Dinamik kelime sayısı!
    val isDownloaded: Boolean,
    val downloadProgress: Int = 0,
    val color: String,
    // Download status fields
    val downloadStatus: PackageDownloadStatus = PackageDownloadStatus.NotDownloaded,
    val newWordsCount: Int = 0
)

// Events
sealed interface PackageSelectionEvent {
    data class SelectPackage(val packageId: String) : PackageSelectionEvent
    data class DownloadPackage(val packageId: String) : PackageSelectionEvent
    data object RetryLoading : PackageSelectionEvent
}

// Effects
sealed interface PackageSelectionEffect {
    data class NavigateToWordSelection(val packageId: String) : PackageSelectionEffect
    data class ShowDownloadDialog(val packageId: String) : PackageSelectionEffect
    data class ShowMessage(val message: String) : PackageSelectionEffect
    data class ShowLoadingAnimation(val packageId: String) : PackageSelectionEffect
}