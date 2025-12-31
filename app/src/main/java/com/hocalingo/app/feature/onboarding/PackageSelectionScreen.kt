package com.hocalingo.app.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.hocalingo.app.core.ui.components.HocaErrorState
import com.hocalingo.app.core.ui.components.HocaLoadingIndicator
import com.hocalingo.app.core.ui.components.HocaSnackbarHost
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import com.hocalingo.app.data.PackageDownloadStatus
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

/**
 * UPDATED Package Selection with Colorful Gradient Cards
 * ✅ Each level has unique gradient colors
 * ✅ Beautiful visual hierarchy
 * ✅ Download icon instead of text
 * ✅ Clean level badge without background
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageSelectionScreen(
    onNavigateToWordSelection: (String) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: PackageSelectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Get theme state
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

    // ✅ Effects - sadece yeni ViewModel'deki effect'leri dinle
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is PackageSelectionEffect.NavigateToWordSelection -> {
                    onNavigateToWordSelection(effect.packageId)
                }
                is PackageSelectionEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
                }

                is PackageSelectionEffect.ShowDownloadDialog -> TODO()
                is PackageSelectionEffect.ShowLoadingAnimation -> TODO()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp
                )
        ) {
            when {
                uiState.isLoading -> {
                    HocaLoadingIndicator(
                        text = "Paketler yükleniyor...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.error != null -> {
                    HocaErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.onEvent(PackageSelectionEvent.RetryLoading) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    PackageSelectionContent(
                        packages = uiState.packages,
                        selectedPackageId = uiState.selectedPackageId,
                        isLoading = uiState.isLoading,
                        isDarkTheme = isDarkTheme,
                        onPackageSelected = { packageId ->
                            viewModel.onEvent(PackageSelectionEvent.SelectPackage(packageId))
                        },
                        onContinue = { packageId ->
                            // Seçili paketin bilgilerini al
                            val selectedPackage = uiState.packages.find { it.id == packageId }

                            if (selectedPackage != null) {
                                if (selectedPackage.isDownloaded) {
                                    // ✅ Paket zaten yüklü, direkt WordSelection'a git
                                    viewModel.onEvent(PackageSelectionEvent.DownloadPackage(packageId))
                                } else {
                                    // ❌ Paket henüz yüklenmedi
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Bu paket henüz yüklenmedi. Lütfen önce paketi indirin."
                                        )
                                    }
                                }
                            } else {
                                // Hata durumu
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Lütfen bir paket seçin.")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PackageSelectionContent(
    packages: List<PackageInfo>,
    selectedPackageId: String?,
    isLoading: Boolean,
    isDarkTheme: Boolean,
    onPackageSelected: (String) -> Unit,
    onContinue: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Image(
            painter = painterResource(id = R.drawable.onboarding_teacher_1),
            contentDescription = null,
            modifier = Modifier
                .size(140.dp)
        )


        Spacer(modifier = Modifier.height(2.dp))

        // Header
        Text(
            text = "Seviyeni seç, öğrenmeye başla!",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Package cards list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(packages) { packageInfo ->
                ColorfulPackageCard(
                    packageInfo = packageInfo,
                    isSelected = selectedPackageId == packageInfo.id,
                    isDarkTheme = isDarkTheme,
                    onClick = { onPackageSelected(packageInfo.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Continue Button
        Button(
            onClick = {
                selectedPackageId?.let { packageId ->
                    onContinue(packageId)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedPackageId != null) {
                    if (isDarkTheme) Color(0xFF9D4FF7) else Color(0xFF8E0CFF)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            enabled = selectedPackageId != null && !isLoading
        ) {
            Text(
                text = "Paketi aç",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (selectedPackageId != null) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ColorfulPackageCard(
    packageInfo: PackageInfo,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val downloadProgress = packageInfo.downloadProgress ?: 0

    // ✅ Gradient renkler - seviyeye göre
    val gradientColors = when (packageInfo.level) {
        "A1" -> if (isDarkTheme) {
            listOf(Color(0xFF66BB6A), Color(0xFF4CAF50))
        } else {
            listOf(Color(0xFF4CAF50), Color(0xFF388E3C))
        }
        "A2" -> if (isDarkTheme) {
            listOf(Color(0xFF42A5F5), Color(0xFF2196F3))
        } else {
            listOf(Color(0xFF2196F3), Color(0xFF1976D2))
        }
        "B1" -> if (isDarkTheme) {
            listOf(Color(0xFFFF7043), Color(0xFFFF5722))
        } else {
            listOf(Color(0xFFFF5722), Color(0xFFE64A19))
        }
        "B2" -> if (isDarkTheme) {
            listOf(Color(0xFFFFCA28), Color(0xFFFFC107))
        } else {
            listOf(Color(0xFFFFC107), Color(0xFFFFA000))
        }
        "C1" -> if (isDarkTheme) {
            listOf(Color(0xFF26A69A), Color(0xFF009688))
        } else {
            listOf(Color(0xFF009688), Color(0xFF00796B))
        }
        "C2" -> if (isDarkTheme) {
            listOf(Color(0xFFEC407A), Color(0xFFBA68C8))
        } else {
            listOf(Color(0xFFE91E63), Color(0xFFAB47BC))
        }
        else -> if (isDarkTheme) {
            listOf(Color(0xFF78909C), Color(0xFF607D8B))
        } else {
            listOf(Color(0xFF90A4AE), Color(0xFF78909C))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() }
            .shadow(
                elevation = if (isSelected) 12.dp else 6.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = gradientColors.first().copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = if (isSelected) {
            BorderStroke(4.dp, Color(0xFFFB9322))
        } else null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(colors = gradientColors),
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            // ✅ GÜNCELLEME: Sağ üst köşe - Kelime sayısı VE indirme ikonu
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Kelime sayısı (üstte)
                if (packageInfo.wordCount > 0) {
                    Text(
                        text = "${packageInfo.wordCount} kelime",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Thin,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                // İndirme durumu (altta)
                when (packageInfo.downloadStatus) {
                    is PackageDownloadStatus.NotDownloaded -> {
                        // Modern indirme ikonu
                        DownloadIconBadge(
                            backgroundColor = Color.White.copy(alpha = 0.65f),
                            iconTint = gradientColors.first()
                        )
                    }
                    is PackageDownloadStatus.FullyDownloaded -> {
                        DownloadBadge(
                            text = "İndirildi ✓",
                            backgroundColor = Color(0xFF4CAF50),
                            textColor = Color.White
                        )
                    }
                    is PackageDownloadStatus.HasNewWords -> {
                        DownloadBadge(
                            text = "${packageInfo.newWordsCount} Yeni",
                            backgroundColor = Color(0xFFFF9800),
                            textColor = Color.White
                        )
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ✅ Level badge
                Text(
                    text = packageInfo.level,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = Color.White,
                    textAlign = TextAlign.Start
                )

                // Package name
                Text(
                    text = packageInfo.name,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )

                // Description
                Text(
                    text = packageInfo.description,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }

            // Download progress overlay
            if (downloadProgress > 0 && downloadProgress < 100) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            progress = downloadProgress / 100f,
                            modifier = Modifier.size(48.dp),
                            color = Color.White,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$downloadProgress%",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * ✅ GÜNCELLEME: Download Icon Badge - Daha modern ve küçük
 */
@Composable
private fun DownloadIconBadge(
    backgroundColor: Color,
    iconTint: Color
) {
    Icon(
        imageVector = Icons.Default.Download,
        contentDescription = "Download",
        tint = iconTint,
        modifier = Modifier.size(24.dp)
    )
}

/**
 * Download Badge Component (text için)
 */
@Composable
private fun DownloadBadge(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.wrapContentSize()
    ) {
        Text(
            text = text,
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}