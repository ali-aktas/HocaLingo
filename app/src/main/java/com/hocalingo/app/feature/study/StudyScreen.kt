package com.hocalingo.app.feature.study

import android.annotation.SuppressLint
import android.app.Activity
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
import com.hocalingo.app.HocaRoutes
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import com.hocalingo.app.core.feedback.SatisfactionDialog
import com.hocalingo.app.core.feedback.FeedbackDialog
import androidx.activity.compose.LocalActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import com.hocalingo.app.core.ads.NativeAdLoader
import com.hocalingo.app.core.ads.NativeAdCard

/**
 * StudyScreen - Complete Enhanced Version with AdMob & Dark Mode
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/study/
 *
 * ✅ TTS Effect handling fixed
 * ✅ Parameter consistency fixed
 * ✅ All components working properly
 * ✅ AdMob rewarded ad integration
 * ✅ Dark mode support (MaterialTheme.colorScheme)
 */

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

// Modern card colors (20 şık renk - beyaz/gri/sarı hariç)
private val cardColors = listOf(
    Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFEF4444),
    Color(0xFFF97316), Color(0xFF10B981), Color(0xFF06B6D4), Color(0xFF3B82F6),
    Color(0xFF8B5A2B), Color(0xFF059669), Color(0xFF7C3AED), Color(0xFFDC2626),
    Color(0xFF0891B2), Color(0xFF065F46), Color(0xFF7C2D12), Color(0xFF1E40AF),
    Color(0xFF7E22CE), Color(0xFF0F766E), Color(0xFFA21CAF), Color(0xFF9A3412)
)

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    onNavigateBack: () -> Unit,
    onNavigateToWordSelection: () -> Unit = {},
    viewModel: StudyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalActivity.current
    val scope = rememberCoroutineScope() // ✅ Coroutine scope for suspend functions

    // ✅ BURAYA EKLE - Native Ad Loader
    val nativeAdLoader: NativeAdLoader = hiltViewModel()
    val nativeAd by nativeAdLoader.studyScreenAd.collectAsState()
    val isPremium by remember {
        // Premium check - if you have subscription repository
        mutableStateOf(false) // Şimdilik false, premium entegrasyonu varsa değiştir
    }

    // ✅ Rewarded Ad Dialog State
    var showRewardedAdDialog by remember { mutableStateOf(false) }

    // ✅ Handle ALL effects including rewarded ad
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                StudyEffect.NavigateToHome -> onNavigateBack()
                StudyEffect.NavigateToWordSelection -> onNavigateToWordSelection()
                is StudyEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                is StudyEffect.SpeakText -> { /* TTS handled in ViewModel */ }
                is StudyEffect.HapticFeedback -> { /* TODO: Implement haptic */ }
                is StudyEffect.PlaySound -> { /* TODO: Implement sound */ }
                is StudyEffect.ShowSessionComplete -> { /* Handled via UI state */ }
                StudyEffect.LaunchNativeStoreRating -> {
                    activity?.let { viewModel.getRatingManager().launchNativeRating(it) }
                }
                // ✅ NEW: Rewarded Ad Effect
                StudyEffect.ShowStudyRewardedAd -> {
                    showRewardedAdDialog = true
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            HocaSnackbarHost(
                hostState = snackbarHostState,
                currentRoute = HocaRoutes.STUDY
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> LoadingState(modifier = Modifier.fillMaxSize())
                uiState.showEmptyQueueMessage || uiState.isQueueEmpty -> {
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
                uiState.currentConcept != null -> {
                    StudyContent(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        onNavigateBack = onNavigateBack
                    )
                }
                else -> LoadingState(modifier = Modifier.fillMaxSize())
            }

            // ========== RATING DIALOGS ==========
            if (uiState.showSatisfactionDialog) {
                SatisfactionDialog(
                    onDismiss = { viewModel.onEvent(StudyEvent.DismissSatisfactionDialog) },
                    onSatisfactionSelected = { level ->
                        viewModel.onEvent(StudyEvent.SatisfactionSelected(level))
                    }
                )
            }

            if (uiState.showFeedbackDialog) {
                val satisfactionLevel = uiState.selectedSatisfactionLevel
                if (satisfactionLevel != null) {
                    FeedbackDialog(
                        satisfactionLevel = satisfactionLevel,
                        onDismiss = { viewModel.onEvent(StudyEvent.DismissFeedbackDialog) },
                        onSubmit = { category, message, email ->
                            viewModel.onEvent(
                                StudyEvent.SubmitFeedback(
                                    category = category,
                                    message = message,
                                    email = email
                                )
                            )
                        }
                    )
                }
            }

            // Native Ad
            if (nativeAd != null && uiState.currentConcept != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(bottom = 100.dp)
                ) {
                    NativeAdCard(
                        nativeAd = nativeAd,
                        cardHeight = 120
                    )
                }
            }

            // ✅ REWARDED AD DIALOG
            if (showRewardedAdDialog) {
                StudyRewardedAdDialog(
                    wordsCompleted = 25,
                    onContinue = {
                        showRewardedAdDialog = false
                        activity?.let { act ->
                            scope.launch { // ✅ Launch coroutine for suspend function
                                viewModel.getAdMobManager().showStudyRewardedAd(
                                    activity = act as Activity,
                                    onAdShown = {},
                                    onAdDismissed = {
                                        viewModel.onEvent(StudyEvent.ContinueAfterAd)
                                    },
                                    onAdFailed = { error ->
                                        scope.launch { // ✅ Launch coroutine for snackbar
                                            snackbarHostState.showSnackbar("Reklam gösterilemedi: $error")
                                        }
                                        viewModel.onEvent(StudyEvent.ContinueAfterAd)
                                    }
                                )
                            }
                        }
                    },
                    onDismiss = {
                        showRewardedAdDialog = false
                        viewModel.onEvent(StudyEvent.ContinueAfterAd)
                    }
                )
            }
        }
    }
}

/**
 * ✅ Rewarded Ad Success Dialog
 */
@Composable
private fun StudyRewardedAdDialog(
    wordsCompleted: Int,
    onContinue: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎉",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Harika İş!",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Text(
                text = "$wordsCompleted kelime tamamladın!\nÖğrenmeye devam etmek için reklam izle.",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Devam Et",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "İptal",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(20.dp)
    )
}

/**
 * Study Content - Main learning interface
 */
@Composable
private fun StudyContent(
    uiState: StudyUiState,
    onEvent: (StudyEvent) -> Unit,
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Çalışma",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        StudyProgressIndicator(
            currentIndex = uiState.currentWordIndex,
            totalWords = uiState.totalWordsInQueue,
            progress = uiState.progressPercentage / 100f
        )

        Spacer(modifier = Modifier.height(16.dp))

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

        StudyActionButtons(
            isCardFlipped = uiState.isCardFlipped,
            easyTimeText = uiState.easyTimeText,
            mediumTimeText = uiState.mediumTimeText,
            hardTimeText = uiState.hardTimeText,
            onHardPressed = { onEvent(StudyEvent.HardButtonPressed) },
            onMediumPressed = { onEvent(StudyEvent.MediumButtonPressed) },
            onEasyPressed = { onEvent(StudyEvent.EasyButtonPressed) }
        )
    }
}

/**
 * Enhanced Study Completion Screen
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
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
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
            Text(text = "🎉", fontSize = 64.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Harika İş Çıkardın!",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Bugün tüm kelimeleri çalıştın.\nÖğrenmeye devam etmek için yeni kelimeler seç!",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(16.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF4ECDC4),
                trackColor = MaterialTheme.colorScheme.surfaceVariant
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
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onCardClick() }
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 12f * density
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotationY <= 90f) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = frontText,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        lineHeight = 36.sp
                    )
                    if (frontExampleText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = frontExampleText,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
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
                Box(modifier = Modifier.graphicsLayer { scaleX = -1f }) {
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
    onHardPressed: () -> Unit,
    onMediumPressed: () -> Unit,
    onEasyPressed: () -> Unit
) {
    if (!isCardFlipped) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Kartı çevirmek için dokunun",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ActionButton(
                mainText = "ZOR",
                timeText = hardTimeText.ifEmpty { "1 dk" },
                backgroundColor = Color(0xFFFF3B30),
                contentColor = Color.White,
                onClick = onHardPressed,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                mainText = "ORTA",
                timeText = mediumTimeText.ifEmpty { "10 dk" },
                backgroundColor = Color(0xFFFF9500),
                contentColor = Color.White,
                onClick = onMediumPressed,
                modifier = Modifier.weight(1f)
            )
            ActionButton(
                mainText = "KOLAY",
                timeText = easyTimeText.ifEmpty { "1 gün" },
                backgroundColor = Color(0xFF34C759),
                contentColor = Color.White,
                onClick = onEasyPressed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ActionButton(
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
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = mainText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = contentColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = timeText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = contentColor.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color(0xFF4ECDC4))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Çalışma hazırlanıyor...",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        Text(text = "⚠️", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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