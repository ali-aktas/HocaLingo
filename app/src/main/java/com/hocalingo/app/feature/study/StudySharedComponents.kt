package com.hocalingo.app.feature.study.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.SwapHoriz
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
import com.hocalingo.app.R
import com.hocalingo.app.feature.profile.WordSummary

/**
 * StudySharedComponents - Shared components for Study feature
 *
 * Package: feature/study/components/
 *
 * Components used in StudyMainScreen:
 * - CompactSelectedWordsCard: Selected words preview with bottomsheet trigger
 * - CompactWordItem: Single word display (EN → TR)
 * - StudyDirectionCard: Study direction toggle card
 *
 * Note: These components were moved from ProfileScreen to StudyMainScreen
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * CompactSelectedWordsCard - Selected words preview card
 *
 * Displays first 5 selected words
 * "View All" button triggers bottomsheet with all words
 *
 * @param words First 5 selected words
 * @param totalCount Total number of selected words
 * @param onViewAllClick Callback to open bottomsheet
 * @param isDarkTheme Theme state for gradient colors
 */
@Composable
fun CompactSelectedWordsCard(
    words: List<WordSummary>,
    totalCount: Int,
    onViewAllClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                            listOf(Color(0xFF1A2980), Color(0xFF26D0CE))
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
                        text = "Seçili Kelimeler",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = "$totalCount kelime",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (words.isEmpty()) {
                    Text(
                        text = "Henüz kelime seçmediniz",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        words.take(5).forEach { word ->
                            CompactWordItem(
                                word = word,
                                isInBottomSheet = false
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = onViewAllClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = if (totalCount > 0) {
                                    "Tümünü Gör ($totalCount kelime)"
                                } else {
                                    "Kelime Seç"
                                },
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * CompactWordItem - Single word display item
 *
 * Shows English → Turkish translation
 * Adapts colors based on container:
 * - Gradient card (ProfileScreen/StudyMainScreen): White text on gradient
 * - BottomSheet: Theme colors on surface
 *
 * @param word Word data (EN/TR)
 * @param isInBottomSheet Display context (affects colors)
 */
@Composable
fun CompactWordItem(
    word: WordSummary,
    isInBottomSheet: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isInBottomSheet) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    Color.White.copy(alpha = 0.1f)
                },
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
            color = if (isInBottomSheet) {
                MaterialTheme.colorScheme.onSurface
            } else {
                Color.White
            },
            modifier = Modifier.weight(1f)
        )

        Text(
            text = "→",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = if (isInBottomSheet) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            } else {
                Color.White.copy(alpha = 0.6f)
            },
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Text(
            text = word.turkish,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = if (isInBottomSheet) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            } else {
                Color.White.copy(alpha = 0.9f)
            },
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

/**
 * StudyDirectionCard - Study direction toggle card
 *
 * Allows user to switch between:
 * - EN → TR (English to Turkish)
 * - TR → EN (Turkish to English)
 *
 * @param currentDirection Current study direction
 * @param onDirectionChange Callback when direction changes
 * @param isDarkTheme Theme state for gradient colors
 */
@Composable
fun StudyDirectionCard(
    currentDirection: com.hocalingo.app.core.common.StudyDirection,
    onDirectionChange: (com.hocalingo.app.core.common.StudyDirection) -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                .clickable {
                    val newDirection = when (currentDirection) {
                        com.hocalingo.app.core.common.StudyDirection.EN_TO_TR ->
                            com.hocalingo.app.core.common.StudyDirection.TR_TO_EN
                        com.hocalingo.app.core.common.StudyDirection.TR_TO_EN ->
                            com.hocalingo.app.core.common.StudyDirection.EN_TO_TR
                        com.hocalingo.app.core.common.StudyDirection.MIXED ->
                            com.hocalingo.app.core.common.StudyDirection.EN_TO_TR
                    }
                    onDirectionChange(newDirection)
                }
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Çalışma Yönü",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = when (currentDirection) {
                            com.hocalingo.app.core.common.StudyDirection.EN_TO_TR ->
                                "İngilizce → Türkçe"
                            com.hocalingo.app.core.common.StudyDirection.TR_TO_EN ->
                                "Türkçe → İngilizce"
                            com.hocalingo.app.core.common.StudyDirection.MIXED ->
                                "Karışık"
                        },
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.SwapHoriz,
                    contentDescription = "Çalışma yönünü değiştir",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}