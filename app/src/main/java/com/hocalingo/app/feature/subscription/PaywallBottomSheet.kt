package com.hocalingo.app.feature.subscription

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.collectLatest

/**
 * PaywallBottomSheet - FIXED ✅
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * ✅ Activity referansını ViewModel'e geçiyor
 * ✅ Satın alma artık çalışıyor
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
        // Context'i Activity'ye cast et
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
        dragHandle = {
            // Custom drag handle
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .width(32.dp)
                        .height(4.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                ) {}
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Box {
            // Paywall content
            PaywallContent(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                modifier = Modifier.fillMaxWidth()
            )

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