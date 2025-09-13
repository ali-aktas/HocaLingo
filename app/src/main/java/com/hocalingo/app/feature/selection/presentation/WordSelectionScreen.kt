package com.hocalingo.app.feature.selection.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import com.hocalingo.app.feature.selection.components.SwipeableCard
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordSelectionScreen(
    onNavigateToStudy: () -> Unit,
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
                    snackbarHostState.showSnackbar(
                        "Tebrikler! Tüm kelimeler işlendi.",
                        duration = SnackbarDuration.Short
                    )
                }
                WordSelectionEffect.ShowUndoMessage -> {
                    snackbarHostState.showSnackbar(
                        "İşlem geri alındı",
                        duration = SnackbarDuration.Short
                    )
                }
                is WordSelectionEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            EnhancedTopAppBar(
                onFinish = { viewModel.onEvent(WordSelectionEvent.FinishSelection) }
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.canUndo,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                EnhancedFloatingActionButton(
                    onClick = { viewModel.onEvent(WordSelectionEvent.Undo) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.03f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            // Background decorative elements
            BackgroundDecorations()

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
                    EnhancedCompletionScreen(
                        selectedCount = uiState.selectedCount,
                        hiddenCount = uiState.hiddenCount,
                        onContinue = { viewModel.onEvent(WordSelectionEvent.FinishSelection) }
                    )
                }
                uiState.currentWord != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Enhanced progress section
                        EnhancedSelectionProgress(
                            selectedCount = uiState.selectedCount,
                            todayCount = uiState.todaySelectionCount,
                            progress = uiState.progress
                        )

                        // Swipeable card with enhanced presentation
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            this@Column.AnimatedVisibility(
                                visible = uiState.currentWord != null,
                                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                                exit = fadeOut() + scaleOut()
                            ) {
                                uiState.currentWord?.let { word ->
                                    SwipeableCard(
                                        word = word.english,
                                        translation = word.turkish,
                                        example = word.exampleEn,
                                        onSwipeLeft = {
                                            viewModel.onEvent(WordSelectionEvent.SwipeLeft(word.id))
                                        },
                                        onSwipeRight = {
                                            viewModel.onEvent(WordSelectionEvent.SwipeRight(word.id))
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }

                        // Enhanced action buttons
                        EnhancedActionButtons(
                            onPass = {
                                uiState.currentWord?.let { word ->
                                    viewModel.onEvent(WordSelectionEvent.SwipeLeft(word.id))
                                }
                            },
                            onLearn = {
                                uiState.currentWord?.let { word ->
                                    viewModel.onEvent(WordSelectionEvent.SwipeRight(word.id))
                                }
                            }
                        )
                    }
                }
                else -> {
                    // Enhanced debug/empty state
                    EnhancedEmptyState(
                        uiState = uiState,
                        onNavigateToStudy = onNavigateToStudy
                    )
                }
            }

            // Enhanced premium bottom sheet
            if (uiState.showPremiumSheet) {
                EnhancedPremiumLimitBottomSheet(
                    onDismiss = { viewModel.onEvent(WordSelectionEvent.DismissPremium) },
                    onContinue = onNavigateToStudy
                )
            }
        }
    }
}

@Composable
private fun BackgroundDecorations() {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    Box(modifier = Modifier.fillMaxSize()) {
        // Floating decorative circles
        repeat(3) { index ->
            val size = (60 + index * 20).dp
            val x = screenWidth * (0.2f + index * 0.3f)
            val y = (100 + index * 150).dp

            Box(
                modifier = Modifier
                    .size(size)
                    .offset(x = x, y = y)
                    .background(
                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f - index * 0.01f),
                        CircleShape
                    )
                    .blur((15 + index * 10).dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedTopAppBar(onFinish: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.word_selection_title),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall
            )
        },
        actions = {
            FilledTonalButton(
                onClick = onFinish,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = stringResource(R.string.finish),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun EnhancedFloatingActionButton(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        shape = CircleShape,
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 8.dp,
            pressedElevation = 12.dp
        )
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = stringResource(R.string.word_selection_undo),
            tint = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun EnhancedSelectionProgress(
    selectedCount: Int,
    todayCount: Int,
    progress: Float
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Progress visualization with gradient
            Text(
                text = "📊 İlerleme",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(6.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            ),
                            shape = RoundedCornerShape(6.dp)
                        )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Enhanced stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Selected count card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "✅",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.word_selection_selected),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$selectedCount",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Daily limit card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (todayCount >= 25)
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (todayCount >= 25) "⚠️" else "🎯",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.word_selection_daily_limit),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (todayCount >= 25)
                                MaterialTheme.colorScheme.onErrorContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(R.string.word_selection_daily_limit_format, todayCount, 25),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (todayCount >= 25)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EnhancedActionButtons(
    onPass: () -> Unit,
    onLearn: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Pass button with enhanced design
        Card(
            modifier = Modifier.size(80.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            ),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            FilledTonalButton(
                onClick = onPass,
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    text = "❌",
                    fontSize = 28.sp
                )
            }
        }

        // Learn button with enhanced design
        Card(
            modifier = Modifier.size(80.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            FilledTonalButton(
                onClick = onLearn,
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.Transparent
                )
            ) {
                Text(
                    text = "✅",
                    fontSize = 28.sp
                )
            }
        }
    }
}

@Composable
private fun EnhancedCompletionScreen(
    selectedCount: Int,
    hiddenCount: Int,
    onContinue: () -> Unit
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
            fontSize = 80.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.word_selection_completion_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.word_selection_completion_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Enhanced stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            EnhancedStatCard(
                emoji = "✅",
                count = selectedCount,
                label = stringResource(R.string.word_selection_learned),
                color = MaterialTheme.colorScheme.primaryContainer
            )
            EnhancedStatCard(
                emoji = "⏭️",
                count = hiddenCount,
                label = stringResource(R.string.word_selection_skipped),
                color = MaterialTheme.colorScheme.secondaryContainer
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = stringResource(R.string.word_selection_start_studying),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun EnhancedStatCard(
    emoji: String,
    count: Int,
    label: String,
    color: Color
) {
    Card(
        modifier = Modifier.size(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = color
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = emoji,
                fontSize = 32.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun EnhancedEmptyState(
    uiState: WordSelectionUiState,
    onNavigateToStudy: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            fontSize = 64.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.empty_words_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_words_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onNavigateToStudy) {
            Text(stringResource(R.string.word_selection_start_studying))
        }

        // Debug info card
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔍 Debug Info:",
                    fontWeight = FontWeight.Bold
                )
                Text("Total words: ${uiState.totalWords}")
                Text("Remaining: ${uiState.remainingWords.size}")
                Text("Selected: ${uiState.selectedCount}")
                Text("Hidden: ${uiState.hiddenCount}")
                Text("Current word: ${uiState.currentWord?.english ?: "null"}")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedPremiumLimitBottomSheet(
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.premium_limit_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.premium_limit_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Enhanced premium features card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = stringResource(R.string.premium_features_title),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    PremiumFeatureItem(stringResource(R.string.premium_feature_unlimited))
                    PremiumFeatureItem(stringResource(R.string.premium_feature_no_ads))
                    PremiumFeatureItem(stringResource(R.string.premium_feature_ai))
                    PremiumFeatureItem(stringResource(R.string.premium_feature_themes))
                    PremiumFeatureItem(stringResource(R.string.premium_feature_stats))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* Premium purchase */ },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.premium_upgrade))
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(stringResource(R.string.premium_continue_free))
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PremiumFeatureItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "✅",
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}