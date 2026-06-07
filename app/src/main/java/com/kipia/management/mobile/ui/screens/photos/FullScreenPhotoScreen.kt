package com.kipia.management.mobile.ui.screens.photos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.kipia.management.mobile.ui.theme.Dimens
import com.kipia.management.mobile.ui.components.dialogs.DeleteConfirmDialog
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.viewmodel.PhotoDetailViewModel
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun FullScreenPhotoScreen(
    initialPhotoPath: String,
    photos: List<String>,
    initialIndex: Int,
    device: Device,
    onNavigateBack: () -> Unit,
    viewModel: PhotoDetailViewModel,
    topAppBarController: TopAppBarController
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var currentIndex by remember { mutableIntStateOf(initialIndex) }
    
    val currentPhotoPath = uiState.currentPhotoPath ?: photos[currentIndex]
    val coroutineScope = rememberCoroutineScope()

    val fileName = remember(currentPhotoPath) { File(currentPhotoPath).name }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isTransformed = scale != 1f || offsetX != 0f || offsetY != 0f

    LaunchedEffect(device) {
        viewModel.setCurrentDevice(device)
    }

    // Сбрасываем трансформации и состояние во ViewModel при смене фото
    LaunchedEffect(currentIndex) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
        viewModel.resetPhotoState(photos[currentIndex])
    }

    LaunchedEffect(device, fileName, currentPhotoPath, uiState.rotationDegrees) {
        topAppBarController.setForScreen(
            "fullscreen_photo",
            mapOf(
                "inventoryNumber" to device.inventoryNumber,
                "valveNumber" to (device.valveNumber ?: ""),
                "photoFileName" to fileName,
                "photoFilePath" to currentPhotoPath,
                "onBackClick" to onNavigateBack,
                "onDeletePhotoClick" to { showDeleteDialog = true }
            )
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmDialog(
            title = "Удалить фото?",
            itemName = fileName,
            message = "Файл будет удалён с устройства без возможности восстановления.",
            onConfirm = {
                showDeleteDialog = false
                coroutineScope.launch {
                    val success = viewModel.deletePhoto(fileName)
                    if (success) {
                        // После удаления возвращаемся назад, так как список photos в этом компоненте неизменяемый
                        // и требует обновления через DeviceDetailViewModel
                        onNavigateBack()
                    }
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
        // Основное изображение
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
                .pointerInput(currentIndex) {
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
                    .padding(Dimens.spacingLarge)
                    .size(Dimens.iconSizeMedium),
                strokeWidth = 2.dp,
                color = Color.White
            )
        }

        // Кнопки навигации (плавающие по бокам, в стиле FAB)
        if (photos.size > 1) {
            // Кнопка ВЛЕВО
            if (currentIndex > 0) {
                FloatingActionButton(
                    onClick = { currentIndex-- },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 16.dp)
                        .size(48.dp),
                    elevation = FloatingActionButtonDefaults.loweredElevation()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Предыдущее фото")
                }
            }

            // Кнопка ВПРАВО
            if (currentIndex < photos.size - 1) {
                FloatingActionButton(
                    onClick = { currentIndex++ },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(48.dp),
                    elevation = FloatingActionButtonDefaults.loweredElevation()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Следующее фото")
                }
            }
            
            // Индикатор текущей позиции (сверху по центру)
            Surface(
                color = Color.Black.copy(alpha = 0.4f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "${currentIndex + 1} / ${photos.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Нижняя панель управления (поворот и сброс)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = Dimens.fabBottomPadding),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingLarge),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = { viewModel.rotatePhoto(currentPhotoPath, -90f) },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(Dimens.fabSize)
            ) {
                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = "Повернуть влево")
            }

            // Кнопка сброса масштаба (появляется только при изменениях)
            if (isTransformed) {
                FloatingActionButton(
                    onClick = { 
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(Dimens.fabSizeLarge)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Сбросить масштаб")
                }
            } else {
                // Пустое место для сохранения симметрии
                Spacer(modifier = Modifier.size(Dimens.fabSizeLarge))
            }

            FloatingActionButton(
                onClick = { viewModel.rotatePhoto(currentPhotoPath, 90f) },
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(Dimens.fabSize)
            ) {
                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = "Повернуть вправо")
            }
        }
    }
}