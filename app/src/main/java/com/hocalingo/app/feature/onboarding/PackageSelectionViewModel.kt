package com.hocalingo.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.database.JsonLoader
import com.hocalingo.app.database.MainDatabaseSeeder
import com.hocalingo.app.database.HocaLingoDatabase
import com.hocalingo.app.database.entities.WordPackageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
 * PackageSelectionViewModel - FIXED
 * ✅ Gereksiz toast mesajları kaldırıldı
 * ✅ Sadece error durumlarında toast gösteriliyor
 */
@HiltViewModel
class PackageSelectionViewModel @Inject constructor(
    private val jsonLoader: JsonLoader,
    private val database: HocaLingoDatabase,
    private val databaseSeeder: MainDatabaseSeeder
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
                // Database'den aktif paketleri al
                val existingPackages = database.wordPackageDao().getActivePackages()
                DebugHelper.log("Database'de ${existingPackages.size} paket bulundu")

                existingPackages.forEach { pkg ->
                    val conceptCount = database.conceptDao().getConceptsByPackage(pkg.packageId).size
                    DebugHelper.log("📦 ${pkg.packageId}: ${pkg.totalWords} words, $conceptCount concepts")
                }

                // UI paket listesini oluştur
                val packages = createPackageList(existingPackages)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        packages = packages,
                        error = null
                    )
                }

                DebugHelper.log("UI packages oluşturuldu: ${packages.size}")
                packages.forEach { pkg ->
                    DebugHelper.log("📱 ${pkg.id}: Downloaded=${pkg.isDownloaded}, Progress=${pkg.downloadProgress}")
                }

            } catch (e: Exception) {
                DebugHelper.logError("Package loading error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Paketler yüklenirken hata: ${e.message}"
                    )
                }
            }
            DebugHelper.log("=== PACKAGES YÜKLEME BİTTİ ===")
        }
    }

    private suspend fun createPackageList(existingPackages: List<WordPackageEntity>): List<PackageInfo> {
        DebugHelper.log("--- createPackageList başlıyor ---")

        // Predefined packages
        val defaultPackages = getDefaultPackages()

        // Update with database status
        val updatedPackages = defaultPackages.map { defaultPackage ->
            val dbPackage = existingPackages.find { it.packageId == defaultPackage.id }

            if (dbPackage != null) {
                // Package exists in database
                val conceptCount = database.conceptDao().getConceptsByPackage(dbPackage.packageId).size
                DebugHelper.log("${defaultPackage.id}: DB'de var, $conceptCount concept")

                defaultPackage.copy(
                    isDownloaded = conceptCount > 0,
                    wordCount = if (conceptCount > 0) conceptCount else defaultPackage.wordCount,
                    downloadProgress = if (conceptCount > 0) 100 else 0
                )
            } else {
                // Package not in database
                DebugHelper.log("${defaultPackage.id}: DB'de yok")
                defaultPackage.copy(
                    isDownloaded = false,
                    downloadProgress = 0
                )
            }
        }

        DebugHelper.log("--- createPackageList bitti ---")
        return updatedPackages
    }

    private fun getDefaultPackages(): List<PackageInfo> {
        return listOf(
            PackageInfo(
                id = "a1_en_tr_test_v1",
                level = "A1",
                name = "Başlangıç",
                description = "Temel kelimeler ve günlük ifadeler",
                wordCount = 200,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#4CAF50"
            ),
            PackageInfo(
                id = "a2_en_tr_v1",
                level = "A2",
                name = "Temel",
                description = "Günlük aktiviteler ve basit konuşmalar",
                wordCount = 300,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#2196F3"
            ),
            PackageInfo(
                id = "b1_en_tr_v1",
                level = "B1",
                name = "Orta",
                description = "İş, okul ve hobiler hakkında konuşma",
                wordCount = 400,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#FF9800"
            ),
            PackageInfo(
                id = "b2_en_tr_v1",
                level = "B2",
                name = "Orta-Üst",
                description = "Karmaşık metinler ve tartışmalar",
                wordCount = 450,
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

    // ✅ FIX: Gereksiz toast mesajı kaldırıldı
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
                // ✅ REMOVED: Gereksiz toast mesajı kaldırıldı
                // Sadece state güncellemesi yeterli, kullanıcı continue'ye basınca download olacak
            }
        }
    }

    private fun downloadPackage(packageId: String) {
        viewModelScope.launch {
            DebugHelper.log("=== PAKET İNDİRME BAŞLADI: $packageId ===")

            // Sadece A1 paketi mevcut
            if (packageId != "a1_en_tr_test_v1") {
                _effect.emit(
                    PackageSelectionEffect.ShowMessage(
                        "Bu paket yakında eklenecek! A1 paketi ile devam edebilirsiniz."
                    )
                )
                return@launch
            }

            try {
                // Progress animasyonu
                val packages = _uiState.value.packages.toMutableList()
                val index = packages.indexOfFirst { it.id == packageId }

                if (index == -1) {
                    DebugHelper.logError("Package not found in UI list: $packageId")
                    return@launch
                }

                // Progress simulation
                DebugHelper.log("Progress animasyonu başlatılıyor")
                for (progress in 25..75 step 25) {
                    packages[index] = packages[index].copy(downloadProgress = progress)
                    _uiState.update { it.copy(packages = packages.toList()) }
                    delay(300)
                }

                // GERÇEK İNDİRME
                DebugHelper.log("JsonLoader.loadTestWords() çağrılıyor...")
                when (val result = jsonLoader.loadTestWords()) {
                    is Result.Success -> {
                        DebugHelper.logSuccess("🎉 DOWNLOAD BAŞARILI: ${result.data} kelime!")

                        // ÖNEMLİ: Database'den packages'ları yeniden yükle
                        DebugHelper.log("Database'den packages yeniden yükleniyor...")
                        loadPackages() // Bu kritik - UI'ı database ile sync eder

                        _effect.emit(
                            PackageSelectionEffect.ShowMessage(
                                "🎉 Paket başarıyla indirildi! ${result.data} kelime eklendi."
                            )
                        )

                        // İndirme başarılıysa kelime seçimine yönlendir
                        _effect.emit(PackageSelectionEffect.NavigateToWordSelection(packageId))

                    } is Result.Error -> {
                    DebugHelper.logError("💥 DOWNLOAD BAŞARISIZ", result.error)

                    // Progress'i sıfırla
                    packages[index] = packages[index].copy(
                        isDownloaded = false,
                        downloadProgress = 0
                    )
                    _uiState.update { it.copy(packages = packages.toList()) }

                    _effect.emit(
                        PackageSelectionEffect.ShowMessage(
                            "❌ İndirme başarısız: ${result.error.message}"
                        )
                    )
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