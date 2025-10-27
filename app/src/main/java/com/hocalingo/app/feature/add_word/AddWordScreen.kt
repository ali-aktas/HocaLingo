package com.hocalingo.app.feature.addword.presentation

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
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
import com.hocalingo.app.HocaRoutes
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import kotlinx.coroutines.flow.collectLatest

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * Add Word Screen - Redesigned & Theme-Aware
 * ✅ Modern, colorful and user-friendly design
 * ✅ Main word card prominently displayed
 * ✅ Expandable example sections to save space
 * ✅ Beautiful gradients (orange, purple, grey, lilac)
 * ✅ Theme-aware for both light and dark modes
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

    // Get theme state for smart styling
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    // Example sections expand/collapse state
    var showEnglishExample by remember { mutableStateOf(false) }
    var showTurkishExample by remember { mutableStateOf(false) }

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
                    snackbarHostState.showSnackbar("✨ Kelime başarıyla eklendi!")
                    // Auto navigate after success
                }
                AddWordEffect.ClearFormFields -> {
                    // Reset expand states on form clear
                    showEnglishExample = false
                    showTurkishExample = false
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
        // topBar YOK!
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Header Section
            HeaderSection(
                userWordsCount = uiState.userWordsCount,
                isDarkTheme = isDarkTheme
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Content with padding
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                // 🎯 Main Word Entry Card (Primary Focus)
                MainWordEntryCard(
                    englishWord = uiState.englishWord,
                    turkishWord = uiState.turkishWord,
                    englishError = uiState.englishWordError,
                    turkishError = uiState.turkishWordError,
                    onEvent = viewModel::onEvent,
                    focusManager = focusManager,
                    isDarkTheme = isDarkTheme
                )

                // 📝 Example Sections (Expandable)
                ExampleSectionsCard(
                    englishExample = uiState.englishExample,
                    turkishExample = uiState.turkishExample,
                    englishError = uiState.englishExampleError,
                    turkishError = uiState.turkishExampleError,
                    showEnglishExample = showEnglishExample,
                    showTurkishExample = showTurkishExample,
                    onToggleEnglishExample = { showEnglishExample = !showEnglishExample },
                    onToggleTurkishExample = { showTurkishExample = !showTurkishExample },
                    onEvent = viewModel::onEvent,
                    focusManager = focusManager,
                    isDarkTheme = isDarkTheme
                )

                // 🎬 Action Buttons
                ActionButtonsCard(
                    canSubmit = uiState.canSubmit,
                    isLoading = uiState.isLoading,
                    onSubmit = { viewModel.onEvent(AddWordEvent.SubmitWord) },
                    onClear = { viewModel.onEvent(AddWordEvent.ClearForm) },
                    isDarkTheme = isDarkTheme
                )

                // Error Display
                uiState.error?.let { error ->
                    ErrorCard(
                        error = error,
                        onDismiss = { viewModel.onEvent(AddWordEvent.DismissError) },
                        isDarkTheme = isDarkTheme
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun HeaderSection(
    userWordsCount: Int,
    isDarkTheme: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 24.dp)
    ) {
        Text(
            text = "Kendi Kelimeni Ekle",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground, // Theme-aware
            textAlign = TextAlign.Center
        )

        Text(
            text = "$userWordsCount özel kelimen var",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant, // Theme-aware
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MainWordEntryCard(
    englishWord: String,
    turkishWord: String,
    englishError: String?,
    turkishError: String?,
    onEvent: (AddWordEvent) -> Unit,
    focusManager: FocusManager,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFFFF8A65), Color(0xFFFF7043)) // Dark orange gradient
                        } else {
                            listOf(Color(0xFFFF6B35), Color(0xFFFF8E53)) // Light orange gradient
                        }
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                // Header with icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Ana Kelimeler",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // English word field
                ModernTextField(
                    value = englishWord,
                    onValueChange = { onEvent(AddWordEvent.EnglishWordChanged(it)) },
                    label = "İngilizce Kelime",
                    placeholder = "beautiful",
                    error = englishError,
                    leadingIcon = Icons.Outlined.Language,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                Spacer(modifier = Modifier.height(12.dp)) // 16dp'den 12dp'ye azaltıldı

                // Turkish word field - gereksiz arrow indicator kaldırıldı
                ModernTextField(
                    value = turkishWord,
                    onValueChange = { onEvent(AddWordEvent.TurkishWordChanged(it)) },
                    label = "Türkçe Karşılığı",
                    placeholder = "güzel",
                    error = turkishError,
                    leadingIcon = Icons.Outlined.Translate,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { focusManager.clearFocus() }
                    )
                )
            }
        }
    }
}

@Composable
private fun ExampleSectionsCard(
    englishExample: String,
    turkishExample: String,
    englishError: String?,
    turkishError: String?,
    showEnglishExample: Boolean,
    showTurkishExample: Boolean,
    onToggleEnglishExample: () -> Unit,
    onToggleTurkishExample: () -> Unit,
    onEvent: (AddWordEvent) -> Unit,
    focusManager: androidx.compose.ui.focus.FocusManager,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF7986CB), Color(0xFF5C6BC0)) // Dark purple gradient
                        } else {
                            listOf(Color(0xFF667eea), Color(0xFF764ba2)) // Light purple gradient
                        }
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FormatQuote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Örnek Cümleler (İsteğe Bağlı)",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // English Example Section
                ExpandableExampleSection(
                    title = "🇺🇸 İngilizce Örnek Ekle",
                    isExpanded = showEnglishExample,
                    onToggle = onToggleEnglishExample,
                    content = {
                        ModernTextField(
                            value = englishExample,
                            onValueChange = { onEvent(AddWordEvent.EnglishExampleChanged(it)) },
                            label = "İngilizce Örnek Cümle",
                            placeholder = "She is very beautiful today.",
                            error = englishError,
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = { focusManager.moveFocus(FocusDirection.Down) }
                            )
                        )
                    }
                )

                if (showEnglishExample && showTurkishExample) {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Turkish Example Section
                ExpandableExampleSection(
                    title = "🇹🇷 Türkçe Örnek Ekle",
                    isExpanded = showTurkishExample,
                    onToggle = onToggleTurkishExample,
                    content = {
                        ModernTextField(
                            value = turkishExample,
                            onValueChange = { onEvent(AddWordEvent.TurkishExampleChanged(it)) },
                            label = "Türkçe Örnek Cümle",
                            placeholder = "O bugün çok güzel görünüyor.",
                            error = turkishError,
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
                )
            }
        }
    }
}

@Composable
private fun ExpandableExampleSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        // Toggle Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onToggle() }
                .background(Color.White.copy(alpha = 0.1f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (isExpanded) "Kapat" else "Aç",
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Expandable Content
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun ActionButtonsCard(
    canSubmit: Boolean,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = if (isDarkTheme) {
                            listOf(Color(0xFF616161), Color(0xFF424242)) // Dark grey gradient
                        } else {
                            listOf(Color(0xFF9E9E9E), Color(0xFF757575)) // Light grey gradient
                        }
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Clear Button - Daha geniş yapıldı
                OutlinedButton(
                    onClick = onClear,
                    modifier = Modifier.weight(1.2f), // 1f'den 1.2f'ye çıkarıldı
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    ),
                    border = null,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp)) // 8dp'den 6dp'ye azaltıldı
                    Text(
                        text = "Temizle",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }

                // Submit Button - Biraz küçültüldü
                Button(
                    onClick = onSubmit,
                    enabled = canSubmit && !isLoading,
                    modifier = Modifier.weight(1.5f), // 2f'den 1.5f'ye azaltıldı
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDarkTheme) {
                            Color(0xFF66BB6A) // Dark theme green
                        } else {
                            Color(0xFF4CAF50) // Light theme green
                        }
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp)) // 8dp'den 6dp'ye azaltıldı
                        Text(
                            text = "Kelime Ekle",
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
private fun ModernTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    error: String? = null,
    leadingIcon: ImageVector? = null,
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
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.5f)
                )
            },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            isError = error != null,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White.copy(alpha = 0.9f),
                focusedBorderColor = Color.White,
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                errorBorderColor = Color(0xFFFFCDD2),
                errorTextColor = Color.White,
                cursorColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Error text
        error?.let { errorText ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = Color(0xFFFFCDD2),
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onDismiss: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                Color(0xFFD32F2F).copy(alpha = 0.9f)
            } else {
                Color(0xFFFFEBEE)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = if (isDarkTheme) Color.White else Color(0xFFD32F2F),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = error,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = if (isDarkTheme) Color.White else Color(0xFFD32F2F),
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Kapat",
                    tint = if (isDarkTheme) Color.White else Color(0xFFD32F2F),
                    modifier = Modifier.size(20.dp)
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