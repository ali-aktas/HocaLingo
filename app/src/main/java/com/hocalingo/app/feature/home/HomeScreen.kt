package com.hocalingo.app.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.HocaRoutes
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import kotlinx.coroutines.flow.collectLatest

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * Modern Home Dashboard Screen - Theme-Aware Version
 * ✅ Smart theme switching with beautiful dark mode
 * ✅ Maintains gradient beauty in both themes
 * ✅ Cards adapt to light/dark backgrounds
 * ✅ Text colors theme-aware via MaterialTheme
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToStudy: () -> Unit,
    onNavigateToPackageSelection: () -> Unit = {},
    onNavigateToAIAssistant: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Get theme state for smart styling
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                HomeEffect.NavigateToStudy -> onNavigateToStudy()
                HomeEffect.NavigateToPackageSelection -> onNavigateToPackageSelection()
                HomeEffect.NavigateToAIAssistant -> onNavigateToAIAssistant()
                is HomeEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is HomeEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.error)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            HocaSnackbarHost(
                hostState = snackbarHostState,
                currentRoute = HocaRoutes.HOME
            )
        },
        containerColor = MaterialTheme.colorScheme.background // Theme-aware background
    ) { _ -> // paddingValues ignore edildi
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 54.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            // App Title - "Hocalingo"
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

            // Welcome Header + Monthly Stats - UPDATED LARGE CARD
            item {
                WelcomeHeaderWithStats(
                    userName = uiState.userName,
                    streakDays = uiState.streakDays,
                    stats = uiState.monthlyStats,
                    onRefresh = { viewModel.onEvent(HomeEvent.RefreshData) },
                    isDarkTheme = isDarkTheme
                )
            }

            // Daily Goal Card - UPDATED COLORS
            item {
                DailyGoalCard(
                    progress = uiState.dailyGoalProgress,
                    onStartStudy = { viewModel.onEvent(HomeEvent.StartStudy) },
                    isDarkTheme = isDarkTheme
                )
            }

            // Package Selection Card - UPDATED TO TEAL
            item {
                PackageSelectionCard(
                    onNavigateToPackageSelection = { viewModel.onEvent(HomeEvent.NavigateToPackageSelection) },
                    isDarkTheme = isDarkTheme
                )
            }

            // AI Assistant Card
            item {
                AIAssistantCard(
                    onNavigateToAI = { viewModel.onEvent(HomeEvent.NavigateToAIAssistant) },
                    isDarkTheme = isDarkTheme
                )
            }

            // Bottom spacing for BottomNavigationBar
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

/**
 * UPDATED Welcome Header + Monthly Stats Combined
 * ✅ Large card with greeting + monthly stats
 * ✅ Left bottom: Merhaba Ali! + Streak + Stats
 * ✅ Right: Lingo Hoca image aligned to bottom
 * ✅ Green gradient + shadow
 */
@Composable
private fun WelcomeHeaderWithStats(
    userName: String,
    streakDays: Int,
    stats: MonthlyStats,
    onRefresh: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp) // Larger card
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.25f)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF388E3C), Color(0xFF2E7D32)) // Dark green
                        } else {
                            listOf(Color(0xFF66BB6A), Color(0xFF81C784)) // Light green
                        }
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            // Left side - Text content at bottom
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(20.dp)
                    .fillMaxWidth(0.6f) // Leave space for image
            ) {
                Text(
                    text = "Merhaba $userName!",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔥",
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$streakDays günlük seri!",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Monthly Stats - Small version
                Text(
                    text = "Bu Ay 📈",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.95f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompactStatItem(
                        label = "Aktif",
                        value = "${stats.activeDaysThisMonth} gün"
                    )
                    CompactStatItem(
                        label = "Süre",
                        value = stats.studyTimeFormatted
                    )
                    CompactStatItem(
                        label = "Disiplin",
                        value = "${stats.disciplineScore}%"
                    )
                }
            }

            // Right side - Lingo Hoca Image aligned to bottom
            Image(
                painter = painterResource(id = R.drawable.main_screen_card),
                contentDescription = "Lingo Hoca",
                modifier = Modifier
                    .size(170.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 15.dp, y = 0.dp), // Slight offset to pop out
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter
            )
        }
    }
}

/**
 * Compact stat item for the welcome card
 */
@Composable
private fun CompactStatItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = value,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color.White
        )
        Text(
            text = label,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

/**
 * UPDATED Daily Goal Card - Orange/Yellow Gradient
 * ✅ Light: Warm orange gradient
 * ✅ Dark: Deeper orange gradient
 */
@Composable
private fun DailyGoalCard(
    progress: DailyGoalProgress,
    onStartStudy: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFFFF8A65), Color(0xFFFF7043)) // Dark warm orange
                        } else {
                            listOf(Color(0xFFFFB74D), Color(0xFFFFA726)) // Light warm orange
                        }
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Günlük Hedef 🎯",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    if (progress.isDailyGoalComplete) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Tamamlandı",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Bugün çalıştığın kelimeler",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress.todayProgress,
                        animationSpec = tween(durationMillis = 1000),
                        label = "progress"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${progress.todayCompletedCards} / ${progress.todayAvailableCards}",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Start Study Button
                Button(
                    onClick = onStartStudy,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = if (isDarkTheme) Color(0xFFFF7043) else Color(0xFFFF9800)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Text(
                        text = "Çalışmaya Başla",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * UPDATED Package Selection Card - Teal/Cyan Gradient
 * ✅ Light: Bright teal gradient
 * ✅ Dark: Deeper cyan gradient
 * ✅ Different from purple AI card
 */
@Composable
private fun PackageSelectionCard(
    onNavigateToPackageSelection: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToPackageSelection() }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF26A69A), Color(0xFF00897B)) // Dark teal
                        } else {
                            listOf(Color(0xFF4ECDC4), Color(0xFF44A08D)) // Light teal
                        }
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Yeni Paket Seç 📚",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Seviyene uygun kelime paketlerini keşfet ve öğrenmeye başla!",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.ArrowForward,
                    contentDescription = "Git",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * AI Assistant Card - Existing Purple Gradient
 */
@Composable
private fun AIAssistantCard(
    onNavigateToAI: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToAI() }
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF45287B), Color(0xFF2D1B4E)) // Dark theme purple
                        } else {
                            listOf(Color(0xFF667eea), Color(0xFF764ba2)) // Light theme purple
                        }
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "AI Asistan 🤖",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Kişisel öğrenme yardımcın hazır",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.Psychology,
                    contentDescription = "AI",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HocaLingoTheme {
        HomeScreen(
            onNavigateToStudy = {},
            onNavigateToPackageSelection = {},
            onNavigateToAIAssistant = {}
        )
    }
}