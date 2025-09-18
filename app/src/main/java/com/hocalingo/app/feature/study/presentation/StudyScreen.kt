package com.hocalingo.app.feature.study.presentation

import android.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.core.database.entities.ConceptEntity
import com.hocalingo.app.core.ui.components.HocaErrorState
import com.hocalingo.app.core.ui.components.HocaLoadingIndicator
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import okio.blackholeSink
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWordSelection: () -> Unit,
    viewModel: StudyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                StudyEffect.NavigateToHome -> onNavigateBack()
                StudyEffect.NavigateToWordSelection -> onNavigateToWordSelection()
                is StudyEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                else -> { /* Handle other effects */ }
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
                        text = "Kelimeler yükleniyor..."
                    )
                }
                uiState.error != null -> {
                    HocaErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.onEvent(StudyEvent.RetryLoading) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.showEmptyQueueMessage -> {
                    StudyEmptyState(
                        onNavigateToWordSelection = onNavigateToWordSelection
                    )
                }

                uiState.hasWordsToStudy -> {
                    StudyContent(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        onNavigateBack = onNavigateBack
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyTopAppBar(
    progress: Float,
    currentIndex: Int,
    totalWords: Int,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5))
            .padding(top = 8.dp) // Minimal top padding
    ) {
        // ✅ FIXED: Custom Row instead of TopAppBar (removes 64dp default height)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(40.dp) // Smaller button
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Title
            Text(
                text = "Günlük İlerleme",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Progress section - much smaller spacing
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 8.dp), // ✅ FIXED: Minimal padding
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentIndex + 1}/${totalWords}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp) // ✅ FIXED: Even thinner
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun StudyContent(
    uiState: StudyUiState,
    onEvent: (StudyEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    val concept = uiState.currentConcept ?: return

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar with progress
        StudyTopAppBar(
            progress = uiState.progressPercentage / 100f,
            currentIndex = uiState.currentWordIndex,
            totalWords = uiState.totalWordsInQueue,
            onNavigateBack = onNavigateBack
        )

        // ✅ FIXED: Main content with better spacing to fit 540dp card
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ✅ REDUCED: Top spacer
            Spacer(modifier = Modifier.height(8.dp))

            // ✅ FIXED: Direct FlipCard without weight constraint
            FlipCard(
                concept = concept,
                isFlipped = uiState.isCardFlipped,
                studyDirection = uiState.studyDirection,
                onFlip = { onEvent(StudyEvent.FlipCard) },
                onPlayPronunciation = { onEvent(StudyEvent.PlayPronunciation) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(540.dp) // ✅ Direct height without constraints
            )

            // ✅ REDUCED: Spacer between card and buttons
            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons at bottom
            ActionButtonsHorizontal(
                onEasyClick = { onEvent(StudyEvent.EasyButtonPressed) },
                onMediumClick = { onEvent(StudyEvent.MediumButtonPressed) },
                onHardClick = { onEvent(StudyEvent.HardButtonPressed) },
                easyTimeText = uiState.easyTimeText,
                mediumTimeText = uiState.mediumTimeText,
                hardTimeText = uiState.hardTimeText,
                modifier = Modifier.fillMaxWidth()
            )

            // ✅ REDUCED: Bottom spacer
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ✅ FIXED: FlipCard - Clean 540dp implementation
@Composable
private fun FlipCard(
    concept: ConceptEntity,
    isFlipped: Boolean,
    studyDirection: com.hocalingo.app.core.database.entities.StudyDirection,
    onFlip: () -> Unit,
    onPlayPronunciation: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val flipProgress = animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(600),
        label = "flip"
    )

    val actualRotation = flipProgress.value + dragOffset

    Box(
        modifier = modifier // ✅ Using modifier directly (height is already set to 540dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        if (abs(dragOffset) > 30f) {
                            onFlip()
                        }
                        dragOffset = 0f
                    }
                ) { _, dragAmount ->
                    dragOffset += dragAmount.x * 0.5f
                    dragOffset = dragOffset.coerceIn(-90f, 90f)
                }
            }
            .graphicsLayer {
                rotationY = actualRotation
                cameraDistance = 12f * density
            }
            .clickable { onFlip() },
        contentAlignment = Alignment.Center
    ) {
        if (actualRotation <= 90f) {
            // Front side
            CardFront(
                concept = concept,
                studyDirection = studyDirection,
                onPlayPronunciation = onPlayPronunciation
            )
        } else {
            // Back side
            CardBack(
                concept = concept,
                studyDirection = studyDirection,
                modifier = Modifier.graphicsLayer {
                    rotationY = 180f
                }
            )
        }
    }
}

// ✅ OPTIMIZED: CardFront with adjusted padding
@Composable
private fun CardFront(
    concept: ConceptEntity,
    studyDirection: com.hocalingo.app.core.database.entities.StudyDirection,
    onPlayPronunciation: () -> Unit
) {
    val frontText = when (studyDirection) {
        com.hocalingo.app.core.database.entities.StudyDirection.EN_TO_TR -> concept.english
        com.hocalingo.app.core.database.entities.StudyDirection.TR_TO_EN -> concept.turkish
    }

    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF00D4FF)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp), // ✅ Optimized padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section
            Spacer(modifier = Modifier.height(16.dp))

            // Main word - centered
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = frontText,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 32.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // TTS Button for English words
                if (studyDirection == com.hocalingo.app.core.database.entities.StudyDirection.EN_TO_TR) {
                    IconButton(
                        onClick = onPlayPronunciation,
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = "Play pronunciation",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Example sentence
                val exampleToShow = when (studyDirection) {
                    com.hocalingo.app.core.database.entities.StudyDirection.EN_TO_TR -> concept.exampleEn
                    com.hocalingo.app.core.database.entities.StudyDirection.TR_TO_EN -> concept.exampleTr
                }

                exampleToShow?.let { example ->
                    if (example.isNotEmpty()) {
                        Text(
                            text = example,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // Bottom section - Tap to flip hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = 90f },
                    tint = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap to flip",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ✅ OPTIMIZED: CardBack with adjusted padding
@Composable
private fun CardBack(
    concept: ConceptEntity,
    studyDirection: com.hocalingo.app.core.database.entities.StudyDirection,
    modifier: Modifier = Modifier
) {
    val backText = when (studyDirection) {
        com.hocalingo.app.core.database.entities.StudyDirection.EN_TO_TR -> concept.turkish
        com.hocalingo.app.core.database.entities.StudyDirection.TR_TO_EN -> concept.english
    }

    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF00D4FF)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp), // ✅ Optimized padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top spacer
            Spacer(modifier = Modifier.height(16.dp))

            // Main translation - centered
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = backText,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    fontSize = 32.sp
                )
            }

            // Bottom section - Tap to flip hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = -90f },
                    tint = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap to flip",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ✅ OPTIMIZED: Smaller action buttons to give more space to card
@Composable
private fun ActionButtonsHorizontal(
    onEasyClick: () -> Unit,
    onMediumClick: () -> Unit,
    onHardClick: () -> Unit,
    easyTimeText: String,
    mediumTimeText: String,
    hardTimeText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Don't know - Red
        ActionButtonSquare(
            text = "✗",
            subText = "Don't know",
            timeText = if (hardTimeText.isNotEmpty()) hardTimeText else "",
            backgroundColor = Color(0xFFFF3B30),
            contentColor = Color.White,
            onClick = onHardClick,
            modifier = Modifier.weight(1f)
        )

        // Not sure - Orange
        ActionButtonSquare(
            text = "?",
            subText = "Not sure",
            timeText = if (mediumTimeText.isNotEmpty()) mediumTimeText else "",
            backgroundColor = Color(0xFFFF9500),
            contentColor = Color.White,
            onClick = onMediumClick,
            modifier = Modifier.weight(1f)
        )

        // I know - Green
        ActionButtonSquare(
            text = "✓",
            subText = "I know",
            timeText = if (easyTimeText.isNotEmpty()) easyTimeText else "",
            backgroundColor = Color(0xFF34C759),
            contentColor = Color.White,
            onClick = onEasyClick,
            modifier = Modifier.weight(1f)
        )
    }
}

// ✅ OPTIMIZED: Reduced button height for more card space
@Composable
private fun ActionButtonSquare(
    text: String,
    subText: String,
    timeText: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp) // ✅ REDUCED: from 120dp to 100dp
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = subText,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center,
                fontSize = 11.sp
            )
            if (timeText.isNotEmpty()) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun StudyEmptyState(
    onNavigateToWordSelection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Harika!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bugün için çalışacak kelimen kalmadı. Daha fazla kelime seçmek ister misin?",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onNavigateToWordSelection,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kelime Seç")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StudyScreenPreview() {
    HocaLingoTheme {
        StudyScreen(
            onNavigateBack = {},
            onNavigateToWordSelection = {}
        )
    }
}