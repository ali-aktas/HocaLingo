package com.hocalingo.app.feature.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import kotlinx.coroutines.flow.collectLatest

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * Modern Authentication Screen
 * ✅ Splash Screen ile aynı turuncu tema
 * ✅ lingo_hoca_image.png görseli
 * ✅ Açık, canlı ve profesyonel tasarım
 * ✅ Responsive - Her ekran boyutuna uyumlu
 * ✅ Status bar transparent
 */
@Composable
fun AuthScreen(
    onNavigateToOnboarding: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Theme awareness
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    // Google Sign-In Helper
    val (googleSignInHelper, launcher) = rememberGoogleSignInHelper(
        onSignInSuccess = { idToken ->
            viewModel.onEvent(AuthEvent.GoogleSignInResult(idToken))
        },
        onSignInFailed = { exception ->
            // Error will be shown via ViewModel
        }
    )

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                AuthEffect.NavigateToOnboarding -> onNavigateToOnboarding()
                is AuthEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    }

    // Breathing animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    Scaffold(
        snackbarHost = {
            HocaSnackbarHost(
                hostState = snackbarHostState,
                currentRoute = null // Auth screen has no bottom nav
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFB9322)) // Turuncu arkaplan
                .windowInsetsPadding(WindowInsets.statusBars) // ✅ Status bar padding
                .padding(paddingValues)
        ) {
            // ✅ Scrollable Column - Her ekrana uyumlu
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 32.dp), // ✅ Responsive padding
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // Logo & Branding - Küçültülmüş
                BrandingSection(breathingScale = breathingScale)

                Spacer(modifier = Modifier.height(40.dp)) // ✅ Azaltıldı

                // Main Content Card
                AuthContentCard(
                    isLoading = uiState.isLoading,
                    onGoogleSignIn = { googleSignInHelper.signIn(launcher) },
                    onAnonymousSignIn = { viewModel.onEvent(AuthEvent.SignInAnonymously) }
                )

                Spacer(modifier = Modifier.height(20.dp)) // ✅ Azaltıldı

                // Terms & Privacy
                TermsText()
            }
        }
    }
}

@Composable
private fun BrandingSection(breathingScale: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp) // ✅ Azaltıldı
    ) {
        // Logo Image - Küçültülmüş
        Image(
            painter = painterResource(id = R.drawable.lingo_hoca_image),
            contentDescription = "HocaLingo Logo",
            modifier = Modifier
                .size(120.dp) // ✅ 160dp → 120dp
                .scale(breathingScale),
            contentScale = ContentScale.Fit
        )

        // App Name - Küçültülmüş
        Text(
            text = "HocaLingo",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp, // ✅ 40sp → 32sp
            color = Color.White,
            letterSpacing = 1.sp
        )

        // Tagline - Küçültülmüş
        Text(
            text = "Kelime Öğrenmenin En Keyifli Yolu",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp, // ✅ 16sp → 14sp
            color = Color.White.copy(alpha = 0.95f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AuthContentCard(
    isLoading: Boolean,
    onGoogleSignIn: () -> Unit,
    onAnonymousSignIn: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 12.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp), // ✅ 32dp → 28dp
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp) // ✅ 20dp → 16dp
        ) {

            // Welcome Text - Küçültülmüş
            Text(
                text = "Hoş Geldin!",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp, // ✅ 28sp → 24sp
                color = Color(0xFF1A1A2E),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Kelime öğrenme yolculuğuna başlamak için giriş yap",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp, // ✅ 15sp → 14sp
                color = Color(0xFF6C757D),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp // ✅ 22sp → 20sp
            )

            Spacer(modifier = Modifier.height(4.dp)) // ✅ 8dp → 4dp

            // Google Sign In Button
            GoogleSignInButton(
                onClick = onGoogleSignIn,
                isLoading = isLoading
            )

            // Divider with OR
            DividerWithText()

            // Anonymous Sign In Button
            AnonymousSignInButton(
                onClick = onAnonymousSignIn,
                isLoading = isLoading
            )

            // Guest Info - Padding azaltıldı
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF4E6)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.auth_guest_info),
                    modifier = Modifier.padding(12.dp), // ✅ 14dp → 12dp
                    fontFamily = PoppinsFontFamily,
                    fontSize = 11.sp, // ✅ 12sp → 11sp
                    color = Color(0xFF8B5A00),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp // ✅ 18sp → 16sp
                )
            }
        }
    }
}

@Composable
private fun GoogleSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean
) {
    Button(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp), // ✅ 58dp → 56dp
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFB9322), // Turuncu tema
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Google Icon
                Box(
                    modifier = Modifier
                        .size(30.dp) // ✅ 32dp → 30dp
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        fontFamily = PoppinsFontFamily,
                        fontSize = 16.sp, // ✅ 18sp → 16sp
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFB9322)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = stringResource(R.string.auth_google_signin),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp, // ✅ 16sp → 15sp
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun AnonymousSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean
) {
    OutlinedButton(
        onClick = onClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp), // ✅ 58dp → 56dp
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color(0xFF1A1A2E)
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            width = 2.dp
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "👤",
                fontSize = 24.sp // ✅ 26sp → 24sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = stringResource(R.string.auth_guest_signin),
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp // ✅ 16sp → 15sp
            )
        }
    }
}

@Composable
private fun DividerWithText() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp), // ✅ 8dp → 6dp
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFFE0E0E0)
        )

        Text(
            text = stringResource(R.string.auth_or),
            modifier = Modifier.padding(horizontal = 16.dp),
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp, // ✅ 13sp → 12sp
            color = Color(0xFF9E9E9E)
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFFE0E0E0)
        )
    }
}

@Composable
private fun TermsText() {
    Text(
        text = stringResource(R.string.auth_terms),
        fontFamily = PoppinsFontFamily,
        fontSize = 11.sp, // ✅ 12sp → 11sp
        color = Color.White.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
        lineHeight = 16.sp // ✅ 18sp → 16sp
    )
}

@Preview(showBackground = true)
@Composable
private fun AuthScreenPreview() {
    HocaLingoTheme {
        AuthScreen(
            onNavigateToOnboarding = {}
        )
    }
}

@Preview(showBackground = true, name = "Loading State")
@Composable
private fun AuthScreenLoadingPreview() {
    HocaLingoTheme {
        // Preview with loading state
        AuthScreen(
            onNavigateToOnboarding = {}
        )
    }
}