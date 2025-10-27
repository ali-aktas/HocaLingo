package com.hocalingo.app.feature.subscription

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * PaywallContentOptimized - More Elegant & Compact ✨
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * ✅ Smaller, more refined UI elements
 * ✅ Better spacing and hierarchy
 * ✅ More premium feel
 * ✅ Optimized for conversion
 */
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
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Section - More compact
        HeroSectionCompact()

        Spacer(modifier = Modifier.height(20.dp))

        // Features Grid - Smaller, cleaner
        FeaturesGridCompact()

        Spacer(modifier = Modifier.height(20.dp))

        // Social Proof - Subtle
        SocialProofBadgeCompact()

        Spacer(modifier = Modifier.height(24.dp))

        // Pricing Cards - More elegant
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = Color(0xFFFF9800),
                strokeWidth = 3.dp
            )
        } else {
            PricingSectionCompact(
                packages = uiState.availablePackages,
                selectedPackage = uiState.selectedPackage,
                onPackageSelected = { pkg ->
                    onEvent(SubscriptionEvent.SelectPackage(pkg))
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CTA Button - More premium
        CTAButtonCompact(
            isLoading = uiState.isPurchasing,
            onPurchaseClick = { onEvent(SubscriptionEvent.PurchaseSelected) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Terms - Smaller
        TermsTextCompact()

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// =============================================
// HERO SECTION - Compact Version
// =============================================

@Composable
private fun HeroSectionCompact() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icon - Smaller
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFB74D),
                            Color(0xFFFF9800)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.main_screen_card), // ✅ Kendi drawable'ını koy
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title - More compact
        Text(
            text = "Hocalingo Premium",
            fontFamily = PoppinsFontFamily,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitle - Smaller
        Text(
            text = "Öğrenme deneyimini bir üst seviyeye taşı",
            fontFamily = PoppinsFontFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}

// =============================================
// FEATURES GRID - Compact Version
// =============================================

@Composable
private fun FeaturesGridCompact() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeatureItemCompact(
                icon = Icons.Default.Block,
                title = "Reklamsız Deneyim",
                modifier = Modifier.weight(1f)
            )
            FeatureItemCompact(
                icon = Icons.Default.AutoAwesome,
                title = "Yapay Zeka Asistanı",
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeatureItemCompact(
                icon = Icons.Default.NoteAlt,
                title = "Çalıştığın Kelimelere Özel Hikayeler",
                modifier = Modifier.weight(1f)
            )
            FeatureItemCompact(
                icon = Icons.Default.SaveAlt,
                title = "Yeni Eklenecek Binlerce Kelime ve Kalıp",
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FeatureItemCompact(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        }
    }
}

// =============================================
// SOCIAL PROOF - Compact Version
// =============================================

@Composable
private fun SocialProofBadgeCompact() {
    Row(
        modifier = Modifier
            .background(
                Color(0xFF4CAF50).copy(alpha = 0.12f),
                RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        Text(
            text = "Türkiye için geliştirildi",
            fontFamily = PoppinsFontFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2E7D32)
        )
    }
}

// =============================================
// PRICING SECTION - Compact Version
// =============================================

@Composable
private fun PricingSectionCompact(
    packages: List<Package>,
    selectedPackage: Package?,
    onPackageSelected: (Package) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        packages.forEach { pkg ->
            PricingCardCompact(
                packageItem = pkg,
                isSelected = selectedPackage?.identifier == pkg.identifier,
                onClick = { onPackageSelected(pkg) }
            )
        }
    }
}

@Composable
private fun PricingCardCompact(
    packageItem: Package,
    isSelected: Boolean,
    onClick: () -> Unit
) {

    // ✅ YENİ: Paket tipine göre başlık belirle
    val title = when {
        packageItem.identifier.contains("monthly", ignoreCase = true) -> "Aylık"
        packageItem.identifier.contains("quarterly", ignoreCase = true) -> "3 Aylık"
        packageItem.identifier.contains("three", ignoreCase = true) -> "3 Aylık"
        packageItem.identifier.contains("yearly", ignoreCase = true) -> "Yıllık"
        packageItem.identifier.contains("annual", ignoreCase = true) -> "Yıllık"
        else -> "Premium"
    }

    // ✅ YENİ: Popular badge için kontrol
    val isPopular = packageItem.identifier.contains("quarterly", ignoreCase = true) ||
            packageItem.identifier.contains("three", ignoreCase = true)

    val borderColor = if (isSelected) {
        Color(0xFFFF9800)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }

    val backgroundColor = if (isSelected) {
        Color(0xFFFFE0B2)  // ✅ Daha belirgin açık turuncu
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontFamily = PoppinsFontFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF424242) else MaterialTheme.colorScheme.onSurface  // ✅ Seçiliyse koyu gri
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = packageItem.product.description,
                    fontFamily = PoppinsFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = packageItem.product.price.formatted,
                    fontFamily = PoppinsFontFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF9800)
                )

                if (isSelected) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Seçili",
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// =============================================
// CTA BUTTON - Compact Version
// =============================================

@Composable
private fun CTAButtonCompact(
    isLoading: Boolean,
    onPurchaseClick: () -> Unit
) {
    Button(
        onClick = onPurchaseClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF9800),
            disabledContainerColor = Color(0xFFFF9800).copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "Premium Üyesi Ol",
                fontFamily = PoppinsFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// =============================================
// TERMS - Compact Version
// =============================================

@Composable
private fun TermsTextCompact() {
    Text(
        text = "Abonelik otomatik yenilenir. İptal için ayarlardan yönetin.",
        fontFamily = PoppinsFontFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}