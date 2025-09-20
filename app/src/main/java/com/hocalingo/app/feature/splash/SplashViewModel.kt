package com.hocalingo.app.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.common.UserPreferencesManager
import com.hocalingo.app.core.database.JsonLoader
import com.hocalingo.app.core.database.MainDatabaseSeeder
import com.hocalingo.app.feature.auth.data.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * SplashViewModel - FIXED VERSION
 *
 * ✅ İlk giriş → Auth → Onboarding → Word Selection → Home
 * ✅ Sonraki girişler → DİREKT HOME (basit fix)
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val jsonLoader: JsonLoader,
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
                DebugHelper.log("Minimum splash delay başlatılıyor...")
                // Minimum splash duration
                delay(1500)

                // Test verisini yükle
                ensureTestDataLoaded()

                DebugHelper.log("User durumu kontrol ediliyor...")
                // Kullanıcı durumunu kontrol et
                val currentUser = authRepository.getCurrentUser()
                DebugHelper.log("Current user: ${currentUser?.uid ?: "null"}")

                if (currentUser == null) {
                    // Kullanıcı giriş yapmamış → Auth ekranına git
                    DebugHelper.log("Kullanıcı giriş yapmamış -> Auth")
                    _navigationEvent.emit(SplashNavigationEvent.NavigateToAuth)
                } else {
                    // ✅ SIMPLE FIX: Kullanıcı giriş yapmış → DİREKT HOME'A GİT
                    DebugHelper.log("Kullanıcı giriş yapmış -> DİREKT HOME")
                    _navigationEvent.emit(SplashNavigationEvent.NavigateToMain)

                    // Eğer setup tamamlanmamışsa ileride kontrol edebiliriz
                    // Şimdilik basit yaklaşım: Her giriş yapan user Home'a gitsin
                }

            } catch (e: Exception) {
                DebugHelper.logError("Splash checkAppState HATASI", e)
                // Hata durumunda auth'a yönlendir
                _navigationEvent.emit(SplashNavigationEvent.NavigateToAuth)
            }
        }
    }

    /**
     * Test verisini sadece gerekirse yükle
     */
    private suspend fun ensureTestDataLoaded() {
        try {
            DebugHelper.log("Test verisi kontrol ediliyor...")

            val isTestLoadedResult = jsonLoader.isTestDataLoaded()
            DebugHelper.log("Test data check result: $isTestLoadedResult")

            when (isTestLoadedResult) {
                is com.hocalingo.app.core.common.base.Result.Success -> {
                    if (!isTestLoadedResult.data) {
                        DebugHelper.log("Test verisi yüklü değil, yükleniyor...")
                        val loadResult = jsonLoader.loadTestWords()
                        when (loadResult) {
                            is com.hocalingo.app.core.common.base.Result.Success -> {
                                DebugHelper.log("Test verisi başarıyla yüklendi: ${loadResult.data} kelime")
                            }
                            is com.hocalingo.app.core.common.base.Result.Error -> {
                                DebugHelper.logError("Test verisi yükleme hatası", loadResult.error)
                            }
                        }
                    } else {
                        DebugHelper.log("Test verisi zaten yüklü, atlanıyor...")
                    }
                }
                is com.hocalingo.app.core.common.base.Result.Error -> {
                    DebugHelper.logError("Test verisi kontrol hatası", isTestLoadedResult.error)
                    // Hata durumunda yine de yüklemeyi dene
                    DebugHelper.log("Hata durumunda fallback loading deneniyor...")
                    jsonLoader.loadTestWords()
                }
            }
        } catch (e: Exception) {
            DebugHelper.logError("ensureTestDataLoaded exception", e)
        }
    }
}

// Navigation Events
sealed interface SplashNavigationEvent {
    data object NavigateToAuth : SplashNavigationEvent
    data object NavigateToOnboarding : SplashNavigationEvent
    data object NavigateToMain : SplashNavigationEvent
}