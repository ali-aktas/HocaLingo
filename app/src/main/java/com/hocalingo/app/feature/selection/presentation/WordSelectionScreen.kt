package com.hocalingo.app.feature.selection.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.core.ui.components.HocaEmptyState
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
            TopAppBar(
                title = {
                    Text(
                        "Kelime Seçimi",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.onEvent(WordSelectionEvent.FinishSelection) }
                    ) {
                        Text("Bitir")
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.canUndo) {
                FloatingActionButton(
                    onClick = { viewModel.onEvent(WordSelectionEvent.Undo) },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Geri Al",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
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
                        error = uiState.error!!,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.isCompleted -> {
                    CompletionScreen(
                        selectedCount = uiState.selectedCount,
                        hiddenCount = uiState.hiddenCount,
                        onContinue = { viewModel.onEvent(WordSelectionEvent.FinishSelection) }
                    )
                }
                uiState.currentWord != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Progress section
                        SelectionProgress(
                            selectedCount = uiState.selectedCount,
                            todayCount = uiState.todaySelectionCount,
                            progress = uiState.progress
                        )

                        // Swipeable card
                        Box(modifier = Modifier.weight(1f)) {
                            SwipeableCard(
                                word = uiState.currentWord!!.english,
                                translation = uiState.currentWord!!.turkish,
                                example = uiState.currentWord!!.exampleEn,
                                onSwipeLeft = {
                                    uiState.currentWord?.let { word ->
                                        viewModel.onEvent(WordSelectionEvent.SwipeLeft(word.id))
                                    }
                                },
                                onSwipeRight = {
                                    uiState.currentWord?.let { word ->
                                        viewModel.onEvent(WordSelectionEvent.SwipeRight(word.id))
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Action buttons (alternatif kontrol)
                        ActionButtons(
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
                    HocaEmptyState(
                        emoji = "📚",
                        title = "Kelime bulunamadı",
                        subtitle = "Bu pakette seçilecek kelime kalmamış",
                        actionText = "Çalışmaya Başla",
                        onAction = onNavigateToStudy,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }

            // Premium bottom sheet
            if (uiState.showPremiumSheet) {
                PremiumLimitBottomSheet(
                    onDismiss = { viewModel.onEvent(WordSelectionEvent.DismissPremium) },
                    onContinue = onNavigateToStudy
                )
            }
        }
    }
}

@Composable
private fun SelectionProgress(
    selectedCount: Int,
    todayCount: Int,
    progress: Float
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Progress bar
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primaryContainer,
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Selected count
            Column {
                Text(
                    text = "Seçilen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$selectedCount kelime",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Today's limit
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Günlük Limit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$todayCount / 25",
                    style = MaterialTheme.typography.titleMedium,
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

@Composable
private fun ActionButtons(
    onPass: () -> Unit,
    onLearn: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Pass button
        FilledTonalButton(
            onClick = onPass,
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = "❌",
                fontSize = 24.sp
            )
        }

        // Learn button
        FilledTonalButton(
            onClick = onLearn,
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = "✅",
                fontSize = 24.sp
            )
        }
    }
}

@Composable
private fun CompletionScreen(
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
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Tebrikler!",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Kelime seçimini tamamladınız",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(
                emoji = "✅",
                count = selectedCount,
                label = "Öğrenilecek"
            )
            StatCard(
                emoji = "⏭️",
                count = hiddenCount,
                label = "Pas Geçilen"
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Çalışmaya Başla")
        }
    }
}

@Composable
private fun StatCard(
    emoji: String,
    count: Int,
    label: String
) {
    Card(
        modifier = Modifier.size(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
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
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumLimitBottomSheet(
    onDismiss: () -> Unit,
    onContinue: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Günlük Limitinize Ulaştınız!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Ücretsiz kullanıcılar günde 25 kelime seçebilir",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Premium features
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Premium ile:",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    PremiumFeatureItem("✅ Sınırsız kelime seçimi")
                    PremiumFeatureItem("🚫 Reklamsız deneyim")
                    PremiumFeatureItem("🤖 AI Asistan desteği")
                    PremiumFeatureItem("🎨 Özel temalar")
                    PremiumFeatureItem("📊 Detaylı istatistikler")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { /* Premium satın alma */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Premium'a Geç - ₺19.99/ay")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Seçtiğim kelimelerle devam et")
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PremiumFeatureItem(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}