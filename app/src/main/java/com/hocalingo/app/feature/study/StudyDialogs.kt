package com.hocalingo.app.feature.study

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hocalingo.app.R
import androidx.compose.runtime.*
import com.hocalingo.app.feature.subscription.PaywallBottomSheet

/**
 * StudyDialogs - Dialog Components & Completion Screen
 *
 * Package: feature/study/
 *
 * Components:
 * - StudyRewardedAdDialog: Rewarded ad dialog (shown every 10 words)
 * - StudyCompletionScreen: All words completed screen (YENILENDI)
 * - CompletionActionButton: Modern 3D action button
 * - CompletionStatCard: Animated stat card with gradient
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * Motivasyon mesajları - Random seçilecek
 */
private val completionMessages = listOf(
    "Harika İş Çıkardın! 🎉",
    "Muhteşem Performans! 🌟",
    "İnanılmaz İş! 🚀",
    "Süpersin! 💪",
    "Mükemmel! 🎯",
    "Harikaydı! ✨"
)

private val motivationSubtitles = listOf(
    "Bugünkü çalışmanı tamamladın!",
    "Hedefine bir adım daha yaklaştın!",
    "Öğrenme yolculuğunda ilerliyorsun!",
    "Her gün biraz daha iyisin!",
    "Başarıya doğru ilerliyorsun!"
)

/**
 * StudyRewardedAdDialog - Rewarded ad dialog
 *
 * Shows after every 10 words completed
 * User must watch ad to continue studying
 *
 * @param wordsCompleted Number of words completed so far
 * @param onContinue Callback to show rewarded ad
 * @param onDismiss Callback to skip ad (premium users)
 */
@Composable
fun StudyRewardedAdDialog(
    wordsCompleted: Int,
    onContinue: () -> Unit,
    onUpgradeToPremium: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color(0xFF2196F3).copy(alpha = 0.2f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    Text(text = "📺", fontSize = 48.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title
                Text(
                    text = "Harika Gidiyorsun!",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Message
                Text(
                    text = "$wordsCompleted kelime çalıştın! Devam etmek için kısa bir video izle.",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Continue Button
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4ECDC4)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Devam Et",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Premium Link
                TextButton(onClick = onUpgradeToPremium) {
                    Text(
                        text = "Premium'a geç, reklamsız çalış",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color(0xFF7B61FF)
                    )
                }
            }
        }
    }
}

/**
 * StudyCompletionScreen - All words completed screen
 *
 * YENILENDI - Modern, motive edici tasarım:
 * - Confetti animation
 * - Random motivasyon mesajları
 * - Gradient stat cards
 * - 3D action buttons (Duolingo-style)
 * - Premium teaser
 *
 * @param onNavigateToWordSelection Navigate to word selection screen
 * @param onNavigateToHome Navigate to home screen
 */
@Composable
fun StudyCompletionScreen(
    onNavigateToWordSelection: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    // Random motivasyon mesajları
    val titleMessage = remember { completionMessages.random() }
    val subtitleMessage = remember { motivationSubtitles.random() }

    // Animasyonlar
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")

    // Scale animation for success icon
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = 0.5f,
            stiffness = Spring.StiffnessLow
        ),
        label = "success_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // ========== SUCCESS ANIMATION ==========
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow effect 1
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(scale)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFD700).copy(alpha = 0.3f),
                                Color(0xFFFFA500).copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Glow effect 2 (rotating)
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(3000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "glow_rotation"
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .rotate(rotation)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4CAF50).copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            // Success emoji
            Text(
                text = "🎉",
                fontSize = 80.sp,
                modifier = Modifier.scale(scale)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ========== TITLE & SUBTITLE ==========
        Text(
            text = titleMessage,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = subtitleMessage,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ========== STATS CARDS ==========
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompletionStatCard(
                icon = Icons.Filled.CheckCircle,
                value = "Başarılı",
                label = "Bugünkü Hedef",
                gradientColors = listOf(Color(0xFF4CAF50), Color(0xFF66BB6A)),
                modifier = Modifier.weight(1f)
            )

            CompletionStatCard(
                icon = Icons.Filled.LocalFireDepartment,
                value = "Aktif",
                label = "Streak",
                gradientColors = listOf(Color(0xFFFF6B35), Color(0xFFFF8C00)),
                modifier = Modifier.weight(1f)
            )

            CompletionStatCard(
                icon = Icons.Filled.EmojiEvents,
                value = "Harika!",
                label = "Başarı",
                gradientColors = listOf(Color(0xFFFFC107), Color(0xFFFFB300)),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ========== MOTIVATIONAL MESSAGE ==========
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "💡", fontSize = 32.sp)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Yeni kelimeler ekleyerek öğrenmeye devam et!",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ========== ACTION BUTTONS (3D Duolingo-style) ==========
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Primary: Yeni Kelimeler Seç
            CompletionActionButton(
                text = "Yeni Kelimeler Seç",
                icon = Icons.Filled.Add,
                topColor = Color(0xFF4ECDC4),
                bottomColor = Color(0xFF37A89A),
                onClick = onNavigateToWordSelection
            )

            // Secondary: Ana Sayfa
            CompletionActionButton(
                text = "Ana Sayfaya Dön",
                icon = Icons.Filled.Home,
                topColor = Color(0xFF6366F1),
                bottomColor = Color(0xFF4F46E5),
                onClick = onNavigateToHome
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ========== PREMIUM TEASER (Opsiyonel) ==========
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateToPremium() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF7B61FF).copy(alpha = 0.2f),
                                Color(0xFF9B51E0).copy(alpha = 0.2f)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "👑", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "HocaLingo Premium",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF7B61FF)
                            )
                            Text(
                                text = "Sınırsız öğrenme deneyimi",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF7B61FF)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

/**
 * CompletionStatCard - Animated gradient stat card
 *
 * @param icon Card icon
 * @param value Stat value text
 * @param label Stat label
 * @param gradientColors Gradient colors for background
 */
@Composable
private fun CompletionStatCard(
    icon: ImageVector,
    value: String,
    label: String,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    // Entry animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier.aspectRatio(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = gradientColors
                        )
                    )
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = value,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = label,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * CompletionActionButton - 3D Duolingo-style action button
 *
 * Features:
 * - 3D pressed effect
 * - Smooth spring animation
 * - Icon + Text layout
 *
 * @param text Button text
 * @param icon Button icon
 * @param topColor Button top layer color
 * @param bottomColor Button bottom layer (shadow) color
 * @param onClick Click callback
 */
@Composable
private fun CompletionActionButton(
    text: String,
    icon: ImageVector,
    topColor: Color,
    bottomColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val offsetY by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMedium
        ),
        label = "button_press"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
    ) {
        // Bottom layer (shadow/depth) - Static
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(16.dp))
                .background(bottomColor)
        )

        // Top layer (interactive surface)
        Surface(
            onClick = {
                onClick()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .offset(y = offsetY)
                .clickable {
                    isPressed = true
                },
            shape = RoundedCornerShape(16.dp),
            color = topColor
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = text,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
            }
        }
    }

    // Reset pressed state
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}