package com.hocalingo.app.feature.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.HocaColors

/**
 * StudyMainComponents - Modern 3D UI Components
 *
 * Package: feature/study/
 * File: StudyMainComponents.kt
 *
 * Components:
 * - Modern3DButton: Reusable 3D button base
 * - StartStudyButton: DEPRECATED - Artık HeroCard'da PlayButton kullanılıyor
 * - AddWordButton: DEPRECATED - Artık StudyWideActionButton kullanılıyor
 * - StudyStatsCard: İstatistik kartı
 * - CompactActionButton: Küçük action button
 *
 * Note: Ana 3D butonlar StudySharedComponents'a taşındı
 * Bu dosya geriye uyumluluk için korunuyor
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

// =====================================================
// MODERN 3D BUTTON BASE
// =====================================================

/**
 * Modern3DButton - Reusable 3D button component
 *
 * Features:
 * - 3D depth effect with shadow
 * - Gradient background
 * - Press animation
 * - Icon + Text layout
 */
@Composable
fun Modern3DButton(
    text: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 80.dp,
    iconSize: Dp = 32.dp
) {
    var isPressed by remember { mutableStateOf(false) }

    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 8f,
        animationSpec = tween(durationMillis = 100),
        label = "elevation"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (isPressed) 4f else 0f,
        animationSpec = tween(durationMillis = 100),
        label = "offsetY"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .offset(y = offsetY.dp)
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(16.dp),
                clip = false
            )
            .clip(RoundedCornerShape(16.dp))
            .background(gradient)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = text,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
            }

            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

// =====================================================
// LEGACY BUTTONS (Geriye Uyumluluk)
// =====================================================

/**
 * StartStudyButton - Large "Start Study" button
 *
 * @deprecated Artık StudyMainScreen'de HeroCard + PlayButton kullanılıyor
 * Bu component geriye uyumluluk için korunuyor
 */
@Deprecated(
    message = "HeroCard içinde HocaPlayButton kullanılıyor",
    replaceWith = ReplaceWith("HocaPlayButton")
)
@Composable
fun StartStudyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Modern3DButton(
        text = "Çalışmaya Başla",
        icon = Icons.Filled.PlayArrow,
        gradient = Brush.horizontalGradient(
            colors = listOf(
                HocaColors.SuccessTop,
                Color(0xFF78D321)
            )
        ),
        onClick = onClick,
        modifier = modifier,
        height = 100.dp,
        iconSize = 48.dp
    )
}

/**
 * AddWordButton - "Add New Word" button
 *
 * @deprecated Artık StudyWideActionButton kullanılıyor
 */
@Deprecated(
    message = "StudyWideActionButton kullanın",
    replaceWith = ReplaceWith("StudyWideActionButton")
)
@Composable
fun AddWordButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Modern3DButton(
        text = "Yeni Kelime Ekle",
        icon = Icons.Filled.Add,
        gradient = Brush.horizontalGradient(
            colors = listOf(
                HocaColors.PurpleTop,
                HocaColors.PurpleLight
            )
        ),
        onClick = onClick,
        modifier = modifier,
        height = 70.dp,
        iconSize = 28.dp
    )
}

// =====================================================
// COMPACT ACTION BUTTON
// =====================================================

/**
 * CompactActionButton - Smaller action button
 *
 * For less prominent actions
 */
@Composable
fun CompactActionButton(
    text: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 1f else 4f,
        animationSpec = tween(durationMillis = 100),
        label = "elevation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(
                elevation = elevation.dp,
                shape = RoundedCornerShape(12.dp),
                clip = false
            )
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = text,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }

            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// =====================================================
// STUDY STATS CARD
// =====================================================

/**
 * StudyStatsCard - Quick stats display
 *
 * Shows study progress at a glance
 */
@Composable
fun StudyStatsCard(
    wordsCount: Int,
    studyDirection: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Words Count
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Book,
                    contentDescription = null,
                    tint = HocaColors.Orange,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "$wordsCount",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Seçili Kelime",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Divider
            HorizontalDivider(
                modifier = Modifier
                    .height(60.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Study Direction
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.SwapHoriz,
                    contentDescription = null,
                    tint = HocaColors.Orange,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = studyDirection,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Çalışma Yönü",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// =====================================================
// STUDY QUICK STATS (Yeni, Minimal)
// =====================================================

/**
 * StudyQuickStats - Minimal stats row
 *
 * Daha kompakt istatistik gösterimi
 */
@Composable
fun StudyQuickStats(
    wordsCount: Int,
    todayStudied: Int,
    streakDays: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickStatItem(
            value = wordsCount.toString(),
            label = "Toplam",
            color = HocaColors.Orange
        )

        QuickStatItem(
            value = todayStudied.toString(),
            label = "Bugün",
            color = HocaColors.SuccessTop
        )

        QuickStatItem(
            value = "$streakDays 🔥",
            label = "Seri",
            color = HocaColors.Orange
        )
    }
}

@Composable
private fun QuickStatItem(
    value: String,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = color
        )

        Text(
            text = label,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}