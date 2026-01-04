package com.hocalingo.app.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.feature.subscription.TrialOfferDialog
import com.hocalingo.app.feature.subscription.PaywallBottomSheet
import com.hocalingo.app.core.common.TrialOfferDataStore
import kotlinx.coroutines.launch

/**
 * Onboarding Intro Screen - 3 Pages
 *
 * Package: feature/onboarding/
 * Flow: AUTH → [THIS SCREEN] → PACKAGE_SELECTION → WORD_SELECTION → HOME
 */
@Composable
fun OnboardingIntroScreen(
    onNavigateToPackageSelection: () -> Unit,
    viewModel: OnboardingIntroViewModel = hiltViewModel()
) {
    // ✅ Trial Offer System
    val trialOfferDataStore: TrialOfferDataStore = hiltViewModel()
    var showTrialOffer by remember { mutableStateOf(false) }
    var showPaywall by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { state.totalPages })
    val pages = OnboardingIntroViewModel.getOnboardingPages()

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                OnboardingIntroEffect.NavigateToPackageSelection -> {
                    // ✅ Check if should show trial offer
                    scope.launch {
                        val shouldShow = trialOfferDataStore.shouldShowTrialOffer()
                        if (shouldShow) {
                            trialOfferDataStore.markFirstShown()
                            showTrialOffer = true
                        } else {
                            onNavigateToPackageSelection()
                        }
                    }
                }
                is OnboardingIntroEffect.ScrollToPage -> {
                    pagerState.animateScrollToPage(effect.page)
                }
            }
        }
    }

    // Sync pager state with ViewModel
    LaunchedEffect(pagerState.currentPage) {
        viewModel.onEvent(OnboardingIntroEvent.OnPageChanged(pagerState.currentPage))
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false // Only button navigation
    ) { pageIndex ->
        OnboardingPage(
            page = pages[pageIndex],
            currentPage = pageIndex + 1,
            totalPages = state.totalPages,
            onNextClick = { viewModel.onEvent(OnboardingIntroEvent.OnNextClick) },
            onGetStartedClick = { viewModel.onEvent(OnboardingIntroEvent.OnGetStartedClick) }
        )
    }
    // ✅ Trial Offer Dialog
    if (showTrialOffer) {
        TrialOfferDialog(
            onStartTrial = {
                showTrialOffer = false
                showPaywall = true
            },
            onDismiss = {
                showTrialOffer = false
                scope.launch {
                    trialOfferDataStore.markFirstDismissed()
                }
                onNavigateToPackageSelection()
            }
        )
    }

    // ✅ Paywall BottomSheet
    if (showPaywall) {
        PaywallBottomSheet(
            onDismiss = {
                showPaywall = false
                onNavigateToPackageSelection()
            },
            onPurchaseSuccess = {
                showPaywall = false
                scope.launch {
                    trialOfferDataStore.resetAfterPurchase()
                }
                onNavigateToPackageSelection()
            }
        )
    }
}

@Composable
private fun OnboardingPage(
    page: OnboardingIntroPage,
    currentPage: Int,
    totalPages: Int,
    onNextClick: () -> Unit,
    onGetStartedClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFB9322),
                        Color(0xFFF58220)
                    ),
                    startY = 0f,
                    endY = 1800f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 60.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Content Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1f)
            ) {
                // Teacher Image - BIGGER SIZE
                Image(
                    painter = painterResource(id = page.imageRes),
                    contentDescription = null,
                    modifier = Modifier.size(350.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title - WHITE & BOLD
                Text(
                    text = page.title,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 36.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = page.description,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }

            // Bottom Section (Dot Indicator + Button)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // DOT INDICATOR (like design)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    repeat(totalPages) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (index + 1 == currentPage) 10.dp else 8.dp)
                                .background(
                                    color = if (index + 1 == currentPage)
                                        Color.White
                                    else
                                        Color.White.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // Button
                Button(
                    onClick = {
                        if (currentPage == totalPages) {
                            onGetStartedClick()
                        } else {
                            onNextClick()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White
                    )
                ) {
                    Text(
                        text = page.buttonText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFB9322)
                    )
                }
            }
        }
    }
}