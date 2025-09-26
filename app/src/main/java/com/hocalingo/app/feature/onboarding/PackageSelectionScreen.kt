package com.hocalingo.app.feature.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaErrorState
import com.hocalingo.app.core.ui.components.HocaLoadingIndicator
import com.hocalingo.app.core.ui.theme.ThemeViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Poppins font family tanımlaması
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold),
    Font(R.font.poppins_black, FontWeight.Black)
)

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

    // ✅ THEME ADAPTATION - Get theme state
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val isDarkTheme = themeViewModel.shouldUseDarkTheme()

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
                // Removed ShowDownloadDialog handling - not needed anymore
                is PackageSelectionEffect.ShowDownloadDialog -> {
                    // Deprecated - download happens directly from continue button
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background // ✅ Theme-aware background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // ✅ Theme-aware background
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    HocaLoadingIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        text = "Paketler yükleniyor..."
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
                        isDarkTheme = isDarkTheme, // ✅ Pass theme state
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
    isDarkTheme: Boolean, // ✅ New parameter for theme awareness
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

        // Header - ✅ Theme-aware text color
        Text(
            text = "Öğrenmek istediğin kelime paketini indir",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onBackground, // ✅ Theme-aware
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Package Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(packages) { packageInfo ->
                PackageCard(
                    packageInfo = packageInfo,
                    isSelected = selectedPackageId == packageInfo.id,
                    isDarkTheme = isDarkTheme, // ✅ Pass theme state to cards
                    onClick = { onPackageSelected(packageInfo.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ✅ THEME-AWARE Continue Button
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

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun PackageCard(
    packageInfo: PackageInfo,
    isSelected: Boolean,
    isDarkTheme: Boolean, // ✅ New theme parameter
    onClick: () -> Unit
) {
    val backgroundColor = Color(packageInfo.color.removePrefix("#").toLong(16) or 0xFF000000)
    val isDownloaded = packageInfo.isDownloaded
    val downloadProgress = packageInfo.downloadProgress

    // ✅ Theme-aware card container
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) {
                MaterialTheme.colorScheme.surfaceVariant
            } else {
                Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 12.dp else 6.dp
        ),
        border = if (isSelected) {
            BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Gradient background for package color
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                backgroundColor.copy(alpha = 0.1f),
                                backgroundColor.copy(alpha = 0.05f)
                            )
                        )
                    )
            )

            // Selection indicator (top-right corner)
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(24.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Package info
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Level badge
                Box(
                    modifier = Modifier
                        .background(backgroundColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = packageInfo.level,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White
                    )
                }

                // Content
                Column {
                    Text(
                        text = packageInfo.name,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface // ✅ Theme-aware
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = packageInfo.description,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // ✅ Theme-aware
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Word count and status
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = backgroundColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${packageInfo.wordCount} kelime",
                            fontFamily = PoppinsFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant // ✅ Theme-aware
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        // Download status
                        when {
                            isDownloaded -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Downloaded",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            downloadProgress > 0 -> {
                                CircularProgressIndicator(
                                    progress = downloadProgress / 100f,
                                    modifier = Modifier.size(16.dp),
                                    color = backgroundColor,
                                    strokeWidth = 2.dp
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "Not downloaded",
                                    tint = MaterialTheme.colorScheme.outline, // ✅ Theme-aware
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}