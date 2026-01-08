package com.hocalingo.app.feature.study.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.HocaColors
import com.hocalingo.app.feature.profile.WordSummary

/**
 * StudySharedComponents - Shared UI components for StudyMainScreen
 *
 * Package: feature/study/components/
 * File: StudySharedComponents.kt
 *
 * Components:
 * - StudyDirectionIndicator: Modern yön göstergesi (EN ↔ TR)
 * - SelectedWordsCard: Seçili kelimeler önizleme kartı
 * - StudyWideActionButton: 3D action button (HomeScreen WideActionButton gibi)
 * - StudyWordsBottomSheet: Tüm kelimeler bottom sheet
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

// =====================================================
// DIRECTION INDICATOR
// =====================================================

/**
 * StudyDirectionIndicator - Modern yön göstergesi
 *
 * EN ↔ TR geçişini gösteren sade kart
 * Tıklanabilir, yön değiştirme tetikler
 */
@Composable
fun StudyDirectionIndicator(
    isEnglishToTurkish: Boolean,
    onToggle: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val fromText = if (isEnglishToTurkish) "EN" else "TR"
    val toText = if (isEnglishToTurkish) "TR" else "EN"
    val fromFlag = if (isEnglishToTurkish) "🇬🇧" else "🇹🇷"
    val toFlag = if (isEnglishToTurkish) "🇹🇷" else "🇬🇧"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme)
                Color(0xFF2D2438)
            else
                Color(0xFFF8F7FC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // From Language
            DirectionLanguageChip(
                flag = fromFlag,
                code = fromText,
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Arrow Indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = HocaColors.Orange.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = "Yön Değiştir",
                    tint = HocaColors.Orange,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // To Language
            DirectionLanguageChip(
                flag = toFlag,
                code = toText,
                isDarkTheme = isDarkTheme
            )
        }
    }
}

@Composable
private fun DirectionLanguageChip(
    flag: String,
    code: String,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .background(
                color = if (isDarkTheme)
                    Color.White.copy(alpha = 0.1f)
                else
                    Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = flag,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = code,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = if (isDarkTheme)
                Color.White
            else
                Color(0xFF1C1C1E)
        )
    }
}

// =====================================================
// SELECTED WORDS CARD
// =====================================================

/**
 * SelectedWordsCard - Sade, okunabilir kelime listesi
 *
 * İlk 5 kelimeyi gösterir
 * "Tümünü Gör" butonu ile BottomSheet açılır
 */
@Composable
fun SelectedWordsCard(
    words: List<WordSummary>,
    totalCount: Int,
    onViewAllClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme)
                Color(0xFF211A2E)
            else
                Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Seçili Kelimeler",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "$totalCount kelime",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Tümünü Gör Button
                TextButton(
                    onClick = onViewAllClick,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = HocaColors.Orange
                    )
                ) {
                    Text(
                        text = "Tümünü Gör",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Words List
            if (words.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📚",
                            fontSize = 32.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Henüz kelime seçilmedi",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Word Items
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    words.take(5).forEach { word ->
                        WordPreviewItem(
                            word = word,
                            isDarkTheme = isDarkTheme
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WordPreviewItem(
    word: WordSummary,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDarkTheme)
                    Color.White.copy(alpha = 0.05f)
                else
                    Color(0xFFF8F7FC),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // English
        Text(
            text = word.english,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        // Arrow
        Text(
            text = "→",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = HocaColors.Orange,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Turkish
        Text(
            text = word.turkish,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

// =====================================================
// 3D WIDE ACTION BUTTON
// =====================================================

/**
 * StudyWideActionButton - HomeScreen WideActionButton ile aynı stil
 *
 * 3D depth effect ile dikdörtgen action button
 * Icon + Title + Subtitle yapısı
 */
@Composable
fun StudyWideActionButton(
    onClick: () -> Unit,
    icon: Painter,
    title: String,
    subtitle: String,
    baseColor: Color,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    // Animations
    val pressDepth by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "press_depth"
    )

    val topColorBrightness by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "color_brightness"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) {
                            onClick()
                        }
                    }
                )
            }
    ) {
        // Shadow Layer
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .offset(y = 8.dp)
        ) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    radius = this.size.width / 2
                ),
                size = this.size,
                cornerRadius = CornerRadius(20.dp.toPx())
            )
        }

        // Main Button Surface
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .offset(y = pressDepth)
        ) {
            val lightColor = Color(
                red = (baseColor.red * topColorBrightness).coerceIn(0f, 1f),
                green = (baseColor.green * topColorBrightness).coerceIn(0f, 1f),
                blue = (baseColor.blue * topColorBrightness).coerceIn(0f, 1f)
            )

            val shadowColor = Color(
                red = (baseColor.red * 0.75f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.75f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.75f).coerceIn(0f, 1f)
            )

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(lightColor, shadowColor)
                ),
                size = this.size,
                cornerRadius = CornerRadius(20.dp.toPx())
            )
        }

        // Content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .offset(y = pressDepth)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Icon
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit,
                alpha = 0.95f
            )

            Spacer(modifier = Modifier.width(20.dp))

            // Text
            Column {
                Text(
                    text = title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}

// =====================================================
// WORDS BOTTOM SHEET
// =====================================================

/**
 * StudyWordsBottomSheet - Tüm kelimeleri gösteren modern bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyWordsBottomSheet(
    words: List<WordSummary>,
    isLoading: Boolean,
    canLoadMore: Boolean,
    onLoadMore: () -> Unit,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = if (isDarkTheme)
            Color(0xFF1A1625)
        else
            Color.White,
        dragHandle = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = if (isDarkTheme)
                                Color.White.copy(alpha = 0.3f)
                            else
                                Color(0xFFE0E0E0),
                            shape = RoundedCornerShape(2.dp)
                        )
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tüm Kelimeler",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = HocaColors.Orange.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${words.size} kelime",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = HocaColors.Orange,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Words List
            if (words.isEmpty() && !isLoading) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "📚",
                            fontSize = 48.sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Henüz kelime eklenmedi",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Yeni kelimeler ekleyerek başlayın",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(
                        items = words,
                        key = { it.id }
                    ) { word ->
                        BottomSheetWordItem(
                            word = word,
                            isDarkTheme = isDarkTheme
                        )
                    }

                    // Loading indicator
                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    color = HocaColors.Orange,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }

                    // Load more trigger
                    if (canLoadMore && !isLoading && words.isNotEmpty()) {
                        item {
                            LaunchedEffect(Unit) {
                                onLoadMore()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomSheetWordItem(
    word: WordSummary,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme)
                Color.White.copy(alpha = 0.08f)
            else
                Color(0xFFF8F7FC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // English Word
            Text(
                text = word.english,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Divider Arrow
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(28.dp)
                    .background(
                        color = HocaColors.Orange.copy(alpha = 0.12f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "→",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = HocaColors.Orange
                )
            }

            // Turkish Word
            Text(
                text = word.turkish,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End
            )
        }
    }
}

// =====================================================
// LEGACY COMPATIBILITY (Eski componentler için)
// =====================================================

/**
 * CompactSelectedWordsCard - Eski isim, SelectedWordsCard'a yönlendirir
 * @deprecated Use SelectedWordsCard instead
 */
@Composable
fun CompactSelectedWordsCard(
    words: List<WordSummary>,
    totalCount: Int,
    onViewAllClick: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    SelectedWordsCard(
        words = words,
        totalCount = totalCount,
        onViewAllClick = onViewAllClick,
        isDarkTheme = isDarkTheme,
        modifier = modifier
    )
}

/**
 * CompactWordItem - Eski component, artık internal
 * @deprecated Use WordPreviewItem instead (internal)
 */
@Composable
fun CompactWordItem(
    word: WordSummary,
    isInBottomSheet: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Eski API için uyumluluk
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f

    if (isInBottomSheet) {
        BottomSheetWordItem(word = word, isDarkTheme = isDarkTheme)
    } else {
        WordPreviewItem(word = word, isDarkTheme = isDarkTheme)
    }
}

/**
 * StudyDirectionCard - Eski isim, StudyDirectionIndicator'a yönlendirir
 * @deprecated Use StudyDirectionIndicator instead
 */
@Composable
fun StudyDirectionCard(
    currentDirection: String,
    onDirectionChange: () -> Unit,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    StudyDirectionIndicator(
        isEnglishToTurkish = currentDirection.contains("İngilizce"),
        onToggle = onDirectionChange,
        isDarkTheme = isDarkTheme,
        modifier = modifier
    )
}