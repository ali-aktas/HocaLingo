package com.hocalingo.app.feature.selection

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
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

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * SwipeableCard - PHASE 2 COMPLETE
 *
 * ✅ Threshold reduced: 25% → 15% (Tinder-level comfort)
 * ✅ Animation duration: 300ms → 500ms (smooth completion)
 * ✅ FastOutSlowInEasing for natural feel
 * ✅ Velocity-based auto-complete (1000px/s threshold)
 * ✅ Cards exit at 180% screen width (fully visible until off-screen)
 * ✅ VelocityTracker for fast swipe detection
 * ✅ Smooth rotation synced with animation
 * ✅ Progressive visual feedback with indicators
 *
 * Package: feature/selection/
 */
@Composable
fun SwipeableCard(
    word: String,
    translation: String,
    example: String? = null,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier,
    nextWord: String? = null,
    nextTranslation: String? = null
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current

    // ✅ PHASE 2: Reduced threshold from 25% to 15%
    val swipeThreshold = with(density) { (screenWidth.toPx() * 0.15f) }

    // ✅ PHASE 2: Velocity threshold for auto-complete (fast swipe)
    val velocityThreshold = 1000f // px/sec

    val cardKey = "$word-$translation"
    val offsetX = remember(cardKey) { Animatable(0f) }
    val offsetY = remember(cardKey) { Animatable(0f) }
    val rotation = remember(cardKey) { Animatable(0f) }
    val scale = remember(cardKey) { Animatable(1f) }

    val scope = rememberCoroutineScope()

    var isDragging by remember(cardKey) { mutableStateOf(false) }
    var hasTriggeredAction by remember(cardKey) { mutableStateOf(false) }

    // ✅ PHASE 2: Velocity tracker for fast swipe detection
    val velocityTracker = remember(cardKey) { VelocityTracker() }

    // Progressive indicators
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

    val cardContentAlpha: Float by remember {
        derivedStateOf {
            if (hasTriggeredAction) {
                val screenWidthPx = with(density) { screenWidth.toPx() }
                val fadeProgress = (abs(offsetX.value) / (screenWidthPx * 0.3f)).coerceIn(0f, 1f)
                1f - fadeProgress
            } else {
                1f
            }
        }
    }

    // Reset on card change
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
        // Background card preview
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
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF90CAF9)
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = nextWord,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = nextTranslation,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 20.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

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
                        onDragStart = { offset ->
                            isDragging = true
                            hasTriggeredAction = false
                            velocityTracker.resetTracking()
                        },
                        onDragEnd = {
                            scope.launch {
                                isDragging = false

                                // ✅ PHASE 2: Calculate velocity
                                val velocity = velocityTracker.calculateVelocity()
                                val velocityX = velocity.x

                                when {
                                    // ✅ PHASE 2: Fast swipe right (velocity-based)
                                    velocityX > velocityThreshold && !hasTriggeredAction -> {
                                        hasTriggeredAction = true
                                        launch {
                                            // ✅ PHASE 2: Exit at 180% with 500ms smooth animation
                                            offsetX.animateTo(
                                                targetValue = size.width.toFloat() * 1.8f,
                                                animationSpec = tween(
                                                    durationMillis = 500,
                                                    easing = FastOutSlowInEasing
                                                )
                                            )
                                        }
                                        launch {
                                            rotation.animateTo(
                                                targetValue = 35f,
                                                animationSpec = tween(
                                                    durationMillis = 500,
                                                    easing = FastOutSlowInEasing
                                                )
                                            )
                                        }
                                        onSwipeRight()
                                    }

                                    // ✅ PHASE 2: Fast swipe left (velocity-based)
                                    velocityX < -velocityThreshold && !hasTriggeredAction -> {
                                        hasTriggeredAction = true
                                        launch {
                                            offsetX.animateTo(
                                                targetValue = -size.width.toFloat() * 1.8f,
                                                animationSpec = tween(
                                                    durationMillis = 500,
                                                    easing = FastOutSlowInEasing
                                                )
                                            )
                                        }
                                        launch {
                                            rotation.animateTo(
                                                targetValue = -35f,
                                                animationSpec = tween(
                                                    durationMillis = 500,
                                                    easing = FastOutSlowInEasing
                                                )
                                            )
                                        }
                                        onSwipeLeft()
                                    }

                                    // ✅ PHASE 2: Threshold-based right swipe (15%)
                                    offsetX.value > swipeThreshold && !hasTriggeredAction -> {
                                        hasTriggeredAction = true
                                        launch {
                                            offsetX.animateTo(
                                                targetValue = size.width.toFloat() * 1.8f,
                                                animationSpec = tween(
                                                    durationMillis = 500,
                                                    easing = FastOutSlowInEasing
                                                )
                                            )
                                        }
                                        launch {
                                            rotation.animateTo(
                                                targetValue = 35f,
                                                animationSpec = tween(
                                                    durationMillis = 500,
                                                    easing = FastOutSlowInEasing
                                                )
                                            )
                                        }
                                        onSwipeRight()
                                    }

                                    // ✅ PHASE 2: Threshold-based left swipe (15%)
                                    offsetX.value < -swipeThreshold && !hasTriggeredAction -> {
                                        hasTriggeredAction = true
                                        launch {
                                            offsetX.animateTo(
                                                targetValue = -size.width.toFloat() * 1.8f,
                                                animationSpec = tween(
                                                    durationMillis = 500,
                                                    easing = FastOutSlowInEasing
                                                )
                                            )
                                        }
                                        launch {
                                            rotation.animateTo(
                                                targetValue = -35f,
                                                animationSpec = tween(
                                                    durationMillis = 500,
                                                    easing = FastOutSlowInEasing
                                                )
                                            )
                                        }
                                        onSwipeLeft()
                                    }

                                    else -> {
                                        // Return to center with spring
                                        launch {
                                            offsetX.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                        launch {
                                            offsetY.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                        launch {
                                            rotation.animateTo(
                                                0f,
                                                spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            // ✅ PHASE 2: Update velocity tracker
                            velocityTracker.addPosition(
                                change.uptimeMillis,
                                change.position
                            )

                            if (!hasTriggeredAction) {
                                scope.launch {
                                    // Update position
                                    offsetX.snapTo(offsetX.value + dragAmount.x)
                                    offsetY.snapTo(offsetY.value + dragAmount.y * 0.3f)

                                    // ✅ PHASE 2: Smooth rotation based on offset
                                    val targetRotation = (offsetX.value / swipeThreshold * 15f).coerceIn(-15f, 15f)
                                    rotation.snapTo(targetRotation)
                                }
                            }
                        }
                    )
                },
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF64B5F6)
            ),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isDragging) 20.dp else 16.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(cardContentAlpha), // ✅ EKLENEN SATIR
                contentAlignment = Alignment.Center
            ) {
                // Left indicator (Geç)
                Text(
                    text = "Geç",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp,
                    color = Color(0xFFEF5350),
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 32.dp)
                        .alpha(leftIndicatorAlpha)
                        .graphicsLayer {
                            rotationZ = -15f
                        }
                )

                // Right indicator (Öğren)
                Text(
                    text = "Öğren",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp,
                    color = Color(0xFF66BB6A),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 32.dp)
                        .alpha(rightIndicatorAlpha)
                        .graphicsLayer {
                            rotationZ = 15f
                        }
                )

                // Word content
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = word,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = translation,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 24.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )

                    if (!example.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = example,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}