package com.hocalingo.app.feature.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocalingo.app.R

/**
 * ProfileComponents - Redesigned Modern UI Components
 *
 * Package: feature/profile/
 *
 * Design Principles:
 * ✅ Material 3 compliant - Clean, no heavy gradients
 * ✅ Compact & Readable - Optimized sizes
 * ✅ Subtle elevation - Professional depth
 * ✅ Icon-first design - Better visual hierarchy
 * ✅ Theme-aware - Light/Dark support
 *
 * Components:
 * - ModernSettingsCard: Clean settings card with pill-style selections
 * - ModernStatsRow: Three compact stat cards
 * - LegalAndSupportCard: Minimal legal & support links
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * ModernSettingsCard - Redesigned Settings Card
 *
 * NEW DESIGN:
 * - Clean white/dark surface (no gradients)
 * - Pill-style selection buttons
 * - Compact spacing
 * - Better visual hierarchy
 * - Icon badges for features
 *
 * @param uiState Current UI state
 * @param currentAIStyle Selected AI style
 * @param onEvent Event handler
 * @param onAIStyleChange AI style change handler
 * @param isDarkTheme Theme mode
 */
@Composable
fun ModernSettingsCard(
    uiState: ProfileUiState,
    currentAIStyle: AIAssistantStyle,
    onEvent: (ProfileEvent) -> Unit,
    onAIStyleChange: (AIAssistantStyle) -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                Color(0xFF1E1E2E)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with icon
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Ayarlar",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Theme Mode - Pill Style Selection
            SettingSection(
                title = "Tema Modu",
                icon = Icons.Outlined.Palette
            ) {
                ThemeModeSelector(
                    currentMode = uiState.themeMode,
                    onModeSelected = { mode ->
                        onEvent(ProfileEvent.UpdateThemeMode(mode))
                    },
                    isDarkTheme = isDarkTheme
                )
            }

            // Notifications Switch
            SettingSwitchRow(
                icon = Icons.Outlined.Notifications,
                title = "Günlük Hatırlatıcı",
                subtitle = "Her gün aynı saatte bildirim al",
                isChecked = uiState.notificationsEnabled,
                onCheckedChange = { enabled ->
                    onEvent(ProfileEvent.UpdateNotifications(enabled))
                },
                isDarkTheme = isDarkTheme
            )

            // Notification Time Picker (shown when enabled)
            AnimatedVisibility(visible = uiState.notificationsEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(modifier = Modifier.height(0.dp))
                    NotificationTimeSelector(
                        selectedHour = uiState.notificationHour,
                        onHourSelected = { hour ->
                            onEvent(ProfileEvent.UpdateNotificationTime(hour))
                        },
                        isDarkTheme = isDarkTheme
                    )
                }
            }

            // AI Assistant Style
            SettingSection(
                title = "AI Asistan Stili",
                icon = Icons.Outlined.Psychology
            ) {
                AIStyleSelector(
                    currentStyle = currentAIStyle,
                    onStyleSelected = onAIStyleChange,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

/**
 * SettingSection - Section header with icon
 */
@Composable
private fun SettingSection(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        content()
    }
}

/**
 * ThemeModeSelector - Pill-style theme selector
 */
@Composable
private fun ThemeModeSelector(
    currentMode: com.hocalingo.app.core.common.ThemeMode,
    onModeSelected: (com.hocalingo.app.core.common.ThemeMode) -> Unit,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ThemeModeButton(
            text = "Açık",
            icon = Icons.Outlined.LightMode,
            isSelected = currentMode == com.hocalingo.app.core.common.ThemeMode.LIGHT,
            onClick = { onModeSelected(com.hocalingo.app.core.common.ThemeMode.LIGHT) },
            isDarkTheme = isDarkTheme,
            modifier = Modifier.weight(1f)
        )
        ThemeModeButton(
            text = "Koyu",
            icon = Icons.Outlined.DarkMode,
            isSelected = currentMode == com.hocalingo.app.core.common.ThemeMode.DARK,
            onClick = { onModeSelected(com.hocalingo.app.core.common.ThemeMode.DARK) },
            isDarkTheme = isDarkTheme,
            modifier = Modifier.weight(1f)
        )
        ThemeModeButton(
            text = "Sistem",
            icon = Icons.Outlined.PhoneAndroid,
            isSelected = currentMode == com.hocalingo.app.core.common.ThemeMode.SYSTEM,
            onClick = { onModeSelected(com.hocalingo.app.core.common.ThemeMode.SYSTEM) },
            isDarkTheme = isDarkTheme,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * ThemeModeButton - Individual pill button
 */
@Composable
private fun ThemeModeButton(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected && isDarkTheme -> MaterialTheme.colorScheme.primary
        isSelected && !isDarkTheme -> MaterialTheme.colorScheme.primary
        !isSelected && isDarkTheme -> Color(0xFF2A2A3E)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isDarkTheme -> Color(0xFF9CA3AF)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                fontFamily = PoppinsFontFamily,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp,
                color = contentColor
            )
        }
    }
}

/**
 * SettingSwitchRow - Modern switch row
 */
@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) {
            Color(0xFF2A2A3E)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

/**
 * NotificationTimeSelector - Compact time picker
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTimeSelector(
    selectedHour: Int,
    onHourSelected: (Int) -> Unit,
    isDarkTheme: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            color = if (isDarkTheme) {
                Color(0xFF2A2A3E)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Bildirim Saati",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = String.format("%02d:00", selectedHour),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            (0..23).forEach { hour ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format("%02d:00", hour),
                                fontFamily = PoppinsFontFamily,
                                fontWeight = if (hour == selectedHour) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (hour == selectedHour) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            if (hour == selectedHour) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onHourSelected(hour)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * AIStyleSelector - Pill-style AI assistant selector
 */
@Composable
private fun AIStyleSelector(
    currentStyle: AIAssistantStyle,
    onStyleSelected: (AIAssistantStyle) -> Unit,
    isDarkTheme: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AIAssistantStyle.entries.chunked(2).forEach { rowStyles ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowStyles.forEach { style ->
                    AIStyleButton(
                        style = style,
                        isSelected = currentStyle == style,
                        onClick = { onStyleSelected(style) },
                        isDarkTheme = isDarkTheme,
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining space if odd number
                if (rowStyles.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * AIStyleButton - Individual AI style button
 */
@Composable
private fun AIStyleButton(
    style: AIAssistantStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected && isDarkTheme -> MaterialTheme.colorScheme.primary
        isSelected && !isDarkTheme -> MaterialTheme.colorScheme.primary
        !isSelected && isDarkTheme -> Color(0xFF2A2A3E)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isDarkTheme -> Color(0xFF9CA3AF)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = style.displayName,
                fontFamily = PoppinsFontFamily,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 13.sp,
                color = contentColor
            )
        }
    }
}

/**
 * ModernStatsRow - Redesigned Compact Stats
 *
 * NEW DESIGN:
 * - Smaller, more compact cards
 * - Subtle colors (no heavy gradients)
 * - Clean icon + number layout
 * - Better spacing
 *
 * @param uiState Current UI state
 * @param isDarkTheme Theme mode
 */
@Composable
fun ModernStatsRow(
    uiState: ProfileUiState,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Streak Card
        CompactStatCard(
            title = "Streak",
            value = "${uiState.userStats.currentStreak}",
            subtitle = "gün",
            icon = Icons.Filled.LocalFireDepartment,
            iconColor = Color(0xFFFF6B35),
            backgroundColor = if (isDarkTheme) Color(0xFF2A2A3E) else Color(0xFFFFEFE9),
            modifier = Modifier.weight(1f),
            isDarkTheme = isDarkTheme
        )

        // Today's Words Card
        CompactStatCard(
            title = "Bugün",
            value = "${uiState.userStats.wordsStudiedToday}",
            subtitle = "kelime",
            icon = Icons.Filled.TrendingUp,
            iconColor = Color(0xFF4ECDC4),
            backgroundColor = if (isDarkTheme) Color(0xFF2A2A3E) else Color(0xFFE8F8F7),
            modifier = Modifier.weight(1f),
            isDarkTheme = isDarkTheme
        )

        // Mastered Words Card
        CompactStatCard(
            title = "Öğrenilen",
            value = "${uiState.userStats.masteredWordsCount}",
            subtitle = "kelime",
            icon = Icons.Filled.EmojiEvents,
            iconColor = Color(0xFFFFB800),
            backgroundColor = if (isDarkTheme) Color(0xFF2A2A3E) else Color(0xFFFFF7E6),
            modifier = Modifier.weight(1f),
            isDarkTheme = isDarkTheme
        )
    }
}

/**
 * CompactStatCard - Single compact stat card
 */
@Composable
private fun CompactStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Value
            Text(
                text = value,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
                color = if (isDarkTheme) Color.White else Color(0xFF1A1A2E)
            )

            // Title
            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = if (isDarkTheme) Color(0xFF9CA3AF) else Color(0xFF6B7280)
            )

            // Subtitle
            Text(
                text = subtitle,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                color = if (isDarkTheme) Color(0xFF6B7280) else Color(0xFF9CA3AF)
            )
        }
    }
}

/**
 * LegalAndSupportCard - Redesigned Minimal Card
 *
 * NEW DESIGN:
 * - Cleaner layout
 * - Better icon placement
 * - Subtle dividers
 * - Compact spacing
 *
 * @param onEvent Event handler
 * @param isDarkTheme Theme mode
 */
@Composable
fun LegalAndSupportCard(
    onEvent: (ProfileEvent) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                Color(0xFF1E1E2E)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Hakkında & Destek",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Privacy Policy
            LegalSupportRow(
                icon = Icons.Outlined.Lock,
                title = "Gizlilik Politikası",
                onClick = { onEvent(ProfileEvent.OpenPrivacyPolicy) },
                isDarkTheme = isDarkTheme
            )

            // Terms of Service
            LegalSupportRow(
                icon = Icons.Outlined.Description,
                title = "Kullanım Sözleşmesi",
                onClick = { onEvent(ProfileEvent.OpenTermsOfService) },
                isDarkTheme = isDarkTheme
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Rate on Play Store
            LegalSupportRow(
                icon = Icons.Outlined.Star,
                title = "Uygulamayı Değerlendir",
                onClick = { onEvent(ProfileEvent.OpenPlayStore) },
                isDarkTheme = isDarkTheme
            )

            // Contact Support
            LegalSupportRow(
                icon = Icons.Outlined.Email,
                title = "Destek İletişim",
                onClick = { onEvent(ProfileEvent.OpenSupport) },
                isDarkTheme = isDarkTheme
            )
        }
    }
}

/**
 * LegalSupportRow - Single clickable row
 */
@Composable
private fun LegalSupportRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isDarkTheme) {
            Color(0xFF2A2A3E)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}