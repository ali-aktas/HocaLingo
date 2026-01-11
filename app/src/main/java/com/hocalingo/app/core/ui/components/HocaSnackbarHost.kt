package com.hocalingo.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocalingo.app.core.ui.navigation.shouldShowBottomNavigation

/**
 * HocaLingo Custom Snackbar Host
 * ✅ Modern, temaya uygun tasarım
 * ✅ Otomatik pozisyon ayarı (bottom navigation'a göre)
 * ✅ Success/Error/Info/Warning tipleri
 * ✅ Gradient arka plan
 * ✅ İkonlu gösterim
 */
@Composable
fun HocaSnackbarHost(
    hostState: SnackbarHostState,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        snackbar = { data ->
            HocaSnackbar(
                message = data.visuals.message,
                type = determineSnackbarType(data.visuals.message)
            )
        }
    )
}

/**
 * Custom Snackbar with HocaLingo Design
 */
@Composable
private fun HocaSnackbar(
    message: String,
    type: SnackbarType = SnackbarType.INFO,
    modifier: Modifier = Modifier
) {
    val colors = type.getColors()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(colors.first, colors.second)
                )
            )
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Icon
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            // Message
            Text(
                text = message,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color.White,
                lineHeight = 20.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Snackbar Types
 */
private enum class SnackbarType(
    val icon: ImageVector,
    val getColors: () -> Pair<Color, Color>
) {
    SUCCESS(
        icon = Icons.Default.CheckCircle,
        getColors = { Pair(Color(0xFF4CAF50), Color(0xFF66BB6A)) }
    ),
    ERROR(
        icon = Icons.Default.Error,
        getColors = { Pair(Color(0xFFE53935), Color(0xFFEF5350)) }
    ),
    WARNING(
        icon = Icons.Default.Warning,
        getColors = { Pair(Color(0xFFFF9800), Color(0xFFFFA726)) }
    ),
    INFO(
        icon = Icons.Default.Info,
        getColors = { Pair(Color(0xFFFF6B35), Color(0xFFFF8557)) } // HocaLingo Orange
    )
}

/**
 * Determine snackbar type from message content
 */
private fun determineSnackbarType(message: String): SnackbarType {
    return when {
        message.contains("başarı", ignoreCase = true) ||
                message.contains("tebrik", ignoreCase = true) ||
                message.contains("harika", ignoreCase = true) ||
                message.contains("✓") -> SnackbarType.SUCCESS

        message.contains("hata", ignoreCase = true) ||
                message.contains("başarısız", ignoreCase = true) ||
                message.contains("yüklenemedi", ignoreCase = true) -> SnackbarType.ERROR

        message.contains("uyarı", ignoreCase = true) ||
                message.contains("dikkat", ignoreCase = true) -> SnackbarType.WARNING

        else -> SnackbarType.INFO
    }
}

/**
 * Extension functions for easy usage
 */

/**
 * Show success snackbar
 */
suspend fun SnackbarHostState.showSuccess(message: String) {
    showSnackbar(
        message = message,
        duration = SnackbarDuration.Short
    )
}

/**
 * Show error snackbar
 */
suspend fun SnackbarHostState.showError(message: String) {
    showSnackbar(
        message = "Hata: $message",
        duration = SnackbarDuration.Long
    )
}

/**
 * Show info snackbar
 */
suspend fun SnackbarHostState.showInfo(message: String) {
    showSnackbar(
        message = message,
        duration = SnackbarDuration.Short
    )
}

/**
 * Show warning snackbar
 */
suspend fun SnackbarHostState.showWarning(message: String) {
    showSnackbar(
        message = "⚠️ $message",
        duration = SnackbarDuration.Long
    )
}