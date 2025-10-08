package com.hocalingo.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
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
 * MainActivity - Enhanced with Theme Management
 * ✅ Bottom navigation fixed to bottom
 * ✅ No floating padding
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Theme management with ViewModel
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val shouldUseDarkTheme = themeViewModel.shouldUseDarkTheme()

            // Apply theme with centralized state management
            HocaLingoTheme(
                darkTheme = shouldUseDarkTheme,
                dynamicColor = true // Enable dynamic colors on Android 12+
            ) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // 🎨 Modern Status Bar Setup
                val view = LocalView.current
                val window = (view.context as Activity).window

                SideEffect {
                    // Make status bar transparent
                    window.statusBarColor = Color.Transparent.toArgb()

                    // Adjust status bar content color based on theme
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !shouldUseDarkTheme

                    // Enable edge-to-edge
                    WindowCompat.setDecorFitsSystemWindows(window, false)
                }

                // Clean design with proper theming
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        // UPDATED: Show bottom navigation directly at bottom
                        if (shouldShowBottomNavigation(currentRoute)) {
                            HocaBottomNavigationBar(navController = navController)
                        }
                    }
                ) { paddingValues ->

                    // UPDATED: Apply padding from Scaffold (includes bottom nav)
                    HocaLingoNavigation(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(paddingValues) // Use Scaffold padding
                    )
                }
            }
        }
    }
}