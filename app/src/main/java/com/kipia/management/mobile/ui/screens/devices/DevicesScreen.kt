package com.kipia.management.mobile.ui.screens.devices

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.ui.theme.DeviceStatus
import com.kipia.management.mobile.ui.theme.DeviceStatusColors
import com.kipia.management.mobile.viewmodel.DeviceDeleteViewModel
import com.kipia.management.mobile.viewmodel.DeviceStats
import com.kipia.management.mobile.viewmodel.DevicesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

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
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val deleteDialogData by deleteViewModel.showDeleteDialog.collectAsStateWithLifecycle()
    val verticalScrollState = rememberLazyListState()

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

    // shouldShowBottomNav уже State — просто инвертируем, derivedStateOf здесь избыточен
    val showScrollToTopButton = !shouldShowBottomNav

    // ── Удаление ─────────────────────────────────────────────────────────────

    val deleteDeviceAction: (Device) -> Unit = { device ->
        scope.launch { deleteViewModel.checkAndShowDialog(device) }
    }

    // ── Уведомления ──────────────────────────────────────────────────────────

    LaunchedEffect(Unit) {
        notificationManager.notification.collect { notification ->
            if (notification is NotificationManager.Notification.None) return@collect

            Timber.d("DevicesScreen: notification=$notification")

            val message = when (notification) {
                is NotificationManager.Notification.DeviceSaved ->
                    "Прибор '${notification.deviceName}' сохранен"

                is NotificationManager.Notification.DeviceDeleted ->
                    if (notification.withScheme)
                        "Прибор '${notification.deviceName}' и схема удалены"
                    else
                        "Прибор '${notification.deviceName}' удален"

                is NotificationManager.Notification.Error ->
                    "Ошибка: ${notification.message}"

                is NotificationManager.Notification.SchemeSaved ->
                    "Схема '${notification.schemeName}' сохранена"

                NotificationManager.Notification.None -> return@collect
            }

            scope.launch {
                snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
                delay(100)
                notificationManager.clearLastNotification()
            }
        }
    }

    // ── UI ───────────────────────────────────────────────────────────────────

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .add(WindowInsets(bottom = 0.dp))
                )
        ) {
            // Статистика — данные из ViewModel, не считаем здесь
            DeviceStatistics(
                stats = stats,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(bottom = 6.dp)
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
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            when {
                uiState.isLoading -> LoadingState()

                devices.isEmpty() -> EmptyDevicesState(
                    onAddDevice = { onNavigateToDeviceEdit(null) }
                )

                else -> DeviceTableWithScroll(
                    devices = devices, // уже отфильтрованы и отсортированы
                    searchQuery = uiState.searchQuery,
                    sortColumn = uiState.sortColumn,
                    sortAscending = uiState.sortAscending,
                    verticalScrollState = verticalScrollState,
                    onSortColumn = { column -> viewModel.setSortColumn(column) },
                    onDeviceClick = { device -> onNavigateToDeviceDetail(device.id) },
                    onEditDevice = { device -> onNavigateToDeviceEdit(device.id) },
                    onDeleteDevice = deleteDeviceAction,
                    modifier = Modifier
                        .weight(1f)
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
                            // Если далеко — сначала прыгаем ближе, потом плавно доезжаем
                            if (verticalScrollState.firstVisibleItemIndex > 20) {
                                verticalScrollState.scrollToItem(20) // мгновенный прыжок
                            }
                            verticalScrollState.animateScrollToItem(0) // плавная анимация последних ~20 строк
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

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        deleteDialogData?.let { dialogData ->
            DeviceDeleteDialog(
                device = dialogData.device,
                scheme = dialogData.scheme,
                deviceCountInLocation = dialogData.deviceCountInLocation,
                isLastInLocation = dialogData.isLastInLocation,
                onDismiss = { deleteViewModel.dismissDialog() },
                onConfirm = { deleteScheme ->
                    scope.launch {
                        try {
                            viewModel.deleteDevice(dialogData.device, deleteScheme)
                            deleteViewModel.dismissDialog()
                        } catch (e: Exception) {
                            deleteViewModel.dismissDialog()
                            snackbarHostState.showSnackbar("Ошибка удаления: ${e.message}")
                        }
                    }
                }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Enum — должен быть здесь, т.к. ViewModel его импортирует
// ═════════════════════════════════════════════════════════════════════════════

enum class SortColumn() {
    TYPE,
    NAME,
    INVENTORY_NUMBER,
    MEASUREMENT_LIMIT,
    LOCATION,
    VALVE_NUMBER,
    STATUS
}

// ═════════════════════════════════════════════════════════════════════════════
// Таблица
// ═════════════════════════════════════════════════════════════════════════════

// ── Веса колонок (сумма = 1f) ────────────────────────────────────────────────
// Тип:120, Модель:150, Инв:100, Предел:120, Место:120, Кран:100 → итого 710dp из 810dp
// Статус(100) и Действия(80) — фиксированные, не растягиваются
private val COL_STATUS_WIDTH = 100.dp
private val COL_ACTIONS_WIDTH = 80.dp
private val COL_MIN_WIDTH = 890.dp // минимальная ширина до горизонтального скролла

// Пропорции 6 гибких колонок (в долях от 1f):
private const val W_TYPE = 120f / 710f  // ~0.169
private const val W_NAME = 150f / 710f  // ~0.211
private const val W_INVENTORY = 100f / 710f  // ~0.141
private const val W_LIMIT = 120f / 710f  // ~0.169
private const val W_LOCATION = 120f / 710f  // ~0.169
private const val W_VALVE = 100f / 710f  // ~0.141

@Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@SuppressLint("UnusedBoxWithConstraintsScope")
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

    // ★ ПАТЧ 1: цвета вычисляются ОДИН РАЗ здесь, не в каждой строке
    val colorScheme = MaterialTheme.colorScheme
    val evenColor      = remember(colorScheme) { colorScheme.surface }
    val oddColor       = remember(colorScheme) { colorScheme.surfaceVariant.copy(alpha = 0.1f) }
    val highlightColor = remember(colorScheme) { colorScheme.primary.copy(alpha = 0.3f) }

    // ★ ПАТЧ 3: вычисляем ширину через BoxWithConstraints СНАРУЖИ Column
    //   чтобы передать её и в шапку и в тело таблицы
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        // На широком экране растягиваемся, на узком — горизонтальный скролл
        val actualTableWidth = maxOf(COL_MIN_WIDTH, maxWidth)

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(headerColor)
                    .horizontalScroll(horizontalScrollState, enabled = false)
            ) {
                TableHeader(
                    sortColumn = sortColumn,
                    sortAscending = sortAscending,
                    onSortColumn = onSortColumn,
                    tableWidth = actualTableWidth  // ★ теперь передаём actualTableWidth
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                LazyColumn(
                    state = verticalScrollState,
                    modifier = Modifier.width(actualTableWidth),
                    contentPadding = PaddingValues(bottom = 1.dp),
                ) {
                    itemsIndexed(
                        items = devices,
                        key = { _, device -> device.id },
                        contentType = { _, _ -> "device_row" }
                    ) { index, device ->
                        TableRowWithDivider(
                            device = device,
                            bgColor = if (index % 2 == 0) evenColor else oddColor,
                            highlightColor = highlightColor,
                            searchQuery = searchQuery,
                            onDeviceClick = onDeviceClick,
                            onEditDevice = onEditDevice,
                            onDeleteDevice = onDeleteDevice,
                            showDivider = index < devices.size - 1,
                            tableWidth = actualTableWidth
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TableRowWithDivider(
    device: Device,
    // ★ ПАТЧ 1: принимаем готовые цвета вместо isEven
    bgColor: Color,
    highlightColor: Color,
    searchQuery: String,
    onDeviceClick: (Device) -> Unit,
    onEditDevice: (Device) -> Unit,
    onDeleteDevice: (Device) -> Unit,
    showDivider: Boolean,
    tableWidth: Dp,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val dividerColor = remember(colorScheme) { colorScheme.outline.copy(alpha = 0.2f) }

    Column(modifier = modifier) {
        TableRow(
            device = device,
            bgColor = bgColor,
            highlightColor = highlightColor,
            searchQuery = searchQuery,
            onDeviceClick = onDeviceClick,
            onEditDevice = onEditDevice,
            onDeleteDevice = onDeleteDevice,
            tableWidth = tableWidth
        )
        if (showDivider) {
            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
        }
    }
}

@Composable
fun TableHeader(
    sortColumn: SortColumn,
    sortAscending: Boolean,
    onSortColumn: (SortColumn) -> Unit,
    tableWidth: Dp,           // ← новый параметр
    modifier: Modifier = Modifier
) {
    val onSortType = remember(onSortColumn) { { onSortColumn(SortColumn.TYPE) } }
    val onSortName = remember(onSortColumn) { { onSortColumn(SortColumn.NAME) } }
    val onSortInventory = remember(onSortColumn) { { onSortColumn(SortColumn.INVENTORY_NUMBER) } }
    val onSortLimit = remember(onSortColumn) { { onSortColumn(SortColumn.MEASUREMENT_LIMIT) } }
    val onSortLocation = remember(onSortColumn) { { onSortColumn(SortColumn.LOCATION) } }
    val onSortValve = remember(onSortColumn) { { onSortColumn(SortColumn.VALVE_NUMBER) } }
    val onSortStatus = remember(onSortColumn) { { onSortColumn(SortColumn.STATUS) } }

    Row(
        modifier = modifier
            .width(tableWidth)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 6 гибких колонок через weight
        TableHeaderCell(
            "Тип прибора",
            Modifier.weight(W_TYPE),
            sortColumn == SortColumn.TYPE,
            sortAscending,
            onSortType
        )
        TableHeaderCell(
            "Модель",
            Modifier.weight(W_NAME),
            sortColumn == SortColumn.NAME,
            sortAscending,
            onSortName
        )
        TableHeaderCell(
            "Инв. №",
            Modifier.weight(W_INVENTORY),
            sortColumn == SortColumn.INVENTORY_NUMBER,
            sortAscending,
            onSortInventory
        )
        TableHeaderCell(
            "Предел измер.",
            Modifier.weight(W_LIMIT),
            sortColumn == SortColumn.MEASUREMENT_LIMIT,
            sortAscending,
            onSortLimit
        )
        TableHeaderCell(
            "Место",
            Modifier.weight(W_LOCATION),
            sortColumn == SortColumn.LOCATION,
            sortAscending,
            onSortLocation
        )
        TableHeaderCell(
            "Номер крана",
            Modifier.weight(W_VALVE),
            sortColumn == SortColumn.VALVE_NUMBER,
            sortAscending,
            onSortValve
        )

        // 2 фиксированные колонки
        TableHeaderCell(
            "Статус",
            Modifier.width(COL_STATUS_WIDTH),
            sortColumn == SortColumn.STATUS,
            sortAscending,
            onSortStatus
        )
        Box(
            modifier = Modifier
                .width(COL_ACTIONS_WIDTH)
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "Действия",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

// TableHeaderCell — убираем Dp, принимаем готовый Modifier
@Composable
fun TableHeaderCell(
    title: String,
    modifier: Modifier = Modifier,   // ← теперь сюда приходит weight ИЛИ width
    isSorted: Boolean,
    sortAscending: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier.height(40.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (isSorted) {
                Icon(
                    imageVector = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun TableRow(
    device: Device,
    // ★ ПАТЧ 1: принимаем готовые цвета — не читаем colorScheme внутри каждой строки
    bgColor: Color,
    highlightColor: Color,
    searchQuery: String,
    onDeviceClick: (Device) -> Unit,
    onEditDevice: (Device) -> Unit,
    onDeleteDevice: (Device) -> Unit,
    tableWidth: Dp,
    modifier: Modifier = Modifier
) {
    // ★ ПАТЧ 2: expanded = showMenu вместо if(showMenu) + expanded = true
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .width(tableWidth)
            .height(40.dp)
            .background(bgColor)
            .clickable { onDeviceClick(device) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableCell(device.type, Modifier.weight(W_TYPE), searchQuery, highlightColor)
        TableCell(device.name ?: "-", Modifier.weight(W_NAME), searchQuery, highlightColor)
        TableCell(device.inventoryNumber, Modifier.weight(W_INVENTORY), searchQuery, highlightColor)
        TableCell(device.measurementLimit ?: "-", Modifier.weight(W_LIMIT), searchQuery, highlightColor)
        TableCell(device.location, Modifier.weight(W_LOCATION), searchQuery, highlightColor)
        TableCell(device.valveNumber ?: "-", Modifier.weight(W_VALVE), searchQuery, highlightColor)

        Box(modifier = Modifier.width(COL_STATUS_WIDTH).padding(horizontal = 8.dp)) {
            StatusBadgeCompact(status = device.status)
        }

        Box(modifier = Modifier.width(COL_ACTIONS_WIDTH).padding(horizontal = 4.dp)) {
            IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.MoreVert, contentDescription = "Меню", modifier = Modifier.size(18.dp))
            }
            // ★ ПАТЧ 2: убран if(showMenu), expanded = showMenu
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Открыть") },
                    onClick = { showMenu = false; onDeviceClick(device) },
                    leadingIcon = { Icon(Icons.Default.Visibility, null) }
                )
                DropdownMenuItem(
                    text = { Text("Изменить") },
                    onClick = { showMenu = false; onEditDevice(device) },
                    leadingIcon = { Icon(Icons.Default.Edit, null) }
                )
                DropdownMenuItem(
                    text = { Text("Удалить") },
                    onClick = { showMenu = false; onDeleteDevice(device) },
                    leadingIcon = { Icon(Icons.Default.Delete, null) }
                )
            }
        }
    }
}

// TableCell — тоже убираем Dp, принимаем Modifier
@OptIn(ExperimentalTextApi::class)
@Composable
fun TableCell(
    text: String,
    modifier: Modifier = Modifier,
    searchQuery: String,
    highlightColor: Color,
    maxLines: Int = 1
) {
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    // Кэшируем стиль — пересчитывается только при смене темы
    val textStyle = remember(typography) {
        typography.bodyMedium.copy(fontSize = typography.labelSmall.fontSize)
    }
    val textColor = remember(colorScheme) { colorScheme.onSurface }

    Box(
        modifier = modifier.height(40.dp).padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        val annotatedString = remember(text, searchQuery, highlightColor) {
            if (searchQuery.isNotEmpty() && text.contains(searchQuery, ignoreCase = true)) {
                buildAnnotatedString {
                    val lowerText = text.lowercase()
                    val lowerQuery = searchQuery.lowercase()
                    var startIndex = 0
                    while (true) {
                        val index = lowerText.indexOf(lowerQuery, startIndex)
                        if (index == -1) break
                        append(text.substring(startIndex, index))
                        withStyle(SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold)) {
                            append(text.substring(index, index + searchQuery.length))
                        }
                        startIndex = index + searchQuery.length
                    }
                    if (startIndex < text.length) append(text.substring(startIndex))
                }
            } else {
                AnnotatedString(text)
            }
        }

        Text(
            text = annotatedString,
            style = textStyle,         // ← стабильный объект из remember
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            color = textColor,         // ← стабильный цвет из remember
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun DeviceStatistics(stats: DeviceStats, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItem(stats.total, "Всего", DeviceStatusColors.Total, Modifier.weight(1f))
            StatItem(stats.inWork, "В работе", DeviceStatusColors.Working, Modifier.weight(1f))
            StatItem(stats.inStorage, "Хранение", DeviceStatusColors.Storage, Modifier.weight(1f))
            StatItem(stats.lost, "Утерян", DeviceStatusColors.Lost, Modifier.weight(1f))
            StatItem(stats.broken, "Испорчен", DeviceStatusColors.Broken, Modifier.weight(1f))
        }
    }
}

@Composable
fun StatItem(count: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StatusBadgeCompact(status: String) {
    val deviceStatus = remember(status) { DeviceStatus.fromString(status) }
    val typography = MaterialTheme.typography

    val textStyle = remember(typography) {
        typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            fontSize = typography.labelSmall.fontSize * 0.9f
        )
    }

    Surface(
        color = deviceStatus.containerColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.height(24.dp).padding(vertical = 2.dp)
    ) {
        Text(
            text = getCompactStatus(status),
            color = deviceStatus.textColor,
            style = textStyle,   // ← стабильный объект
            modifier = Modifier.padding(horizontal = 6.dp).wrapContentHeight(Alignment.CenterVertically),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getCompactStatus(fullStatus: String): String = when (fullStatus) {
    "В работе" -> "В работе"
    "Хранение" -> "Хранение"
    "Утерян" -> "Утерян"
    "Испорчен" -> "Испорчен"
    else -> fullStatus
}

@Composable
fun ActiveFiltersBadge(
    searchQuery: String,
    locationFilter: String?,
    statusFilter: String?,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.5f
            )
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FilterAlt,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildActiveFiltersText(searchQuery, locationFilter, statusFilter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onClearFilters, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Close,
                    "Очистить фильтры",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun buildActiveFiltersText(
    searchQuery: String,
    locationFilter: String?,
    statusFilter: String?
): String {
    val filters = buildList {
        if (searchQuery.isNotEmpty()) add("Поиск: \"$searchQuery\"")
        if (locationFilter != null) add("Место: $locationFilter")
        if (statusFilter != null) add("Статус: $statusFilter")
    }
    return if (filters.isEmpty()) "Нет активных фильтров" else "Фильтры: ${filters.joinToString(", ")}"
}

@Composable
fun LoadingState() {
    Box(modifier = Modifier
        .fillMaxSize()
        .padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Загрузка приборов...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyDevicesState(onAddDevice: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier
        .fillMaxSize()
        .padding(32.dp), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Devices,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Нет приборов",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Добавьте первый прибор, нажав на кнопку ниже",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddDevice,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить прибор")
            }
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
    onConfirm: (deleteScheme: Boolean) -> Unit
) {
    if (isLastInLocation && scheme != null) {
        // Спецдиалог с тремя кнопками
        DeviceDeleteWithSchemeDialog(
            deviceName = "${device.getDisplayName()} (${device.inventoryNumber})",
            schemeName = "'${scheme.name}'",
            onDeleteWithScheme = { onConfirm(true) },
            onDeleteOnly = { onConfirm(false) },
            onDismiss = onDismiss
        )
    } else {
        // Обычный диалог удаления
        DeleteConfirmDialog(
            title = if (isLastInLocation) "Удаление устройства" else "Подтверждение удаления",
            itemName = "${device.getDisplayName()} (${device.inventoryNumber})",
            message = if (!isLastInLocation)
                "В локации '${device.location}' останется ещё ${deviceCountInLocation - 1} приборов."
            else
                "Это последнее устройство в локации '${device.location}'.",
            onConfirm = { onConfirm(false) },
            onDismiss = onDismiss
        )
    }
}