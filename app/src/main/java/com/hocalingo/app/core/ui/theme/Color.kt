package com.hocalingo.app.core.ui.theme

import androidx.compose.ui.graphics.Color

// ╔════════════════════════════════════════════════════════════════════════╗
// ║                    MATERIAL 3 COLOR SYSTEM                             ║
// ║                                                                        ║
// ║  Material 3 uses a semantic color system:                             ║
// ║  • Background: Full screen background color                           ║
// ║  • Surface: Cards, dialogs, bottom sheets background                  ║
// ║  • SurfaceVariant: Subtle variations (dividers, disabled states)      ║
// ║  • Primary: Main brand color (buttons, active states)                 ║
// ║  • Secondary: Secondary actions and highlights                        ║
// ║  • Tertiary: Accent colors for special elements                       ║
// ║                                                                        ║
// ║  "On" colors: Text/icons drawn ON TOP of their base color             ║
// ║  Example: OnSurface = Text color on Surface background                ║
// ╚════════════════════════════════════════════════════════════════════════╝

// ============================================================================
// LIGHT THEME - Material 3 Colors
// ============================================================================

// PRIMARY - Main brand color (buttons, navigation highlights)
val Primary = Color(0xFF6750A4)
val OnPrimary = Color(0xFFFFFFFF)              // Text on primary
val PrimaryContainer = Color(0xFFE9DDFF)       // Subtle primary backgrounds
val OnPrimaryContainer = Color(0xFF6327CE)

// SECONDARY - Supporting actions
val Secondary = Color(0xFF4CAF50)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFE8F5E8)
val OnSecondaryContainer = Color(0xFF1B5E20)

// TERTIARY - Accent colors
val Tertiary = Color(0xFFFF9800)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFFFF3E0)
val OnTertiaryContainer = Color(0xFFE65100)

// ERROR - Error states
val Error = Color(0xFFD32F2F)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFEBEE)
val OnErrorContainer = Color(0xFFB71C1C)

// BACKGROUND & SURFACE - Main layout colors
// Background: Tüm ekranın arka plan rengi (önceden #FAFAFA)
// Surface: Kartlar, dialoglar, bottom sheets için (önceden #FFFFFF)
val Background = Color(0xFFE7DFD3)             // ✨ Krem - Modern, göz yormaz
val OnBackground = Color(0xFF1C1B1F)           // Text on background
val Surface = Color(0xFFFFFFFF)                // ✨ Pure white - Kartlar için
val OnSurface = Color(0xFF1C1B1F)              // Text on surface (cards)
val SurfaceVariant = Color(0xFFF5F5F7)         // Subtle gray for dividers
val OnSurfaceVariant = Color(0xFF49454F)       // Text on surface variant

// BORDERS & OUTLINES
val Outline = Color(0xFFE5E5EA)                // Main borders
val OutlineVariant = Color(0xFFF0F0F2)         // Lighter borders

// ============================================================================
// DARK THEME - Material 3 Colors
// ============================================================================

// PRIMARY - Dark theme variants
val DarkPrimary = Color(0xFF90CAF9)
val DarkOnPrimary = Color(0xFF0D47A1)
val DarkPrimaryContainer = Color(0xFF1565C0)
val DarkOnPrimaryContainer = Color(0xFFE3F2FD)

// SECONDARY - Dark theme variants
val DarkSecondary = Color(0xFF81C784)
val DarkOnSecondary = Color(0xFF1B5E20)
val DarkSecondaryContainer = Color(0xFF388E3C)
val DarkOnSecondaryContainer = Color(0xFFE8F5E8)

// TERTIARY - Dark theme variants
val DarkTertiary = Color(0xFFFFB74D)
val DarkOnTertiary = Color(0xFFE65100)
val DarkTertiaryContainer = Color(0xFFF57C00)
val DarkOnTertiaryContainer = Color(0xFFFFF3E0)

// ERROR - Dark theme variants
val DarkError = Color(0xFFEF5350)
val DarkOnError = Color(0xFFB71C1C)
val DarkErrorContainer = Color(0xFFC62828)
val DarkOnErrorContainer = Color(0xFFFFEBEE)

// BACKGROUND & SURFACE - Dark theme layout colors
// Background: Tüm ekranın arka plan rengi (önceden #121212)
// Surface: Kartlar, dialoglar için (önceden #1E1E1E)
val DarkBackground = Color(0xFF181818)         // ✨ Çok koyu - AMOLED friendly
val DarkOnBackground = Color(0xFFE6E1E5)       // Text on dark background
val DarkSurface = Color(0xFF1C1C1C)            // ✨ Koyu gri - Kartlar için
val DarkOnSurface = Color(0xFFE6E1E5)          // Text on dark surface
val DarkSurfaceVariant = Color(0xFF2A2A2A)     // Darker variant for dividers
val DarkOnSurfaceVariant = Color(0xFFCAC4D0)   // Text on dark surface variant

// BORDERS & OUTLINES - Dark theme
val DarkOutline = Color(0xFF3A3A3A)            // Dark borders
val DarkOutlineVariant = Color(0xFF2F2F2F)     // Lighter dark borders

// ╔════════════════════════════════════════════════════════════════════════╗
// ║                    HOCALINGO CUSTOM COLORS                             ║
// ║                                                                        ║
// ║  Bu renkler HocaLingo'ya özel branding ve özel UI elementleri için    ║
// ║  Material 3 renkleriyle birlikte kullanılır                           ║
// ╚════════════════════════════════════════════════════════════════════════╝

object HocaColors {

    // ========================================================================
    // STUDY DIFFICULTY BUTTONS - Mevcut sistem (değişmeyecek)
    // ========================================================================
    val EasyGreen = Color(0xFF4CAF50)          // Kolay - Yeşil
    val MediumYellow = Color(0xFFFF9800)       // Orta - Turuncu/Sarı
    val HardRed = Color(0xFFE53935)            // Zor - Kırmızı

    // ========================================================================
    // PRIMARY BRAND COLORS - Ana marka renkleri
    // ========================================================================
    val Orange = Color(0xFFFF6B35)             // Ana turuncu (CTA, navigation)
    val OrangeDark = Color(0xFFE85A2B)         // 3D button shadow için
    val OrangeLight = Color(0xFFFF8C61)        // Hover states için

    // ========================================================================
    // SECONDARY BRAND COLORS - AI ve özel özellikler
    // ========================================================================
    val Purple = Color(0xFF7B61FF)             // AI assistant highlight
    val PurpleDark = Color(0xFF6247E5)         // Purple shadow
    val PurpleLight = Color(0xFF9B82FF)        // Purple hover

    // ========================================================================
    // ADDITIONAL ACCENT COLORS - Ek vurgu renkleri
    // ========================================================================
    val Turkuaz = Color(0xFF22ACB8)            // Turkuaz accent
    val FuturisticGreen = Color(0xFF64C27C)    // Futuristik yeşil

    // ========================================================================
    // SPECIAL INDICATORS - Özel göstergeler
    // ========================================================================
    val StreakFire = Color(0xFFFF5722)         // Streak göstergesi (ateş)
    val MasteredGold = Color(0xFFFFD700)       // Mastered word indicator (altın)

    // ========================================================================
    // SEMANTIC COLORS - Anlam taşıyan renkler
    // ========================================================================
    val Success = Color(0xFF34C759)            // Başarı mesajları
    val Warning = Color(0xFFFF9500)            // Uyarı mesajları
    val ErrorColor = Color(0xFFFF3B30)         // Hata mesajları
    val Info = Color(0xFF007AFF)               // Bilgi mesajları

    // ========================================================================
    // UI ELEMENTS - Genel UI elementleri
    // ========================================================================
    val CardShadow = Color(0x1A000000)         // Kart gölgesi (10% alpha black)
    val DividerGray = Color(0xFFE0E0E0)        // Ayırıcı çizgiler
    val HintGray = Color(0xFF9E9E9E)           // Hint text rengi

    // ========================================================================
    // 3D BUTTON SYSTEM - Top/Bottom renk çiftleri
    // ========================================================================
    // Easy button (Yeşil)
    val SuccessTop = Color(0xFF4CAF50)         // EasyGreen ile aynı
    val SuccessBottom = Color(0xFF2DA84D)      // Shadow

    // Medium button (Turuncu/Sarı)
    val WarningTop = Color(0xFFFF9800)         // MediumYellow ile aynı
    val WarningBottom = Color(0xFFE68600)      // Shadow

    // Hard button (Kırmızı)
    val ErrorTop = Color(0xFFE53935)           // HardRed ile aynı
    val ErrorBottom = Color(0xFFE6332A)        // Shadow

    // Primary button (Turuncu)
    val OrangeTop = Color(0xFFFF6B35)          // Orange ile aynı
    val OrangeBottom = Color(0xFFE85A2B)       // OrangeDark ile aynı

    // Purple button (Mor)
    val PurpleTop = Color(0xFF7B61FF)          // Purple ile aynı
    val PurpleBottom = Color(0xFF6247E5)       // PurpleDark ile aynı
}