package com.hocalingo.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// ============ MATERIAL 3 LIGHT THEME COLORS ============
val Primary = Color(0xFF6750A4)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFFE9DDFF)
val OnPrimaryContainer = Color(0xFF6327CE)

val Secondary = Color(0xFF4CAF50)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFE8F5E8)
val OnSecondaryContainer = Color(0xFF1B5E20)

val Tertiary = Color(0xFFFF9800)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFF3E0)
val OnTertiaryContainer = Color(0xFFE65100)

val Error = Color(0xFFD32F2F)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFEBEE)
val OnErrorContainer = Color(0xFFB71C1C)

val Background = Color(0xFFFAFAFA)
val OnBackground = Color(0xFF1C1B1F)
val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF1C1B1F)
val SurfaceVariant = Color(0xFFF5F5F5)
val OnSurfaceVariant = Color(0xFF49454F)

val Outline = Color(0xFFE0E0E0)
val OutlineVariant = Color(0xFFEEEEEE)

// ============ MATERIAL 3 DARK THEME COLORS ============
val DarkPrimary = Color(0xFF90CAF9)
val DarkOnPrimary = Color(0xFF0D47A1)
val DarkPrimaryContainer = Color(0xFF1565C0)
val DarkOnPrimaryContainer = Color(0xFFE3F2FD)

val DarkSecondary = Color(0xFF81C784)
val DarkOnSecondary = Color(0xFF1B5E20)
val DarkSecondaryContainer = Color(0xFF388E3C)
val DarkOnSecondaryContainer = Color(0xFFE8F5E8)

val DarkTertiary = Color(0xFFFFB74D)
val DarkOnTertiary = Color(0xFFE65100)
val DarkTertiaryContainer = Color(0xFFF57C00)
val DarkOnTertiaryContainer = Color(0xFFFFF3E0)

val DarkError = Color(0xFFEF5350)
val DarkOnError = Color(0xFFB71C1C)
val DarkErrorContainer = Color(0xFFC62828)
val DarkOnErrorContainer = Color(0xFFFFEBEE)

val DarkBackground = Color(0xFF121212)
val DarkOnBackground = Color(0xFFE6E1E5)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnSurface = Color(0xFFE6E1E5)
val DarkSurfaceVariant = Color(0xFF2C2C2C)
val DarkOnSurfaceVariant = Color(0xFFCAC4D0)

val DarkOutline = Color(0xFF424242)
val DarkOutlineVariant = Color(0xFF383838)

// ============ HOCALINGO CUSTOM COLORS ============
object HocaColors {
    // Study difficulty buttons (orijinal renkler korundu)
    val EasyGreen = Color(0xFF4CAF50)
    val MediumYellow = Color(0xFFFF9800)
    val HardRed = Color(0xFFE53935)

    // Special indicators (orijinal renkler korundu)
    val StreakFire = Color(0xFFFF5722)
    val MasteredGold = Color(0xFFFFD700)

    // UI Elements (orijinal renkler korundu)
    val CardShadow = Color(0x1A000000)
    val DividerGray = Color(0xFFE0E0E0)

    // ============ DESIGN SYSTEM V2 - 3D BUTTON COLORS ============
    // Primary Orange (Ana marka rengi)
    val Orange = Color(0xFFFF6B35)
    val OrangeDark = Color(0xFFE85A2B)
    val OrangeLight = Color(0xFFFF8C61)

    // Secondary Purple (AI özellikleri)
    val Purple = Color(0xFF7B61FF)
    val PurpleDark = Color(0xFF6247E5)
    val PurpleLight = Color(0xFF9B82FF)

    // 3D Button depth colors (top/bottom pairs)
    val SuccessTop = Color(0xFF4CAF50)      // EasyGreen ile aynı
    val SuccessBottom = Color(0xFF2DA84D)

    val PurpleTop = Color(0xFF7B61FF)       // Purple ile aynı
    val PurpleBottom = Color(0xFF6247E5)    // PurpleDark ile aynı

    val OrangeTop = Color(0xFFFF6B35)       // Orange ile aynı
    val OrangeBottom = Color(0xFFE85A2B)    // OrangeDark ile aynı
}