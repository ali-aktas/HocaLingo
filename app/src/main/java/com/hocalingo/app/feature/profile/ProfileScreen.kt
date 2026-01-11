package com.hocalingo.app.feature.profile

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.hocalingo.app.feature.subscription.PaywallBottomSheet
import com.hocalingo.app.feature.subscription.SubscriptionRepository
import kotlinx.coroutines.flow.collectLatest

/**
 * ProfileScreen - User Profile & Settings Screen
 *
 * Package: feature/profile/
 *
 * Features:
 * - Premium Card (upgrade button for free users, badge for premium users)
 * - User statistics display (streak, words studied, mastered)
 * - Settings management (theme, study direction, notifications, daily goal)
 * - Legal & support links (privacy, terms, play store, support)
 * - Theme-aware gradients for light/dark mode
 * - Notification permission handling (Android 13+)
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

    // ✅ Premium state
    // ✅ Premium state
    val subscriptionViewModel: com.hocalingo.app.feature.subscription.SubscriptionViewModel = hiltViewModel()
    val subscriptionState by subscriptionViewModel.uiState.collectAsStateWithLifecycle()
    var showPaywall by remember { mutableStateOf(false) }

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
                    text = "HocaLingo",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // ✅ Premium Card
            item {
                ProfilePremiumCard(
                    isPremium = subscriptionState.currentSubscription.isPremium,
                    productType = subscriptionState.currentSubscription.productType,
                    onClick = {
                        if (!subscriptionState.currentSubscription.isPremium) {
                            showPaywall = true
                        }
                    },
                    isDarkTheme = isDarkTheme
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

        // ✅ Paywall BottomSheet
        if (showPaywall) {
            PaywallBottomSheet(
                onDismiss = { showPaywall = false },
                onPurchaseSuccess = { showPaywall = false }
            )
        }
    }
}

/**
 * ProfilePremiumCard - Premium status card
 * Shows upgrade button for free users
 * Shows badge for premium users
 */
@Composable
private fun ProfilePremiumCard(
    isPremium: Boolean,
    productType: com.hocalingo.app.feature.subscription.ProductType?,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isPremium) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(
                    brush = if (isPremium) {
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF7C3AED),
                                Color(0xFF9D5CFF)
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            colors = if (isDarkTheme) {
                                listOf(
                                    Color(0xFF2F1C52),
                                    Color(0xFF3D2463)
                                )
                            } else {
                                listOf(
                                    Color(0xFF3C246A),
                                    Color(0xFF5D3895)
                                )
                            }
                        )
                    }
                )
                .padding(20.dp)
        ) {
            if (isPremium) {
                // Premium Badge
                PremiumBadgeContent(productType = productType)
            } else {
                // Upgrade Card
                PremiumUpgradeContent(isDarkTheme = isDarkTheme)
            }
        }
    }
}

@Composable
private fun PremiumBadgeContent(
    productType: com.hocalingo.app.feature.subscription.ProductType?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    "Premium Üyesin! 🎉",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )

                // Show subscription type
                productType?.let {
                    Text(
                        it.toReadableString(),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun PremiumUpgradeContent(isDarkTheme: Boolean) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = null,
                tint = if (isDarkTheme) Color.White else Color(0xFF813FF1),
                modifier = Modifier.size(28.dp)
            )
            Text(
                "Premium'a Geç",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isDarkTheme) Color.White else Color(0xFFAA81F3)
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PremiumFeatureItem(
                text = "Tamamen reklamsız kullanım",
                isDarkTheme = isDarkTheme
            )
            PremiumFeatureItem(
                text = "Premium yapay zeka deneyimi",
                isDarkTheme = isDarkTheme
            )
            PremiumFeatureItem(
                text = "Daha fazla kelime seçme seçeneği",
                isDarkTheme = isDarkTheme
            )
        }

        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Şimdi Yükselt →",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = if (isDarkTheme) Color.White else Color(0xC3DCADFF)
            )
        }
    }
}

@Composable
private fun PremiumFeatureItem(
    text: String,
    isDarkTheme: Boolean
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color(0xFF7C3AED),
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = if (isDarkTheme) Color.White.copy(alpha = 0.9f) else Color(0xFF1F2937)
        )
    }
}