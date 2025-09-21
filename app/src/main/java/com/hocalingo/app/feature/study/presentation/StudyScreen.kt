package com.hocalingo.app.feature.study.presentation

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.absoluteValue

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

// Modern card colors (10 şık renk - beyaz/gri/sarı hariç)
private val cardColors = listOf(
    Color(0xFF6366F1), // Indigo
    Color(0xFF8B5CF6), // Purple
    Color(0xFFEC4899), // Pink
    Color(0xFFEF4444), // Red
    Color(0xFFF97316), // Orange
    Color(0xFF10B981), // Emerald
    Color(0xFF06B6D4), // Cyan
    Color(0xFF3B82F6), // Blue
    Color(0xFF8B5A2B), // Brown
    Color(0xFF059669)  // Green
)

/**
 * Modern Study Screen - Enhanced Completion UI
 * Improved completion state with motivational design
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWordSelection: () -> Unit = {},
    viewModel: StudyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                StudyEffect.NavigateToHome -> onNavigateBack()
                StudyEffect.NavigateToWordSelection -> onNavigateToWordSelection()
                is StudyEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                else -> {
                    // Handle other effects (TTS, Haptic, etc.)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFA) // Light background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }
                uiState.showEmptyQueueMessage -> {
                    // ENHANCED: Beautiful completion screen
                    StudyCompletionScreen(
                        onNavigateToWordSelection = onNavigateToWordSelection,
                        onNavigateToHome = onNavigateBack
                    )
                }
                uiState.error != null -> {
                    ErrorState(
                        error = uiState.error ?: "",
                        onRetry = { viewModel.onEvent(StudyEvent.RetryLoading) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
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

/**
 * ENHANCED: Beautiful Study Completion Screen
 * Motivational design with card-based buttons
 */
@Composable
private fun StudyCompletionScreen(
    onNavigateToWordSelection: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Celebration animation area
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background circle
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4CAF50).copy(alpha = 0.2f),
                                Color(0xFF4CAF50).copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Celebration emoji
            Text(
                text = "🎉",
                fontSize = 64.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Main completion message
        Text(
            text = "Harika İş Çıkardın!",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Bugün tüm kelimeleri çalıştın.\nÖğrenmeye devam etmek için yeni kelimeler seç!",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = Color(0xFF6C7B8A),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Action buttons in equal cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Select new words card
            CompletionActionCard(
                title = "Yeni Kelimeler",
                subtitle = "Öğrenmeye devam et",
                icon = Icons.Filled.Add,
                backgroundColor = Brush.linearGradient(
                    colors = listOf(Color(0xFF4ECDC4), Color(0xFF44A08D))
                ),
                onClick = onNavigateToWordSelection,
                modifier = Modifier.weight(1f)
            )

            // Go to home card
            CompletionActionCard(
                title = "Ana Sayfa",
                subtitle = "İstatistikleri gör",
                icon = Icons.Filled.Home,
                backgroundColor = Brush.linearGradient(
                    colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
                ),
                onClick = onNavigateToHome,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Additional stats or motivational text
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CompletionStat(
                    icon = Icons.Filled.CheckCircle,
                    label = "Tamamlandı",
                    color = Color(0xFF4CAF50)
                )
                CompletionStat(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "Streak devam",
                    color = Color(0xFFFF5722)
                )
                CompletionStat(
                    icon = Icons.Filled.EmojiEvents,
                    label = "Hedef tamamlandı",
                    color = Color(0xFFFFC107)
                )
            }
        }
    }
}

@Composable
private fun CompletionActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundColor)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )

                Column {
                    Text(
                        text = title,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionStat(
    icon: ImageVector,
    label: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color(0xFF6C7B8A),
            textAlign = TextAlign.Center
        )
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar with back button and progress
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .background(
                        color = Color.White,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = Color(0xFF2C3E50)
                )
            }

            Text(
                text = "Çalışma",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF2C3E50)
            )

            // Placeholder for symmetry
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress indicator
        StudyProgressIndicator(
            currentIndex = uiState.currentWordIndex,
            totalWords = uiState.totalWordsInQueue,
            progress = uiState.progressPercentage / 100f
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Study card area
        StudyCard(
            frontText = uiState.frontText,
            backText = uiState.backText,
            frontExampleText = uiState.frontExampleText,
            backExampleText = uiState.backExampleText,
            isFlipped = uiState.isCardFlipped,
            onCardClick = { onEvent(StudyEvent.FlipCard) },
            onPronunciationClick = { onEvent(StudyEvent.PlayPronunciation) },
            showPronunciationButton = uiState.shouldShowTtsButton,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Action buttons
        StudyActionButtons(
            isCardFlipped = uiState.isCardFlipped,
            easyTimeText = uiState.easyTimeText,
            mediumTimeText = uiState.mediumTimeText,
            hardTimeText = uiState.hardTimeText,
            onHardClick = { onEvent(StudyEvent.HardButtonPressed) },
            onMediumClick = { onEvent(StudyEvent.MediumButtonPressed) },
            onEasyClick = { onEvent(StudyEvent.EasyButtonPressed) }
        )
    }
}

@Composable
private fun StudyProgressIndicator(
    currentIndex: Int,
    totalWords: Int,
    progress: Float
) {
    if (totalWords > 0) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${currentIndex + 1}/${totalWords}",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF6C7B8A)
            )

            Spacer(modifier = Modifier.width(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF4ECDC4),
                trackColor = Color(0xFFE0E0E0)
            )
        }
    }
}

@Composable
private fun StudyCard(
    frontText: String,
    backText: String,
    frontExampleText: String,
    backExampleText: String,
    isFlipped: Boolean,
    onCardClick: () -> Unit,
    onPronunciationClick: () -> Unit,
    showPronunciationButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Rastgele renk seçimi (kelime ID'sine göre sabit kalır)
    val cardColor = remember(frontText, backText) {
        cardColors[(frontText + backText).hashCode().absoluteValue % cardColors.size]
    }

    val rotationY by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "cardFlip"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                indication = null, // Ripple efektini kaldır
                interactionSource = remember { MutableInteractionSource() }
            ) { onCardClick() }
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor // Dinamik renk
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            // FIXED: Conditional rendering to avoid mirror text
            if (rotationY <= 90f) {
                // Ön yüz (0° - 90°)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = frontText,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = Color.White, // Yazı rengi beyaz
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )

                    // Ön yüz örnek cümle
                    if (frontExampleText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = frontExampleText,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f), // Hafif transparan beyaz
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                // Seslendirme butonu (sadece ön yüzde, sağ üst köşe)
                if (showPronunciationButton) {
                    IconButton(
                        onClick = onPronunciationClick,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = "Seslendirme",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                // FIXED: Arka yüz (90° - 180°) - ters görüntü düzeltildi
                Box(
                    modifier = Modifier.graphicsLayer {
                        // Arka yüz metnini tekrar ters çevir (ayna etkisini düzelt)
                        scaleX = -1f
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = backText,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 36.sp
                        )

                        // Arka yüz örnek cümle
                        if (backExampleText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = backExampleText,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyActionButtons(
    isCardFlipped: Boolean,
    easyTimeText: String,
    mediumTimeText: String,
    hardTimeText: String,
    onHardClick: () -> Unit,
    onMediumClick: () -> Unit,
    onEasyClick: () -> Unit
) {
    if (!isCardFlipped) {
        // Show flip instruction
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F5F5)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.TouchApp,
                    contentDescription = null,
                    tint = Color(0xFF6C7B8A),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kartı çevirmek için dokunun",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF6C7B8A)
                )
            }
        }
    } else {
        // FIXED: Güncellenenen action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Zor - Red
            ActionButtonUpdated(
                mainText = "ZOR",
                timeText = if (hardTimeText.isNotEmpty()) hardTimeText else "1 dk",
                backgroundColor = Color(0xFFFF3B30),
                contentColor = Color.White,
                onClick = onHardClick,
                modifier = Modifier.weight(1f)
            )

            // Orta - Orange
            ActionButtonUpdated(
                mainText = "ORTA",
                timeText = if (mediumTimeText.isNotEmpty()) mediumTimeText else "10 dk",
                backgroundColor = Color(0xFFFF9500),
                contentColor = Color.White,
                onClick = onMediumClick,
                modifier = Modifier.weight(1f)
            )

            // Kolay - Green
            ActionButtonUpdated(
                mainText = "KOLAY",
                timeText = if (easyTimeText.isNotEmpty()) easyTimeText else "1 gün",
                backgroundColor = Color(0xFF34C759),
                contentColor = Color.White,
                onClick = onEasyClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// Yeni buton tasarımı
@Composable
private fun ActionButtonUpdated(
    mainText: String,
    timeText: String,
    backgroundColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .height(80.dp), // Biraz daha yüksek
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Ana metin - büyük ve kalın
            Text(
                text = mainText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium, // Poppins Medium
                fontSize = 16.sp, // Büyük
                color = contentColor,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Süre metni - küçük ve ince
            Text(
                text = timeText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal, // İnce
                fontSize = 11.sp, // Küçük
                color = contentColor.copy(alpha = 0.8f), // Hafif transparan
                textAlign = TextAlign.Center
            )
        }
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
    Card(
        modifier = modifier
            .height(100.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
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
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = contentColor
            )
            Text(
                text = subText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = contentColor.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
            if (timeText.isNotEmpty()) {
                Text(
                    text = timeText,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = Color(0xFF4ECDC4)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Çalışma hazırlanıyor...",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = Color(0xFF6C7B8A)
            )
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = Color(0xFF6C7B8A),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Tekrar Dene")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun StudyCompletionScreenPreview() {
    HocaLingoTheme {
        StudyCompletionScreen(
            onNavigateToWordSelection = {},
            onNavigateToHome = {}
        )
    }
}