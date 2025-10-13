package com.hocalingo.app.feature.subscription

import android.annotation.SuppressLint
import androidx.compose.animation.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revenuecat.purchases.Package

/**
 * PaywallContent
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/subscription/
 *
 * Conversion-optimized paywall design
 * Based on successful subscription app patterns
 */
@Composable
fun PaywallContent(
    uiState: SubscriptionUiState,
    onEvent: (SubscriptionEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Section
        HeroSection()

        Spacer(modifier = Modifier.height(24.dp))

        // Features Grid
        FeaturesGrid()

        Spacer(modifier = Modifier.height(24.dp))

        // Social Proof
        SocialProofBadge()

        Spacer(modifier = Modifier.height(32.dp))

        // Pricing Cards
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color(0xFFFF9800)
            )
        } else {
            PricingSection(
                packages = uiState.availablePackages,
                selectedPackage = uiState.selectedPackage,
                onPackageSelected = { pkg ->
                    onEvent(SubscriptionEvent.SelectPackage(pkg))
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // CTA Button
        CTAButton(
            isLoading = uiState.isPurchasing,
            onPurchaseClick = { onEvent(SubscriptionEvent.PurchaseSelected) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Trust Signal
        TrustSignal()

        Spacer(modifier = Modifier.height(12.dp))

        // Restore Purchases
        TextButton(
            onClick = { onEvent(SubscriptionEvent.RestorePurchases) },
            enabled = !uiState.isRestoring
        ) {
            if (uiState.isRestoring) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "Satın Alımları Geri Yükle",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun HeroSection() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "✨ HocaLingo Premium",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Hedeflerine daha hızlı ulaş",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FeaturesGrid() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeatureItem(
            icon = Icons.Default.AutoStories,
            title = "Sınırsız\nKelime",
            modifier = Modifier.weight(1f)
        )
        FeatureItem(
            icon = Icons.Default.Psychology,
            title = "AI\nAsistanı",
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FeatureItem(
            icon = Icons.Default.Block,
            title = "Reklamsız\nDeneyim",
            modifier = Modifier.weight(1f)
        )
        FeatureItem(
            icon = Icons.Default.Insights,
            title = "Detaylı\nİstatistik",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF424242),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun SocialProofBadge() {
    Row(
        modifier = Modifier
            .background(
                Color(0xFF4CAF50).copy(alpha = 0.1f),
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.VerifiedUser,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "10,000+ mutlu kullanıcı",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2E7D32)
        )
    }
}

@Composable
private fun PricingSection(
    packages: List<Package>,
    selectedPackage: Package?,
    onPackageSelected: (Package) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        packages.forEach { pkg ->
            val isSelected = pkg.identifier == selectedPackage?.identifier
            val isPopular = pkg.identifier.contains("quarterly")

            PricingCard(
                packageItem = pkg,
                isSelected = isSelected,
                isPopular = isPopular,
                onClick = { onPackageSelected(pkg) }
            )
        }
    }
}

@Composable
private fun PricingCard(
    packageItem: Package,
    isSelected: Boolean,
    isPopular: Boolean,
    onClick: () -> Unit
) {
    val title = when {
        packageItem.identifier.contains("monthly") -> "Aylık"
        packageItem.identifier.contains("quarterly") -> "3 Aylık"
        packageItem.identifier.contains("yearly") -> "Yıllık"
        else -> packageItem.identifier
    }

    val price = packageItem.product.price.formatted

    val borderColor = when {
        isSelected -> Color(0xFFFF9800)
        isPopular -> Color(0xFF4CAF50)
        else -> Color.Transparent
    }

    val backgroundColor = when {
        isSelected -> Color(0xFFFFF3E0)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isPopular) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Popular badge
            if (isPopular) {
                Row(
                    modifier = Modifier
                        .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "EN POPÜLER",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
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
                Column {
                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "🎁 7 gün ücretsiz",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = price,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )

                    // Price per day/month calculation
                    Text(
                        text = getPricePerPeriod(packageItem),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun CTAButton(
    isLoading: Boolean,
    onPurchaseClick: () -> Unit
) {
    Button(
        onClick = onPurchaseClick,
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF9800),
                            Color(0xFFFF6F00)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text(
                    text = "Hemen Başla",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun TrustSignal() {
    Text(
        text = "✓ İstediğin zaman iptal edebilirsin",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

/**
 * Helper: Calculate price per day or per month
 */
@SuppressLint("DefaultLocale")
private fun getPricePerPeriod(packageItem: Package): String {
    val priceString = packageItem.product.price.formatted
    val priceValue = priceString.replace(Regex("[^0-9.,]"), "")
        .replace(",", ".")
        .toDoubleOrNull() ?: 0.0

    return when {
        packageItem.identifier.contains("monthly") -> {
            "günde ${String.format("%.2f", priceValue / 30)} TL"
        }
        packageItem.identifier.contains("quarterly") -> {
            "ayda ${String.format("%.2f", priceValue / 3)} TL"
        }
        packageItem.identifier.contains("yearly") -> {
            "ayda ${String.format("%.2f", priceValue / 12)} TL"
        }
        else -> ""
    }
}