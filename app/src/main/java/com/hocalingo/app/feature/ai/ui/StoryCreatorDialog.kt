package com.hocalingo.app.feature.ai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hocalingo.app.R
import com.hocalingo.app.feature.ai.models.StoryDifficulty
import com.hocalingo.app.feature.ai.models.StoryLength
import com.hocalingo.app.feature.ai.models.StoryType

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * StoryCreatorDialog - Redesigned Version
 *
 * New Design Features:
 * ✅ Dark theme (#1A1625, #211A2E)
 * ✅ Pill-style selection buttons
 * ✅ Lock icons for premium features (NO "Pro" badge)
 * ✅ Modern gradient background
 * ✅ Clean, minimal design
 * ✅ FlowRow for responsive layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryCreatorDialog(
    isPremium: Boolean,
    quotaRemaining: Int,
    isGenerating: Boolean,
    generationError: String?,
    onDismiss: () -> Unit,
    onGenerate: (String?, StoryType, StoryDifficulty, StoryLength) -> Unit,
    onShowPremiumPaywall: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    var topic by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(StoryType.STORY) }
    var selectedDifficulty by remember { mutableStateOf(StoryDifficulty.EASY) }
    var selectedLength by remember { mutableStateOf(StoryLength.SHORT) }

    Dialog(
        onDismissRequest = { if (!isGenerating) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isGenerating,
            dismissOnClickOutside = !isGenerating,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF211A2E),
                            Color(0xFF1A1625)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                DialogHeader(
                    onDismiss = if (!isGenerating) onDismiss else null
                )

                Spacer(Modifier.height(24.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Story type selection
                    TypeSelectionSection(
                        selectedType = selectedType,
                        onTypeSelected = { type ->
                            if (type != StoryType.STORY && !isPremium) {
                                onShowPremiumPaywall()
                            } else {
                                selectedType = type
                            }
                        },
                        isPremium = isPremium,
                        enabled = !isGenerating
                    )

                    // Difficulty selection
                    DifficultySelectionSection(
                        selectedDifficulty = selectedDifficulty,
                        onDifficultySelected = { difficulty ->
                            if (difficulty != StoryDifficulty.EASY && !isPremium) {
                                onShowPremiumPaywall()
                            } else {
                                selectedDifficulty = difficulty
                            }
                        },
                        isPremium = isPremium,
                        enabled = !isGenerating
                    )

                    // Length selection
                    LengthSelectionSection(
                        selectedLength = selectedLength,
                        onLengthSelected = { length ->
                            if (length != StoryLength.SHORT && !isPremium) {
                                onShowPremiumPaywall()
                            } else {
                                selectedLength = length
                            }
                        },
                        isPremium = isPremium,
                        enabled = !isGenerating
                    )

                    // Error display
                    AnimatedVisibility(visible = generationError != null) {
                        ErrorMessage(
                            message = generationError ?: "",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Generate button
                GenerateButton(
                    onClick = {
                        onGenerate(
                            if (topic.isBlank()) null else topic,
                            selectedType,
                            selectedDifficulty,
                            selectedLength
                        )
                    },
                    enabled = !isGenerating && quotaRemaining > 0,
                    isLoading = isGenerating,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Dialog Header
 */
@Composable
private fun DialogHeader(
    onDismiss: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Hikayeni Özelleştir",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color.White
        )

        if (onDismiss != null) {
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Kapat",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * Type Selection Section
 */
@Composable
private fun TypeSelectionSection(
    selectedType: StoryType,
    onTypeSelected: (StoryType) -> Unit,
    isPremium: Boolean,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Hikaye Türü",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StoryType.entries.forEach { type ->
                val isSelected = selectedType == type
                val isPremiumLocked = type != StoryType.STORY && !isPremium

                PillButton(
                    text = type.displayName,
                    isSelected = isSelected,
                    isPremiumLocked = isPremiumLocked,
                    onClick = { if (enabled) onTypeSelected(type) },
                    enabled = enabled
                )
            }
        }
    }
}

/**
 * Difficulty Selection Section
 */
@Composable
private fun DifficultySelectionSection(
    selectedDifficulty: StoryDifficulty,
    onDifficultySelected: (StoryDifficulty) -> Unit,
    isPremium: Boolean,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Zorluk Seviyesi",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StoryDifficulty.entries.forEach { difficulty ->
                val isSelected = selectedDifficulty == difficulty
                val isPremiumLocked = difficulty != StoryDifficulty.EASY && !isPremium

                PillButton(
                    text = difficulty.displayName,
                    isSelected = isSelected,
                    isPremiumLocked = isPremiumLocked,
                    onClick = { if (enabled) onDifficultySelected(difficulty) },
                    enabled = enabled
                )
            }
        }
    }
}

/**
 * Length Selection Section
 */
@Composable
private fun LengthSelectionSection(
    selectedLength: StoryLength,
    onLengthSelected: (StoryLength) -> Unit,
    isPremium: Boolean,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Uzunluk",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StoryLength.entries.forEach { length ->
                val isSelected = selectedLength == length
                val isPremiumLocked = length != StoryLength.SHORT && !isPremium

                PillButton(
                    text = length.displayName,
                    isSelected = isSelected,
                    isPremiumLocked = isPremiumLocked,
                    onClick = { if (enabled) onLengthSelected(length) },
                    enabled = enabled
                )
            }
        }
    }
}

/**
 * Pill Button - Modern selection button
 * ✅ ONLY shows Lock icon for premium items (NO "Pro" badge text)
 */
@Composable
private fun PillButton(
    text: String,
    isSelected: Boolean,
    isPremiumLocked: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isSelected -> Color(0xFF7C3AED)
        isPremiumLocked -> Color(0xFF2D1B4E)
        else -> Color(0xFF2D1B4E)
    }

    val contentColor = when {
        isSelected -> Color.White
        isPremiumLocked -> Color.White.copy(alpha = 0.5f)
        else -> Color.White.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(backgroundColor)
            .then(
                if (!isSelected && !isPremiumLocked) {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    )
                } else Modifier
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Sadece kilit ikonu (Premium badge yok!)
            if (isPremiumLocked) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Premium özellik",
                    tint = Color(0xFFFF8A00),
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = text,
                fontFamily = PoppinsFontFamily,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = contentColor
            )
        }
    }
}

/**
 * Generate Button
 */
@Composable
private fun GenerateButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF7C3AED),
            disabledContainerColor = Color(0xFF2D1B4E)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                "Hikaye Oluştur",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Error Message
 */
@Composable
private fun ErrorMessage(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFF5252).copy(alpha = 0.2f))
            .border(
                width = 1.dp,
                color = Color(0xFFFF5252),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFFF5252),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = message,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Color.White
            )
        }
    }
}