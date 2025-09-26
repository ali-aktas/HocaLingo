package com.hocalingo.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseUser
import com.hocalingo.app.core.base.Result
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
 * ViewModel for Authentication Screen
 * BASİT VERSİYON - AuthStateManager olmadan
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Navigation Effects
    private val _effect = MutableSharedFlow<AuthEffect>()
    val effect: SharedFlow<AuthEffect> = _effect.asSharedFlow()

    init {
        checkAuthStatus()
    }

    /**
     * Check if user is already authenticated
     * BASİT: Kullanıcı giriş yaptıysa direkt onboarding'e yönlendir
     */
    private fun checkAuthStatus() {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            viewModelScope.launch {
                // Kullanıcı giriş yapmış, direkt onboarding'e gönder
                // Onboarding ekranı zaten gerekli kontrolleri yapacak
                _effect.emit(AuthEffect.NavigateToOnboarding)
            }
        }
    }

    /**
     * Handle UI events
     */
    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.GoogleSignInResult -> handleGoogleSignIn(event.idToken)
            AuthEvent.SignInAnonymously -> signInAnonymously()
            AuthEvent.RetryClicked -> clearError()
        }
    }

    /**
     * Handle Google Sign-In with ID token
     * BASİT: Başarılı girişte direkt onboarding'e git
     */
    private fun handleGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = if (authRepository.isAnonymousUser()) {
                // Link anonymous account with Google
                authRepository.linkWithGoogle(idToken)
            } else {
                // Regular Google sign in
                authRepository.signInWithGoogle(idToken)
            }

            when (result) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            user = result.data
                        )
                    }
                    // Basit: Her zaman onboarding'e git
                    _effect.emit(AuthEffect.NavigateToOnboarding)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Google ile giriş başarısız. Lütfen tekrar deneyin."
                        )
                    }
                }
            }
        }
    }

    /**
     * Sign in anonymously (guest mode)
     * BASİT: Başarılı girişte direkt onboarding'e git
     */
    private fun signInAnonymously() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = authRepository.signInAnonymously()) {
                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            user = result.data,
                            isAnonymous = true
                        )
                    }
                    // Basit: Her zaman onboarding'e git
                    _effect.emit(AuthEffect.NavigateToOnboarding)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Misafir girişi başarısız. Lütfen tekrar deneyin."
                        )
                    }
                }
            }
        }
    }

    /**
     * Clear error message
     */
    private fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI State for Auth Screen
 */
data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isAnonymous: Boolean = false,
    val user: FirebaseUser? = null,
    val error: String? = null
)

/**
 * UI Events
 */
sealed interface AuthEvent {
    data class GoogleSignInResult(val idToken: String) : AuthEvent
    data object SignInAnonymously : AuthEvent
    data object RetryClicked : AuthEvent
}

/**
 * Side Effects
 * NOT: NavigateToWordSelection ve NavigateToHome kaldırıldı, sadece Onboarding var
 */
sealed interface AuthEffect {
    data object NavigateToOnboarding : AuthEffect
    data class ShowError(val message: String) : AuthEffect
}