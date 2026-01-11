package com.hocalingo.app.feature.study

import android.annotation.SuppressLint
import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.ads.nativead.NativeAd
import com.hocalingo.app.HocaRoutes
import com.hocalingo.app.core.feedback.FeedbackDialog
import com.hocalingo.app.core.feedback.SatisfactionDialog
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.feature.subscription.PaywallBottomSheet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * StudyScreen - Main Study Screen Component
 *
 * Package: feature/study/
 *
 * Responsibilities:
 * - State management via ViewModel
 * - Effect handling (navigation, snackbars, TTS)
 * - Scaffold & layout structure
 * - Ad lifecycle management
 * - Rating dialog coordination
 *
 * Architecture: MVVM + MVI
 * - UiState: Immutable state container
 * - Events: User actions
 * - Effects: One-time side effects
 */
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
    val scope = rememberCoroutineScope()

    val nativeAd by viewModel.premiumAwareNativeAd.collectAsState()
    var showRewardedAdDialog by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }

    // Effect Handler - Centralized side effect management
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                StudyEffect.NavigateToHome -> onNavigateBack()
                StudyEffect.NavigateToWordSelection -> onNavigateToWordSelection()
                is StudyEffect.ShowMessage -> snackbarHostState.showSnackbar(effect.message)
                StudyEffect.LaunchNativeStoreRating -> {
                    activity?.let { viewModel.getRatingManager().launchNativeRating(it) }
                }
                StudyEffect.ShowStudyRewardedAd -> {
                    showRewardedAdDialog = true
                }
                is StudyEffect.SpeakText -> { /* Handled in ViewModel */ }
                is StudyEffect.HapticFeedback -> { /* TODO: Implement haptic feedback */ }
                is StudyEffect.PlaySound -> {
                    // Already handled in ViewModel via SoundEffectManager
                    // No additional UI action needed
                }
                is StudyEffect.ShowSessionComplete -> { /* Handled via UI state */ }
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
                uiState.isLoading -> {
                    LoadingState(modifier = Modifier.fillMaxSize())
                }

                uiState.showEmptyQueueMessage || uiState.isQueueEmpty -> {
                    StudyCompletionScreen(
                        onNavigateToWordSelection = onNavigateToWordSelection,
                        onNavigateToHome = onNavigateBack,
                        onNavigateToPremium = { showPaywall = true }
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
                        onNavigateBack = onNavigateBack,
                        nativeAd = nativeAd
                    )
                }

                else -> LoadingState(modifier = Modifier.fillMaxSize())
            }

            // Rating Dialogs
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

            // Rewarded Ad Dialog (don't show on completion screen)
            if (showRewardedAdDialog && !uiState.showEmptyQueueMessage) {
                StudyRewardedAdDialog(
                    wordsCompleted = 50,
                    onContinue = {
                        showRewardedAdDialog = false
                        activity?.let { act ->
                            scope.launch {
                                viewModel.getAdMobManager().showStudyRewardedAd(
                                    activity = act as Activity,
                                    onAdShown = {},
                                    onAdDismissed = {
                                        viewModel.onEvent(StudyEvent.ContinueAfterAd)
                                    },
                                    onAdFailed = { error ->
                                        showRewardedAdDialog = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Reklam yüklenemedi, devam ediyorsunuz")
                                        }
                                        viewModel.onEvent(StudyEvent.ContinueAfterAd)
                                    }
                                )
                            }
                        }
                    },
                    onUpgradeToPremium = { // ✅ YENİ PARAMETRE
                        showRewardedAdDialog = false
                        showPaywall = true
                    },
                    onDismiss = {
                        showRewardedAdDialog = false
                        viewModel.onEvent(StudyEvent.ContinueAfterAd)
                    }
                )
            }

            // ✅ Paywall BottomSheet
            if (showPaywall) {
                PaywallBottomSheet(
                    onDismiss = {
                        showPaywall = false
                        // Only show ad dialog if it was already showing
                        if (showRewardedAdDialog) {
                            showRewardedAdDialog = true
                        }
                    },
                    onPurchaseSuccess = {
                        showPaywall = false
                        // Premium purchased
                        if (showRewardedAdDialog) {
                            // Continue study if ad dialog was showing
                            viewModel.onEvent(StudyEvent.ContinueAfterAd)
                        }
                    }
                )
            }

        }
    }

}

/**
 * StudyContent - Main study interface layout
 *
 * Layout structure:
 * - Top bar with navigation
 * - Progress indicator
 * - Study card (or native ad overlay)
 * - Action buttons
 */
@Composable
private fun StudyContent(
    uiState: StudyUiState,
    onEvent: (StudyEvent) -> Unit,
    onNavigateBack: () -> Unit,
    nativeAd: NativeAd?
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top Navigation Bar
            StudyTopBar(
                onBackClick = onNavigateBack,
                onSettingsClick = { /* TODO: Settings */ }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Progress Indicator
            StudyProgressIndicator(
                currentIndex = uiState.currentCardIndex,
                totalWords = uiState.totalWordsInQueue,
                progress = uiState.progressPercentage / 100f
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Study Card (hide if native ad is showing)
            if (!uiState.showNativeAd || nativeAd == null) {
                StudyCard(
                    frontText = uiState.frontText,
                    backText = uiState.backText,
                    frontExampleText = uiState.frontExampleText,
                    backExampleText = uiState.backExampleText,
                    isFlipped = uiState.isCardFlipped,
                    onCardClick = { onEvent(StudyEvent.FlipCard) },
                    onPronunciationClick = { onEvent(StudyEvent.PlayPronunciation) },
                    showPronunciationButton = uiState.shouldShowTtsButton,
                    showTtsOnFrontSide = uiState.showTtsOnFrontSide,
                    cardColor = uiState.currentCardColor,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            if (!uiState.showNativeAd || nativeAd == null) {
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

        // Native Ad Overlay (positioned over the card)
        if (uiState.showNativeAd && nativeAd != null) {
            StudyNativeAdOverlay(
                nativeAd = nativeAd,
                onClose = { onEvent(StudyEvent.CloseNativeAd) },
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.Center)
                    .padding(16.dp)
            )
        }
    }



}