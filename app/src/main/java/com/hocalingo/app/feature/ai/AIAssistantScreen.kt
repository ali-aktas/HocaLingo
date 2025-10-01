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
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.ThemeViewModel

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * AI Assistant Screen - Professional Coming Soon
 * ✅ Showcases future AI features
 * ✅ HocaLingo branding
 * ✅ Theme-aware design
 * ✅ Clean and modern UI
 */
@Composable
fun AIAssistantScreen(
    onNavigateBack: () -> Unit = {}
) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "AI Asistan",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold
                    )
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // Hero Section - HocaLingo AI
            item {
                HeroSection(isDarkTheme)
            }

            // Coming Soon Badge
            item {
                ComingSoonBadge(isDarkTheme)
            }

            // Features Preview
            item {
                Text(
                    text = "Yakında Gelecek Özellikler",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Feature Cards
            item {
                AIFeatureCard(
                    icon = Icons.Filled.AutoStories,
                    title = "Kişiselleştirilmiş Hikayeler",
                    description = "Çalıştığınız kelimelerle oluşturulan özel hikayeler. Seviyenize uygun, eğlenceli ve öğretici içerikler.",
                    gradient = if (isDarkTheme) {
                        listOf(Color(0xFF6B46C1), Color(0xFF553C9A))
                    } else {
                        listOf(Color(0xFF667eea), Color(0xFF764ba2))
                    }
                )
            }

            item {
                AIFeatureCard(
                    icon = Icons.Filled.MenuBook,
                    title = "Okuma Metinleri",
                    description = "Kelime dağarcığınızı geliştirmek için özel tasarlanmış okuma parçaları. Her seviyeye uygun içerik.",
                    gradient = if (isDarkTheme) {
                        listOf(Color(0xFF0F766E), Color(0xFF115E59))
                    } else {
                        listOf(Color(0xFF4ECDC4), Color(0xFF44A08D))
                    }
                )
            }

            item {
                AIFeatureCard(
                    icon = Icons.Filled.ChatBubble,
                    title = "Akıllı Örnek Cümleler",
                    description = "Her kelime için bağlama uygun, günlük hayattan örnekler. AI destekli açıklamalar ve kullanım önerileri.",
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
                    description = "Kelimeler hakkında soru sorun, anlamları keşfedin, kullanım alanlarını öğrenin. 7/24 AI desteği.",
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
                    description = "AI tarafından oluşturulan özel alıştırmalar ve quizler. Öğrenme hızınıza göre uyarlanır.",
                    gradient = if (isDarkTheme) {
                        listOf(Color(0xFFB8860B), Color(0xFFFFD700))
                    } else {
                        listOf(Color(0xFFF7971E), Color(0xFFFFD200))
                    }
                )
            }

            // Bottom Spacer
            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun HeroSection(isDarkTheme: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                // AI Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Title
                Text(
                    text = "HocaLingo AI",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                // Subtitle
                Text(
                    text = "Yapay Zeka Destekli\nKelime Öğrenme Deneyimi",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }
    }
}

@Composable
private fun ComingSoonBadge(isDarkTheme: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(50.dp),
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
                    text = "Çok Yakında",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
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
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(colors = gradient)
                )
                .padding(20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Content
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
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