package com.hocalingo.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.HocaLingoTheme

// Poppins font family
val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * Modern, şık dialog component
 * Tüm uygulama boyunca kullanılabilir
 */
@Composable
fun HocaDialog(
    title: String,
    message: String,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    confirmButtonText: String = "Tamam",
    dismissButtonText: String? = null,
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
    isDismissible: Boolean = true
) {
    Dialog(
        onDismissRequest = (if (isDismissible) onDismissRequest else {}) as () -> Unit,
        properties = DialogProperties(
            dismissOnBackPress = isDismissible,
            dismissOnClickOutside = isDismissible
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon (if provided)
                icon?.let {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        iconTint.copy(alpha = 0.15f),
                                        iconTint.copy(alpha = 0.05f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Title
                Text(
                    text = title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Message
                Text(
                    text = message,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (dismissButtonText != null) {
                        Arrangement.spacedBy(12.dp)
                    } else {
                        Arrangement.Center
                    }
                ) {
                    // Dismiss button (if provided)
                    dismissButtonText?.let { text ->
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onDismissRequest()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        ) {
                            Text(
                                text = text,
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    }

                    // Confirm button
                    Button(
                        onClick = {
                            onConfirm()
                            onDismissRequest()
                        },
                        modifier = if (dismissButtonText != null) {
                            Modifier.weight(1f)
                        } else {
                            Modifier.widthIn(min = 120.dp)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = confirmButtonText,
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Success dialog variant
 */
@Composable
fun HocaSuccessDialog(
    title: String = "Başarılı!",
    message: String,
    onConfirm: () -> Unit = {},
    onDismissRequest: () -> Unit = {}
) {
    HocaDialog(
        title = title,
        message = message,
        icon = Icons.Default.CheckCircle,
        iconTint = Color(0xFF4CAF50),
        confirmButtonText = "Harika!",
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest
    )
}

/**
 * Info dialog variant
 */
@Composable
fun HocaInfoDialog(
    title: String = "Bilgilendirme",
    message: String,
    onConfirm: () -> Unit = {},
    onDismissRequest: () -> Unit = {}
) {
    HocaDialog(
        title = title,
        message = message,
        icon = Icons.Default.Info,
        iconTint = Color(0xFF2196F3),
        confirmButtonText = "Anladım",
        onConfirm = onConfirm,
        onDismissRequest = onDismissRequest
    )
}

/**
 * Warning dialog variant
 */
@Composable
fun HocaWarningDialog(
    title: String = "Uyarı",
    message: String,
    confirmButtonText: String = "Tamam",
    dismissButtonText: String? = "İptal",
    onConfirm: () -> Unit = {},
    onDismiss: () -> Unit = {},
    onDismissRequest: () -> Unit = {}
) {
    HocaDialog(
        title = title,
        message = message,
        icon = Icons.Default.Warning,
        iconTint = Color(0xFFFF9800),
        confirmButtonText = confirmButtonText,
        dismissButtonText = dismissButtonText,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        onDismissRequest = onDismissRequest
    )
}

@Preview(showBackground = true)
@Composable
private fun HocaDialogPreview() {
    HocaLingoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            verticalArrangement = Arrangement.Center
        ) {
            HocaInfoDialog(
                title = "Paket Bilgisi",
                message = "Bu paket yakında eklenecek! Şimdilik A1 paketi ile devam edebilirsiniz.",
                onConfirm = {},
                onDismissRequest = {}
            )
        }
    }
}