package com.hocalingo.app.feature.selection

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.HocaRoutes
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaErrorState
import com.hocalingo.app.core.ui.components.HocaLoadingIndicator
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.ThemeViewModel
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
                WordSelectionEffect.ShowUndoMessage -> {
                    snackbarHostState.showSnackbar("Geri alındı")
                }
                is WordSelectionEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
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
    // ✅ PHASE 3: Responsive card height
    val (cardHeight, cardContainerHeight) = calculateOptimalCardHeight()

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Header
        Text(
            text = "Hocalingo",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        // ✅ PHASE 3: Compact instruction bar
        InstructionBar()

        // ✅ PHASE 3: Daily limit warning
        DailyLimitWarning(
            todaySelectionCount = uiState.todaySelectionCount,
            dailyLimit = 50
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ✅ PHASE 3: Progress bar
        SelectionProgressBar(
            progress = uiState.progress,
            currentIndex = uiState.processedWords,
            totalWords = uiState.totalWords
        )

        Spacer(modifier = Modifier.weight(1f))

        // ✅ PHASE 3: Card container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardContainerHeight),
            contentAlignment = Alignment.Center
        ) {
            // Processing indicator
            ProcessingIndicator(
                isProcessing = uiState.isProcessingSwipe,
                modifier = Modifier.align(Alignment.Center)
            )

            // Current word card
            uiState.currentWord?.let { currentWord ->
                val nextWord = uiState.remainingWords.getOrNull(uiState.currentWordIndex + 1)

                SwipeableCard(
                    word = currentWord.english,
                    translation = currentWord.turkish,
                    // ✅ FIXED: exampleSentence → exampleEn
                    example = currentWord.exampleEn,
                    nextWord = nextWord?.english,
                    nextTranslation = nextWord?.turkish,
                    onSwipeLeft = { onSwipeLeft(currentWord.id) },
                    onSwipeRight = { onSwipeRight(currentWord.id) },
                    modifier = Modifier.height(cardHeight)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp), // ✅ 32dp → 24dp (4 buton için)
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ YENİ: Home button (solda)
            SmallActionButton(
                icon = Icons.Default.Home,
                backgroundColor = Color(0xFF9C27B0), // Purple
                contentDescription = "Home",
                onClick = onNavigateToHome,
                enabled = !uiState.isProcessingSwipe
            )

            // Skip button
            ActionButton(
                icon = Icons.Default.Close,
                label = "Geç",
                backgroundColor = if (isDarkTheme) Color(0xFFEF5350) else Color(0xFFEF5350),
                contentDescription = "Skip",
                onClick = {
                    uiState.currentWord?.let { word ->
                        onSwipeLeft(word.id)
                    }
                },
                enabled = !uiState.isProcessingSwipe
            )

            // Learn button
            ActionButton(
                icon = Icons.Default.Check,
                label = "Öğren",
                backgroundColor = if (isDarkTheme) Color(0xFF66BB6A) else Color(0xFF66BB6A),
                contentDescription = "Learn",
                onClick = {
                    uiState.currentWord?.let { word ->
                        onSwipeRight(word.id)
                    }
                },
                enabled = !uiState.isProcessingSwipe
            )

            // Undo button (sağda)
            SmallActionButton(
                icon = Icons.Default.Undo,
                backgroundColor = if (isDarkTheme) Color(0xFF2196F3) else Color(0xFF2196F3),
                contentDescription = "Undo",
                onClick = onUndo,
                enabled = uiState.canUndo && !uiState.isProcessingSwipe
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}