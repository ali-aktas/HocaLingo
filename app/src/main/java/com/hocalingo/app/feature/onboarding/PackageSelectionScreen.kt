package com.hocalingo.app.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.flow.collectLatest

// Poppins Font Family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * PACKAGE SELECTION SCREEN - Yeni Modern Tasarım ✨
 *
 * Özellikler:
 * ✅ 2'li grid düzen (LazyVerticalGrid)
 * ✅ Kare şeklinde kartlar
 * ✅ Pastel renkler (A1→C2 artan tonlarda)
 * ✅ Sade Material Design
 * ✅ Bottom navigation padding korundu
 * ✅ Non-scrollable - ekrana tam oturur
 * ✅ Hero image kaldırıldı
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
                        color = MaterialTheme.colorScheme.primary
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
                        viewModel.onEvent(PackageSelectionEvent.SelectPackage(packageId))
                        viewModel.onEvent(PackageSelectionEvent.DownloadPackage(packageId))
                    }
                )
            }
        }
    }
}

/**
 * Main Content - Grid Layout
 */
@Composable
private fun PackageSelectionContent(
    packages: List<PackageInfo>,
    selectedPackageId: String?,
    paddingValues: PaddingValues,
    onPackageClick: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues),
        contentPadding = PaddingValues(
            start = 20.dp,
            top = 32.dp,
            end = 20.dp,
            bottom = 16.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header section - spans full width
        item(span = { GridItemSpan(2) }) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Seviyeni Seç",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle
                Text(
                    text = "İngilizce seviyene uygun paketi seçerek başla",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // Package cards
        items(packages) { packageInfo ->
            PackageCard(
                packageInfo = packageInfo,
                isSelected = selectedPackageId == packageInfo.id,
                onClick = { onPackageClick(packageInfo.id) }
            )
        }
    }
}

/**
 * Package Card - Sade Material Design
 */
@Composable
private fun PackageCard(
    packageInfo: PackageInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Pastel renkler - A1'den C2'ye artan tonlarda
    val cardColor = when (packageInfo.level) {
        "A1" -> Color(0xFFA8E6CF)  // Açık yeşil (Mint)
        "A2" -> Color(0xFFAEC6CF)  // Açık mavi (Sky Blue)
        "B1" -> Color(0xFFFFD8B1)  // Açık turuncu (Peach)
        "B2" -> Color(0xFFFFB3BA)  // Açık pembe (Pink)
        "C1" -> Color(0xFFD4B3E8)  // Açık mor (Lavender)
        "C2" -> Color(0xFFB3E8E5)  // Açık teal (Aqua)
        else -> Color(0xFFE8E8E8)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),  // Kare şekli
        colors = CardDefaults.cardColors(
            containerColor = cardColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 2.dp
        ),
        border = if (isSelected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Level Badge
            Surface(
                color = Color.White.copy(alpha = 0.9f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = packageInfo.level,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1C1C1E),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Center: Title & Description
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = packageInfo.name,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF1C1C1E),
                    textAlign = TextAlign.Start,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = packageInfo.description,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = Color(0xFF1C1C1E).copy(alpha = 0.8f),
                    textAlign = TextAlign.Start,
                    maxLines = 2,
                    lineHeight = 18.sp
                )
            }

            // Bottom: Word Count
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📚 ${packageInfo.wordCount} kelime",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFF1C1C1E)
                )
            }
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
                containerColor = MaterialTheme.colorScheme.primary
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