package com.kipia.management.mobile.ui.screens.photos

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.component1
import androidx.core.graphics.component2
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.kipia.management.mobile.R
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.viewmodel.PhotosViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    onNavigateToFullScreenPhoto: (String, String) -> Unit, // photoPath, deviceName
    viewModel: PhotosViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фотографии") },
                actions = {
                    // Фильтр по устройству
                    DeviceFilterDropdown(
                        devices = devices,
                        selectedDeviceId = uiState.selectedDeviceId,
                        onDeviceSelected = { deviceId ->
                            viewModel.selectDevice(deviceId)
                        }
                    )

                    // Переключение вида (сетка/список)
                    IconButton(
                        onClick = { viewModel.toggleViewMode() }
                    ) {
                        Icon(
                            if (uiState.isGridView) Icons.Default.GridView else Icons.Default.ViewList,
                            contentDescription = if (uiState.isGridView) "Список" else "Сетка"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingState()
            }
            uiState.error != null -> {
                ErrorState(
                    error = uiState.error,
                    onRetry = { viewModel.loadPhotos() },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            photos.isEmpty() -> {
                EmptyPhotosState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                PhotosGallery(
                    photos = photos,
                    viewMode = uiState.viewMode,
                    selectedDeviceId = uiState.selectedDeviceId,
                    onPhotoClick = { photo, device ->
                        onNavigateToFullScreenPhoto(photo, device.getDisplayName())
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun DeviceFilterDropdown(
    devices: List<Device>,
    selectedDeviceId: Int?,
    onDeviceSelected: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FilterAlt, contentDescription = "Фильтр по прибору")

                if (selectedDeviceId != null) {
                    Badge(
                        modifier = Modifier
                            .offset(x = (-8).dp, y = (-8).dp)
                            .size(8.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Все приборы") },
                onClick = {
                    onDeviceSelected(null)
                    expanded = false
                },
                leadingIcon = {
                    Icon(Icons.Default.AllInclusive, contentDescription = null)
                }
            )

            Divider()

            devices.forEach { device ->
                DropdownMenuItem(
                    text = {
                        Text(
                            device.getDisplayName(),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onDeviceSelected(device.id)
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Devices, contentDescription = null)
                    }
                )
            }
        }
    }
}

@Composable
fun PhotosGallery(
    photos: List<Pair<String, Device>>,
    viewMode: ViewMode,
    selectedDeviceId: Int?,
    onPhotoClick: (String, Device) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredPhotos = if (selectedDeviceId != null) {
        photos.filter { (_, device) -> device.id == selectedDeviceId }
    } else {
        photos
    }

    Column(modifier = modifier) {
        // Статистика
        PhotosStats(
            totalPhotos = photos.size,
            filteredPhotos = filteredPhotos.size,
            selectedDeviceId = selectedDeviceId,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Галерея
        when (viewMode) {
            ViewMode.GRID -> {
                PhotosGrid(
                    photos = filteredPhotos,
                    onPhotoClick = onPhotoClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            ViewMode.LIST -> {
                PhotosList(
                    photos = filteredPhotos,
                    onPhotoClick = onPhotoClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PhotosGrid(
    photos: List<Pair<String, Device>>,
    onPhotoClick: (String, Device) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 120.dp),
        modifier = modifier,
        verticalItemSpacing = 4.dp,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(photos, key = { (photoPath, _) -> photoPath }) { (photoPath, device) ->
            PhotoGridItem(
                photoPath = photoPath,
                device = device,
                onClick = { onPhotoClick(photoPath, device) }
            )
        }
    }
}

@Composable
fun PhotoGridItem(
    photoPath: String,
    device: Device,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(model = photoPath),
                contentDescription = "Фото прибора",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Накладка с информацией об устройстве
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )

            // Информация об устройстве
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
            ) {
                Text(
                    text = device.getDisplayName(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Text(
                    text = device.inventoryNumber,
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PhotosList(
    photos: List<Pair<String, Device>>,
    onPhotoClick: (String, Device) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(photos, key = { (photoPath, _) -> photoPath }) { (photoPath, device) ->
            PhotoListItem(
                photoPath = photoPath,
                device = device,
                onClick = { onPhotoClick(photoPath, device) }
            )
        }
    }
}

@Composable
fun PhotoListItem(
    photoPath: String,
    device: Device,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
        ) {
            // Миниатюра фото
            Image(
                painter = rememberAsyncImagePainter(model = photoPath),
                contentDescription = "Фото прибора",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )

            // Информация об устройстве
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = device.getDisplayName(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Инв. №: ${device.inventoryNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Место: ${device.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Статус
                StatusChip(status = device.status)
            }

            // Иконка перехода
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Просмотр",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(end = 16.dp)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "В работе" -> Pair(Color(0xFFE8F5E9), Color(0xFF2E7D32))
        "На ремонте" -> Pair(Color(0xFFFFF3E0), Color(0xFFEF6C00))
        "Списан" -> Pair(Color(0xFFFFEBEE), Color(0xFFC62828))
        "В резерве" -> Pair(Color(0xFFF5F5F5), Color(0xFF616161))
        else -> Pair(Color(0xFFF5F5F5), Color(0xFF616161))
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun PhotosStats(
    totalPhotos: Int,
    filteredPhotos: Int,
    selectedDeviceId: Int?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (selectedDeviceId == null) {
                        "Все фото: $totalPhotos"
                    } else {
                        "Отфильтровано: $filteredPhotos из $totalPhotos"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )

                if (selectedDeviceId != null) {
                    Text(
                        text = "Показаны фото только выбранного прибора",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selectedDeviceId != null) {
                AssistChip(
                    onClick = { /* Фильтр сбросится через ViewModel */ },
                    label = { Text("Сбросить фильтр") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Загрузка фотографий...")
        }
    }
}

@Composable
fun ErrorState(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = "Ошибка",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Ошибка загрузки",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Text("Повторить")
        }
    }
}

@Composable
fun EmptyPhotosState(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.PhotoLibrary,
            contentDescription = "Нет фото",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Нет фотографий",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Добавьте фотографии к приборам,\nчтобы они появились здесь",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

enum class ViewMode {
    GRID, LIST
}