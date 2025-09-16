package com.hocalingo.app.feature.selection.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaErrorState
import com.hocalingo.app.core.ui.components.HocaLoadingIndicator
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.*

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

@Composable
fun WordSelectionScreen(
    onNavigateToStudy: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    viewModel: WordSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                WordSelectionEffect.NavigateToStudy -> onNavigateToStudy()
                WordSelectionEffect.ShowCompletionMessage -> {
                    snackbarHostState.showSnackbar("Tebrikler! Tüm kelimeler işlendi.")
                }
                WordSelectionEffect.ShowUndoMessage -> {
                    snackbarHostState.showSnackbar("Önceki işlem geri alındı")
                }
                is WordSelectionEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    HocaLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        text = stringResource(R.string.loading)
                    )
                }
                uiState.error != null -> {
                    HocaErrorState(
                        error = uiState.error!!,
                        onRetry = { /* Retry logic if needed */ },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.isCompleted -> {
                    CompletionScreen(
                        selectedCount = uiState.selectedCount,
                        onContinue = { viewModel.onEvent(WordSelectionEvent.FinishSelection) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.currentWord != null -> {
                    WordSelectionContent(
                        uiState = uiState,
                        onSwipeLeft = { wordId ->
                            viewModel.onEvent(WordSelectionEvent.SwipeLeft(wordId))
                        },
                        onSwipeRight = { wordId ->
                            viewModel.onEvent(WordSelectionEvent.SwipeRight(wordId))
                        },
                        onUndo = {
                            viewModel.onEvent(WordSelectionEvent.Undo)
                        },
                        onNavigateToHome = onNavigateToHome
                    )
                }
            }
        }
    }
}

@Composable
private fun WordSelectionContent(
    uiState: WordSelectionUiState,
    onSwipeLeft: (Int) -> Unit,
    onSwipeRight: (Int) -> Unit,
    onUndo: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    // Local state for triggering button animations
    var triggerSwipeLeft by remember { mutableStateOf(false) }
    var triggerSwipeRight by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header - Hocalingo title
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Hocalingo",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = Color.Black,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Instruction text
        Text(
            text = "Swipe right to learn, left if you already know.",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Card area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            uiState.currentWord?.let { word ->
                SwipeableWordCard(
                    word = word.english,
                    translation = word.turkish,
                    onSwipeLeft = { onSwipeLeft(word.id) },
                    onSwipeRight = { onSwipeRight(word.id) },
                    triggerSwipeLeft = triggerSwipeLeft,
                    triggerSwipeRight = triggerSwipeRight,
                    onAnimationComplete = {
                        triggerSwipeLeft = false
                        triggerSwipeRight = false
                    }
                )
            }
        }

        // Enhanced 4-button action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home button (small)
            SmallActionButton(
                icon = Icons.Default.Home,
                backgroundColor = Color(0xFF6C757D),
                contentDescription = "Home",
                onClick = onNavigateToHome
            )

            // Skip button (large)
            LargeActionButton(
                icon = Icons.Default.Close,
                backgroundColor = Color(0xFFFF4444),
                contentDescription = "Skip",
                onClick = {
                    triggerSwipeLeft = true
                    uiState.currentWord?.let { word ->
                        onSwipeLeft(word.id)
                    }
                }
            )

            // Learn button (large)
            LargeActionButton(
                icon = Icons.Default.Star,
                backgroundColor = Color(0xFFFFD700),
                contentDescription = "Learn",
                onClick = {
                    triggerSwipeRight = true
                    uiState.currentWord?.let { word ->
                        onSwipeRight(word.id)
                    }
                }
            )

            // Undo button (small)
            SmallActionButton(
                icon = Icons.Default.Refresh,
                backgroundColor = Color(0xFF17A2B8),
                contentDescription = "Undo",
                onClick = onUndo,
                enabled = uiState.canUndo
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LargeActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = Modifier
            .size(72.dp) // Large size
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f)
        ),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun SmallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )

    Card(
        modifier = Modifier
            .size(48.dp) // Small size (half of large)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(enabled = enabled) { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) backgroundColor else backgroundColor.copy(alpha = 0.5f)
        ),
        shape = CircleShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SwipeableWordCard(
    word: String,
    translation: String,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
    triggerSwipeLeft: Boolean,
    triggerSwipeRight: Boolean,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val density = LocalDensity.current

    // Swipe threshold
    val swipeThreshold = with(density) { (screenWidth.toPx() * 0.25f) }

    // Animation states
    val offsetX = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    val scope = rememberCoroutineScope()

    // Derived state for visual feedback
    val swipeProgress: Float by remember {
        derivedStateOf {
            (abs(offsetX.value) / swipeThreshold).coerceIn(0f, 1f)
        }
    }

    val cardRotation: Float by remember {
        derivedStateOf {
            (offsetX.value / swipeThreshold * 15f).coerceIn(-15f, 15f)
        }
    }

    // Handle button-triggered animations
    LaunchedEffect(triggerSwipeLeft) {
        if (triggerSwipeLeft) {
            launch {
                val screenWidthPx = with(density) { screenWidth.toPx() }
                offsetX.animateTo(
                    targetValue = -screenWidthPx * 1.5f, // ✅ Completely off-screen
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
                onAnimationComplete()
            }
        }
    }

    LaunchedEffect(triggerSwipeRight) {
        if (triggerSwipeRight) {
            launch {
                val screenWidthPx = with(density) { screenWidth.toPx() }
                offsetX.animateTo(
                    targetValue = screenWidthPx * 1.5f, // ✅ Completely off-screen
                    animationSpec = tween(400, easing = FastOutSlowInEasing)
                )
                onAnimationComplete()
            }
        }
    }

    // Reset animation when word changes
    LaunchedEffect(word) {
        offsetX.snapTo(0f)
        rotation.snapTo(0f)
        scale.snapTo(1f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer {
                rotationZ = cardRotation
                scaleX = 1f - swipeProgress * 0.1f
                scaleY = 1f - swipeProgress * 0.1f
            }
            .pointerInput(word) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            when {
                                offsetX.value > swipeThreshold -> {
                                    // Swipe right - learn
                                    launch {
                                        offsetX.animateTo(
                                            targetValue = size.width.toFloat() * 1.5f, // ✅ Fully off-screen
                                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                                        )
                                    }
                                    onSwipeRight()
                                }
                                offsetX.value < -swipeThreshold -> {
                                    // Swipe left - skip
                                    launch {
                                        offsetX.animateTo(
                                            targetValue = -size.width.toFloat() * 1.5f, // ✅ Fully off-screen
                                            animationSpec = tween(400, easing = FastOutSlowInEasing)
                                        )
                                    }
                                    onSwipeLeft()
                                }
                                else -> {
                                    // Return to center
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        }
                    }
                ) { _, dragAmount ->
                    scope.launch {
                        offsetX.snapTo(offsetX.value + dragAmount.x)
                    }
                }
            },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(32.dp)
            ) {
                // Main word - ✅ Açık mavi renk
                Text(
                    text = word,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 36.sp,
                    color = Color(0xFF2196F3), // ✅ Açık mavi
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Translation
                Text(
                    text = translation,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            // Swipe indicators
            if (swipeProgress > 0.1f) {
                val indicatorColor = if (offsetX.value > 0) {
                    Color(0xFF4CAF50) // Green for learn
                } else {
                    Color(0xFFFF4444) // Red for skip
                }

                val indicatorText = if (offsetX.value > 0) "LEARN" else "SKIP"

                Box(
                    modifier = Modifier
                        .align(if (offsetX.value > 0) Alignment.TopEnd else Alignment.TopStart)
                        .padding(24.dp)
                        .background(
                            color = indicatorColor.copy(alpha = swipeProgress * 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = indicatorText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionScreen(
    selectedCount: Int,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Tebrikler! 🎉",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = Color.Black,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "$selectedCount kelime seçtiniz",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00D4FF)
            )
        ) {
            Text(
                text = "Öğrenmeye Başla",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WordSelectionScreenPreview() {
    HocaLingoTheme {
        WordSelectionScreen(
            onNavigateToStudy = {},
            onNavigateToHome = {}
        )
    }
}