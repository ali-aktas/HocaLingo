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

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is PackageSelectionEffect.NavigateToWordSelection -> {
                    onNavigateToWordSelection(effect.packageId)
                }
                is PackageSelectionEffect.ShowDownloadDialog -> {
                    // Handle download dialog if needed
                }
                is PackageSelectionEffect.ShowMessage -> {
                    snackbarHostState.showSnackbar(effect.message)
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
                            // A1 paketi için doğrudan indirme
                            if (packageId == "a1_en_tr_test_v1") {
                                viewModel.onEvent(PackageSelectionEvent.DownloadPackage(packageId))
                            } else {
                                // Diğer paketler için mesaj
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Bu paket yakında eklenecek! A1 paketi ile devam edebilirsiniz.")
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Header
        Text(
            text = "Öğrenmek istediğin kelime paketini indir",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Package cards list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
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

        Spacer(modifier = Modifier.height(24.dp))

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
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = if (isLoading) "İndiriliyor..." else "Devam Et",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * UPDATED Colorful Package Card with Level-Based Gradients
 * ✅ A1: Orange/Yellow
 * ✅ A2: Green/Teal
 * ✅ B1: Blue/Purple
 * ✅ B2: Purple/Pink
 * ✅ C1: Red/Orange
 * ✅ C2: Pink/Purple
 */
@Composable
private fun ColorfulPackageCard(
    packageInfo: PackageInfo,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val isDownloaded = packageInfo.isDownloaded
    val downloadProgress = packageInfo.downloadProgress

    // UPDATED: Level-based gradient colors
    val gradientColors = when (packageInfo.level) {
        "A1" -> if (isDarkTheme) {
            listOf(Color(0xFFFF8A65), Color(0xFFFFB74D)) // Dark: Soft orange to yellow
        } else {
            listOf(Color(0xFFFF9800), Color(0xFFFFC107)) // Light: Orange to yellow
        }
        "A2" -> if (isDarkTheme) {
            listOf(Color(0xFF66BB6A), Color(0xFF26A69A)) // Dark: Green to teal
        } else {
            listOf(Color(0xFF4CAF50), Color(0xFF009688)) // Light: Green to teal
        }
        "B1" -> if (isDarkTheme) {
            listOf(Color(0xFF42A5F5), Color(0xFF7E57C2)) // Dark: Blue to purple
        } else {
            listOf(Color(0xFF2196F3), Color(0xFF9C27B0)) // Light: Blue to purple
        }
        "B2" -> if (isDarkTheme) {
            listOf(Color(0xFFAB47BC), Color(0xFFEC407A)) // Dark: Purple to pink
        } else {
            listOf(Color(0xFF9C27B0), Color(0xFFE91E63)) // Light: Purple to pink
        }
        "C1" -> if (isDarkTheme) {
            listOf(Color(0xFFEF5350), Color(0xFFFF7043)) // Dark: Red to orange
        } else {
            listOf(Color(0xFFF44336), Color(0xFFFF5722)) // Light: Red to orange
        }
        "C2" -> if (isDarkTheme) {
            listOf(Color(0xFFEC407A), Color(0xFFBA68C8)) // Dark: Pink to purple
        } else {
            listOf(Color(0xFFE91E63), Color(0xFFAB47BC)) // Light: Pink to purple
        }
        else -> if (isDarkTheme) {
            listOf(Color(0xFF78909C), Color(0xFF607D8B)) // Default gray
        } else {
            listOf(Color(0xFF90A4AE), Color(0xFF78909C))
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
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
            BorderStroke(3.dp, gradientColors.first())
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
            // Selection indicator
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(32.dp)
                        .background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = gradientColors.first(),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Level badge
                Box(
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.25f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = packageInfo.level,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }

                Column {
                    Text(
                        text = packageInfo.name,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = packageInfo.description,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Word count
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${packageInfo.wordCount} kelime",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.95f)
                        )
                    }

                    // Download status
                    if (isDownloaded) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "İndirildi",
                                fontFamily = PoppinsFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Download progress
                    if (downloadProgress > 0 && downloadProgress < 100) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = downloadProgress / 100f,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}