package com.hocalingo.app.feature.selection.components

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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocalingo.app.R
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * Premium SwipeableCard with Tinder-style animations
 * Features: Natural movement, background card preview, visual feedback
 */
@Composable
fun SwipeableCard(
    word: String,
    translation: String,
    example: String? = null,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    nextWord: String? = null, // Background card preview
    nextTranslation: String? = null // Background card preview
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

    // Swipe indicators - emojis and alpha
    val leftIndicatorAlpha: Float by remember {
        derivedStateOf {
            if (offsetX.value < 0) {
                (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
            } else 0f
        }
    }

    val rightIndicatorAlpha: Float by remember {
        derivedStateOf {
            if (offsetX.value > 0) {
                (offsetX.value / swipeThreshold).coerceIn(0f, 1f)
            } else 0f
        }
    }

    val cardScale: Float by remember {
        derivedStateOf {
            with(density) {
                1f - (abs(offsetX.value) / (screenWidth.toPx() * 2)) * 0.1f
            }
        }
    }

    // Reset animation states when card changes
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
                    .graphicsLayer {
                        scaleX = 0.95f
                        scaleY = 0.95f
                    },
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
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = nextTranslation,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // MAIN CARD with enhanced design
        Box {
            // Glow effect behind card
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
                        shape = RoundedCornerShape(24.dp)
                    )
                    .pointerInput(cardKey) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                hasTriggeredAction = false
                            },
                            onDragEnd = {
                                scope.launch {
                                    isDragging = false

                                    when {
                                        offsetX.value > swipeThreshold && !hasTriggeredAction -> {
                                            // Swipe right - LEARN
                                            hasTriggeredAction = true

                                            launch {
                                                offsetX.animateTo(
                                                    targetValue = size.width.toFloat() + 200f,
                                                    animationSpec = tween(300)
                                                )
                                            }
                                            launch {
                                                rotation.animateTo(
                                                    targetValue = 30f,
                                                    animationSpec = tween(300)
                                                )
                                            }

                                            onSwipeRight()
                                        }
                                        offsetX.value < -swipeThreshold && !hasTriggeredAction -> {
                                            // Swipe left - SKIP
                                            hasTriggeredAction = true

                                            launch {
                                                offsetX.animateTo(
                                                    targetValue = -size.width.toFloat() - 200f,
                                                    animationSpec = tween(300)
                                                )
                                            }
                                            launch {
                                                rotation.animateTo(
                                                    targetValue = -30f,
                                                    animationSpec = tween(300)
                                                )
                                            }

                                            onSwipeLeft()
                                        }
                                        else -> {
                                            // Return to center with spring animation
                                            launch {
                                                offsetX.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                                        stiffness = Spring.StiffnessLow
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
                            }
                        ) { change, dragAmount ->
                            if (!hasTriggeredAction) {
                                scope.launch {
                                    // Update position
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo(offsetY.value + dragAmount.y * 0.3f) // Damped Y movement

                                    // Natural rotation based on X movement
                                    val rotationAmount = (offsetX.value / swipeThreshold * 15f).coerceIn(-15f, 15f)
                                    rotation.snapTo(rotationAmount)
                                }
                            }
                        }
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 20.dp else 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = RoundedCornerShape(24.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Card content
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Main word
                        Text(
                            text = word,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            color = Color.Black,
                            textAlign = TextAlign.Center,
                            lineHeight = 40.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Translation
                        Text(
                            text = translation,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 18.sp,
                            color = Color(0xFF666666),
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )

                        // Example sentence (if provided)
                        example?.let { exampleText ->
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "\"$exampleText\"",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = Color(0xFF888888),
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp,
                                modifier = Modifier
                                    .background(
                                        color = Color(0xFFF5F5F5),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(16.dp)
                            )
                        }
                    }

                    // LEFT SWIPE INDICATOR (❌ SKIP)
                    if (leftIndicatorAlpha > 0.1f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(24.dp)
                                .background(
                                    color = Color(0xFFFF4444).copy(alpha = leftIndicatorAlpha * 0.9f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "❌ SKIP",
                                color = Color.White,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // RIGHT SWIPE INDICATOR (⭐ LEARN)
                    if (rightIndicatorAlpha > 0.1f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(24.dp)
                                .background(
                                    color = Color(0xFF4CAF50).copy(alpha = rightIndicatorAlpha * 0.9f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "⭐ LEARN",
                                color = Color.White,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}