package com.kipia.management.mobile.ui.screens.devices

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.viewmodel.DevicesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Приборы") },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                devices.isEmpty() -> {
                    EmptyDevicesState(
                        onAddDevice = { onNavigateToDeviceEdit(null) }
                    )
                }
                else -> {
                    DevicesList(
                        devices = devices,
                        searchQuery = searchQuery,
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
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
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
    val filterText = buildString {
        if (typeFilter != null) append("Тип: $typeFilter")
        if (statusFilter != null) {
            if (isNotEmpty()) append(", ")
            append("Статус: $statusFilter")
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shadowElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = filterText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onClearFilters,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Сбросить фильтры",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun TypeFilterDropdown(
    currentFilter: String?,
    onFilterSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(Icons.Default.Category, contentDescription = "Фильтр по типу")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Все типы") },
                onClick = {
                    onFilterSelected(null)
                    expanded = false
                }
            )
            Device.TYPES.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type) },
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

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Icon(Icons.Default.FilterAlt, contentDescription = "Фильтр по статусу")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Все статусы") },
                onClick = {
                    onFilterSelected(null)
                    expanded = false
                }
            )
            Device.STATUSES.forEach { status ->
                DropdownMenuItem(
                    text = { Text(status) },
                    onClick = {
                        onFilterSelected(status)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text("Поиск по инв. №, названию...") },
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            singleLine = true
        )

        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Закрыть поиск")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DevicesList(
    devices: List<Device>,
    searchQuery: String,
    onDeviceClick: (Device) -> Unit,
    onEditDevice: (Device) -> Unit,
    onDeleteDevice: (Device) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredDevices = if (searchQuery.isBlank()) {
        devices
    } else {
        devices.filter { device ->
            device.inventoryNumber.contains(searchQuery, ignoreCase = true) ||
                    device.name?.contains(searchQuery, ignoreCase = true) == true ||
                    device.manufacturer?.contains(searchQuery, ignoreCase = true) == true ||
                    device.location.contains(searchQuery, ignoreCase = true) ||
                    device.valveNumber?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(filteredDevices, key = { it.id }) { device ->
            DeviceCard(
                device = device,
                onClick = { onDeviceClick(device) },
                onEdit = { onEditDevice(device) },
                onDelete = { onDeleteDevice(device) },
                modifier = Modifier.animateItemPlacement()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceCard(
    device: Device,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Фото устройства (если есть)
            device.getMainPhoto()?.let { photoUrl ->
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Фото прибора",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.width(16.dp))
            }

            // Информация об устройстве
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Заголовок с инвентарным номером
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = device.getDisplayName(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "Инв. №: ${device.inventoryNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
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

                Spacer(modifier = Modifier.height(8.dp))

                // Основная информация
                DeviceInfoRow(label = "Тип:", value = device.type)
                Spacer(modifier = Modifier.height(4.dp))

                device.manufacturer?.let { manufacturer ->
                    DeviceInfoRow(label = "Производитель:", value = manufacturer)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                DeviceInfoRow(label = "Место:", value = device.location)
                Spacer(modifier = Modifier.height(4.dp))

                // Дополнительная информация в одной строке
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    device.year?.let { year ->
                        Text(
                            text = "Год: $year",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    device.measurementLimit?.let { limit ->
                        Text(
                            text = "Предел: $limit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    device.accuracyClass?.let { accuracy ->
                        Text(
                            text = "Кл. точности: $accuracy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Статус с цветным индикатором
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusIndicator(status = device.status)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = device.status,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Количество фото (если есть)
                val photoCount = device.getPhotoList().size
                if (photoCount > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Photo,
                            contentDescription = "Фото",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$photoCount фото",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatusIndicator(status: String) {
    val color = when (status) {
        "В работе" -> Color(0xFF4CAF50)      // Зеленый
        "На ремонте" -> Color(0xFFFF9800)    // Оранжевый
        "Списан" -> Color(0xFFF44336)        // Красный
        "В резерве" -> Color(0xFF9E9E9E)     // Серый
        else -> Color(0xFF757575)            // Темно-серый
    }

    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun EmptyDevicesState(
    onAddDevice: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Devices,
            contentDescription = "Нет приборов",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Нет приборов",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Добавьте первый прибор, чтобы начать работу",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAddDevice,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Добавить прибор")
        }
    }
}