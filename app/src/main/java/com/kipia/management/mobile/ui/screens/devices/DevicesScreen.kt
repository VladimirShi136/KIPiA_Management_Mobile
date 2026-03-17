package com.kipia.management.mobile.ui.screens.devices

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.Scheme
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
    val devices    by viewModel.devices.collectAsStateWithLifecycle()
    val uiState    by viewModel.uiState.collectAsStateWithLifecycle()
    val stats      by viewModel.stats.collectAsStateWithLifecycle()
    val snackbarHostState  = remember { SnackbarHostState() }
    val scope              = rememberCoroutineScope()
    val deleteDialogData   by deleteViewModel.showDeleteDialog.collectAsStateWithLifecycle()
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

    val showScrollToTopButton by remember {
        derivedStateOf { !shouldShowBottomNav }
    }

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
                uiState.locationFilter != null   ||
                uiState.statusFilter   != null
            ) {
                ActiveFiltersBadge(
                    searchQuery    = uiState.searchQuery,
                    locationFilter = uiState.locationFilter,
                    statusFilter   = uiState.statusFilter,
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
                    devices             = devices, // уже отфильтрованы и отсортированы
                    searchQuery         = uiState.searchQuery,
                    sortColumn          = uiState.sortColumn,
                    sortAscending       = uiState.sortAscending,
                    verticalScrollState = verticalScrollState,
                    onSortColumn        = { column -> viewModel.setSortColumn(column) },
                    onDeviceClick       = { device -> onNavigateToDeviceDetail(device.id) },
                    onEditDevice        = { device -> onNavigateToDeviceEdit(device.id) },
                    onDeleteDevice      = deleteDeviceAction,
                    modifier            = Modifier
                        .weight(1f)
                        .fillMaxSize()
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
                enter   = fadeIn() + scaleIn(),
                exit    = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick        = { scope.launch { verticalScrollState.animateScrollToItem(0) } },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor   = MaterialTheme.colorScheme.onPrimary,
                    modifier       = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Наверх", modifier = Modifier.size(24.dp))
                }
            }

            FloatingActionButton(
                onClick        = { onNavigateToDeviceEdit(null) },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor   = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier       = Modifier.size(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить прибор", modifier = Modifier.size(24.dp))
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier  = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        deleteDialogData?.let { dialogData ->
            DeviceDeleteDialog(
                device               = dialogData.device,
                scheme               = dialogData.scheme,
                deviceCountInLocation = dialogData.deviceCountInLocation,
                isLastInLocation     = dialogData.isLastInLocation,
                onDismiss            = { deleteViewModel.dismissDialog() },
                onConfirm            = { deleteScheme ->
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

enum class SortColumn(val displayName: String) {
    TYPE("Тип прибора"),
    NAME("Модель"),
    INVENTORY_NUMBER("Инв. №"),
    MEASUREMENT_LIMIT("Предел измерений"),
    LOCATION("Место установки"),
    VALVE_NUMBER("Номер крана"),
    STATUS("Статус")
}

// ═════════════════════════════════════════════════════════════════════════════
// Таблица
// Архитектура: один horizontalScroll снаружи, LazyColumn внутри.
// Строки — обычные Row без подписки на ScrollState.
// При горизонтальном скролле рекомпозируется только внешний Box,
// а не каждая из видимых строк — это главное ускорение.
// ═════════════════════════════════════════════════════════════════════════════

// Суммарная ширина всех колонок (120+150+100+120+120+100+100+80)
private val TABLE_TOTAL_WIDTH = 890.dp

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
    // Один ScrollState на всю таблицу — шапка и строки скроллятся синхронно
    val horizontalScrollState = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {

        // ── Шапка ──────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .horizontalScroll(horizontalScrollState)
        ) {
            TableHeader(
                sortColumn    = sortColumn,
                sortAscending = sortAscending,
                onSortColumn  = onSortColumn
            )
        }

        // ── Тело таблицы ───────────────────────────────────────────────────
        // horizontalScroll СНАРУЖИ LazyColumn — строки не знают о скролле
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                .horizontalScroll(horizontalScrollState)
        ) {
            LazyColumn(
                state          = verticalScrollState,
                modifier       = Modifier.width(TABLE_TOTAL_WIDTH),
                contentPadding = PaddingValues(bottom = 1.dp)
            ) {
                itemsIndexed(devices, key = { _, device -> device.id }) { index, device ->
                    // remember стабилизирует лямбды — строка не перерисовывается
                    // если device.id не изменился
                    val onClick  = remember(device.id) { { onDeviceClick(device) } }
                    val onEdit   = remember(device.id) { { onEditDevice(device) } }
                    val onDelete = remember(device.id) { { onDeleteDevice(device) } }

                    TableRowWithDivider(
                        device      = device,
                        index       = index,
                        searchQuery = searchQuery,
                        onClick     = onClick,
                        onEdit      = onEdit,
                        onDelete    = onDelete,
                        showDivider = index < devices.size - 1
                    )
                }
            }
        }
    }
}

@Composable
fun TableRowWithDivider(
    device: Device,
    index: Int,
    searchQuery: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    // dividerColor через remember — не читаем colorScheme при каждом скролле
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Column(modifier = modifier) {
        TableRow(
            device      = device,
            index       = index,
            searchQuery = searchQuery,
            onClick     = onClick,
            onEdit      = onEdit,
            onDelete    = onDelete,
            modifier    = Modifier
                .width(TABLE_TOTAL_WIDTH)
                .height(40.dp)
        )
        if (showDivider) {
            HorizontalDivider(
                modifier  = Modifier
                    .width(TABLE_TOTAL_WIDTH)
                    .padding(horizontal = 8.dp),
                thickness = 0.5.dp,
                color     = dividerColor
            )
        }
    }
}

@Composable
fun TableHeader(
    sortColumn: SortColumn,
    sortAscending: Boolean,
    onSortColumn: (SortColumn) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier          = modifier
            .width(TABLE_TOTAL_WIDTH)
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TableHeaderCell(title = "Тип прибора",    width = 120.dp, isSorted = sortColumn == SortColumn.TYPE,              sortAscending = sortAscending, onClick = { onSortColumn(SortColumn.TYPE) })
        TableHeaderCell(title = "Модель",         width = 150.dp, isSorted = sortColumn == SortColumn.NAME,              sortAscending = sortAscending, onClick = { onSortColumn(SortColumn.NAME) })
        TableHeaderCell(title = "Инв. №",         width = 100.dp, isSorted = sortColumn == SortColumn.INVENTORY_NUMBER,  sortAscending = sortAscending, onClick = { onSortColumn(SortColumn.INVENTORY_NUMBER) })
        TableHeaderCell(title = "Предел измер.",  width = 120.dp, isSorted = sortColumn == SortColumn.MEASUREMENT_LIMIT, sortAscending = sortAscending, onClick = { onSortColumn(SortColumn.MEASUREMENT_LIMIT) })
        TableHeaderCell(title = "Место",          width = 120.dp, isSorted = sortColumn == SortColumn.LOCATION,          sortAscending = sortAscending, onClick = { onSortColumn(SortColumn.LOCATION) })
        TableHeaderCell(title = "Номер крана",    width = 100.dp, isSorted = sortColumn == SortColumn.VALVE_NUMBER,      sortAscending = sortAscending, onClick = { onSortColumn(SortColumn.VALVE_NUMBER) })
        TableHeaderCell(title = "Статус",         width = 100.dp, isSorted = sortColumn == SortColumn.STATUS,            sortAscending = sortAscending, onClick = { onSortColumn(SortColumn.STATUS) })

        Box(modifier = Modifier.width(80.dp).padding(vertical = 12.dp)) {
            Text(
                text     = "Действия",
                style    = MaterialTheme.typography.labelMedium,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun TableHeaderCell(
    title: String,
    width: androidx.compose.ui.unit.Dp,
    isSorted: Boolean,
    sortAscending: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.width(width).height(40.dp),
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
                text     = title,
                style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (isSorted) {
                Icon(
                    imageVector        = if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = if (sortAscending) "По возрастанию" else "По убыванию",
                    modifier           = Modifier.size(16.dp),
                    tint               = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun TableRow(
    device: Device,
    index: Int,
    searchQuery: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    // Читаем цвета один раз — Compose кэширует если colorScheme не менялась
    val bgColor   = if (index % 2 == 0) MaterialTheme.colorScheme.surface
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
    val highlight = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)

    Row(
        modifier = modifier
            .width(TABLE_TOTAL_WIDTH)
            .height(40.dp)
            .clickable(onClick = onClick)
            .background(bgColor),
        verticalAlignment = Alignment.CenterVertically
    ) {

        TableCell(device.type,                   120.dp, searchQuery, highlight)
        TableCell(device.name ?: "-",            150.dp, searchQuery, highlight)
        TableCell(device.inventoryNumber,         100.dp, searchQuery, highlight)
        TableCell(device.measurementLimit ?: "-", 120.dp, searchQuery, highlight)
        TableCell(device.location,               120.dp, searchQuery, highlight)
        TableCell(device.valveNumber ?: "-",     100.dp, searchQuery, highlight)

        Box(modifier = Modifier.width(100.dp).padding(horizontal = 8.dp)) {
            StatusBadgeCompact(status = device.status)
        }

        Box(modifier = Modifier.width(80.dp).padding(horizontal = 4.dp)) {
            IconButton(
                onClick  = { showMenu = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.MoreVert, contentDescription = "Меню", modifier = Modifier.size(18.dp))
            }

            DropdownMenu(
                expanded          = showMenu,
                onDismissRequest  = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text         = { Text("Открыть") },
                    onClick      = { showMenu = false; onClick() },
                    leadingIcon  = { Icon(Icons.Default.Visibility, null) }
                )
                DropdownMenuItem(
                    text         = { Text("Изменить") },
                    onClick      = { showMenu = false; onEdit() },
                    leadingIcon  = { Icon(Icons.Default.Edit, null) }
                )
                DropdownMenuItem(
                    text         = { Text("Удалить") },
                    onClick      = { showMenu = false; onDelete() },
                    leadingIcon  = { Icon(Icons.Default.Delete, null) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    searchQuery: String,
    highlightColor: Color,
    maxLines: Int = 1,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(40.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (searchQuery.isNotEmpty() && text.contains(searchQuery, ignoreCase = true)) {
            // remember кэширует результат — не пересчитывается пока text/searchQuery не изменились
            val annotatedString = remember(text, searchQuery, highlightColor) {
                buildAnnotatedString {
                    val lowerText  = text.lowercase()
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
            }
            Text(
                text     = annotatedString,
                style    = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text     = text,
                style    = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                color    = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Статистика — принимает готовый DeviceStats из ViewModel
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun DeviceStatistics(
    stats: DeviceStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatItem(stats.total,     "Всего",    DeviceStatusColors.Total,   Modifier.weight(1f))
            StatItem(stats.inWork,    "В работе", DeviceStatusColors.Working, Modifier.weight(1f))
            StatItem(stats.inStorage, "Хранение", DeviceStatusColors.Storage, Modifier.weight(1f))
            StatItem(stats.lost,      "Утерян",   DeviceStatusColors.Lost,    Modifier.weight(1f))
            StatItem(stats.broken,    "Испорчен", DeviceStatusColors.Broken,  Modifier.weight(1f))
        }
    }
}

@Composable
fun StatItem(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text  = count.toString(),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text      = label,
            style     = MaterialTheme.typography.labelSmall,
            color     = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Прочие компоненты
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun StatusBadgeCompact(status: String) {
    val deviceStatus = DeviceStatus.fromString(status)
    Surface(
        color    = deviceStatus.containerColor,
        shape    = RoundedCornerShape(4.dp),
        modifier = Modifier.height(24.dp).padding(vertical = 2.dp)
    ) {
        Text(
            text     = getCompactStatus(status),
            color    = deviceStatus.textColor,
            style    = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize   = MaterialTheme.typography.labelSmall.fontSize * 0.9f
            ),
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .wrapContentHeight(Alignment.CenterVertically),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun getCompactStatus(fullStatus: String): String = when (fullStatus) {
    "В работе" -> "В работе"
    "Хранение" -> "Хранение"
    "Утерян"   -> "Утерян"
    "Испорчен" -> "Испорчен"
    else        -> fullStatus
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
        shape    = RoundedCornerShape(8.dp),
        colors   = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Row(
                modifier          = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.FilterAlt, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text     = buildActiveFiltersText(searchQuery, locationFilter, statusFilter),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onClearFilters, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, "Очистить фильтры", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
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
        if (locationFilter != null)   add("Место: $locationFilter")
        if (statusFilter   != null)   add("Статус: $statusFilter")
    }
    return if (filters.isEmpty()) "Нет активных фильтров"
    else "Фильтры: ${filters.joinToString(", ")}"
}

@Composable
fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp), strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Загрузка приборов...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EmptyDevicesState(
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(Icons.Default.Devices, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Нет приборов", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Добавьте первый прибор, нажав на кнопку ниже", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAddDevice, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) {
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isLastInLocation) "Удаление устройства" else "Подтверждение удаления",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(modifier = Modifier.padding(16.dp).heightIn(max = 200.dp)) {
                if (isLastInLocation) {
                    Text("Вы удаляете устройство:", style = MaterialTheme.typography.bodyMedium)
                    Text("${device.getDisplayName()} (${device.inventoryNumber})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
                    if (scheme != null) {
                        Text("Это последнее устройство в локации:", style = MaterialTheme.typography.bodyMedium)
                        Text("'${scheme.name}'", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
                        Text("Что делать со схемой этой локации?", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text("Это последнее устройство в локации '${device.location}'.", style = MaterialTheme.typography.bodyMedium)
                        Text("Схема не привязана.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("Вы уверены, что хотите удалить устройство?", style = MaterialTheme.typography.bodyMedium)
                    Text("${device.getDisplayName()} (${device.inventoryNumber})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 8.dp))
                    Text("В локации '${device.location}' останется ещё ${deviceCountInLocation - 1} приборов.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Схема останется без изменений.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(isLastInLocation && scheme != null) },
                colors  = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text  = if (isLastInLocation && scheme != null) "Удалить с схемой" else "Удалить устройство",
                    color = MaterialTheme.colorScheme.onError
                )
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    isLastInLocation && scheme != null -> {
                        Button(
                            onClick = { onConfirm(false) },
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor   = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) { Text("Только устройство") }
                        Button(onClick = onDismiss) { Text("Отмена") }
                    }
                    else -> {
                        Button(
                            onClick = onDismiss,
                            colors  = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor   = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) { Text("Отмена") }
                    }
                }
            }
        }
    )
}