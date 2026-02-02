package com.kipia.management.mobile.ui.screens.photos

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.viewmodel.DeviceDetailViewModel
import com.kipia.management.mobile.viewmodel.PhotoDetailViewModel
import kotlinx.coroutines.delay

/**
 * Контейнер для полноэкранного просмотра фото
 * Загружает устройство и передает данные в FullScreenPhotoScreen
 */
@Composable
fun FullScreenPhotoContainer(
    deviceId: Int,
    photoIndex: Int,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceDetailViewModel: DeviceDetailViewModel = hiltViewModel()
    val photoDetailViewModel: PhotoDetailViewModel = hiltViewModel()

    val device by deviceDetailViewModel.device.collectAsStateWithLifecycle()
    val photos by deviceDetailViewModel.photos.collectAsStateWithLifecycle()

    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(deviceId) {
        try {
            deviceDetailViewModel.loadDevice(deviceId)
            delay(300)
            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    FullScreenPhotoContent(
        device = device,
        photos = photos,
        photoIndex = photoIndex,
        isLoading = isLoading,
        error = error,
        photoDetailViewModel = photoDetailViewModel,
        onNavigateBack = onNavigateBack,
        onRetry = { deviceDetailViewModel.loadDevice(deviceId) },
        modifier = modifier
    )
}