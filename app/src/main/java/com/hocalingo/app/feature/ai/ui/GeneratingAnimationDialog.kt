package com.hocalingo.app.feature.ai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.hocalingo.app.R
import kotlinx.coroutines.delay

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * GeneratingAnimationDialog - Loading animation for story generation
 *
 * Features:
 * ✅ Lottie animation
 * ✅ Phase-based messages
 * ✅ Premium AI feel
 * ✅ Non-dismissible during generation
 *
 * Required:
 * - Lottie animation file: res/raw/ai_generating_animation.json
 * - Lottie Compose dependency in build.gradle
 */
@Composable
fun GeneratingAnimationDialog() {
    var currentPhase by remember { mutableStateOf(0) }

    val phases = listOf(
        "Kelimelerini topluyorum..." to "🔍",
        "Hikayeni yazıyorum..." to "✍️",
        "Son rötuşlar..." to "✨"
    )

    // Cycle through phases
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000) // Change phase every 2 seconds
            currentPhase = (currentPhase + 1) % phases.size
        }
    }

    Dialog(
        onDismissRequest = { /* Non-dismissible */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF211A2E),
                            Color(0xFF1A1625)
                        )
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Lottie Animation
                LottieAnimationView()

                // Phase message with emoji
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = phases[currentPhase].second,
                        fontSize = 32.sp
                    )

                    Text(
                        text = phases[currentPhase].first,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }

                // Loading indicator
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF7C3AED),
                    trackColor = Color(0xFF2D1B4E)
                )
            }
        }
    }
}

/**
 * Lottie Animation View
 */
@Composable
private fun LottieAnimationView() {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.ai_generating_animation)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 1f
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier
            .size(200.dp)
    )
}

/**
 * FALLBACK: If Lottie animation not available, show simple loading
 */
@Composable
private fun FallbackLoadingAnimation() {
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(Color(0xFF2D1B4E).copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(80.dp),
            color = Color(0xFF7C3AED),
            strokeWidth = 6.dp
        )
    }
}