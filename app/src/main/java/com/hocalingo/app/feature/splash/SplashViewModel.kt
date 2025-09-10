package com.hocalingo.app.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        checkAppState()
    }

    private fun checkAppState() {
        viewModelScope.launch {
            // Minimum splash duration
            delay(1500)

            try {
                // 1. Test verisini yükle (eğer yüklü değilse)
                loadTestDataIfNeeded()

                // 2. Kullanıcı durumunu kontrol et
                val currentUser = authRepository.getCurrentUser()

                if (currentUser == null) {
                    // Kullanıcı giriş yapmamış
                    _navigationEvent.emit(SplashNavigationEvent.NavigateToAuth)
                } else {
                    // Kullanıcı giriş yapmış, onboarding durumunu kontrol et
                    val setupStatus = preferencesManager.getAppSetupStatus()

                    setupStatus.fold(
                        onSuccess = { status ->
                            when {
                                !status.areWordsSelected -> {
                                    // Kelime seçimi yapılmamış
                                    _navigationEvent.emit(SplashNavigationEvent.NavigateToOnboarding)
                                }
                                else -> {
                                    // Her şey tamam, ana ekrana git
                                    _navigationEvent.emit(SplashNavigationEvent.NavigateToMain)
                                }
                            }
                        },
                        onError = {
                            // Hata durumunda onboarding'e yönlendir
                            _navigationEvent.emit(SplashNavigationEvent.NavigateToOnboarding)
                        }
                    )
                }
            } catch (e: Exception) {
                // Hata durumunda auth'a yönlendir
                _navigationEvent.emit(SplashNavigationEvent.NavigateToAuth)
            }
        }
    }

    private suspend fun loadTestDataIfNeeded() {
        try {
            // Test verisinin yüklü olup olmadığını kontrol et
            jsonLoader.isTestDataLoaded().fold(
                onSuccess = { isLoaded ->
                    if (!isLoaded) {
                        // Test verisini yükle
                        jsonLoader.loadTestWords().fold(
                            onSuccess = { count ->
                                println("HocaLingo: $count test kelimesi yüklendi")
                            },
                            onError = { error ->
                                println("HocaLingo: Test verisi yükleme hatası: ${error.message}")
                            }
                        )
                    } else {
                        println("HocaLingo: Test verisi zaten yüklü")
                    }
                },
                onError = { error ->
                    println("HocaLingo: Test verisi kontrol hatası: ${error.message}")
                    // Hata durumunda yine de yüklemeyi dene
                    jsonLoader.loadTestWords()
                }
            )
        } catch (e: Exception) {
            println("HocaLingo: Test verisi yükleme exception: ${e.message}")
        }
    }
}

// Navigation Events
sealed interface SplashNavigationEvent {
    object NavigateToAuth : SplashNavigationEvent
    object NavigateToOnboarding : SplashNavigationEvent
    object NavigateToMain : SplashNavigationEvent
}