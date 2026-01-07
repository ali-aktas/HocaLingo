package com.hocalingo.app.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocalingo.app.R

/**
 * PREMIUM CIRCULAR STAT CARD
 *
 * Tasarım Felsefesi:
 * - Inner shadow ile derinlik (progress arkası çukur görünür)
 * - Glow effect ile progress öne çıkar
 * - Stroke kalınlığı dinamik (progress dolu olan yerde kalın)
 * - Optik hiyerarşi (parlak center, koyu edge)
 *
 * Layer Yapısı:
 * 1. Background ring (inner shadow ile çukur)
 * 2. Progress ring (glow + gradient)
 * 3. Value text (bold, center)
 * 4. Label text (subtle, bottom)
 */
@Composable
fun CircularStatCard(
    value: String,
    label: String,
    progress: Float,
    color: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "stat_progress"
    )

    // Poppins font
    val poppins = FontFamily(
        Font(R.font.poppins_black, FontWeight.Black),
        Font(R.font.poppins_bold, FontWeight.Bold),
        Font(R.font.poppins_medium, FontWeight.Medium)
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.width(size + 20.dp)
    ) {
        // Circular Progress Stack
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(size)
        ) {
            // Layer 1: Background Ring (inner shadow)
            Canvas(modifier = Modifier.size(size)) {
                val center = Offset(this.size.width / 2, this.size.height / 2)
                val radius = this.size.minDimension / 2 - 12.dp.toPx()

                // Inner shadow (koyu gradient, çukur hissi)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = if (isDarkTheme) {
                            listOf(
                                Color(0xFF1A1625), // Center dark
                                Color(0xFF2D2438)  // Edge lighter
                            )
                        } else {
                            listOf(
                                Color(0xFFE8E8EA), // Center dark
                                Color(0xFFF5F5F7)  // Edge lighter
                            )
                        },
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // Background arc (subtle)
                drawArc(
                    color = if (isDarkTheme)
                        Color(0xFF2D2438).copy(alpha = 0.5f)
                    else
                        Color(0xFFE5E5EA).copy(alpha = 0.8f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 11.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Layer 2: Progress Ring (glow + gradient)
            Canvas(modifier = Modifier.size(size)) {
                if (animatedProgress > 0f) {
                    val sweepAngle = 360f * animatedProgress

                    // Outer glow (soft halo)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                color.copy(alpha = 0.3f),
                                color.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Main progress arc (solid + gradient)
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                color,
                                color.copy(alpha = 0.85f),
                                color
                            )
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(
                            width = 11.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // Layer 3: Center Value Text
            Text(
                text = value,
                fontFamily = poppins,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = if (isDarkTheme) Color.White else Color(0xFF1C1B1F),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Layer 4: Label Text
        Text(
            text = label,
            fontFamily = poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            color = if (isDarkTheme)
                Color(0xFFB4B4B8)
            else
                Color(0xFF8E8E93),
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}