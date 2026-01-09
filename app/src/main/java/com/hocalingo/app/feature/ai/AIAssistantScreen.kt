package com.hocalingo.app.feature.ai

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import com.hocalingo.app.feature.ai.ui.GeneratingAnimationDialog
import com.hocalingo.app.feature.ai.ui.StoryCreatorDialog
import com.hocalingo.app.feature.ai.ui.StoryHistorySheet
import com.hocalingo.app.feature.subscription.PaywallBottomSheet
import kotlinx.coroutines.flow.collectLatest

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * AIAssistantScreen - Yenilenmiş Tasarım
 *
 * Yeni Özellikler:
 * ✅ Hero image (PaywallContentOptimized gibi)
 * ✅ 2 kare box (Kalan Hak + Geçmiş Hikayeler) - 3D
 * ✅ Hikaye oluştur butonu - 3D yüksek hissiyat
 * ✅ Premium section kaldırıldı
 * ✅ Hak bitince paywall göster
 */
@Composable
fun AIAssistantScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit = {},
    viewModel: AIViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    val snackbarHostState = remember { SnackbarHostState() }
    var showPaywall by remember { mutableStateOf(false) }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is AIEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is AIEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is AIEffect.NavigateToDetail -> {
                    onNavigateToDetail(effect.storyId)
                }
                AIEffect.ShowGeneratingAnimation -> {}
                AIEffect.ShowSuccessAnimation -> {}
            }
        }
    }

    // Main container with gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(Color(0xFF070109), Color(0xFF1E1336))
                    } else {
                        listOf(Color(0xFFF5F5F5), Color(0xFFE8E8E8))
                    }
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                // 1. HERO IMAGE (Ekrana tam oturmuş)
                item {
                    HeroImageSection(
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Spacer ekle - image ile box'lar arası
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 2. INFO BOXES - Yan yana (Kalan Hak + Geçmiş Hikayeler)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sol: Kalan Hak
                        InfoBox3D(
                            title = "Bugünkü Hakların",
                            value = "${uiState.quotaRemaining}",
                            subtitle = "Kalan hikaye",
                            icon = Icons.Default.AutoAwesome,
                            color = Color(0xFF7C3AED),
                            modifier = Modifier.weight(1f),
                            isDarkTheme = isDarkTheme
                        )

                        // Sağ: Geçmiş Hikayeler
                        InfoBox3D(
                            title = "Geçmiş Hikayeler",
                            value = "${uiState.stories.size}",
                            subtitle = "Toplam hikaye",
                            icon = Icons.Default.History,
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(1f),
                            isDarkTheme = isDarkTheme,
                            onClick = {
                                if (uiState.stories.isNotEmpty()) {
                                    viewModel.onEvent(AIEvent.ShowHistory)
                                }
                            }
                        )
                    }
                }

                // 3. HİKAYE OLUŞTUR BUTTON - 3D yüksek hissiyat
                item {
                    CreateStoryButton3D(
                        onClick = {
                            if (uiState.hasQuotaRemaining) {
                                viewModel.onEvent(AIEvent.OpenCreatorDialog)
                            } else {
                                // Hak bitti, paywall göster
                                showPaywall = true
                            }
                        },
                        enabled = true, // Her zaman aktif - hak bitince paywall göster
                        hasQuota = uiState.hasQuotaRemaining,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    )
                }

                // 4. AÇIKLAMA METNİ
                item {
                    Text(
                        text = "Yapay zeka ile çalıştığın kelimelerden bağlamsal okuma parçaları yarat. Öğrenmeyi hızlandır!",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isDarkTheme) {
                            Color.White.copy(alpha = 0.7f)
                        } else {
                            Color(0xFF718096)
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                }
            }
        }
    }

    // Dialogs
    if (showPaywall) {
        PaywallBottomSheet(
            onDismiss = { showPaywall = false },
            onPurchaseSuccess = {
                showPaywall = false
            }
        )
    }

    if (uiState.showCreatorDialog) {
        StoryCreatorDialog(
            isPremium = uiState.isPremium,
            quotaRemaining = uiState.quotaRemaining,
            isGenerating = uiState.isGenerating,
            generationError = uiState.generationError,
            onDismiss = { viewModel.onEvent(AIEvent.CloseCreatorDialog) },
            onGenerate = { topic, type, difficulty, length ->
                viewModel.onEvent(AIEvent.GenerateStory(topic, type, difficulty, length))
            },
            onShowPremiumPaywall = { showPaywall = true },
            isDarkTheme = isDarkTheme
        )
    }

    if (uiState.showHistorySheet) {
        StoryHistorySheet(
            stories = uiState.stories,
            isLoading = uiState.isLoadingHistory,
            onStoryClick = { storyId ->
                viewModel.onEvent(AIEvent.CloseHistory)
                viewModel.onEvent(AIEvent.OpenStoryDetail(storyId))
            },
            onDeleteStory = { storyId ->
                viewModel.onEvent(AIEvent.DeleteStory(storyId))
            },
            onDismiss = { viewModel.onEvent(AIEvent.CloseHistory) },
            isDarkTheme = isDarkTheme
        )
    }

    // Generating animation dialog
    if (uiState.isGenerating) {
        GeneratingAnimationDialog()
    }
}

// =====================================================
// HERO IMAGE SECTION
// =====================================================

/**
 * Hero Image - Ekrana tam oturmuş, PaywallContentOptimized stilinde
 */
@Composable
private fun HeroImageSection(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp), // Daha yüksek
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.lingo_ai_image),
            contentDescription = "Lingo AI Asistan",
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

// =====================================================
// INFO BOX 3D - Kalan Hak + Geçmiş Hikayeler
// =====================================================

/**
 * 3D Info Box - StudyMainScreen card'ları gibi
 */
@Composable
private fun InfoBox3D(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    var isPressed by remember { mutableStateOf(false) }

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 8.dp,
        animationSpec = tween(durationMillis = 100),
        label = "elevation"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (isPressed) 4f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "offsetY"
    )

    Box(
        modifier = modifier
            .height(140.dp)
            .offset(y = offsetY.dp)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(20.dp),
                clip = false
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isDarkTheme) {
                    Color(0xFF2D1B4E)
                } else {
                    Color.White
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isPressed = true
                                tryAwaitRelease()
                                isPressed = false
                            },
                            onTap = { onClick() }
                        )
                    }
                } else {
                    Modifier
                }
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Content
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = if (isDarkTheme) {
                        Color.White.copy(alpha = 0.7f)
                    } else {
                        Color(0xFF718096)
                    }
                )

                Text(
                    text = value,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    color = if (isDarkTheme) Color.White else Color(0xFF2D3748)
                )

                Text(
                    text = subtitle,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (isDarkTheme) {
                        Color.White.copy(alpha = 0.5f)
                    } else {
                        Color(0xFFA0AEC0)
                    }
                )
            }
        }
    }
}

// =====================================================
// CREATE STORY BUTTON 3D
// =====================================================

/**
 * 3D Hikaye Oluştur Button - WideActionButton gibi ama daha yüksek
 */
@Composable
private fun CreateStoryButton3D(
    onClick: () -> Unit,
    enabled: Boolean,
    hasQuota: Boolean,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val elevation by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 12.dp,
        animationSpec = tween(durationMillis = 100),
        label = "elevation"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (isPressed) 6f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "offsetY"
    )

    // Renk seçimi
    val gradientColors = if (hasQuota) {
        listOf(
            Color(0xFF9D5CFF), // Top - Parlak mor
            Color(0xFF7C3AED)  // Bottom - Koyu mor
        )
    } else {
        listOf(
            Color(0xFFFF9800), // Top - Turuncu (premium)
            Color(0xFFFF6B00)  // Bottom - Koyu turuncu
        )
    }

    Box(
        modifier = modifier
            .height(72.dp)
            .offset(y = offsetY.dp)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(20.dp),
                clip = false
            )
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(gradientColors)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (hasQuota) {
                    Icons.Default.AutoFixHigh
                } else {
                    Icons.Default.Lock
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = if (hasQuota) "Hikaye Oluştur" else "Premium'a Geç",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                if (!hasQuota) {
                    Text(
                        text = "Günlük limitin doldu",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}