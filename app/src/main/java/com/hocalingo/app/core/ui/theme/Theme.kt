package com.hocalingo.app.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.hocalingo.app.core.common.ThemeMode

// Enhanced Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    outlineVariant = OutlineVariant
)

// Enhanced Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

/**
 * Enhanced HocaLingo Theme with Smart Theming Support
 * ✅ Material 3 dynamic color support (Android 12+)
 * ✅ Light/Dark theme support
 * ✅ Gradient extensions for consistent theming
 * ✅ Backward compatibility with existing code
 */
@Composable
fun HocaLingoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// ===== GRADIENT EXTENSIONS - Smart Theme-Aware Gradients =====

/**
 * Theme-aware gradient extensions
 * These gradients automatically adapt to light/dark theme
 * while maintaining the beautiful visual design
 */

// Main App Gradients - Theme Adaptive
@Composable
fun ColorScheme.getTealGradient(): Brush = Brush.linearGradient(
    colors = if (isSystemInDarkTheme()) {
        listOf(
            Color(0xFF26C6DA), // Lighter teal for dark theme
            Color(0xFF00ACC1)  // Adjusted teal for dark theme
        )
    } else {
        listOf(
            Color(0xFF4ECDC4), // Original teal
            Color(0xFF44A08D)  // Original teal darker
        )
    }
)

@Composable
fun ColorScheme.getPurpleGradient(): Brush = Brush.linearGradient(
    colors = if (isSystemInDarkTheme()) {
        listOf(
            Color(0xFF7986CB), // Lighter purple for dark theme
            Color(0xFF5C6BC0)  // Adjusted purple for dark theme
        )
    } else {
        listOf(
            Color(0xFF667eea), // Original purple
            Color(0xFF764ba2)  // Original purple darker
        )
    }
)

@Composable
fun ColorScheme.getFireGradient(): Brush = Brush.linearGradient(
    colors = if (isSystemInDarkTheme()) {
        listOf(
            Color(0xFFFF8A65), // Lighter fire for dark theme
            Color(0xFFFF7043)  // Adjusted fire for dark theme
        )
    } else {
        listOf(
            Color(0xFFFF6B35), // Original fire
            Color(0xFFFF8E53)  // Original fire lighter
        )
    }
)

@Composable
fun ColorScheme.getGoldGradient(): Brush = Brush.linearGradient(
    colors = if (isSystemInDarkTheme()) {
        listOf(
            Color(0xFFFFD54F), // Lighter gold for dark theme
            Color(0xFFFFB300)  // Adjusted gold for dark theme
        )
    } else {
        listOf(
            Color(0xFFFFD700), // Original gold
            Color(0xFFFFA500)  // Original gold darker
        )
    }
)

@Composable
fun ColorScheme.getGreenGradient(): Brush = Brush.linearGradient(
    colors = if (isSystemInDarkTheme()) {
        listOf(
            Color(0xFF26A69A), // Lighter green for dark theme
            Color(0xFF00897B)  // Adjusted green for dark theme
        )
    } else {
        listOf(
            Color(0xFF11998e), // Original green
            Color(0xFF38ef7d)  // Original green lighter
        )
    }
)

// Level-based Gradients (for word packages)
@Composable
fun ColorScheme.getLevelGradient(level: String): Brush = when (level) {
    "A1" -> getFireGradient()
    "A2" -> getTealGradient()
    "B1" -> Brush.linearGradient(
        colors = if (isSystemInDarkTheme()) {
            listOf(Color(0xFF66BB6A), Color(0xFF4CAF50))
        } else {
            listOf(Color(0xFF43E97B), Color(0xFF38F9D7))
        }
    )
    "B2" -> getPurpleGradient()
    "C1" -> Brush.linearGradient(
        colors = if (isSystemInDarkTheme()) {
            listOf(Color(0xFFFF8A65), Color(0xFFFF7043))
        } else {
            listOf(Color(0xFFf12711), Color(0xFFf5af19))
        }
    )
    "C2" -> Brush.linearGradient(
        colors = if (isSystemInDarkTheme()) {
            listOf(Color(0xFFCE93D8), Color(0xFFBA68C8))
        } else {
            listOf(Color(0xFFff9a9e), Color(0xFFfecfef))
        }
    )
    else -> getFireGradient() // Default fallback
}

// Splash Screen Gradient
@Composable
fun ColorScheme.getSplashGradient(): Brush = Brush.verticalGradient(
    colors = if (isSystemInDarkTheme()) {
        listOf(
            Color(0xFF37474F), // Dark theme splash
            Color(0xFF263238)  // Darker
        )
    } else {
        listOf(
            Color(0xFF00D4FF), // Original light splash
            Color(0xFF1E88E5)  // Original darker
        )
    }
)

// Extension properties for easy access to custom colors
val ColorScheme.easyGreen get() = HocaColors.EasyGreen
val ColorScheme.mediumYellow get() = HocaColors.MediumYellow
val ColorScheme.hardRed get() = HocaColors.HardRed
val ColorScheme.streakFire get() = HocaColors.StreakFire
val ColorScheme.masteredGold get() = HocaColors.MasteredGold
val ColorScheme.cardShadow get() = HocaColors.CardShadow
val ColorScheme.dividerGray get() = HocaColors.DividerGray

// ===== THEME MODE UTILITIES =====

/**
 * Utility composable to get current theme mode
 */
@Composable
fun getCurrentThemeMode(): ThemeMode = when {
    isSystemInDarkTheme() -> ThemeMode.DARK
    else -> ThemeMode.LIGHT
}