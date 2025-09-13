package com.hocalingo.app.feature.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import kotlinx.coroutines.flow.collectLatest

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit = {},
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToMain: () -> Unit = {},
    viewModel: SplashViewModel = hiltViewModel()
) {
    // Enhanced animations
    val infiniteTransition = rememberInfiniteTransition(label = "splash_animation")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutCubic),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "icon_rotation"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        MaterialTheme.colorScheme.background
                    ),
                    radius = 800f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Enhanced background decorations
        SplashBackgroundDecorations(pulseAlpha = pulseAlpha)

        // Main content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Enhanced app icon with animations
            EnhancedAppIcon(
                scale = scale,
                rotation = rotation
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Enhanced app name and tagline
            EnhancedAppBranding()

            Spacer(modifier = Modifier.height(48.dp))

            // Enhanced loading indicator
            EnhancedLoadingIndicator()
        }

        // Version info at bottom
        VersionInfo(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
        )
    }
}

@Composable
private fun SplashBackgroundDecorations(pulseAlpha: Float) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Floating circles with pulse effect
        repeat(5) { index ->
            val size = (80 + index * 40).dp
            val xOffset = screenWidth * (0.1f + index * 0.2f)
            val yOffset = screenHeight * (0.1f + index * 0.2f)

            Box(
                modifier = Modifier
                    .size(size)
                    .offset(x = xOffset, y = yOffset)
                    .scale(1f + pulseAlpha * 0.3f)
                    .background(
                        Color.White.copy(alpha = pulseAlpha * 0.1f),
                        CircleShape
                    )
                    .blur((20 + index * 10).dp)
            )
        }
    }
}

@Composable
private fun EnhancedAppIcon(
    scale: Float,
    rotation: Float
) {
    Box(
        contentAlignment = Alignment.Center
    ) {
        // Outer glow effect
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .size((160 + index * 20).dp)
                    .scale(scale * (0.8f + index * 0.1f))
                    .background(
                        Color.White.copy(alpha = 0.1f - index * 0.03f),
                        CircleShape
                    )
                    .blur((15 + index * 5).dp)
            )
        }

        // Main icon container
        Card(
            modifier = Modifier
                .size(140.dp)
                .scale(scale),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            shape = CircleShape
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Rotating graduation cap
                Text(
                    text = "🎓",
                    fontSize = 64.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            rotationY = rotation * 0.5f
                        }
                )
            }
        }
    }
}

@Composable
private fun EnhancedAppBranding() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App name with enhanced typography
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = (-0.8).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline with better styling
            Text(
                text = stringResource(R.string.app_tagline),
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EnhancedLoadingIndicator() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun VersionInfo(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "Version 1.0.0 MVP",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SplashScreenPreview() {
    HocaLingoTheme {
        SplashScreen()
    }
}