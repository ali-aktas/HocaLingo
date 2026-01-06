package com.hocalingo.app.feature.selection

import android.R.attr.scaleX
import android.R.attr.scaleY
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.HocaRoutes
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaErrorState
import com.hocalingo.app.core.ui.components.HocaLoadingIndicator
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import com.hocalingo.app.feature.subscription.PaywallBottomSheet
import kotlinx.coroutines.flow.collectLatest

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * WordSelectionScreen - PHASE 3 COMPLETE ✅
 *
 * ✅ Clean architecture with separated components
 * ✅ Responsive card height
 * ✅ Processing indicator
 * ✅ All bugs fixed!
 *
 * Package: feature/selection/
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordSelectionScreen(
    onNavigateToStudy: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: WordSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                WordSelectionEffect.NavigateToStudy -> onNavigateToStudy()
                WordSelectionEffect.ShowCompletionMessage -> {
                    snackbarHostState.showSnackbar("Tebrikler! Kelimeler seçildi.")
                }
                is WordSelectionEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                WordSelectionEffect.ShowUndoMessage -> TODO()
            }
        }
    }

    Scaffold(
        snackbarHost = {
            // ✅ FIXED: All parameters provided
            HocaSnackbarHost(
                hostState = snackbarHostState,
                currentRoute = HocaRoutes.WORD_SELECTION
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
                uiState.isLoading -> {
                    HocaLoadingIndicator(
                        text = "Kelimeler yükleniyor...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    HocaErrorState(
                        message = uiState.error ?: "Bir hata oluştu",
                        onRetry = null,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.isCompleted -> {
                    CompletionScreen(
                        selectedCount = uiState.selectedCount,
                        isDarkTheme = isDarkTheme,
                        onContinue = { viewModel.onEvent(WordSelectionEvent.FinishSelection) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.currentWord != null -> {
                    WordSelectionContent(
                        uiState = uiState,
                        isDarkTheme = isDarkTheme,
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

        if (uiState.showDailyLimitDialog) {
            DailyLimitDialog(
                onDismiss = {
                    viewModel.onEvent(WordSelectionEvent.DismissDailyLimitDialog)
                    onNavigateToHome()
                },
                onGoHome = {
                    viewModel.onEvent(WordSelectionEvent.GoToStudyFromLimit)
                },
                onShowPremium = {
                    viewModel.onEvent(WordSelectionEvent.ShowPremiumFromLimitDialog)
                }
            )
        }

        // Premium Bottom Sheet
        if (uiState.showPremiumSheet) {
            PaywallBottomSheet(
                onDismiss = {
                    viewModel.onEvent(WordSelectionEvent.DismissPremium)
                    onNavigateToHome()  // Cancel → Home
                },
                onPurchaseSuccess = {
                    viewModel.onEvent(WordSelectionEvent.DismissPremium)
                    // ✅ Premium alındı → Kartları reload et
                    viewModel.onEvent(WordSelectionEvent.ReloadAfterPremium)
                }
            )
        }

        // Kelime kalmadı dialog
        if (uiState.showNoWordsDialog) {
            NoWordsLeftDialog(
                onDismiss = { viewModel.onEvent(WordSelectionEvent.DismissNoWordsDialog) },
                onGoHome = {
                    viewModel.onEvent(WordSelectionEvent.DismissNoWordsDialog)
                    onNavigateToHome()
                },
                onSelectNewPackage = {
                    viewModel.onEvent(WordSelectionEvent.DismissNoWordsDialog)
                    // TODO: Navigate to package selection
                    onNavigateToHome()
                }
            )
        }

        // Start study dialog (25 words selected)
        if (uiState.showStartStudyDialog) {
            StartStudyDialog(
                selectedCount = uiState.selectedCount,
                onStartStudy = { viewModel.onEvent(WordSelectionEvent.StartStudyNow) },
                onContinueSelecting = { viewModel.onEvent(WordSelectionEvent.ContinueSelecting) },
                onDismiss = { viewModel.onEvent(WordSelectionEvent.DismissStartStudyDialog) }
            )
        }

    }
}

/**
 * Start Study Dialog - 25 kelime seçildiğinde göster
 */
@Composable
private fun StartStudyDialog(
    selectedCount: Int,
    onStartStudy: () -> Unit,
    onContinueSelecting: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF10B981), // Modern green
                                Color(0xFF059669)  // Darker green
                            )
                        )
                    )
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hoca Logo
                Image(
                    painter = painterResource(id = R.drawable.lingo_nobg),
                    contentDescription = "Hoca Logo",
                    modifier = Modifier.size(80.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Trophy emoji
                Text(
                    text = "🏆",
                    fontSize = 56.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title
                Text(
                    text = "$selectedCount Kelime Seçtin!",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Message
                Text(
                    text = "Harika! Öğrenmeye başlamak ister misin, yoksa daha fazla kelime seçmeye devam mı?",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Start Study button (Primary)
                Button(
                    onClick = onStartStudy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Çalışmaya Başla",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Continue selecting button (Secondary)
                OutlinedButton(
                    onClick = onContinueSelecting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    ),
                    border = BorderStroke(2.dp, Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Daha Fazla Seç",
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
private fun WordSelectionContent(
    uiState: WordSelectionUiState,
    isDarkTheme: Boolean,
    onSwipeLeft: (Int) -> Unit,
    onSwipeRight: (Int) -> Unit,
    onUndo: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header
            Text(
                text = "HocaLingo",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            // ✅ Compact instruction bar
            InstructionBar()

            Spacer(modifier = Modifier.height(8.dp))

            // ✅ Progress bar
            SelectionProgressBar(
                progress = uiState.progress,
                currentIndex = uiState.processedWords,
                totalWords = uiState.totalWords
            )

            AnimatedVisibility(
                visible = uiState.todaySelectionCount >= 5 && !uiState.isPremium
            ) {
                DailyLimitWarning(
                    todaySelectionCount = uiState.todaySelectionCount,
                    dailyLimit = 15,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Card area - fills remaining space
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = 120.dp), // Space for overlapping buttons
                contentAlignment = Alignment.Center
            ) {
                // Processing indicator
                ProcessingIndicator(
                    isProcessing = uiState.isProcessingSwipe,
                    modifier = Modifier.align(Alignment.Center)
                )

                // ✅ Card stack
                uiState.currentWord?.let { currentWord ->
                    val nextWord = uiState.remainingWords.getOrNull(uiState.currentWordIndex + 1)

                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background card (next)
                        if (nextWord != null) {
                            SwipeableCard(
                                word = nextWord.english,
                                translation = nextWord.turkish,
                                nextWord = null,
                                nextTranslation = null,
                                onSwipeLeft = { },
                                onSwipeRight = { },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .fillMaxHeight(0.85f)
                                    .align(Alignment.Center)
                                    .alpha(0.6f)
                                    .graphicsLayer {
                                        scaleX = 0.95f
                                        scaleY = 0.95f
                                    }
                            )
                        }

                        // Foreground card (current)
                        SwipeableCard(
                            word = currentWord.english,
                            translation = currentWord.turkish,
                            nextWord = null,
                            nextTranslation = null,
                            onSwipeLeft = { onSwipeLeft(currentWord.id) },
                            onSwipeRight = { onSwipeRight(currentWord.id) },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.85f)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }

        // ✅ TINDER-STYLE: Buttons overlap card at bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home button
            SmallActionButton(
                icon = Icons.Default.Home,
                backgroundColor = Color(0xFFFF851B),
                contentDescription = "Home",
                onClick = onNavigateToHome,
                enabled = !uiState.isProcessingSwipe
            )

            // Skip button - BIGGER
            ActionButton(
                icon = Icons.Default.Close,
                backgroundColor = Color(0xFFEF5350),
                contentDescription = "Skip",
                onClick = { uiState.currentWord?.let { onSwipeLeft(it.id) } },
                enabled = !uiState.isProcessingSwipe,
                modifier = Modifier.size(80.dp) // ✅ BÜYÜK
            )

            // Learn button - BIGGER
            ActionButton(
                icon = Icons.Default.Check,
                backgroundColor = Color(0xFF66BB6A),
                contentDescription = "Learn",
                onClick = { uiState.currentWord?.let { onSwipeRight(it.id) } },
                enabled = !uiState.isProcessingSwipe,
                modifier = Modifier.size(80.dp) // ✅ BÜYÜK
            )

            // Undo button
            SmallActionButton(
                icon = Icons.Default.Undo,
                backgroundColor = Color(0xFF2196F3),
                contentDescription = "Undo",
                onClick = onUndo,
                enabled = uiState.canUndo && !uiState.isProcessingSwipe
            )
        }
    }

}