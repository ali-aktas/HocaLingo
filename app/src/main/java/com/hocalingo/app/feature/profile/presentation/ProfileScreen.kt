package com.hocalingo.app.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

// Poppins font family - modern, clean
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

/**
 * Profile Screen - Modern, Performance Optimized
 * ✅ No top bar - space efficient
 * ✅ Card-based design matching home screen
 * ✅ Small, clean fonts
 * ✅ 5 words preview + "View More"
 * ✅ Settings toggles
 */
@Composable
fun ProfileScreen(
    onNavigateToWordsList: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Welcome Header
            item {
                WelcomeHeader(
                    userName = uiState.userName,
                    onRefresh = { viewModel.onEvent(ProfileEvent.RefreshData) }
                )
            }

            // Quick Stats Row
            item {
                QuickStatsRow(uiState = uiState)
            }

            // Selected Words Card
            item {
                SelectedWordsCard(
                    words = uiState.selectedWordsPreview,
                    totalCount = uiState.totalWordsCount,
                    onViewMore = { viewModel.onEvent(ProfileEvent.ViewAllWords) }
                )
            }

            // Settings Section
            item {
                SettingsCard(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }

            // Bottom spacing for navigation
            item {
                Spacer(modifier = Modifier.height(32.dp))
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
private fun WelcomeHeader(
    userName: String,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Profil 👤",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF2C3E50)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Merhaba, $userName!",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF6C7B8A)
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Yenile",
                    tint = Color(0xFF4ECDC4),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickStatsRow(uiState: ProfileUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Streak
        StatCard(
            title = "Streak",
            value = "${uiState.userStats.currentStreak}",
            subtitle = "gün",
            icon = Icons.Filled.LocalFireDepartment,
            color = Color(0xFFFF5722),
            modifier = Modifier.weight(1f)
        )

        // Words Studied Today
        StatCard(
            title = "Bugün",
            value = "${uiState.userStats.wordsStudiedToday}",
            subtitle = "kelime",
            icon = Icons.Filled.TrendingUp,
            color = Color(0xFF4ECDC4),
            modifier = Modifier.weight(1f)
        )

        // Mastered Words
        StatCard(
            title = "Öğrenilen",
            value = "${uiState.userStats.masteredWordsCount}",
            subtitle = "toplam",
            icon = Icons.Filled.EmojiEvents,
            color = Color(0xFFFFD700),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
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
                fontSize = 10.sp,
                color = Color(0xFF6C7B8A)
            )
            Text(
                text = subtitle,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 9.sp,
                color = Color(0xFF9E9E9E)
            )
        }
    }
}

@Composable
private fun SelectedWordsCard(
    words: List<WordSummary>,
    totalCount: Int,
    onViewMore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
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
                Column {
                    Text(
                        text = "Seçili Kelimeler 📚",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF2C3E50)
                    )
                    Text(
                        text = "$totalCount kelime seçili",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color(0xFF6C7B8A)
                    )
                }

                TextButton(
                    onClick = onViewMore,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF4ECDC4)
                    )
                ) {
                    Text(
                        text = "Daha fazla",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (words.isEmpty()) {
                Text(
                    text = "Henüz kelime seçmediniz.",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFF9E9E9E),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    words.take(5).forEach { word ->
                        WordItem(word = word)
                    }
                }
            }
        }
    }
}

@Composable
private fun WordItem(word: WordSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFF8FAFA),
                RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = word.english,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Color(0xFF2C3E50)
            )
            Text(
                text = word.turkish,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Color(0xFF6C7B8A)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = word.level,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 9.sp,
                color = Color(0xFF9E9E9E)
            )

            if (word.isMastered) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Öğrenildi",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    uiState: ProfileUiState,
    onEvent: (ProfileEvent) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Ayarlar ⚙️",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF2C3E50)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Theme Setting
            SettingItem(
                title = "Tema",
                subtitle = uiState.themeModeText,
                icon = Icons.Outlined.Palette,
                onClick = {
                    val nextTheme = when (uiState.themeMode) {
                        ThemeMode.SYSTEM -> ThemeMode.LIGHT
                        ThemeMode.LIGHT -> ThemeMode.DARK
                        ThemeMode.DARK -> ThemeMode.SYSTEM
                    }
                    onEvent(ProfileEvent.UpdateThemeMode(nextTheme))
                }
            )

            // Study Direction Setting
            SettingItem(
                title = "Çalışma Yönü",
                subtitle = uiState.studyDirectionText,
                icon = Icons.Outlined.SwapHoriz,
                onClick = {
                    val nextDirection = when (uiState.studyDirection) {
                        StudyDirection.EN_TO_TR -> StudyDirection.TR_TO_EN
                        StudyDirection.TR_TO_EN -> StudyDirection.MIXED
                        StudyDirection.MIXED -> StudyDirection.EN_TO_TR
                    }
                    onEvent(ProfileEvent.UpdateStudyDirection(nextDirection))
                }
            )

            // Notifications Setting
            SettingToggleItem(
                title = "Motivasyon Bildirimleri",
                subtitle = if (uiState.notificationsEnabled) "Açık" else "Kapalı",
                icon = Icons.Outlined.Notifications,
                checked = uiState.notificationsEnabled,
                onCheckedChange = { enabled ->
                    onEvent(ProfileEvent.UpdateNotifications(enabled))
                }
            )
        }
    }
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4ECDC4),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Color(0xFF2C3E50)
            )
            Text(
                text = subtitle,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Color(0xFF6C7B8A)
            )
        }

        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF9E9E9E),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SettingToggleItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4ECDC4),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Color(0xFF2C3E50)
            )
            Text(
                text = subtitle,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Color(0xFF6C7B8A)
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF4ECDC4),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFE0E0E0)
            )
        )
    }
}