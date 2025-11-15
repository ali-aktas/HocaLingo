package com.hocalingo.app.core.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

private val PoppinsFontFamily = FontFamily(
    Font(R.font.poppins_regular, FontWeight.Normal),
    Font(R.font.poppins_medium, FontWeight.Medium),
    Font(R.font.poppins_bold, FontWeight.Bold)
)

/**
 * Professional Bottom Navigation - Fixed for All Devices
 *
 * ✅ Gesture navigation (swipe) - Works perfectly
 * ✅ Button navigation (3 buttons) - No overlap, no shrinking
 * ✅ Content height: 70dp (always consistent)
 * ✅ Total height: 70dp + system bar padding (automatic)
 *
 * CRITICAL FIX:
 * - Card uses wrapContentHeight() instead of fixed height
 * - Box inside Card has navigationBarsPadding()
 * - Content (Row) has fixed 70dp height
 * - Result: Content stays 70dp, total height adapts to system bars
 */
@Composable
fun HocaBottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

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
                route = HocaRoutes.STUDY,
                selectedIcon = Icons.Filled.School,
                unselectedIcon = Icons.Outlined.School,
                labelRes = R.string.nav_study,
                label = "Çalış",
                isMainAction = true
            ),
            BottomNavItem(
                route = HocaRoutes.ADD_WORD,
                selectedIcon = Icons.Filled.Add,
                unselectedIcon = Icons.Outlined.Add,
                labelRes = R.string.nav_add_word,
                label = "Kelime Ekle"
            ),
            BottomNavItem(
                route = HocaRoutes.PROFILE,
                selectedIcon = Icons.Filled.Person2,
                unselectedIcon = Icons.Outlined.Person2,
                labelRes = R.string.nav_profile,
                label = "Profil"
            )
        )
    }

    // ✅ CRITICAL FIX:
    // - Card height is wrapContentHeight (adapts to content)
    // - Box inside has navigationBarsPadding() for system bars
    // - Row has fixed 70dp height for consistent UI
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(), // 🎯 Adapts to content + system bars
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFFF6B00).copy(alpha = 0.95f),
                            Color(0xFFCD7A37).copy(alpha = 0.95f),
                            Color(0xFFFF920E).copy(alpha = 0.95f)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .navigationBarsPadding() // 🎯 Handles system navigation bars
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp) // 🎯 Fixed content height - always 70dp
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                bottomNavItems.forEach { item ->
                    val isSelected = currentDestination == item.route
                    BottomNavigationItem(
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
}

@Composable
private fun BottomNavigationItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected && item.isMainAction) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
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
            .clip(RoundedCornerShape(0.dp))
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
        currentRoute.startsWith(HocaRoutes.ONBOARDING_INTRO) -> false
        currentRoute.startsWith(HocaRoutes.ONBOARDING_LANGUAGE) -> false
        currentRoute.startsWith(HocaRoutes.ONBOARDING_LEVEL) -> false
        currentRoute.startsWith(HocaRoutes.WORD_SELECTION) -> false
        currentRoute.startsWith(HocaRoutes.STUDY) -> false
        currentRoute.startsWith(HocaRoutes.AI_ASSISTANT) -> false
        currentRoute.startsWith(HocaRoutes.AI_STORY_DETAIL) -> false
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