package com.hocalingo.app.core.feedback

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.HocaLingoTheme

/**
 * SatisfactionDialog
 * ==================
 * First step: User selects their satisfaction level via emoji
 *
 * Flow:
 * - Shows after successful study session
 * - 4 emoji options (😍😊😐😞)
 * - Positive → Navigate to store rating
 * - Negative → Navigate to feedback form
 *
 * Package: app/src/main/java/com/hocalingo/app/core/feedback/
 */

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

@Composable
fun SatisfactionDialog(
    onDismiss: () -> Unit,
    onSatisfactionSelected: (SatisfactionLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Success icon with gradient
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFF6B35),
                                    Color(0xFFFF8E53)
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🎉",
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "Harika Çalışma!",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "HocaLingo deneyimini nasıl buluyorsun?",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color(0xFF7F8C8D),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Emoji grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    SatisfactionLevel.values().forEach { level ->
                        EmojiButton(
                            emoji = level.emoji,
                            label = level.displayName,
                            onClick = { onSatisfactionSelected(level) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Dismiss button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Şimdi Değil",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF95A5A6)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiButton(
    emoji: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "emoji_scale"
    )

    Column(
        modifier = modifier
            .clickable {
                isPressed = true
                onClick()
            }
            .scale(scale)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Emoji circle
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = Color(0xFFF8F9FA),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emoji,
                fontSize = 28.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Label
        Text(
            text = label,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color(0xFF7F8C8D),
            textAlign = TextAlign.Center
        )
    }
}

// ========== PREVIEW ==========

@Preview(showBackground = true)
@Composable
private fun SatisfactionDialogPreview() {
    HocaLingoTheme {
        SatisfactionDialog(
            onDismiss = {},
            onSatisfactionSelected = {}
        )
    }
}