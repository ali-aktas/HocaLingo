package com.hocalingo.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.hocalingo.app.core.ui.theme.ThemeViewModel
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
 * Profile Screen - Theme-Aware Version
 * ✅ Smart gradients that adapt to light/dark theme
 * ✅ Material 3 theme colors integration
 * ✅ Real-time theme switching support
 * ✅ Maintains visual beauty in both themes
 * ✅ BottomSheet for all selected words with pagination
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToWordsList: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Get theme state for gradients
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    // Local state for AI style selection (temporary - will be connected to ViewModel later)
    var currentAIStyle by remember { mutableStateOf(AIAssistantStyle.FRIENDLY) }

    // BottomSheet state
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

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
                ProfileEffect.ShowWordsBottomSheet -> {
                    // BottomSheet will be shown by state
                }
                ProfileEffect.HideWordsBottomSheet -> {
                    // BottomSheet will be hidden by state
                }
                is ProfileEffect.ShowWordsLoadError -> {
                    snackbarHostState.showSnackbar(effect.error)
                }
                // Notification effects - şimdilik boş implement
                ProfileEffect.RequestNotificationPermission -> { /* TODO: Handle later */ }
                ProfileEffect.ShowNotificationPermissionDialog -> { /* TODO: Handle later */ }
                is ProfileEffect.ShowNotificationScheduled -> { /* TODO: Handle later */ }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background // Theme-aware background
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Settings Header - Theme-aware text color
            item {
                Text(
                    text = "Kişiselleştir",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground, // Theme-aware
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Modern Settings Card - Theme-aware gradients
            item {
                ModernSettingsCard(
                    uiState = uiState,
                    currentAIStyle = currentAIStyle,
                    onEvent = viewModel::onEvent,
                    onAIStyleChange = { newStyle -> currentAIStyle = newStyle },
                    isDarkTheme = isDarkTheme
                )
            }

            // Quick Stats Row - Theme-aware gradient cards
            item {
                ModernStatsRow(uiState = uiState, isDarkTheme = isDarkTheme)
            }

            // Compact Selected Words Card - Theme-aware gradients
            item {
                CompactSelectedWordsCard(
                    words = uiState.selectedWordsPreview.take(5), // Max 5 words
                    totalCount = uiState.totalWordsCount,
                    onViewMore = { viewModel.onEvent(ProfileEvent.ViewAllWords) },
                    isDarkTheme = isDarkTheme
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
                    color = MaterialTheme.colorScheme.primary // Theme-aware
                )
            }
        }
    }

    // Words BottomSheet - Theme-aware
    if (uiState.showWordsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                viewModel.onEvent(ProfileEvent.HideWordsBottomSheet)
            },
            sheetState = bottomSheetState,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            dragHandle = null // Custom header in WordsBottomSheet
        ) {
            WordsBottomSheet(
                words = uiState.allSelectedWords,
                totalCount = uiState.totalWordsCount,
                isLoading = uiState.isLoadingAllWords,
                canLoadMore = uiState.canLoadMoreWords,
                error = uiState.wordsLoadingError,
                onLoadMore = { viewModel.onEvent(ProfileEvent.LoadMoreWords) },
                onRefresh = { viewModel.onEvent(ProfileEvent.RefreshAllWords) },
                onDismiss = { viewModel.onEvent(ProfileEvent.HideWordsBottomSheet) }
            )
        }
    }
}

@Composable
private fun SettingSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onCheckedChange(!isChecked) }
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

        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.White.copy(alpha = 0.3f),
                uncheckedThumbColor = Color.White.copy(alpha = 0.7f),
                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun ModernStatsRow(uiState: ProfileUiState, isDarkTheme: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Streak - Theme-aware fire gradient
        GradientStatCard(
            title = "Streak",
            value = "${uiState.userStats.currentStreak}",
            subtitle = "gün",
            icon = Icons.Filled.LocalFireDepartment,
            gradient = Brush.linearGradient(
                colors = if (isDarkTheme) {
                    listOf(Color(0xFF927E71), Color(0xFFB45433))
                } else {
                    listOf(Color(0xFFFF6B35), Color(0xFFFF8E53))
                }
            ),
            modifier = Modifier.weight(1f)
        )

        // Words Studied Today - Theme-aware teal gradient
        GradientStatCard(
            title = "Bugün",
            value = "${uiState.userStats.wordsStudiedToday}",
            subtitle = "kelime",
            icon = Icons.Filled.TrendingUp,
            gradient = Brush.linearGradient(
                colors = if (isDarkTheme) {
                    listOf(Color(0xFF119A43), Color(0xFF006A79))
                } else {
                    listOf(Color(0xFF4ECDC4), Color(0xFF44A08D))
                }
            ),
            modifier = Modifier.weight(1f)
        )

        // Mastered Words - Theme-aware gold gradient
        GradientStatCard(
            title = "Öğrenilen",
            value = "${uiState.userStats.masteredWordsCount}",
            subtitle = "toplam",
            icon = Icons.Filled.EmojiEvents,
            gradient = Brush.linearGradient(
                colors = if (isDarkTheme) {
                    listOf(Color(0xFFB3973D), Color(0xFF845C00))
                } else {
                    listOf(Color(0xFFFFD700), Color(0xFFFFA500))
                }
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
    onViewMore: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewMore() }, // Make entire card clickable
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF1E1034), Color(0xFF710299))
                        } else {
                            listOf(Color(0xFF667eea), Color(0xFF764ba2))
                        }
                    )
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
                        text = "📚 Seçili Kelimeler",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )

                    Text(
                        text = "Tümünü Gör →",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (words.isEmpty()) {
                    Text(
                        text = "Henüz kelime seçmedin 🚀",
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
    onAIStyleChange: (AIAssistantStyle) -> Unit,
    isDarkTheme: Boolean
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
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF570A7D), Color(0xFF123C50))
                        } else {
                            listOf(Color(0xFF11998e), Color(0xFF38ef7d))
                        }
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "⚙️ Kişiselleştir",
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
                    isEnabled = true,
                    onToggle = {
                        val newDirection = if (uiState.studyDirection == StudyDirection.EN_TO_TR) {
                            StudyDirection.TR_TO_EN
                        } else {
                            StudyDirection.EN_TO_TR
                        }
                        onEvent(ProfileEvent.UpdateStudyDirection(newDirection))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Theme Toggle
                SettingToggleItem(
                    title = "Tema Modu",
                    subtitle = uiState.themeModeText,
                    icon = Icons.Filled.Palette,
                    isEnabled = true,
                    onToggle = {
                        val newTheme = when (uiState.themeMode) {
                            ThemeMode.LIGHT -> ThemeMode.DARK
                            ThemeMode.DARK -> ThemeMode.SYSTEM
                            ThemeMode.SYSTEM -> ThemeMode.LIGHT
                        }
                        onEvent(ProfileEvent.UpdateThemeMode(newTheme))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notifications Toggle
                SettingSwitchItem(
                    title = "Bildirimler",
                    subtitle = "Günlük çalışma hatırlatması",
                    icon = Icons.Filled.Notifications,
                    isChecked = uiState.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        onEvent(ProfileEvent.UpdateNotifications(enabled))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // AI Assistant Style (Future feature - placeholder)
                SettingDropdownItem(
                    title = "AI Asistan Stili",
                    subtitle = currentAIStyle.displayName,
                    icon = Icons.Filled.Psychology,
                    options = AIAssistantStyle.entries,
                    selectedOption = currentAIStyle,
                    onOptionSelected = onAIStyleChange
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
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp)
            )
            .clickable { if (isEnabled) onToggle() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = if (isEnabled) 1f else 0.5f),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = if (isEnabled) 1f else 0.5f)
            )
            Text(
                text = subtitle,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = if (isEnabled) 0.8f else 0.4f)
            )
        }

        Icon(
            imageVector = Icons.Filled.ArrowForward,
            contentDescription = "Değiştir",
            tint = Color.White.copy(alpha = if (isEnabled) 0.7f else 0.3f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingDropdownItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    options: List<AIAssistantStyle>,
    selectedOption: AIAssistantStyle,
    onOptionSelected: (AIAssistantStyle) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(12.dp)
                )
                .clickable { expanded = true }
                .menuAnchor()
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
                imageVector = if (expanded) Icons.Filled.ArrowDropUp else Icons.Filled.ArrowDropDown,
                contentDescription = if (expanded) "Kapat" else "Aç",
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface) // Theme-aware
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.displayName,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (option == selectedOption) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface // Theme-aware
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}