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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.hocalingo.app.feature.auth.presentation.AuthScreen
import com.hocalingo.app.feature.home.presentation.HomeScreen
import com.hocalingo.app.feature.onboarding.presentation.PackageSelectionScreen
import com.hocalingo.app.feature.selection.presentation.WordSelectionScreen
import com.hocalingo.app.feature.splash.SplashScreen
import com.hocalingo.app.feature.study.presentation.StudyScreen

/**
 * Enhanced HocaLingo Navigation Routes
 * Updated with bottom navigation support and new routes
 */
object HocaRoutes {
    // Onboarding Flow
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val ONBOARDING_LANGUAGE = "onboarding_language"
    const val ONBOARDING_LEVEL = "onboarding_level"
    const val WORD_SELECTION = "word_selection"

    // Main App Flow (with bottom navigation)
    const val HOME = "home"
    const val STUDY = "study"
    const val ADD_WORD = "add_word"
    const val AI_ASSISTANT = "ai_assistant"
    const val PROFILE = "profile"

    // Settings and other screens
    const val SETTINGS = "settings"
}

/**
 * Main Navigation Composable with Bottom Navigation Support
 * Enhanced with proper route management and parameter handling
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
        // ===== ONBOARDING FLOW =====

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
                    navController.navigate(HocaRoutes.HOME) {
                        popUpTo(HocaRoutes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // Authentication Screen
        composable(route = HocaRoutes.AUTH) {
            AuthScreen(
                onNavigateToOnboarding = {
                    navController.navigate(HocaRoutes.ONBOARDING_LEVEL) {
                        popUpTo(HocaRoutes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        // Language Selection (Future Feature)
        composable(route = HocaRoutes.ONBOARDING_LANGUAGE) {
            PlaceholderScreen(
                title = "🌍 ${stringResource(R.string.settings_title)}",
                subtitle = "Ana dilinizi ve öğrenmek istediğiniz dili seçin\nTürkçe → İngilizce",
                buttonText = stringResource(R.string.next),
                onNavigate = {
                    navController.navigate(HocaRoutes.ONBOARDING_LEVEL)
                }
            )
        }

        // Package/Level Selection - REAL IMPLEMENTATION
        composable(route = HocaRoutes.ONBOARDING_LEVEL) {
            PackageSelectionScreen(
                onNavigateToWordSelection = { packageId ->
                    navController.navigate("${HocaRoutes.WORD_SELECTION}/$packageId")
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Word Selection Screen - REAL IMPLEMENTATION with FIXED PARAMETER
        composable(
            route = "${HocaRoutes.WORD_SELECTION}/{packageId}",
            arguments = listOf(
                navArgument("packageId") {
                    type = NavType.StringType
                    defaultValue = "a1_en_tr_test_v1"
                }
            )
        ) { backStackEntry ->
            WordSelectionScreen(
                onNavigateToStudy = {
                    navController.navigate(HocaRoutes.HOME) {
                        popUpTo("${HocaRoutes.WORD_SELECTION}/{packageId}") { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(HocaRoutes.HOME) {
                        popUpTo("${HocaRoutes.WORD_SELECTION}/{packageId}") { inclusive = true }
                    }
                }
            )
        }

        // ===== MAIN APP FLOW (with Bottom Navigation) =====

        // Home Screen - REAL IMPLEMENTATION ✅
        composable(route = HocaRoutes.HOME) {
            HomeScreen(
                onNavigateToStudy = {
                    navController.navigate(HocaRoutes.STUDY)
                },
                onNavigateToAddWord = {
                    navController.navigate(HocaRoutes.ADD_WORD)
                },
                onNavigateToProfile = {
                    navController.navigate(HocaRoutes.PROFILE)
                }
            )
        }

        // Study Screen - Active Learning Session - REAL IMPLEMENTATION
        composable(route = HocaRoutes.STUDY) {
            StudyScreen(
                onNavigateBack = {
                    navController.navigate(HocaRoutes.HOME) {
                        popUpTo(HocaRoutes.HOME) { inclusive = false }
                    }
                },
                onNavigateToWordSelection = {
                    navController.navigate(HocaRoutes.ONBOARDING_LEVEL) {
                        // Keep study in back stack for return
                        popUpTo(HocaRoutes.STUDY) { inclusive = false }
                    }
                }
            )
        }

        // Add Word Screen - Custom Word Entry
        composable(route = HocaRoutes.ADD_WORD) {
            AddWordScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // AI Assistant Screen - Premium Feature
        composable(route = HocaRoutes.AI_ASSISTANT) {
            AIAssistantScreen()
        }

        // Profile Screen - Statistics and User Info
        composable(route = HocaRoutes.PROFILE) {
            ProfileScreen(
                onNavigateToSettings = {
                    navController.navigate(HocaRoutes.SETTINGS)
                }
            )
        }

        // Settings Screen
        composable(route = HocaRoutes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

// ===== PLACEHOLDER SCREENS =====
// These will be replaced with actual implementations

@Composable
private fun AddWordScreen(
    onNavigateBack: () -> Unit
) {
    PlaceholderScreen(
        title = "➕ ${stringResource(R.string.nav_add_word)}",
        subtitle = "Kendi kelimelerinizi ekleyin\nİngilizce - Türkçe - Örnek cümle - Kategori",
        buttonText = stringResource(R.string.close),
        onNavigate = onNavigateBack
    )
}

@Composable
private fun AIAssistantScreen() {
    PlaceholderScreen(
        title = "🤖 ${stringResource(R.string.nav_ai_assistant)}",
        subtitle = "Öğrendiğiniz kelimelerle hikayeler oluşturun\nPremium özellik - OpenAI entegrasyonu",
        buttonText = stringResource(R.string.premium_upgrade)
    ) {
        // Premium upgrade action
    }
}

@Composable
private fun ProfileScreen(
    onNavigateToSettings: () -> Unit
) {
    PlaceholderScreen(
        title = "👤 ${stringResource(R.string.nav_profile)}",
        subtitle = "İstatistikleriniz ve ilerlemeniz\nÖğrenilen kelimeler, streak, başarı oranı, haftalık/aylık grafikler",
        buttonText = stringResource(R.string.settings_title),
        onNavigate = onNavigateToSettings
    )
}

@Composable
private fun SettingsScreen(
    onNavigateBack: () -> Unit
) {
    PlaceholderScreen(
        title = "⚙️ ${stringResource(R.string.settings_title)}",
        subtitle = "Uygulama tercihleri ve seçenekleri\nBildirimler, sesler, tema, hesap yönetimi, dil değiştirme",
        buttonText = stringResource(R.string.close),
        onNavigate = onNavigateBack
    )
}

/**
 * Enhanced placeholder screen with better design
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String = "",
    buttonText: String = stringResource(R.string.next),
    onNavigate: () -> Unit = {}
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