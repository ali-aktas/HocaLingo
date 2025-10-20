package com.hocalingo.app.feature.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay

/**
 * LoadingAnimationDialog.kt
 *
 * Package: app/src/main/java/com/hocalingo/app/feature/onboarding/
 *
 * Güzel bir yüklenme animasyonu dialog'u
 * - Hoca logosu
 * - Progress bar animasyonu
 * - 3 saniye süre
 * - Otomatik kapanma ve navigation
 */

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

@Composable
fun LoadingAnimationDialog(
    onDismiss: () -> Unit,
    isDarkTheme: Boolean = false
) {
    // Progress animation (0 -> 1 in 3 seconds)
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val duration = 2000L // 2 saniye
        val steps = 60 // 60 FPS
        val delayTime = duration / steps
        val increment = 1f / steps

        repeat(steps) {
            delay(delayTime)
            progress += increment
        }

        // Animation bitti, dialog'u kapat
        onDismiss()
    }

    Dialog(
        onDismissRequest = { /* Kapatılmaz */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = if (isDarkTheme) {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1A1A2E),
                                Color(0xFF16213E)
                            )
                        )
                    } else {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF8F9FA),
                                Color(0xFFE9ECEF)
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(
                        color = if (isDarkTheme) {
                            Color(0xFF2D2D44).copy(alpha = 0.95f)
                        } else {
                            Color.White.copy(alpha = 0.95f)
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hoca Logo (R.drawable.ic_launcher_foreground veya senin logo'n)
                Image(
                    painter = painterResource(id = R.drawable.main_screen_card),
                    contentDescription = "HocaLingo Logo",
                    modifier = Modifier.size(120.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Başlık
                Text(
                    text = "Kelimelerin Hazırlanıyor",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = if (isDarkTheme) Color.White else Color(0xFF2D3748),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Alt yazı
                Text(
                    text = "Harika bir öğrenme deneyimi için her şey ayarlanıyor...",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (isDarkTheme) {
                        Color.White.copy(alpha = 0.7f)
                    } else {
                        Color(0xFF718096)
                    },
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Progress Bar
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = if (isDarkTheme) {
                            Color(0xFF4FC3F7)
                        } else {
                            Color(0xFF00D4FF)
                        },
                        trackColor = if (isDarkTheme) {
                            Color(0xFF37474F)
                        } else {
                            Color(0xFFE0E0E0)
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Yüzde göstergesi
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = if (isDarkTheme) {
                            Color(0xFF4FC3F7)
                        } else {
                            Color(0xFF00D4FF)
                        }
                    )
                }
            }
        }
    }
}