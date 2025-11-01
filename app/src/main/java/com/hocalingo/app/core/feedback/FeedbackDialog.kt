package com.hocalingo.app.core.feedback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import kotlinx.coroutines.launch

/**
 * FeedbackDialog
 * ==============
 * Second step for negative users: Collect detailed feedback
 *
 * Components:
 * - Category selection (Bug, Feature, Content, Other)
 * - Message text field
 * - Optional email field
 * - Submit button
 *
 * Package: app/src/main/java/com/hocalingo/app/core/feedback/
 */

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackDialog(
    satisfactionLevel: SatisfactionLevel,
    onDismiss: () -> Unit,
    onSubmit: (category: FeedbackCategory, message: String, email: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(FeedbackCategory.OTHER) }
    var message by remember { mutableStateOf(TextFieldValue("")) }
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var isSubmitting by remember { mutableStateOf(false) }

    // ✅ FIX: Reset isSubmitting when dialog reopens
    LaunchedEffect(Unit) {
        isSubmitting = false
    }

    Dialog(
        onDismissRequest = {
            if (!isSubmitting) {  // ✅ Submitting sırasında kapanmaz
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = !isSubmitting,      // ✅ Submitting sırasında back disabled
            dismissOnClickOutside = !isSubmitting    // ✅ Submitting sırasında dış tıklama disabled
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💬",
                        fontSize = 32.sp
                    )

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting  // ✅ Submitting sırasında disabled
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Kapat",
                            tint = if (isSubmitting) {
                                Color(0xFF95A5A6).copy(alpha = 0.5f)
                            } else {
                                Color(0xFF95A5A6)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = "Üzgünüz! Yardımcı Olalım",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color(0xFF2C3E50)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "Ne yaşadığını bizimle paylaş, daha iyi bir deneyim için çalışalım!",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = Color(0xFF7F8C8D),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Category selection
                Text(
                    text = "Kategori Seç",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF2C3E50)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Category grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FeedbackCategory.values().forEach { category ->
                        CategoryChip(
                            category = category,
                            isSelected = selectedCategory == category,
                            onClick = {
                                if (!isSubmitting) {  // ✅ Submitting sırasında değiştirilemez
                                    selectedCategory = category
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Message field
                Text(
                    text = "Mesajın *",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF2C3E50)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = message,
                    onValueChange = {
                        if (!isSubmitting) {  // ✅ Submitting sırasında değiştirilemez
                            message = it
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = {
                        Text(
                            text = "Deneyimini bizimle paylaş...",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFFBDC3C7)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B35),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        disabledBorderColor = Color(0xFFE0E0E0).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 5,
                    enabled = !isSubmitting  // ✅ Submitting sırasında disabled
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email field (optional)
                Text(
                    text = "E-posta (Opsiyonel)",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF2C3E50)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        if (!isSubmitting) {  // ✅ Submitting sırasında değiştirilemez
                            email = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "ornek@email.com",
                            fontFamily = PoppinsFontFamily,
                            fontSize = 14.sp,
                            color = Color(0xFFBDC3C7)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF6B35),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        disabledBorderColor = Color(0xFFE0E0E0).copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    enabled = !isSubmitting  // ✅ Submitting sırasında disabled
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Submit button
                Button(
                    onClick = {
                        if (message.text.isNotBlank() && !isSubmitting) {
                            isSubmitting = true
                            onSubmit(
                                selectedCategory,
                                message.text,
                                email.text.takeIf { it.isNotBlank() }
                            )

                            // ✅ FIX: Reset after 3 seconds as fallback
                            // (Normal case: dialog will close on success)
                            kotlinx.coroutines.GlobalScope.launch {
                                kotlinx.coroutines.delay(3000)
                                isSubmitting = false
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = message.text.isNotBlank() && !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B35),
                        disabledContainerColor = Color(0xFFBDC3C7)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Gönder",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: FeedbackCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(
                color = if (isSelected) Color(0xFFFF6B35) else Color(0xFFF8F9FA),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = category.icon,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = category.displayName,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = if (isSelected) Color.White else Color(0xFF2C3E50),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ========== PREVIEW ==========

@Preview(showBackground = true)
@Composable
private fun FeedbackDialogPreview() {
    HocaLingoTheme {
        FeedbackDialog(
            satisfactionLevel = SatisfactionLevel.NEUTRAL,
            onDismiss = {},
            onSubmit = { _, _, _ -> }
        )
    }
}