package com.hocalingo.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hocalingo.app.core.ads.AdMobManager
import com.hocalingo.app.core.ads.NativeAdLoader
import com.hocalingo.app.core.common.DebugHelper
import com.hocalingo.app.core.common.TrialOfferDataStore
import com.hocalingo.app.core.ui.navigation.HocaBottomNavigationBar
import com.hocalingo.app.core.ui.navigation.shouldShowBottomNavigation
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.hocalingo.app.core.config.RemoteConfigManager
import com.hocalingo.app.feature.subscription.PaywallBottomSheet

/**
 * MainActivity - Professional Edge-to-Edge Implementation with AdMob
 *
 * Package: app/src/main/java/com/hocalingo/app/
 *
 * ✅ API 35 Compatible
 * ✅ AdMob Integration (Rewarded & Native Ads)
 * ✅ Status Bar: Transparent, content extends behind it
 * ✅ Navigation Bar: Proper padding for gesture navigation
 * ✅ Central WindowInsets management for all screens
 *
 * CRITICAL CHANGES:
 * - Added contentWindowInsets = WindowInsets(0, 0, 0, 0) to Scaffold
 * - This allows each screen to manage its own status bar padding
 * - Bottom navigation handles gesture bar padding automatically
 * - AdMob initialized on app start
 * - Native ads preloaded for better UX
 * - App launch counter for rewarded ads
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // ✅ AdMob Dependencies
    @Inject
    lateinit var adMobManager: AdMobManager

    @Inject
    lateinit var nativeAdLoader: NativeAdLoader

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager

    @Inject
    lateinit var trialOfferDataStore: TrialOfferDataStore


    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen first
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Initialize AdMob SDK
        initializeAdMob()

        // ✅ CRITICAL FIX: Premium kontrolü ile app launch counter
        incrementAppLaunchCountIfFree()

        // ✅ Enable edge-to-edge (mandatory for API 35)
        enableEdgeToEdge()

        lifecycleScope.launch {
            remoteConfigManager.fetchAndActivate()
            DebugHelper.log("🔥 Remote Config fetched on app start")
        }

        setContent {
            // Theme management
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val shouldUseDarkTheme = themeViewModel.shouldUseDarkTheme()
            // ✅ Trial Offer State (for second showing after 3 days)
            var showTrialOffer by remember { mutableStateOf(false) }
            var showPaywall by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            HocaLingoTheme(
                darkTheme = shouldUseDarkTheme,
                dynamicColor = true
            ) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // 🎨 System bars configuration
                val view = LocalView.current
                val window = (view.context as Activity).window

                SideEffect {
                    // Status bar: completely transparent
                    window.statusBarColor = Color.Transparent.toArgb()

                    // Navigation bar: transparent (for gesture navigation)
                    window.navigationBarColor = Color.Transparent.toArgb()

                    // Icon colors based on theme
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !shouldUseDarkTheme
                        isAppearanceLightNavigationBars = !shouldUseDarkTheme
                    }
                }

                // ✅ Check and show app launch rewarded ad
                LaunchedEffect(Unit) {
                    // Delay 2 seconds for better UX
                    delay(2000)

                    if (adMobManager.shouldShowAppLaunchAd()) {
                        adMobManager.showAppLaunchRewardedAd(
                            activity = this@MainActivity,
                            onAdShown = {},
                            onAdDismissed = {},
                            onAdFailed = { error ->
                                // Log error but don't show to user
                                println("❌ App launch ad failed: $error")
                            }
                        )
                    }
                }

                // ✅ Check trial offer (second showing after 3 days)
                LaunchedEffect(currentRoute) {
                    if (currentRoute == HocaRoutes.HOME) {
                        delay(2000) // Wait 2 seconds

                        val shouldShow = trialOfferDataStore.shouldShowTrialOffer()
                        if (shouldShow) {
                            val prefs = trialOfferDataStore.getDebugInfo()
                            DebugHelper.log("Trial Offer Check: $prefs")

                            // Mark as second shown
                            trialOfferDataStore.markSecondShown()
                            showTrialOffer = true
                        }
                    }
                }

                // ✅ CRITICAL FIX: contentWindowInsets removes default padding
                // Now each screen manages its own status bar padding
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    containerColor = MaterialTheme.colorScheme.background,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0), // 👈 BU SATIR MUTLAKA OLMALI!
                    bottomBar = {
                        if (shouldShowBottomNavigation(currentRoute)) {
                            HocaBottomNavigationBar(navController = navController)
                        }
                    }
                ) { paddingValues ->
                    // Content starts here
                    // Each screen will add its own statusBarsPadding()
                    HocaLingoNavigation(
                        navController = navController,
                        trialOfferDataStore = trialOfferDataStore,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(paddingValues) // Only bottom nav padding
                    )
                }

                // ✅ Premium Push - 3 Gün Sonrası
                if (showTrialOffer) {
                    PaywallBottomSheet(
                        onDismiss = {
                            showTrialOffer = false
                            scope.launch {
                                trialOfferDataStore.markPermanentlyDismissed()
                            }
                        },
                        onPurchaseSuccess = {
                            showTrialOffer = false
                            scope.launch {
                                trialOfferDataStore.resetAfterPurchase()
                            }
                        }
                    )
                }

                // 👇 Aşağıdaki satır hala kalmalı (farklı senaryolar için)
                if (showPaywall) {
                    PaywallBottomSheet(
                        onDismiss = {
                            showPaywall = false
                        },
                        onPurchaseSuccess = {
                            showPaywall = false
                            scope.launch {
                                trialOfferDataStore.resetAfterPurchase()
                            }
                        }
                    )
                }
            }
        }
    }

    /**
     * Initialize AdMob SDK
     */
    private fun initializeAdMob() {
        lifecycleScope.launch {
            try {
                // ✅ Only initialize SDK
                adMobManager.initialize()
                DebugHelper.logSuccess("✅ AdMob SDK initialized (Lazy loading enabled)")

                // ❌ REMOVED: nativeAdLoader.preloadNativeAds()
                // Ads are now loaded lazily when screens are opened

            } catch (e: Exception) {
                DebugHelper.logError("❌ AdMob initialization failed", e)
            }
        }
    }

    /**
     * ✅ CRITICAL FIX: App launch counter'ı sadece free user'lar için artır
     * Premium user için counter artmıyor, böylece rewarded ad gösterilmiyor
     */
    private fun incrementAppLaunchCountIfFree() {
        lifecycleScope.launch {
            try {
                // Premium kontrolü yap
                val isPremium = adMobManager.isPremiumUser()

                if (!isPremium) {
                    // Free user - counter'ı artır
                    adMobManager.incrementAppLaunchCount()
                    DebugHelper.log("📊 App launch counter incremented (Free user)")
                } else {
                    // Premium user - counter artırılmıyor
                    DebugHelper.log("👑 Premium user - App launch counter skipped")
                }

            } catch (e: Exception) {
                DebugHelper.logError("❌ Failed to increment app launch count", e)
            }
        }
    }

    /**
     * ============================================
     * LIFECYCLE CLEANUP
     * ============================================
     */

    override fun onDestroy() {
        super.onDestroy()

        // Clean up ads when app is destroyed
        try {
            adMobManager.clearAllAds()
            nativeAdLoader.destroyAllAds()
        } catch (e: Exception) {
            println("❌ Failed to clean up ads: ${e.message}")
        }
    }
}