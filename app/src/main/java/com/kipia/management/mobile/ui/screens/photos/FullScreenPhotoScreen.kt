package com.kipia.management.mobile.ui.screens.photos

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.viewmodel.PhotoDetailViewModel
import java.io.File
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPhotoScreen(
    photoPath: String,
    device: Device,
    onNavigateBack: () -> Unit,
    viewModel: PhotoDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPhotoPath = uiState.currentPhotoPath ?: photoPath

    // ✅ ДОБАВЛЯЕМ: CoroutineScope для вызова suspend функций
    val coroutineScope = rememberCoroutineScope()

    // Определяем имя файла из пути
    val fileName = remember(photoPath) {
        File(photoPath).name
    }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Сохраняем устройство во ViewModel
    LaunchedEffect(device) {
        viewModel.setCurrentDevice(device)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        // Имя устройства
                        Text(
                            device.getDisplayName(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium
                        )
                        // ★ ДОБАВЛЯЕМ: Имя файла
                        Text(
                            fileName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                },
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(24.dp)
                                .padding(8.dp),
                            strokeWidth = 2.dp
                        )
                    }

                    IconButton(
                        onClick = {
                            viewModel.rotatePhoto(currentPhotoPath, -90f)
                        }
                    ) {
                        Icon(Icons.Default.RotateLeft, contentDescription = "Повернуть влево")
                    }

                    IconButton(
                        onClick = {
                            viewModel.rotatePhoto(currentPhotoPath, 90f)
                        }
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Повернуть вправо")
                    }

                    IconButton(
                        onClick = {
                            // ✅ ИСПРАВЛЕНО: используем coroutineScope
                            coroutineScope.launch {
                                val success = viewModel.deletePhoto(fileName)
                                if (success) {
                                    onNavigateBack()
                                }
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
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
                        detectTransformGestures(
                            onGesture = { centroid, pan, gestureZoom, _ ->
                                scale *= gestureZoom
                                offsetX += pan.x
                                offsetY += pan.y
                            }
                        )
                    }
            )

            // Кнопка сброса масштаба
            if (scale != 1f || offsetX != 0f || offsetY != 0f) {
                FloatingActionButton(
                    onClick = {
                        scale = 1f
                        offsetX = 0f
                        offsetY = 0f
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = "Сбросить")
                }
            }
        }
    }
}