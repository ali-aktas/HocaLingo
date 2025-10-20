package com.hocalingo.app.feature.selection

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocalingo.app.R

/**
 * WordSelectionComponents - PHASE 3
 *
 * Reusable UI components for Word Selection feature
 * - Responsive card height calculation
 * - Instruction bar with icons
 * - Daily limit warning
 * - Progress indicators
 * - Action buttons (large & small)
 * - Completion screen
 *
 * Package: feature/selection/
 */

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

// =====================================================
// RESPONSIVE HEIGHT CALCULATION
// =====================================================

/**
 * ✅ PHASE 3: Dynamic card height based on screen size
 * Returns: (cardHeight, containerHeight)
 */
@Composable
fun calculateOptimalCardHeight(): Pair<Dp, Dp> {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // Calculate available space
    val availableHeight = screenHeight -
            56.dp -  // Header (Hocalingo title)
            80.dp -  // Instruction bar
            150.dp - // Action buttons
            80.dp -  // Bottom padding & safety margin
            32.dp    // Extra margins

    // Min 340dp, Max 480dp for optimal UX
    val cardHeight = availableHeight.coerceIn(340.dp, 480.dp)
    val containerHeight = cardHeight + 60.dp // With padding

    return Pair(cardHeight, containerHeight)
}

// =====================================================
// INSTRUCTION BAR
// =====================================================

/**
 * ✅ PHASE 3: Compact instruction bar with icons
 * Shortened from long text to visual indicators
 */
@Composable
fun InstructionBar(
    modifier: Modifier = Modifier
) {
    Text(
        text = "İstediğin kelimeyi çalışma destene ekle",
        fontFamily = PoppinsFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 8.dp)
    )
}

// =====================================================
// DAILY LIMIT WARNING
// =====================================================

/**
 * ✅ PHASE 3: Warning when approaching daily limit
 * Shows when <= 10 words remaining
 */
@Composable
fun DailyLimitWarning(
    todaySelectionCount: Int,
    dailyLimit: Int = 50,
    modifier: Modifier = Modifier
) {
    val remaining = dailyLimit - todaySelectionCount

    if (remaining in 1..10) {
        Text(
            text = "⚠️ $remaining kelime hakkın kaldı",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color(0xFFFF9800),
            modifier = modifier.padding(vertical = 4.dp)
        )
    }
}

// =====================================================
// PROGRESS INDICATORS
// =====================================================

/**
 * Progress bar with count text
 */
@Composable
fun SelectionProgressBar(
    progress: Float,
    currentIndex: Int,
    totalWords: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$currentIndex / $totalWords",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =====================================================
// ACTION BUTTONS
// =====================================================

/**
 * Large action button (Skip & Learn)
 * With label below icon
 */
@Composable
fun ActionButton(
    icon: ImageVector,
    label: String,
    backgroundColor: Color,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        FloatingActionButton(
            onClick = onClick,
            containerColor = backgroundColor.copy(alpha = if (enabled) 1f else 0.4f),
            contentColor = Color.White,
            modifier = Modifier.size(64.dp),
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            ),
            shape = CircleShape
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

/**
 * Small action button (Undo)
 * Icon only, no label
 */
@Composable
fun SmallActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = backgroundColor.copy(alpha = if (enabled) 1f else 0.4f),
        contentColor = Color.White,
        modifier = modifier.size(48.dp),
        elevation = FloatingActionButtonDefaults.elevation(
            defaultElevation = 6.dp,
            pressedElevation = 10.dp
        ),
        shape = CircleShape
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(24.dp)
        )
    }
}

// =====================================================
// COMPLETION SCREEN
// =====================================================

/**
 * ✅ PHASE 3: Shown when all words are processed
 */
@Composable
fun CompletionScreen(
    selectedCount: Int,
    isDarkTheme: Boolean,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                MaterialTheme.colorScheme.surface
            } else {
                Color.White
            }
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎉",
                fontSize = 64.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Harika iş!",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "$selectedCount kelime seçtiniz.",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Çalışmaya Başla")
            }
        }
    }
}

// =====================================================
// PROCESSING INDICATOR OVERLAY
// =====================================================

/**
 * ✅ PHASE 3: Shows when button is clicked and processing
 */
@Composable
fun ProcessingIndicator(
    isProcessing: Boolean,
    modifier: Modifier = Modifier
) {
    if (isProcessing) {
        CircularProgressIndicator(
            modifier = modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}