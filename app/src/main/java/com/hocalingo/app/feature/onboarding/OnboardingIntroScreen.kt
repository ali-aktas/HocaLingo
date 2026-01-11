package com.hocalingo.app.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

/**
 * Onboarding Intro Screen - 5 Pages
 *
 * Package: feature/onboarding/
 * Flow: AUTH → [THIS SCREEN] → PACKAGE_SELECTION → WORD_SELECTION → HOME
 *
 * Features:
 * - Edge-to-edge design (Android 35+ compatible)
 * - Swipeable pages with manual navigation
 * - Large centered images
 * - Fixed bottom button
 * - Professional modern UI
 */
@Composable
fun OnboardingIntroScreen(
    onNavigateToPackageSelection: () -> Unit,
    viewModel: OnboardingIntroViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { state.totalPages })
    val pages = OnboardingIntroViewModel.getOnboardingPages()
    val coroutineScope = rememberCoroutineScope()

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                OnboardingIntroEffect.NavigateToPackageSelection -> {
                    onNavigateToPackageSelection()
                }
                is OnboardingIntroEffect.ScrollToPage -> {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(effect.page)
                    }
                }
            }
        }
    }

    // Sync pager state with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onEvent(OnboardingIntroEvent.OnPageChanged(pagerState.currentPage))
    }

    // ✅ Edge-to-edge container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF431F84),
                        Color(0xFF5428A3)
                    )
                )
            )
    ) {
        // ✅ Swipeable content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true  // ✅ Elle kaydırma aktif
        ) { pageIndex ->
            OnboardingPageContent(
                page = pages[pageIndex],
                modifier = Modifier.fillMaxSize()
            )
        }

        // ✅ Fixed bottom section (button + dots)
        FixedBottomSection(
            currentPage = pagerState.currentPage,
            totalPages = state.totalPages,
            onNextClick = {
                if (pagerState.currentPage < state.totalPages - 1) {
                    viewModel.onEvent(OnboardingIntroEvent.OnNextClick)
                } else {
                    viewModel.onEvent(OnboardingIntroEvent.OnGetStartedClick)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        )
    }
}

/**
 * Individual page content
 */
@Composable
private fun OnboardingPageContent(
    page: OnboardingIntroPage,
    modifier: Modifier = Modifier
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    Column(
        modifier = modifier
            .padding(systemBarsPadding)  // ✅ Sistem barlarıyla çakışmayı önle
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(0.5f))

        // ✅ Large centered image
        Image(
            painter = painterResource(id = page.imageRes),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(420.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ✅ Title - 32sp bold Poppins white
        Text(
            text = page.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Fixed bottom section (dots + button)
 */
@Composable
private fun FixedBottomSection(
    currentPage: Int,
    totalPages: Int,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()

    Column(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .padding(bottom = systemBarsPadding.calculateBottomPadding() + 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ✅ Dot Indicator
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            repeat(totalPages) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentPage) 12.dp else 8.dp)
                        .background(
                            color = if (index == currentPage)
                                Color.White
                            else
                                Color.White.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                )
            }
        }

        // ✅ Fixed button
        Button(
            onClick = onNextClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = if (currentPage == totalPages - 1) "Başla" else "İleri",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF431F84)
            )
        }
    }
}