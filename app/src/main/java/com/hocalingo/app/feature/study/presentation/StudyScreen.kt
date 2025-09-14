package com.hocalingo.app.feature.study.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaErrorState
import com.hocalingo.app.core.ui.components.HocaLoadingIndicator
import com.hocalingo.app.core.ui.components.HocaProgressCard
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import com.hocalingo.app.core.ui.theme.easyGreen
import com.hocalingo.app.core.ui.theme.hardRed
import com.hocalingo.app.core.ui.theme.mediumYellow
import kotlinx.coroutines.flow.collectLatest

/**
 * Study Screen - Main Learning Interface
 *
 * Features:
 * - Flip card animation with smooth 3D effects
 * - SM-2 algorithm integration with dynamic button text
 * - TTS pronunciation support
 * - Haptic feedback and sound effects
 * - Progress tracking and session management
 * - Empty state handling
 */
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
                    // Handle haptic feedback based on type
                    when (effect.type) {
                        HapticType.LIGHT -> haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        HapticType.SUCCESS -> haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        HapticType.ERROR -> haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        else -> {}
                    }
                }

                is StudyEffect.ShowSessionComplete -> {
                    snackbarHostState.showSnackbar(
                        "🎉 Oturum tamamlandı! ${effect.stats.wordsStudied} kelime çalışıldı."
                    )
                }

                // Note: TTS and sound effects would be handled in actual implementation
                is StudyEffect.SpeakText -> {}
                is StudyEffect.PlaySound -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            StudyTopAppBar(
                progress = uiState.progressPercentage,
                currentIndex = uiState.currentWordIndex,
                totalWords = uiState.totalWordsInQueue,
                onNavigateBack = { viewModel.onEvent(StudyEvent.NavigateBack) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
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
                        onRetry = { viewModel.onEvent(event = StudyEvent.RetryLoading) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.showEmptyQueueMessage -> {
                    StudyEmptyState(
                        onNavigateToWordSelection = { viewModel.onEvent(StudyEvent.NavigateToWordSelection) }
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
    TopAppBar(
        title = {
            Column {
                Text(
                    text = stringResource(R.string.study_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${currentIndex + 1} / $totalWords",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
}

@Composable
private fun StudyContent(
    uiState: StudyUiState,
    onEvent: (StudyEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Progress section
        StudyProgressSection(
            dailyProgress = uiState.dailyProgressPercentage,
            wordsStudiedToday = uiState.wordsStudiedToday,
            dailyGoal = uiState.dailyGoal,
            sessionAccuracy = uiState.accuracyPercentage
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main study card (flip card)
        StudyFlipCard(
            uiState = uiState,
            onFlipCard = { onEvent(StudyEvent.FlipCard) },
            onPlayPronunciation = { onEvent(StudyEvent.PlayPronunciation) },
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Response buttons (Easy, Medium, Hard)
        StudyResponseButtons(
            easyText = uiState.easyButtonText,
            easyTimeText = uiState.easyTimeText,
            mediumText = uiState.mediumButtonText,
            mediumTimeText = uiState.mediumTimeText,
            hardText = uiState.hardButtonText,
            hardTimeText = uiState.hardTimeText,
            isCardFlipped = uiState.isCardFlipped,
            onEasyClick = { onEvent(StudyEvent.EasyButtonPressed) },
            onMediumClick = { onEvent(StudyEvent.MediumButtonPressed) },
            onHardClick = { onEvent(StudyEvent.HardButtonPressed) }
        )
    }
}

@Composable
private fun StudyProgressSection(
    dailyProgress: Float,
    wordsStudiedToday: Int,
    dailyGoal: Int,
    sessionAccuracy: Float
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Daily progress card
        HocaProgressCard(
            title = "Günlük Hedef",
            value = "$wordsStudiedToday",
            subtitle = "/ $dailyGoal kelime",
            backgroundColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.weight(1f)
        )

        // Session accuracy card
        if (sessionAccuracy > 0) {
            HocaProgressCard(
                title = "Doğruluk",
                value = "${sessionAccuracy.toInt()}%",
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StudyFlipCard(
    uiState: StudyUiState,
    onFlipCard: () -> Unit,
    onPlayPronunciation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFlipped = uiState.isCardFlipped

    // Flip animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_flip"
    )

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clickable { onFlipCard() },
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Front/Back content based on flip state
                if (rotation <= 90f) {
                    // Front side
                    StudyCardFront(
                        text = uiState.frontText,
                        onPlayPronunciation = onPlayPronunciation,
                        showTtsButton = !uiState.studyDirection.name.startsWith("TR")
                    )
                } else {
                    // Back side (rotated content)
                    Box(
                        modifier = Modifier.graphicsLayer { rotationY = 180f }
                    ) {
                        StudyCardBack(
                            text = uiState.backText,
                            exampleText = uiState.exampleText,
                            pronunciationText = uiState.pronunciationText
                        )
                    }
                }

                // Flip instruction
                if (!isFlipped) {
                    Text(
                        text = "Kartı çevirmek için dokun",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .alpha(0.6f),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyCardFront(
    text: String,
    onPlayPronunciation: () -> Unit,
    showTtsButton: Boolean
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // TTS button
        if (showTtsButton) {
            FloatingActionButton(
                onClick = onPlayPronunciation,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(48.dp),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Pronunciation",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun StudyCardBack(
    text: String,
    exampleText: String,
    pronunciationText: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Translation/meaning
        Text(
            text = text,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Pronunciation guide
        if (pronunciationText.isNotBlank()) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "/$pronunciationText/",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }

        // Example sentence
        if (exampleText.isNotBlank()) {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Örnek:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = exampleText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        textAlign = TextAlign.Start,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyResponseButtons(
    easyText: String,
    easyTimeText: String,
    mediumText: String,
    mediumTimeText: String,
    hardText: String,
    hardTimeText: String,
    isCardFlipped: Boolean,
    onEasyClick: () -> Unit,
    onMediumClick: () -> Unit,
    onHardClick: () -> Unit
) {
    // Show buttons only when card is flipped
    val buttonsAlpha by animateFloatAsState(
        targetValue = if (isCardFlipped) 1f else 0.3f,
        animationSpec = tween(300),
        label = "buttons_alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(buttonsAlpha)
    ) {
        Text(
            text = if (isCardFlipped) "Bu kelimeyi ne kadar iyi biliyorsun?" else "Önce kartı çevir",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Response buttons
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Hard button
            StudyResponseButton(
                text = hardText,
                timeText = hardTimeText,
                backgroundColor = MaterialTheme.colorScheme.hardRed,
                contentColor = Color.White,
                enabled = isCardFlipped,
                onClick = onHardClick
            )

            // Medium button
            StudyResponseButton(
                text = mediumText,
                timeText = mediumTimeText,
                backgroundColor = MaterialTheme.colorScheme.mediumYellow,
                contentColor = Color.Black,
                enabled = isCardFlipped,
                onClick = onMediumClick
            )

            // Easy button
            StudyResponseButton(
                text = easyText,
                timeText = easyTimeText,
                backgroundColor = MaterialTheme.colorScheme.easyGreen,
                contentColor = Color.White,
                enabled = isCardFlipped,
                onClick = onEasyClick
            )
        }
    }
}

@Composable
private fun StudyResponseButton(
    text: String,
    timeText: String,
    backgroundColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor,
            contentColor = contentColor,
            disabledContainerColor = backgroundColor.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor.copy(alpha = 0.9f)
            )
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
            text = "🎯",
            fontSize = 80.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Çalışılacak Kelime Yok",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Öğrenmek için yeni kelimeler seç veya daha sonra tekrar dene.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

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

@Preview(showBackground = true)
@Composable
private fun StudyScreenPreview() {
    HocaLingoTheme {
        // Mock data for preview
        StudyContent(
            uiState = StudyUiState(
                currentConcept = null,
                isCardFlipped = false,
                totalWordsInQueue = 10,
                currentWordIndex = 3,
                wordsStudiedToday = 15,
                dailyGoal = 20,
                sessionWordsCount = 5,
                correctAnswers = 4,
                easyTimeText = "3 gün sonra",
                mediumTimeText = "1 gün sonra",
                hardTimeText = "10 dakika sonra"
            ),
            onEvent = {}
        )
    }
}