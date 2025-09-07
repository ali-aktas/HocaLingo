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

        // Authentication
        composable(route = HocaRoutes.AUTH) {
            AuthScreen(
                onNavigateToOnboarding = {
                    navController.navigate(HocaRoutes.ONBOARDING_LANGUAGE) {
                        popUpTo(HocaRoutes.AUTH) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(HocaRoutes.STUDY) {
                        popUpTo(HocaRoutes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        // Onboarding Flow
        composable(route = HocaRoutes.ONBOARDING_LANGUAGE) {
            PlaceholderScreen(
                title = "🌍 Language Selection",
                subtitle = "Choose your native and target language",
                buttonText = "Select Level",
                onNavigate = {
                    navController.navigate(HocaRoutes.ONBOARDING_LEVEL)
                }
            )
        }

        composable(route = HocaRoutes.ONBOARDING_LEVEL) {
            PlaceholderScreen(
                title = "📊 Level Selection",
                subtitle = "A1, A2, B1, B2, C1, C2",
                buttonText = "Download Package",
                onNavigate = {
                    navController.navigate(HocaRoutes.ONBOARDING_DOWNLOAD)
                }
            )
        }

        composable(route = HocaRoutes.ONBOARDING_DOWNLOAD) {
            PlaceholderScreen(
                title = "📥 Package Download",
                subtitle = "Downloading B1 English words...",
                buttonText = "Select Words",
                onNavigate = {
                    navController.navigate(HocaRoutes.WORD_SELECTION)
                }
            )
        }

        composable(route = HocaRoutes.WORD_SELECTION) {
            PlaceholderScreen(
                title = "✨ Word Selection",
                subtitle = "Swipe to choose words you want to learn",
                buttonText = "Start Learning",
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
                title = "🎯 Study Screen",
                subtitle = "Learn words with spaced repetition",
                buttonText = "View Profile",
                onNavigate = {
                    navController.navigate(HocaRoutes.PROFILE)
                }
            )
        }

        composable(route = HocaRoutes.PROFILE) {
            PlaceholderScreen(
                title = "👤 Profile Screen",
                subtitle = "Your stats and progress",
                buttonText = "Settings",
                onNavigate = {
                    navController.navigate(HocaRoutes.SETTINGS)
                }
            )
        }

        composable(route = HocaRoutes.SETTINGS) {
            PlaceholderScreen(
                title = "⚙️ Settings Screen",
                subtitle = "App preferences and options",
                buttonText = "Back",
                onNavigate = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = HocaRoutes.ADD_WORD) {
            PlaceholderScreen(
                title = "➕ Add Word Screen",
                subtitle = "Add your own words to learn",
                buttonText = "Back",
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
    buttonText: String = "Next Screen",
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
            text = "🚧 Placeholder Screen - Will be implemented",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}