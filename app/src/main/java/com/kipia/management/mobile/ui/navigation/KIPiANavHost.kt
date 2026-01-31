package com.kipia.management.mobile.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.ui.screens.devices.DeviceDetailScreen
import com.kipia.management.mobile.ui.screens.devices.DeviceEditScreen
import com.kipia.management.mobile.ui.screens.devices.DevicesScreen
import com.kipia.management.mobile.ui.screens.photos.FullScreenPhotoScreen
import com.kipia.management.mobile.ui.screens.photos.PhotosScreen
import com.kipia.management.mobile.ui.screens.reports.ReportsScreen
import com.kipia.management.mobile.ui.screens.schemes.SchemeEditorScreen
import com.kipia.management.mobile.ui.screens.schemes.SchemesScreen
import com.kipia.management.mobile.ui.screens.settings.SettingsScreen
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.viewmodel.DevicesViewModel
import com.kipia.management.mobile.viewmodel.PhotoDetailViewModel

/**
 * Навигационный хост
 */
@Composable
fun KIPiANavHost(
    navController: NavHostController = rememberNavController(),
    devicesViewModel: DevicesViewModel,
    topAppBarController: TopAppBarController,
    notificationManager: NotificationManager, // ★★★★ ДОБАВЛЕНО ★★★★
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
        // Экран устройств — ✅
        composable(BottomNavItem.Devices.route) {
            DevicesScreen(
                // ★★★★ ПЕРЕДАЕМ ФУНКЦИЮ ★★★★
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
                notificationManager = notificationManager // ★★★★ ДОБАВЛЕНО ★★★★
            )
        }

        // Детальный экран устройства — ✅
        composable("device_detail/{deviceId}") { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId")?.toIntOrNull()
            if (deviceId != null) {
                DeviceDetailScreen(
                    deviceId = deviceId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { navController.navigate("device_edit/$deviceId") }
                    // Убираем updateBottomNavVisibility
                )
            }
        }

        // Экран редактирования прибора — ✅
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
                topAppBarController = topAppBarController, // Оставляем только это
                viewModel = hiltViewModel(),
                notificationManager = notificationManager // ★★★★ ДОБАВЛЕНО ★★★★
            )
        }

        // Экран создания прибора — ✅
        composable("device_edit") {
            DeviceEditScreen(
                deviceId = null,
                onNavigateBack = { navController.popBackStack() },
                topAppBarController = topAppBarController, // Оставляем только это
                viewModel = hiltViewModel(),
                notificationManager = notificationManager // ★★★★ ДОБАВЛЕНО ★★★★
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

        // Фото
        composable(BottomNavItem.Photos.route) {
            PhotosScreen(
                onNavigateToFullScreenPhoto = { photoPath, deviceName ->
                    navController.navigate("fullscreen_photo/${Uri.encode(photoPath)}?deviceName=${Uri.encode(deviceName)}")
                }
            )
        }

        // Полноэкранный просмотр фото
        composable("fullscreen_photo/{photoPath}") { backStackEntry ->
            val photoPath = Uri.decode(backStackEntry.arguments?.getString("photoPath") ?: "")
            val deviceName = Uri.decode(backStackEntry.arguments?.getString("deviceName") ?: "Прибор")
            val photoDetailViewModel: PhotoDetailViewModel = hiltViewModel() // ← Правильно — отдельный ViewModel
            FullScreenPhotoScreen(
                photoPath = photoPath,
                deviceName = deviceName,
                onNavigateBack = { navController.popBackStack() },
                onRotateLeft = {
                    photoDetailViewModel.rotatePhoto(photoPath, -90f)
                },
                onRotateRight = {
                    photoDetailViewModel.rotatePhoto(photoPath, 90f)
                },
                onDelete = {
                    photoDetailViewModel.deletePhoto(photoPath)
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                navController = navController,
            )
        }
    }
}