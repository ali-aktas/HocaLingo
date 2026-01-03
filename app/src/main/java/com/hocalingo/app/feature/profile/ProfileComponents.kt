package com.hocalingo.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.hocalingo.app.R

/**
 * ProfileComponents - Reusable UI Components
 *
 * Package: feature/profile/
 *
 * Components:
 * - ModernSettingsCard: Main settings card with all options
 * - ModernStatsRow: Three gradient stat cards (Streak, Today, Mastered)
 * - CompactSelectedWordsCard: Selected words preview
 * - LegalAndSupportCard: Privacy, terms, support links
 * - Various setting items: Toggle, Switch, Dropdown, TimePicker
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * ModernSettingsCard - Main settings card
 *
 * Contains all user preferences:
 * - Study Direction (EN→TR / TR→EN)
 * - Theme Mode (Light/Dark/System)
 * - Notifications toggle
 * - Notification time picker
 * - AI Assistant Style (Future feature)
 *
 * @param isDarkTheme Theme state for gradient colors
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
                            listOf(Color(0xFF571EAC), Color(0xFF541780))
                        } else {
                            listOf(Color(0xFF53138B), Color(0xFF6418AF))
                        }
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "Ayarlar",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Theme Mode Toggle
                SettingToggleItem(
                    title = "Tema Modu",
                    subtitle = uiState.themeModeText,
                    icon = Icons.Filled.Palette,
                    isEnabled = true,
                    onToggle = {
                        val newTheme = when (uiState.themeMode) {
                            com.hocalingo.app.core.common.ThemeMode.LIGHT ->
                                com.hocalingo.app.core.common.ThemeMode.DARK
                            com.hocalingo.app.core.common.ThemeMode.DARK ->
                                com.hocalingo.app.core.common.ThemeMode.SYSTEM
                            com.hocalingo.app.core.common.ThemeMode.SYSTEM ->
                                com.hocalingo.app.core.common.ThemeMode.LIGHT
                        }
                        onEvent(ProfileEvent.UpdateThemeMode(newTheme))
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notifications Switch
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

                // Notification Time Picker (shown when notifications enabled)
                if (uiState.notificationsEnabled) {
                    NotificationTimePickerItem(
                        selectedHour = uiState.notificationHour,
                        onHourSelected = { hour ->
                            onEvent(ProfileEvent.UpdateNotificationTime(hour))
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // AI Assistant Style Dropdown
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

/**
 * SettingToggleItem - Toggle setting item
 *
 * Used for cyclic settings (e.g., Theme: Light→Dark→System)
 */
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

/**
 * SettingSwitchItem - Switch setting item
 *
 * Used for boolean settings (e.g., Notifications on/off)
 */
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

/**
 * SettingDropdownItem - Dropdown setting item
 *
 * Used for multiple choice settings (e.g., AI Assistant Style)
 */
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
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option.displayName,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = if (option == selectedOption) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
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

/**
 * NotificationTimePickerItem - Notification time selector
 *
 * Shows when notifications are enabled
 * Allows user to select notification hour (0-23)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationTimePickerItem(
    selectedHour: Int,
    onHourSelected: (Int) -> Unit
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
                imageVector = Icons.Filled.Schedule,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bildirim Saati",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = String.format("%02d:00", selectedHour),
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
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
        ) {
            (0..23).forEach { hour ->
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = String.format("%02d:00", hour),
                                fontFamily = PoppinsFontFamily,
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
                                    modifier = Modifier.size(18.dp)
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
 * ModernStatsRow - Three gradient stat cards
 *
 * Displays:
 * - Streak (Fire icon)
 * - Words Studied Today (TrendingUp icon)
 * - Mastered Words (Trophy icon)
 *
 * @param isDarkTheme Theme state for gradient colors
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

        // Words Studied Today Card
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

        // Mastered Words Card
        GradientStatCard(
            title = "Öğrenilen",
            value = "${uiState.userStats.masteredWordsCount}",
            subtitle = "kelime",
            icon = Icons.Filled.EmojiEvents,
            gradient = Brush.linearGradient(
                colors = if (isDarkTheme) {
                    listOf(Color(0xFFB8860B), Color(0xFFFFD700))
                } else {
                    listOf(Color(0xFFF7971E), Color(0xFFFFD200))
                }
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * GradientStatCard - Single gradient stat card
 *
 * Displays icon, value, title, subtitle with gradient background
 */
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
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
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = Color.White
                )

                Text(
                    text = title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )

                Text(
                    text = subtitle,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * LegalAndSupportCard - Legal & support links card
 *
 * Contains links to:
 * - Privacy Policy
 * - Terms of Service
 * - Play Store (app rating)
 * - Support Email
 */
@Composable
fun LegalAndSupportCard(
    onEvent: (ProfileEvent) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                Color(0xFF1E1E2E)
            } else {
                Color(0xFFFAFAFA)
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
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
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
            LegalSupportItem(
                icon = Icons.Default.Lock,
                title = "Gizlilik Politikası",
                onClick = { onEvent(ProfileEvent.OpenPrivacyPolicy) },
                isDarkTheme = isDarkTheme
            )

            // Terms of Service
            LegalSupportItem(
                icon = Icons.Default.Description,
                title = "Kullanım Sözleşmesi",
                onClick = { onEvent(ProfileEvent.OpenTermsOfService) },
                isDarkTheme = isDarkTheme
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Rate on Play Store
            LegalSupportItem(
                icon = Icons.Default.Star,
                title = "Uygulamayı Değerlendir",
                onClick = { onEvent(ProfileEvent.OpenPlayStore) },
                isDarkTheme = isDarkTheme
            )

            // Contact Support
            LegalSupportItem(
                icon = Icons.Default.Email,
                title = "Destek İletişim",
                onClick = { onEvent(ProfileEvent.OpenSupport) },
                isDarkTheme = isDarkTheme
            )
        }
    }
}

/**
 * LegalSupportItem - Single legal/support link item
 *
 * Displays icon, title with chevron for navigation
 */
@Composable
private fun LegalSupportItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDarkTheme) Color(0xFF9CA3AF) else Color(0xFF6B7280),
            modifier = Modifier.size(20.dp)
        )
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
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isDarkTheme) Color(0xFF6B7280) else Color(0xFF9CA3AF),
            modifier = Modifier.size(20.dp)
        )
    }
}