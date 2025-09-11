package com.hocalingo.app.feature.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.base.Result
import com.hocalingo.app.core.database.JsonLoader
import com.hocalingo.app.core.database.MainDatabaseSeeder
import com.hocalingo.app.core.database.HocaLingoDatabase
import com.hocalingo.app.core.database.entities.WordPackageEntity
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
        loadPackages()
    }

    fun onEvent(event: PackageSelectionEvent) {
        when (event) {
            is PackageSelectionEvent.SelectPackage -> selectPackage(event.packageId)
            is PackageSelectionEvent.DownloadPackage -> downloadPackage(event.packageId)
            PackageSelectionEvent.RetryLoading -> loadPackages()
        }
    }

    private fun loadPackages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Önce database'deki mevcut paketleri kontrol et
                val existingPackages = database.wordPackageDao().getActivePackages()

                // Test için A1 paketinin yüklü olup olmadığını kontrol et
                // ÖNEMLİ: Package ID'yi doğru yazıyoruz!
                val isA1Loaded = existingPackages.any { it.packageId == "a1_en_tr_test_v1" }

                if (!isA1Loaded) {
                    // A1 test verisini yükle
                    jsonLoader.loadTestWords()
                }

                // Tüm paket listesini oluştur (6 seviye)
                val packages = createPackageList()

                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        packages = packages,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Paketler yüklenirken hata oluştu"
                    )
                }
            }
        }
    }

    private suspend fun createPackageList(): List<PackageInfo> {
        // Database'den mevcut paketleri kontrol et
        val existingPackages = database.wordPackageDao().getActivePackages()
        val isA1Downloaded = existingPackages.any { it.packageId == "a1_en_tr_test_v1" }

        return listOf(
            PackageInfo(
                id = "a1_en_tr_test_v1",  // DÜZELTME: test_words.json'daki ID ile eşleşmeli!
                level = "A1",
                name = "Başlangıç",
                description = "Temel kelimeler ve günlük ifadeler",
                wordCount = 50, // Test için 50 kelime
                isDownloaded = isA1Downloaded,
                downloadProgress = if (isA1Downloaded) 100 else 0,
                color = "#4CAF50" // Green
            ),
            PackageInfo(
                id = "a2_en_tr_v1",
                level = "A2",
                name = "Temel",
                description = "Basit iletişim ve yaygın kelimeler",
                wordCount = 400,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#8BC34A" // Light Green
            ),
            PackageInfo(
                id = "b1_en_tr_v1",
                level = "B1",
                name = "Orta",
                description = "Günlük konuşma ve seyahat kelimeleri",
                wordCount = 600,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#FF9800" // Orange
            ),
            PackageInfo(
                id = "b2_en_tr_v1",
                level = "B2",
                name = "Orta-İleri",
                description = "İş ve akademik kelimeler",
                wordCount = 800,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#FF5722" // Deep Orange
            ),
            PackageInfo(
                id = "c1_en_tr_v1",
                level = "C1",
                name = "İleri",
                description = "Karmaşık konular ve deyimler",
                wordCount = 500,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#9C27B0" // Purple
            ),
            PackageInfo(
                id = "c2_en_tr_v1",
                level = "C2",
                name = "Uzman",
                description = "Ana dil seviyesi kelime hazinesi",
                wordCount = 300,
                isDownloaded = false,
                downloadProgress = 0,
                color = "#673AB7" // Deep Purple
            )
        )
    }

    private fun selectPackage(packageId: String) {
        viewModelScope.launch {
            val selectedPackage = _uiState.value.packages.find { it.id == packageId }

            if (selectedPackage?.isDownloaded == true) {
                // Paket yüklüyse direkt kelime seçimine geç
                _uiState.update { it.copy(selectedPackageId = packageId) }
                _effect.emit(PackageSelectionEffect.NavigateToWordSelection(packageId))
            } else {
                // Paket yüklü değilse önce indir
                _effect.emit(PackageSelectionEffect.ShowDownloadDialog(packageId))
            }
        }
    }

    private fun downloadPackage(packageId: String) {
        viewModelScope.launch {
            // Şimdilik sadece A1 paketi var, diğerleri için "Yakında" mesajı
            if (packageId != "a1_en_tr_test_v1") {
                _effect.emit(
                    PackageSelectionEffect.ShowMessage(
                        "Bu paket yakında eklenecek! Şimdilik A1 paketi ile devam edebilirsiniz."
                    )
                )
                return@launch
            }

            // İndirme simülasyonu
            val packages = _uiState.value.packages.toMutableList()
            val index = packages.indexOfFirst { it.id == packageId }

            if (index != -1) {
                // Progress güncellemeleri
                for (progress in 0..100 step 20) {
                    packages[index] = packages[index].copy(downloadProgress = progress)
                    _uiState.update { it.copy(packages = packages.toList()) }
                    kotlinx.coroutines.delay(200) // Simülasyon için
                }

                packages[index] = packages[index].copy(
                    isDownloaded = true,
                    downloadProgress = 100
                )
                _uiState.update { it.copy(packages = packages.toList()) }

                _effect.emit(PackageSelectionEffect.ShowMessage("Paket başarıyla indirildi!"))
            }
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