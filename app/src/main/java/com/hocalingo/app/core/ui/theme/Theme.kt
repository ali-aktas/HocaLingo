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

// ╔════════════════════════════════════════════════════════════════════════╗
// ║                    MATERIAL 3 COLOR SCHEMES                            ║
// ║                                                                        ║
// ║  Light ve Dark tema için Material 3 color scheme tanımları            ║
// ║  Bu scheme'ler MaterialTheme.colorScheme ile erişilir                 ║
// ╚════════════════════════════════════════════════════════════════════════╝

/**
 * LIGHT THEME COLOR SCHEME
 * Açık tema için tüm Material 3 renk tanımları
 */
private val LightColorScheme = lightColorScheme(
    // Primary colors (Ana marka renkleri)
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,

    // Secondary colors (İkincil renkler)
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,

    // Tertiary colors (Üçüncül renkler)
    tertiary = Tertiary,
    onTertiary = OnTertiary,
    tertiaryContainer = TertiaryContainer,
    onTertiaryContainer = OnTertiaryContainer,

    // Error colors (Hata renkleri)
    error = Error,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,

    // Background & Surface (Arka plan ve yüzey renkleri)
    background = Background,           // Krem (#FAF9F6) - Tüm ekran
    onBackground = OnBackground,
    surface = Surface,                 // Pure white (#FFFFFF) - Kartlar
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,   // Hafif gri - Divider vb.
    onSurfaceVariant = OnSurfaceVariant,

    // Borders (Kenarlıklar)
    outline = Outline,
    outlineVariant = OutlineVariant
)

/**
 * DARK THEME COLOR SCHEME
 * Karanlık tema için tüm Material 3 renk tanımları
 */
private val DarkColorScheme = darkColorScheme(
    // Primary colors
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,

    // Secondary colors
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,

    // Tertiary colors
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,

    // Error colors
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,

    // Background & Surface
    background = DarkBackground,        // Çok koyu (#0F0F0F) - AMOLED
    onBackground = DarkOnBackground,
    surface = DarkSurface,              // Koyu gri (#1C1C1C) - Kartlar
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant, // Daha açık gri
    onSurfaceVariant = DarkOnSurfaceVariant,

    // Borders
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

// ╔════════════════════════════════════════════════════════════════════════╗
// ║                    HOCALINGO THEME COMPOSABLE                          ║
// ╚════════════════════════════════════════════════════════════════════════╝

/**
 * HocaLingo Ana Tema
 *
 * Özellikleri:
 * • Material 3 desteği
 * • Light/Dark tema
 * • Dynamic color (Android 12+)
 * • Status bar renk yönetimi
 *
 * @param darkTheme Dark tema aktif mi? (Default: sistem ayarı)
 * @param dynamicColor Android 12+ dynamic color kullansın mı? (Default: true)
 */
@Composable
fun HocaLingoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    // Color scheme seçimi
    val colorScheme = when {
        // Android 12+ ve dynamic color aktifse sistem renkleri kullan
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // Değilse kendi tanımladığımız renkleri kullan
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Status bar rengini ayarla
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

// ╔════════════════════════════════════════════════════════════════════════╗
// ║                    GRADIENT EXTENSIONS                                 ║
// ║                                                                        ║
// ║  Tema-uyumlu gradient'ler için extension functions                    ║
// ║  Kullanım: MaterialTheme.colorScheme.getFireGradient()                ║
// ╚════════════════════════════════════════════════════════════════════════╝

/**
 * Teal Gradient - A2 seviye paketler için
 */
@Composable
fun ColorScheme.getTealGradient(): Brush = Brush.linearGradient(
    colors = if (isSystemInDarkTheme()) {
        listOf(Color(0xFF26C6DA), Color(0xFF00ACC1))
    } else {
        listOf(Color(0xFF4ECDC4), Color(0xFF44A08D))
    }
)

/**
 * Purple Gradient - B2 seviye ve AI özellikleri için
 */
@Composable
fun ColorScheme.getPurpleGradient(): Brush = Brush.linearGradient(
    colors = if (isSystemInDarkTheme()) {
        listOf(Color(0xFF7986CB), Color(0xFF5C6BC0))
    } else {
        listOf(Color(0xFF667eea), Color(0xFF764ba2))
    }
)

/**
 * Fire Gradient - A1 seviye ve ana brand için
 */
@Composable
fun ColorScheme.getFireGradient(): Brush = Brush.linearGradient(
    colors = if (isSystemInDarkTheme()) {
        listOf(Color(0xFFFF8A65), Color(0xFFFF7043))
    } else {
        listOf(Color(0xFFFF6B35), Color(0xFFFF8E53))
    }
)

/**
 * Gold Gradient - Premium özellikler için
 */
@Composable
fun ColorScheme.getGoldGradient(): Brush = Brush.linearGradient(
    colors = if (isSystemInDarkTheme()) {
        listOf(Color(0xFFFFD54F), Color(0xFFFFB300))
    } else {
        listOf(Color(0xFFFFD700), Color(0xFFFFA500))
    }
)

/**
 * Green Gradient - Başarı durumları için
 */
@Composable
fun ColorScheme.getGreenGradient(): Brush = Brush.linearGradient(
    colors = if (isSystemInDarkTheme()) {
        listOf(Color(0xFF26A69A), Color(0xFF00897B))
    } else {
        listOf(Color(0xFF11998e), Color(0xFF38ef7d))
    }
)

/**
 * Level-based Gradient - Kelime paket seviyelerine göre
 *
 * @param level CEFR seviyesi (A1, A2, B1, B2, C1, C2)
 * @return Seviyeye uygun gradient
 */
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
    else -> getFireGradient() // Fallback
}

/**
 * Splash Screen Gradient
 */
@Composable
fun ColorScheme.getSplashGradient(): Brush = Brush.verticalGradient(
    colors = if (isSystemInDarkTheme()) {
        listOf(Color(0xFF37474F), Color(0xFF263238))
    } else {
        listOf(Color(0xFF00D4FF), Color(0xFF1E88E5))
    }
)

// ╔════════════════════════════════════════════════════════════════════════╗
// ║                    CUSTOM COLOR EXTENSIONS                             ║
// ║                                                                        ║
// ║  HocaColors object'inden kolay erişim için extension'lar              ║
// ║  Kullanım: MaterialTheme.colorScheme.easyGreen                        ║
// ╚════════════════════════════════════════════════════════════════════════╝

val ColorScheme.easyGreen get() = HocaColors.EasyGreen
val ColorScheme.mediumYellow get() = HocaColors.MediumYellow
val ColorScheme.hardRed get() = HocaColors.HardRed
val ColorScheme.streakFire get() = HocaColors.StreakFire
val ColorScheme.masteredGold get() = HocaColors.MasteredGold
val ColorScheme.cardShadow get() = HocaColors.CardShadow
val ColorScheme.dividerGray get() = HocaColors.DividerGray

// ╔════════════════════════════════════════════════════════════════════════╗
// ║                    THEME UTILITIES                                     ║
// ╚════════════════════════════════════════════════════════════════════════╝

/**
 * Mevcut tema modunu döndürür
 */
@Composable
fun getCurrentThemeMode(): ThemeMode = when {
    isSystemInDarkTheme() -> ThemeMode.DARK
    else -> ThemeMode.LIGHT
}