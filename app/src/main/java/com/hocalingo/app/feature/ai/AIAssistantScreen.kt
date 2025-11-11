package com.hocalingo.app.feature.ai

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI Asistan",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Geri")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
            item {
                HeroCard(isDarkTheme = isDarkTheme)
            }

            if (!uiState.isPremium) {
                item {
                    PremiumUpgradeCard(
                        onClick = { showPaywall = true },
                        isDarkTheme = isDarkTheme
                    )
                }
            }

            item {
                QuotaCard(
                    quotaText = uiState.quotaText,
                    hasQuota = uiState.hasQuotaRemaining,
                    isPremium = uiState.isPremium,
                    isDarkTheme = isDarkTheme
                )
            }

            item {
                CreateStoryButton(
                    onClick = { viewModel.onEvent(AIEvent.OpenCreatorDialog) },
                    enabled = uiState.hasQuotaRemaining,
                    isDarkTheme = isDarkTheme
                )
            }

            if (uiState.stories.isNotEmpty()) {
                item {
                    HistoryButton(
                        storyCount = uiState.stories.size,
                        onClick = { viewModel.onEvent(AIEvent.ShowHistory) },
                        isDarkTheme = isDarkTheme
                    )
                }
            }

            item {
                FeaturesSection(isDarkTheme = isDarkTheme)
            }
        }
    }

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
}

@Composable
private fun HeroCard(isDarkTheme: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF4A148C), Color(0xFF6A1B9A))
                        } else {
                            listOf(Color(0xFF667eea), Color(0xFF764ba2))
                        }
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(50.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.onboarding_teacher_3),
                        contentDescription = "AI Assistant",
                        modifier = Modifier.size(60.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                Text(
                    text = "AI Hikaye Oluşturucu",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Öğrendiğiniz kelimelerle kişiselleştirilmiş hikayeler oluşturun",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PremiumUpgradeCard(
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF2D2D2D) else Color(0xFFFFF8E1)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = null,
                    tint = if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFFF9800),
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        "Premium'a Geçin",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )
                    Text(
                        "Tüm özelliklere erişin",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 12.sp,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                    )
                }
            }

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) Color(0xFFFFB74D) else Color(0xFFFF9800)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Yükselt",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun QuotaCard(
    quotaText: String,
    hasQuota: Boolean,
    isPremium: Boolean,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF2D2D2D) else Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (hasQuota) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (hasQuota) Color(0xFF4CAF50) else Color(0xFFFF5252),
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        "Günlük Hikaye Hakkı",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
                    )
                    Text(
                        quotaText,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = if (hasQuota) Color(0xFF4CAF50) else Color(0xFFFF5252)
                    )
                }
            }

            if (isPremium) {
                Surface(
                    color = Color(0xFFFFD700).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Stars,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            "Premium",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateStoryButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isDarkTheme: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDarkTheme) Color(0xFF7986CB) else Color(0xFF667eea),
            disabledContainerColor = if (isDarkTheme) Color(0xFF404040) else Color(0xFFE0E0E0)
        )
    ) {
        Icon(Icons.Default.AutoStories, "Hikaye Oluştur")
        Spacer(Modifier.width(8.dp))
        Text(
            "Yeni Hikaye Oluştur",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun HistoryButton(
    storyCount: Int,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = if (isDarkTheme) Color.White else Color.Black
        )
    ) {
        Icon(Icons.Default.History, "Geçmiş")
        Spacer(Modifier.width(8.dp))
        Text(
            "Hikaye Geçmişi ($storyCount)",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun FeaturesSection(isDarkTheme: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Özellikler",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = if (isDarkTheme) Color.White else Color.Black
        )

        FeatureItem(
            icon = Icons.Default.AutoStories,
            title = "Kişiselleştirilmiş İçerik",
            description = "Öğrendiğiniz kelimelerle otomatik hikaye",
            isDarkTheme = isDarkTheme
        )

        FeatureItem(
            icon = Icons.Default.TrendingUp,
            title = "Zorluk Seviyeleri",
            description = "Kolay, orta ve zor seviyeler",
            isDarkTheme = isDarkTheme
        )

        FeatureItem(
            icon = Icons.Default.Category,
            title = "Çeşitli Türler",
            description = "Hikaye, motivasyon, diyalog ve makale",
            isDarkTheme = isDarkTheme
        )
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) Color(0xFF2D2D2D) else Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDarkTheme) Color(0xFF7986CB) else Color(0xFF667eea),
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isDarkTheme) Color.White else Color.Black
                )
                Text(
                    description,
                    fontFamily = PoppinsFontFamily,
                    fontSize = 12.sp,
                    color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
                )
            }
        }
    }
}