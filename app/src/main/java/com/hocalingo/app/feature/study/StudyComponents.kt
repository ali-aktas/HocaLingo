package com.hocalingo.app.feature.study

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.google.android.gms.ads.nativead.NativeAd
import com.hocalingo.app.R
import com.hocalingo.app.core.ads.NativeAdCard
import kotlin.math.absoluteValue

/**
 * StudyComponents - Reusable UI Components
 *
 * Package: feature/study/
 *
 * Components:
 * - StudyCard: Flip animation card with TTS
 * - StudyActionButtons: Easy/Medium/Hard response buttons
 * - StudyProgressIndicator: Progress bar with counter
 * - StudyTopBar: Navigation bar
 * - StudyNativeAdOverlay: Native ad display
 * - LoadingState: Loading indicator
 * - ErrorState: Error handling UI
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

// Modern card colors (20 vibrant colors, excluding white/gray/yellow)
private val cardColors = listOf(
    Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFEF4444),
    Color(0xFFF97316), Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF3B82F6),
    Color(0xFF8B5A2B), Color(0xFF059669), Color(0xFF7C3AED), Color(0xFFDC2626),
    Color(0xFF0891B2), Color(0xFF065F46), Color(0xFF7C2D12), Color(0xFF1E40AF),
    Color(0xFF7E22CE), Color(0xFF0F766E), Color(0xFFA21CAF), Color(0xFF9A3412)
)

/**
 * StudyTopBar - Navigation bar with back button and title
 */
@Composable
fun StudyTopBar(
    onBackClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Geri",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Akıllı Kelime Çalışması",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

/**
 * StudyProgressIndicator - Shows current word position and progress
 *
 * @param currentIndex Current word index (0-based)
 * @param totalWords Total words in queue
 * @param progress Progress percentage (0.0 - 1.0)
 */
@Composable
fun StudyProgressIndicator(
    currentIndex: Int,
    totalWords: Int,
    progress: Float
) {
    if (totalWords > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentIndex + 1}/$totalWords",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF4ECDC4),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/**
 * StudyCard - Flip animation study card with TTS button
 *
 * Features:
 * - 3D flip animation
 * - Front/Back text display
 * - Example sentences
 * - Text-to-Speech button
 * - Dynamic card colors
 *
 * @param isFlipped Card flip state
 * @param showTtsOnFrontSide Show TTS button on front or back
 */
@Composable
fun StudyCard(
    frontText: String,
    backText: String,
    frontExampleText: String,
    backExampleText: String,
    isFlipped: Boolean,
    onCardClick: () -> Unit,
    onPronunciationClick: () -> Unit,
    showTtsOnFrontSide: Boolean,
    showPronunciationButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardColor = remember(frontText, backText) {
        val hash = (frontText + backText).hashCode().absoluteValue
        cardColors[hash % cardColors.size]
    }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = 280f
        ),
        label = "cardRotation"
    )

    // 🔥 Dynamic elevation (tablet hissinin %30’u burada)
    val dynamicElevation by remember {
        derivedStateOf<Dp> {
            val normalized = kotlin.math.abs(rotation - 90f) / 90f
            lerp(4.dp, 18.dp, normalized)
        }
    }


    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
    ) {

        // 🔻 ALT KATMAN → SAHTE KALINLIK / GÖVDE
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 8.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 32f * density
                }
                .background(
                    color = Color.Black.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(28.dp)
                )
        )

        // 🔺 ÜST KATMAN → ASIL KART
        Card(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 32f * density
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onCardClick() },
            shape = RoundedCornerShape(28.dp),
            elevation = CardDefaults.cardElevation(dynamicElevation)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                cardColor.copy(alpha = 1f),
                                cardColor.copy(alpha = 0.75f)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {

                if (rotation <= 90f) {
                    // FRONT
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = frontText,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        if (frontExampleText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = frontExampleText,
                                fontFamily = PoppinsFontFamily,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    if (showPronunciationButton && showTtsOnFrontSide) {
                        IconButton(
                            onClick = onPronunciationClick,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolumeUp,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }

                } else {
                    // BACK
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { rotationY = 180f },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = backText,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        if (backExampleText.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = backExampleText,
                                fontFamily = PoppinsFontFamily,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }

                    if (showPronunciationButton && !showTtsOnFrontSide) {
                        IconButton(
                            onClick = onPronunciationClick,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .graphicsLayer { scaleX = -1f }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VolumeUp,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}


/**
 * StudyActionButtons - Easy/Medium/Hard response buttons
 *
 * Shows tap instruction when card is not flipped
 * Shows difficulty buttons when card is flipped
 */
@Composable
fun StudyActionButtons(
    isCardFlipped: Boolean,
    easyTimeText: String,
    mediumTimeText: String,
    hardTimeText: String,
    onHardPressed: () -> Unit,
    onMediumPressed: () -> Unit,
    onEasyPressed: () -> Unit
) {
    if (!isCardFlipped) {
        // Tap instruction card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ne kadar hatırlıyorsun?",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Difficulty buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StudyButton(
                mainText = "Zor",
                timeText = hardTimeText,
                backgroundColor = Color(0xFFFF3B30),
                contentColor = Color.White,
                onClick = onHardPressed,
                modifier = Modifier.weight(1f)
            )

            StudyButton(
                mainText = "Orta",
                timeText = mediumTimeText,
                backgroundColor = Color(0xFFFF9500),
                contentColor = Color.White,
                onClick = onMediumPressed,
                modifier = Modifier.weight(1f)
            )

            StudyButton(
                mainText = "Kolay",
                timeText = easyTimeText,
                backgroundColor = Color(0xFF34C759),
                contentColor = Color.White,
                onClick = onEasyPressed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * StudyButton - 3D Style difficulty button
 *
 * Duolingo-inspired tactile button with press animation
 *
 * @param mainText Button label (Kolay/Orta/Zor)
 * @param timeText Next review time
 * @param backgroundColor Button base color
 */
@Composable
private fun StudyButton(
    mainText: String,
    timeText: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
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

    // Calculate shadow color (darker version of base color)
    val shadowColor = Color(
        red = (backgroundColor.red * 0.7f).coerceIn(0f, 1f),
        green = (backgroundColor.green * 0.7f).coerceIn(0f, 1f),
        blue = (backgroundColor.blue * 0.7f).coerceIn(0f, 1f)
    )

    Box(
        modifier = modifier
            .height(86.dp)
            .pointerInput(enabled) {
                if (enabled) {
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
            }
    ) {
        // Bottom Layer (Shadow/Depth) - Static
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(16.dp))
                .background(shadowColor)
        )

        // Top Layer (Interactive Surface)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .offset(y = pressDepth)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Color(
                        red = (backgroundColor.red * topColorBrightness).coerceIn(0f, 1f),
                        green = (backgroundColor.green * topColorBrightness).coerceIn(0f, 1f),
                        blue = (backgroundColor.blue * topColorBrightness).coerceIn(0f, 1f)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = mainText,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = contentColor
                )

                if (timeText.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeText,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = contentColor.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}



/**
 * StudyNativeAdOverlay - Native ad display overlay
 *
 * Positioned over the study card when showing ads
 */
@Composable
fun StudyNativeAdOverlay(
    nativeAd: NativeAd?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (nativeAd == null) return

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.7f),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            NativeAdCard(
                nativeAd = nativeAd,
                modifier = Modifier.fillMaxSize()
            )

            // Close button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
                    .size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kapat",
                    tint = Color.White
                )
            }
        }
    }
}

/**
 * LoadingState - Loading indicator with message
 */
@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF4ECDC4))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Çalışma hazırlanıyor...",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * ErrorState - Error display with retry button
 *
 * @param error Error message to display
 * @param onRetry Retry action callback
 */
@Composable
fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "⚠️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Tekrar Dene")
        }
    }
}