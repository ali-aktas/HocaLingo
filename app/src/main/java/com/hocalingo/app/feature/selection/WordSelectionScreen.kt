package com.hocalingo.app.feature.selection

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaErrorState
import com.hocalingo.app.core.ui.components.HocaLoadingIndicator
import com.hocalingo.app.core.ui.theme.ThemeViewModel
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
    val uiState by viewModel.uiState.collectAsState() // ✅ Using original collectAsState
    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ THEME ADAPTATION - Get theme state
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    // Handle effects - ✅ Using original effect handling
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                WordSelectionEffect.NavigateToStudy -> onNavigateToStudy()
                WordSelectionEffect.ShowCompletionMessage -> {
                    snackbarHostState.showSnackbar("Tebrikler! Tüm kelimeler işlendi.")
                }
                WordSelectionEffect.ShowUndoMessage -> {
                    snackbarHostState.showSnackbar("Geri alındı")
                }
                is WordSelectionEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    // ✅ Theme-aware scaffold
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background // ✅ Theme-aware background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // ✅ Theme-aware background
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    HocaLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        text = "Kelimeler yükleniyor..."
                    )
                }
                uiState.error != null -> {
                    HocaErrorState(
                        message = uiState.error!!, // ✅ FIXED: error -> message
                        onRetry = { /* Retry logic if needed */ },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.isCompleted -> {
                    CompletionScreen(
                        selectedCount = uiState.selectedCount,
                        isDarkTheme = isDarkTheme, // ✅ Pass theme state
                        onContinue = { viewModel.onEvent(WordSelectionEvent.FinishSelection) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.currentWord != null -> {
                    WordSelectionContent(
                        uiState = uiState,
                        isDarkTheme = isDarkTheme, // ✅ Pass theme state
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
    isDarkTheme: Boolean, // ✅ New theme parameter
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
            color = MaterialTheme.colorScheme.onBackground, // ✅ Theme-aware
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // Instruction text - ✅ Theme-aware
        Text(
            text = "Swipe right to learn, left if you already know.",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, // ✅ Theme-aware
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Card area - ✅ Using original single card approach
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            uiState.currentWord?.let { word ->
                SwipeableWordCard(
                    word = word.english, // ✅ Using ConceptEntity.english
                    translation = word.turkish, // ✅ Using ConceptEntity.turkish
                    isDarkTheme = isDarkTheme, // ✅ Theme support
                    onSwipeLeft = { onSwipeLeft(word.id) }, // ✅ Using ConceptEntity.id
                    onSwipeRight = { onSwipeRight(word.id) }, // ✅ Using ConceptEntity.id
                    triggerSwipeLeft = triggerSwipeLeft,
                    triggerSwipeRight = triggerSwipeRight,
                    onAnimationComplete = {
                        triggerSwipeLeft = false
                        triggerSwipeRight = false
                    }
                )
            }
        }

        // Enhanced 4-button action row - ✅ Theme-aware
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
                backgroundColor = if (isDarkTheme) Color(0xFF6C757D) else Color(0xFF6C757D),
                contentDescription = "Home",
                onClick = onNavigateToHome
            )

            // Skip button (large)
            LargeActionButton(
                icon = Icons.Default.Close,
                backgroundColor = if (isDarkTheme) Color(0xFFFF6B6B) else Color(0xFFFF4444),
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
                icon = Icons.Default.Favorite,
                backgroundColor = if (isDarkTheme) Color(0xFF66BB6A) else Color(0xFF4CAF50),
                contentDescription = "Learn",
                onClick = {
                    triggerSwipeRight = true
                    uiState.currentWord?.let { word ->
                        onSwipeRight(word.id)
                    }
                }
            )

            // Undo button (small) - ✅ Using original canUndo logic
            SmallActionButton(
                icon = Icons.Default.Undo,
                backgroundColor = if (isDarkTheme) Color(0xFF2196F3) else Color(0xFF2196F3),
                contentDescription = "Undo",
                onClick = onUndo,
                enabled = uiState.canUndo
            )
        }
    }
}

@Composable
private fun CompletionScreen(
    selectedCount: Int,
    isDarkTheme: Boolean, // ✅ Theme parameter
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                MaterialTheme.colorScheme.surface
            } else {
                Color.White
            }
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎉",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Great job!",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface // ✅ Theme-aware
            )

            Text(
                text = "You selected $selectedCount words to learn.",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, // ✅ Theme-aware
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Learning")
            }
        }
    }
}

// ✅ IMPROVED: Smooth swipeable card with Tinder-like animations
@Composable
private fun SwipeableWordCard(
    word: String,
    translation: String,
    isDarkTheme: Boolean,
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
    val offsetY = remember { Animatable(0f) }
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
            scope.launch {
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
            scope.launch {
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
        offsetY.snapTo(0f)
        rotation.snapTo(0f)
        scale.snapTo(1f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .graphicsLayer {
                rotationZ = cardRotation
                scaleX = 1f - swipeProgress * 0.1f
                scaleY = 1f - swipeProgress * 0.1f
                alpha = 1f - swipeProgress * 0.3f
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
                                    launch { offsetX.animateTo(0f, tween(300)) }
                                    launch { offsetY.animateTo(0f, tween(300)) }
                                }
                            }
                        }
                    }
                ) { _, dragAmount ->
                    scope.launch {
                        offsetX.snapTo(offsetX.value + dragAmount.x)
                        offsetY.snapTo(offsetY.value + dragAmount.y * 0.3f) // Slight vertical movement
                    }
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                MaterialTheme.colorScheme.surface
            } else {
                Color.White
            }
        ),
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
                // Main word - ✅ Theme-aware colors
                Text(
                    text = word,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = if (isDarkTheme) Color(0xFF81D4FA) else Color(0xFF1976D2),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Translation - ✅ Theme-aware colors
                Text(
                    text = translation,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
            }

            // Swipe indicators overlay
            // Left indicator (Skip)
            if (offsetX.value < -50f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(32.dp)
                        .background(
                            Color.Red.copy(alpha = swipeProgress * 0.8f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "❌ SKIP",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Right indicator (Learn)
            if (offsetX.value > 50f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(32.dp)
                        .background(
                            Color.Green.copy(alpha = swipeProgress * 0.8f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = "✅ LEARN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

// ✅ THEME-AWARE Action Buttons
@Composable
private fun LargeActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "LargeButtonScale"
    )

    Card(
        modifier = Modifier
            .size(64.dp)
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
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SmallActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "SmallButtonScale"
    )

    Card(
        modifier = Modifier
            .size(48.dp)
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