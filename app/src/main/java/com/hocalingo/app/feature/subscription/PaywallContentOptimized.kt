package com.hocalingo.app.feature.subscription

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hocalingo.app.R
import com.revenuecat.purchases.Package

/**
 * PaywallContentOptimized - Modern Responsive Tasarım ✨
 *
 * Package: feature/subscription/
 *
 * Tasarım:
 * - Hero section (PNG)
 * - Features (3 emoji bullet)
 * - 3 yan yana responsive pricing card
 * - Compact bottom section
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

// Pastel Renk Paleti (Profesyonel)
private val pastelPurple = Color(0xFF3DA099)    // Aylık - Pastel mor
private val pastelGreen = Color(0xFFB44B2E)     // 3 Aylık - Pastel yeşil
private val pastelOrange = Color(0xFF604397)    // Yıllık - Pastel turuncu
private val redBadge = Color(0xFFFF6B6B)        // İndirim badge

// Helper data class for package info
private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun PaywallContentOptimized(
    uiState: SubscriptionUiState,
    onEvent: (SubscriptionEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.99f)  // ← 0.95'ten 0.99'a çıkar
            .verticalScroll(scrollState)
            .background(Color(0xFF1D021D)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ========================================
        // HERO SECTION
        // ========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.4f)
        ) {
            Image(
                painter = painterResource(R.drawable.paywall_image),
                contentDescription = "Premium",
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.TopCenter,
                contentScale = ContentScale.Crop
            )

            Text(
                text = "Premium Kelime Öğrenme Deneyimi",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 26.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp, start = 32.dp, end = 32.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ========================================
        // FEATURES (Compact)
        // ========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FeatureItem(emoji = "🚀", text = "Günlük 50 kelime ekleme hakkı")
            FeatureItem(emoji = "📚", text = "Reklamsız ve aralıksız deneyim")
            FeatureItem(emoji = "🤖", text = "Özelleştirilebilir yapay zeka")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ========================================
        // PRICING CARDS
        // ========================================
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = Color(0xFFFF9800),
                    strokeWidth = 3.dp
                )
            }
        } else {
            PricingSection(
                packages = uiState.availablePackages,
                selectedPackage = uiState.selectedPackage,
                isPurchasing = uiState.isPurchasing,
                onPackageClick = { pkg ->
                    onEvent(SubscriptionEvent.SelectPackage(pkg))
                    onEvent(SubscriptionEvent.PurchaseSelected)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ========================================
        // BOTTOM SECTION (Compact)
        // ========================================
        BottomSection(
            isRestoring = uiState.isRestoring,
            onRestoreClick = { onEvent(SubscriptionEvent.RestorePurchases) }
        )

        Spacer(modifier = Modifier.height(8.dp))  // ← 12'den 8'e düşür
    }
}

// =============================================
// FEATURE ITEM
// =============================================

@Composable
private fun FeatureItem(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = emoji,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = Color.White
        )
    }
}

// =============================================
// PRICING SECTION (3 Yan Yana Kart)
// =============================================

@Composable
private fun PricingSection(
    packages: List<Package>,
    selectedPackage: Package?,
    isPurchasing: Boolean,
    onPackageClick: (Package) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        packages.forEachIndexed { index, pkg ->
            PricingCard(
                packageItem = pkg,
                packageIndex = index,  // ← Index ekle
                isSelected = selectedPackage?.identifier == pkg.identifier,
                isPurchasing = isPurchasing && selectedPackage?.identifier == pkg.identifier,
                onClick = { onPackageClick(pkg) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// =============================================
// PRICING CARD (Responsive)
// =============================================

@Composable
private fun PricingCard(
    packageItem: Package,
    packageIndex: Int,  // ← Yeni parametre
    isSelected: Boolean,
    isPurchasing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Index'e göre paket bilgileri (RevenueCat sıralaması: 0=Aylık, 1=3Aylık, 2=Yıllık)
    val (periodText, subtitle, color, badgeText) = when (packageIndex) {
        0 -> Quadruple("Aylık", "Premium", pastelPurple, null)
        1 -> Quadruple("Yıllık", "Premium", pastelGreen, "%40 avantajlı")
        2 -> Quadruple("3 Aylık", "Premium", pastelOrange, null)
        else -> Quadruple("Premium", "", pastelPurple, null)
    }

    val price = packageItem.product.price.formatted

    Box(
        modifier = modifier.fillMaxHeight()
    ) {
        // Ana Kart (Surface + clickable ile ripple)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))  // ← Ripple için clip
                .clickable(
                    enabled = !isPurchasing,
                    onClick = onClick,
                    interactionSource = remember { MutableInteractionSource() }
                ),
            shape = RoundedCornerShape(20.dp),
            color = color,
            tonalElevation = if (isSelected) 8.dp else 3.dp,
            border = if (isSelected) BorderStroke(3.dp, Color.White) else null
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isPurchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                        color = Color.White
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Period (Aylık / 3 Aylık / Yıllık)
                        Text(
                            text = periodText,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        // Subtitle (Premium)
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Fiyat
                        Text(
                            text = price,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Badge (sadece yıllık için)
        if (badgeText != null) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp),
                shape = RoundedCornerShape(12.dp),
                color = redBadge
            ) {
                Text(
                    text = badgeText,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// =============================================
// BOTTOM SECTION (Compact)
// =============================================

@Composable
private fun BottomSection(
    isRestoring: Boolean,
    onRestoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "Dilediğin zaman iptal et"
        Text(
            text = "Dilediğin zaman iptal et",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        // Restore Button
        if (isRestoring) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = Color(0xFFFF9800)
            )
        } else {
            TextButton(
                onClick = onRestoreClick,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Restore et",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }

        // Bilgilendirme yazısı
        Text(
            text = "Abonelik otomatik olarak yenilenir. İptal etmezseniz süre bitiminde hesabınızdan ücret alınır.",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            lineHeight = 12.sp
        )
    }
}