package com.hocalingo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hocalingo.app.core.ui.navigation.HocaBottomNavigationBar
import com.hocalingo.app.core.ui.navigation.shouldShowBottomNavigation
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity - FIXED VERSION
 * ✅ Removed innerPadding usage - no more invisible top app bar space
 * ✅ Clean, edge-to-edge navigation
 * ✅ Bottom navigation only when needed
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HocaLingoTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // ✅ FIXED: No top app bar padding, clean design
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8FAFA)),
                    containerColor = Color(0xFFF8FAFA),
                    bottomBar = {
                        // Show bottom navigation only for main app screens
                        if (shouldShowBottomNavigation(currentRoute)) {
                            HocaBottomNavigationBar(navController = navController)
                        }
                    }
                ) { _ ->

                    // ✅ Smart padding - only bottom when navigation is visible
                    HocaLingoNavigation(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF8FAFA))
                            // Bottom padding only when bottom nav is shown
                            .padding(
                                bottom = if (shouldShowBottomNavigation(currentRoute)) 50.dp else 0.dp
                            )
                    )
                }
            }
        }
    }
}