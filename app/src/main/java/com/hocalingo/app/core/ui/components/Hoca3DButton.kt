package com.hocalingo.app.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hocalingo.app.core.ui.theme.HocaSpacing
import com.hocalingo.app.core.ui.theme.HocaSize

/**
 * HocaLingo 3D Button Component
 * Duolingo-inspired tactile button with press animation
 *
 * Features:
 * - 2-layer depth illusion
 * - Spring press animation
 * - Support for text, image, or both
 * - Adaptive sizing
 */
@Composable
fun Hoca3DButton(
    onClick: () -> Unit,
    topColor: Color,
    bottomColor: Color,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp = HocaSize.buttonLG,
    cornerRadius: Dp = HocaSize.radiusLG,
    depthOffset: Dp = 6.dp,
    text: String? = null,
    textColor: Color = Color.White, // Parametrik text rengi
    image: Painter? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = HocaSpacing.md)
) {
    var isPressed by remember { mutableStateOf(false) }

    val offsetY by animateDpAsState(
        targetValue = if (isPressed) depthOffset else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_press_animation"
    )

    val containerModifier = if (width != null) {
        modifier
            .width(width)
            .height(height + depthOffset)
    } else {
        modifier
            .fillMaxWidth()
            .height(height + depthOffset)
    }

    Box(modifier = containerModifier) {
        // Bottom Layer (Shadow/Depth) - Static
        Box(
            modifier = Modifier
                .then(
                    if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
                )
                .height(height)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(cornerRadius))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = bottomColor)
            }
        }

        // Top Layer (Interactive Surface)
        Surface(
            onClick = onClick,
            modifier = Modifier
                .then(
                    if (width != null) Modifier.width(width) else Modifier.fillMaxWidth()
                )
                .height(height)
                .offset(y = offsetY)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            val released = tryAwaitRelease()
                            isPressed = false
                            if (released) {
                                // onClick already triggered by Surface
                            }
                        }
                    )
                },
            shape = RoundedCornerShape(cornerRadius),
            color = topColor
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                when {
                    image != null && text != null -> {
                        // Image + Text layout
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Image(
                                painter = image,
                                contentDescription = null,
                                modifier = Modifier.size(HocaSize.iconXL),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(HocaSpacing.xs))
                            Text(
                                text = text,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                    image != null -> {
                        // Image only
                        Image(
                            painter = image,
                            contentDescription = null,
                            modifier = Modifier.size(HocaSize.iconXL),
                            contentScale = ContentScale.Fit
                        )
                    }
                    text != null -> {
                        // Text only
                        Text(
                            text = text,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}