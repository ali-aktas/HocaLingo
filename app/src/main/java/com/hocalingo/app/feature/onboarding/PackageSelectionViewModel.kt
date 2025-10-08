package com.hocalingo.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.data.WordPackageDownloadState
import com.hocalingo.app.data.WordPackageRepository
import com.hocalingo.app.data.toProgressPercentage
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
 * PackageSelectionViewModel - FIREBASE VERSION
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/onboarding/
 *
 * ✅ Firebase entegrasyonu tamamlandı
 * ✅ Progress tracking eklendi (0-100%)
 * ✅ WordPackageRepository kullanılıyor
 * ✅ JsonLoader backward compatibility korundu
 */
@HiltViewModel
class PackageSelectionViewModel @Inject constructor(
    private val jsonLoader: JsonLoader,
    private val database: HocaLingoDatabase,
    private val databaseSeeder: MainDatabaseSeeder,
    private val packageRepository: WordPackageRepository // ✅ YENİ: Firebase repository
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
                // Check which packages are downloaded
                val downloadedPackages = database.wordPackageDao().getActivePackages()
                val downloadedIds = downloadedPackages.map { it.packageId }.toSet()

                DebugHelper.log("İndirilen paketler: $downloadedIds")

                // Create package list with download status
                val packages = createDefaultPackages().map { pkg ->
                    pkg.copy(
                        isDownloaded = downloadedIds.contains(pkg.id),
                        downloadProgress = if (downloadedIds.contains(pkg.id)) 100 else 0
                    )
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        packages = packages
                    )
                }

                DebugHelper.log("Packages yüklendi: ${packages.size} paket")

            } catch (e: Exception) {
                DebugHelper.logError("Packages yükleme hatası", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Paketler yüklenemedi: ${e.message}"
                    )
                }
            }
        }
    }

    private fun createDefaultPackages(): List<PackageInfo> {
        return listOf(
            PackageInfo(
                id = "a1_en_tr_test_v1",
                level = "A1",
                name = "Başlangıç",
                description = "Temel kelimeler ve günlük ifadeler",
                wordCount = 50,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#4CAF50"
            ),
            PackageInfo(
                id = "a2_en_tr_v1",
                level = "A2",
                name = "Temel",
                description = "Günlük rutinler ve basit konuşmalar",
                wordCount = 150,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#2196F3"
            ),
            PackageInfo(
                id = "b1_en_tr_v1",
                level = "B1",
                name = "Orta",
                description = "İş ve seyahat konuları",
                wordCount = 200,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#FF9800"
            ),
            PackageInfo(
                id = "b2_en_tr_v1",
                level = "B2",
                name = "Orta Üstü",
                description = "Soyut konular ve detaylı tartışmalar",
                wordCount = 250,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#FF5722"
            ),
            PackageInfo(
                id = "c1_en_tr_v1",
                level = "C1",
                name = "İleri",
                description = "Karmaşık konular ve deyimler",
                wordCount = 500,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#9C27B0"
            ),
            PackageInfo(
                id = "c2_en_tr_v1",
                level = "C2",
                name = "Uzman",
                description = "Ana dil seviyesi kelime hazinesi",
                wordCount = 300,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#673AB7"
            )
        )
    }

    private fun selectPackage(packageId: String) {
        viewModelScope.launch {
            DebugHelper.log("=== PAKET SEÇİLDİ: $packageId ===")

            // State'i güncelle
            _uiState.update { it.copy(selectedPackageId = packageId) }

            val selectedPackage = _uiState.value.packages.find { it.id == packageId }
            DebugHelper.log("Seçilen paket: $selectedPackage")

            if (selectedPackage?.isDownloaded == true) {
                DebugHelper.log("Paket indirilmiş, kelime seçimine geçiliyor")
                _effect.emit(PackageSelectionEffect.NavigateToWordSelection(packageId))
            } else {
                DebugHelper.log("Paket indirilmemiş, state sadece güncellendi")
            }
        }
    }

    private fun downloadPackage(packageId: String) {
        viewModelScope.launch {
            DebugHelper.log("=== PAKET İNDİRME BAŞLADI: $packageId ===")

            // Sadece A1 paketi mevcut (şimdilik)
            if (packageId != "a1_en_tr_test_v1") {
                _effect.emit(
                    PackageSelectionEffect.ShowMessage(
                        "Bu paket yakında eklenecek! A1 paketi ile devam edebilirsiniz."
                    )
                )
                return@launch
            }

            try {
                val packages = _uiState.value.packages.toMutableList()
                val index = packages.indexOfFirst { it.id == packageId }

                if (index == -1) {
                    DebugHelper.logError("Package not found in UI list: $packageId")
                    return@launch
                }

                // ✅ YENİ: Firebase Repository ile progress tracking
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

                            // Paketleri yeniden yükle
                            loadPackages()

                            _effect.emit(
                                PackageSelectionEffect.ShowMessage(
                                    "🎉 Paket başarıyla indirildi! ${state.wordCount} kelime eklendi."
                                )
                            )

                            // Kelime seçimine geç
                            _effect.emit(PackageSelectionEffect.NavigateToWordSelection(packageId))
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
    val color: String
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
}