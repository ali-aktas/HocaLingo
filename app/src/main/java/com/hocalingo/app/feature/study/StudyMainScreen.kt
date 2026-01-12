package com.hocalingo.app.feature.study

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
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
import com.hocalingo.app.core.common.StudyDirection
import com.hocalingo.app.core.ui.components.PlayButton
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.HocaColors
import com.hocalingo.app.core.ui.theme.HocaSpacing
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import com.hocalingo.app.feature.study.components.SelectedWordsCard
import com.hocalingo.app.feature.study.components.StudyDirectionIndicator
import com.hocalingo.app.feature.study.components.StudyWideActionButton
import com.hocalingo.app.feature.study.components.StudyWordsBottomSheet
import kotlinx.coroutines.flow.collectLatest
import java.util.Calendar

/**
 * StudyMainScreen - Premium Study Hub
 *
 * Package: feature/study/
 * File: StudyMainScreen.kt
 *
 * HomeScreen ile tutarlı modern tasarım:
 * 1. Hero Section (PlayButton + Motivasyon + Maskot)
 * 2. Direction Indicator (EN ↔ TR)
 * 3. Selected Words Preview (Sade liste)
 * 4. 3D Action Buttons (Çalışma Yönü + Yeni Kelime)
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

// Motivasyon yazıları (Study için özel)
private val studyMotivationTexts = listOf(
    "Bugün öğrenmeye hazır mısın?",
    "Her kelime bir adım ileri!",
    "Pratik yapmak mükemmelleştirir",
    "Hedefine yaklaşıyorsun!",
    "Bugün de harika iş çıkar",
    "Kelime gücün artıyor",
    "Disiplin başarı getirir",
    "Öğrenme yolculuğun devam ediyor"
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

    // Günlük motivasyon
    val dailyMotivation = remember {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        studyMotivationTexts[dayOfYear % studyMotivationTexts.size]
    }

    // Bottom Sheet State
    var showWordsBottomSheet by remember { mutableStateOf(false) }

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
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 48.dp,
                end = 20.dp,
                bottom = 100.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. HEADER
            item {
                Text(
                    text = "Çalışma Merkezi",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2. HERO CARD (PlayButton + Motivasyon + Maskot)
            item {
                StudyHeroCard(
                    motivationText = dailyMotivation,
                    onPlayClick = { viewModel.onEvent(StudyMainEvent.StartStudy) },
                    isDarkTheme = isDarkTheme
                )
            }

            // 3. DIRECTION INDICATOR (Modern, Sade)
            item {
                StudyDirectionIndicator(
                    isEnglishToTurkish = uiState.studyDirection == StudyDirection.EN_TO_TR,
                    onToggle = {
                        val newDirection = if (uiState.studyDirection == StudyDirection.EN_TO_TR) {
                            StudyDirection.TR_TO_EN
                        } else {
                            StudyDirection.EN_TO_TR
                        }
                        viewModel.onEvent(StudyMainEvent.UpdateStudyDirection(newDirection))
                    },
                    isDarkTheme = isDarkTheme
                )
            }

            item {
                StudyWideActionButton(
                    onClick = { viewModel.onEvent(StudyMainEvent.AddWord) },
                    icon = painterResource(id = R.drawable.add_img),
                    title = "Yeni Kelime-Kalıp Ekle",
                    subtitle = "Kendi kelimelerini desteye ekle",
                    baseColor = HocaColors.PurpleTop
                )
            }

            // 4. SELECTED WORDS PREVIEW
            item {
                SelectedWordsCard(
                    words = uiState.selectedWordsPreview,
                    totalCount = uiState.totalWordsCount,
                    onViewAllClick = {
                        viewModel.onEvent(StudyMainEvent.ShowWordsBottomSheet)
                        showWordsBottomSheet = true
                    },
                    isDarkTheme = isDarkTheme
                )
            }

            // 6. ACTION BUTTON - Çalışma Yönü Değiştir
            item {
                StudyWideActionButton(
                    onClick = {
                        val newDirection = if (uiState.studyDirection == StudyDirection.EN_TO_TR) {
                            StudyDirection.TR_TO_EN
                        } else {
                            StudyDirection.EN_TO_TR
                        }
                        viewModel.onEvent(StudyMainEvent.UpdateStudyDirection(newDirection))
                    },
                    icon = painterResource(id = R.drawable.route_img),
                    title = "Çalışma Yönünü Değiştir",
                    subtitle = if (uiState.studyDirection == StudyDirection.EN_TO_TR)
                        "Şu an: İngilizce → Türkçe"
                    else
                        "Şu an: Türkçe → İngilizce",
                    baseColor = HocaColors.SuccessTop
                )
            }

        }

        // Bottom Sheet - Tüm Kelimeler
        if (showWordsBottomSheet) {
            StudyWordsBottomSheet(
                words = uiState.allSelectedWords,
                isLoading = uiState.isLoadingAllWords,
                canLoadMore = uiState.canLoadMoreWords,
                onLoadMore = { viewModel.onEvent(StudyMainEvent.LoadMoreWords) },
                onDismiss = {
                    showWordsBottomSheet = false
                    viewModel.onEvent(StudyMainEvent.HideWordsBottomSheet)
                },
                isDarkTheme = isDarkTheme
            )
        }
    }
}

// =====================================================
// HERO CARD (Private - bu dosyada)
// =====================================================

/**
 * StudyHeroCard - PlayButton + Motivasyon + Maskot
 * HomeScreen HeroCard ile aynı yapıda
 */
@Composable
private fun StudyHeroCard(
    motivationText: String,
    onPlayClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(HocaSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        // Ortada: Play Button
        PlayButton(
            onClick = onPlayClick,
            size = 170.dp
        )
    }
}