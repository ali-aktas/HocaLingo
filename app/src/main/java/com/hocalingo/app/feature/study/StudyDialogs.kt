package com.hocalingo.app.feature.study

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hocalingo.app.R

/**
 * StudyDialogs - Dialog Components & Completion Screen
 *
 * Package: feature/study/
 *
 * Components:
 * - StudyRewardedAdDialog: Rewarded ad dialog (shown every 10 words)
 * - StudyCompletionScreen: All words completed screen
 * - CompletionActionCard: Action cards for new words/home navigation
 * - CompletionStat: Stat icons (badges, streaks, trophies)
 */

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
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
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF6366F1),
                                Color(0xFF8B5CF6)
                            )
                        )
                    )
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hoca Logo
                Image(
                    painter = painterResource(id = R.drawable.lingo_happy),
                    contentDescription = "Hoca Logo",
                    modifier = Modifier.size(100.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = "Harika Gidiyorsun!",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Message
                Text(
                    text = "$wordsCompleted kart çalıştın! 🚀\n\nÖğrenmeye devam etmek için kısa bir reklam izle.",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Continue Button
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Devam Et",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ✅ Premium Upgrade Button
                OutlinedButton(
                    onClick = onUpgradeToPremium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .border(
                            width = 1.5.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF7C3AED),
                                    Color(0xFF9D5CFF)
                                )
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color(0xFF7C3AED)
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "Premium'a Geç - Reklamsız",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF7C3AED)
                        )
                    }
                }
            }
        }
    }
}

/**
 * StudyCompletionScreen - All words completed screen
 *
 * Shows when user finishes all words in queue
 * Displays achievement stats and navigation options
 *
 * @param onNavigateToWordSelection Navigate to word selection screen
 * @param onNavigateToHome Navigate to home screen
 */
@Composable
fun StudyCompletionScreen(
    onNavigateToWordSelection: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Success Icon
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF4CAF50).copy(alpha = 0.2f),
                                Color(0xFF4CAF50).copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            Text(text = "🎉", fontSize = 64.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Text(
            text = "Harika İş Çıkardın!",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        Text(
            text = "Bugün tüm kelimeleri çalıştın.\nÖğrenmeye devam etmek için yeni kelimeler seç!",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Achievement Stats
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompletionStat(
                    icon = Icons.Filled.CheckCircle,
                    label = "Çalışma tamamlandı",
                    color = Color(0xFF4CAF50)
                )
                CompletionStat(
                    icon = Icons.Filled.LocalFireDepartment,
                    label = "Streak devam",
                    color = Color(0xFFFF5722)
                )
                CompletionStat(
                    icon = Icons.Filled.EmojiEvents,
                    label = "Hedef tamamlandı",
                    color = Color(0xFFFFC107)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Action Cards
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CompletionActionCard(
                title = "Yeni Kelimeler Seç",
                subtitle = "Öğrenmeye devam et",
                icon = Icons.Filled.Add,
                backgroundColor = Brush.linearGradient(
                    colors = listOf(Color(0xFF4ECDC4), Color(0xFF44A08D))
                ),
                onClick = onNavigateToWordSelection
            )

            CompletionActionCard(
                title = "Ana Sayfaya Dön",
                subtitle = "İlerlemeni kontrol et",
                icon = Icons.Filled.Home,
                backgroundColor = Brush.linearGradient(
                    colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                ),
                onClick = onNavigateToHome
            )
        }
    }
}

/**
 * CompletionActionCard - Action card for navigation
 *
 * @param title Card title
 * @param subtitle Card subtitle
 * @param icon Card icon
 * @param backgroundColor Gradient background
 * @param onClick Click callback
 */
@Composable
private fun CompletionActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    backgroundColor: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(120.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundColor)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

/**
 * CompletionStat - Achievement stat icon
 *
 * @param icon Stat icon
 * @param label Stat label
 * @param color Icon color
 */
@Composable
private fun CompletionStat(
    icon: ImageVector,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}