package com.hocalingo.app.core.ads

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.hocalingo.app.BuildConfig
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.feature.subscription.SubscriptionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NativeAdLoader - Native Ad Manager with LAZY LOADING ✅
 *
 * Package: app/src/main/java/com/hocalingo/app/core/ads/
 *
 * ✅ OPTIMIZED: Lazy loading strategy
 * - Selection screen ad: Loaded ONLY when user enters selection screen
 * - Study screen ad: Loaded ONLY when user starts studying
 *
 * ❌ REMOVED: Aggressive preloading in MainActivity
 *
 * Benefits:
 * - Reduced unnecessary ad requests (650 → ~300)
 * - Improved impression ratio (17% → 50%+)
 * - Better user experience (ads load when needed)
 * - Less network usage
 */
@Singleton
class NativeAdLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subscriptionRepository: SubscriptionRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Native Ad Unit ID (Production vs Debug)
    private val nativeAdUnitId = if (BuildConfig.DEBUG) {
        "ca-app-pub-3940256099942544/2247696110" // Test ID for debug
    } else {
        BuildConfig.ADMOB_NATIVE_AD_UNIT_ID // Production ID
    }

    // Native Ad Cache (Multiple ads for different screens)
    private val _selectionScreenAd = MutableStateFlow<NativeAd?>(null)
    val selectionScreenAd: StateFlow<NativeAd?> = _selectionScreenAd.asStateFlow()

    private val _studyScreenAd = MutableStateFlow<NativeAd?>(null)
    val studyScreenAd: StateFlow<NativeAd?> = _studyScreenAd.asStateFlow()

    // Ad States
    private val _selectionAdState = MutableStateFlow<AdState>(AdState.NotLoaded)
    val selectionAdState: StateFlow<AdState> = _selectionAdState.asStateFlow()

    private val _studyAdState = MutableStateFlow<AdState>(AdState.NotLoaded)
    val studyAdState: StateFlow<AdState> = _studyAdState.asStateFlow()

    /**
     * ============================================
     * ❌ REMOVED: Aggressive preloading
     * ============================================
     *
     * OLD CODE (REMOVED):
     * fun preloadNativeAds() {
     *     loadSelectionScreenAd()  // ❌ Unnecessary request
     *     loadStudyScreenAd()      // ❌ Unnecessary request
     * }
     *
     * PROBLEM: Loaded ads that might never be shown
     * RESULT: 650 requests, 111 impressions (17%)
     */

    /**
     * ============================================
     * SELECTION SCREEN AD - LAZY LOADING ✅
     * ============================================
     */

    /**
     * Load native ad for concept selection screen
     *
     * ✅ CALL THIS: When user enters PackageSelectionScreen or WordSelectionScreen
     * ❌ DON'T CALL: On app launch (MainActivity)
     */
    fun loadSelectionScreenAd() {
        scope.launch {
            if (!shouldShowAnyAd()) {
                DebugHelper.log("👑 Premium user - Skipping selection screen ad")
                return@launch
            }

            if (_selectionScreenAd.value != null) {
                DebugHelper.log("✅ Selection screen ad already loaded")
                return@launch
            }

            // ✅ Prevent duplicate loading
            if (_selectionAdState.value is AdState.Loading) {
                DebugHelper.log("⚠️ Selection screen ad already loading...")
                return@launch
            }

            _selectionAdState.value = AdState.Loading
            DebugHelper.log("🔄 Loading selection screen native ad (LAZY)...")

            val adLoader = AdLoader.Builder(context, nativeAdUnitId)
                .forNativeAd { nativeAd ->
                    DebugHelper.logSuccess("✅ Selection screen native ad loaded")
                    _selectionScreenAd.value = nativeAd
                    _selectionAdState.value = AdState.Loaded(nativeAd.responseInfo?.responseId ?: "")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        DebugHelper.logError("❌ Selection screen ad failed: ${error.message}")
                        _selectionAdState.value = AdState.Error(error.message)

                        // ❌ REMOVED: No retry on failure (prevents unnecessary requests)
                        // Old code tried to reload → more requests, same failures
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setRequestMultipleImages(false)
                        .build()
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    /**
     * Get current selection screen ad
     */
    fun getSelectionScreenAd(): NativeAd? {
        return _selectionScreenAd.value
    }

    /**
     * Clear selection screen ad
     */
    fun clearSelectionScreenAd() {
        _selectionScreenAd.value?.destroy()
        _selectionScreenAd.value = null
        _selectionAdState.value = AdState.NotLoaded
        DebugHelper.log("🗑️ Selection screen ad cleared")
    }

    /**
     * ============================================
     * STUDY SCREEN AD - LAZY LOADING ✅
     * ============================================
     */

    /**
     * Load native ad for study screen
     *
     * ✅ CALL THIS: When user starts studying (StudyViewModel init)
     * ❌ DON'T CALL: On app launch (MainActivity)
     */
    fun loadStudyScreenAd() {
        scope.launch {
            if (!shouldShowAnyAd()) {
                DebugHelper.log("👑 Premium user - Skipping study screen ad")
                return@launch
            }

            if (_studyScreenAd.value != null) {
                DebugHelper.log("✅ Study screen ad already loaded")
                return@launch
            }

            // ✅ Prevent duplicate loading
            if (_studyAdState.value is AdState.Loading) {
                DebugHelper.log("⚠️ Study screen ad already loading...")
                return@launch
            }

            _studyAdState.value = AdState.Loading
            DebugHelper.log("🔄 Loading study screen native ad (LAZY)...")

            val adLoader = AdLoader.Builder(context, nativeAdUnitId)
                .forNativeAd { nativeAd ->
                    DebugHelper.logSuccess("✅ Study screen native ad loaded")
                    _studyScreenAd.value = nativeAd
                    _studyAdState.value = AdState.Loaded(nativeAd.responseInfo?.responseId ?: "")
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        DebugHelper.logError("❌ Study screen ad failed: ${error.message}")
                        _studyAdState.value = AdState.Error(error.message)

                        // ❌ REMOVED: No retry on failure (prevents unnecessary requests)
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setRequestMultipleImages(false)
                        .build()
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    /**
     * Get current study screen ad
     */
    fun getStudyScreenAd(): NativeAd? {
        return _studyScreenAd.value
    }

    /**
     * Clear study screen ad
     */
    fun clearStudyScreenAd() {
        _studyScreenAd.value?.destroy()
        _studyScreenAd.value = null
        _studyAdState.value = AdState.NotLoaded
        DebugHelper.log("🗑️ Study screen ad cleared")
    }

    /**
     * ============================================
     * PREMIUM CHECK
     * ============================================
     */

    /**
     * Should show any ad (premium bypass)
     */
    private suspend fun shouldShowAnyAd(): Boolean {
        val isPremium = subscriptionRepository.isPremium()
        if (isPremium) {
            DebugHelper.log("👑 Premium user - No native ads")
        }
        return !isPremium
    }

    /**
     * ✅ Premium user için tüm reklamları cache'den temizle
     */
    suspend fun clearAdsForPremiumUser() {
        val isPremium = subscriptionRepository.isPremium()
        if (isPremium) {
            DebugHelper.log("🗑️ Clearing all native ads for premium user")

            // Destroy existing ads
            _selectionScreenAd.value?.destroy()
            _studyScreenAd.value?.destroy()

            // Clear cache
            _selectionScreenAd.value = null
            _studyScreenAd.value = null

            // Reset states
            _selectionAdState.value = AdState.NotLoaded
            _studyAdState.value = AdState.NotLoaded

            DebugHelper.logSuccess("✅ All native ads cleared for premium user")
        }
    }

    /**
     * Clear all cached ads
     */
    fun clearAllAds() {
        clearSelectionScreenAd()
        clearStudyScreenAd()
        DebugHelper.log("🗑️ All native ads cleared")
    }

    /**
     * Destroy all ads (on app close)
     */
    fun destroyAllAds() {
        _selectionScreenAd.value?.destroy()
        _studyScreenAd.value?.destroy()
        _selectionScreenAd.value = null
        _studyScreenAd.value = null
        DebugHelper.log("💥 All native ads destroyed")
    }
}