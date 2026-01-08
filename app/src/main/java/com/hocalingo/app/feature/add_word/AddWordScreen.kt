package com.hocalingo.app.feature.add_word

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.HocaRoutes
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.HocaColors
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
import com.hocalingo.app.core.ui.theme.HocaSpacing
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
 * Add Word Screen - Yenilenmiş Tasarım
 *
 * Yenilikler:
 * ✅ Tek birleşik kart (iki kart birleştirildi)
 * ✅ HocaColors ve HocaSpacing kullanımı
 * ✅ 3D Action Button
 * ✅ Kompakt ve responsive tasarım
 * ✅ HomeScreen ile tutarlılık
 * ✅ Parlak mor kelime sayısı
 *
 * Package: feature/add_word/
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
    val scrollState = rememberScrollState()

    // Get theme state
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
                }
                AddWordEffect.ClearFormFields -> {
                    showEnglishExample = false
                    showTurkishExample = false
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = {
            HocaSnackbarHost(
                hostState = snackbarHostState,
                currentRoute = HocaRoutes.ADD_WORD
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(horizontal = HocaSpacing.md)
                .padding(bottom = HocaSpacing.lg),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(HocaSpacing.xl))

            // Header
            HeaderSection(
                userWordsCount = uiState.userWordsCount
            )

            Spacer(modifier = Modifier.height(HocaSpacing.md))

            // Info Banner
            InfoBanner(isDarkTheme = isDarkTheme)

            Spacer(modifier = Modifier.height(HocaSpacing.lg))

            // Unified Word Entry Card
            UnifiedWordEntryCard(
                englishWord = uiState.englishWord,
                turkishWord = uiState.turkishWord,
                englishExample = uiState.englishExample,
                turkishExample = uiState.turkishExample,
                englishError = uiState.englishWordError,
                turkishError = uiState.turkishWordError,
                englishExampleError = uiState.englishExampleError,
                turkishExampleError = uiState.turkishExampleError,
                showEnglishExample = showEnglishExample,
                showTurkishExample = showTurkishExample,
                onToggleEnglishExample = { showEnglishExample = !showEnglishExample },
                onToggleTurkishExample = { showTurkishExample = !showTurkishExample },
                onEvent = viewModel::onEvent,
                focusManager = focusManager
            )

            Spacer(modifier = Modifier.height(HocaSpacing.lg))

            // Action Buttons (3D)
            ActionButtons3D(
                canSubmit = uiState.canSubmit,
                isLoading = uiState.isLoading,
                onSubmit = { viewModel.onEvent(AddWordEvent.SubmitWord) },
                onClear = { viewModel.onEvent(AddWordEvent.ClearForm) }
            )

            // Error Display
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(HocaSpacing.md))
                ErrorCard(
                    error = error,
                    onDismiss = { viewModel.onEvent(AddWordEvent.DismissError) },
                    isDarkTheme = isDarkTheme
                )
            }

            Spacer(modifier = Modifier.height(HocaSpacing.md))
        }
    }
}

/**
 * Header Section
 */
@Composable
private fun HeaderSection(
    userWordsCount: Int
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Kendi Kelimeni Ekle",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(HocaSpacing.xs))

        // Parlak mor kelime sayısı
        Text(
            text = "$userWordsCount özel kelimen var",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = HocaColors.Purple, // ✅ Parlak mor
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Info Banner - Kompakt
 */
@Composable
private fun InfoBanner(
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = HocaSpacing.xs),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isDarkTheme) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(HocaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HocaSpacing.sm)
        ) {
            // Lingo Hoca
            Image(
                painter = painterResource(R.drawable.onboarding_teacher_1),
                contentDescription = "Lingo Hoca",
                modifier = Modifier.size(80.dp),
                contentScale = ContentScale.Fit
            )

            // Açıklama
            Text(
                text = "Günlük hayatta görüp öğrenmek istediğin kelimeleri buradan çalışma destene ekleyebilirsin.",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Unified Word Entry Card - Tek birleşik kart
 */
@Composable
private fun UnifiedWordEntryCard(
    englishWord: String,
    turkishWord: String,
    englishExample: String,
    turkishExample: String,
    englishError: String?,
    turkishError: String?,
    englishExampleError: String?,
    turkishExampleError: String?,
    showEnglishExample: Boolean,
    showTurkishExample: Boolean,
    onToggleEnglishExample: () -> Unit,
    onToggleTurkishExample: () -> Unit,
    onEvent: (AddWordEvent) -> Unit,
    focusManager: FocusManager
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(HocaSpacing.lg)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = HocaColors.Orange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(HocaSpacing.sm))
                Text(
                    text = "Kelime ve Anlamı",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(HocaSpacing.md))

            // English Word Field
            CompactTextField(
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

            // English Example Section (Expandable)
            ExpandableSection(
                title = "🇺🇸 İngilizce Örnek Ekle (İsteğe Bağlı)",
                isExpanded = showEnglishExample,
                onToggle = onToggleEnglishExample,
                content = {
                    CompactTextField(
                        value = englishExample,
                        onValueChange = { onEvent(AddWordEvent.EnglishExampleChanged(it)) },
                        label = "İngilizce Örnek Cümle",
                        placeholder = "She is very beautiful today.",
                        error = englishExampleError,
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

            Spacer(modifier = Modifier.height(HocaSpacing.md))

            // Turkish Word Field
            CompactTextField(
                value = turkishWord,
                onValueChange = { onEvent(AddWordEvent.TurkishWordChanged(it)) },
                label = "Türkçe Anlamı",
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

            // Turkish Example Section (Expandable)
            ExpandableSection(
                title = "🇹🇷 Türkçe Örnek Ekle (İsteğe Bağlı)",
                isExpanded = showTurkishExample,
                onToggle = onToggleTurkishExample,
                content = {
                    CompactTextField(
                        value = turkishExample,
                        onValueChange = { onEvent(AddWordEvent.TurkishExampleChanged(it)) },
                        label = "Türkçe Örnek Cümle",
                        placeholder = "O bugün çok güzel görünüyor.",
                        error = turkishExampleError,
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

/**
 * Expandable Section
 */
@Composable
private fun ExpandableSection(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Column {
        Spacer(modifier = Modifier.height(HocaSpacing.xs))

        // Toggle Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onToggle() }
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(HocaSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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
                Spacer(modifier = Modifier.height(HocaSpacing.sm))
                content()
            }
        }
    }
}

/**
 * Compact TextField
 */
@Composable
private fun CompactTextField(
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
                    fontSize = 13.sp
                )
            },
            placeholder = {
                Text(
                    text = placeholder,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp
                )
            },
            leadingIcon = leadingIcon?.let { icon ->
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            isError = error != null,
            maxLines = maxLines,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HocaColors.Orange,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = HocaColors.HardRed,
                cursorColor = HocaColors.Orange
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Error text
        error?.let { errorText ->
            Spacer(modifier = Modifier.height(HocaSpacing.xxs))
            Text(
                text = errorText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = HocaColors.HardRed,
                modifier = Modifier.padding(start = HocaSpacing.md)
            )
        }
    }
}

/**
 * Action Buttons 3D - Yeni tasarım
 */
@Composable
private fun ActionButtons3D(
    canSubmit: Boolean,
    isLoading: Boolean,
    onSubmit: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(HocaSpacing.md)
    ) {
        // Clear Button (küçük, 3D)
        Small3DButton(
            text = "Temizle",
            icon = Icons.Outlined.Refresh,
            onClick = onClear,
            modifier = Modifier.weight(1f)
        )

        // Submit Button (büyük, 3D, turuncu)
        Large3DSubmitButton(
            text = "Destene Ekle",
            enabled = canSubmit && !isLoading,
            isLoading = isLoading,
            onClick = onSubmit,
            modifier = Modifier.weight(2f)
        )
    }
}

/**
 * Small 3D Button (Clear)
 */
@Composable
private fun Small3DButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val pressDepth by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "press_depth"
    )

    val brightness by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "brightness"
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) onClick()
                    }
                )
            }
    ) {
        // Shadow
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 6.dp)
        ) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.2f),
                        Color.Transparent
                    )
                ),
                cornerRadius = CornerRadius(16.dp.toPx())
            )
        }

        // Button surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = pressDepth)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFBDBDBD).copy(alpha = brightness),
                            Color(0xFF9E9E9E).copy(alpha = brightness)
                        )
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(HocaSpacing.sm),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(HocaSpacing.xs))
                Text(
                    text = text,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Large 3D Submit Button - Daha büyük ve net
 */
@Composable
private fun Large3DSubmitButton(
    text: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val pressDepth by animateDpAsState(
        targetValue = if (isPressed && enabled) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "press_depth"
    )

    val brightness by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "brightness"
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            val released = tryAwaitRelease()
                            isPressed = false
                            if (released) onClick()
                        }
                    )
                }
            }
    ) {
        // Shadow
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 6.dp)
        ) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = if (enabled) 0.25f else 0.1f),
                        Color.Transparent
                    )
                ),
                cornerRadius = CornerRadius(16.dp.toPx())
            )
        }

        // Button surface
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = pressDepth)
                .background(
                    brush = Brush.verticalGradient(
                        colors = if (enabled) {
                            listOf(
                                HocaColors.Orange.copy(alpha = brightness),
                                HocaColors.OrangeDark.copy(alpha = brightness)
                            )
                        } else {
                            listOf(
                                Color(0xFFBDBDBD),
                                Color(0xFF9E9E9E)
                            )
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(HocaSpacing.md),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(HocaSpacing.xs))
                    Text(
                        text = text,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * Error Card
 */
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
                .padding(HocaSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = if (isDarkTheme) Color.White else Color(0xFFD32F2F),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(HocaSpacing.sm))
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