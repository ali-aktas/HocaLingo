package com.hocalingo.app.feature.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hocalingo.app.HocaRoutes
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * ProfileScreen - User Profile & Settings Screen
 *
 * Package: feature/profile/
 *
 * Features:
 * - User statistics display (streak, words studied, mastered)
 * - Settings management (theme, study direction, notifications, daily goal)
 * - Selected words preview with pagination
 * - Legal & support links (privacy, terms, play store, support)
 * - Theme-aware gradients for light/dark mode
 * - Notification permission handling (Android 13+)
 * - BottomSheet for all selected words with lazy loading
 *
 * Architecture: MVVM + MVI
 * - UiState: Immutable state container
 * - Events: User actions
 * - Effects: One-time side effects
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * AI Assistant Style - UI display options
 * Future feature for AI assistant personality customization
 */
enum class AIAssistantStyle(val displayName: String) {
    FRIENDLY("Samimi"),
    MOTIVATIONAL("Motivasyonel"),
    PROFESSIONAL("Profesyonel")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToWordsList: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Notification Permission Launcher (Android 13+)
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.onEvent(ProfileEvent.UpdateNotifications(true))
        } else {
            viewModel.onPermissionDenied()
        }
    }

    // Theme State
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    // AI Style Local State (temporary - will be connected to ViewModel)
    var currentAIStyle by remember { mutableStateOf(AIAssistantStyle.FRIENDLY) }

    // Effect Handler - Centralized side effect management
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                ProfileEffect.NavigateToWordsList -> onNavigateToWordsList()

                is ProfileEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is ProfileEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.error)
                }

                // BottomSheet Effects (artık kullanılmıyor ama exhaustive check için gerekli)
                ProfileEffect.ShowWordsBottomSheet -> {
                    // No-op: BottomSheet feature StudyMainScreen'e taşındı
                }

                ProfileEffect.HideWordsBottomSheet -> {
                    // No-op: BottomSheet feature StudyMainScreen'e taşındı
                }

                is ProfileEffect.ShowWordsLoadError -> {
                    // No-op: BottomSheet feature StudyMainScreen'e taşındı
                }

                // Notification Permission Handling
                ProfileEffect.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onEvent(ProfileEvent.UpdateNotifications(true))
                    }
                }

                ProfileEffect.ShowNotificationPermissionDialog -> {
                    snackbarHostState.showSnackbar(
                        "Bildirim izni gerekli. Lütfen ayarlardan bildirim iznini açın."
                    )
                }

                is ProfileEffect.ShowNotificationScheduled -> {
                    snackbarHostState.showSnackbar(
                        "Bildirimler açıldı! Bir sonraki bildirim: ${effect.time}"
                    )
                }

                // Legal & Support Effects
                is ProfileEffect.OpenUrl -> {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(effect.url))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Link açılamadı")
                    }
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            HocaSnackbarHost(
                hostState = snackbarHostState,
                currentRoute = HocaRoutes.PROFILE
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { _ ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 14.dp,
                end = 14.dp,
                top = 54.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Hocalingo",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Settings Card
            item {
                ModernSettingsCard(
                    uiState = uiState,
                    currentAIStyle = currentAIStyle,
                    onEvent = viewModel::onEvent,
                    onAIStyleChange = { newStyle -> currentAIStyle = newStyle },
                    isDarkTheme = isDarkTheme
                )
            }

            // Quick Stats Row
            item {
                ModernStatsRow(
                    uiState = uiState,
                    isDarkTheme = isDarkTheme
                )
            }

            // Legal & Support Card
            item {
                LegalAndSupportCard(
                    onEvent = viewModel::onEvent,
                    isDarkTheme = isDarkTheme
                )
            }

            // Bottom Spacer
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}