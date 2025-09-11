package com.hocalingo.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hocalingo.app.feature.auth.presentation.AuthScreen
import com.hocalingo.app.feature.onboarding.presentation.PackageSelectionScreen
import com.hocalingo.app.feature.selection.presentation.WordSelectionScreen
import com.hocalingo.app.feature.splash.SplashScreen

/**
 * HocaLingo Navigation Routes
 * Centralized navigation management for the app
 */
object HocaRoutes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val ONBOARDING_LANGUAGE = "onboarding_language"
    const val ONBOARDING_LEVEL = "onboarding_level"
    const val ONBOARDING_DOWNLOAD = "onboarding_download"
    const val WORD_SELECTION = "word_selection"
    const val STUDY = "study"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val ADD_WORD = "add_word"
}

/**
 * Main Navigation Composable
 * Handles all app navigation flows
 * DÜZELTME: Package ID parametresi eklendi
 */
@Composable
fun HocaLingoNavigation(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HocaRoutes.SPLASH,
        modifier = modifier
    ) {
        // Splash Screen
        composable(route = HocaRoutes.SPLASH) {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate(HocaRoutes.AUTH) {
                        popUpTo(HocaRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToOnboarding = {
                    navController.navigate(HocaRoutes.ONBOARDING_LEVEL) {
                        popUpTo(HocaRoutes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(HocaRoutes.STUDY) {
                        popUpTo(HocaRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // Authentication Screen
        // BASİT: Sadece onboarding'e navigation var
        composable(route = HocaRoutes.AUTH) {
            AuthScreen(
                onNavigateToOnboarding = {
                    navController.navigate(HocaRoutes.ONBOARDING_LEVEL) {
                        popUpTo(HocaRoutes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding - Language Selection (Placeholder for now)
        composable(route = HocaRoutes.ONBOARDING_LANGUAGE) {
            PlaceholderScreen(
                title = "🌍 Dil Seçimi",
                subtitle = "Ana dilinizi ve öğrenmek istediğiniz dili seçin\nTürkçe → İngilizce",
                buttonText = "Seviye Seç",
                onNavigate = {
                    navController.navigate(HocaRoutes.ONBOARDING_LEVEL)
                }
            )
        }

        // Onboarding - Package/Level Selection (REAL SCREEN)
        // DÜZELTME: Package ID route parametresi ile geçiriliyor
        composable(route = HocaRoutes.ONBOARDING_LEVEL) {
            PackageSelectionScreen(
                onNavigateToWordSelection = { packageId ->
                    // PackageId'yi route parametresi olarak geçiyoruz
                    navController.navigate("${HocaRoutes.WORD_SELECTION}/$packageId")
                }
            )
        }

        // Word Selection Screen (REAL SCREEN)
        // DÜZELTME: Package ID parametresi eklendi
        composable(
            route = "${HocaRoutes.WORD_SELECTION}/{packageId}",
            arguments = listOf(
                navArgument("packageId") {
                    type = NavType.StringType
                    defaultValue = "a1_en_tr_test_v1"
                }
            )
        ) { backStackEntry ->
            // Package ID'yi navigation argument'ten al
            val packageId = backStackEntry.arguments?.getString("packageId") ?: "a1_en_tr_test_v1"

            WordSelectionScreen(
                onNavigateToStudy = {
                    navController.navigate(HocaRoutes.STUDY) {
                        popUpTo("${HocaRoutes.WORD_SELECTION}/{packageId}") { inclusive = true }
                    }
                }
            )
        }

        // Study Screen (Placeholder)
        composable(route = HocaRoutes.STUDY) {
            PlaceholderScreen(
                title = "🎯 Çalışma Ekranı",
                subtitle = "Akıllı tekrar sistemi ile kelime öğrenin\nSM-2 algoritması",
                buttonText = "Profil",
                onNavigate = {
                    navController.navigate(HocaRoutes.PROFILE)
                }
            )
        }

        // Profile Screen (Placeholder)
        composable(route = HocaRoutes.PROFILE) {
            PlaceholderScreen(
                title = "👤 Profil",
                subtitle = "İstatistikleriniz ve ilerlemeniz\nÖğrenilen kelimeler, streak, başarı oranı",
                buttonText = "Ayarlar",
                onNavigate = {
                    navController.navigate(HocaRoutes.SETTINGS)
                }
            )
        }

        // Settings Screen (Placeholder)
        composable(route = HocaRoutes.SETTINGS) {
            PlaceholderScreen(
                title = "⚙️ Ayarlar",
                subtitle = "Uygulama tercihleri ve seçenekleri\nBildirimler, sesler, tema",
                buttonText = "Geri",
                onNavigate = {
                    navController.popBackStack()
                }
            )
        }

        // Add Word Screen (Placeholder)
        composable(route = HocaRoutes.ADD_WORD) {
            PlaceholderScreen(
                title = "➕ Kelime Ekle",
                subtitle = "Kendi kelimelerinizi ekleyin\nİngilizce - Türkçe - Örnek cümle",
                buttonText = "Geri",
                onNavigate = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Temporary placeholder screen for development
 * Will be replaced with actual screens
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String = "",
    buttonText: String = "Sonraki Ekran",
    onNavigate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigate,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Text(buttonText)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "🚧 Geçici Ekran - Yakında implement edilecek",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}