package com.kipia.management.mobile.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kipia.management.mobile.R
import timber.log.Timber


// Модель для элементов навигации
sealed class BottomNavItem(
    val route: String,
    val titleResId: Int,
    val backgroundColor: Color
) {
    object Devices : BottomNavItem(
        route = "devices",
        titleResId = R.string.title_devices,
        backgroundColor = Color(0xFFBBBF06) // #bbbf06
    )

    object Photos : BottomNavItem(
        route = "photos",
        titleResId = R.string.photo_gallery,
        backgroundColor = Color(0xFF14B1B1) // #14b1b1
    )

    object Schemes : BottomNavItem(
        route = "schemes",
        titleResId = R.string.title_schemes,
        backgroundColor = Color(0xFF620F8C) // #620f8c
    )

    object Reports : BottomNavItem(
        route = "reports",
        titleResId = R.string.title_reports,
        backgroundColor = Color(0xFFE67E22) // #e67e22
    )
}

val bottomNavItems = listOf(
    BottomNavItem.Devices,
    BottomNavItem.Photos,
    BottomNavItem.Schemes,
    BottomNavItem.Reports
)

@Composable
fun BottomNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Определяем, активен ли таб devices
    val isDevicesTabActive = when {
        currentRoute == "devices" -> true
        currentRoute?.startsWith("device_") == true -> true
        else -> false
    }

    val isSchemesTabActive = when {
        currentRoute == "schemes" -> true
        currentRoute?.startsWith("scheme_") == true -> true
        else -> false
    }

    val isPhotosTabActive = currentRoute == "photos" || currentRoute?.startsWith("fullscreen_photo") == true

    val isReportsTabActive = currentRoute == "reports"

    // Используем Row вместо NavigationBar для кастомного дизайна
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(MaterialTheme.colorScheme.secondary)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        bottomNavItems.forEach { item ->
            val isSelected = when (item) {
                is BottomNavItem.Devices -> isDevicesTabActive
                is BottomNavItem.Schemes -> isSchemesTabActive
                is BottomNavItem.Photos -> isPhotosTabActive
                is BottomNavItem.Reports -> isReportsTabActive
                else -> false
            }

            // Текстовая кнопка в рамке
            TextButton(
                onClick = {
                    if (!isSelected) {
                        resetToTabRoot(navController, item.route)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .height(44.dp)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .background(
                        color = item.backgroundColor.copy(alpha = if (isSelected) 1f else 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = stringResource(id = item.titleResId),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}

/**
 * Сброс к корневому экрану таба
 */
private fun resetToTabRoot(navController: NavController, targetRoute: String) {
    Timber.d("═══════════════════════════════════════════")
    Timber.d("RESET TO TAB ROOT: $targetRoute")

    // ★★★★ КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ ★★★★
    // НЕ используем restoreState - это восстанавливает предыдущее состояние
    navController.navigate(targetRoute) {
        // Полная очистка стека
        popUpTo(0) {
            saveState = false  // ★★★★ ВАЖНО: не сохраняем состояние ★★★★
        }

        launchSingleTop = true
        restoreState = false  // ★★★★ ВАЖНО: не восстанавливаем состояние ★★★★

        // Отключаем анимации
        anim {
            enter = 0
            exit = 0
            popEnter = 0
            popExit = 0
        }
    }

    Timber.d("Reset navigation completed")
    Timber.d("═══════════════════════════════════════════")
}