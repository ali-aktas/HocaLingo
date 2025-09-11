package com.hocalingo.app.feature.selection.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * SwipeableCard - PREMIUM VERSION
 * Natural movement, next card preview, emojis, glow effects
 */
@SuppressLint("UnrememberedMutableState")
@Composable
fun SwipeableCard(
    word: String,
    translation: String,
    example: String? = null,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    nextWord: String? = null, // Arkadaki kartın kelimesi
    nextTranslation: String? = null // Arkadaki kartın çevirisi
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current

    // Swipe threshold
    val swipeThreshold = with(density) { (screenWidth.toPx() * 0.25f) }

    // Animation states
    val cardKey = "$word-$translation"
    val offsetX = remember(cardKey) { Animatable(0f) }
    val offsetY = remember(cardKey) { Animatable(0f) }
    val rotation = remember(cardKey) { Animatable(0f) }
    val scale = remember(cardKey) { Animatable(1f) }

    val scope = rememberCoroutineScope()

    var isDragging by remember(cardKey) { mutableStateOf(false) }
    var hasTriggeredAction by remember(cardKey) { mutableStateOf(false) }

    // Swipe indicators - emojis and alpha (TİP HATASI DÜZELTİLDİ - AÇIK TİP BELİRTİLDİ)
    val leftIndicatorAlpha by derivedStateOf<Float> {
        if (offsetX.value < 0) {
            (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
        } else 0f
    }

    val rightIndicatorAlpha by derivedStateOf<Float> {
        if (offsetX.value > 0) {
            (offsetX.value / swipeThreshold).coerceIn(0f, 1f)
        } else 0f
    }

    val cardScale by derivedStateOf<Float> {
        with(density) {
            1f - (abs(offsetX.value) / (screenWidth.toPx() * 2)) * 0.1f
        }
    }

    // Reset animation states
    LaunchedEffect(cardKey) {
        offsetX.snapTo(0f)
        offsetY.snapTo(0f)
        rotation.snapTo(0f)
        scale.snapTo(1f)
        isDragging = false
        hasTriggeredAction = false
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(420.dp)
    ) {

        // BACKGROUND CARD (Next Card Preview)
        if (nextWord != null && nextTranslation != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 28.dp, vertical = 20.dp)
                    .alpha(0.6f)
                    .scale(0.95f),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = nextWord,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = nextTranslation,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // MAIN CARD with glow effect
        Box {
            // Purple glow effect behind card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .blur(12.dp)
                    .alpha(0.3f),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF9C27B0) // Purple glow
                ),
                shape = RoundedCornerShape(24.dp)
            ) {}

            // Main card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .offset {
                        IntOffset(
                            offsetX.value.roundToInt(),
                            offsetY.value.roundToInt()
                        )
                    }
                    .graphicsLayer {
                        rotationZ = rotation.value
                        scaleX = scale.value * cardScale
                        scaleY = scale.value * cardScale
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .pointerInput(cardKey) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                hasTriggeredAction = false
                            },
                            onDragEnd = {
                                scope.launch {
                                    val threshold = swipeThreshold

                                    if (!hasTriggeredAction) {
                                        when {
                                            offsetX.value > threshold -> {
                                                // Swipe right - Accept
                                                hasTriggeredAction = true

                                                launch {
                                                    offsetX.animateTo(
                                                        targetValue = screenWidth.toPx() * 1.5f,
                                                        animationSpec = tween(400)
                                                    )
                                                }
                                                launch {
                                                    rotation.animateTo(
                                                        targetValue = 20f,
                                                        animationSpec = tween(400)
                                                    )
                                                }
                                                launch {
                                                    scale.animateTo(
                                                        targetValue = 0.8f,
                                                        animationSpec = tween(400)
                                                    )
                                                }

                                                kotlinx.coroutines.delay(200)
                                                onSwipeRight()
                                            }
                                            offsetX.value < -threshold -> {
                                                // Swipe left - Reject
                                                hasTriggeredAction = true

                                                launch {
                                                    offsetX.animateTo(
                                                        targetValue = -screenWidth.toPx() * 1.5f,
                                                        animationSpec = tween(400)
                                                    )
                                                }
                                                launch {
                                                    rotation.animateTo(
                                                        targetValue = -20f,
                                                        animationSpec = tween(400)
                                                    )
                                                }
                                                launch {
                                                    scale.animateTo(
                                                        targetValue = 0.8f,
                                                        animationSpec = tween(400)
                                                    )
                                                }

                                                kotlinx.coroutines.delay(200)
                                                onSwipeLeft()
                                            }
                                            else -> {
                                                // Spring back to center
                                                launch {
                                                    offsetX.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring(
                                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                                            stiffness = Spring.StiffnessMedium
                                                        )
                                                    )
                                                }
                                                launch {
                                                    offsetY.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring()
                                                    )
                                                }
                                                launch {
                                                    rotation.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring()
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    isDragging = false
                                }
                            },
                            onDrag = { _, dragAmount ->
                                // DÜZELTME: Natural movement - elimle attığım yöne git
                                if (isDragging && !hasTriggeredAction) {
                                    scope.launch {
                                        offsetX.snapTo(offsetX.value + dragAmount.x)
                                        // Y hareketi de doğal olsun
                                        offsetY.snapTo(offsetY.value + dragAmount.y * 0.6f)

                                        // Rotation based on X offset
                                        val rotationFraction = (offsetX.value / screenWidth.toPx()).coerceIn(-1f, 1f)
                                        rotation.snapTo(rotationFraction * 12f)
                                    }
                                }
                            }
                        )
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // EMOJI OVERLAYS
                    if (rightIndicatorAlpha > 0.3f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .alpha(rightIndicatorAlpha)
                                .background(
                                    Color.Green.copy(alpha = 0.2f),
                                    RoundedCornerShape(50.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "✅",
                                fontSize = 48.sp
                            )
                        }
                    }

                    if (leftIndicatorAlpha > 0.3f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .alpha(leftIndicatorAlpha)
                                .background(
                                    Color.Red.copy(alpha = 0.2f),
                                    RoundedCornerShape(50.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "❌",
                                fontSize = 48.sp
                            )
                        }
                    }

                    // Card content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // English word
                        Text(
                            text = word,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Turkish translation
                        Text(
                            text = translation,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        // Example sentence
                        example?.let {
                            Spacer(modifier = Modifier.height(32.dp))

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(20.dp),
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }

                    // Bottom hint with gradient
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                    )
                                )
                            )
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = "← Geç   |   Öğren →",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}