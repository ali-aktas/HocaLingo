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
import com.hocalingo.app.feature.auth.AuthScreen
import com.hocalingo.app.feature.home.HomeScreen
import com.hocalingo.app.feature.onboarding.OnboardingIntroScreen
import com.hocalingo.app.feature.onboarding.PackageSelectionScreen
import com.hocalingo.app.feature.selection.WordSelectionScreen
import com.hocalingo.app.feature.splash.SplashScreen
import com.hocalingo.app.feature.study.StudyScreen
import com.hocalingo.app.feature.addword.presentation.AddWordScreen
import com.hocalingo.app.feature.ai.AIAssistantScreen
import com.hocalingo.app.feature.profile.ProfileScreen
import com.hocalingo.app.feature.ai.ui.StoryDetailScreen
import com.hocalingo.app.feature.ai.AIViewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import com.hocalingo.app.feature.ai.AIEvent

/**
 * Enhanced HocaLingo Navigation Routes
 * ✅ Onboarding Intro (3 pages) added
 */
object HocaRoutes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val ONBOARDING_INTRO = "onboarding_intro"
    const val ONBOARDING_LANGUAGE = "onboarding_language"
    const val ONBOARDING_LEVEL = "onboarding_level"
    const val PACKAGE_SELECTION = "package_selection"
    const val WORD_SELECTION = "word_selection"
    const val HOME = "home"
    const val STUDY = "study"
    const val ADD_WORD = "add_word"
    const val AI_ASSISTANT = "ai_assistant"
    const val PROFILE = "profile"
    const val WORDS_LIST = "words_list"
    const val SETTINGS = "settings"
    const val AI_STORY_DETAIL = "ai_story_detail"
}

/**
 * Main Navigation Composable
 * Updated Flow: SPLASH → AUTH → ONBOARDING_INTRO → PACKAGE_SELECTION → WORD_SELECTION → HOME
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
                    navController.navigate(HocaRoutes.ONBOARDING_INTRO) { // ✅ CHANGED: INTRO first
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
                    navController.navigate(HocaRoutes.ONBOARDING_INTRO) { // ✅ CHANGED: INTRO first
                        popUpTo(HocaRoutes.AUTH) { inclusive = true }
                    }
                }
            )
        }

        // ✅ NEW: Onboarding Intro Screen (3 pages)
        composable(route = HocaRoutes.ONBOARDING_INTRO) {
            OnboardingIntroScreen(
                onNavigateToPackageSelection = {
                    navController.navigate(HocaRoutes.ONBOARDING_LEVEL) {
                        popUpTo(HocaRoutes.ONBOARDING_INTRO) { inclusive = true }
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

        // Package/Level Selection - ONBOARDING FLOW
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

        // ✅ NEW: Package Selection - HOME ACCESS
        composable(route = HocaRoutes.PACKAGE_SELECTION) {
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

        // Home Screen - FIXED NAVIGATION ✅
        composable(route = HocaRoutes.HOME) {
            HomeScreen(
                onNavigateToStudy = {
                    navController.navigate(HocaRoutes.STUDY)
                },
                onNavigateToPackageSelection = {
                    navController.navigate(HocaRoutes.PACKAGE_SELECTION)
                },
                onNavigateToAIAssistant = {
                    navController.navigate(HocaRoutes.AI_ASSISTANT)
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
                    navController.navigate(HocaRoutes.PACKAGE_SELECTION)
                }
            )
        }

        // Add Word Screen - REAL IMPLEMENTATION ✅
        composable(route = HocaRoutes.ADD_WORD) {
            AddWordScreen(
                onNavigateBack = {
                    navController.navigate(HocaRoutes.HOME) {
                        popUpTo(HocaRoutes.HOME) { inclusive = false }
                    }
                },
                onNavigateToStudy = {
                    navController.navigate(HocaRoutes.STUDY)
                }
            )
        }

        // ✅ Profile Screen - REAL IMPLEMENTATION
        composable(route = HocaRoutes.PROFILE) {
            ProfileScreen(
                onNavigateToWordsList = {
                    navController.navigate(HocaRoutes.WORDS_LIST)
                }
            )
        }

        // ✅ Words List Screen - Future Implementation (placeholder for now)
        composable(route = HocaRoutes.WORDS_LIST) {
            PlaceholderScreen(
                title = "📚 Tüm Kelimeler",
                subtitle = "Seçili kelimelerinizin tam listesi\n20'şer kelime lazy loading ile yüklenecek",
                buttonText = "Profil'e Dön",
                onNavigate = {
                    navController.popBackStack()
                }
            )
        }

        // AI Assistant Screen - Professional Coming Soon
        composable(route = HocaRoutes.AI_ASSISTANT) {
            AIAssistantScreen(
                onNavigateBack = {
                    navController.navigate(HocaRoutes.HOME) {
                        popUpTo(HocaRoutes.HOME) { inclusive = false }
                    }
                }
            )
        }

        // Settings Screen (Placeholder)
        composable(route = HocaRoutes.SETTINGS) {
            PlaceholderScreen(
                title = "⚙️ ${stringResource(R.string.settings_title)}",
                subtitle = "Tema, bildirimler, dil ayarları\nYakında eklenecek",
                buttonText = "Ana Ekran",
                onNavigate = {
                    navController.navigate(HocaRoutes.HOME)
                }
            )
        }

        composable(
            route = "${HocaRoutes.AI_STORY_DETAIL}/{storyId}",
            arguments = listOf(
                navArgument("storyId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val storyId = backStackEntry.arguments?.getString("storyId") ?: ""
            val viewModel: AIViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme = themeViewModel.shouldUseDarkTheme()
            val context = LocalContext.current

            LaunchedEffect(storyId) {
                viewModel.onEvent(AIEvent.OpenStoryDetail(storyId))
            }

            val story = uiState.currentStory

            if (story != null) {
                StoryDetailScreen(
                    story = story,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onShare = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, story.content)
                            putExtra(Intent.EXTRA_TITLE, story.title)
                        }
                        context.startActivity(
                            Intent.createChooser(shareIntent, "Hikayeyi Paylaş")
                        )
                    },
                    onDelete = {
                        viewModel.onEvent(AIEvent.DeleteStory(storyId))
                        navController.popBackStack()
                    },
                    onToggleFavorite = {
                        viewModel.onEvent(AIEvent.ToggleFavorite(storyId))
                    },
                    isDarkTheme = isDarkTheme
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }

    }
}

/**
 * Placeholder Screen for future features
 */
@Composable
private fun PlaceholderScreen(
    title: String,
    subtitle: String,
    buttonText: String,
    onNavigate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onNavigate) {
            Text(buttonText)
        }
    }
}