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
import androidx.compose.ui.res.painterResource
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
 * Bottom Navigation Item Data Class
 */
data class BottomNavItem(
    val route: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null,
    val selectedIconRes: Int? = null,
    val unselectedIconRes: Int? = null,
    val labelRes: Int,
    val label: String,
    val isMainAction: Boolean = false
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
 *
 * UPDATED:
 * - STUDY route → StudyMainScreen (Hub, shows bottom nav)
 * - STUDY_SCREEN route → StudyScreen (Actual study, hides bottom nav)
 */
@Composable
fun HocaBottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem(
            route = HocaRoutes.HOME,
            selectedIconRes = R.drawable.smart_home,
            unselectedIconRes = R.drawable.smart_home,
            labelRes = R.string.nav_home,
            label = "Ana Sayfa"
        ),
        BottomNavItem(
            route = HocaRoutes.STUDY,
            selectedIconRes = R.drawable.flame,
            unselectedIconRes = R.drawable.flame,
            labelRes = R.string.nav_study,
            label = "Çalışma",
            isMainAction = true
        ),
        BottomNavItem(
            route = HocaRoutes.AI_ASSISTANT,
            selectedIconRes = R.drawable.robot,
            unselectedIconRes = R.drawable.robot,
            labelRes = R.string.nav_ai_assistant,
            label = "Yapay Zeka"
        ),
        BottomNavItem(
            route = HocaRoutes.PROFILE,
            selectedIconRes = R.drawable.settings,
            unselectedIconRes = R.drawable.settings,
            labelRes = R.string.nav_profile,
            label = "Ayarlar"
        )
    )


    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF673AB7).copy(alpha = 1f),
                            Color(0xFF431F84).copy(alpha = 1f),
                            Color(0xFF3D197D).copy(alpha = 1f)
                        )
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
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

/**
 * Single Bottom Navigation Item
 */
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
        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 200),
        label = "iconColor"
    )

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (item.selectedIconRes != null && item.unselectedIconRes != null) {
            Icon(
                painter = painterResource(
                    id = if (isSelected)
                        item.selectedIconRes
                    else
                        item.unselectedIconRes
                ),
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(if (item.isMainAction) 28.dp else 24.dp)
            )
        } else {
            Icon(
                imageVector = if (isSelected)
                    item.selectedIcon ?: Icons.Filled.Home
                else
                    item.unselectedIcon ?: Icons.Outlined.Home,
                contentDescription = item.label,
                tint = iconColor,
                modifier = Modifier.size(if (item.isMainAction) 28.dp else 24.dp)
            )
        }


        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = item.label,
            fontFamily = PoppinsFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = if (item.isMainAction) 11.sp else 10.sp,
            color = iconColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Bottom Navigation Visibility Logic
 *
 * ✅ UPDATED: ONBOARDING_LEVEL removed from hidden routes
 * Now package selection screen shows bottom navigation in both flows:
 * - Onboarding flow (ONBOARDING_LEVEL)
 * - Main app flow (PACKAGE_SELECTION)
 *
 * Hidden only in:
 * - Splash, Auth, Onboarding Intro (initial setup)
 * - Word Selection (focused selection experience)
 * - Study Session (focused study experience)
 * - AI Assistant screens (immersive AI experience)
 */
fun shouldShowBottomNavigation(
    currentRoute: String?
): Boolean {
    return when {
        currentRoute == null -> false
        currentRoute.startsWith(HocaRoutes.SPLASH) -> false
        currentRoute.startsWith(HocaRoutes.AUTH) -> false
        currentRoute.startsWith(HocaRoutes.ONBOARDING_INTRO) -> false
        currentRoute.startsWith(HocaRoutes.ONBOARDING_LANGUAGE) -> false
        currentRoute.startsWith(HocaRoutes.WORD_SELECTION) -> false
        currentRoute.startsWith(HocaRoutes.STUDY_SCREEN) -> false  // Hide in actual study session
        else -> true  // ✅ ONBOARDING_LEVEL artık true dönecek
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