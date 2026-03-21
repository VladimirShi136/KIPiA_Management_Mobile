package com.kipia.management.mobile.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kipia.management.mobile.R
import com.kipia.management.mobile.ui.theme.getBottomNavButtonColor
import com.kipia.management.mobile.ui.theme.getBottomNavColors
import com.kipia.management.mobile.ui.theme.getBottomNavTextColor
import timber.log.Timber

// Модель для элементов навигации
sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
) {
    object Devices : BottomNavItem("devices", R.string.title_devices)
    object Photos : BottomNavItem("photos", R.string.photo_gallery)
    object Schemes : BottomNavItem("schemes", R.string.title_schemes)
    object Reports : BottomNavItem("reports", R.string.title_reports)
}

val bottomNavItems = listOf(
    BottomNavItem.Devices,
    BottomNavItem.Photos,
    BottomNavItem.Schemes,
    BottomNavItem.Reports
)

private val BUTTON_COLORS = bottomNavItems.mapIndexed { index, _ ->
    getBottomNavButtonColor(index)
}
private val TEXT_COLORS = BUTTON_COLORS.map { getBottomNavTextColor(it) }
private val BUTTON_COLORS_INACTIVE = BUTTON_COLORS.map { it.copy(alpha = 0.7f) }
private val TEXT_COLORS_INACTIVE = TEXT_COLORS.map { it.copy(alpha = 0.9f) }


@Composable
fun BottomNavigationBar(
    navController: NavController,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val bottomNavColors = getBottomNavColors(isDarkTheme)

    val isDevicesTabActive = currentRoute == "devices" ||
            currentRoute?.startsWith("device_") == true
    val isSchemesTabActive = currentRoute == "schemes" ||
            currentRoute?.startsWith("scheme_") == true
    val isPhotosTabActive = currentRoute == "photos" ||
            currentRoute?.startsWith("fullscreen_photo") == true
    val isReportsTabActive = currentRoute == "reports"

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = bottomNavColors.background,
        shape = RectangleShape,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bottomNavItems.forEachIndexed { index, item ->
                val isSelected = when (item) {
                    is BottomNavItem.Devices -> isDevicesTabActive
                    is BottomNavItem.Schemes -> isSchemesTabActive
                    is BottomNavItem.Photos -> isPhotosTabActive
                    is BottomNavItem.Reports -> isReportsTabActive
                }

                TextButton(
                    onClick = {
                        if (!isSelected) resetToTabRoot(navController, item.route)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                        .height(44.dp)
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) TEXT_COLORS[index]
                            else bottomNavColors.border,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .background(
                            color = if (isSelected) BUTTON_COLORS[index]
                            else BUTTON_COLORS_INACTIVE[index],
                            shape = RoundedCornerShape(12.dp)
                        ),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        contentColor = TEXT_COLORS[index]
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = stringResource(id = item.titleResId),
                        color = if (isSelected) TEXT_COLORS[index]
                        else TEXT_COLORS_INACTIVE[index],
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold
                        else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Сброс к корневому экрану таба
 */
private fun resetToTabRoot(navController: NavController, targetRoute: String) {
    navController.navigate(targetRoute) {
        // Полная очистка стека
        popUpTo(0) {
            saveState = false
        }

        launchSingleTop = true
        restoreState = false
    }
}