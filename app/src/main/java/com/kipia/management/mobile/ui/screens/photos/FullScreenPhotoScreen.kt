package com.kipia.management.mobile.ui.screens.photos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.components.dialogs.DeleteConfirmDialog
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.viewmodel.PhotoDetailViewModel
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun FullScreenPhotoScreen(
    photoPath: String,
    device: Device,
    onNavigateBack: () -> Unit,
    viewModel: PhotoDetailViewModel,
    topAppBarController: TopAppBarController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPhotoPath = uiState.currentPhotoPath ?: photoPath
    val coroutineScope = rememberCoroutineScope()

    val fileName = remember(photoPath) { File(photoPath).name }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isTransformed = scale != 1f || offsetX != 0f || offsetY != 0f

    LaunchedEffect(device) {
        viewModel.setCurrentDevice(device)
    }

    // Передаём onDeletePhotoClick как открытие диалога — сам диалог живёт здесь
    LaunchedEffect(device, fileName, currentPhotoPath, uiState.rotationDegrees) {
        topAppBarController.setForScreen(
            "fullscreen_photo",
            mapOf(
                "inventoryNumber" to device.inventoryNumber,
                "valveNumber" to (device.valveNumber ?: ""),
                "photoFileName" to fileName,
                "photoFilePath" to currentPhotoPath,
                "onBackClick" to onNavigateBack,
                "onDeletePhotoClick" to { showDeleteDialog = true }  // ← только открываем диалог
            )
        )
    }

    // Диалог удаления фото — общий стиль
    if (showDeleteDialog) {
        DeleteConfirmDialog(
            title = "Удалить фото?",
            itemName = fileName,
            message = "Файл будет удалён с устройства без возможности восстановления.",
            onConfirm = {
                showDeleteDialog = false
                coroutineScope.launch {
                    val success = viewModel.deletePhoto(fileName)
                    if (success) onNavigateBack()
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = rememberAsyncImagePainter(model = currentPhotoPath),
            contentDescription = "Фото прибора",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = uiState.rotationDegrees
                    translationX = offsetX
                    translationY = offsetY
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        scale = (scale * gestureZoom).coerceIn(0.5f, 5f)
                        offsetX += pan.x
                        offsetY += pan.y
                    }
                }
        )

        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(24.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = { viewModel.rotatePhoto(currentPhotoPath, -90f) },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Повернуть влево")
            }

            if (isTransformed) {
                FloatingActionButton(
                    onClick = { scale = 1f; offsetX = 0f; offsetY = 0f },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Сбросить")
                }
            } else {
                Spacer(modifier = Modifier.size(56.dp))
            }

            FloatingActionButton(
                onClick = { viewModel.rotatePhoto(currentPhotoPath, 90f) },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Повернуть вправо")
            }
        }
    }
}