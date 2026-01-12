package com.kipia.management.mobile.ui.screens.devices

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.theme.DeviceStatus
import com.kipia.management.mobile.ui.theme.DeviceStatusColors
import com.kipia.management.mobile.viewmodel.DevicesViewModel
import kotlinx.coroutines.launch

/**
 * Основной экран для отображения и управления приборами
 *
 * Ключевые улучшения:
 * 1. Верхняя панель статистики с цветовой кодировкой
 * 2. Горизонтально прокручиваемая таблица с фиксированными колонками
 * 3. Сортировка по всем колонкам
 * 4. Быстрая фильтрация по конкретной колонке
 * 5. Подсветка найденного текста при поиске
 * 6. Компактные бейджи статусов
 * 7. LazyColumn для производительности
 * 8. Контекстное меню действий для каждого прибора
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun DevicesScreen(
    onNavigateToDeviceDetail: (Int) -> Unit,
    onNavigateToDeviceEdit: (Int?) -> Unit,
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var sortColumn by remember { mutableStateOf(SortColumn.INVENTORY_NUMBER) }
    var sortAscending by remember { mutableStateOf(true) }
    var quickFilterColumn by remember { mutableStateOf<SortColumn?>(null) }
    var quickFilterText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Приборы")
                        AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                            Text(
                                text = " • Поиск: \"$searchQuery\"",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                },
                actions = {
                    // Поиск
                    var showSearch by remember { mutableStateOf(false) }
                    if (showSearch) {
                        SearchField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            onClose = {
                                showSearch = false
                                viewModel.setSearchQuery("")
                            }
                        )
                    } else {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        }
                    }

                    // Фильтр по типу
                    TypeFilterDropdown(
                        currentFilter = uiState.typeFilter,
                        onFilterSelected = { viewModel.setTypeFilter(it) }
                    )

                    // Фильтр по статусу
                    StatusFilterDropdown(
                        currentFilter = uiState.statusFilter,
                        onFilterSelected = { viewModel.setStatusFilter(it) }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToDeviceEdit(null) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить прибор")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // СТАТИСТИКА
            DeviceStatistics(
                devices = devices,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Показываем активные фильтры
            if (uiState.typeFilter != null || uiState.statusFilter != null) {
                ActiveFiltersBadge(
                    typeFilter = uiState.typeFilter,
                    statusFilter = uiState.statusFilter,
                    onClearFilters = {
                        viewModel.setTypeFilter(null)
                        viewModel.setStatusFilter(null)
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Быстрая фильтрация по колонке
            AnimatedVisibility(visible = quickFilterColumn != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Фильтр по ${quickFilterColumn?.displayName ?: ""}:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    OutlinedTextField(
                        value = quickFilterText,
                        onValueChange = { quickFilterText = it },
                        placeholder = { Text("Введите текст...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        trailingIcon = {
                            if (quickFilterText.isNotEmpty()) {
                                IconButton(onClick = { quickFilterText = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                                }
                            }
                        }
                    )

                    IconButton(onClick = {
                        quickFilterColumn = null
                        quickFilterText = ""
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть фильтр")
                    }
                }
            }

            // Сортированные и отфильтрованные устройства
            val sortedDevices = remember(devices, sortColumn, sortAscending, quickFilterColumn, quickFilterText) {
                var result = devices.sortedWith(
                    compareBy<Device> { device ->
                        when (sortColumn) {
                            SortColumn.TYPE -> device.type
                            SortColumn.NAME -> device.name ?: ""
                            SortColumn.INVENTORY_NUMBER -> device.inventoryNumber
                            SortColumn.MEASUREMENT_LIMIT -> device.measurementLimit ?: ""
                            SortColumn.LOCATION -> device.location
                            SortColumn.VALVE_NUMBER -> device.valveNumber ?: ""
                            SortColumn.STATUS -> device.status
                        }
                    }.let { comparator ->
                        if (!sortAscending) comparator.reversed() else comparator
                    }
                )

                // Применяем быструю фильтрацию если активна
                if (quickFilterColumn != null && quickFilterText.isNotEmpty()) {
                    result = result.filter { device ->
                        val fieldValue = when (quickFilterColumn) {
                            SortColumn.TYPE -> device.type
                            SortColumn.NAME -> device.name ?: ""
                            SortColumn.INVENTORY_NUMBER -> device.inventoryNumber
                            SortColumn.MEASUREMENT_LIMIT -> device.measurementLimit ?: ""
                            SortColumn.LOCATION -> device.location
                            SortColumn.VALVE_NUMBER -> device.valveNumber ?: ""
                            SortColumn.STATUS -> device.status
                            null -> ""
                        }
                        fieldValue.contains(quickFilterText, ignoreCase = true)
                    }
                }

                result
            }

            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                sortedDevices.isEmpty() -> {
                    EmptyDevicesState(
                        onAddDevice = { onNavigateToDeviceEdit(null) }
                    )
                }
                else -> {
                    // Таблица устройств с горизонтальной прокруткой
                    DeviceTable(
                        devices = sortedDevices,
                        searchQuery = searchQuery,
                        sortColumn = sortColumn,
                        sortAscending = sortAscending,
                        quickFilterColumn = quickFilterColumn,
                        onSortColumn = { column ->
                            if (sortColumn == column) {
                                sortAscending = !sortAscending
                            } else {
                                sortColumn = column
                                sortAscending = true
                            }
                        },
                        onQuickFilter = { column ->
                            quickFilterColumn = if (quickFilterColumn == column) null else column
                            quickFilterText = ""
                        },
                        onDeviceClick = { device ->
                            onNavigateToDeviceDetail(device.id)
                        },
                        onEditDevice = { device ->
                            onNavigateToDeviceEdit(device.id)
                        },
                        onDeleteDevice = { device ->
                            scope.launch {
                                viewModel.deleteDevice(device)
                                snackbarHostState.showSnackbar("Прибор удален")
                            }
                        }
                    )
                }
            }
        }
    }
}

enum class SortColumn(val displayName: String) {
    TYPE("Тип прибора"),
    NAME("Модель"),
    INVENTORY_NUMBER("Инв. №"),
    MEASUREMENT_LIMIT("Предел измерений"),
    LOCATION("Место установки"),
    VALVE_NUMBER("Номер крана"),
    STATUS("Статус")
}

@Composable
fun DeviceStatistics(
    devices: List<Device>,
    modifier: Modifier = Modifier
) {
    val total = devices.size
    val inWork = devices.count { it.status == "В работе" }
    val inStorage = devices.count { it.status == "Хранение" }
    val lost = devices.count { it.status == "Утерян" }
    val broken = devices.count { it.status == "Испорчен" }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Всего приборов
            StatItem(
                count = total,
                label = "Всего",
                color = Color(0xFF213CF1),  // Синий
                modifier = Modifier.weight(1f)
            )

            // В работе
            StatItem(
                count = inWork,
                label = "В работе",
                color = DeviceStatusColors.Working, // Используем цвета из единого источника
                modifier = Modifier.weight(1f)
            )

            // Хранение
            StatItem(
                count = inStorage,
                label = "Хранение",
                color = DeviceStatusColors.Storage,
                modifier = Modifier.weight(1f)
            )

            // Утерян
            StatItem(
                count = lost,
                label = "Утерян",
                color = DeviceStatusColors.Lost,
                modifier = Modifier.weight(1f)
            )

            // Испорчен
            StatItem(
                count = broken,
                label = "Испорчен",
                color = DeviceStatusColors.Broken,
                modifier = Modifier.weight(1f)
            )
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
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
fun DeviceTable(
    devices: List<Device>,
    searchQuery: String,
    sortColumn: SortColumn,
    sortAscending: Boolean,
    quickFilterColumn: SortColumn?,
    onSortColumn: (SortColumn) -> Unit,
    onQuickFilter: (SortColumn) -> Unit,
    onDeviceClick: (Device) -> Unit,
    onEditDevice: (Device) -> Unit,
    onDeleteDevice: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberLazyListState()

    Column(modifier = modifier.fillMaxSize()) {
        // Заголовок таблицы
        TableHeader(
            sortColumn = sortColumn,
            sortAscending = sortAscending,
            quickFilterColumn = quickFilterColumn,
            onSortColumn = onSortColumn,
            onQuickFilter = onQuickFilter,
            horizontalScrollState = horizontalScrollState
        )

        // Тело таблицы с LazyColumn для виртуализации
        LazyColumn(
            state = verticalScrollState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(devices, key = { it.id }) { device ->
                TableRow(
                    device = device,
                    searchQuery = searchQuery,
                    onClick = { onDeviceClick(device) },
                    onEdit = { onEditDevice(device) },
                    onDelete = { onDeleteDevice(device) },
                    horizontalScrollState = horizontalScrollState
                )
            }
        }
    }
}

@Composable
fun TableHeader(
    sortColumn: SortColumn,
    sortAscending: Boolean,
    quickFilterColumn: SortColumn?,
    onSortColumn: (SortColumn) -> Unit,
    onQuickFilter: (SortColumn) -> Unit,
    horizontalScrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(horizontalScrollState)
    ) {
        // 1. Тип прибора
        TableHeaderCell(
            title = "Тип прибора",
            width = 120.dp,
            isSorted = sortColumn == SortColumn.TYPE,
            sortAscending = sortAscending,
            hasQuickFilter = quickFilterColumn == SortColumn.TYPE,
            onClick = { onSortColumn(SortColumn.TYPE) },
            onQuickFilter = { onQuickFilter(SortColumn.TYPE) }
        )

        // 2. Модель (Название)
        TableHeaderCell(
            title = "Модель",
            width = 150.dp,
            isSorted = sortColumn == SortColumn.NAME,
            sortAscending = sortAscending,
            hasQuickFilter = quickFilterColumn == SortColumn.NAME,
            onClick = { onSortColumn(SortColumn.NAME) },
            onQuickFilter = { onQuickFilter(SortColumn.NAME) }
        )

        // 3. Инв. №
        TableHeaderCell(
            title = "Инв. №",
            width = 100.dp,
            isSorted = sortColumn == SortColumn.INVENTORY_NUMBER,
            sortAscending = sortAscending,
            hasQuickFilter = quickFilterColumn == SortColumn.INVENTORY_NUMBER,
            onClick = { onSortColumn(SortColumn.INVENTORY_NUMBER) },
            onQuickFilter = { onQuickFilter(SortColumn.INVENTORY_NUMBER) }
        )

        // 4. Предел измерений
        TableHeaderCell(
            title = "Предел измер.",
            width = 120.dp,
            isSorted = sortColumn == SortColumn.MEASUREMENT_LIMIT,
            sortAscending = sortAscending,
            hasQuickFilter = quickFilterColumn == SortColumn.MEASUREMENT_LIMIT,
            onClick = { onSortColumn(SortColumn.MEASUREMENT_LIMIT) },
            onQuickFilter = { onQuickFilter(SortColumn.MEASUREMENT_LIMIT) }
        )

        // 5. Место установки
        TableHeaderCell(
            title = "Место",
            width = 120.dp,
            isSorted = sortColumn == SortColumn.LOCATION,
            sortAscending = sortAscending,
            hasQuickFilter = quickFilterColumn == SortColumn.LOCATION,
            onClick = { onSortColumn(SortColumn.LOCATION) },
            onQuickFilter = { onQuickFilter(SortColumn.LOCATION) }
        )

        // 6. Номер крана
        TableHeaderCell(
            title = "Номер крана",
            width = 100.dp,
            isSorted = sortColumn == SortColumn.VALVE_NUMBER,
            sortAscending = sortAscending,
            hasQuickFilter = quickFilterColumn == SortColumn.VALVE_NUMBER,
            onClick = { onSortColumn(SortColumn.VALVE_NUMBER) },
            onQuickFilter = { onQuickFilter(SortColumn.VALVE_NUMBER) }
        )

        // 7. Статус
        TableHeaderCell(
            title = "Статус",
            width = 100.dp,
            isSorted = sortColumn == SortColumn.STATUS,
            sortAscending = sortAscending,
            hasQuickFilter = quickFilterColumn == SortColumn.STATUS,
            onClick = { onSortColumn(SortColumn.STATUS) },
            onQuickFilter = { onQuickFilter(SortColumn.STATUS) }
        )

        // 8. Действия
        Box(
            modifier = Modifier
                .width(80.dp)
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

@Composable
fun TableHeaderCell(
    title: String,
    width: androidx.compose.ui.unit.Dp,
    isSorted: Boolean,
    sortAscending: Boolean,
    hasQuickFilter: Boolean,
    onClick: () -> Unit,
    onQuickFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
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
                        if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                        contentDescription = if (sortAscending) "По возрастанию" else "По убыванию",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Кнопка быстрой фильтрации
            IconButton(
                onClick = onQuickFilter,
                modifier = Modifier
                    .align(Alignment.End)
                    .size(20.dp)
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Быстрая фильтрация",
                    modifier = Modifier.size(14.dp),
                    tint = if (hasQuickFilter) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun TableRow(
    device: Device,
    searchQuery: String,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    horizontalScrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                color = if (device.id % 2 == 0) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                }
            )
            .horizontalScroll(horizontalScrollState)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Тип прибора
        TableCell(
            text = device.type,
            width = 120.dp,
            searchQuery = searchQuery,
            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )

        // 2. Модель (Название)
        TableCell(
            text = device.name ?: "-",
            width = 150.dp,
            searchQuery = searchQuery,
            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )

        // 3. Инв. №
        TableCell(
            text = device.inventoryNumber,
            width = 100.dp,
            searchQuery = searchQuery,
            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )

        // 4. Предел измерений
        TableCell(
            text = device.measurementLimit ?: "-",
            width = 120.dp,
            searchQuery = searchQuery,
            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )

        // 5. Место установки
        TableCell(
            text = device.location,
            width = 120.dp,
            searchQuery = searchQuery,
            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )

        // 6. Номер крана
        TableCell(
            text = device.valveNumber ?: "-",
            width = 100.dp,
            searchQuery = searchQuery,
            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )

        // 7. Статус с цветовым индикатором
        Box(
            modifier = Modifier
                .width(100.dp)
                .padding(horizontal = 8.dp)
        ) {
            StatusBadgeCompact(status = device.status)
        }

        // 8. Действия
        Box(
            modifier = Modifier
                .width(80.dp)
                .padding(horizontal = 8.dp)
        ) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Открыть") },
                        onClick = {
                            showMenu = false
                            onClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Visibility, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Изменить") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Удалить") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
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
    maxLines: Int = 2,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (searchQuery.isNotEmpty() && text.contains(searchQuery, ignoreCase = true)) {
            // Подсветка найденного текста
            val annotatedString = buildAnnotatedString {
                val lowerText = text.lowercase()
                val lowerQuery = searchQuery.lowercase()
                var startIndex = 0

                while (true) {
                    val index = lowerText.indexOf(lowerQuery, startIndex)
                    if (index == -1) break

                    // Текст до найденного
                    append(text.substring(startIndex, index))

                    // Найденный текст с подсветкой
                    withStyle(
                        SpanStyle(
                            background = highlightColor,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append(text.substring(index, index + searchQuery.length))
                    }

                    startIndex = index + searchQuery.length
                }

                // Остаток текста
                if (startIndex < text.length) {
                    append(text.substring(startIndex))
                }
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun StatusBadgeCompact(status: String) {
    val deviceStatus = DeviceStatus.fromString(status)

    Surface(
        color = deviceStatus.backgroundColor,
        shape = RoundedCornerShape(4.dp),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(
            text = getCompactStatus(status), // Используем исправленную функцию
            color = deviceStatus.textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}


private fun getCompactStatus(fullStatus: String): String {
    return when (fullStatus) {
        "В работе" -> "В работе"
        "Хранение" -> "Хранение"
        "Утерян" -> "Утерян"
        "Испорчен" -> "Испорчен"
        else -> fullStatus
    }
}

@Composable
fun SearchField(
    value: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Поиск приборов...") },
            modifier = Modifier.weight(1f),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (value.isNotEmpty()) {
                    IconButton(onClick = { onValueChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Очистить")
                    }
                }
            }
        )

        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Закрыть поиск")
        }
    }
}

@Composable
fun TypeFilterDropdown(
    currentFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val deviceTypes = remember {
        listOf(
            "Манометр",
            "Термометр",
            "Датчик давления",
            "Вольтметр",
            "Амперметр",
            "Расходомер",
            "Таймер",
            "Датчик температуры"
        )
    }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp)
        ) {
            Badge(
                containerColor = if (currentFilter != null) MaterialTheme.colorScheme.primary
                else Color.Transparent,
                modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
            ) {
                if (currentFilter != null) {
                    Text("✓", color = Color.White, fontSize = 10.sp)
                }
            }
            Icon(
                Icons.Default.FilterAlt,
                contentDescription = "Фильтр по типу",
                tint = if (currentFilter != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        "Все типы",
                        fontWeight = if (currentFilter == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    onFilterSelected(null)
                    expanded = false
                }
            )

            deviceTypes.forEach { type ->
                DropdownMenuItem(
                    text = {
                        Text(
                            type,
                            fontWeight = if (currentFilter == type) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onFilterSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun StatusFilterDropdown(
    currentFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val statuses = DeviceStatus.ALL_STATUSES // Используем единый источник

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp)
        ) {
            Badge(
                containerColor = if (currentFilter != null) MaterialTheme.colorScheme.primary
                else Color.Transparent,
                modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
            ) {
                if (currentFilter != null) {
                    Text("✓", color = Color.White, fontSize = 10.sp)
                }
            }
            Icon(
                Icons.Default.FilterList,
                contentDescription = "Фильтр по статусу",
                tint = if (currentFilter != null) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(
                        "Все статусы",
                        fontWeight = if (currentFilter == null) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    onFilterSelected(null)
                    expanded = false
                }
            )

            statuses.forEach { status ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusBadgeCompact(status = status)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                status,
                                fontWeight = if (currentFilter == status) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    },
                    onClick = {
                        onFilterSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun ActiveFiltersBadge(
    typeFilter: String?,
    statusFilter: String?,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFilters by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FilterAlt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = buildActiveFiltersText(typeFilter, statusFilter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (showFilters) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!showFilters && (typeFilter != null && statusFilter != null)) {
                    Text(
                        text = " (ещё...)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row {
                if (showFilters) {
                    IconButton(
                        onClick = { showFilters = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ExpandLess,
                            contentDescription = "Свернуть",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else if (typeFilter != null && statusFilter != null) {
                    IconButton(
                        onClick = { showFilters = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = "Развернуть",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onClearFilters,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Очистить фильтры",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun buildActiveFiltersText(typeFilter: String?, statusFilter: String?): String {
    val filters = mutableListOf<String>()

    if (typeFilter != null) {
        filters.add("Тип: $typeFilter")
    }

    if (statusFilter != null) {
        filters.add("Статус: $statusFilter")
    }

    return if (filters.isEmpty()) {
        "Нет активных фильтров"
    } else {
        "Фильтры: ${filters.joinToString(", ")}"
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Загрузка приборов...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EmptyDevicesState(
    onAddDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Devices,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Нет приборов",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Добавьте первый прибор, нажав на кнопку ниже",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onAddDevice,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить прибор")
            }
        }
    }
}