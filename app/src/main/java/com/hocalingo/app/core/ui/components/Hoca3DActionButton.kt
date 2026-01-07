package com.hocalingo.app.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * PREMIUM 3D ACTION BUTTON
 *
 * Tasarım Felsefesi:
 * - Kare/rectangular fiziksel düğme
 * - Işık kaynağı üstten geliyor
 * - Basınca color darken + depth offset
 * - Icon centered, optik hizalama
 *
 * Layer Yapısı:
 * 1. Bottom shadow (blur effect)
 * 2. Depth layer (gradient, 3D hissi)
 * 3. Top surface (ışıklı, basınca kararan)
 * 4. Top highlight (subtle)
 * 5. Icon content
 */
@Composable
fun Hoca3DActionButton(
    onClick: () -> Unit,
    icon: Painter,
    baseColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    cornerRadius: Dp = 24.dp,
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
        targetValue = if (isPressed) 0.85f else 1f,
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
            .size(size)
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
        // Layer 1: Bottom Shadow
        Canvas(
            modifier = Modifier
                .size(size)
                .offset(y = 12.dp)
        ) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = shadowAlpha),
                        Color.Transparent
                    ),
                    center = Offset(this.size.width / 2, this.size.height / 2),
                    radius = this.size.minDimension / 1.5f
                ),
                size = this.size,
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }

        // Layer 2: Depth Layer
        Canvas(
            modifier = Modifier
                .size(size)
                .offset(y = pressDepth + 4.dp)
        ) {
            drawRoundRect(
                color = baseColor.copy(alpha = 0.4f),
                size = this.size,
                cornerRadius = CornerRadius(cornerRadius.toPx()),
                style = Fill
            )
        }

        // Layer 3: Main Button Surface
        Canvas(
            modifier = Modifier
                .size(size)
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

            // Vertical gradient (light to shadow)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(lightColor, shadowColor),
                    startY = 0f,
                    endY = this.size.height
                ),
                size = this.size,
                cornerRadius = CornerRadius(cornerRadius.toPx())
            )
        }

        // Layer 4: Top Highlight (subtle light reflection)
        Canvas(
            modifier = Modifier
                .size(size * 0.8f, size * 0.4f)
                .offset(y = pressDepth - (size * 0.2f))
        ) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.25f * topColorBrightness),
                        Color.Transparent
                    ),
                    center = Offset(this.size.width / 2, this.size.height / 2),
                    radius = this.size.minDimension
                ),
                size = this.size,
                cornerRadius = CornerRadius((cornerRadius * 0.6f).toPx())
            )
        }

        // Layer 5: Icon Content
        Box(
            modifier = Modifier
                .size(size)
                .offset(y = pressDepth),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(size * 0.45f),
                contentScale = ContentScale.Fit,
                alpha = 0.95f
            )
        }
    }
}