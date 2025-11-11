package com.hocalingo.app.feature.ai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
 * StoryCreatorDialog - AI Story Generation Dialog
 *
 * Package: feature/ai/ui/
 *
 * Features:
 * ✅ Topic input field
 * ✅ Type selection (Hikaye, Motivasyon, Diyalog, Makale)
 * ✅ Difficulty selection (Kolay, Orta, Zor)
 * ✅ Length selection (Kısa, Orta, Uzun)
 * ✅ Premium gates (Orta/Zor difficulty, Orta/Uzun length)
 * ✅ Generate button with loading state
 * ✅ Error display
 * ✅ Quota display
 * ✅ Theme-aware design
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
        Card(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF1E1E1E) else Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Hikaye Oluştur",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = if (isDarkTheme) Color.White else Color.Black
                    )

                    if (!isGenerating) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = if (isDarkTheme) Color.White else Color.Black
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Quota indicator
                QuotaIndicator(
                    quotaRemaining = quotaRemaining,
                    isDarkTheme = isDarkTheme
                )

                Spacer(Modifier.height(20.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Topic input (optional)
                    TopicInputSection(
                        topic = topic,
                        onTopicChange = { topic = it },
                        enabled = !isGenerating,
                        isDarkTheme = isDarkTheme
                    )

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
                        isPremium = isPremium,  // ✅ EKLE
                        enabled = !isGenerating,
                        isDarkTheme = isDarkTheme
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
                        enabled = !isGenerating,
                        isDarkTheme = isDarkTheme
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
                        enabled = !isGenerating,
                        isDarkTheme = isDarkTheme
                    )

                    // Error display
                    AnimatedVisibility(visible = generationError != null) {
                        ErrorMessage(
                            message = generationError ?: "",
                            isDarkTheme = isDarkTheme
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Generate button
                GenerateButton(
                    onClick = {
                        val topicText = topic.trim().takeIf { it.isNotBlank() }
                        onGenerate(topicText, selectedType, selectedDifficulty, selectedLength)
                    },
                    enabled = !isGenerating && quotaRemaining > 0,
                    isLoading = isGenerating,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
}

@Composable
private fun QuotaIndicator(
    quotaRemaining: Int,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDarkTheme) Color(0xFF2D2D2D) else Color(0xFFF5F5F5),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (quotaRemaining > 0) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (quotaRemaining > 0) Color(0xFF4CAF50) else Color(0xFFFF5252),
                modifier = Modifier.size(20.dp)
            )
            Text(
                "Kalan Hakkınız",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f)
            )
        }

        Text(
            "$quotaRemaining/2",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = if (quotaRemaining > 0) Color(0xFF4CAF50) else Color(0xFFFF5252)
        )
    }
}

@Composable
private fun TopicInputSection(
    topic: String,
    onTopicChange: (String) -> Unit,
    enabled: Boolean,
    isDarkTheme: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Konu (İsteğe Bağlı)",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = if (isDarkTheme) Color.White else Color.Black
        )

        OutlinedTextField(
            value = topic,
            onValueChange = onTopicChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Örn: Deniz kenarında tatil",
                    fontFamily = PoppinsFontFamily,
                    fontSize = 14.sp
                )
            },
            enabled = enabled,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isDarkTheme) Color(0xFF7986CB) else Color(0xFF667eea),
                unfocusedBorderColor = if (isDarkTheme) Color(0xFF404040) else Color(0xFFE0E0E0)
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp
            )
        )

        Text(
            "Boş bırakırsanız kelimelerinizle rastgele bir hikaye oluşturulur",
            fontFamily = PoppinsFontFamily,
            fontSize = 12.sp,
            color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun TypeSelectionSection(
    selectedType: StoryType,
    onTypeSelected: (StoryType) -> Unit,
    isPremium: Boolean,  // ✅ EKLE
    enabled: Boolean,
    isDarkTheme: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Hikaye Türü",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = if (isDarkTheme) Color.White else Color.Black
        )

        SelectableChipRow(
            options = StoryType.entries.toList(),
            selectedOption = selectedType,
            onOptionSelected = onTypeSelected,
            enabled = enabled,
            isPremiumRequired = { it != StoryType.STORY && !isPremium },  // ✅ Sadece STORY free
            isDarkTheme = isDarkTheme,
            optionText = { it.displayName },
            optionIcon = { it.icon }
        )

        // ✅ FREE USER UYARISI EKLE
        if (!isPremium) {
            Text(
                "🔒 Motivasyon, Diyalog ve Makale Premium üyelere özeldir",
                fontFamily = PoppinsFontFamily,
                fontSize = 11.sp,
                color = Color(0xFFFFB74D)
            )
        }
    }
}

@Composable
private fun DifficultySelectionSection(
    selectedDifficulty: StoryDifficulty,
    onDifficultySelected: (StoryDifficulty) -> Unit,
    isPremium: Boolean,
    enabled: Boolean,
    isDarkTheme: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Zorluk Seviyesi",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (isDarkTheme) Color.White else Color.Black
            )

            if (!isPremium) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Premium",
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        SelectableChipRow(
            options = StoryDifficulty.entries.toList(),
            selectedOption = selectedDifficulty,
            onOptionSelected = onDifficultySelected,
            enabled = enabled,
            isPremiumRequired = { it != StoryDifficulty.EASY && !isPremium },
            isDarkTheme = isDarkTheme,
            optionText = { it.displayName },
            optionIcon = { it.icon }
        )

        if (!isPremium) {
            Text(
                "🔒 Orta ve Zor seviye Premium üyelere özeldir",
                fontFamily = PoppinsFontFamily,
                fontSize = 11.sp,
                color = Color(0xFFFFB74D)
            )
        }
    }
}

@Composable
private fun LengthSelectionSection(
    selectedLength: StoryLength,
    onLengthSelected: (StoryLength) -> Unit,
    isPremium: Boolean,
    enabled: Boolean,
    isDarkTheme: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Hikaye Uzunluğu",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (isDarkTheme) Color.White else Color.Black
            )

            if (!isPremium) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Premium",
                    tint = Color(0xFFFFB74D),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        SelectableChipRow(
            options = StoryLength.entries.toList(),
            selectedOption = selectedLength,
            onOptionSelected = onLengthSelected,
            enabled = enabled,
            isPremiumRequired = { it != StoryLength.SHORT && !isPremium },
            isDarkTheme = isDarkTheme,
            optionText = { it.displayName },
            optionIcon = { it.icon }
        )

        if (!isPremium) {
            Text(
                "🔒 Orta ve Uzun hikaye Premium üyelere özeldir",
                fontFamily = PoppinsFontFamily,
                fontSize = 11.sp,
                color = Color(0xFFFFB74D)
            )
        }
    }
}

@Composable
private fun <T> SelectableChipRow(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    enabled: Boolean,
    isPremiumRequired: (T) -> Boolean,
    isDarkTheme: Boolean,
    optionText: (T) -> String,
    optionIcon: (T) -> String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            val needsPremium = isPremiumRequired(option)

            FilterChip(
                selected = isSelected,
                onClick = { if (enabled) onOptionSelected(option) },
                label = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(optionIcon(option), fontSize = 16.sp)
                        Text(
                            optionText(option),
                            fontFamily = PoppinsFontFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        if (needsPremium) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Premium",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFB74D)
                            )
                        }
                    }
                },
                enabled = enabled,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = if (isDarkTheme) Color(0xFF7986CB) else Color(0xFF667eea),
                    selectedLabelColor = Color.White,
                    containerColor = if (isDarkTheme) Color(0xFF2D2D2D) else Color(0xFFF5F5F5),
                    labelColor = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                    disabledContainerColor = if (isDarkTheme) Color(0xFF1A1A1A) else Color(0xFFEEEEEE),
                    disabledLabelColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.3f)
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = if (isDarkTheme) Color(0xFF404040) else Color(0xFFE0E0E0),
                    selectedBorderColor = if (isDarkTheme) Color(0xFF7986CB) else Color(0xFF667eea)
                )
            )
        }
    }
}

@Composable
private fun ErrorMessage(
    message: String,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFFFF5252).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = "Error",
            tint = Color(0xFFFF5252),
            modifier = Modifier.size(20.dp)
        )
        Text(
            message,
            fontFamily = PoppinsFontFamily,
            fontSize = 13.sp,
            color = if (isDarkTheme) Color.White else Color.Black,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GenerateButton(
    onClick: () -> Unit,
    enabled: Boolean,
    isLoading: Boolean,
    isDarkTheme: Boolean
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isDarkTheme) Color(0xFF7986CB) else Color(0xFF667eea),
            disabledContainerColor = if (isDarkTheme) Color(0xFF404040) else Color(0xFFE0E0E0)
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 3.dp
            )
            Spacer(Modifier.width(12.dp))
            Text(
                "Oluşturuluyor...",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        } else {
            Icon(Icons.Default.AutoAwesome, "Generate")
            Spacer(Modifier.width(8.dp))
            Text(
                "Hikaye Oluştur",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}