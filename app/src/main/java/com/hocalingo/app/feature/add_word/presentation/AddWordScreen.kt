package com.hocalingo.app.feature.addword.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import kotlinx.coroutines.flow.collectLatest

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * Add Word Screen - Real Implementation
 * Beautiful form with PackageSelection theme
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToStudy: () -> Unit = {},
    viewModel: AddWordViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                AddWordEffect.NavigateBack -> onNavigateBack()
                AddWordEffect.NavigateToStudy -> onNavigateToStudy()
                is AddWordEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is AddWordEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.error)
                }
                AddWordEffect.ShowSuccessAndNavigate -> {
                    onNavigateToStudy()
                }
                AddWordEffect.ClearFormFields -> {
                    focusManager.clearFocus()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFA) // Light background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header
            AddWordHeader(
                userWordsCount = uiState.userWordsCount,
                onNavigateBack = onNavigateBack
            )

            // Content
            if (uiState.showSuccessAnimation) {
                SuccessAnimation(
                    onDismiss = { viewModel.onEvent(AddWordEvent.DismissSuccess) }
                )
            } else {
                AddWordForm(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    focusManager = focusManager
                )
            }
        }
    }
}

@Composable
private fun AddWordHeader(
    userWordsCount: Int,
    onNavigateBack: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .background(
                        color = Color(0xFFF5F5F5),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Geri",
                    tint = Color(0xFF2C3E50)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Kelime Ekle",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF2C3E50)
                )
                Text(
                    text = "$userWordsCount özel kelimen var",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFF6C7B8A)
                )
            }

            // Placeholder for symmetry
            Spacer(modifier = Modifier.size(48.dp))
        }
    }
}

@Composable
private fun AddWordForm(
    uiState: AddWordUiState,
    onEvent: (AddWordEvent) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Form instruction
        FormInstructionCard()

        // Required fields card
        RequiredFieldsCard(
            englishWord = uiState.englishWord,
            turkishWord = uiState.turkishWord,
            englishWordError = uiState.englishWordError,
            turkishWordError = uiState.turkishWordError,
            onEvent = onEvent,
            focusManager = focusManager
        )

        // Optional fields card
        OptionalFieldsCard(
            englishExample = uiState.englishExample,
            turkishExample = uiState.turkishExample,
            englishExampleError = uiState.englishExampleError,
            turkishExampleError = uiState.turkishExampleError,
            onEvent = onEvent,
            focusManager = focusManager
        )

        // Action buttons
        ActionButtonsRow(
            canSubmit = uiState.canSubmit,
            isLoading = uiState.isLoading,
            onSubmit = { onEvent(AddWordEvent.SubmitWord) },
            onClear = { onEvent(AddWordEvent.ClearForm) }
        )

        // Error display
        uiState.error?.let { error ->
            ErrorCard(
                error = error,
                onDismiss = { onEvent(AddWordEvent.DismissError) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun FormInstructionCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF0F9FF)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Lightbulb,
                contentDescription = null,
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Kendi kelimeni ekle ve hemen çalışmaya başla! Örnek cümleler isteğe bağlıdır.",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF0284C7),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun RequiredFieldsCard(
    englishWord: String,
    turkishWord: String,
    englishWordError: String?,
    turkishWordError: String?,
    onEvent: (AddWordEvent) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Zorunlu Alanlar",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF2C3E50)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // English word field
            CustomTextField(
                value = englishWord,
                onValueChange = { onEvent(AddWordEvent.EnglishWordChanged(it)) },
                label = "İngilizce Kelime",
                placeholder = "örn: beautiful",
                error = englishWordError,
                leadingIcon = Icons.Outlined.Language,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Turkish word field
            CustomTextField(
                value = turkishWord,
                onValueChange = { onEvent(AddWordEvent.TurkishWordChanged(it)) },
                label = "Türkçe Kelime",
                placeholder = "örn: güzel",
                error = turkishWordError,
                leadingIcon = Icons.Outlined.Translate,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )
        }
    }
}

@Composable
private fun OptionalFieldsCard(
    englishExample: String,
    turkishExample: String,
    englishExampleError: String?,
    turkishExampleError: String?,
    onEvent: (AddWordEvent) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.TextSnippet,
                    contentDescription = null,
                    tint = Color(0xFF4ECDC4),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Örnek Cümleler (İsteğe Bağlı)",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF2C3E50)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // English example field
            CustomTextField(
                value = englishExample,
                onValueChange = { onEvent(AddWordEvent.EnglishExampleChanged(it)) },
                label = "İngilizce Örnek Cümle",
                placeholder = "örn: She is very beautiful",
                error = englishExampleError,
                leadingIcon = Icons.Outlined.FormatQuote,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Turkish example field
            CustomTextField(
                value = turkishExample,
                onValueChange = { onEvent(AddWordEvent.TurkishExampleChanged(it)) },
                label = "Türkçe Örnek Cümle",
                placeholder = "örn: O çok güzel",
                error = turkishExampleError,
                leadingIcon = Icons.Outlined.FormatQuote,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { focusManager.clearFocus() }
                )
            )
        }
    }
}

@Composable
private fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String?,
    leadingIcon: ImageVector,
    maxLines: Int = 1,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = {
                Text(
                    text = label,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = PoppinsFontFamily,
                    color = Color(0xFF9E9E9E)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (error != null) Color(0xFFFF3B30) else Color(0xFF4ECDC4)
                )
            },
            isError = error != null,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF4ECDC4),
                unfocusedBorderColor = Color(0xFFE0E0E0),
                errorBorderColor = Color(0xFFFF3B30),
                focusedLabelColor = Color(0xFF4ECDC4),
                unfocusedLabelColor = Color(0xFF6C7B8A)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Error message
        error?.let { errorText ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = Color(0xFFFF3B30),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    canSubmit: Boolean,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Clear button
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF9E9E9E), Color(0xFF6C7B8A))
                )
            ),
            enabled = !isLoading
        ) {
            Icon(
                imageVector = Icons.Outlined.Clear,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Temizle",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Submit button
        Button(
            onClick = onSubmit,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (canSubmit) Color(0xFF4ECDC4) else Color(0xFFE0E0E0),
                disabledContainerColor = Color(0xFFE0E0E0)
            ),
            enabled = canSubmit && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isLoading) "Ekleniyor..." else "Kelime Ekle",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = error,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFFD32F2F)
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Kapat",
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SuccessAnimation(
    onDismiss: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "successScale"
    )

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🎉",
                    fontSize = 64.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Kelime Eklendi!",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color(0xFF2C3E50),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Artık çalışma listende",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = Color(0xFF6C7B8A),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddWordScreenPreview() {
    HocaLingoTheme {
        AddWordScreen(
            onNavigateBack = {},
            onNavigateToStudy = {}
        )
    }
}