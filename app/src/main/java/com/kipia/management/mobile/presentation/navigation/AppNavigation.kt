package com.kipia.management.mobile.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kipia.management.mobile.presentation.devices.DeviceListScreen
import com.kipia.management.mobile.presentation.screens.Screen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.DeviceList.route
    ) {
        composable(Screen.DeviceList.route) {
            DeviceListScreen(
                onDeviceClick = { deviceId ->
                    // Пока заглушка - потом перейдем к редактированию
                },
                onAddDeviceClick = {
                    // Пока заглушка - потом перейдем к добавлению
                }
            )
        }
    }
}