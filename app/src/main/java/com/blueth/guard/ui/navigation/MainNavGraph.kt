package com.blueth.guard.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Battery4Bar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Rocket
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Battery4Bar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PhonelinkLock
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.blueth.guard.ui.screens.BatteryScreen
import com.blueth.guard.ui.screens.HomeScreen
import com.blueth.guard.ui.screens.OptimizerScreen
import com.blueth.guard.ui.screens.PrivacyScreen
import com.blueth.guard.ui.screens.SecurityScreen
import com.blueth.guard.ui.screens.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object SecurityRoute
@Serializable data object OptimizerRoute
@Serializable data object PrivacyRoute
@Serializable data object BatteryRoute
@Serializable data object SettingsRoute

data class TopLevelRoute(
    val label: String,
    val route: Any,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val topLevelRoutes = listOf(
    TopLevelRoute("Home", HomeRoute, Icons.Filled.Home, Icons.Outlined.Home),
    TopLevelRoute("Security", SecurityRoute, Icons.Filled.Security, Icons.Outlined.Security),
    TopLevelRoute("Privacy", PrivacyRoute, Icons.Filled.PhonelinkLock, Icons.Outlined.PhonelinkLock),
    TopLevelRoute("Battery", BatteryRoute, Icons.Filled.Battery4Bar, Icons.Outlined.Battery4Bar),
    TopLevelRoute("Optimize", OptimizerRoute, Icons.Filled.Rocket, Icons.Outlined.RocketLaunch)
)

@Composable
fun MainNavGraph() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                topLevelRoutes.forEach { topLevelRoute ->
                    val selected = currentDestination?.hierarchy?.any {
                        it.hasRoute(topLevelRoute.route::class)
                    } == true

                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(topLevelRoute.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) topLevelRoute.selectedIcon else topLevelRoute.unselectedIcon,
                                contentDescription = topLevelRoute.label
                            )
                        },
                        label = { Text(topLevelRoute.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
            },
            exitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(300))
            },
            popEnterTransition = {
                slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
            },
            popExitTransition = {
                slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(300))
            }
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onNavigateToSecurity = { navController.navigate(SecurityRoute) },
                    onNavigateToPrivacy = { navController.navigate(PrivacyRoute) },
                    onNavigateToBattery = { navController.navigate(BatteryRoute) },
                    onNavigateToOptimizer = { navController.navigate(OptimizerRoute) },
                    onNavigateToSettings = { navController.navigate(SettingsRoute) }
                )
            }
            composable<SecurityRoute> { SecurityScreen() }
            composable<OptimizerRoute> { OptimizerScreen() }
            composable<PrivacyRoute> { PrivacyScreen() }
            composable<BatteryRoute> { BatteryScreen() }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}
