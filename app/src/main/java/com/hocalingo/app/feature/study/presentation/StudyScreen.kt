package com.hocalingo.app.feature.study.presentation

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.core.ui.components.HocaErrorState
import com.hocalingo.app.core.ui.components.HocaLoadingIndicator
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import com.hocalingo.app.core.database.entities.ConceptEntity
import kotlinx.coroutines.flow.collectLatest
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
    val haptic = LocalHapticFeedback.current

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                StudyEffect.NavigateToHome -> onNavigateBack()
                StudyEffect.NavigateToWordSelection -> onNavigateToWordSelection()
                is StudyEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is StudyEffect.HapticFeedback -> {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                }
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            StudyTopAppBar(
                progress = uiState.progressPercentage / 100f,
                currentIndex = uiState.currentWordIndex,
                totalWords = uiState.totalWordsInQueue,
                onNavigateBack = { viewModel.onEvent(StudyEvent.NavigateBack) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    HocaLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center)
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
                        onEvent = viewModel::onEvent
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            .background(Color(0xFFF5F5F5)) // Background color ekledik
    ) {
        TopAppBar(
            title = {
                Text(
                    text = "Word Study",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Progress section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp), // Daha fazla bottom padding
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
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun StudyContent(
    uiState: StudyUiState,
    onEvent: (StudyEvent) -> Unit
) {
    val concept = uiState.currentConcept ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween // SpaceBetween kullanarak düzgün dağıtım
    ) {
        // Üst boşluk
        Spacer(modifier = Modifier.height(8.dp))

        // Main flip card - Ortada konumlanacak
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f), // Available space'i kullan
            contentAlignment = Alignment.Center
        ) {
            FlipCard(
                concept = concept,
                isFlipped = uiState.isCardFlipped,
                studyDirection = uiState.studyDirection,
                onFlip = { onEvent(StudyEvent.FlipCard) },
                onPlayPronunciation = { onEvent(StudyEvent.PlayPronunciation) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp) // %50 büyütüldü (320dp -> 480dp)
            )
        }

        // Action buttons - En alta yerleşecek
        Column {
            ActionButtonsHorizontal(
                onEasyClick = { onEvent(StudyEvent.EasyButtonPressed) },
                onMediumClick = { onEvent(StudyEvent.MediumButtonPressed) },
                onHardClick = { onEvent(StudyEvent.HardButtonPressed) },
                easyTimeText = uiState.easyTimeText,
                mediumTimeText = uiState.mediumTimeText,
                hardTimeText = uiState.hardTimeText,
                modifier = Modifier.fillMaxWidth()
            )

            // Bottom navigation için boşluk
            Spacer(modifier = Modifier.height(10.dp)) // Button spacer 10dp olarak ayarlandı
        }
    }
}

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
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Kartı daha uzun yapmak için azaltıldı (1.3f -> 1.1f)
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = frontText,
                style = MaterialTheme.typography.headlineMedium, // Biraz küçülttük
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Pronunciation
            if (concept.pronunciation?.isNotEmpty() == true) {
                Text(
                    text = "[${concept.pronunciation}]",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // TTS Button
            if (studyDirection == com.hocalingo.app.core.database.entities.StudyDirection.EN_TO_TR) {
                IconButton(
                    onClick = onPlayPronunciation,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.9f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Play pronunciation",
                        tint = Color(0xFF00D4FF)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Example sentence
            concept.exampleEn?.let { exampleEn ->
                val exampleToShow = when (studyDirection) {
                    com.hocalingo.app.core.database.entities.StudyDirection.EN_TO_TR -> exampleEn
                    com.hocalingo.app.core.database.entities.StudyDirection.TR_TO_EN -> concept.exampleTr
                }

                exampleToShow?.let { example ->
                    if (example.isNotEmpty()) {
                        Text(
                            text = example,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Tap to flip hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = 90f },
                    tint = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap to flip",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = backText,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Tap to flip hint
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = -90f },
                    tint = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tap to flip",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

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
            .padding(bottom = 16.dp), // Bottom padding ekledik
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

        // I know this - Green
        ActionButtonSquare(
            text = "✓",
            subText = "I know this",
            timeText = if (easyTimeText.isNotEmpty()) easyTimeText else "",
            backgroundColor = Color(0xFF34C759),
            contentColor = Color.White,
            onClick = onEasyClick,
            modifier = Modifier.weight(1f)
        )
    }
}

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
    Button(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(1.2f) // Biraz daha dikdörtgen
            .heightIn(min = 85.dp, max = 100.dp), // Min/max height
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subText,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = contentColor.copy(alpha = 0.9f),
                maxLines = 1
            )

            if (timeText.isNotEmpty()) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = contentColor.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1
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
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = "Tüm kelimeler öğrenildi!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Yeni kelimeler eklemek için kelime seçim sayfasına gidin.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onNavigateToWordSelection,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kelime Seç",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}