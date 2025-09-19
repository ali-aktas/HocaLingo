package com.hocalingo.app.feature.home.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
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
 * Modern Home Dashboard Screen
 * Central hub for HocaLingo app with clean Material 3 design
 */
@Composable
fun HomeScreen(
    onNavigateToStudy: () -> Unit,
    onNavigateToAddWord: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                HomeEffect.NavigateToStudy -> onNavigateToStudy()
                HomeEffect.NavigateToAddWord -> onNavigateToAddWord()
                HomeEffect.NavigateToProfile -> onNavigateToProfile()
                is HomeEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        if (uiState.isLoading) {
            LoadingState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome header with streak
                item {
                    WelcomeHeader(
                        userName = uiState.userName,
                        streakDays = uiState.streakDays,
                        onRefresh = { viewModel.onEvent(HomeEvent.RefreshData) }
                    )
                }

                // Daily goal progress
                item {
                    DailyGoalCard(
                        progress = uiState.dailyGoalProgress,
                        onStartStudy = { viewModel.onEvent(HomeEvent.StartStudy) }
                    )
                }

                // Today's words quick study
                item {
                    TodayWordsSection(
                        words = uiState.todayWords,
                        onWordClick = { wordId ->
                            viewModel.onEvent(HomeEvent.QuickWordStudy(wordId))
                        }
                    )
                }

                // Weekly stats overview
                item {
                    WeeklyStatsCard(
                        stats = uiState.weeklyStats,
                        onViewDetails = { viewModel.onEvent(HomeEvent.ViewProgress) }
                    )
                }

                // Quick actions grid
                item {
                    QuickActionsGrid(
                        actions = uiState.quickActions,
                        onActionClick = { action ->
                            when (action.action) {
                                QuickActionType.START_STUDY -> viewModel.onEvent(HomeEvent.StartStudy)
                                QuickActionType.ADD_WORD -> viewModel.onEvent(HomeEvent.AddWord)
                                QuickActionType.VIEW_PROGRESS -> viewModel.onEvent(HomeEvent.ViewProgress)
                                QuickActionType.DAILY_CHALLENGE -> {
                                    // TODO: Implement daily challenge
                                }
                            }
                        }
                    )
                }

                // Bottom spacing for better scrolling
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
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
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Dashboard yükleniyor...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(20.dp)
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
                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Yenile",
                    tint = MaterialTheme.colorScheme.primary
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (progress.isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Tamamlandı",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Words progress
            ProgressItem(
                title = "Kelimeler",
                current = progress.completedWords,
                target = progress.targetWords,
                progress = progress.wordsProgress,
                icon = Icons.Outlined.MenuBook,
                color = Color(0xFF2196F3)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Minutes progress
            ProgressItem(
                title = "Dakika",
                current = progress.completedMinutes,
                target = progress.targetMinutes,
                progress = progress.minutesProgress,
                icon = Icons.Outlined.AccessTime,
                color = Color(0xFF4CAF50)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStartStudy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Çalışmaya Başla",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ProgressItem(
    title: String,
    current: Int,
    target: Int,
    progress: Float,
    icon: ImageVector,
    color: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(),
        label = "progress_animation"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$current/$target",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun TodayWordsSection(
    words: List<TodayWord>,
    onWordClick: (String) -> Unit
) {
    if (words.isEmpty()) return

    Column {
        Text(
            text = "Bugünkü Kelimeler 📚",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(words) { word ->
                WordCard(
                    word = word,
                    onClick = { onWordClick(word.id) }
                )
            }
        }
    }
}

@Composable
private fun WordCard(
    word: TodayWord,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (word.isLearned) {
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = word.level,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = getLevelColor(word.level),
                    modifier = Modifier
                        .background(
                            color = getLevelColor(word.level).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )

                if (word.isLearned) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Öğrenildi",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = word.english,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = word.turkish,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeeklyStatsCard(
    stats: WeeklyStats,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
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
                    text = "Bu Hafta 📊",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                TextButton(onClick = onViewDetails) {
                    Text(
                        text = "Detaylar",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    title = "Kelime",
                    value = "${stats.totalWords}",
                    icon = Icons.Outlined.MenuBook
                )
                StatItem(
                    title = "Dakika",
                    value = "${stats.totalMinutes}",
                    icon = Icons.Outlined.AccessTime
                )
                StatItem(
                    title = "Doğruluk",
                    value = "${(stats.accuracyRate * 100).toInt()}%",
                    icon = Icons.Outlined.SettingsAccessibility
                )
                StatItem(
                    title = "Aktif Gün",
                    value = "${stats.daysActive}",
                    icon = Icons.Outlined.CalendarToday
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = title,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuickActionsGrid(
    actions: List<QuickAction>,
    onActionClick: (QuickAction) -> Unit
) {
    Column {
        Text(
            text = "Hızlı Eylemler ⚡",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2x2 grid layout
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                actions.take(2).forEach { action ->
                    QuickActionCard(
                        action = action,
                        onClick = { onActionClick(action) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                actions.drop(2).take(2).forEach { action ->
                    QuickActionCard(
                        action = action,
                        onClick = { onActionClick(action) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    action: QuickAction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(100.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = getActionIcon(action.icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = action.title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = action.description,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getActionIcon(iconName: String): ImageVector {
    return when (iconName) {
        "study" -> Icons.Filled.School
        "add" -> Icons.Filled.Add
        "chart" -> Icons.Filled.BarChart
        "trophy" -> Icons.Filled.EmojiEvents
        else -> Icons.Filled.Star
    }
}

private fun getLevelColor(level: String): Color {
    return when (level) {
        "A1" -> Color(0xFFFF6B35)
        "A2" -> Color(0xFF4ECDC4)
        "B1" -> Color(0xFF43E97B)
        "B2" -> Color(0xFF667eea)
        "C1" -> Color(0xFFf12711)
        "C2" -> Color(0xFFff9a9e)
        else -> Color(0xFF666666)
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    HocaLingoTheme {
        // Preview placeholder
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Home Screen Preview")
        }
    }
}