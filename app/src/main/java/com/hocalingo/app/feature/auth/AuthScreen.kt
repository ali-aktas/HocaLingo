package com.hocalingo.app.feature.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_semibold, FontWeight.SemiBold),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

@Composable
fun AuthScreen(
    onNavigateToOnboarding: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    val (googleSignInHelper, launcher) = rememberGoogleSignInHelper(
        onSignInSuccess = { idToken ->
            viewModel.onEvent(AuthEvent.GoogleSignInResult(idToken))
        },
        onSignInFailed = { exception ->
        }
    )

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
                currentRoute = null
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF431F84))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(top = 20.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                BrandingSection(breathingScale = breathingScale)

                Spacer(modifier = Modifier.height(40.dp))

                AuthContentCard(
                    isLoading = uiState.isLoading,
                    onGoogleSignIn = { googleSignInHelper.signIn(launcher) },
                    onAnonymousSignIn = { viewModel.onEvent(AuthEvent.SignInAnonymously) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                TermsText()
            }
        }
    }
}

@Composable
private fun BrandingSection(breathingScale: Float) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.hocalingo_cards),
            contentDescription = "HocaLingo Logo",
            modifier = Modifier
                .size(120.dp)
                .scale(breathingScale),
            contentScale = ContentScale.Fit
        )

        Text(
            text = "HocaLingo",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            color = Color.White,
            letterSpacing = 1.sp
        )

        Text(
            text = "Kelime Öğrenmenin En Keyifli Yolu",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
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
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Text(
                text = "Hoş Geldin!",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color(0xFF1A1A2E),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Kelime öğrenme yolculuğuna başlamak için giriş yap",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                color = Color(0xFF6C757D),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            GoogleSignInButton(
                onClick = onGoogleSignIn,
                isLoading = isLoading
            )

            DividerWithText()

            AnonymousSignInButton(
                onClick = onAnonymousSignIn,
                isLoading = isLoading
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0x5E959595)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.auth_guest_info),
                    modifier = Modifier.padding(12.dp),
                    fontFamily = PoppinsFontFamily,
                    fontSize = 11.sp,
                    color = Color(0xFF5A008B),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
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
    var isPressed by remember { mutableStateOf(false) }

    val offsetY by animateFloatAsState(
        targetValue = if (isPressed) 4f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "button_press"
    )

    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 2f else 6f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "button_elevation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .pointerInput(isLoading) {
                if (!isLoading) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onClick() }
                    )
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset(y = 6.dp)
        ) {
            drawRoundRect(
                color = Color(0xFFE85A2B),
                size = size,
                cornerRadius = CornerRadius(16.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset(y = offsetY.dp)
                .shadow(
                    elevation = elevation.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFB9322),
                            Color(0xFFFF6B35)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color.White,
                    strokeWidth = 3.dp
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFB9322)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = stringResource(R.string.auth_google_signin),
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AnonymousSignInButton(
    onClick: () -> Unit,
    isLoading: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }

    val offsetY by animateFloatAsState(
        targetValue = if (isPressed) 3f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "button_press"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .pointerInput(isLoading) {
                if (!isLoading) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        },
                        onTap = { onClick() }
                    )
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .offset(y = 5.dp)
        ) {
            drawRoundRect(
                color = Color(0xFFE0E0E0),
                size = size,
                cornerRadius = CornerRadius(16.dp.toPx())
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .offset(y = offsetY.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "👤",
                    fontSize = 26.sp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = stringResource(R.string.auth_guest_signin),
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = Color(0xFF1A1A2E)
                )
            }
        }
    }
}

@Composable
private fun DividerWithText() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
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
            fontSize = 12.sp,
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
        fontSize = 11.sp,
        color = Color.White.copy(alpha = 0.85f),
        textAlign = TextAlign.Center,
        lineHeight = 16.sp
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
        AuthScreen(
            onNavigateToOnboarding = {}
        )
    }
}