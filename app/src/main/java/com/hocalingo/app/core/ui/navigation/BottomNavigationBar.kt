package com.hocalingo.app.core.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
 * Modern Light-Themed Bottom Navigation Bar
 * Clean design with proper icons and smooth animations
 */
@Composable
fun HocaBottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    // Define navigation items with CORRECT icons
    val bottomNavItems = remember {
        listOf(
            BottomNavItem(
                route = HocaRoutes.HOME,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                labelRes = R.string.nav_home,
                label = "Home"
            ),
            BottomNavItem(
                route = HocaRoutes.STUDY,
                selectedIcon = Icons.Filled.School, // ✅ Daha uygun ikon
                unselectedIcon = Icons.Outlined.School,
                labelRes = R.string.nav_study,
                label = "Study"
            ),
            BottomNavItem(
                route = HocaRoutes.ADD_WORD,
                selectedIcon = Icons.Filled.Add,
                unselectedIcon = Icons.Outlined.Add,
                labelRes = R.string.nav_add_word,
                label = "Add Word"
            ),
            BottomNavItem(
                route = HocaRoutes.PROFILE,
                selectedIcon = Icons.Filled.AccountCircle, // ✅ Profile için doğru ikon
                unselectedIcon = Icons.Outlined.AccountCircle,
                labelRes = R.string.nav_profile,
                label = "Profile"
            )
        )
    }

    // Modern bottom navigation container
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEach { item ->
                val isSelected = currentDestination?.startsWith(item.route) == true

                ModernBottomNavItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = {
                        if (currentDestination != item.route) {
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

@Composable
private fun ModernBottomNavItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Smooth animations
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "icon_scale"
    )

    val iconColor = if (isSelected) {
        Color(0xFF2196F3) // Açık mavi
    } else {
        Color(0xFF9E9E9E) // Açık gri
    }

    val backgroundColor = if (isSelected) {
        Color(0xFF2196F3).copy(alpha = 0.1f) // Çok açık mavi background
    } else {
        Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() } // ✅ Material 3 default ripple
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Icon container with background
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = backgroundColor,
                    shape = CircleShape
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Label
        Text(
            text = item.label,
            fontFamily = PoppinsFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.sp,
            color = iconColor,
            maxLines = 1
        )

        // Selection indicator dot
        if (isSelected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(
                        color = Color(0xFF2196F3),
                        shape = CircleShape
                    )
            )
        }
    }
}

/**
 * Data class for bottom navigation items
 */
data class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val labelRes: Int,
    val label: String
)

/**
 * Helper function to check if route should show bottom navigation
 */
fun shouldShowBottomNavigation(currentRoute: String?): Boolean {
    return when {
        currentRoute == null -> false
        currentRoute.startsWith(HocaRoutes.SPLASH) -> false
        currentRoute.startsWith(HocaRoutes.AUTH) -> false
        currentRoute.startsWith(HocaRoutes.ONBOARDING_LANGUAGE) -> false
        currentRoute.startsWith(HocaRoutes.ONBOARDING_LEVEL) -> false
        currentRoute.startsWith(HocaRoutes.WORD_SELECTION) -> false
        else -> true
    }
}

@Preview(showBackground = true)
@Composable
private fun HocaBottomNavigationBarPreview() {
    HocaLingoTheme {
        Column {
            Spacer(modifier = Modifier.weight(1f))
            HocaBottomNavigationBar(
                navController = rememberNavController()
            )
        }
    }
}