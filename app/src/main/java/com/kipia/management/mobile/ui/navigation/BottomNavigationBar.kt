package com.kipia.management.mobile.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kipia.management.mobile.R

// Модель для элементов навигации
sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
    val iconResId: Int
) {
    object Devices : BottomNavItem(
        route = "devices",
        titleResId = R.string.title_devices,
        iconResId = R.drawable.ic_devices
    )

    object Photos : BottomNavItem(
        route = "photos",
        titleResId = R.string.photo_gallery,
        iconResId = R.drawable.ic_photo
    )

    object Schemes : BottomNavItem(
        route = "schemes",
        titleResId = R.string.title_schemes,
        iconResId = R.drawable.ic_schemes  // Создать иконку
    )

    object Reports : BottomNavItem(
        route = "reports",
        titleResId = R.string.title_reports,
        iconResId = R.drawable.ic_reports
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Devices,
    BottomNavItem.Photos,
    BottomNavItem.Schemes,
    BottomNavItem.Reports
)

@Composable
fun BottomNavigationBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        // Очищаем back stack при навигации к основным экранам
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = stringResource(id = item.titleResId)
                    )
                },
                label = {
                    Text(text = stringResource(id = item.titleResId))
                }
            )
        }
    }
}