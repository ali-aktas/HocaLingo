package com.hocalingo.app.feature.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import kotlinx.coroutines.flow.collectLatest

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToMain: () -> Unit = {},
    viewModel: SplashViewModel = hiltViewModel()
) {
    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collectLatest { event ->
            when (event) {
                SplashNavigationEvent.NavigateToAuth -> onNavigateToAuth()
                SplashNavigationEvent.NavigateToOnboarding -> onNavigateToOnboarding()
                SplashNavigationEvent.NavigateToMain -> onNavigateToMain()
            }
        }
    }

    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "splash_animation")

    // Logo animations
    val logoScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )

    val logoAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, delayMillis = 200),
        label = "logo_alpha"
    )

    // Text animations
    val titleAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, delayMillis = 600),
        label = "title_alpha"
    )

    val taglineAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, delayMillis = 1000),
        label = "tagline_alpha"
    )

    // Shimmer effect for background
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer"
    )

    // Pulse effect for logo
    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF00D4FF), // Açık turkuaz
                        Color(0xFF1E88E5)  // Koyu mavi
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated background elements
        AnimatedBackgroundElements(shimmerOffset)

        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(logoScale * logoPulse)
                    .alpha(logoAlpha)
            ) {
                // Logo background circle
                Card(
                    modifier = Modifier.size(120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        // Main logo circle
                        Card(
                            modifier = Modifier.size(80.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = CircleShape,
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                // Simple logo icon
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Hocalingo Logo",
                                    tint = Color(0xFF2196F3),
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // App Name
            Text(
                text = "Hocalingo",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 42.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(titleAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline
            Text(
                text = "Learn words, stay fluent.",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha)
            )
        }
    }
}

@Composable
private fun AnimatedBackgroundElements(shimmerOffset: Float) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    Box(modifier = Modifier.fillMaxSize()) {
        // Floating geometric shapes
        repeat(8) { index ->
            val size = (40 + index * 15).dp
            val xPos = (screenWidth * (0.1f + (index * 0.13f) % 1.0f)).dp
            val yPos = (screenHeight * (0.15f + (index * 0.17f) % 1.0f)).dp

            val animatedAlpha by rememberInfiniteTransition(label = "shape_$index").animateFloat(
                initialValue = 0.05f,
                targetValue = 0.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 1500 + index * 200,
                        easing = EaseInOutSine
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "shape_alpha_$index"
            )

            Card(
                modifier = Modifier
                    .size(size)
                    .offset(x = xPos, y = yPos)
                    .graphicsLayer {
                        rotationZ = shimmerOffset * 45f + index * 30f
                        alpha = animatedAlpha
                    },
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                shape = if (index % 2 == 0) CircleShape else RoundedCornerShape(8.dp)
            ) {}
        }

        // Shimmer waves
        repeat(3) { waveIndex ->
            val waveHeight = screenHeight * (0.3f + waveIndex * 0.2f)
            val waveWidth = screenWidth * 1.5f

            Box(
                modifier = Modifier
                    .size(waveWidth.dp, 2.dp)
                    .offset(
                        x = (shimmerOffset * screenWidth * 0.5f).dp,
                        y = waveHeight.dp
                    )
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    HocaLingoTheme {
        SplashScreen()
    }
}