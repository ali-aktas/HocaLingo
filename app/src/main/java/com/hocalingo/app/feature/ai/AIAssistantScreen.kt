package com.hocalingo.app.feature.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import com.hocalingo.app.feature.subscription.PaywallBottomSheet
import com.hocalingo.app.feature.subscription.SubscriptionViewModel

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * AI Assistant Screen - Premium Feature
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/ai/
 *
 * ✅ Premium control
 * ✅ Paywall integration
 * ✅ Coming soon content for premium users
 */
@Composable
fun AIAssistantScreen(
    onNavigateBack: () -> Unit = {},
    subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    val subscriptionState by subscriptionViewModel.uiState.collectAsStateWithLifecycle()
    val isPremium = subscriptionState.currentSubscription.isPremium &&
            subscriptionState.currentSubscription.isActive()

    var showPaywall by remember { mutableStateOf(!isPremium) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Yapay Zeka Asistanı",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        if (isPremium) {
                            Icon(
                                imageVector = Icons.Default.Stars,
                                contentDescription = "Premium",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        if (isPremium) {
            // Premium content (Coming Soon)
            PremiumAIContent(
                isDarkTheme = isDarkTheme,
                modifier = Modifier.padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp
                )

            )
        } else {
            // Free user - show teaser + upgrade button
            FreeUserContent(
                isDarkTheme = isDarkTheme,
                onUpgradeClick = { showPaywall = true },
                modifier = Modifier.padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp
                )

            )
        }
    }

    // Paywall BottomSheet
    if (showPaywall && !isPremium) {
        PaywallBottomSheet(
            onDismiss = { showPaywall = false },
            onPurchaseSuccess = {
                showPaywall = false
                // Premium activated, screen will auto-update
            }
        )
    }
}

/**
 * Premium users see this (Coming Soon content)
 */
@Composable
private fun PremiumAIContent(
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            HeroSection(isDarkTheme, isPremium = true)
        }

        item {
            AIFeatureCard(
                icon = Icons.Filled.AutoStories,
                title = "Hikaye Oluşturucu",
                description = "Öğrendiğiniz kelimelerle kişiselleştirilmiş hikayeler. Seviyenize göre otomatik uyarlanır.",
                gradient = if (isDarkTheme) {
                    listOf(Color(0xFFB45433), Color(0xFF927E71))
                } else {
                    listOf(Color(0xFFFF6B35), Color(0xFFFF8E53))
                }
            )
        }

        item {
            AIFeatureCard(
                icon = Icons.Filled.Psychology,
                title = "Kelime Asistanı",
                description = "Kelimeler hakkında soru sorun, anlamları keşfedin, kullanım alanlarını öğrenin.",
                gradient = if (isDarkTheme) {
                    listOf(Color(0xFF1E3A8A), Color(0xFF1E40AF))
                } else {
                    listOf(Color(0xFF3B82F6), Color(0xFF2563EB))
                }
            )
        }

        item {
            AIFeatureCard(
                icon = Icons.Filled.Quiz,
                title = "İnteraktif Alıştırmalar",
                description = "AI tarafından oluşturulan özel alıştırmalar ve quizler.",
                gradient = if (isDarkTheme) {
                    listOf(Color(0xFFB8860B), Color(0xFFFFD700))
                } else {
                    listOf(Color(0xFFF7971E), Color(0xFFFFD200))
                }
            )
        }

        item {
            ComingSoonBadge(isDarkTheme)
        }
    }
}

/**
 * Free users see this (Teaser + Upgrade)
 */
@Composable
private fun FreeUserContent(
    isDarkTheme: Boolean,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Lock Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(
                    color = Color(0xFFFF9800).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(50.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(50.dp)
            )
        }

        // Title
        Text(
            text = "Yapay Zeka Asistanı",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Premium özellik",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Features
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeatureRow(
                icon = Icons.Default.AutoStories,
                text = "Kişiselleştirilmiş hikayeler"
            )
            FeatureRow(
                icon = Icons.Default.Psychology,
                text = "7/24 AI kelime asistanı"
            )
            FeatureRow(
                icon = Icons.Default.Quiz,
                text = "Interaktif alıştırmalar"
            )
            FeatureRow(
                icon = Icons.Default.TrendingUp,
                text = "Hızlandırılmış öğrenme"
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Upgrade Button
        Button(
            onClick = onUpgradeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent
            ),
            contentPadding = PaddingValues(0.dp),
            shape = RoundedCornerShape(16.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stars,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = "Premium Üyesi Ol",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                }
            }
        }

        Text(
            text = "7 gün ücretsiz dene",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color(0xFF4CAF50)
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HeroSection(
    isDarkTheme: Boolean,
    isPremium: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF4A148C), Color(0xFF6A1B9A))
                        } else {
                            listOf(Color(0xFF667eea), Color(0xFF764ba2))
                        }
                    )
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(40.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Text(
                    text = "AI Asistanınız",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isPremium) {
                        "Yakında aktif olacak"
                    } else {
                        "Öğrenmeyi hızlandırın"
                    },
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AIFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    gradient: List<Color>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush = Brush.linearGradient(colors = gradient))
                .padding(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = description,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ComingSoonBadge(isDarkTheme: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkTheme) {
            Color(0xFF2DD4BF)
        } else {
            Color(0xFF14B8A6)
        },
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Rocket,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Çok Yakında Aktif",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}