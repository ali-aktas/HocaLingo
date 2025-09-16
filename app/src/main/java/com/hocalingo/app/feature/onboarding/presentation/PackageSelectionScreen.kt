package com.hocalingo.app.feature.onboarding.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
    val coroutineScope = rememberCoroutineScope() // ✅ CoroutineScope eklendi

    // Local state for selection
    var selectedPackageId by remember { mutableStateOf<String?>(null) }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is PackageSelectionEffect.NavigateToWordSelection -> {
                    onNavigateToWordSelection(effect.packageId)
                }
                is PackageSelectionEffect.ShowMessage -> {
                    // A1 indirme başarısı mesajında direkt navigate et
                    if (effect.message.contains("🎉") && selectedPackageId == "A1") {
                        // Mesajı gösterme, direkt git
                        onNavigateToWordSelection("a1_en_tr_test_v1")
                    } else {
                        snackbarHostState.showSnackbar(effect.message)
                    }
                }
                is PackageSelectionEffect.ShowDownloadDialog -> {
                    // Auto-download for now
                    viewModel.onEvent(PackageSelectionEvent.DownloadPackage(effect.packageId))
                }
            }
        }
    }

    Scaffold(
        snackbarHost = {
            // ✅ SnackbarHost'u daha yukarı taşıdık
            Box(modifier = Modifier.fillMaxSize()) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp) // Continue butonundan uzak
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // "Choose your level" başlığı
            Text(
                text = "Choose your level",
                fontFamily = PoppinsFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 24.sp,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // Level kartları grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(getLevelPackages()) { levelPackage ->
                    LevelCard(
                        levelPackage = levelPackage,
                        isSelected = selectedPackageId == levelPackage.id,
                        onClick = {
                            selectedPackageId = levelPackage.id
                            viewModel.onEvent(PackageSelectionEvent.SelectPackage(levelPackage.id))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Continue butonu
            Button(
                onClick = {
                    selectedPackageId?.let { packageId ->
                        // A1 paketi için doğrudan indirme
                        if (packageId == "A1") {
                            val realPackageId = "a1_en_tr_test_v1"
                            viewModel.onEvent(PackageSelectionEvent.DownloadPackage(realPackageId))
                        } else {
                            // Diğer paketler için "yakında" mesajı
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Bu paket yakında eklenecek! A1 paketi ile devam edebilirsiniz.")
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D4FF)
                ),
                enabled = selectedPackageId != null && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
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
                        text = "Continue",
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LevelCard(
    levelPackage: LevelPackage,
    isSelected: Boolean,
    onClick: () -> Unit
) {
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
                    brush = levelPackage.gradient,
                    shape = RoundedCornerShape(20.dp)
                )
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(
                    imageVector = levelPackage.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Level ve açıklama
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Text(
                    text = levelPackage.level,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Text(
                    text = levelPackage.description,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (levelPackage.id) {
                        "A1" -> "Select words"
                        else -> "Download package"
                    },
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Level paketleri tanımları
private data class LevelPackage(
    val id: String,
    val level: String,
    val description: String,
    val gradient: Brush,
    val icon: ImageVector
)

private fun getLevelPackages(): List<LevelPackage> = listOf(
    LevelPackage(
        id = "A1",
        level = "A1",
        description = "Beginner - basic\neveryday words",
        gradient = Brush.linearGradient(
            colors = listOf(Color(0xFFFF6B35), Color(0xFFF7931E))
        ),
        icon = Icons.Outlined.MenuBook
    ),
    LevelPackage(
        id = "A2",
        level = "A2",
        description = "Elementary - simple\nsentences",
        gradient = Brush.linearGradient(
            colors = listOf(Color(0xFF4ECDC4), Color(0xFF44A08D))
        ),
        icon = Icons.Filled.Star
    ),
    LevelPackage(
        id = "B1",
        level = "B1",
        description = "Intermediate -\neveryday\nconversations",
        gradient = Brush.linearGradient(
            colors = listOf(Color(0xFF43E97B), Color(0xFF38F9D7))
        ),
        icon = Icons.Outlined.TrendingUp
    ),
    LevelPackage(
        id = "B2",
        level = "B2",
        description = "Upper Intermediate -\ncomplex topics",
        gradient = Brush.linearGradient(
            colors = listOf(Color(0xFF667eea), Color(0xFF764ba2))
        ),
        icon = Icons.Outlined.Psychology
    ),
    LevelPackage(
        id = "C1",
        level = "C1",
        description = "Advanced - nuanced\ndiscussions",
        gradient = Brush.linearGradient(
            colors = listOf(Color(0xFFf12711), Color(0xFFf5af19))
        ),
        icon = Icons.Outlined.EmojiEvents
    ),
    LevelPackage(
        id = "C2",
        level = "C2",
        description = "Proficient - mastery of\nthe language",
        gradient = Brush.linearGradient(
            colors = listOf(Color(0xFFff9a9e), Color(0xFFfecfef))
        ),
        icon = Icons.Outlined.Verified
    )
)

@Preview(showBackground = true)
@Composable
private fun PackageSelectionScreenPreview() {
    HocaLingoTheme {
        // Mock preview without ViewModel
        PackageSelectionScreen(
            onNavigateToWordSelection = {},
            onNavigateBack = {}
        )
    }
}