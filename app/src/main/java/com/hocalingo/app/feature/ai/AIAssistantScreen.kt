package com.hocalingo.app.feature.ai

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
 * AIAssistantScreen - Redesigned Version
 *
 * New Design Features:
 * ✅ Dark gradient background (#1A1625 → #211A2E)
 * ✅ Hero section with Lingo AI character
 * ✅ Progress bar (quota visualization)
 * ✅ Modern pill-style buttons
 * ✅ Premium section for free users
 * ✅ Premium badge for premium users
 * ✅ Lottie generation animation
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
                    colors = listOf(
                        Color(0xFF1A1625),
                        Color(0xFF211A2E)
                    )
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "AI Hikaye Asistanı",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Geri",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                // Hero section with Lingo AI character
                item {
                    HeroSection(
                        isPremium = uiState.isPremium,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quota progress bar
                item {
                    QuotaProgressCard(
                        quotaUsed = uiState.quotaUsed,
                        quotaTotal = uiState.quotaTotal,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Create story button
                item {
                    CreateStoryButton(
                        onClick = { viewModel.onEvent(AIEvent.OpenCreatorDialog) },
                        enabled = uiState.hasQuotaRemaining,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Story history button
                if (uiState.stories.isNotEmpty()) {
                    item {
                        HistoryButton(
                            storyCount = uiState.stories.size,
                            onClick = { viewModel.onEvent(AIEvent.ShowHistory) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Premium section (free users) or Premium badge (premium users)
                item {
                    if (!uiState.isPremium) {
                        PremiumUpgradeSection(
                            onUpgradeClick = { showPaywall = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        PremiumUserBadge(
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showPaywall && !uiState.isPremium) {
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
            isDarkTheme = isDarkTheme  // ✅ EKLE
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

/**
 * Hero Section - Lingo AI Character with title
 */
@Composable
private fun HeroSection(
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(320.dp)
            .clip(RoundedCornerShape(24.dp))
    ) {
        Image(
            painter = painterResource(R.drawable.lingo_ai_image),
            contentDescription = "Lingo AI Asistan",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),
                            Color.Black.copy(alpha = 0.6f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                text = "Hocalingo yapay zeka asistanı ile çalıştığın kelimelerden hikayeler yarat.",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Thin,
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Quota Progress Card - Shows remaining stories
 */
@Composable
private fun QuotaProgressCard(
    quotaUsed: Int,
    quotaTotal: Int,
    modifier: Modifier = Modifier
) {
    val quotaRemaining = quotaTotal - quotaUsed
    val progress = quotaUsed.toFloat() / quotaTotal.toFloat()

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF2D1B4E))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Bugün $quotaRemaining/$quotaTotal hikaye hakkın kaldı",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White
        )

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF1A1625))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF7C3AED),
                                Color(0xFF9D5CFF)
                            )
                        )
                    )
            )
        }
    }
}

/**
 * Create Story Button - Primary action
 */
@Composable
private fun CreateStoryButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF7C3AED),
            disabledContainerColor = Color(0xFF2D1B4E)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoFixHigh,
                contentDescription = null,
                tint = Color.White
            )
            Text(
                "Hikaye Oluştur",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

/**
 * History Button - Secondary action
 */
@Composable
private fun HistoryButton(
    storyCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF2D1B4E).copy(alpha = 0.5f),
            contentColor = Color.White
        ),
        border = null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    "Geçmiş Hikayeler",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            // Story count badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF7C3AED))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = storyCount.toString(),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Premium Upgrade Section - For free users
 */
@Composable
private fun PremiumUpgradeSection(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF3D2463),
                        Color(0xFF2D1B4E)
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Premium badge
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(24.dp)
            )
            Text(
                "Premium'a Geç",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
        }

        // Benefits list
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PremiumBenefitItem("Sınırsız hikaye oluşturma")
            PremiumBenefitItem("Gelişmiş stil seçenekleri")
            PremiumBenefitItem("Reklamsız deneyim")
        }

        // Upgrade button
        Button(
            onClick = onUpgradeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7C3AED)
            )
        ) {
            Text(
                "Şimdi Yükselt",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Premium benefit item
 */
@Composable
private fun PremiumBenefitItem(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF7C3AED),
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = text,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

/**
 * Premium User Badge - For premium users
 */
@Composable
private fun PremiumUserBadge(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF7C3AED),
                        Color(0xFF9D5CFF)
                    )
                )
            )
            .padding(vertical = 20.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Stars,
                contentDescription = null,
                tint = Color(0xFFFFD700),
                modifier = Modifier.size(28.dp)
            )
            Text(
                "Premium Üyesin! 🎉",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
            )
        }
    }
}