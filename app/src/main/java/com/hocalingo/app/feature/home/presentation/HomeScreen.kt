package com.hocalingo.app.feature.home.presentation

import androidx.compose.animation.animateContentSize
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
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import kotlinx.coroutines.flow.collectLatest

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * Modern Home Dashboard Screen - v2.0
 * PackageSelection teması ile yeniden tasarlandı
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeckInfoRow(
    totalCards: Int,
    masteredCards: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Toplam deck kartları
        DeckInfoItem(
            label = "Destedeki Kelime",
            value = totalCards.toString(),
            color = Color(0xFF44A08D)
        )

        // Mastered kartlar
        DeckInfoItem(
            label = "Öğrenilen Kelime",
            value = masteredCards.toString(),
            color = Color(0xFF4CAF50)
        )
    }
}

@Composable
private fun DeckInfoItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            color = Color(0xFF6C7B8A),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HomeScreen(
    onNavigateToStudy: () -> Unit,
    onNavigateToPackageSelection: () -> Unit = {},
    onNavigateToAIAssistant: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFA) // Açık background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                LoadingState(modifier = Modifier.fillMaxSize())
            } else {
                HomeContent(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onEvent: (HomeEvent) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcome Header with Streak
        item {
            WelcomeHeader(
                userName = uiState.userName,
                streakDays = uiState.streakDays,
                onRefresh = { onEvent(HomeEvent.RefreshData) }
            )
        }

        // Daily Goal Progress
        item {
            DailyGoalCard(
                progress = uiState.dailyGoalProgress,
                onStartStudy = { onEvent(HomeEvent.StartStudy) }
            )
        }

        // Package Selection Card (Bugünkü kelimeler yerine)
        item {
            PackageSelectionCard(
                onNavigateToPackageSelection = { onEvent(HomeEvent.NavigateToPackageSelection) }
            )
        }

        // Monthly Stats (Bu Hafta → Bu Ay)
        item {
            MonthlyStatsCard(
                stats = uiState.monthlyStats
            )
        }

        // AI Assistant Card (Hızlı Eylemler yerine)
        item {
            AIAssistantCard(
                onNavigateToAI = { onEvent(HomeEvent.NavigateToAIAssistant) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WelcomeHeader(
    userName: String,
    streakDays: Int,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
                    color = Color(0xFF2C3E50)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocalFireDepartment,
                        contentDescription = null,
                        tint = Color(0xFFFF5722),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$streakDays gün streak!",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF6C7B8A)
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .background(
                        color = Color(0xFF4ECDC4).copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Yenile",
                    tint = Color(0xFF4ECDC4)
                )
            }
        }
    }
}

@Composable
private fun DailyGoalCard(
    progress: DailyGoalProgress,
    onStartStudy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
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
                    color = Color(0xFF2C3E50)
                )

                if (progress.isDailyGoalComplete) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Tamamlandı",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Günlük kartlar progress (sadece bu progress bar'lı)
            ProgressItem(
                title = "Günlük Kartlar",
                current = progress.todayCompletedCards,
                total = progress.todayAvailableCards,
                progress = progress.todayProgress,
                color = Color(0xFF4ECDC4)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Deck bilgisi (sadece sayılar, progress bar yok)
            DeckInfoRow(
                totalCards = progress.totalDeckCards,
                masteredCards = progress.masteredDeckCards
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Start Study Button
            Button(
                onClick = onStartStudy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D4FF)
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Çalışmaya Başla",
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
private fun ProgressItem(
    title: String,
    current: Int,
    total: Int,
    progress: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF6C7B8A)
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
    onNavigateToPackageSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToPackageSelection() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF43E97B), Color(0xFF38F9D7))
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
                    Text(
                        text = "Seviyene uygun paketleri keşfet",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun MonthlyStatsCard(
    stats: MonthlyStats
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Bu Ay 📊",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF2C3E50)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    title = "Süre",
                    value = stats.studyTimeFormatted,
                    icon = Icons.Outlined.AccessTime,
                    color = Color(0xFF4ECDC4)
                )
                StatItem(
                    title = "Aktif Gün",
                    value = "${stats.activeDaysThisMonth}",
                    icon = Icons.Outlined.CalendarToday,
                    color = Color(0xFF44A08D)
                )
                StatItem(
                    title = "Disiplin",
                    value = "${stats.disciplineScore}%",
                    icon = Icons.Outlined.EmojiEvents,
                    color = Color(0xFF00D4FF)
                )
            }

            if (stats.chartData.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                // Simple Chart
                SimpleLineChart(
                    data = stats.chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF2C3E50)
        )
        Text(
            text = title,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color(0xFF6C7B8A)
        )
    }
}

@Composable
private fun SimpleLineChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier
) {
    // Simple bar chart using LinearProgressIndicator
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.take(7).forEach { point ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Bar
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(40.dp)
                        .background(
                            color = Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(4.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(point.value)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF4ECDC4),
                                        Color(0xFF44A08D)
                                    )
                                ),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .align(Alignment.BottomCenter)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Day label
                Text(
                    text = point.day,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = Color(0xFF6C7B8A)
                )
            }
        }
    }
}

@Composable
private fun AIAssistantCard(
    onNavigateToAI: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToAI() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
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
                    Text(
                        text = "Sorularınız için yapay zeka desteği",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Icon(
                    imageVector = Icons.Filled.Psychology,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF4ECDC4)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Dashboard yükleniyor...",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = Color(0xFF6C7B8A)
            )
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