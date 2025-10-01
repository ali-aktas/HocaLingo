package com.hocalingo.app.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hocalingo.app.core.ui.navigation.shouldShowBottomNavigation

/**
 * HocaLingo Custom Snackbar Host
 * ✅ Automatically adjusts position based on bottom navigation visibility
 * ✅ Prevents overlap with bottom navigation bar
 * ✅ Single source of truth for all snackbar positioning
 *
 * Usage in Scaffold:
 * ```
 * Scaffold(
 *     snackbarHost = {
 *         HocaSnackbarHost(
 *             hostState = snackbarHostState,
 *             currentRoute = currentRoute
 *         )
 *     }
 * ) { ... }
 * ```
 */
@Composable
fun HocaSnackbarHost(
    hostState: SnackbarHostState,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier.padding(
            bottom = if (shouldShowBottomNavigation(currentRoute)) {
                // Bottom navigation height (50dp) + extra spacing (20dp)
                70.dp
            } else {
                // Default padding when no bottom navigation
                16.dp
            }
        )
    )
}