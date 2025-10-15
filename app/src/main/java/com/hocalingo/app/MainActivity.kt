package com.hocalingo.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hocalingo.app.core.ui.navigation.HocaBottomNavigationBar
import com.hocalingo.app.core.ui.navigation.shouldShowBottomNavigation
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity - Professional Edge-to-Edge Implementation
 *
 * ✅ API 35 Compatible
 * ✅ Status Bar: Transparent, content extends behind it
 * ✅ Navigation Bar: Proper padding for gesture navigation
 * ✅ Central WindowInsets management for all screens
 *
 * CRITICAL CHANGES:
 * - Added contentWindowInsets = WindowInsets(0, 0, 0, 0) to Scaffold
 * - This allows each screen to manage its own status bar padding
 * - Bottom navigation handles gesture bar padding automatically
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen first
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // ✅ Enable edge-to-edge (mandatory for API 35)
        enableEdgeToEdge()

        setContent {
            // Theme management
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val shouldUseDarkTheme = themeViewModel.shouldUseDarkTheme()

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
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(paddingValues) // Only bottom nav padding
                    )
                }
            }
        }
    }
}