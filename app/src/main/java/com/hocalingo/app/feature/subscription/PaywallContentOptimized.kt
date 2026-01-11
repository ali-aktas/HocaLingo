package com.hocalingo.app.feature.subscription

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
 * PaywallContentOptimized - YENİ TASARIM ✨
 *
 * Package: feature/subscription/
 *
 * Tasarım:
 * - Hero section (PNG + Mor gradient)
 * - Features (3 bullet points)
 * - Pricing cards (3 adet, direkt tıklanabilir)
 * - Restore purchases link
 * - Terms & policy links
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

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
            .fillMaxHeight(0.95f) // %95 ekranı kapla
            .verticalScroll(scrollState)
            .background(Color(0xFF1D021D)), // Tüm sayfa bu mor renk
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ========================================
        // HERO SECTION (PNG + Title Overlay)
        // ========================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f) // Üst yarıyı kapla
        ) {
            // PNG tam kaplar
            Image(
                painter = painterResource(R.drawable.paywall_image),
                contentDescription = "Premium",
                modifier = Modifier.fillMaxSize(),
                alignment = Alignment.TopCenter,
                contentScale = ContentScale.Crop // Tam oturur
            )

            // Title overlay (görselin ALT kısmında, ortada)
            Text(
                text = "Premium Kelime Öğrenme Deneyimi",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp, start = 24.dp, end = 24.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========================================
        // FEATURES (Emoji ile)
        // ========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        ) {
            FeatureItem(emoji = "🚀", text = "Daha çok kelime seçme imkanı")
            Spacer(modifier = Modifier.height(12.dp))
            FeatureItem(emoji = "\uD83D\uDCDA", text = "Tamamen reklamsız kullanım")
            Spacer(modifier = Modifier.height(12.dp))
            FeatureItem(emoji = "🤖", text = "Özelleştirilebilir yapay zeka deneyimi")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========================================
        // PRICING CARDS (3 adet, tıklanabilir)
        // ========================================
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
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
                    // ✅ Direkt satın alma (CTA button yok)
                    onEvent(SubscriptionEvent.SelectPackage(pkg))
                    onEvent(SubscriptionEvent.PurchaseSelected)
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========================================
        // TERMS & RESTORE
        // ========================================
        BottomLinks(
            isRestoring = uiState.isRestoring,
            onRestoreClick = { onEvent(SubscriptionEvent.RestorePurchases) }
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

// =============================================
// FEATURE ITEM - Emoji ile
// =============================================

@Composable
private fun FeatureItem(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Emoji
        Text(
            text = emoji,
            fontSize = 20.sp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = text,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = Color.White
        )
    }
}

// =============================================
// PRICING SECTION - 3 Tıklanabilir Kart
// =============================================

@Composable
private fun PricingSection(
    packages: List<Package>,
    selectedPackage: Package?,
    isPurchasing: Boolean,
    onPackageClick: (Package) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        packages.forEach { pkg ->
            PricingCard(
                packageItem = pkg,
                isSelected = selectedPackage?.identifier == pkg.identifier,
                isPurchasing = isPurchasing && selectedPackage?.identifier == pkg.identifier,
                onClick = { onPackageClick(pkg) }
            )
        }
    }
}

@Composable
private fun PricingCard(
    packageItem: Package,
    isSelected: Boolean,
    isPurchasing: Boolean,
    onClick: () -> Unit
) {
    // Paket tipine göre bilgiler
    val (title, color, isPopular) = when {
        packageItem.identifier.contains("monthly", ignoreCase = true) -> {
            Triple("Aylık Premium", Color(0xFFFF6B6B), false)
        }
        packageItem.identifier.contains("quarterly", ignoreCase = true) -> {
            Triple("3 Aylık Premium", Color(0xFF4CAF50), true) // Yeşil + Popüler
        }
        packageItem.identifier.contains("yearly", ignoreCase = true) -> {
            Triple("Yıllık Premium", Color(0xFFFFB84D), false)
        }
        else -> {
            Triple(packageItem.product.title, Color(0xFF999999), false)
        }
    }

    val price = packageItem.product.price.formatted

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(enabled = !isPurchasing) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPurchasing) {
                Color.LightGray
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Popüler badge (sadece 3 aylık için)
                if (isPopular) {
                    Row(
                        modifier = Modifier
                            .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "⭐ EN POPÜLER",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sol taraf: Başlık + 7 gün ücretsiz
                    Column {
                        Text(
                            text = title,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "🎁 7 gün ücretsiz",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }

                    // Sağ taraf: Fiyat veya loading
                    if (isPurchasing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                            color = color
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = price,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = color
                            )

                            if (isSelected) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Seçili",
                                    tint = color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =============================================
// BOTTOM LINKS - Terms & Restore
// =============================================

@Composable
private fun BottomLinks(
    isRestoring: Boolean,
    onRestoreClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // İptal policy
        TextButton(onClick = { /* TODO: İptal politikası göster */ }) {
            Text(
                text = "İstediğin zaman iptal et",
                fontFamily = PoppinsFontFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Restore purchases
        TextButton(
            onClick = onRestoreClick,
            enabled = !isRestoring
        ) {
            if (isRestoring) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "Restore et",
                fontFamily = PoppinsFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Terms text (küçük)
        Text(
            text = "Abonelik otomatik yenilenir. İptal etmek istediğinizde \n Google Play Ayarlar>Aboneliklerim ile yönetebilirsiniz..",
            fontFamily = PoppinsFontFamily,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}