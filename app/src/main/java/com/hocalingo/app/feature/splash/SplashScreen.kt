package com.hocalingo.app.feature.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.hocalingo.app.core.ui.theme.ThemeViewModel
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
    // ✅ THEME SUPPORT - Get theme state
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

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

    // ✅ MODERN PROFESSIONAL ANIMATIONS
    // Title scale-in animation
    val titleScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "title_scale"
    )

    // Title fade-in animation (earlier since no icon)
    val titleAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600, delayMillis = 300),
        label = "title_alpha"
    )

    // Tagline fade-in animation
    val taglineAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(600, delayMillis = 600),
        label = "tagline_alpha"
    )

    // ✅ BREATHING PULSE ANIMATION (subtle)
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_scale"
    )

    // ✅ THEME-AWARE BACKGROUND COLORS
    val backgroundColor = if (isDarkTheme) {
        Color(0xFFFB9322) // Darker orange for dark theme
    } else {
        Color(0xFFFB9322) // Updated orange tone
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ✅ BRAND TITLE - Main Focus (No Icon)
            Text(
                text = "HocaLingo",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 48.sp, // Bigger since no icon
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .scale(titleScale * breathingScale) // Scale + breathing combined
                    .alpha(titleAlpha)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ TAGLINE
            Text(
                text = "Akıllı Kelime Öğrenme",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 18.sp,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // ✅ SUBTLE LOADING INDICATOR (Optional)
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .alpha(0.7f),
                color = Color.White,
                strokeWidth = 2.dp
            )
        }

        // ✅ VERSION INFO (Bottom corner - optional)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "v1.1.5",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f)
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

@Preview(showBackground = true, name = "Dark Theme")
@Composable
private fun SplashScreenDarkPreview() {
    HocaLingoTheme(darkTheme = true) {
        SplashScreen()
    }
}