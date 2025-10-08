package com.hocalingo.app.core.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hocalingo.app.HocaRoutes
import com.hocalingo.app.R
import com.hocalingo.app.core.ui.theme.HocaLingoTheme

// Poppins font family
private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

/**
 * UPDATED Bottom Navigation - Fixed to Bottom
 * ✅ No floating padding
 * ✅ Direct attachment to screen bottom
 * ✅ Gradient colors maintained
 */
@Composable
fun HocaBottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    // Navigation items
    val bottomNavItems = remember {
        listOf(
            BottomNavItem(
                route = HocaRoutes.HOME,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                labelRes = R.string.nav_home,
                label = "Ana Sayfa"
            ),
            BottomNavItem(
                route = HocaRoutes.ADD_WORD,
                selectedIcon = Icons.Filled.Add,
                unselectedIcon = Icons.Outlined.Add,
                labelRes = R.string.nav_add_word,
                label = "Ekle"
            ),
            BottomNavItem(
                route = HocaRoutes.STUDY,
                selectedIcon = Icons.Filled.School,
                unselectedIcon = Icons.Outlined.School,
                labelRes = R.string.nav_study,
                label = "Çalış",
                isMainAction = true
            ),
            BottomNavItem(
                route = HocaRoutes.PROFILE,
                selectedIcon = Icons.Filled.Settings,
                unselectedIcon = Icons.Outlined.Settings,
                labelRes = R.string.nav_profile,
                label = "Ayarlar"
            )
        )
    }

    // UPDATED: Direct container without padding
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), // Rounded top corners only
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFC28152).copy(alpha = 0.95f),
                            Color(0xFFE07B2D).copy(alpha = 0.95f),
                            Color(0xFFBA8546).copy(alpha = 0.95f)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.3f)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentDestination?.startsWith(item.route) == true

                    FixedNavItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
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
private fun FixedNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.2f else 1f,
        animationSpec = tween(200),
        label = "scale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
        animationSpec = tween(200),
        label = "iconColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected && item.isMainAction) {
            Color.White.copy(alpha = 0.2f)
        } else if (isSelected) {
            Color.White.copy(alpha = 0.15f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(200),
        label = "backgroundColor"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (item.isMainAction) 20.dp else 12.dp,
                vertical = 8.dp
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(
                if (item.isMainAction) 28.dp else 24.dp
            )
        )

        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.label,
                fontFamily = PoppinsFontFamily,
                fontWeight = if (item.isMainAction) FontWeight.Bold else FontWeight.Medium,
                fontSize = if (item.isMainAction) 12.sp else 10.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelRes: Int,
    val label: String,
    val isMainAction: Boolean = false
)

fun shouldShowBottomNavigation(currentRoute: String?): Boolean {
    return when {
        currentRoute == null -> false
        currentRoute.startsWith(HocaRoutes.SPLASH) -> false
        currentRoute.startsWith(HocaRoutes.AUTH) -> false
        currentRoute.startsWith(HocaRoutes.ONBOARDING_LANGUAGE) -> false
        currentRoute.startsWith(HocaRoutes.ONBOARDING_LEVEL) -> false
        currentRoute.startsWith(HocaRoutes.WORD_SELECTION) -> false
        currentRoute.startsWith(HocaRoutes.STUDY) -> false
        else -> true
    }
}

@Preview(showBackground = true)
@Composable
private fun FixedBottomNavigationPreview() {
    HocaLingoTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF8FAFA))
        ) {
            Spacer(modifier = Modifier.weight(1f))
            HocaBottomNavigationBar(
                navController = rememberNavController()
            )
        }
    }
}