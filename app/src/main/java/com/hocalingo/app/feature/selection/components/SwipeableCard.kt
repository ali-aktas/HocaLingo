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
import androidx.compose.ui.draw.rotate
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

@Composable
fun SwipeableCard(
    word: String,
    translation: String,
    example: String? = null,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current

    // Swipe threshold (Tinder benzeri - ekranın %30'u)
    val swipeThreshold = with(density) { (screenWidth.toPx() * 0.3f) }

    // Animation states
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    val scope = rememberCoroutineScope()

    // Swipe indicator visibility
    val leftIndicatorAlpha by remember {
        derivedStateOf {
            if (offsetX.value < 0) {
                (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
            } else 0f
        }
    }

    val rightIndicatorAlpha by remember {
        derivedStateOf {
            if (offsetX.value > 0) {
                (offsetX.value / swipeThreshold).coerceIn(0f, 1f)
            } else 0f
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
    ) {
        // Swipe indicators background
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left indicator (Pas)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .alpha(leftIndicatorAlpha * 0.3f)
                    .background(
                        color = Color.Red.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Right indicator (Öğren)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .alpha(rightIndicatorAlpha * 0.3f)
                    .background(
                        color = Color.Green.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }

        // Main card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .graphicsLayer {
                    rotationZ = rotation.value
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            scope.launch {
                                val threshold = swipeThreshold

                                when {
                                    offsetX.value > threshold -> {
                                        // Swipe right - Öğren
                                        launch {
                                            offsetX.animateTo(
                                                targetValue = screenWidth.toPx(),
                                                animationSpec = tween(200)
                                            )
                                        }
                                        launch {
                                            rotation.animateTo(
                                                targetValue = 15f,
                                                animationSpec = tween(200)
                                            )
                                        }
                                        onSwipeRight()
                                    }
                                    offsetX.value < -threshold -> {
                                        // Swipe left - Pas
                                        launch {
                                            offsetX.animateTo(
                                                targetValue = -screenWidth.toPx(),
                                                animationSpec = tween(200)
                                            )
                                        }
                                        launch {
                                            rotation.animateTo(
                                                targetValue = -15f,
                                                animationSpec = tween(200)
                                            )
                                        }
                                        onSwipeLeft()
                                    }
                                    else -> {
                                        // Spring back to center
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
                        },
                        onDrag = { _, dragAmount ->
                            scope.launch {
                                offsetX.snapTo(offsetX.value + dragAmount.x)
                                offsetY.snapTo(offsetY.value + dragAmount.y * 0.3f) // Y ekseni daha az hassas

                                // Rotation based on X offset
                                val rotationFraction = (offsetX.value / screenWidth.toPx()).coerceIn(-1f, 1f)
                                rotation.snapTo(rotationFraction * 10f)
                            }
                        }
                    )
                },
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Swipe hints overlay
                if (rightIndicatorAlpha > 0.5f) {
                    Text(
                        text = "ÖĞREN",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .alpha(rightIndicatorAlpha),
                        color = Color.Green,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (leftIndicatorAlpha > 0.5f) {
                    Text(
                        text = "PAS",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .alpha(leftIndicatorAlpha),
                        color = Color.Red,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Card content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
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
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }

                // Bottom hint
                Text(
                    text = "← Pas   |   Öğren →",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}