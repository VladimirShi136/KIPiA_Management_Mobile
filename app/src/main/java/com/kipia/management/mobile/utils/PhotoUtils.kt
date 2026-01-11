package com.kipia.management.mobile.utils

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberPhotoState(
    photoManager: PhotoManager,
    scope: CoroutineScope,
    snackbarHostState: androidx.compose.material3.SnackbarHostState
): PhotoState {
    val context = LocalContext.current

    // 1. Лаунчер для разрешений
    val permissionLauncher = PhotoManagerCompose.rememberPermissionLauncher(
        onPermissionGranted = { /* Можно показать сообщение */ },
        onPermissionDenied = {
            scope.launch {
                snackbarHostState.showSnackbar("Нужны разрешения для работы с фото")
            }
        }
    )

    // 2. Лаунчер для выбора фото из галереи
    val pickPhotoLauncher = PhotoManagerCompose.rememberPickPhotoLauncher(
        onPhotoSelected = { uri ->
            scope.launch {
                val savedPath = photoManager.savePhotoFromUri(uri)
                savedPath?.let { path ->
                    snackbarHostState.showSnackbar("Фото сохранено")
                } ?: run {
                    snackbarHostState.showSnackbar("Ошибка сохранения фото")
                }
            }
        }
    )

    // 3. Лаунчер для съемки фото
    val (takePhotoLauncher, setPhotoUri) = PhotoManagerCompose.rememberTakePhotoLauncher(
        photoManager = photoManager,
        onPhotoTaken = { uri ->
            scope.launch {
                val savedPath = photoManager.savePhotoFromUri(uri)
                savedPath?.let { path ->
                    snackbarHostState.showSnackbar("Фото сохранено")
                } ?: run {
                    snackbarHostState.showSnackbar("Ошибка сохранения фото")
                }
            }
        }
    )

    return remember(scope, snackbarHostState) {
        PhotoState(
            permissionLauncher = { permissions ->
                permissionLauncher.launch(permissions)
            },
            pickPhotoLauncher = {
                pickPhotoLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            takePhotoLauncher = takePhotoLauncher,
            setPhotoUri = setPhotoUri
        )
    }
}

data class PhotoState(
    val permissionLauncher: (Array<String>) -> Unit,
    val pickPhotoLauncher: () -> Unit,
    val takePhotoLauncher: () -> Unit,
    val setPhotoUri: (android.net.Uri) -> Unit
)