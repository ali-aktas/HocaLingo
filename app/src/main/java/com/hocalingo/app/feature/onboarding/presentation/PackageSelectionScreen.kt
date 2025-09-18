package com.hocalingo.app.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.components.HocaErrorState
import com.hocalingo.app.core.ui.components.HocaLoadingIndicator
import com.hocalingo.app.core.ui.theme.HocaLingoTheme
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
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
            text = "Öğrenmek istediğin seviye paketini indir",
            fontFamily = PoppinsFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = Color(0xFF2C3E50),
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

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
                    onClick = { onPackageSelected(packageInfo.id) }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ✅ FIXED: Continue Button - Conditional enabling
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
                containerColor = if (selectedPackageId != null) Color(0xFF00D4FF) else Color(0xFFE0E0E0),
                disabledContainerColor = Color(0xFFE0E0E0)
            ),
            enabled = selectedPackageId != null && !isLoading // ✅ Only enabled when package selected
        ) {
            if (isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Loading...",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            } else {
                Text(
                    text = if (selectedPackageId != null) "Continue" else "Select a Package",
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (selectedPackageId != null) Color.White else Color(0xFF999999)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ✅ FIXED: Real Package Card with original gradient colors
@Composable
private fun PackageCard(
    packageInfo: PackageInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // ✅ Get original gradient for each level
    val gradient = getOriginalGradient(packageInfo.level)
    val icon = getLevelIcon(packageInfo.level)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clickable { onClick() }
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 3.dp,
                        color = Color(0xFF00D4FF),
                        shape = RoundedCornerShape(20.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = gradient, // ✅ Using original gradients
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            // Level icon
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // ✅ FIXED: Download status indicator - Simplified
            Box(
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopEnd)
                    .background(
                        color = Color.White.copy(alpha = 0.9f),
                        shape = CircleShape
                    )
                    .padding(6.dp)
            ) {
                if (packageInfo.isDownloaded) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Downloaded",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Outlined.FileDownload,
                        contentDescription = "Download",
                        tint = Color(0xFF757575),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Level ve açıklama
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = packageInfo.level,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = packageInfo.name,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = packageInfo.description,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 14.sp,
                    maxLines = 2
                )
            }
        }
    }
}

// ✅ Original gradient colors from the old system
private fun getOriginalGradient(level: String): Brush {
    return when (level) {
        "A1" -> Brush.linearGradient(
            colors = listOf(Color(0xFFFF6B35), Color(0xFFF7931E))
        )
        "A2" -> Brush.linearGradient(
            colors = listOf(Color(0xFF4ECDC4), Color(0xFF44A08D))
        )
        "B1" -> Brush.linearGradient(
            colors = listOf(Color(0xFF43E97B), Color(0xFF38F9D7))
        )
        "B2" -> Brush.linearGradient(
            colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
        )
        "C1" -> Brush.linearGradient(
            colors = listOf(Color(0xFFf12711), Color(0xFFf5af19))
        )
        "C2" -> Brush.linearGradient(
            colors = listOf(Color(0xFFff9a9e), Color(0xFFfecfef))
        )
        else -> Brush.linearGradient(
            colors = listOf(Color(0xFFFF6B35), Color(0xFFF7931E))
        )
    }
}

// Helper function for level icons
private fun getLevelIcon(level: String): ImageVector {
    return when (level) {
        "A1" -> Icons.Outlined.MenuBook
        "A2" -> Icons.Filled.Star
        "B1" -> Icons.Outlined.TrendingUp
        "B2" -> Icons.Outlined.Psychology
        "C1" -> Icons.Outlined.EmojiEvents
        "C2" -> Icons.Outlined.Verified
        else -> Icons.Outlined.MenuBook
    }
}

@Preview(showBackground = true)
@Composable
private fun PackageSelectionScreenPreview() {
    HocaLingoTheme {
        PackageSelectionScreen(
            onNavigateToWordSelection = {},
            onNavigateBack = {}
        )
    }
}