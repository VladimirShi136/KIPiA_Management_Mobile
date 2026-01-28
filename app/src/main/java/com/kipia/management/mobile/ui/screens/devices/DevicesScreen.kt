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
import com.kipia.management.mobile.viewmodel.DevicesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Основной экран для отображения и управления приборами
 */
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
    val searchQuery = uiState.searchQuery
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var sortColumn by remember { mutableStateOf(SortColumn.INVENTORY_NUMBER) }
    var sortAscending by remember { mutableStateOf(true) }
    val deleteDialogData by deleteViewModel.showDeleteDialog.collectAsStateWithLifecycle()
    val verticalScrollState = rememberLazyListState()

    // ★★★★ УПРОЩЕННАЯ ЛОГИКА СКРЫТИЯ BOTTOM NAV ★★★★
    val shouldShowBottomNav by remember {
        derivedStateOf {
            with(verticalScrollState) {
                // Если мы в самом верху списка (первый элемент и не было скролла)
                firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
            }
        }
    }

    // Обновляем состояние BottomNav
    LaunchedEffect(shouldShowBottomNav) {
        Timber.d("DevicesScreen: Обновление BottomNav видимости: $shouldShowBottomNav")
        updateBottomNavVisibility(shouldShowBottomNav)
    }

    // ★★★★ КНОПКА "ВВЕРХ" ★★★★
    val showScrollToTopButton by remember {
        derivedStateOf {
            !shouldShowBottomNav
        }
    }

    // Функция для удаления устройства
    val deleteDeviceAction: (Device) -> Unit = { device ->
        scope.launch {
            deleteViewModel.checkAndShowDialog(device)
        }
    }

    // ★★★★ ОБРАБОТКА УВЕДОМЛЕНИЙ ★★★★
    LaunchedEffect(Unit) {
        notificationManager.notification.collect { notification ->
            // Пропускаем пустые уведомления
            if (notification is NotificationManager.Notification.None) {
                println("DEBUG DevicesScreen: Получено пустое уведомление - пропускаем")
                return@collect
            }

            println("DEBUG DevicesScreen: Получено уведомление: $notification")

            val message = when (notification) {
                is NotificationManager.Notification.DeviceSaved -> {
                    println("DEBUG DevicesScreen: Показываем уведомление о сохранении")
                    "Прибор '${notification.deviceName}' сохранен"
                }

                is NotificationManager.Notification.DeviceDeleted -> {
                    println("DEBUG DevicesScreen: Показываем уведомление об удалении")
                    if (notification.withScheme) {
                        "Прибор '${notification.deviceName}' и схема удалены"
                    } else {
                        "Прибор '${notification.deviceName}' удален"
                    }
                }

                is NotificationManager.Notification.Error -> {
                    println("DEBUG DevicesScreen: Показываем уведомление об ошибке")
                    "Ошибка: ${notification.message}"
                }

                NotificationManager.Notification.None -> return@collect
            }

            scope.launch {
                println("DEBUG DevicesScreen: Показываем snackbar: $message")
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )

                // ★★★★ ВАЖНО: Очищаем replay cache после показа ★★★★
                delay(100) // Небольшая задержка для гарантии показа
                notificationManager.clearLastNotification()
            }
        }
    }

    // ★★★★ ТОЛЬКО СОРТИРОВКА (фильтрация в ViewModel) ★★★★
    val sortedDevices = remember(
        devices, // ViewModel УЖЕ отфильтровал устройства
        sortColumn,
        sortAscending
    ) {
        devices.sortedWith(
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
    }

    // ★★★★ ВСЁ В ОДНОМ Box ★★★★
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
            // ★★★★ СТАТИСТИКА ★★★★
            DeviceStatistics(
                devices = devices,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(bottom = 6.dp)
            )

            // ★★★★ АКТИВНЫЕ ФИЛЬТРЫ ★★★★
            if (searchQuery.isNotEmpty() || uiState.locationFilter != null || uiState.statusFilter != null) {
                ActiveFiltersBadge(
                    searchQuery = searchQuery,
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
                uiState.isLoading -> {
                    LoadingState()
                }

                sortedDevices.isEmpty() -> {
                    EmptyDevicesState(
                        onAddDevice = { onNavigateToDeviceEdit(null) }
                    )
                }

                else -> {
                    DeviceTableWithScroll(
                        devices = sortedDevices,
                        searchQuery = searchQuery,
                        sortColumn = sortColumn,
                        sortAscending = sortAscending,
                        verticalScrollState = verticalScrollState,
                        onSortColumn = { column ->
                            if (sortColumn == column) {
                                sortAscending = !sortAscending
                            } else {
                                sortColumn = column
                                sortAscending = true
                            }
                        },
                        onDeviceClick = { device ->
                            onNavigateToDeviceDetail(device.id)
                        },
                        onEditDevice = { device ->
                            onNavigateToDeviceEdit(device.id)
                        },
                        onDeleteDevice = deleteDeviceAction,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    )
                }
            }
        }

        // ★★★★ COLUMN ДЛЯ ВЕРТИКАЛЬНОГО РАСПОЛОЖЕНИЯ КНОПОК ★★★★
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 46.dp,
                    bottom = 30.dp
                )
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ★★★★ КНОПКА "ВВЕРХ" (появляется когда навигация скрыта) ★★★★
            AnimatedVisibility(
                visible = showScrollToTopButton,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch {
                            verticalScrollState.animateScrollToItem(0)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(48.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Наверх",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // ★★★★ КНОПКА ДОБАВЛЕНИЯ ПРИБОРА ★★★★
            FloatingActionButton(
                onClick = { onNavigateToDeviceEdit(null) },
                containerColor = Color(0xFF2ECC71), // Зеленый цвет #2ecc71
                contentColor = Color.White, // Белый цвет для иконки
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Добавить прибор",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // ★★★★ ДИАЛОГ УДАЛЕНИЯ ★★★★
        deleteDialogData?.let { dialogData ->
            DeviceDeleteDialog(
                device = dialogData.device,
                scheme = dialogData.scheme,
                deviceCountInLocation = dialogData.deviceCountInLocation,
                isLastInLocation = dialogData.isLastInLocation,
                onDismiss = {
                    deleteViewModel.dismissDialog()
                },
                onConfirm = { deleteScheme ->
                    scope.launch {
                        try {
                            // ★★★★ Передаем флаг deleteScheme в ViewModel ★★★★
                            // ViewModel сам решит, удалять ли схему
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
fun DeviceDeleteDialog(
    device: Device,
    scheme: Scheme?,
    deviceCountInLocation: Int,
    isLastInLocation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (deleteScheme: Boolean) -> Unit
) {
    val messageContent = @Composable {
        Column(
            modifier = Modifier
                .padding(16.dp) // ← ВСЕГДА фиксированный padding — НЕ fillMaxWidth!
                .heightIn(max = 200.dp) // ← Ограничиваем высоту, чтобы не "вылазил"
        ) {
            if (isLastInLocation) {
                Text(
                    text = "Вы удаляете устройство:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${device.getDisplayName()} (${device.inventoryNumber})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (scheme != null) {
                    Text(
                        text = "Это последнее устройство в локации:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "'${scheme.name}'",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Text(
                        text = "Что делать со схемой этой локации?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = "Это последнее устройство в локации '${device.location}'.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Схема не привязана.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Вы уверены, что хотите удалить устройство?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${device.getDisplayName()} (${device.inventoryNumber})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "В локации '${device.location}' останется ещё ${deviceCountInLocation - 1} приборов.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Схема останется без изменений.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isLastInLocation) "Удаление устройства" else "Подтверждение удаления",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = { messageContent() }, // ← Передаём как lambda
        confirmButton = {
            Button(
                onClick = {
                    if (isLastInLocation && scheme != null) {
                        onConfirm(true) // "Удалить с схемой"
                    } else {
                        onConfirm(false) // "Удалить устройство"
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(
                    text = if (isLastInLocation && scheme != null) {
                        "Удалить с схемой"
                    } else {
                        "Удалить устройство"
                    },
                    color = Color.White
                )
            }
        },
        dismissButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when {
                    // Случай 1: Последний прибор с привязанной схемой (3 кнопки)
                    isLastInLocation && scheme != null -> {
                        // Кнопка "Только устройство"
                        Button(
                            onClick = { onConfirm(false) }, // Удалить только устройство
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Только устройство")
                        }

                        // Кнопка "Отмена"
                        Button(onClick = onDismiss) {
                            Text("Отмена")
                        }
                    }

                    // Случай 2: НЕ последний прибор или нет схемы (2 кнопки)
                    else -> {
                        // Кнопка "Отмена" - просто закрывает диалог
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Отмена")
                        }
                    }
                }
            }
        }
    )
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
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Всего приборов
            StatItem(
                count = total,
                label = "Всего",
                color = Color(0xFF213CF1),
                modifier = Modifier.weight(1f)
            )

            // В работе
            StatItem(
                count = inWork,
                label = "В работе",
                color = DeviceStatusColors.Working,
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

    Column(modifier = modifier.fillMaxSize()) {
        // Заголовок таблицы с верхними скругленными углами
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    )
                )
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            TableHeader(
                sortColumn = sortColumn,
                sortAscending = sortAscending,
                onSortColumn = onSortColumn,
                horizontalScrollState = horizontalScrollState,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Тело таблицы с LazyColumn и нижними скругленными углами
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(
                        topStart = 0.dp,
                        topEnd = 0.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    )
                )
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
        ) {
            LazyColumn(
                state = verticalScrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 1.dp)
            ) {
                itemsIndexed(devices, key = { _, device -> device.id }) { index, device ->
                    TableRowWithDivider(
                        device = device,
                        index = index, // ← ВАЖНО: передаем индекс из itemsIndexed
                        searchQuery = searchQuery,
                        onClick = { onDeviceClick(device) },
                        onEdit = { onEditDevice(device) },
                        onDelete = { onDeleteDevice(device) },
                        horizontalScrollState = horizontalScrollState,
                        showDivider = index < devices.size - 1 // ← Упрощенная логика
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
    horizontalScrollState: ScrollState,
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        TableRow(
            device = device,
            index = index,
            searchQuery = searchQuery,
            onClick = onClick,
            onEdit = onEdit,
            onDelete = onDelete,
            horizontalScrollState = horizontalScrollState,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp) // Уменьшенная высота строки
        )
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
fun TableHeader(
    sortColumn: SortColumn,
    sortAscending: Boolean,
    onSortColumn: (SortColumn) -> Unit,
    horizontalScrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .horizontalScroll(horizontalScrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Тип прибора
        TableHeaderCell(
            title = "Тип прибора",
            width = 120.dp,
            isSorted = sortColumn == SortColumn.TYPE,
            sortAscending = sortAscending,
            onClick = { onSortColumn(SortColumn.TYPE) }
        )

        // 2. Модель
        TableHeaderCell(
            title = "Модель",
            width = 150.dp,
            isSorted = sortColumn == SortColumn.NAME,
            sortAscending = sortAscending,
            onClick = { onSortColumn(SortColumn.NAME) }
        )

        // 3. Инв. №
        TableHeaderCell(
            title = "Инв. №",
            width = 100.dp,
            isSorted = sortColumn == SortColumn.INVENTORY_NUMBER,
            sortAscending = sortAscending,
            onClick = { onSortColumn(SortColumn.INVENTORY_NUMBER) }
        )

        // 4. Предел измерений
        TableHeaderCell(
            title = "Предел измер.",
            width = 120.dp,
            isSorted = sortColumn == SortColumn.MEASUREMENT_LIMIT,
            sortAscending = sortAscending,
            onClick = { onSortColumn(SortColumn.MEASUREMENT_LIMIT) }
        )

        // 5. Место установки
        TableHeaderCell(
            title = "Место",
            width = 120.dp,
            isSorted = sortColumn == SortColumn.LOCATION,
            sortAscending = sortAscending,
            onClick = { onSortColumn(SortColumn.LOCATION) }
        )

        // 6. Номер крана
        TableHeaderCell(
            title = "Номер крана",
            width = 100.dp,
            isSorted = sortColumn == SortColumn.VALVE_NUMBER,
            sortAscending = sortAscending,
            onClick = { onSortColumn(SortColumn.VALVE_NUMBER) }
        )

        // 7. Статус
        TableHeaderCell(
            title = "Статус",
            width = 100.dp,
            isSorted = sortColumn == SortColumn.STATUS,
            sortAscending = sortAscending,
            onClick = { onSortColumn(SortColumn.STATUS) }
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(40.dp),
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
                    if (sortAscending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                    contentDescription = if (sortAscending) "По возрастанию" else "По убыванию",
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
    index: Int,
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
            .height(40.dp)
            .clickable(onClick = onClick)
            .background(
                color = if (index % 2 == 0) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                }
            )
            .horizontalScroll(horizontalScrollState),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Тип прибора
        TableCell(
            text = device.type,
            width = 120.dp,
            searchQuery = searchQuery,
            highlightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )

        // 2. Модель
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

        // 7. Статус
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
                .padding(horizontal = 4.dp)
        ) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        modifier = Modifier.size(18.dp)
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
                            onDelete() // ← ВАЖНО: вызывается ли этот колбэк?
                            println("DEBUG: Кнопка 'Удалить' нажата для устройства ${device.id}")
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
            val annotatedString = buildAnnotatedString {
                val lowerText = text.lowercase()
                val lowerQuery = searchQuery.lowercase()
                var startIndex = 0

                while (true) {
                    val index = lowerText.indexOf(lowerQuery, startIndex)
                    if (index == -1) break

                    append(text.substring(startIndex, index))
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

                if (startIndex < text.length) {
                    append(text.substring(startIndex))
                }
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
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
        modifier = Modifier
            .height(24.dp) // Уменьшенная высота бейджа
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = getCompactStatus(status),
            color = deviceStatus.textColor,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Medium,
                fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.9f
            ),
            modifier = Modifier
                .padding(horizontal = 6.dp)
                .wrapContentHeight(Alignment.CenterVertically),
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
                    text = buildActiveFiltersText(searchQuery, locationFilter, statusFilter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

private fun buildActiveFiltersText(
    searchQuery: String,
    locationFilter: String?,
    statusFilter: String?
): String {
    val filters = mutableListOf<String>()

    if (searchQuery.isNotEmpty()) {
        filters.add("Поиск: \"$searchQuery\"")
    }

    if (locationFilter != null) {
        filters.add("Место: $locationFilter")
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
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Добавить прибор")
            }
        }
    }
}