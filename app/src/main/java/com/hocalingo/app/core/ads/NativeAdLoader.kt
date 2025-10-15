package com.hocalingo.app.core.ads

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
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
 * NativeAdLoader - Native Ad Yöneticisi
 *
 * Package: app/src/main/java/com/hocalingo/app/core/ads/
 *
 * Native reklamların yüklenmesi, cache'lenmesi ve yönetimi.
 * - Preloading (background'da yükle)
 * - Caching (bellekte tut)
 * - Premium bypass
 * - Multiple ad instances
 */
@Singleton
class NativeAdLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subscriptionRepository: SubscriptionRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Test ad unit IDs (Replace with real IDs in production)
    private val nativeAdUnitId = "ca-app-pub-3940256099942544/2247696110" // Test ID

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
     * PRELOADING
     * ============================================
     */

    /**
     * Preload all native ads (called from MainActivity)
     */
    fun preloadNativeAds() {
        scope.launch {
            if (!shouldShowAnyAd()) {
                DebugHelper.log("👑 Premium user - No native ads")
                return@launch
            }

            DebugHelper.log("🔄 Preloading native ads...")
            loadSelectionScreenAd()
            loadStudyScreenAd()
        }
    }

    /**
     * ============================================
     * SELECTION SCREEN AD
     * ============================================
     */

    /**
     * Load native ad for concept selection screen
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

            _selectionAdState.value = AdState.Loading
            DebugHelper.log("🔄 Loading selection screen native ad...")

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
                    }

                    override fun onAdClicked() {
                        DebugHelper.log("👆 Selection screen ad clicked")
                    }

                    override fun onAdOpened() {
                        DebugHelper.log("📺 Selection screen ad opened")
                    }

                    override fun onAdClosed() {
                        DebugHelper.log("❌ Selection screen ad closed")
                        // Reload ad for next time
                        _selectionScreenAd.value = null
                        loadSelectionScreenAd()
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build()
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    /**
     * Get selection screen ad (for UI)
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
     * STUDY SCREEN AD
     * ============================================
     */

    /**
     * Load native ad for study screen
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

            _studyAdState.value = AdState.Loading
            DebugHelper.log("🔄 Loading study screen native ad...")

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
                    }

                    override fun onAdClicked() {
                        DebugHelper.log("👆 Study screen ad clicked")
                    }

                    override fun onAdOpened() {
                        DebugHelper.log("📺 Study screen ad opened")
                    }

                    override fun onAdClosed() {
                        DebugHelper.log("❌ Study screen ad closed")
                        // Reload ad for next time
                        _studyScreenAd.value = null
                        loadStudyScreenAd()
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                        .build()
                )
                .build()

            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    /**
     * Get study screen ad (for UI)
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
     * ============================================
     * CLEANUP
     * ============================================
     */

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