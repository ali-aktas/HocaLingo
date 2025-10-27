package com.hocalingo.app.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
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
 * AdMobManager - Ana Reklam Yönetim Sınıfı
 *
 * Package: app/src/main/java/com/hocalingo/app/core/ads/
 *
 * AdMob entegrasyonunun merkezi yöneticisi.
 * - Premium kontrolü
 * - Ad loading/preloading
 * - Ad showing with callbacks
 * - Counter management integration
 */
@Singleton
class AdMobManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subscriptionRepository: SubscriptionRepository,
    private val adCounterDataStore: AdCounterDataStore
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Ad Unit IDs (Test IDs for development)
    private val rewardedAdUnitId = "ca-app-pub-3940256099942544/5224354917" // Test ID

    // Rewarded Ad States
    private val _appLaunchAdState = MutableStateFlow<AdState>(AdState.NotLoaded)
    val appLaunchAdState: StateFlow<AdState> = _appLaunchAdState.asStateFlow()

    private val _studyRewardedAdState = MutableStateFlow<AdState>(AdState.NotLoaded)
    val studyRewardedAdState: StateFlow<AdState> = _studyRewardedAdState.asStateFlow()

    // Cached Rewarded Ads
    private var appLaunchRewardedAd: RewardedAd? = null
    private var studyRewardedAd: RewardedAd? = null

    // Initialization flag
    private var isInitialized = false

    /**
     * ============================================
     * INITIALIZATION
     * ============================================
     */

    /**
     * Initialize AdMob SDK
     * MainActivity onCreate'de çağrılmalı
     */
    fun initialize() {
        if (isInitialized) {
            DebugHelper.log("⚠️ AdMob already initialized")
            return
        }

        DebugHelper.log("🚀 Initializing AdMob SDK...")

        MobileAds.initialize(context) { initializationStatus ->
            DebugHelper.logSuccess("✅ AdMob SDK initialized")
            DebugHelper.log("Adapter statuses: ${initializationStatus.adapterStatusMap}")
            isInitialized = true

            // Preload ads
            scope.launch {
                preloadAdsIfNeeded()
            }
        }
    }

    /**
     * ============================================
     * PREMIUM CHECK
     * ============================================
     */

    /**
     * Premium kullanıcı mı kontrol et
     */
    suspend fun isPremiumUser(): Boolean {
        return subscriptionRepository.isPremium()
    }

    /**
     * Should show any ad (premium bypass)
     */
    private suspend fun shouldShowAnyAd(): Boolean {
        val isPremium = isPremiumUser()
        if (isPremium) {
            DebugHelper.log("👑 Premium user - No ads")
        }
        return !isPremium
    }

    /**
     * ✅ NEW: Premium kullanıcı için tüm rewarded ad cache'leri temizle
     */
    suspend fun clearAdsForPremiumUser() {
        val isPremium = isPremiumUser()
        if (isPremium) {
            DebugHelper.log("🗑️ Clearing all rewarded ads for premium user")
            appLaunchRewardedAd = null
            studyRewardedAd = null
            _appLaunchAdState.value = AdState.NotLoaded
            _studyRewardedAdState.value = AdState.NotLoaded
        }
    }

    /**
     * Check if should show app launch ad
     */
    suspend fun shouldShowAppLaunchAd(): Boolean {
        if (!shouldShowAnyAd()) return false

        return adCounterDataStore.shouldShowAppLaunchAd()
    }

    /**
     * Increment app launch count
     * Her app açılışında çağrılmalı
     */
    suspend fun incrementAppLaunchCount() {
        if (!shouldShowAnyAd()) return

        adCounterDataStore.incrementAppLaunchCount()
    }

    /**
     * Reset study word count (ad gösterilemediğinde kullanılır)
     */
    suspend fun resetStudyWordCount() {
        if (!shouldShowAnyAd()) return

        DebugHelper.log("🔄 Resetting study word count")
        adCounterDataStore.resetStudyWordCount()
    }

    /**
     * Load app launch rewarded ad
     */
    fun loadAppLaunchRewardedAd() {
        scope.launch {
            if (!shouldShowAnyAd()) {
                DebugHelper.log("👑 Premium user - Skipping app launch ad load")
                return@launch
            }

            if (appLaunchRewardedAd != null) {
                DebugHelper.log("✅ App launch ad already loaded")
                return@launch
            }

            _appLaunchAdState.value = AdState.Loading
            DebugHelper.log("🔄 Loading app launch rewarded ad...")

            val adRequest = AdRequest.Builder().build()

            RewardedAd.load(
                context,
                rewardedAdUnitId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        DebugHelper.logSuccess("✅ App launch rewarded ad loaded")
                        appLaunchRewardedAd = ad
                        _appLaunchAdState.value = AdState.Loaded(ad.adUnitId)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        DebugHelper.logError("❌ App launch ad failed to load: ${error.message}")
                        appLaunchRewardedAd = null
                        _appLaunchAdState.value = AdState.Error(error.message)
                    }
                }
            )
        }
    }

    /**
     * Show app launch rewarded ad
     */
    suspend fun showAppLaunchRewardedAd(
        activity: Activity,
        onAdShown: () -> Unit = {},
        onAdDismissed: () -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ) {
        if (!shouldShowAnyAd()) {
            DebugHelper.log("👑 Premium user - Skipping ad")
            return
        }

        val ad = appLaunchRewardedAd
        if (ad == null) {
            DebugHelper.logError("❌ App launch ad not loaded")
            onAdFailed("Ad not loaded")
            return
        }

        _appLaunchAdState.value = AdState.Showing
        DebugHelper.log("📺 Showing app launch rewarded ad...")

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                DebugHelper.log("📺 App launch ad showed")
                onAdShown()
            }

            override fun onAdDismissedFullScreenContent() {
                DebugHelper.log("❌ App launch ad dismissed")
                appLaunchRewardedAd = null
                _appLaunchAdState.value = AdState.Dismissed

                scope.launch {
                    adCounterDataStore.resetAppLaunchCount()
                }

                onAdDismissed()

                // Preload next ad
                loadAppLaunchRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                DebugHelper.logError("❌ App launch ad failed to show: ${error.message}")
                appLaunchRewardedAd = null
                _appLaunchAdState.value = AdState.Error(error.message)
                onAdFailed(error.message)
            }
        }

        ad.show(activity, OnUserEarnedRewardListener { reward ->
            DebugHelper.logSuccess("🎁 Reward earned: ${reward.amount} ${reward.type}")
            _appLaunchAdState.value = AdState.Completed
        })
    }

    /**
     * ============================================
     * STUDY REWARDED AD
     * ============================================
     */

    /**
     * Check if should show study rewarded ad
     */
    suspend fun shouldShowStudyRewardedAd(): Boolean {
        if (!shouldShowAnyAd()) return false

        return adCounterDataStore.shouldShowStudyRewardedAd()
    }

    /**
     * Increment study word count
     * Her kelime tamamlandığında çağrılmalı
     */
    suspend fun incrementStudyWordCount() {
        if (!shouldShowAnyAd()) return

        adCounterDataStore.incrementStudyWordCount()
    }

    /**
     * Load study rewarded ad
     */
    fun loadStudyRewardedAd() {
        scope.launch {
            if (!shouldShowAnyAd()) {
                DebugHelper.log("👑 Premium user - Skipping study ad load")
                return@launch
            }

            if (studyRewardedAd != null) {
                DebugHelper.log("✅ Study ad already loaded")
                return@launch
            }

            _studyRewardedAdState.value = AdState.Loading
            DebugHelper.log("🔄 Loading study rewarded ad...")

            val adRequest = AdRequest.Builder().build()

            RewardedAd.load(
                context,
                rewardedAdUnitId,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        DebugHelper.logSuccess("✅ Study rewarded ad loaded")
                        studyRewardedAd = ad
                        _studyRewardedAdState.value = AdState.Loaded(ad.adUnitId)
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        DebugHelper.logError("❌ Study ad failed to load: ${error.message}")
                        studyRewardedAd = null
                        _studyRewardedAdState.value = AdState.Error(error.message)
                    }
                }
            )
        }
    }

    /**
     * Show study rewarded ad
     */
    suspend fun showStudyRewardedAd(
        activity: Activity,
        onAdShown: () -> Unit = {},
        onAdDismissed: () -> Unit = {},
        onAdFailed: (String) -> Unit = {}
    ) {
        if (!shouldShowAnyAd()) {
            DebugHelper.log("👑 Premium user - Skipping ad")
            return
        }

        val ad = studyRewardedAd
        if (ad == null) {
            DebugHelper.logError("❌ Study ad not loaded")
            onAdFailed("Ad not loaded")
            return
        }

        _studyRewardedAdState.value = AdState.Showing
        DebugHelper.log("📺 Showing study rewarded ad...")

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                DebugHelper.log("📺 Study ad showed")
                onAdShown()
            }

            override fun onAdDismissedFullScreenContent() {
                DebugHelper.log("❌ Study ad dismissed")
                studyRewardedAd = null
                _studyRewardedAdState.value = AdState.Dismissed

                scope.launch {
                    adCounterDataStore.resetStudyWordCount()
                }

                onAdDismissed()

                // Preload next ad
                loadStudyRewardedAd()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                DebugHelper.logError("❌ Study ad failed to show: ${error.message}")
                studyRewardedAd = null
                _studyRewardedAdState.value = AdState.Error(error.message)
                onAdFailed(error.message)
            }
        }

        ad.show(activity, OnUserEarnedRewardListener { reward ->
            DebugHelper.logSuccess("🎁 Reward earned: ${reward.amount} ${reward.type}")
            _studyRewardedAdState.value = AdState.Completed
        })
    }

    /**
     * ============================================
     * PRELOADING
     * ============================================
     */

    /**
     * Preload all ads if needed
     */
    private suspend fun preloadAdsIfNeeded() {
        if (!shouldShowAnyAd()) {
            DebugHelper.log("👑 Premium user - No preloading")
            return
        }

        DebugHelper.log("🔄 Preloading ads...")
        loadAppLaunchRewardedAd()
        loadStudyRewardedAd()
    }

    /**
     * ============================================
     * UTILITIES
     * ============================================
     */

    /**
     * Get debug info
     */
    suspend fun getDebugInfo(): String {
        val counterInfo = adCounterDataStore.getDebugInfo()
        val isPremium = isPremiumUser()

        return """
            🎯 AdMob Manager Debug Info:
            
            Premium Status: ${if (isPremium) "👑 Premium" else "🆓 Free"}
            
            $counterInfo
            
            Ad States:
            - App Launch Ad: ${_appLaunchAdState.value}
            - Study Ad: ${_studyRewardedAdState.value}
        """.trimIndent()
    }

    /**
     * Clear all cached ads
     */
    fun clearAllAds() {
        appLaunchRewardedAd = null
        studyRewardedAd = null
        _appLaunchAdState.value = AdState.NotLoaded
        _studyRewardedAdState.value = AdState.NotLoaded
        DebugHelper.log("🗑️ All cached ads cleared")
    }
}