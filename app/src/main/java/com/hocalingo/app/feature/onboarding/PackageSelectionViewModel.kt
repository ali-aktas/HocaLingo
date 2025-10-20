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
 * PackageSelectionViewModel - FIREBASE VERSION + DOWNLOAD STATUS
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/onboarding/
 *
 * ✅ Firebase entegrasyonu tamamlandı
 * ✅ Progress tracking eklendi (0-100%)
 * ✅ WordPackageRepository kullanılıyor
 * ✅ JsonLoader backward compatibility korundu
 * ✨ YENİ: Package download status tracking (badge için)
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

    private fun loadPackages() {
        viewModelScope.launch {
            DebugHelper.log("=== PACKAGES YÜKLENMEYE BAŞLIYOR ===")
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Create default package list
                val packages = createDefaultPackages()

                // ✨ YENİ: Her paket için download status'ü hesapla
                val packagesWithStatus = packages.map { pkg ->
                    val status = packageRepository.getPackageDownloadStatus(pkg.id)

                    val newWordsCount = when (status) {
                        is PackageDownloadStatus.HasNewWords -> status.newWordsCount
                        else -> 0
                    }

                    pkg.copy(
                        downloadStatus = status,
                        newWordsCount = newWordsCount,
                        isDownloaded = status is PackageDownloadStatus.FullyDownloaded, // Backward compatibility
                        downloadProgress = if (status is PackageDownloadStatus.FullyDownloaded) 100 else 0
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        packages = packagesWithStatus
                    )
                }

                DebugHelper.log("Packages yüklendi: ${packagesWithStatus.size} paket")

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

            // ✨ YENİ: Eğer paket zaten indirilmişse, direkt loading animation göster
            if (selectedPackage.isDownloaded) {
                DebugHelper.log("Paket zaten indirilmiş, direkt açılıyor...")
                _effect.emit(PackageSelectionEffect.ShowLoadingAnimation(packageId))
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
                            "Lütfen önce A1 paketi ile devam edebilirsiniz."
                        )
                    )
                    return@launch
                }
            }

            try {
                val packages = _uiState.value.packages.toMutableList()
                val index = packages.indexOfFirst { it.id == packageId }

                if (index == -1) {
                    DebugHelper.logError("Package not found in UI list: $packageId")
                    return@launch
                }

                // Firebase Repository ile progress tracking
                packageRepository.downloadPackageWithProgress(packageId).collect { state ->
                    when (state) {
                        is WordPackageDownloadState.Idle -> {
                            DebugHelper.log("İndirme hazırlanıyor...")
                        }

                        is WordPackageDownloadState.Downloading -> {
                            DebugHelper.log("İndiriliyor: ${state.progress}%")
                            packages[index] = packages[index].copy(downloadProgress = state.progress)
                            _uiState.update { it.copy(packages = packages.toList()) }
                        }

                        is WordPackageDownloadState.Parsing -> {
                            DebugHelper.log("Parse ediliyor...")
                            packages[index] = packages[index].copy(downloadProgress = 75)
                            _uiState.update { it.copy(packages = packages.toList()) }
                        }

                        is WordPackageDownloadState.Saving -> {
                            DebugHelper.log("Kaydediliyor: ${state.currentWord}/${state.totalWords}")
                            val totalProgress = 75 + (state.progress / 4) // 75-100 arası
                            packages[index] = packages[index].copy(downloadProgress = totalProgress)
                            _uiState.update { it.copy(packages = packages.toList()) }
                        }

                        is WordPackageDownloadState.Success -> {
                            DebugHelper.logSuccess("🎉 DOWNLOAD BAŞARILI: ${state.wordCount} kelime!")

                            // ✨ YENİ: Sadece ilgili paketin status'ünü güncelle (yanıp sönme yok!)
                            updateSinglePackageStatus(packageId)

                            _effect.emit(
                                PackageSelectionEffect.ShowMessage(
                                    "🎉 Paket başarıyla indirildi! ${state.wordCount} kelime eklendi."
                                )
                            )

                            // ✨ YENİ: 3 saniye loading animation göster
                            _effect.emit(PackageSelectionEffect.ShowLoadingAnimation(packageId))
                        }

                        is WordPackageDownloadState.Error -> {
                            DebugHelper.logError("💥 DOWNLOAD BAŞARISIZ", state.throwable)

                            // Progress'i sıfırla
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
     * ✨ YENİ: Tek bir paketin status'ünü güncelle (yanıp sönme olmadan)
     */
    private fun updateSinglePackageStatus(packageId: String) {
        viewModelScope.launch {
            val status = packageRepository.getPackageDownloadStatus(packageId)

            val newWordsCount = when (status) {
                is PackageDownloadStatus.HasNewWords -> status.newWordsCount
                else -> 0
            }

            val packages = _uiState.value.packages.toMutableList()
            val index = packages.indexOfFirst { it.id == packageId }

            if (index != -1) {
                packages[index] = packages[index].copy(
                    downloadStatus = status,
                    newWordsCount = newWordsCount,
                    isDownloaded = status is PackageDownloadStatus.FullyDownloaded,
                    downloadProgress = if (status is PackageDownloadStatus.FullyDownloaded) 100 else 0
                )

                _uiState.update { it.copy(packages = packages) }
                DebugHelper.log("✅ Paket status güncellendi: $packageId")
            }
        }
    }

    private fun createDefaultPackages(): List<PackageInfo> {
        return listOf(
            PackageInfo(
                id = "a1_en_tr_test_v1",
                level = "A1",
                name = "Başlangıç", // Türkçe
                description = "Temel seviye kelimeler",
                wordCount = 50,
                isDownloaded = false,
                color = "#4CAF50"
            )
            // Diğer paketler Firebase Storage'a eklenince buraya eklenecek:
            // PackageInfo(id = "a2_en_tr_v1", level = "A2", name = "Temel", ...),
            // PackageInfo(id = "b1_en_tr_v1", level = "B1", name = "Orta Seviye", ...),
        )
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
    val wordCount: Int,
    val isDownloaded: Boolean,
    val downloadProgress: Int = 0,
    val color: String,
    // ✨ YENİ: Download status fields
    val downloadStatus: PackageDownloadStatus = PackageDownloadStatus.NotDownloaded,
    val newWordsCount: Int = 0  // Kaç yeni kelime var
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
    data class ShowLoadingAnimation(val packageId: String) : PackageSelectionEffect // ✨ YENİ
}