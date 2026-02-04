package com.kipia.management.mobile.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.ui.screens.devices.DeviceDetailScreen
import com.kipia.management.mobile.ui.screens.devices.DeviceEditScreen
import com.kipia.management.mobile.ui.screens.devices.DevicesScreen
import com.kipia.management.mobile.ui.screens.photos.FullScreenPhotoContainer
import com.kipia.management.mobile.ui.screens.photos.PhotosScreen
import com.kipia.management.mobile.ui.screens.reports.ReportsScreen
import com.kipia.management.mobile.ui.screens.schemes.SchemeEditorScreen
import com.kipia.management.mobile.ui.screens.schemes.SchemesScreen
import com.kipia.management.mobile.ui.screens.settings.SettingsScreen
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.utils.PhotoManager
import com.kipia.management.mobile.viewmodel.DevicesViewModel
import com.kipia.management.mobile.viewmodel.PhotosViewModel

/**
 * Чистый навигационный хост - только маршрутизация
 */
@Composable
fun KIPiANavHost(
    navController: NavHostController = rememberNavController(),
    devicesViewModel: DevicesViewModel,
    photosViewModel: PhotosViewModel,
    topAppBarController: TopAppBarController,
    notificationManager: NotificationManager,
    photoManager: PhotoManager, // ✅ ДОБАВЛЕНО параметр
    updateBottomNavVisibility: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
    startDestination: String = BottomNavItem.Devices.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
            .fillMaxSize()
    ) {
        // Экран устройств
        composable(BottomNavItem.Devices.route) {
            DevicesScreen(
                updateBottomNavVisibility = updateBottomNavVisibility,
                onNavigateToDeviceDetail = { deviceId ->
                    navController.navigate("device_detail/$deviceId")
                },
                onNavigateToDeviceEdit = { deviceId ->
                    val route = if (deviceId != null) {
                        "device_edit/$deviceId"
                    } else {
                        "device_edit"
                    }
                    navController.navigate(route)
                },
                viewModel = devicesViewModel,
                deleteViewModel = hiltViewModel(),
                notificationManager = notificationManager
            )
        }

        // Детальный экран устройства
        composable("device_detail/{deviceId}") { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId")?.toIntOrNull()
            if (deviceId != null) {
                DeviceDetailScreen(
                    deviceId = deviceId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { navController.navigate("device_edit/$deviceId") },
                    onNavigateToPhotos = { photoIndex, device ->
                        navController.navigate("fullscreen_photo/$deviceId/$photoIndex")
                    }
                )
            }
        }

        // Экран редактирования прибора
        composable("device_edit/{deviceId}") { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId")?.toIntOrNull()
            DeviceEditScreen(
                deviceId = deviceId,
                onNavigateBack = {
                    navController.popBackStack("devices", false)
                    if (!navController.popBackStack()) {
                        navController.navigate("devices") {
                            popUpTo(0) { saveState = false }
                            launchSingleTop = true
                        }
                    }
                },
                topAppBarController = topAppBarController,
                viewModel = hiltViewModel(),
                notificationManager = notificationManager,
                deleteViewModel = hiltViewModel(),
                photoManager = photoManager // ✅ ПЕРЕДАЕМ photoManager
            )
        }

        // Экран создания прибора
        composable("device_edit") {
            DeviceEditScreen(
                deviceId = null,
                onNavigateBack = { navController.popBackStack() },
                topAppBarController = topAppBarController,
                viewModel = hiltViewModel(),
                notificationManager = notificationManager,
                deleteViewModel = hiltViewModel(),
                photoManager = photoManager // ✅ ПЕРЕДАЕМ photoManager
            )
        }

        // Схемы
        composable(BottomNavItem.Schemes.route) {
            SchemesScreen(
                onNavigateToSchemeEditor = { schemeId ->
                    val route = if (schemeId != null) {
                        "scheme_editor/$schemeId"
                    } else {
                        "scheme_editor"
                    }
                    navController.navigate(route)
                }
            )
        }

        // Редактор схем
        composable("scheme_editor/{schemeId}") { backStackEntry ->
            val schemeId = backStackEntry.arguments?.getString("schemeId")?.toIntOrNull()
            SchemeEditorScreen(
                schemeId = schemeId,
                onNavigateBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }

        // Отчеты
        composable(BottomNavItem.Reports.route) {
            ReportsScreen()
        }

        // Фото (общая галерея)
        composable(BottomNavItem.Photos.route) {
            PhotosScreen(
                onNavigateToFullScreenPhoto = { photoPath, device ->
                    val fileName = Uri.decode(photoPath.substringAfterLast("/"))
                    val photoIndex = device.photos.indexOfFirst { it == fileName }
                    if (photoIndex != -1) {
                        navController.navigate("fullscreen_photo/${device.id}/$photoIndex")
                    }
                },
                updateBottomNavVisibility = updateBottomNavVisibility,
                viewModel = photosViewModel,
                topAppBarController = topAppBarController
            )
        }

        // Полноэкранный просмотр фото
        composable(
            route = "fullscreen_photo/{deviceId}/{photoIndex}",
            arguments = listOf(
                navArgument("deviceId") {
                    type = NavType.IntType
                    nullable = false  // Явно укажите, что НЕ может быть null
                },
                navArgument("photoIndex") {
                    type = NavType.IntType
                    nullable = false
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getInt("deviceId") ?: 0
            val photoIndex = backStackEntry.arguments?.getInt("photoIndex") ?: 0

            // ✅ Используем отдельный экран, а не функцию в NavHost
            FullScreenPhotoContainer(
                deviceId = deviceId,
                photoIndex = photoIndex,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Настройки
        composable("settings") {
            SettingsScreen(
                navController = navController,
            )
        }
    }
}