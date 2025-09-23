package com.hocalingo.app.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hocalingo.app.R
import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.common.ThemeMode
import com.hocalingo.app.feature.profile.domain.WordSummary
import kotlinx.coroutines.flow.collectLatest

// Poppins font family - enhanced with Black weight
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

// AI Assistant Style enum for UI display
enum class AIAssistantStyle(val displayName: String) {
    FRIENDLY("Samimi"),
    MOTIVATIONAL("Motivasyonel"),
    PROFESSIONAL("Profesyonel")
}

/**
 * Profile Screen - Modern, Gradient Design
 * ✅ Settings card moved to top
 * ✅ Motivation notifications toggle added
 * ✅ Sound setting removed
 * ✅ AI Assistant Style added
 * ✅ Settings header with Poppins Black
 * ✅ Selected words card moved to bottom
 */
@Composable
fun ProfileScreen(
    onNavigateToWordsList: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Local state for AI style selection (temporary - will be connected to ViewModel later)
    var currentAIStyle by remember { mutableStateOf(AIAssistantStyle.FRIENDLY) }

    // Handle effects
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
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFA)
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Settings Header - Poppins Black
            item {
                Text(
                    text = "Ayarlar",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color(0xFF2C3E50),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Modern Settings Card - Now at the top
            item {
                ModernSettingsCard(
                    uiState = uiState,
                    currentAIStyle = currentAIStyle,
                    onEvent = viewModel::onEvent,
                    onAIStyleChange = { newStyle -> currentAIStyle = newStyle }
                )
            }

            // Quick Stats Row - Gradient Cards
            item {
                ModernStatsRow(uiState = uiState)
            }

            // Compact Selected Words Card - Now at the bottom
            item {
                CompactSelectedWordsCard(
                    words = uiState.selectedWordsPreview.take(5), // Max 5 words
                    totalCount = uiState.totalWordsCount,
                    onViewMore = { viewModel.onEvent(ProfileEvent.ViewAllWords) }
                )
            }

            // Bottom spacing for navigation
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Loading overlay
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF4ECDC4)
                )
            }
        }
    }
}

@Composable
private fun ModernStatsRow(uiState: ProfileUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Streak - Fire gradient
        GradientStatCard(
            title = "Streak",
            value = "${uiState.userStats.currentStreak}",
            subtitle = "gün",
            icon = Icons.Filled.LocalFireDepartment,
            gradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFF6B35),
                    Color(0xFFFF8E53)
                )
            ),
            modifier = Modifier.weight(1f)
        )

        // Words Studied Today - Teal gradient (matching bottom nav)
        GradientStatCard(
            title = "Bugün",
            value = "${uiState.userStats.wordsStudiedToday}",
            subtitle = "kelime",
            icon = Icons.Filled.TrendingUp,
            gradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF4ECDC4),
                    Color(0xFF44A08D)
                )
            ),
            modifier = Modifier.weight(1f)
        )

        // Mastered Words - Gold gradient
        GradientStatCard(
            title = "Öğrenilen",
            value = "${uiState.userStats.masteredWordsCount}",
            subtitle = "toplam",
            icon = Icons.Filled.EmojiEvents,
            gradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFFD700),
                    Color(0xFFFFA500)
                )
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GradientStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = subtitle,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun CompactSelectedWordsCard(
    words: List<WordSummary>,
    totalCount: Int,
    onViewMore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFF36D25),
                            Color(0xFF9F6ADB)
                        )
                    )
                )
                .padding(30.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "📚 Seçili Kelimeler",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                        Text(
                            text = "$totalCount kelime toplam",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    TextButton(
                        onClick = onViewMore,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Daha fazla →",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (words.isEmpty()) {
                    Text(
                        text = "Henüz kelime seçmediniz.\nPaket seçimi yaparak başlayın! 🚀",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Show first 5 words in compact format
                    words.forEach { word ->
                        CompactWordItem(word = word)
                        if (word != words.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    if (totalCount > 5) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "ve ${totalCount - 5} kelime daha...",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactWordItem(word: WordSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = word.english,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "→",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Text(
            text = word.turkish,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun ModernSettingsCard(
    uiState: ProfileUiState,
    currentAIStyle: AIAssistantStyle,
    onEvent: (ProfileEvent) -> Unit,
    onAIStyleChange: (AIAssistantStyle) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF11998e),
                            Color(0xFF38ef7d)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "⚙️ Ayarlar",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Study Direction Toggle
                SettingToggleItem(
                    title = "Çalışma Yönü",
                    subtitle = if (uiState.studyDirection == StudyDirection.EN_TO_TR) "İngilizce → Türkçe" else "Türkçe → İngilizce",
                    icon = Icons.Filled.SwapHoriz,
                    onClick = {
                        val newDirection = if (uiState.studyDirection == StudyDirection.EN_TO_TR) StudyDirection.TR_TO_EN else StudyDirection.EN_TO_TR
                        onEvent(ProfileEvent.UpdateStudyDirection(newDirection))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Motivation Notifications Toggle - NEW
                SettingToggleItem(
                    title = "Motivasyon Bildirimleri",
                    subtitle = if (uiState.notificationsEnabled) "Açık" else "Kapalı",
                    icon = if (uiState.notificationsEnabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                    onClick = {
                        // Will be connected to ViewModel later
                        // onEvent(ProfileEvent.UpdateNotifications(!uiState.notificationsEnabled))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // AI Assistant Style Toggle - NEW
                SettingToggleItem(
                    title = "Yapay Zeka Tarzı",
                    subtitle = currentAIStyle.displayName,
                    icon = Icons.Filled.Psychology,
                    onClick = {
                        val nextStyle = when (currentAIStyle) {
                            AIAssistantStyle.FRIENDLY -> AIAssistantStyle.MOTIVATIONAL
                            AIAssistantStyle.MOTIVATIONAL -> AIAssistantStyle.PROFESSIONAL
                            AIAssistantStyle.PROFESSIONAL -> AIAssistantStyle.FRIENDLY
                        }
                        onAIStyleChange(nextStyle)
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Theme Toggle
                SettingToggleItem(
                    title = "Tema",
                    subtitle = when (uiState.themeMode) {
                        ThemeMode.LIGHT -> "Açık"
                        ThemeMode.DARK -> "Koyu"
                        ThemeMode.SYSTEM -> "Sistem"
                    },
                    icon = Icons.Filled.Palette,
                    onClick = {
                        val newTheme = when (uiState.themeMode) {
                            ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.SYSTEM
                            ThemeMode.SYSTEM -> ThemeMode.LIGHT
                        }
                        onEvent(ProfileEvent.UpdateThemeMode(newTheme))
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}