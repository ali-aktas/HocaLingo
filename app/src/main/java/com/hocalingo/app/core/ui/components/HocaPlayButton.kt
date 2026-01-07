package com.hocalingo.app.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * GERÇEK 3D PLAY BUTTON
 *
 * Tasarım Felsefesi:
 * - Fiziksel düğme hissi (Duolingo-style)
 * - Işık kaynağı üstten geliyor
 * - Basınca üst yüzey kararıyor + derine giriyor
 * - Minimal scale, maksimum tactile feedback
 *
 * Layer Yapısı:
 * 1. Bottom shadow (sabit, koyu)
 * 2. Middle depth (gradient, derinlik hissi)
 * 3. Top surface (ışıklı, basınca kararan)
 * 4. Icon (en üst, hafif offset)
 */
@Composable
fun HocaPlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    baseColor: Color = Color(0xFFFF6B35), // Turuncu
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }

    // Animations
    val pressDepth by animateDpAsState(
        targetValue = if (isPressed) 6.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "press_depth"
    )

    val topColorBrightness by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f, // Basınca karart
        animationSpec = tween(durationMillis = 100),
        label = "color_brightness"
    )

    val shadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.4f else 0.3f,
        animationSpec = tween(durationMillis = 100),
        label = "shadow_alpha"
    )

    Box(
        modifier = modifier
            .size(size + 12.dp) // Extra space for shadow
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
            },
        contentAlignment = Alignment.Center
    ) {
        // Layer 1: Bottom Shadow (en derin, sabit)
        Canvas(
            modifier = Modifier
                .size(size)
                .offset(y = 12.dp)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = shadowAlpha),
                        Color.Transparent
                    ),
                    radius = this.size.minDimension / 2
                ),
                radius = this.size.minDimension / 2
            )
        }

        // Layer 2: Depth Ring (gradient derinlik)
        Canvas(
            modifier = Modifier
                .size(size)
                .offset(y = pressDepth + 4.dp)
        ) {
            drawCircle(
                color = baseColor.copy(alpha = 0.3f),
                radius = this.size.minDimension / 2,
                style = Fill
            )
        }

        // Layer 3: Main Button Body (gradient, basınca kararan)
        Canvas(
            modifier = Modifier
                .size(size)
                .offset(y = pressDepth)
        ) {
            val lightColor = baseColor.copy(
                red = (baseColor.red * topColorBrightness).coerceIn(0f, 1f),
                green = (baseColor.green * topColorBrightness).coerceIn(0f, 1f),
                blue = (baseColor.blue * topColorBrightness).coerceIn(0f, 1f)
            )

            val shadowColor = Color(
                red = (baseColor.red * 0.7f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.7f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.7f).coerceIn(0f, 1f)
            )

            // Gradient: Üstten aydınlık, alttan koyu
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lightColor,
                        shadowColor
                    ),
                    startY = 0f,
                    endY = this.size.height
                ),
                radius = this.size.minDimension / 2
            )
        }

        // Layer 4: Top Highlight (ışık yansıması)
        Canvas(
            modifier = Modifier
                .size(size * 0.6f)
                .offset(y = pressDepth - (size * 0.15f))
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f * topColorBrightness),
                        Color.Transparent
                    ),
                    center = Offset(this.size.width / 2, this.size.height / 3)
                ),
                radius = this.size.minDimension / 2
            )
        }

        // Layer 5: Play Icon
        Box(
            modifier = Modifier
                .size(size)
                .offset(y = pressDepth),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                modifier = Modifier
                    .size(size * 0.5f)
                    .offset(x = 4.dp), // Optical centering
                tint = Color.White.copy(alpha = 0.95f)
            )
        }
    }
}