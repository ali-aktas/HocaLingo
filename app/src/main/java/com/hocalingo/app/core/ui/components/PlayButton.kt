package com.hocalingo.app.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hocalingo.app.R

/**
 * Professional 3D Play Button - SVG Based
 *
 * Default:
 * - Shadow görünür (yükseklik hissi)
 *
 * Press:
 * - Top aşağı çöker
 * - Shadow kapanır (opacity + offset)
 */
@Composable
fun PlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }

    // Top layer press movement
    val topOffset by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "top_offset"
    )

    // Shadow kapanma animasyonu
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 12.dp,
        animationSpec = tween(durationMillis = 120),
        label = "shadow_offset"
    )

    val shadowAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0f else 1f,
        animationSpec = tween(durationMillis = 120),
        label = "shadow_alpha"
    )

    // Press karartma
    val brightness by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "brightness"
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
                            if (released) onClick()
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {

        // SHADOW — default görünür, basınca kapanır
        Image(
            painter = painterResource(id = R.drawable.play_shadow),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .offset(y = shadowOffset)
                .graphicsLayer {
                    alpha = shadowAlpha
                }
        )

        // TOP SHAPE — basınca aşağı çöker
        Image(
            painter = painterResource(id = R.drawable.play_top),
            contentDescription = "Play",
            modifier = Modifier
                .fillMaxSize()
                .offset(y = topOffset),
            colorFilter = if (brightness < 1f) {
                ColorFilter.colorMatrix(
                    ColorMatrix().apply {
                        setToScale(brightness, brightness, brightness, 1f)
                    }
                )
            } else null
        )
    }
}

/** Small variant */
@Composable
fun PlayButtonSmall(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayButton(
        onClick = onClick,
        modifier = modifier,
        size = 80.dp
    )
}

/** Large variant */
@Composable
fun PlayButtonLarge(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlayButton(
        onClick = onClick,
        modifier = modifier,
        size = 160.dp
    )
}
