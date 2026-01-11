package com.kipia.management.mobile.utils

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*

// Оставляем как object для Compose функций
object PhotoManagerCompose {

    /**
     * Создает лаунчер для запроса разрешений в Compose
     */
    @Composable
    fun rememberPermissionLauncher(
        onPermissionGranted: () -> Unit = {},
        onPermissionDenied: () -> Unit = {}
    ) = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            onPermissionGranted()
        } else {
            onPermissionDenied()
        }
    }

    /**
     * Создает лаунчер для выбора фото из галереи
     */
    @Composable
    fun rememberPickPhotoLauncher(
        onPhotoSelected: (Uri) -> Unit
    ) = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let(onPhotoSelected)
    }

    /**
     * Создает лаунчер для съемки фото
     */
    @Composable
    fun rememberTakePhotoLauncher(
        photoManager: PhotoManager,
        onPhotoTaken: (Uri) -> Unit
    ): Pair<() -> Unit, (Uri) -> Unit> {
        var photoUri by remember { mutableStateOf<Uri?>(null) }

        val takePictureLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture()
        ) { success ->
            if (success && photoUri != null) {
                photoUri?.let(onPhotoTaken)
            }
        }

        val launchTakePhoto: () -> Unit = {
            photoManager.createImageFile()?.let { uri ->
                photoUri = uri
                takePictureLauncher.launch(uri)
            }
        }

        return Pair(launchTakePhoto, { uri -> photoUri = uri })
    }
}