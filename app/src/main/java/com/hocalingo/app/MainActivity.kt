package com.hocalingo.app

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
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
 * MainActivity - True Edge-to-Edge Experience
 * ✅ API 35 uyumlu tam ekran tasarım
 * ✅ Status bar transparan ve içerik arkasından başlıyor
 * ✅ Bottom navigation doğru konumda
 * ✅ Tüm ekranlar için merkezi yönetim
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // ✅ API 35 için edge-to-edge zorunlu, eski API'ler için de aktif ediyoruz
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

                // 🎨 Modern Status Bar - Transparan ve temaya uygun ikonlar
                val view = LocalView.current
                val window = (view.context as Activity).window

                SideEffect {
                    // Status bar tamamen transparan
                    window.statusBarColor = Color.Transparent.toArgb()

                    // Navigation bar da transparan (gesture nav için)
                    window.navigationBarColor = Color.Transparent.toArgb()

                    // Status bar icon renklerini temaya göre ayarla
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !shouldUseDarkTheme
                        isAppearanceLightNavigationBars = !shouldUseDarkTheme
                    }
                }

                // ✅ Edge-to-Edge Scaffold - İçerik tam ekran
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        // Bottom navigation sadece gerekli ekranlarda göster
                        if (shouldShowBottomNavigation(currentRoute)) {
                            HocaBottomNavigationBar(navController = navController)
                        }
                    }
                ) { paddingValues ->
                    // 🚀 İçerik buradan başlıyor - STATUS BAR'IN ARKASINDA!
                    // windowInsetsPadding KALDIRILDI - artık edge-to-edge!
                    HocaLingoNavigation(
                        navController = navController,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(paddingValues) // Sadece Scaffold padding'i (bottom nav için)
                    )
                }
            }
        }
    }
}