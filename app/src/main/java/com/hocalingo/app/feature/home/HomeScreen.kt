package com.hocalingo.app.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // Theme-aware
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Welcome Header
            item {
                WelcomeHeader(
                    userName = uiState.userName,
                    streakDays = uiState.streakDays,
                    onRefresh = { viewModel.onEvent(HomeEvent.RefreshData) },
                    isDarkTheme = isDarkTheme
                )
            }

            // Daily Goal Card
            item {
                DailyGoalCard(
                    progress = uiState.dailyGoalProgress,
                    onStartStudy = { viewModel.onEvent(HomeEvent.StartStudy) },
                    isDarkTheme = isDarkTheme
                )
            }

            // Package Selection Card (Theme-aware gradients)
            item {
                PackageSelectionCard(
                    onNavigateToPackageSelection = { viewModel.onEvent(HomeEvent.NavigateToPackageSelection) },
                    isDarkTheme = isDarkTheme
                )
            }

            // Monthly Stats
            item {
                MonthlyStatsCard(
                    stats = uiState.monthlyStats,
                    isDarkTheme = isDarkTheme
                )
            }

            // AI Assistant Card (Theme-aware gradients)
            item {
                AIAssistantCard(
                    onNavigateToAI = { viewModel.onEvent(HomeEvent.NavigateToAIAssistant) },
                    isDarkTheme = isDarkTheme
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun WelcomeHeader(
    userName: String,
    streakDays: Int,
    onRefresh: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                MaterialTheme.colorScheme.surface // Dark theme card
            } else {
                Color.White // Light theme card
            }
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Merhaba, $userName! 👋",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface // Theme-aware text
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = if (isDarkTheme) Color(0xFFFF8A65) else Color(0xFFFF5722), // Theme-aware fire color
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$streakDays gün streak!",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // Theme-aware subtitle
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .background(
                        color = if (isDarkTheme) {
                            Color(0xFF26C6DA).copy(alpha = 0.2f) // Dark theme teal
                        } else {
                            Color(0xFF4ECDC4).copy(alpha = 0.1f) // Light theme teal
                        },
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Yenile",
                    tint = if (isDarkTheme) Color(0xFF26C6DA) else Color(0xFF4ECDC4) // Theme-aware teal
                )
            }
        }
    }
}

@Composable
private fun DailyGoalCard(
    progress: DailyGoalProgress,
    onStartStudy: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                MaterialTheme.colorScheme.surface // Dark theme card
            } else {
                Color.White // Light theme card
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
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
                    color = MaterialTheme.colorScheme.onSurface // Theme-aware text
                )

                if (progress.isDailyGoalComplete) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Tamamlandı",
                        tint = if (isDarkTheme) Color(0xFF66BB6A) else Color(0xFF4CAF50), // Theme-aware green
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Günlük kartlar
            DailyGoalProgressRow(
                current = progress.todayCompletedCards,
                total = progress.todayAvailableCards,
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Çalışmaya başla butonu
            Button(
                onClick = onStartStudy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDarkTheme) {
                        Color(0xFFFB9322) // Dark theme orange
                    } else {
                        Color(0xFFFB9322) // Light theme orange
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (progress.todayCompletedCards == 0) "Çalışmaya Başla" else "Devam Et",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyGoalProgressRow(
    current: Int,
    total: Int,
    isDarkTheme: Boolean
) {
    val progress = if (total > 0) current.toFloat() / total.toFloat() else 0f
    val color = if (isDarkTheme) Color(0xFF26C6DA) else Color(0xFF4ECDC4)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Bugün çalıştığın kelimeler",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant // Theme-aware
            )
            Text(
                text = "$current/$total",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val animatedProgress by animateFloatAsState(
            targetValue = progress,
            animationSpec = tween(durationMillis = 800),
            label = "progress"
        )

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

@Composable
private fun PackageSelectionCard(
    onNavigateToPackageSelection: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToPackageSelection() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF26A69A), Color(0xFF00897B)) // Dark theme green
                        } else {
                            listOf(Color(0xFF43E97B), Color(0xFF38F9D7)) // Light theme green
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
                        text = "Seviyene uygun kelime paketlerini keşfet",
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

@Composable
private fun AIAssistantCard(
    onNavigateToAI: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToAI() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF7986CB), Color(0xFF5C6BC0)) // Dark theme purple
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

@Composable
private fun MonthlyStatsCard(
    stats: MonthlyStats,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                MaterialTheme.colorScheme.surface // Dark theme card
            } else {
                Color.White // Light theme card
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Bu Ay 📈",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface // Theme-aware text
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Çalışılan günler
                MonthlyStatItem(
                    label = "Aktif Gün",
                    value = stats.activeDaysThisMonth.toString(),
                    color = if (isDarkTheme) Color(0xFFFF8A65) else Color(0xFFFF5722),
                    icon = Icons.Filled.CalendarToday
                )

                // Çalışma süresi
                MonthlyStatItem(
                    label = "Çalışma Süresi",
                    value = stats.studyTimeFormatted,
                    color = if (isDarkTheme) Color(0xFF66BB6A) else Color(0xFF4CAF50),
                    icon = Icons.Filled.Schedule
                )

                // Disiplin puanı
                MonthlyStatItem(
                    label = "Disiplin",
                    value = "${stats.disciplineScore}%",
                    color = if (isDarkTheme) Color(0xFF26C6DA) else Color(0xFF4ECDC4),
                    icon = Icons.Filled.TrendingUp
                )
            }
        }
    }
}

@Composable
private fun MonthlyStatItem(
    label: String,
    value: String,
    color: Color,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = color
        )

        Text(
            text = label,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, // Theme-aware text
            textAlign = TextAlign.Center
        )
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