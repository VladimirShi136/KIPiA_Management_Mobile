package com.kipia.management.mobile.ui.screens.photos

import com.kipia.management.mobile.ui.components.photos.DisplayMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.components.photos.PhotoItem
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.viewmodel.LocationPhotoGroup
import com.kipia.management.mobile.viewmodel.PhotosViewModel
import timber.log.Timber

/**
 * Экран галереи фото
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    onNavigateToFullScreenPhoto: (String, Device) -> Unit,
    viewModel: PhotosViewModel = hiltViewModel(),
    topAppBarController: TopAppBarController? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val allLocations by viewModel.allLocations.collectAsStateWithLifecycle()
    val groupedByLocation by viewModel.groupedByLocation.collectAsStateWithLifecycle()
    val expandedGroups by viewModel.expandedGroups.collectAsStateWithLifecycle()

    // ★ ОБНОВЛЯЕМ: Управление TopAppBar
    LaunchedEffect(topAppBarController, uiState.isGridView, uiState.displayMode) {
        topAppBarController?.setForScreen("photos", buildMap {
            put("isGridView", uiState.isGridView)
            put("displayMode", uiState.displayMode)
            put("locations", allLocations)
            put("devices", devices)
            put("onLocationFilterChange", { location: String? ->
                viewModel.selectLocation(location)
            })
            put("onDeviceFilterChange", { deviceId: Int? ->
                viewModel.selectDevice(deviceId)
            })
            put("onSortClick", {
                Timber.d("Сортировка фото")
            })
            put("onViewModeClick", {
                viewModel.toggleViewMode()
            })
            put("onGroupModeClick", {
                // ★ НОВАЯ КНОПКА: переключение режима отображения
                val newMode = if (uiState.displayMode == DisplayMode.GROUPED) {
                    DisplayMode.FLAT
                } else {
                    DisplayMode.GROUPED
                }
                viewModel.updateDisplayMode(newMode)
            })
            put("onExpandAllClick", {
                // ★ НОВАЯ КНОПКА: раскрыть все группы
                viewModel.toggleAllGroups(true)
            })
            put("onCollapseAllClick", {
                // ★ НОВАЯ КНОПКА: свернуть все группы
                viewModel.toggleAllGroups(false)
            })
        })
    }

    val photoItems = remember(photos) {
        photos.map { (fileName, fullPath, device) ->
            PhotoItem(
                fileName = fileName,
                fullPath = fullPath,
                device = device
            )
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                // ★ ОБНОВЛЯЕМ: Кнопки FAB
                // 1. Кнопка переключения вида (grid/list)
                FloatingActionButton(
                    onClick = {
                        viewModel.toggleViewMode()
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        if (uiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = if (uiState.isGridView) "Список" else "Сетка"
                    )
                }

                // 2. ★ НОВАЯ КНОПКА: Переключение режима группировки
                FloatingActionButton(
                    onClick = {
                        val newMode = if (uiState.displayMode == DisplayMode.GROUPED) {
                            DisplayMode.FLAT
                        } else {
                            DisplayMode.GROUPED
                        }
                        viewModel.updateDisplayMode(newMode)
                    },
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        if (uiState.displayMode == DisplayMode.GROUPED)
                            Icons.Default.ViewDay
                        else
                            Icons.Default.Folder,
                        contentDescription = if (uiState.displayMode == DisplayMode.GROUPED)
                            "Плоский вид"
                        else "Группировка по папкам"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ★ ГАЛЕРЕЯ С ГРУППИРОВКОЙ ★
            when {
                uiState.isLoading -> {
                    PhotosLoadingState()
                }

                uiState.error != null -> {
                    PhotosErrorState(
                        error = uiState.error ?: "Неизвестная ошибка",
                        onRetry = { viewModel.loadPhotos() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                groupedByLocation.isEmpty() && photos.isEmpty() -> {
                    PhotosEmptyState(
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.displayMode == DisplayMode.GROUPED -> {
                    // ★ НОВЫЙ РЕЖИМ: СГРУППИРОВАННЫЙ ПО ЛОКАЦИЯМ
                    GroupedPhotosGallery(
                        groups = groupedByLocation,
                        viewMode = uiState.viewMode,
                        onGroupToggle = { location ->
                            viewModel.toggleLocationGroup(location)
                        },
                        onPhotoClick = { photoItem ->
                            onNavigateToFullScreenPhoto(photoItem.fullPath, photoItem.device)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                else -> {
                    // ★ СТАРЫЙ РЕЖИМ: ПЛОСКИЙ СПИСОК (как было)
                    PhotosGallery(
                        photos = photos.map { (fileName, fullPath, device) ->
                            PhotoItem(
                                fileName = fileName,
                                fullPath = fullPath,
                                device = device
                            )
                        },
                        viewMode = uiState.viewMode,
                        selectedDeviceId = uiState.selectedDeviceId,
                        selectedLocation = uiState.selectedLocation,
                        onPhotoClick = { photoItem ->
                            onNavigateToFullScreenPhoto(photoItem.fullPath, photoItem.device)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

// ★ НОВЫЙ КОМПОЗАБЛ: Галерея с группировкой по локациям
@Composable
fun GroupedPhotosGallery(
    groups: List<LocationPhotoGroup>,
    viewMode: ViewMode,
    onGroupToggle: (String) -> Unit,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
    ) {
        groups.forEach { group ->
            item {
                LocationGroupCard(
                    group = group,
                    viewMode = viewMode,
                    onToggle = { onGroupToggle(group.location) },
                    onPhotoClick = onPhotoClick
                )
            }
        }
    }
}

// ★ НОВЫЙ КОМПОЗАБЛ: Карточка группы (локации)
@Composable
fun LocationGroupCard(
    group: LocationPhotoGroup,
    viewMode: ViewMode,
    onToggle: () -> Unit,
    onPhotoClick: (PhotoItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // ★ ЗАГОЛОВОК ГРУППЫ (кликабельный)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = group.location,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${group.photos.size} фото",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Icon(
                    imageVector = if (group.isExpanded)
                        Icons.Filled.ExpandLess
                    else
                        Icons.Filled.ExpandMore,
                    contentDescription = if (group.isExpanded)
                        "Свернуть"
                    else "Развернуть",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ★ ФОТОГРАФИИ В ГРУППЕ (раскрывающаяся часть)
            AnimatedVisibility(
                visible = group.isExpanded,
                enter = expandVertically(
                    animationSpec = tween(durationMillis = 300)
                ),
                exit = shrinkVertically(
                    animationSpec = tween(durationMillis = 300)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    when (viewMode) {
                        ViewMode.GRID -> {
                            // Сетка внутри группы
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(group.photos) { photoItem ->
                                    PhotoThumbnailCard(
                                        photoItem = photoItem,
                                        onClick = { onPhotoClick(photoItem) }
                                    )
                                }
                            }
                        }

                        ViewMode.LIST -> {
                            // Список внутри группы
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                group.photos.forEach { photoItem ->
                                    PhotoListItem(
                                        photoItem = photoItem,
                                        onClick = { onPhotoClick(photoItem) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ★ НОВЫЙ КОМПОЗАБЛ: Миниатюрная карточка фото для горизонтального ряда
@Composable
fun PhotoThumbnailCard(
    photoItem: PhotoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .width(120.dp)
            .height(150.dp),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(model = photoItem.fullPath),
                contentDescription = "Фото прибора",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Накладка с информацией
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                        )
                    )
            )

            // Имя файла
            Text(
                text = photoItem.fileName.substringBeforeLast("."),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 6.dp, bottom = 6.dp)
            )
        }
    }
}

@Composable
fun PhotosGallery(
    photos: List<PhotoItem>,
    viewMode: ViewMode,
    selectedDeviceId: Int?,
    selectedLocation: String?,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {

    // Фильтрация по устройству И местоположению
    val filteredPhotos = remember(photos, selectedDeviceId, selectedLocation) {
        photos.filter { photoItem ->
            (selectedDeviceId == null || photoItem.device.id == selectedDeviceId) &&
                    (selectedLocation == null || photoItem.device.location == selectedLocation)
        }
    }

    Column(modifier = modifier) {
        // ★ ИСПРАВЛЕННЫЙ ВЫЗОВ: добавьте selectedLocation = null
        PhotosStats(
            totalPhotos = photos.size,
            filteredPhotos = filteredPhotos.size,
            selectedLocation = selectedLocation,
            selectedDeviceId = selectedDeviceId,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp)
        )

        when (viewMode) {
            ViewMode.GRID -> {
                PhotosGrid(
                    photos = filteredPhotos,
                    onPhotoClick = onPhotoClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
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
    photos: List<PhotoItem>, // ✅ ИЗМЕНЕНО
    onPhotoClick: (PhotoItem) -> Unit, // ✅ ИЗМЕНЕНО
    modifier: Modifier = Modifier
) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 120.dp),
        modifier = modifier,
        verticalItemSpacing = 4.dp,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        items(photos.size, key = { index -> photos[index].fileName }) { index ->
            val photoItem = photos[index]
            PhotoGridItem(
                photoItem = photoItem,
                onClick = { onPhotoClick(photoItem) }
            )
        }
    }
}

@Composable
fun PhotoGridItem(
    photoItem: PhotoItem, // ✅ ИЗМЕНЕНО
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
                painter = rememberAsyncImagePainter(model = photoItem.fullPath),
                contentDescription = "Фото прибора",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Накладка с информацией
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
                    text = photoItem.device.getDisplayName(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Text(
                    text = photoItem.device.inventoryNumber,
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
    photos: List<PhotoItem>, // ✅ ИЗМЕНЕНО
    onPhotoClick: (PhotoItem) -> Unit, // ✅ ИЗМЕНЕНО
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(photos.size, key = { index -> photos[index].fileName }) { index ->
            val photoItem = photos[index]
            PhotoListItem(
                photoItem = photoItem,
                onClick = { onPhotoClick(photoItem) }
            )
        }
    }
}

@Composable
fun PhotoListItem(
    photoItem: PhotoItem, // ✅ ИЗМЕНЕНО
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
                .height(120.dp)
        ) {
            // Миниатюра фото
            Image(
                painter = rememberAsyncImagePainter(model = photoItem.fullPath),
                contentDescription = "Фото прибора",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(110.dp)
                    //.height(110.dp)
                    .fillMaxHeight()
                    //.clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                    .padding(15.dp)
            )

            // Информация об устройстве
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = photoItem.device.getDisplayName(),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Инв. №: ${photoItem.device.inventoryNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Место: ${photoItem.device.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
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
fun PhotosStats(
    totalPhotos: Int,
    filteredPhotos: Int,
    selectedLocation: String?,
    selectedDeviceId: Int?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Фотографии: $filteredPhotos из $totalPhotos",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (selectedLocation != null || selectedDeviceId != null) {
                Text(
                    text = buildString {
                        if (selectedLocation != null) {
                            append("Место: $selectedLocation")
                        }
                        if (selectedDeviceId != null && selectedLocation != null) {
                            append(", ")
                        }
                        if (selectedDeviceId != null) {
                            append("Прибор: #$selectedDeviceId")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Все фото",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun PhotosLoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
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
fun PhotosErrorState(
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
fun PhotosEmptyState(
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