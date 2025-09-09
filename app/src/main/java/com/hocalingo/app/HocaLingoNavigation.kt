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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hocalingo.app.feature.auth.presentation.AuthScreen
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
                    navController.navigate(HocaRoutes.ONBOARDING_LANGUAGE) {
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

        // Authentication - Mevcut AuthScreen parametreleriyle uyumlu
        composable(route = HocaRoutes.AUTH) {
            AuthScreen(
                onNavigateToOnboarding = {
                    navController.navigate(HocaRoutes.ONBOARDING_LANGUAGE) {
                        popUpTo(HocaRoutes.AUTH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    // Direkt study'ye git - çünkü AuthViewModel'de NavigateToWordSelection henüz implement değil
                    navController.navigate(HocaRoutes.STUDY) {
                        popUpTo(HocaRoutes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding Flow
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

        composable(route = HocaRoutes.ONBOARDING_LEVEL) {
            PlaceholderScreen(
                title = "📊 Seviye Seçimi",
                subtitle = "A1 (Başlangıç) - A2 (Temel) - B1 (Orta)\nB2 (Orta-İleri) - C1 (İleri) - C2 (Uzman)",
                buttonText = "Paket İndir",
                onNavigate = {
                    navController.navigate(HocaRoutes.ONBOARDING_DOWNLOAD)
                }
            )
        }

        composable(route = HocaRoutes.ONBOARDING_DOWNLOAD) {
            PlaceholderScreen(
                title = "📥 Paket İndiriliyor",
                subtitle = "A1 İngilizce kelime paketi indiriliyor...\n50 kelime yükleniyor",
                buttonText = "Kelime Seç",
                onNavigate = {
                    navController.navigate(HocaRoutes.WORD_SELECTION)
                }
            )
        }

        composable(route = HocaRoutes.WORD_SELECTION) {
            PlaceholderScreen(
                title = "✨ Kelime Seçimi",
                subtitle = "Öğrenmek istediğiniz kelimeleri seçin\nSağa kaydır = Öğren, Sola kaydır = Geç",
                buttonText = "Çalışmaya Başla",
                onNavigate = {
                    navController.navigate(HocaRoutes.STUDY) {
                        popUpTo(HocaRoutes.WORD_SELECTION) { inclusive = true }
                    }
                }
            )
        }

        // Main App Screens
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