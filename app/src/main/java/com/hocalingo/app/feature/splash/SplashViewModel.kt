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
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

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
                DebugHelper.log("🎬 Splash başlatılıyor...")

                // ✅ 1. Paralel yükleme - internet gerektirmez
                val assetsJob = launch {
                    loadBundledPackages()
                }

                // ✅ 2. Minimum delay - sadece 500ms
                delay(500)

                // ✅ 3. Auth kontrolü - offline da çalışır
                val currentUser = authRepository.getCurrentUser()
                DebugHelper.log("👤 User: ${currentUser?.uid ?: "YOK"}")

                // ✅ 4. Assets yüklemesi bitsin (max 2 saniye bekle)
                kotlinx.coroutines.withTimeoutOrNull(2000) {
                    assetsJob.join()
                }

                // ✅ 5. Firebase test paketi - SADECE internet varsa
                // İnternet yoksa atla, uygulama açılsın
                try {
                    kotlinx.coroutines.withTimeout(1000) {
                        ensureTestDataLoaded()
                    }
                } catch (e: Exception) {
                    DebugHelper.log("⚠️ Test data atlandı (internet yok/yavaş)")
                }

                // ✅ 6. Navigation
                if (currentUser != null) {
                    checkOnboardingStatus()
                } else {
                    _navigationEvent.emit(SplashNavigationEvent.NavigateToAuth)
                }

            } catch (e: Exception) {
                DebugHelper.logError("💥 Splash error", e)
                // Hata olsa bile uygulama açılsın
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
            DebugHelper.log("📦 Bundled packages kontrol ediliyor...")

            when (val result = localPackageLoader.loadBundledPackagesIfNeeded()) {
                is Result.Success -> {
                    DebugHelper.logSuccess("✅ ${result.data} kelime hazır")
                }
                is Result.Error -> {
                    DebugHelper.log("⚠️ Assets atlandı") // Error değil warning
                }
            }
        } catch (e: Exception) {
            DebugHelper.log("⚠️ Assets yükleme atlandı")
        }
    }

    /**
     * Firebase test paketi yükle (backward compatibility)
     * Mevcut kod - değişiklik yok
     */
    private suspend fun ensureTestDataLoaded() {
        try {
            DebugHelper.log("🔍 Test data kontrol...")

            // ✅ Timeout ile kontrol - max 1 saniye
            withTimeout(1000) {
                when (val result = jsonLoader.isTestDataLoaded()) {
                    is Result.Success -> {
                        if (!result.data) {
                            // Sadece yüklü değilse yükle
                            jsonLoader.loadTestWords()
                        }
                    }
                    is Result.Error -> {
                        // Hata varsa atla
                    }
                }
            }
        } catch (e: Exception) {
            // Timeout veya hata - atla
            DebugHelper.log("⚠️ Test data atlandı")
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