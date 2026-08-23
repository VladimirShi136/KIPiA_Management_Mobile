package com.kipia.management.mobile.ui.screens.photos

import com.kipia.management.mobile.ui.components.photos.DisplayMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.kipia.management.mobile.ui.components.photos.PhotosActiveFiltersBadge
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.ui.components.stats.StatCard
import com.kipia.management.mobile.ui.components.stats.StatGroup
import com.kipia.management.mobile.ui.components.stats.StatItemData
import com.kipia.management.mobile.ui.components.dialogs.LoadingOverlay
import com.kipia.management.mobile.ui.theme.Dimens
import com.kipia.management.mobile.ui.theme.DeviceStatusColors
import com.kipia.management.mobile.viewmodel.LocationPhotoGroup
import com.kipia.management.mobile.viewmodel.PhotoStats
import com.kipia.management.mobile.viewmodel.PhotosViewModel
import kotlinx.coroutines.launch

/**
 * Экран галереи фото
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PhotosScreen(
    onNavigateToFullScreenPhoto: (String, Device) -> Unit,
    updateBottomNavVisibility: (Boolean) -> Unit = {},
    viewModel: PhotosViewModel = hiltViewModel(),
    topAppBarController: TopAppBarController? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val photos by viewModel.photos.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val allLocations by viewModel.allLocations.collectAsStateWithLifecycle()
    val groupedByLocation by viewModel.groupedByLocation.collectAsStateWithLifecycle()
    val totalStats by viewModel.totalStats.collectAsStateWithLifecycle()
    val filteredStats by viewModel.filteredStats.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Состояния скролла для каждого режима
    val groupedScrollState = rememberLazyListState()
    val gridScrollState = rememberLazyGridState()
    val listScrollState = rememberLazyListState()

    // Определяем, какой режим сейчас активен
    val isGroupedMode = uiState.displayMode == DisplayMode.GROUPED
    val isListViewMode = uiState.viewMode == ViewMode.LIST

    // ★ ОПРЕДЕЛЯЕМ видимость BottomNav
    val shouldShowBottomNav by remember(
        groupedScrollState,
        gridScrollState,
        listScrollState,
        isGroupedMode,
        isListViewMode,
        photos,
        groupedByLocation,
        uiState.isLoading
    ) {
        derivedStateOf {
            // Если идет загрузка или контента нет — навигация всегда видна
            if (uiState.isLoading) return@derivedStateOf true
            
            val isEmpty = if (isGroupedMode) groupedByLocation.isEmpty() else photos.isEmpty()
            if (isEmpty) return@derivedStateOf true

            when {
                isGroupedMode -> {
                    groupedScrollState.firstVisibleItemIndex == 0 && groupedScrollState.firstVisibleItemScrollOffset == 0
                }
                isListViewMode -> {
                    listScrollState.firstVisibleItemIndex == 0 && listScrollState.firstVisibleItemScrollOffset == 0
                }
                else -> {
                    val visibleItems = gridScrollState.layoutInfo.visibleItemsInfo
                    if (visibleItems.isEmpty()) {
                        true
                    } else {
                        val isFirstItemVisible = visibleItems.any { it.index == 0 }
                        val isFirstItemAtTop = visibleItems.firstOrNull { it.index == 0 }?.offset?.y == 0
                        isFirstItemVisible && isFirstItemAtTop
                    }
                }
            }
        }
    }

    // Запускаем индикацию при каждом входе
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    // При уходе с экрана взводим загрузку для следующего входа
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetLoadingState()
        }
    }

    LaunchedEffect(shouldShowBottomNav) {
        updateBottomNavVisibility(shouldShowBottomNav)
    }

    // Кнопка появляется только если есть контент И мы проскроллили вниз
    val showScrollToTopButton by remember(shouldShowBottomNav, photos, groupedByLocation, uiState.isLoading) {
        derivedStateOf { 
            !uiState.isLoading && !shouldShowBottomNav && (photos.isNotEmpty() || groupedByLocation.isNotEmpty())
        }
    }

    // ★ LaunchedEffect для TopAppBar
    LaunchedEffect(topAppBarController, uiState.isGridView, uiState.displayMode, allLocations, devices) {
        topAppBarController?.setForScreen("photos", buildMap {
            put("isGridView", uiState.isGridView)
            put("displayMode", uiState.displayMode)
            put("locations", allLocations)
            put("devices", devices)
            put("onLocationFilterChange", { location: String? -> viewModel.selectLocation(location) })
            put("onDeviceFilterChange", { deviceId: Int? -> viewModel.selectDevice(deviceId) })
            put("onResetAllFilters", { viewModel.resetAllFilters() })
            put("onViewModeClick", { viewModel.toggleViewMode() })
            put("onGroupModeClick", {
                val newMode = if (uiState.displayMode == DisplayMode.GROUPED) DisplayMode.FLAT else DisplayMode.GROUPED
                viewModel.updateDisplayMode(newMode)
            })
            put("onExpandAllClick", { viewModel.toggleAllGroups(true) })
            put("onCollapseAllClick", { viewModel.toggleAllGroups(false) })
        })
    }

    val scrollToTop: () -> Unit = {
        scope.launch {
            when {
                isGroupedMode -> groupedScrollState.animateScrollToItem(0)
                isListViewMode -> listScrollState.animateScrollToItem(0)
                else -> gridScrollState.animateScrollToItem(0)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.screenPadding)
        ) {
            // Статистика фото
            PhotoStatistics(
                totalStats = totalStats,
                filteredStats = filteredStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.spacingMedium)
            )

            // Активные фильтры
            PhotosActiveFiltersBadge(
                searchQuery = uiState.searchQuery,
                currentSort = uiState.sortBy,
                onClearFilters = { viewModel.resetAllFilters() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.spacingMedium)
            )

            // ГАЛЕРЕЯ
            Box(modifier = Modifier.weight(1f)) {
                when {
                    uiState.isLoading -> {
                        // Скрываем контент во время загрузки (аналогично отчетам)
                        Spacer(modifier = Modifier.fillMaxSize())
                    }

                    uiState.error != null -> {
                        PhotosErrorState(
                            error = uiState.error ?: "Неизвестная ошибка",
                            onRetry = { viewModel.loadPhotos() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    photos.isEmpty() && groupedByLocation.isEmpty() -> {
                        PhotosEmptyState(modifier = Modifier.fillMaxSize())
                    }

                    uiState.displayMode == DisplayMode.GROUPED -> {
                        GroupedPhotosGallery(
                            groups = groupedByLocation,
                            viewMode = uiState.viewMode,
                            scrollState = groupedScrollState,
                            onGroupToggle = { location -> viewModel.toggleLocationGroup(location) },
                            onPhotoClick = { photoItem ->
                                onNavigateToFullScreenPhoto(photoItem.fullPath, photoItem.device)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        PhotosGallery(
                            photos = photos,
                            viewMode = uiState.viewMode,
                            gridScrollState = gridScrollState,
                            listScrollState = listScrollState,
                            onPhotoClick = { photoItem ->
                                onNavigateToFullScreenPhoto(photoItem.fullPath, photoItem.device)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // КНОПКИ FAB
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 46.dp, bottom = 30.dp)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingFab)
        ) {
            AnimatedVisibility(
                visible = showScrollToTopButton,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = scrollToTop,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Наверх", modifier = Modifier.size(24.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingFab)) {
                FloatingActionButton(
                    onClick = { viewModel.toggleViewMode() },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        if (uiState.isGridView) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                        contentDescription = if (uiState.isGridView) "Список" else "Сетка",
                        modifier = Modifier.size(24.dp)
                    )
                }

                FloatingActionButton(
                    onClick = {
                        val newMode = if (uiState.displayMode == DisplayMode.GROUPED) DisplayMode.FLAT else DisplayMode.GROUPED
                        viewModel.updateDisplayMode(newMode)
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(
                        if (uiState.displayMode == DisplayMode.GROUPED) Icons.Default.ViewDay else Icons.Default.Folder,
                        contentDescription = if (uiState.displayMode == DisplayMode.GROUPED) "Плоский вид" else "Группировка по папкам",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Глобальный индикатор загрузки
        LoadingOverlay(
            isLoading = uiState.isLoading,
            text = "Загрузка фотографий..."
        )
    }
}

@Composable
fun GroupedPhotosGallery(
    groups: List<LocationPhotoGroup>,
    viewMode: ViewMode,
    scrollState: LazyListState,
    onGroupToggle: (String) -> Unit,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = scrollState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        items(groups, key = { it.location }) { group ->
            LocationGroupCard(
                group = group,
                viewMode = viewMode,
                onToggle = { onGroupToggle(group.location) },
                onPhotoClick = onPhotoClick
            )
        }
    }
}

@Composable
fun LocationGroupCard(
    group: LocationPhotoGroup,
    viewMode: ViewMode,
    onToggle: () -> Unit,
    onPhotoClick: (PhotoItem) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(Dimens.spacingLarge),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = group.location, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = "${group.photos.size} фото", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    imageVector = if (group.isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = group.isExpanded,
                enter = expandVertically(animationSpec = tween(durationMillis = 300)),
                exit = shrinkVertically(animationSpec = tween(durationMillis = 300))
            ) {
                Column(modifier = Modifier.padding(horizontal = Dimens.spacingLarge, vertical = Dimens.spacingMedium)) {
                    when (viewMode) {
                        ViewMode.GRID -> {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
                                items(group.photos, key = { it.fullPath }) { photoItem ->
                                    PhotoThumbnailCard(photoItem = photoItem, onClick = { onPhotoClick(photoItem) })
                                }
                            }
                        }
                        ViewMode.LIST -> {
                            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
                                group.photos.forEach { photoItem ->
                                    PhotoListItem(photoItem = photoItem, onClick = { onPhotoClick(photoItem) })
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoThumbnailCard(photoItem: PhotoItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.size(120.dp, 150.dp),
        shape = RoundedCornerShape(Dimens.thumbRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(model = photoItem.fullPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
            )
            Text(
                text = photoItem.fileName.substringBeforeLast("."),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
            )
        }
    }
}

@Composable
fun PhotosGallery(
    photos: List<PhotoItem>,
    viewMode: ViewMode,
    gridScrollState: LazyGridState,
    listScrollState: LazyListState,
    onPhotoClick: (PhotoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    when (viewMode) {
         ViewMode.GRID -> {
             LazyVerticalGrid(
                 state = gridScrollState,
                 columns = GridCells.Adaptive(minSize = 120.dp),
                 modifier = modifier,
                 verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                 horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall),
                 contentPadding = PaddingValues(bottom = 80.dp)
             ) {
                 items(photos, key = { it.fullPath }) { photoItem ->
                     PhotoGridItem(photoItem = photoItem, onClick = { onPhotoClick(photoItem) })
                 }
             }
         }
        ViewMode.LIST -> {
            LazyColumn(
                state = listScrollState,
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(photos, key = { it.fullPath }) { photoItem ->
                    PhotoListItem(photoItem = photoItem, onClick = { onPhotoClick(photoItem) })
                }
            }
        }
    }
}

@Composable
fun PhotoGridItem(photoItem: PhotoItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        shape = RoundedCornerShape(Dimens.thumbRadius)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = rememberAsyncImagePainter(model = photoItem.fullPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                    .padding(8.dp)
            ) {
                Text(text = photoItem.device.getDisplayName(), color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(text = photoItem.device.inventoryNumber, color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun PhotoListItem(photoItem: PhotoItem, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            Image(
                painter = rememberAsyncImagePainter(model = photoItem.fullPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(100.dp).fillMaxHeight().padding(8.dp)
            )
            Column(modifier = Modifier.padding(8.dp).weight(1f)) {
                Text(text = photoItem.device.getDisplayName(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(text = "Инв. №: ${photoItem.device.inventoryNumber}", style = MaterialTheme.typography.bodySmall)
                Text(text = "Место: ${photoItem.device.location}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                if (!photoItem.device.valveNumber.isNullOrBlank()) {
                    Text(text = "№ крана: ${photoItem.device.valveNumber}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun PhotoStatistics(totalStats: PhotoStats, filteredStats: PhotoStats, modifier: Modifier = Modifier) {
    val hasFilter = filteredStats.photos != totalStats.photos
    val totalGroup = StatGroup(
        title = if (hasFilter) "Всего:" else null,
        items = listOf(
            StatItemData(totalStats.locations, "мест", DeviceStatusColors.Total),
            StatItemData(totalStats.devices, "приборов", DeviceStatusColors.Working),
            StatItemData(totalStats.photos, "фото", DeviceStatusColors.Storage)
        )
    )
    val filteredGroup = StatGroup(
        title = "Выбрано:",
        items = listOf(
            StatItemData(filteredStats.locations, "мест", DeviceStatusColors.Total),
            StatItemData(filteredStats.devices, "приборов", DeviceStatusColors.Working),
            StatItemData(filteredStats.photos, "фото", DeviceStatusColors.Storage)
        )
    )
    StatCard(groups = if (hasFilter) listOf(totalGroup, filteredGroup) else listOf(totalGroup), modifier = modifier)
}

@Composable
fun PhotosErrorState(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
        Text(text = error, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        Button(onClick = onRetry) { Text("Повторить") }
    }
}

@Composable
fun PhotosEmptyState(modifier: Modifier = Modifier) {
    Box(modifier = modifier
        .fillMaxSize()
        .padding(Dimens.spacingXXLarge), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.PhotoLibrary,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSizeXXLarge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(Dimens.spacingLarge))
            Text(
                text = "Нет фотографий",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(Dimens.spacingMedium))
            Text(
                text = "Фотографии добавляются в карточке прибора.\nПерейдите в список приборов, выберите нужный и добавьте фото через форму.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class ViewMode { GRID, LIST }
