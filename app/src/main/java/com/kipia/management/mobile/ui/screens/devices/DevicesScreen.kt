package com.kipia.management.mobile.ui.screens.devices

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.ui.components.dialogs.DeleteConfirmDialog
import com.kipia.management.mobile.ui.components.dialogs.DeviceDeleteWithSchemeDialog
import com.kipia.management.mobile.ui.components.dialogs.DeviceDeleteConfirmDialog
import com.kipia.management.mobile.ui.components.dialogs.LoadingOverlay
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.ui.theme.DeviceStatus
import com.kipia.management.mobile.ui.theme.Dimens
import com.kipia.management.mobile.ui.theme.DeviceStatusColors
import com.kipia.management.mobile.viewmodel.DeviceDeleteViewModel
import com.kipia.management.mobile.viewmodel.DeviceStats
import com.kipia.management.mobile.ui.components.stats.StatCard
import com.kipia.management.mobile.ui.components.stats.StatGroup
import com.kipia.management.mobile.ui.components.stats.StatItemData
import com.kipia.management.mobile.viewmodel.DevicesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun DevicesScreen(
    updateBottomNavVisibility: (Boolean) -> Unit = {},
    onNavigateToDeviceDetail: (Int) -> Unit,
    onNavigateToDeviceEdit: (Int?) -> Unit,
    viewModel: DevicesViewModel,
    deleteViewModel: DeviceDeleteViewModel = hiltViewModel(),
    notificationManager: NotificationManager
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val deleteDialogData by deleteViewModel.showDeleteDialog.collectAsStateWithLifecycle()
    val verticalScrollState = rememberLazyListState()

    // Запускаем индикацию при каждом входе на экран
    LaunchedEffect(Unit) {
        viewModel.refreshData()
    }

    // При уходе с экрана взводим загрузку для следующего входа
    DisposableEffect(Unit) {
        onDispose {
            viewModel.resetLoadingState()
        }
    }

    // ── Видимость BottomNav ───────────────────────────────────────────────────

    val shouldShowBottomNav by remember {
        derivedStateOf {
            verticalScrollState.firstVisibleItemIndex == 0 &&
                    verticalScrollState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(shouldShowBottomNav) {
        updateBottomNavVisibility(shouldShowBottomNav)
    }

    val showScrollToTopButton = !shouldShowBottomNav

    // ── Удаление ─────────────────────────────────────────────────────────────

    val deleteDeviceAction: (Device) -> Unit = { device ->
        scope.launch { deleteViewModel.checkAndShowDialog(device) }
    }

    // ── UI ───────────────────────────────────────────────────────────────────

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.screenPadding)
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .add(WindowInsets(bottom = 0.dp))
                )
        ) {
            // Статистика
            DeviceStatistics(
                stats = stats,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Dimens.spacingMedium)
            )

            // Активные фильтры
            if (uiState.searchQuery.isNotEmpty() ||
                uiState.locationFilter != null ||
                uiState.statusFilter != null
            ) {
                ActiveFiltersBadge(
                    searchQuery = uiState.searchQuery,
                    locationFilter = uiState.locationFilter,
                    statusFilter = uiState.statusFilter,
                    onClearFilters = {
                        viewModel.setSearchQuery("")
                        viewModel.setLocationFilter(null)
                        viewModel.setStatusFilter(null)
                    },
                    modifier = Modifier.padding(bottom = Dimens.spacingSmall)
                )
            }

            // Логика как в отчетах: если грузимся - показываем пустоту
            if (uiState.isLoading) {
                Spacer(modifier = Modifier.weight(1f))
            } else if (devices.isEmpty()) {
                EmptyDevicesState()
            } else {
                DeviceTableWithScroll(
                    devices = devices,
                    searchQuery = uiState.searchQuery,
                    sortColumn = uiState.sortColumn,
                    sortAscending = uiState.sortAscending,
                    verticalScrollState = verticalScrollState,
                    onSortColumn = { column -> viewModel.setSortColumn(column) },
                    onDeviceClick = { device -> onNavigateToDeviceDetail(device.id) },
                    onEditDevice = { device -> onNavigateToDeviceEdit(device.id) },
                    onDeleteDevice = deleteDeviceAction,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // FAB-кнопки
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 46.dp, bottom = 30.dp)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AnimatedVisibility(
                visible = showScrollToTopButton,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            if (verticalScrollState.firstVisibleItemIndex > 20) {
                                verticalScrollState.scrollToItem(20)
                            }
                            verticalScrollState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Наверх",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            FloatingActionButton(
                onClick = { onNavigateToDeviceEdit(null) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Добавить прибор",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        deleteDialogData?.let { dialogData ->
            DeviceDeleteDialog(
                device = dialogData.device,
                scheme = dialogData.scheme,
                deviceCountInLocation = dialogData.deviceCountInLocation,
                isLastInLocation = dialogData.isLastInLocation,
                onDismiss = { deleteViewModel.dismissDialog() },
                onConfirm = { deletePhotos, deleteScheme ->
                    scope.launch {
                        try {
                            viewModel.deleteDevice(dialogData.device, deletePhotos, deleteScheme)
                            deleteViewModel.dismissDialog()
                        } catch (e: Exception) {
                            deleteViewModel.dismissDialog()
                            notificationManager.notifyError("Ошибка удаления: ${e.message}")
                        }
                    }
                }
            )
        }

        // Глобальный индикатор загрузки
        LoadingOverlay(
            isLoading = uiState.isLoading,
            text = "Загрузка приборов..."
        )
    }
}

enum class SortColumn() {
    TYPE, NAME, INVENTORY_NUMBER, MEASUREMENT_LIMIT, LOCATION, VALVE_NUMBER, STATUS
}

private val COL_STATUS_WIDTH = 100.dp
private val COL_ACTIONS_WIDTH = 80.dp
private val COL_MIN_WIDTH = 890.dp
private const val W_TYPE = 120f / 710f
private const val W_NAME = 150f / 710f
private const val W_INVENTORY = 100f / 710f
private const val W_LIMIT = 120f / 710f
private const val W_LOCATION = 120f / 710f
private const val W_VALVE = 100f / 710f

@Composable
fun DeviceTableWithScroll(
    devices: List<Device>,
    searchQuery: String,
    sortColumn: SortColumn,
    sortAscending: Boolean,
    verticalScrollState: LazyListState,
    onSortColumn: (SortColumn) -> Unit,
    onDeviceClick: (Device) -> Unit,
    onEditDevice: (Device) -> Unit,
    onDeleteDevice: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val headerColor = MaterialTheme.colorScheme.surfaceVariant
    val colorScheme = MaterialTheme.colorScheme
    val evenColor = remember(colorScheme) { colorScheme.surface }
    val oddColor = remember(colorScheme) { colorScheme.surfaceVariant.copy(alpha = 0.1f) }
    val highlightColor = remember(colorScheme) { colorScheme.primary.copy(alpha = 0.3f) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val actualTableWidth = maxOf(COL_MIN_WIDTH, maxWidth)
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).background(headerColor).horizontalScroll(horizontalScrollState, enabled = false)) {
                TableHeader(sortColumn = sortColumn, sortAscending = sortAscending, onSortColumn = onSortColumn, tableWidth = actualTableWidth)
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth().horizontalScroll(horizontalScrollState)) {
                LazyColumn(state = verticalScrollState, modifier = Modifier.width(actualTableWidth), contentPadding = PaddingValues(bottom = 1.dp)) {
                    itemsIndexed(items = devices, key = { _, device -> device.id }) { index, device ->
                        TableRowWithDivider(device = device, bgColor = if (index % 2 == 0) evenColor else oddColor, highlightColor = highlightColor, searchQuery = searchQuery, onDeviceClick = onDeviceClick, onEditDevice = onEditDevice, onDeleteDevice = onDeleteDevice, showDivider = index < devices.size - 1, tableWidth = actualTableWidth)
                    }
                }
            }
        }
    }
}

@Composable
fun TableRowWithDivider(device: Device, bgColor: Color, highlightColor: Color, searchQuery: String, onDeviceClick: (Device) -> Unit, onEditDevice: (Device) -> Unit, onDeleteDevice: (Device) -> Unit, showDivider: Boolean, tableWidth: Dp) {
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Column {
        TableRow(device = device, bgColor = bgColor, highlightColor = highlightColor, searchQuery = searchQuery, onDeviceClick = onDeviceClick, onEditDevice = onEditDevice, onDeleteDevice = onDeleteDevice, tableWidth = tableWidth)
        if (showDivider) HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
    }
}

@Composable
fun TableHeader(sortColumn: SortColumn, sortAscending: Boolean, onSortColumn: (SortColumn) -> Unit, tableWidth: Dp) {
    Row(modifier = Modifier.width(tableWidth).height(Dimens.tableHeaderHeight).background(MaterialTheme.colorScheme.surfaceVariant), verticalAlignment = Alignment.CenterVertically) {
        TableHeaderCell("Тип прибора", Modifier.weight(W_TYPE), sortColumn == SortColumn.TYPE, sortAscending) { onSortColumn(SortColumn.TYPE) }
        TableHeaderCell("Модель", Modifier.weight(W_NAME), sortColumn == SortColumn.NAME, sortAscending) { onSortColumn(SortColumn.NAME) }
        TableHeaderCell("Инв. №", Modifier.weight(W_INVENTORY), sortColumn == SortColumn.INVENTORY_NUMBER, sortAscending) { onSortColumn(SortColumn.INVENTORY_NUMBER) }
        TableHeaderCell("Предел измер.", Modifier.weight(W_LIMIT), sortColumn == SortColumn.MEASUREMENT_LIMIT, sortAscending) { onSortColumn(SortColumn.MEASUREMENT_LIMIT) }
        TableHeaderCell("Место", Modifier.weight(W_LOCATION), sortColumn == SortColumn.LOCATION, sortAscending) { onSortColumn(SortColumn.LOCATION) }
        TableHeaderCell("Номер крана", Modifier.weight(W_VALVE), sortColumn == SortColumn.VALVE_NUMBER, sortAscending) { onSortColumn(SortColumn.VALVE_NUMBER) }
        TableHeaderCell("Статус", Modifier.width(COL_STATUS_WIDTH), sortColumn == SortColumn.STATUS, sortAscending) { onSortColumn(SortColumn.STATUS) }
        Box(modifier = Modifier.width(COL_ACTIONS_WIDTH).padding(vertical = 12.dp)) { Text("Действия", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp)) }
    }
}

@Composable
fun TableHeaderCell(title: String, modifier: Modifier = Modifier, isSorted: Boolean, sortAscending: Boolean, onClick: () -> Unit) {
    Box(modifier = modifier.height(40.dp), contentAlignment = Alignment.CenterStart) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(40.dp).clickable(onClick = onClick).padding(horizontal = 8.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (isSorted) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun TableRow(device: Device, bgColor: Color, highlightColor: Color, searchQuery: String, onDeviceClick: (Device) -> Unit, onEditDevice: (Device) -> Unit, onDeleteDevice: (Device) -> Unit, tableWidth: Dp) {
    var showMenu by remember { mutableStateOf(false) }

    // Проверяем, были ли добавлены фото сегодня (на основе timestamp в имени файла)
    val hasTodayPhoto = remember(device.photos) {
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        device.photos.any { fileName ->
            // Формат имени: device_ID_NAME_TIMESTAMP_HASH.jpg
            val parts = fileName.substringBeforeLast(".").split("_")
            if (parts.size >= 4) {
                val timestamp = parts[parts.size - 2].toLongOrNull() ?: 0L
                timestamp >= todayStart
            } else false
        }
    }

    // Если фото добавлено сегодня, делаем фон более заметным
    val effectiveBgColor = if (hasTodayPhoto) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else bgColor

    Row(modifier = Modifier.width(tableWidth).height(40.dp).background(effectiveBgColor).clickable { onDeviceClick(device) }, verticalAlignment = Alignment.CenterVertically) {
        // В первую колонку добавим индикатор, если фото новое
        Box(modifier = Modifier.weight(W_TYPE), contentAlignment = Alignment.CenterStart) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (hasTodayPhoto) {
                    PulsatingIndicator(
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                    )
                    TableCell(device.type, Modifier.fillMaxWidth(), searchQuery, highlightColor)
                } else {
                    TableCell(device.type, Modifier.fillMaxWidth(), searchQuery, highlightColor)
                }
            }
        }
        TableCell(device.name ?: "-", Modifier.weight(W_NAME), searchQuery, highlightColor)
        TableCell(device.inventoryNumber, Modifier.weight(W_INVENTORY), searchQuery, highlightColor)
        TableCell(device.measurementLimit ?: "-", Modifier.weight(W_LIMIT), searchQuery, highlightColor)
        TableCell(device.location, Modifier.weight(W_LOCATION), searchQuery, highlightColor)
        TableCell(device.valveNumber ?: "-", Modifier.weight(W_VALVE), searchQuery, highlightColor)
        Box(modifier = Modifier.width(COL_STATUS_WIDTH).padding(horizontal = Dimens.tableCellPaddingHorizontal)) { StatusBadgeCompact(status = device.status) }
        Box(modifier = Modifier.width(COL_ACTIONS_WIDTH).padding(horizontal = 4.dp)) {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.MoreVert, null, modifier = Modifier.size(18.dp)) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Открыть") }, onClick = { showMenu = false; onDeviceClick(device) }, leadingIcon = { Icon(Icons.Default.Visibility, null) })
                DropdownMenuItem(text = { Text("Изменить") }, onClick = { showMenu = false; onEditDevice(device) }, leadingIcon = { Icon(Icons.Default.Edit, null) })
                DropdownMenuItem(text = { Text("Удалить") }, onClick = { showMenu = false; onDeleteDevice(device) }, leadingIcon = { Icon(Icons.Default.Delete, null) })
            }
        }
    }
}

@Composable
fun PulsatingIndicator(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsating")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier.size(12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Пульсирующий ореол
        Box(
            modifier = Modifier
                .size(10.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .background(Color(0xFF4CAF50), CircleShape)
        )
        // Статичная точка в центре
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(Color(0xFF2E7D32), CircleShape)
        )
    }
}

@Composable
fun TableCell(text: String, modifier: Modifier = Modifier, searchQuery: String, highlightColor: Color, maxLines: Int = 1) {
    val textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize)
    Box(modifier = modifier.height(40.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
        val annotatedString = remember(text, searchQuery, highlightColor) {
            if (searchQuery.isNotEmpty() && text.contains(searchQuery, ignoreCase = true)) {
                buildAnnotatedString {
                    val lowerText = text.lowercase(); val lowerQuery = searchQuery.lowercase(); var startIndex = 0
                    while (true) {
                        val index = lowerText.indexOf(lowerQuery, startIndex)
                        if (index == -1) break
                        append(text.substring(startIndex, index))
                        withStyle(SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold)) { append(text.substring(index, index + searchQuery.length)) }
                        startIndex = index + searchQuery.length
                    }
                    if (startIndex < text.length) append(text.substring(startIndex))
                }
            } else AnnotatedString(text)
        }
        Text(text = annotatedString, style = textStyle, maxLines = maxLines, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun DeviceStatistics(stats: DeviceStats, modifier: Modifier = Modifier) {
    StatCard(groups = listOf(StatGroup(items = listOf(
        StatItemData(stats.total, "Всего", DeviceStatusColors.Total),
        StatItemData(stats.inWork, "В работе", DeviceStatusColors.Working),
        StatItemData(stats.inStorage, "Хранение", DeviceStatusColors.Storage),
        StatItemData(stats.lost, "Утерян", DeviceStatusColors.Lost),
        StatItemData(stats.broken, "Испорчен", DeviceStatusColors.Broken)
    ))), modifier = modifier)
}

@Composable
fun StatusBadgeCompact(status: String) {
    val deviceStatus = remember(status) { DeviceStatus.fromString(status) }
    Surface(color = deviceStatus.containerColor, shape = RoundedCornerShape(4.dp), modifier = Modifier.height(24.dp).padding(vertical = 2.dp)) {
        Text(text = status, color = deviceStatus.textColor, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.9f), modifier = Modifier.padding(horizontal = 6.dp).wrapContentHeight(Alignment.CenterVertically), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ActiveFiltersBadge(searchQuery: String, locationFilter: String?, statusFilter: String?, onClearFilters: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(Dimens.chipRadius), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(Dimens.spacingMedium), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FilterAlt, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = buildActiveFiltersText(searchQuery, locationFilter, statusFilter), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onClearFilters, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error) }
        }
    }
}

private fun buildActiveFiltersText(searchQuery: String, locationFilter: String?, statusFilter: String?): String {
    val filters = buildList {
        if (searchQuery.isNotEmpty()) add("Поиск: \"$searchQuery\"")
        if (locationFilter != null) add("Место: $locationFilter")
        if (statusFilter != null) add("Статус: $statusFilter")
    }
    return if (filters.isEmpty()) "Нет активных фильтров" else "Фильтры: ${filters.joinToString(", ")}"
}

@Composable
fun EmptyDevicesState(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().padding(Dimens.spacingXXLarge), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Devices, null, modifier = Modifier.size(Dimens.iconSizeXXLarge), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(Dimens.spacingLarge))
            Text("Нет приборов", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(Dimens.spacingMedium))
            Text("Добавьте первый прибор, нажав на кнопку \"+\" в нижнем углу экрана", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun DeviceDeleteDialog(
    device: Device,
    scheme: Scheme?,
    deviceCountInLocation: Int,
    isLastInLocation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (deletePhotos: Boolean, deleteScheme: Boolean) -> Unit
) {
    val photoCount = device.photos.size
    if (isLastInLocation && scheme != null) {
        DeviceDeleteWithSchemeDialog(
            deviceName = "${device.getDisplayName()} (${device.inventoryNumber})",
            schemeName = "'${scheme.name}'",
            photoCount = photoCount,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    } else {
        DeviceDeleteConfirmDialog(
            deviceName = "${device.getDisplayName()} (${device.inventoryNumber})",
            photoCount = photoCount,
            message = if (!isLastInLocation)
                "В локации '${device.location}' останется ещё ${deviceCountInLocation - 1} приборов."
            else "Это последнее устройство в локации '${device.location}'.",
            onConfirm = { deletePhotos -> onConfirm(deletePhotos, false) },
            onDismiss = onDismiss
        )
    }
}
