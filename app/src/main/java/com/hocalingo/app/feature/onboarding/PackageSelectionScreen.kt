package com.hocalingo.app.feature.onboarding

import androidx.compose.foundation.BorderStroke
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
                .padding(paddingValues)
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
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(2.dp))

        // Header
        Text(
            text = "Hocalingo seviye paketleri",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
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

        Spacer(modifier = Modifier.height(18.dp))

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
                    if (isDarkTheme) Color(0xFF4FC3F7) else Color(0xFF00D4FF)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                },
                disabledContainerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            ),
            enabled = selectedPackageId != null && !isLoading
        ) {
            Text(
                text = "Devam Et",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

/**
 * ✅ UPDATED: Colorful Package Card
 * - İndir yazısı yerine ICON
 * - Level badge arka planı KALDIRILDI
 * - Kelime sayısı korundu
 */
@Composable
private fun ColorfulPackageCard(
    packageInfo: PackageInfo,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val downloadProgress = packageInfo.downloadProgress

    val gradientColors = when (packageInfo.level) {
        "A1" -> if (isDarkTheme) {
            listOf(Color(0xFF66BB6A), Color(0xFF81C784))
        } else {
            listOf(Color(0xFF4CAF50), Color(0xFF8BC34A))
        }
        "A2" -> if (isDarkTheme) {
            listOf(Color(0xFF26C6DA), Color(0xFF29B6F6))
        } else {
            listOf(Color(0xFF00BCD4), Color(0xFF03A9F4))
        }
        "B1" -> if (isDarkTheme) {
            listOf(Color(0xFF42A5F5), Color(0xFF7E57C2))
        } else {
            listOf(Color(0xFF2196F3), Color(0xFF9C27B0))
        }
        "B2" -> if (isDarkTheme) {
            listOf(Color(0xFFAB47BC), Color(0xFFEC407A))
        } else {
            listOf(Color(0xFF9C27B0), Color(0xFFE91E63))
        }
        "C1" -> if (isDarkTheme) {
            listOf(Color(0xFFEF5350), Color(0xFFFF7043))
        } else {
            listOf(Color(0xFFF44336), Color(0xFFFF5722))
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
            .height(120.dp)
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
            BorderStroke(4.dp, gradientColors.first())
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
            // ✅ UPDATED: Download Status Badge (sağ üst köşe) - İCON
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            ) {
                when (packageInfo.downloadStatus) {
                    is PackageDownloadStatus.NotDownloaded -> {
                        // ✅ İndir yazısı yerine ICON
                        DownloadIconBadge(
                            backgroundColor = Color.White.copy(alpha = 0.95f),
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
                // ✅ UPDATED: Level badge - ARKA PLAN KALDIRILDI
                Text(
                    text = packageInfo.level,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Black,  // Kalın font
                    fontSize = 18.sp,
                    color = Color.White,
                    textAlign = TextAlign.Start
                )

                // Package name
                Text(
                    text = packageInfo.name,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )

                // Description & word count
                Column {
                    Text(
                        text = packageInfo.description,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // ✅ Kelime sayısı korundu
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "Words",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (packageInfo.wordCount > 0) {
                                "${packageInfo.wordCount} kelime"
                            } else {
                                "Yükleniyor..."
                            },
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
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
 * ✅ YENİ: Download Icon Badge (sadece icon)
 * "İndir" yazısı yerine download icon
 */
@Composable
private fun DownloadIconBadge(
    backgroundColor: Color,
    iconTint: Color
) {
    Surface(
        color = backgroundColor,
        shape = CircleShape,
        modifier = Modifier.size(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Download",
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
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