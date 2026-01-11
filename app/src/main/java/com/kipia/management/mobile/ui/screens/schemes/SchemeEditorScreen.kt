package com.kipia.management.mobile.ui.screens.schemes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.components.scheme.SchemeCanvas
import com.kipia.management.mobile.viewmodel.EditorMode
import com.kipia.management.mobile.viewmodel.SchemeEditorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeEditorScreen(
    schemeId: Int?,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: SchemeEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val editorMode by viewModel.editorMode.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // Состояние интерфейса
    var showShapeProperties by remember { mutableStateOf(false) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showGridSettings by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            SchemeEditorTopBar(
                schemeName = uiState.scheme.name,
                isDirty = uiState.isDirty,
                onSchemeNameChanged = { viewModel.updateSchemeName(it) },
                onNavigateBack = {
                    if (uiState.isDirty) {
                        showExitDialog = true
                    } else {
                        onNavigateBack()
                    }
                },
                onSave = {
                    scope.launch {
                        val success = viewModel.saveScheme()
                        if (success) onSaveSuccess()
                    }
                },
                onShowSettings = { showSettingsDialog = true },
                onShowProperties = { showPropertiesDialog = true }
            )
        },
        floatingActionButton = {
            if (editorMode == EditorMode.SELECT) {
                FloatingActionButton(
                    onClick = { showAddDeviceDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить прибор")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Основной холст
            SchemeCanvas(
                schemeData = uiState.schemeData,
                devices = devices,
                schemeDevices = uiState.schemeData.devices,
                shapes = uiState.schemeData.shapes.map { it.toComposeShape() },
                editorMode = editorMode,
                onDeviceDrag = { deviceId, position ->
                    viewModel.updateDevicePosition(deviceId, position)
                },
                onDeviceClick = { deviceId ->
                    viewModel.setSelectedDevice(deviceId)
                    viewModel.setEditorMode(EditorMode.SELECT)
                },
                onShapeClick = { point ->
                    // TODO: Реализовать выбор фигуры через viewModel
                    // val selectedShape = viewModel.selectShapeAt(point)
                    // if (selectedShape != null) {
                    //     showShapeProperties = true
                    // }
                },
                onShapeDrag = { deltaX, deltaY ->
                    // TODO: Реализовать через viewModel
                    // viewModel.moveSelectedShape(deltaX, deltaY)
                },
                onCanvasClick = {
                    viewModel.setSelectedDevice(null)
                    // TODO: Очистить выбор фигуры через viewModel
                },
                selectedDeviceId = uiState.selectedDeviceId,
                selectedShape = null,
                modifier = Modifier.fillMaxSize()
            )

            // Нижняя панель настроек
            EditorToolbar(
                onToggleGrid = { viewModel.toggleGrid() },
                onShowGridSettings = { showGridSettings = true },
                onZoomIn = { /* TODO: Увеличить масштаб */ },
                onZoomOut = { /* TODO: Уменьшить масштаб */ },
                onResetView = { /* TODO: Сбросить вид */ },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }

        // Диалог настроек сетки
        if (showGridSettings) {
            GridSettingsDialog(
                gridEnabled = uiState.schemeData.gridEnabled,
                gridSize = uiState.schemeData.gridSize,
                onGridToggled = { viewModel.toggleGrid() },
                onGridSizeChanged = { size -> viewModel.updateGridSize(size) },
                onDismiss = { showGridSettings = false }
            )
        }

        // Диалог экспорта
        if (showExportDialog) {
            ExportSchemeDialog(
                schemeName = uiState.scheme.name,
                onExportAsImage = {
                    // TODO: Экспорт схемы как изображение
                    showExportDialog = false
                },
                onExportAsPDF = {
                    // TODO: Экспорт схемы как PDF
                    showExportDialog = false
                },
                onExportToDesktop = {
                    // TODO: Экспорт для Desktop версии
                    showExportDialog = false
                },
                onDismiss = { showExportDialog = false }
            )
        }

        // Диалог выхода
        if (showExitDialog) {
            ExitConfirmationDialog(
                onSaveAndExit = {
                    scope.launch {
                        val success = viewModel.saveScheme()
                        if (success) onSaveSuccess()
                        showExitDialog = false
                    }
                },
                onExitWithoutSaving = {
                    onNavigateBack()
                    showExitDialog = false
                },
                onDismiss = { showExitDialog = false }
            )
        }

        // Диалог добавления устройства
        if (showAddDeviceDialog) {
            // Фильтруем устройства, которые еще не добавлены на схему
            val availableDevices = devices.filter { device ->
                uiState.schemeData.devices.none { it.deviceId == device.id }
            }

            AddDeviceDialog(
                devices = availableDevices,
                onDeviceSelected = { device ->
                    // Добавляем устройство в центр холста
                    val centerX = uiState.schemeData.width / 2f
                    val centerY = uiState.schemeData.height / 2f
                    viewModel.addDevice(device.id, Offset(centerX, centerY))
                    showAddDeviceDialog = false
                },
                onDismiss = { showAddDeviceDialog = false }
            )
        }

        // Диалог настроек
        if (showSettingsDialog) {
            AlertDialog(
                onDismissRequest = { showSettingsDialog = false },
                title = { Text("Настройки редактора") },
                text = {
                    Text("Настройки редактора схем будут реализованы позже")
                },
                confirmButton = {
                    TextButton(onClick = { showSettingsDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }

        // Диалог свойств схемы
        if (showPropertiesDialog) {
            AlertDialog(
                onDismissRequest = { showPropertiesDialog = false },
                title = { Text("Свойства схемы") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Название: ${uiState.scheme.name}")
                        Text("Описание: ${uiState.scheme.description ?: "Нет описания"}")
                        Text("Размер: ${uiState.schemeData.width} x ${uiState.schemeData.height}")
                        Text("Устройств: ${uiState.schemeData.devices.size}")
                        Text("Фигур: ${uiState.schemeData.shapes.size}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showPropertiesDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeEditorTopBar(
    schemeName: String,
    isDirty: Boolean,
    onSchemeNameChanged: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onShowSettings: () -> Unit,
    onShowProperties: () -> Unit
) {
    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(schemeName) }

    CenterAlignedTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isDirty) {
                    Box(
                        modifier = Modifier.size(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                }

                if (isEditingName) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        singleLine = true,
                        modifier = Modifier
                            .width(200.dp)
                            .onFocusChanged { focusState ->
                                if (!focusState.isFocused) {
                                    isEditingName = false
                                    onSchemeNameChanged(tempName)
                                }
                            }
                    )
                } else {
                    Text(
                        text = schemeName,
                        modifier = Modifier.clickable {
                            tempName = schemeName
                            isEditingName = true
                        }
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
        },
        actions = {
            // Описание
            IconButton(onClick = { /* TODO: Редактировать описание */ }) {
                Icon(Icons.Default.Description, contentDescription = "Описание")
            }

            // Свойства схемы
            IconButton(onClick = onShowProperties) {
                Icon(Icons.Default.Tune, contentDescription = "Свойства")
            }

            // Настройки
            IconButton(onClick = onShowSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Настройки")
            }

            // Сохранить
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Save, contentDescription = "Сохранить")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridSettingsDialog(
    gridEnabled: Boolean,
    gridSize: Int,
    onGridToggled: () -> Unit,
    onGridSizeChanged: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var localGridSize by remember { mutableIntStateOf(gridSize) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Настройки сетки") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Включить сетку")
                    Switch(
                        checked = gridEnabled,
                        onCheckedChange = { onGridToggled() }
                    )
                }

                if (gridEnabled) {
                    Column {
                        Text("Размер сетки: ${localGridSize}px")
                        Slider(
                            value = localGridSize.toFloat(),
                            onValueChange = { localGridSize = it.toInt() },
                            onValueChangeFinished = { onGridSizeChanged(localGridSize) },
                            valueRange = 10f..200f,
                            steps = 19
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(10, 25, 50, 100).forEach { size ->
                                FilterChip(
                                    selected = localGridSize == size,
                                    onClick = {
                                        localGridSize = size
                                        onGridSizeChanged(size)
                                    },
                                    label = { Text("${size}px") }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSchemeDialog(
    schemeName: String,
    onExportAsImage: () -> Unit,
    onExportAsPDF: () -> Unit,
    onExportToDesktop: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Экспорт схемы") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Экспортировать схему '$schemeName':")

                ListItem(
                    headlineContent = { Text("Как изображение (PNG/JPG)") },
                    leadingContent = {
                        Icon(Icons.Default.Image, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = onExportAsImage)
                )

                ListItem(
                    headlineContent = { Text("Как PDF документ") },
                    leadingContent = {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = onExportAsPDF)
                )

                ListItem(
                    headlineContent = { Text("Для Desktop версии") },
                    leadingContent = {
                        Icon(Icons.Default.Computer, contentDescription = null)
                    },
                    modifier = Modifier.clickable(onClick = onExportToDesktop)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun EditorToolbar(
    onToggleGrid: () -> Unit,
    onShowGridSettings: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetView: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Сетка
            IconButton(onClick = onToggleGrid) {
                Icon(Icons.Default.GridOn, contentDescription = "Сетка")
            }

            // Настройки сетки
            IconButton(onClick = onShowGridSettings) {
                Icon(Icons.Default.Tune, contentDescription = "Настройки сетки")
            }

            HorizontalDivider(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
            )

            // Масштаб
            IconButton(onClick = onZoomOut) {
                Icon(Icons.Default.ZoomOut, contentDescription = "Уменьшить")
            }

            IconButton(onClick = onResetView) {
                Icon(Icons.Default.FilterCenterFocus, contentDescription = "Сбросить вид")
            }

            IconButton(onClick = onZoomIn) {
                Icon(Icons.Default.ZoomIn, contentDescription = "Увеличить")
            }

            HorizontalDivider(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
            )

            // Слои
            IconButton(onClick = { /* TODO: Управление слоями */ }) {
                Icon(Icons.Default.Layers, contentDescription = "Слои")
            }

            // Выравнивание
            IconButton(onClick = { /* TODO: Выравнивание */ }) {
                Icon(Icons.Default.AlignHorizontalCenter, contentDescription = "Выравнивание")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExitConfirmationDialog(
    onSaveAndExit: () -> Unit,
    onExitWithoutSaving: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Есть несохраненные изменения") },
        text = { Text("Сохранить изменения перед выходом?") },
        confirmButton = {
            Button(onClick = onSaveAndExit) {
                Text("Сохранить и выйти")
            }
        },
        dismissButton = {
            TextButton(onClick = onExitWithoutSaving) {
                Text("Выйти без сохранения")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceDialog(
    devices: List<Device>,
    onDeviceSelected: (Device) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить прибор") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (devices.isEmpty()) {
                    Text("Все доступные приборы уже добавлены на схему")
                } else {
                    devices.forEach { device ->
                        Card(
                            onClick = { onDeviceSelected(device) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = when (device.type) {
                                        "Теплосчетчик" -> Icons.Default.Thermostat
                                        "Водосчетчик" -> Icons.Default.WaterDrop
                                        "Электросчетчик" -> Icons.Default.FlashOn
                                        "Газосчетчик" -> Icons.Default.GasMeter
                                        "Датчик" -> Icons.Default.Sensors
                                        "Регулятор" -> Icons.Default.Tune
                                        else -> Icons.Default.Devices
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.name ?: device.type,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${device.type} • Инв. №${device.inventoryNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }

                                // Статус устройства
                                Surface(
                                    color = when (device.status) {
                                        "В работе" -> Color(0xFFE8F5E9)
                                        "На ремонте" -> Color(0xFFFFF3E0)
                                        "Списан" -> Color(0xFFFFEBEE)
                                        "В резерве" -> Color(0xFFF5F5F5)
                                        else -> Color(0xFFF5F5F5)
                                    },
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = device.status,
                                        color = when (device.status) {
                                            "В работе" -> Color(0xFF2E7D32)
                                            "На ремонте" -> Color(0xFFEF6C00)
                                            "Списан" -> Color(0xFFC62828)
                                            "В резерве" -> Color(0xFF616161)
                                            else -> Color(0xFF616161)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}