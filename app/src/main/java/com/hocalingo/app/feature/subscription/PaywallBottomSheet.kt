package com.hocalingo.app.feature.subscription

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * PaywallBottomSheet - Optimized & Elegant ✨
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * ✅ 30dp padding from top of screen
 * ✅ More elegant, compact design
 * ✅ Smoother animations
 * ✅ Better visual hierarchy
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallBottomSheet(
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit = {},
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // ✅ Activity referansını al ve ViewModel'e geç
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val activity = context as? android.app.Activity
        if (activity != null) {
            viewModel.setActivity(activity)
        }
    }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SubscriptionEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is SubscriptionEffect.ShowError -> {
                    snackbarHostState.showSnackbar(
                        message = effect.message,
                        duration = SnackbarDuration.Short
                    )
                }
                SubscriptionEffect.PurchaseSuccess -> {
                    onPurchaseSuccess()
                    onDismiss()
                }
                SubscriptionEffect.DismissPaywall -> {
                    onDismiss()
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null, // Custom drag handle inside content
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 54.dp) // ✅ 30dp padding from screen top
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Custom drag handle
                CustomDragHandle()

                // Paywall content with optimized design
                PaywallContentOptimized(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}

/**
 * Custom Drag Handle - More elegant
 */
@Composable
private fun CustomDragHandle() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp),
            shape = RoundedCornerShape(2.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        ) {}
        Spacer(modifier = Modifier.height(20.dp))
    }
}