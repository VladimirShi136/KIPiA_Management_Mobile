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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.kipia.management.mobile.viewmodel.PhotoDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPhotoScreen(
    photoPath: String,
    deviceName: String,
    onNavigateBack: () -> Unit,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    onDelete: () -> Unit,
    viewModel: PhotoDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentPhotoPath = uiState.currentPhotoPath ?: photoPath

    // Обработка успешного удаления
    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) {
            onNavigateBack()
        }
    }

    // Обработка ошибок
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // TODO: Показать снекбар с ошибкой
            viewModel.clearError()
        }
    }

    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        deviceName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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

                    IconButton(onClick = onRotateLeft) {
                        Icon(Icons.Default.RotateLeft, contentDescription = "Повернуть влево")
                    }

                    IconButton(onClick = onRotateRight) {
                        Icon(Icons.Default.RotateRight, contentDescription = "Повернуть вправо")
                    }

                    IconButton(
                        onClick = onDelete,
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