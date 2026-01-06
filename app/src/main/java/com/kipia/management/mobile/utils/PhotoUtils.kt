package com.kipia.management.mobile.utils

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun rememberPhotoState(
    scope: CoroutineScope,
    snackbarHostState: androidx.compose.material3.SnackbarHostState
): PhotoState {
    val context = LocalContext.current

    val permissionLauncher = PhotoManager.rememberPermissionLauncher(
        onPermissionGranted = { /* Можно показать сообщение */ },
        onPermissionDenied = {
            scope.launch {
                snackbarHostState.showSnackbar("Нужны разрешения для работы с фото")
            }
        }
    )

    val pickPhotoLauncher = PhotoManager.rememberPickPhotoLauncher(
        onPhotoSelected = { uri ->
            scope.launch {
                val savedPath = PhotoManager.savePhotoFromUri(context, uri)
                savedPath?.let { path ->
                    // Обработка сохраненного фото
                    snackbarHostState.showSnackbar("Фото сохранено")
                } ?: run {
                    snackbarHostState.showSnackbar("Ошибка сохранения фото")
                }
            }
        }
    )

    val (takePhotoLauncher, setPhotoUri) = PhotoManager.rememberTakePhotoLauncher(
        onPhotoTaken = { uri ->
            scope.launch {
                val savedPath = PhotoManager.savePhotoFromUri(context, uri)
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
            permissionLauncher = permissionLauncher,
            pickPhotoLauncher = pickPhotoLauncher,
            takePhotoLauncher = takePhotoLauncher,
            setPhotoUri = setPhotoUri
        )
    }
}

data class PhotoState(
    val permissionLauncher: (Array<String>) -> Unit,
    val pickPhotoLauncher: () -> Unit,
    val takePhotoLauncher: () -> Unit,
    val setPhotoUri: (android.net.Uri?) -> Unit
)