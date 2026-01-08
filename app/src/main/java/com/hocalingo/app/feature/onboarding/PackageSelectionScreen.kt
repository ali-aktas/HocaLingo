package com.hocalingo.app.feature.onboarding

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hocalingo.app.HocaRoutes
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.HocaColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * PACKAGE SELECTION SCREEN - Yenilenmiş Tasarım
 *
 * Yenilikler:
 * ✅ 3D button'lar (Hoca3DButton benzeri)
 * ✅ "Paketi aç" butonu kaldırıldı - Direkt tıklama ile navigasyon
 * ✅ İndirme badge'leri kaldırıldı
 * ✅ "B1 - Orta" formatında başlık
 * ✅ Kelime sayısı sağ üstte
 * ✅ Minimal bottom padding (BottomNav ile arasında boşluk yok)
 * ✅ Her zaman bottom navigation görünür
 *
 * Package: feature/onboarding/
 */
@Composable
fun PackageSelectionScreen(
    onNavigateToWordSelection: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: PackageSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is PackageSelectionEffect.NavigateToWordSelection -> {
                    onNavigateToWordSelection(effect.packageId)
                }
                is PackageSelectionEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is PackageSelectionEffect.ShowLoadingAnimation -> {
                    // İndirme başarılı, navigation yap
                    onNavigateToWordSelection(effect.packageId)
                }
                is PackageSelectionEffect.ShowDownloadDialog -> {
                    // Reserved for future use
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            HocaSnackbarHost(
                hostState = snackbarHostState,
                currentRoute = HocaRoutes.PACKAGE_SELECTION
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.packages.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = HocaColors.Orange
                    )
                }
            }
            uiState.error != null && uiState.packages.isEmpty() -> {
                ErrorState(
                    message = uiState.error!!,
                    onRetry = { viewModel.onEvent(PackageSelectionEvent.RetryLoading) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                PackageSelectionContent(
                    packages = uiState.packages,
                    selectedPackageId = uiState.selectedPackageId,
                    paddingValues = paddingValues,
                    onPackageClick = { packageId ->
                        // Paketi seç ve işlemi başlat
                        viewModel.onEvent(PackageSelectionEvent.SelectPackage(packageId))
                        viewModel.onEvent(PackageSelectionEvent.DownloadPackage(packageId))
                    }
                )
            }
        }
    }
}

@Composable
private fun PackageSelectionContent(
    packages: List<PackageInfo>,
    selectedPackageId: String?,
    paddingValues: PaddingValues,
    onPackageClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Hero Image
        Image(
            painter = painterResource(id = R.drawable.onboarding_teacher_1),
            contentDescription = "Lingo Hoca",
            modifier = Modifier.size(140.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Seviyeni seç, öğrenmeye başla!",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Package Buttons
        packages.forEach { packageInfo ->
            Package3DButton(
                packageInfo = packageInfo,
                isSelected = selectedPackageId == packageInfo.id,
                onClick = { onPackageClick(packageInfo.id) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Minimal bottom spacing
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Package 3D Button
 *
 * Tasarım:
 * - 3D depth effect (Hoca3DButton benzeri)
 * - Sol: "B1 - Orta" başlık + açıklama
 * - Sağ üst: Kelime sayısı
 * - Full width, 100dp height
 * - Press animation
 * - Selection indication (border)
 */
@Composable
private fun Package3DButton(
    packageInfo: PackageInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    // Animations
    val pressDepth by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "press_depth"
    )

    val topColorBrightness by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "color_brightness"
    )

    // Gradient colors based on package level
    val gradientColors = when (packageInfo.level) {
        "A1" -> listOf(Color(0xFF4CAF50), Color(0xFF388E3C))  // Yeşil
        "A2" -> listOf(Color(0xFF42A5F5), Color(0xFF1976D2))  // Mavi
        "B1" -> listOf(Color(0xFFFF9800), Color(0xFFF57C00))  // Turuncu
        "B2" -> listOf(Color(0xFFE53935), Color(0xFFC62828))  // Kırmızı
        "C1" -> listOf(Color(0xFF9C27B0), Color(0xFF7B1FA2))  // Mor
        "C2" -> listOf(Color(0xFF26A69A), Color(0xFF00897B))  // Teal
        else -> listOf(HocaColors.Orange, HocaColors.OrangeDark)
    }
    val baseColor = gradientColors.first()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        val released = tryAwaitRelease()
                        isPressed = false
                        if (released) {
                            onClick()
                        }
                    }
                )
            }
    ) {
        // Bottom shadow
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .offset(y = 8.dp)
        ) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    radius = size.width / 2
                ),
                size = this.size,
                cornerRadius = CornerRadius(20.dp.toPx())
            )
        }

        // Main button surface
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .offset(y = pressDepth)
        ) {
            val lightColor = Color(
                red = (baseColor.red * topColorBrightness).coerceIn(0f, 1f),
                green = (baseColor.green * topColorBrightness).coerceIn(0f, 1f),
                blue = (baseColor.blue * topColorBrightness).coerceIn(0f, 1f)
            )

            val shadowColor = Color(
                red = (baseColor.red * 0.75f).coerceIn(0f, 1f),
                green = (baseColor.green * 0.75f).coerceIn(0f, 1f),
                blue = (baseColor.blue * 0.75f).coerceIn(0f, 1f)
            )

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(lightColor, shadowColor)
                ),
                size = this.size,
                cornerRadius = CornerRadius(20.dp.toPx())
            )
        }

        // Selection border
        if (isSelected) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .offset(y = pressDepth)
            ) {
                drawRoundRect(
                    color = Color.White,
                    size = this.size,
                    cornerRadius = CornerRadius(20.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                )
            }
        }

        // Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .offset(y = pressDepth)
                .padding(20.dp)
        ) {
            // Left: Title + Description
            Column(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalArrangement = Arrangement.Center
            ) {
                // Title: "B1 - Orta"
                Text(
                    text = "${packageInfo.level} - ${packageInfo.name}",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Description
                Text(
                    text = packageInfo.description,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1
                )
            }

            // Right Top: Word Count
            Text(
                text = "${packageInfo.wordCount} kelime",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.End,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

/**
 * Error State
 */
@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "❌",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = message,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = HocaColors.Orange
            )
        ) {
            Text(
                text = "Tekrar Dene",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold
            )
        }
    }
}