package com.hocalingo.app.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hocalingo.app.core.base.Result
import com.hocalingo.app.core.common.DebugHelper
import com.revenuecat.purchases.Package
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
 * SubscriptionViewModel
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * Handles subscription state and purchase flow
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val getSubscriptionStatusUseCase: GetSubscriptionStatusUseCase,
    private val purchaseSubscriptionUseCase: PurchaseSubscriptionUseCase,
    private val restorePurchasesUseCase: RestorePurchasesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SubscriptionUiState())
    val uiState: StateFlow<SubscriptionUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<SubscriptionEffect>()
    val effect: SharedFlow<SubscriptionEffect> = _effect.asSharedFlow()

    init {
        observeSubscriptionState()
        loadAvailablePackages()
    }

    /**
     * Observe subscription state from repository
     */
    private fun observeSubscriptionState() {
        viewModelScope.launch {
            getSubscriptionStatusUseCase.observeSubscriptionState().collect { state ->
                _uiState.update { it.copy(currentSubscription = state) }
            }
        }
    }

    /**
     * Load available packages from RevenueCat
     */
    private fun loadAvailablePackages() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            when (val result = purchaseSubscriptionUseCase.getAvailablePackages()) {
                is Result.Success -> {
                    val packages = result.data

                    // Sort: Monthly, Quarterly, Yearly
                    val sortedPackages = packages.sortedBy { pkg ->
                        when {
                            pkg.identifier.contains("monthly") -> 0
                            pkg.identifier.contains("quarterly") -> 1
                            pkg.identifier.contains("yearly") -> 2
                            else -> 3
                        }
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            availablePackages = sortedPackages,
                            selectedPackage = sortedPackages.getOrNull(1) // Default: Quarterly
                        )
                    }

                    DebugHelper.logSuccess("Packages loaded: ${packages.size}")
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effect.emit(SubscriptionEffect.ShowError("Paketler yüklenemedi"))
                    DebugHelper.logError("Failed to load packages", result.error)
                }
            }
        }
    }

    /**
     * Handle user events
     */
    fun onEvent(event: SubscriptionEvent) {
        when (event) {
            is SubscriptionEvent.SelectPackage -> selectPackage(event.packageItem)
            SubscriptionEvent.PurchaseSelected -> purchaseSelectedPackage()
            SubscriptionEvent.RestorePurchases -> restorePurchases()
            SubscriptionEvent.DismissPaywall -> {
                viewModelScope.launch {
                    _effect.emit(SubscriptionEffect.DismissPaywall)
                }
            }
        }
    }

    /**
     * Select a package
     */
    private fun selectPackage(packageItem: Package) {
        _uiState.update { it.copy(selectedPackage = packageItem) }
    }

    /**
     * Purchase selected package
     */
    private fun purchaseSelectedPackage() {
        val selectedPackage = _uiState.value.selectedPackage

        if (selectedPackage == null) {
            viewModelScope.launch {
                _effect.emit(SubscriptionEffect.ShowError("Lütfen bir paket seçin"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPurchasing = true) }

            // TODO: Activity reference gerekli, şimdilik error göster
            _effect.emit(
                SubscriptionEffect.ShowError(
                    "Satın alma özelliği yakında eklenecek"
                )
            )

            _uiState.update { it.copy(isPurchasing = false) }
        }
    }

    /**
     * Restore previous purchases
     */
    private fun restorePurchases() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRestoring = true) }

            when (val result = restorePurchasesUseCase()) {
                is Result.Success -> {
                    _uiState.update { it.copy(isRestoring = false) }
                    _effect.emit(SubscriptionEffect.ShowMessage("✅ Satın almalar geri yüklendi"))
                    _effect.emit(SubscriptionEffect.PurchaseSuccess)
                }
                is Result.Error -> {
                    _uiState.update { it.copy(isRestoring = false) }
                    _effect.emit(SubscriptionEffect.ShowError("Geri yükleme başarısız"))
                }
            }
        }
    }
}

/**
 * UI State
 */
data class SubscriptionUiState(
    val isLoading: Boolean = false,
    val isPurchasing: Boolean = false,
    val isRestoring: Boolean = false,
    val currentSubscription: SubscriptionState = SubscriptionState.FREE,
    val availablePackages: List<Package> = emptyList(),
    val selectedPackage: Package? = null
)

/**
 * User Events
 */
sealed interface SubscriptionEvent {
    data class SelectPackage(val packageItem: Package) : SubscriptionEvent
    data object PurchaseSelected : SubscriptionEvent
    data object RestorePurchases : SubscriptionEvent
    data object DismissPaywall : SubscriptionEvent
}

/**
 * Side Effects
 */
sealed interface SubscriptionEffect {
    data class ShowMessage(val message: String) : SubscriptionEffect
    data class ShowError(val message: String) : SubscriptionEffect
    data object PurchaseSuccess : SubscriptionEffect
    data object DismissPaywall : SubscriptionEffect
}