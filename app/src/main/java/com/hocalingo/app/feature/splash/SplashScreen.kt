package com.hocalingo.app.feature.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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

/**
 * 🎯 UPDATED SPLASH SCREEN
 *
 * ✅ Seamless transition from system splash
 * ✅ Hoca head (lingo_nobg.png) centered - same as system splash
 * ✅ Text overlays fade-in over hoca head
 * ✅ Smooth animations with breathing effect
 *
 * Visual Hierarchy:
 * 1. Orange background (consistent with system splash)
 * 2. Hoca head (static, centered)
 * 3. Text overlays (fade-in with breathing animation)
 * 4. Loading spinner
 */
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

    // ✅ ANIMATIONS - Smooth and professional
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")

    // Hoca head fade-in (instant to match system splash)
    val iconAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "iconAlpha"
    )

    // Title fade-in with delay (after icon appears)
    val titleAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, delayMillis = 400, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )

    // Tagline fade-in with more delay
    val taglineAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, delayMillis = 700, easing = FastOutSlowInEasing),
        label = "taglineAlpha"
    )

    // Loading indicator fade-in last
    val loadingAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, delayMillis = 1000, easing = FastOutSlowInEasing),
        label = "loadingAlpha"
    )

    // 🔥 BREATHING EFFECT - BOLD & VISIBLE (1.0 ↔ 1.10)
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.10f, // 🔥 Much more visible!
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    // 🎨 LOADING PROGRESS - Linear bar animation
    val loadingProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingProgress"
    )

    // ✅ BACKGROUND - Brand purple (same as system splash)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF431F84)) // HocaLingo orange - matches themes.xml
    ) {
        // ✅ MAIN CONTENT - Absolute center with Box for perfect alignment
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 🎯 HOCA HEAD ICON - Box wrapper for perfect centering
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .scale(breathingScale) // 🔥 BOLD breathing (1.0 ↔ 1.10)
                        .alpha(iconAlpha),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.lingo_nobg),
                        contentDescription = "HocaLingo Logo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(modifier = Modifier.height(2.dp)) // 🔥 Reduced spacing

                // ✅ APP TITLE - BOLD breathing effect
                Text(
                    text = "HocaLingo",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 48.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .scale(breathingScale) // 🔥 Same bold breathing
                        .alpha(titleAlpha)
                )

                Spacer(modifier = Modifier.height(8.dp)) // 🔥 Reduced spacing

                // ✅ TAGLINE - Static, clean
                Text(
                    text = "Akıllı Kelime Öğrenme",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(taglineAlpha)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 🎨 COLORFUL LINEAR PROGRESS BAR - Purple fill
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(6.dp)
                        .alpha(loadingAlpha)
                        .clip(RoundedCornerShape(3.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    // Background track (light transparent)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.3f))
                    )

                    // Animated purple fill
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(loadingProgress)
                            .background(
                                Color(0xFF7840DE) // 🎨 Purple/Magenta
                            )
                    )
                }
            }
        }

        // ✅ VERSION INFO - Bottom center
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = "1.1.9",
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