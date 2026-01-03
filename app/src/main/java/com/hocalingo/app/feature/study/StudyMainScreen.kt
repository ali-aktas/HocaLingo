package com.hocalingo.app.feature.study

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hocalingo.app.HocaRoutes
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import com.hocalingo.app.feature.study.components.CompactSelectedWordsCard
import com.hocalingo.app.feature.study.components.CompactWordItem
import com.hocalingo.app.feature.study.components.StudyDirectionCard
import kotlinx.coroutines.flow.collectLatest

/**
 * StudyMainScreen - Study Hub Screen
 *
 * Package: feature/study/
 *
 * Modern study center with 3D buttons (Duolingo-inspired)
 *
 * Features:
 * - Start Study button (primary action)
 * - Selected words preview with bottomsheet
 * - Study direction toggle
 * - Add new word button
 * - Quick stats display
 *
 * Features moved from ProfileScreen:
 * - Selected words management
 * - Study direction setting
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMainScreen(
    onNavigateToStudy: () -> Unit,
    onNavigateToAddWord: () -> Unit,
    viewModel: StudyMainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Theme State
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    // BottomSheet State
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Effect Handler
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                StudyMainEffect.NavigateToStudy -> onNavigateToStudy()
                StudyMainEffect.NavigateToAddWord -> onNavigateToAddWord()
                is StudyMainEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 54.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            item {
                Text(
                    text = "Çalışma Merkezi",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Start Study Button (Primary Action - Big & Eye-catching)
            item {
                StartStudyButton(
                    onClick = { viewModel.onEvent(StudyMainEvent.StartStudy) }
                )
            }

            // Quick Stats Card
            item {
                StudyStatsCard(
                    wordsCount = uiState.totalWordsCount,
                    studyDirection = uiState.studyDirectionText
                )
            }

            // Selected Words Card
            item {
                CompactSelectedWordsCard(
                    words = uiState.selectedWordsPreview,
                    totalCount = uiState.totalWordsCount,
                    onViewAllClick = { viewModel.onEvent(StudyMainEvent.ShowWordsBottomSheet) },
                    isDarkTheme = isDarkTheme
                )
            }

            // Study Direction Card
            item {
                StudyDirectionCard(
                    currentDirection = uiState.studyDirection,
                    onDirectionChange = { newDirection ->
                        viewModel.onEvent(StudyMainEvent.UpdateStudyDirection(newDirection))
                    },
                    isDarkTheme = isDarkTheme
                )
            }

            // Add Word Button
            item {
                AddWordButton(
                    onClick = { viewModel.onEvent(StudyMainEvent.AddWord) }
                )
            }

            // Bottom Spacer
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // BottomSheet - All Selected Words with Pagination
    if (uiState.showWordsBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(StudyMainEvent.HideWordsBottomSheet) },
            sheetState = bottomSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Seçili Kelimeler (${uiState.totalWordsCount})",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (uiState.isLoadingAllWords) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.height(400.dp)
                    ) {
                        items(uiState.allSelectedWords.size) { index ->
                            val word = uiState.allSelectedWords[index]
                            CompactWordItem(
                                word = word,
                                isInBottomSheet = true
                            )

                            // Load more when reaching near end
                            if (index >= uiState.allSelectedWords.size - 3 && uiState.canLoadMoreWords) {
                                LaunchedEffect(Unit) {
                                    viewModel.onEvent(StudyMainEvent.LoadMoreWords)
                                }
                            }
                        }

                        if (uiState.isLoadingAllWords) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}