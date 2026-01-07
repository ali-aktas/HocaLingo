package com.hocalingo.app.feature.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.HocaRoutes
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.CircularStatCard
import com.hocalingo.app.core.ui.components.HocaPlayButton
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.HocaColors
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import com.hocalingo.app.feature.subscription.PaywallBottomSheet
import kotlinx.coroutines.flow.collectLatest
import java.util.Calendar

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

// Motivasyon yazıları
private val motivationTexts = listOf(
    "Kelime öğrenmek için harika bir vakit!",
    "7 dakikanı İngilizce için ayırabilirsin",
    "Her gün biraz pratik, büyük fark yaratır",
    "Bugün hangi kelimeleri öğreneceğiz?",
    "İngilizce yolculuğun devam ediyor!",
    "Küçük adımlar, büyük hedefler",
    "Dil öğrenmek bir maraton, hadi başlayalım",
    "Bugün kendine yatırım yapma zamanı",
    "Her kelime seni hedefe yaklaştırıyor",
    "Pratik mükemmelleştirir, başlayalım",
    "Bugün de harika gidiyorsun!",
    "İngilizce macerası seni bekliyor",
    "Yeni kelimeler, yeni fırsatlar",
    "Hedefine bir adım daha yakınsın",
    "Bugün hangi hikayeyi yazacaksın?",
    "Motivasyon yüksek, hadi başla",
    "Başarı sabırla gelir, devam et",
    "Her çalışma seansi bir zafer",
    "Bugün de öğrenmeye hazır mısın?",
    "İngilizce dünyasına hoş geldin"
)

/**
 * PREMIUM HOME SCREEN - Yenilenmiş Tasarım
 *
 * Yapı:
 * 1. HocaLingo başlık
 * 2. Hero Card (Play + Maskot + Motivasyon)
 * 3. Stats (düzeltilmiş)
 * 4. Action Buttons (alt alta, icon+text)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToStudy: () -> Unit,
    onNavigateToPackageSelection: () -> Unit = {},
    onNavigateToAIAssistant: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Theme state
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    // Günlük motivasyon (her gün değişir)
    val dailyMotivation = remember {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        motivationTexts[dayOfYear % motivationTexts.size]
    }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                HomeEffect.NavigateToStudy -> onNavigateToStudy()
                HomeEffect.NavigateToPackageSelection -> onNavigateToPackageSelection()
                HomeEffect.NavigateToAIAssistant -> onNavigateToAIAssistant()
                is HomeEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }
                is HomeEffect.ShowError -> {
                    snackbarHostState.showSnackbar(effect.error)
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            HocaSnackbarHost(
                hostState = snackbarHostState,
                currentRoute = HocaRoutes.HOME
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { _ ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 64.dp,
                end = 20.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 1. HOCALINGO BAŞLIK
            item {
                Text(
                    text = "HocaLingo",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // 2. HERO CARD (Play + Maskot + Motivasyon)
            item {
                HeroCard(
                    userName = uiState.userName,
                    motivationText = dailyMotivation,
                    onPlayClick = { viewModel.onEvent(HomeEvent.StartStudy) },
                    isDarkTheme = isDarkTheme
                )
            }

            // 3. STATS BAŞLIK
            item {
                Text(
                    text = "Bu ayın istatistikleri",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }

            // 4. STATS (Düzeltilmiş)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Streak
                    CircularStatCard(
                        value = uiState.streakDays.toString(),
                        label = "Günlük seri!",
                        progress = if (uiState.streakDays > 0) 1f else 0f,
                        color = HocaColors.Orange,
                        isDarkTheme = isDarkTheme,
                        size = 90.dp
                    )

                    // Study Time (düzeltildi - formatted kullanıyor)
                    val studyTimeDisplay = if (uiState.monthlyStats.studyTimeMinutes >= 60) {
                        "${uiState.monthlyStats.studyTimeMinutes / 60}"
                    } else {
                        "${uiState.monthlyStats.studyTimeMinutes}"
                    }
                    val studyTimeLabel = if (uiState.monthlyStats.studyTimeMinutes >= 60) {
                        "Saat\nÇalışma"
                    } else {
                        "Dakika\nÇalışma"
                    }
                    CircularStatCard(
                        value = studyTimeDisplay,
                        label = studyTimeLabel,
                        progress = if (uiState.monthlyStats.studyTimeMinutes > 0) 1f else 0f,
                        color = HocaColors.EasyGreen,
                        isDarkTheme = isDarkTheme,
                        size = 90.dp
                    )

                    // Discipline
                    CircularStatCard(
                        value = "${uiState.monthlyStats.disciplineScore}%",
                        label = "Disiplin\nPuanı",
                        progress = uiState.monthlyStats.disciplineScore / 100f,
                        color = HocaColors.Purple,
                        isDarkTheme = isDarkTheme,
                        size = 90.dp
                    )
                }
            }

            // 5. ACTION BUTTONS (Alt alta, dikdörtgen 3D butonlar, icon+text)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Add Words Button
                    WideActionButton(
                        onClick = { viewModel.onEvent(HomeEvent.NavigateToPackageSelection) },
                        icon = painterResource(id = R.drawable.card_icon),
                        title = "Yeni Kelimeler Ekle",
                        subtitle = "Destendeki kelimeler azaldı  mı?",
                        baseColor = HocaColors.SuccessTop
                    )

                    // AI Assistant Button
                    WideActionButton(
                        onClick = { viewModel.onEvent(HomeEvent.NavigateToAIAssistant) },
                        icon = painterResource(id = R.drawable.ai_icon),
                        title = "Yapay Zeka Asistanı",
                        subtitle = "Çalışma Destene özel hikaye yazarı",
                        baseColor = HocaColors.PurpleBottom
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // Premium Push Bottom Sheet
        if (uiState.showPremiumPush) {
            PaywallBottomSheet(
                onDismiss = {
                    viewModel.onEvent(HomeEvent.DismissPremiumPush)
                },
                onPurchaseSuccess = {
                    viewModel.onEvent(HomeEvent.PremiumPurchaseSuccess)
                }
            )
        }
    }
}

/**
 * HERO CARD - Play Button + Motivasyon + BÜYÜK Maskot
 * Sol: Play button
 * Orta: Motivasyon yazısı
 * Sağ: Maskot (büyük, belirgin)
 */
@Composable
private fun HeroCard(
    userName: String,
    motivationText: String,
    onPlayClick: () -> Unit,
    isDarkTheme: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color.Black.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme)
                Color(0xFF211A2E)
            else
                Color.White
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol: Play Button
            HocaPlayButton(
                onClick = onPlayClick,
                size = 120.dp,
                baseColor = HocaColors.MediumYellow
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Orta: Motivasyon yazısı
            Text(
                text = motivationText,
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier
                    .offset(x = 10.dp)
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            )

            // Sağ: BÜYÜK Maskot (sabitlenmiş)
            Image(
                painter = painterResource(id = R.drawable.lingo_hoca_image),
                contentDescription = "Lingo Hoca",
                modifier = Modifier
                    .size(130.dp)
                    .offset(y = 15.dp, x = 15.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

/**
 * WIDE ACTION BUTTON - Dikdörtgen 3D Buton (Card değil!)
 * Sol: Icon (küçük 3D)
 * Sağ: Title + Subtitle
 */
@Composable
private fun WideActionButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.painter.Painter,
    title: String,
    subtitle: String,
    baseColor: Color
) {
    var isPressed by remember { mutableStateOf(false) }

    val pressDepth by animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "button_press"
    )

    val topColorBrightness by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "color_brightness"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
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
        // Bottom shadow layer
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .offset(y = 8.dp)
        ) {
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    radius = this.size.width / 2
                ),
                size = this.size,
                cornerRadius = CornerRadius(20.dp.toPx())
            )
        }

        // Main button surface
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
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

        // Content (Icon + Text)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
                .offset(y = pressDepth)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            // Icon
            Image(
                painter = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                contentScale = ContentScale.Fit,
                alpha = 0.95f
            )

            Spacer(modifier = Modifier.width(20.dp))

            // Text
            Column {
                Text(
                    text = title,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
    }
}