package com.hocalingo.app.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.database.JsonLoader
import com.hocalingo.app.database.LocalPackageLoader
import com.hocalingo.app.database.MainDatabaseSeeder
import com.hocalingo.app.feature.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SplashViewModel - ASSETS PACKAGE LOADER ENTEGRE EDİLDİ
 *
 * ✅ İlk açılışta 1600 kelime assets'ten yükleniyor
 * ✅ Sonraki açılışlarda kontrol ediliyor (duplicate engelleniyor)
 * ✅ 2-3 saniyelik animasyon sırasında yükleme yapılıyor
 * ✅ Firebase test paketi backward compatibility korundu
 *
 * İlk giriş → Auth → Onboarding → Word Selection → Home
 * Sonraki girişler → DİREKT HOME
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val jsonLoader: JsonLoader,                 // Firebase test paketi için (mevcut)
    private val localPackageLoader: LocalPackageLoader, // ✨ YENİ: Assets'ten 1600 kelime için
    private val databaseSeeder: MainDatabaseSeeder,
    private val preferencesManager: UserPreferencesManager
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<SplashNavigationEvent>()
    val navigationEvent: SharedFlow<SplashNavigationEvent> = _navigationEvent.asSharedFlow()

    init {
        DebugHelper.log("=== SPLASH VIEW MODEL BAŞLATILIYOR ===")
        checkAppState()
    }

    private fun checkAppState() {
        viewModelScope.launch {
            try {
                DebugHelper.log("🎬 Splash animasyonu başlatılıyor...")

                // ✨ YENİ: Assets'ten 1600 kelimeyi yükle (ilk açılışta)
                // 2-3 saniyelik animasyon sırasında arka planda yüklenir
                loadBundledPackages()

                // Minimum splash duration (animasyonun görünmesi için)
                delay(1500)

                // Firebase test paketi yükle (backward compatibility)
                ensureTestDataLoaded()

                DebugHelper.log("👤 User durumu kontrol ediliyor...")

                // Kullanıcı durumunu kontrol et
                val currentUser = authRepository.getCurrentUser()
                DebugHelper.log("Current user: ${currentUser?.uid ?: "YOK"}")

                if (currentUser != null) {
                    // Kullanıcı var - onboarding tamamlanmış mı kontrol et
                    checkOnboardingStatus()
                } else {
                    // Kullanıcı yok - Auth ekranına yönlendir
                    DebugHelper.log("➡️  Auth ekranına yönlendiriliyor...")
                    _navigationEvent.emit(SplashNavigationEvent.NavigateToAuth)
                }

            } catch (e: Exception) {
                DebugHelper.logError("💥 Splash kontrol hatası", e)
                _navigationEvent.emit(SplashNavigationEvent.NavigateToAuth)
            }
        }
    }

    /**
     * ✨ YENİ METOD: Assets'ten 1600 kelimeyi yükle
     *
     * - İlk açılışta 16 JSON dosyasını okur
     * - Database'e kaydeder
     * - Sonraki açılışlarda kontrol eder, yüklü ise atlar
     * - Animasyon sırasında arka planda çalışır
     */
    private suspend fun loadBundledPackages() {
        try {
            DebugHelper.log("📦 ═══════════════════════════════════")
            DebugHelper.log("📦 BUNDLED PACKAGES YÜKLEME BAŞLIYOR")
            DebugHelper.log("📦 ═══════════════════════════════════")

            when (val result = localPackageLoader.loadBundledPackagesIfNeeded()) {
                is Result.Success -> {
                    DebugHelper.logSuccess("✅ Bundled packages hazır: ${result.data} kelime")
                }
                is Result.Error -> {
                    DebugHelper.logError("⚠️  Bundled packages yüklenemedi", result.error)
                    // Non-critical error - devam et
                }
            }

            DebugHelper.log("📦 ═══════════════════════════════════")

        } catch (e: Exception) {
            DebugHelper.logError("💥 loadBundledPackages hatası", e)
            // Non-critical - uygulama açılmaya devam eder
        }
    }

    /**
     * Firebase test paketi yükle (backward compatibility)
     * Mevcut kod - değişiklik yok
     */
    private suspend fun ensureTestDataLoaded() {
        try {
            DebugHelper.log("🔍 Test verisi kontrol ediliyor...")

            when (val result = jsonLoader.isTestDataLoaded()) {
                is Result.Success -> {
                    if (!result.data) {
                        DebugHelper.log("⬇️  Test verisi yükleniyor...")
                        when (val loadResult = jsonLoader.loadTestWords()) {
                            is Result.Success -> {
                                DebugHelper.logSuccess("✅ Test verisi yüklendi: ${loadResult.data} kelime")
                            }
                            is Result.Error -> {
                                DebugHelper.logError("⚠️  Test verisi yüklenemedi", loadResult.error)
                            }
                        }
                    } else {
                        DebugHelper.log("✅ Test verisi zaten mevcut")
                    }
                }
                is Result.Error -> {
                    DebugHelper.logError("Test verisi kontrolü başarısız", result.error)
                }
            }
        } catch (e: Exception) {
            DebugHelper.logError("ensureTestDataLoaded hatası", e)
        }
    }

    /**
     * Onboarding durumunu kontrol et
     * Mevcut kod - değişiklik yok
     */
    private suspend fun checkOnboardingStatus() {
        try {
            val setupStatus = preferencesManager.getAppSetupStatus()

            setupStatus.fold(
                onSuccess = { status ->
                    DebugHelper.log("📊 Setup Status:")
                    DebugHelper.log("  - Logged in: ${status.isUserLoggedIn}")
                    DebugHelper.log("  - Onboarding: ${status.isOnboardingCompleted}")
                    DebugHelper.log("  - Words selected: ${status.areWordsSelected}")

                    when {
                        !status.isOnboardingCompleted -> {
                            DebugHelper.log("➡️  Onboarding'e yönlendiriliyor...")
                            _navigationEvent.emit(SplashNavigationEvent.NavigateToOnboarding)
                        }
                        else -> {
                            DebugHelper.log("➡️  Home'a yönlendiriliyor...")
                            _navigationEvent.emit(SplashNavigationEvent.NavigateToMain)
                        }
                    }
                },
                onError = {
                    DebugHelper.logError("Setup status alınamadı", it)
                    _navigationEvent.emit(SplashNavigationEvent.NavigateToAuth)
                }
            )
        } catch (e: Exception) {
            DebugHelper.logError("checkOnboardingStatus hatası", e)
            _navigationEvent.emit(SplashNavigationEvent.NavigateToAuth)
        }
    }
}

/**
 * Navigation Events
 * Mevcut kod - değişiklik yok
 */
sealed interface SplashNavigationEvent {
    data object NavigateToAuth : SplashNavigationEvent
data object NavigateToOnboarding : SplashNavigationEvent
    data object NavigateToMain : SplashNavigationEvent
}