package com.kipia.management.mobile.ui.screens.schemes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.components.scheme.*
import com.kipia.management.mobile.ui.components.scheme.shapes.*
import com.kipia.management.mobile.viewmodel.EditorMode
import com.kipia.management.mobile.viewmodel.SchemeEditorViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeEditorScreen(
    schemeId: Int?,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: SchemeEditorViewModel = hiltViewModel(),
    topAppBarController: com.kipia.management.mobile.ui.components.topappbar.TopAppBarController? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val editorMode by viewModel.editorMode.collectAsStateWithLifecycle()
    val showPropertiesPanel by viewModel.showPropertiesPanel.collectAsStateWithLifecycle()
    val schemeLocation = uiState.scheme.name
    val scope = rememberCoroutineScope()
    val devicesForScheme by viewModel.devicesForScheme.collectAsStateWithLifecycle()

    val shapes by remember(uiState.schemeData.shapes) {
        derivedStateOf {
            uiState.schemeData.shapes.map { it.toComposeShape() }
        }
    }

    // Локальное состояние
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showGridSettings by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showLayersDialog by remember { mutableStateOf(false) }
    var showAlignmentDialog by remember { mutableStateOf(false) }

    // Настраиваем TopAppBar через контроллер
    LaunchedEffect(topAppBarController, uiState, viewModel) {
        topAppBarController?.setForScreen("scheme_editor", buildMap {
            put("schemeName", uiState.scheme.name)
            put("isNewScheme", uiState.isNewScheme)
            put("isDirty", uiState.isDirty)
            put("canSave", true)
            put("onBackClick", {
                if (uiState.isDirty) {
                    showExitDialog = true
                } else {
                    onNavigateBack()
                }
            })
            put("onSaveClick", {
                scope.launch {
                    val success = viewModel.saveScheme()
                    if (success) onSaveSuccess()
                }
            })
            put("onPropertiesClick", { showPropertiesDialog = true })
            put("onEditorSettingsClick", { showSettingsDialog = true })
        })
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CanvasControls(
                canUndo = viewModel.canUndo(),
                canRedo = viewModel.canRedo(),
                selectedShape = uiState.selectedShape,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                onToggleGrid = { viewModel.toggleGrid() },
                onShowGridSettings = { showGridSettings = true },
                onZoomIn = { /* Зум внутри SchemeCanvas */ },
                onZoomOut = { /* Зум внутри SchemeCanvas */ },
                onResetView = { /* Зум внутри SchemeCanvas */ },
                onShowLayers = { showLayersDialog = true },
                onShowAlignment = { showAlignmentDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                SchemeCanvas(
                    schemeData = uiState.schemeData,
                    devices = devicesForScheme,
                    schemeDevices = uiState.schemeData.devices,
                    shapes = shapes,
                    editorMode = editorMode,
                    onDeviceDrag = { deviceId, position ->
                        viewModel.updateDevicePosition(deviceId, position)
                    },
                    onDeviceClick = { deviceId ->
                        viewModel.setSelectedDevice(deviceId)
                    },
                    onShapeClick = { shape ->
                        viewModel.selectShape(shape)
                    },
                    onCanvasClick = { position ->
                        when (editorMode) {
                            EditorMode.SELECT -> {
                                viewModel.clearSelection()
                            }
                            EditorMode.RECTANGLE -> viewModel.addRectangleAt(position)
                            EditorMode.LINE -> viewModel.addLineAt(position)
                            EditorMode.ELLIPSE -> viewModel.addEllipseAt(position)
                            EditorMode.RHOMBUS -> viewModel.addRhombusAt(position)
                            EditorMode.TEXT -> viewModel.showTextInputDialog(position)
                            else -> {}
                        }
                    },
                    onShapeDrag = { shapeId, delta ->
                        viewModel.moveShape(shapeId, delta)
                    },
                    onShapeResize = { shapeId, newWidth, newHeight ->
                        viewModel.resizeShape(shapeId, newWidth, newHeight)
                    },
                    // УБИРАЕМ onSetDraggingState - его больше нет!
                    selectedDeviceId = uiState.selectedDeviceId,
                    selectedShape = uiState.selectedShape,
                    modifier = Modifier.fillMaxSize()
                )

                // Панель свойств - показываем только по кнопке
                if (showPropertiesPanel && uiState.selectedShape != null) {
                    ShapePropertiesPanel(
                        shape = uiState.selectedShape,
                        onUpdateShape = { updatedShape ->
                            viewModel.updateShapeProperties(updatedShape)
                        },
                        onClose = {
                            viewModel.togglePropertiesPanel() // Закрываем панель
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 16.dp, end = 16.dp)
                    )
                }

                // Панель свойств устройства
                if (uiState.selectedDeviceId != null) {
                    val device = devicesForScheme.find { it.id == uiState.selectedDeviceId }
                    val schemeDevice =
                        uiState.schemeData.devices.find { it.deviceId == uiState.selectedDeviceId }

                    if (device != null && schemeDevice != null) {
                        DevicePropertiesPanel(
                            device = device,
                            schemeDevice = schemeDevice,
                            onUpdatePosition = { x, y ->
                                viewModel.updateDevicePosition(device.id, Offset(x, y))
                            },
                            onUpdateRotation = { rotation ->
                                viewModel.updateDeviceRotation(device.id, rotation)
                            },
                            onUpdateScale = { _ -> },
                            onRemoveDevice = {
                                viewModel.removeDevice(device.id)
                            },
                            onClose = {
                                viewModel.setSelectedDevice(null)
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 16.dp, end = 16.dp)
                        )
                    }
                }
            }

            // BOTTOM SHAPE TOOLBAR
            BottomShapeToolbar(
                editorMode = editorMode,
                selectedShape = uiState.selectedShape,
                onModeChanged = { viewModel.setEditorMode(it) },
                onAddDevice = { showAddDeviceDialog = true },
                onDeleteShape = uiState.selectedShape?.let {
                    { viewModel.deleteSelectedShape() }
                },
                onBringToFront = uiState.selectedShape?.let {
                    { viewModel.bringShapeToFront() }
                },
                onSendToBack = uiState.selectedShape?.let {
                    { viewModel.sendShapeToBack() }
                },
                onCopyShape = uiState.selectedShape?.let {
                    { /* Реализовать копирование */ }
                },
                onDuplicateShape = uiState.selectedShape?.let {
                    { viewModel.duplicateSelectedShape() }
                },
                onOpenShapeProperties = {
                    viewModel.togglePropertiesPanel()
                },
                onTogglePropertiesPanel = {
                    viewModel.togglePropertiesPanel()
                },
                isPropertiesPanelVisible = showPropertiesPanel,
                onChangeFillColor = { color ->
                    uiState.selectedShape?.let { shape ->
                        val updatedShape = shape.copyWithFillColor(color)
                        viewModel.updateShapeProperties(updatedShape)
                    }
                },
                onChangeStrokeColor = { color ->
                    uiState.selectedShape?.let { shape ->
                        val updatedShape = shape.copyWithStrokeColor(color)
                        viewModel.updateShapeProperties(updatedShape)
                    }
                },
                onChangeStrokeWidth = { width ->
                    uiState.selectedShape?.let { shape ->
                        val updatedShape = shape.copyWithStrokeWidth(width)
                        viewModel.updateShapeProperties(updatedShape)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // ДИАЛОГИ
        if (showGridSettings) {
            GridSettingsDialog(
                gridEnabled = uiState.schemeData.gridEnabled,
                gridSize = uiState.schemeData.gridSize,
                onGridToggled = { viewModel.toggleGrid() },
                onGridSizeChanged = { size -> viewModel.updateGridSize(size) },
                onDismiss = { showGridSettings = false }
            )
        }

        if (showLayersDialog) {
            AlertDialog(
                onDismissRequest = { showLayersDialog = false },
                title = { Text("Управление слоями") },
                text = { Text("Управление слоями будет реализовано позже") },
                confirmButton = {
                    TextButton(onClick = { showLayersDialog = false }) {
                        Text("Закрыть")
                    }
                }
            )
        }

        if (showAlignmentDialog) {
            AlertDialog(
                onDismissRequest = { showAlignmentDialog = false },
                title = { Text("Выравнивание") },
                text = { Text("Выравнивание объектов будет реализовано позже") },
                confirmButton = {
                    TextButton(onClick = { showAlignmentDialog = false }) {
                        Text("Закрыть")
                    }
                }
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
            val availableDevices = devicesForScheme.filter { device ->
                uiState.schemeData.devices.none { it.deviceId == device.id }
            }

            AddDeviceDialog(
                devices = availableDevices,
                schemeLocation = schemeLocation,
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

        // Диалог ввода текста
        if (uiState.showTextInputDialog && uiState.textInputPosition != null) {
            TextInputDialog(
                position = uiState.textInputPosition!!,
                onDismiss = {
                    viewModel.hideTextInputDialog()
                },
                onConfirm = { text ->
                    uiState.textInputPosition?.let { position ->
                        viewModel.addTextAt(position, text)
                    }
                    viewModel.hideTextInputDialog()
                }
            )
        }
    }
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
    schemeLocation: String,
    onDeviceSelected: (Device) -> Unit,
    onDismiss: () -> Unit
) {
    // Фильтруем приборы по локации схемы
    val filteredDevices = remember(devices, schemeLocation) {
        devices.filter { it.location == schemeLocation }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить прибор") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Показываем локацию схемы
                Text(
                    text = "Схема: $schemeLocation",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (filteredDevices.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Нет приборов для этой локации")
                        Text(
                            text = "Добавьте приборы с локацией '$schemeLocation'",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    filteredDevices.forEach { device ->
                        DeviceSelectionCard(
                            device = device,
                            schemeLocation = schemeLocation,
                            onClick = { onDeviceSelected(device) }
                        )
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

@Composable
fun DeviceSelectionCard(
    device: Device,
    schemeLocation: String,
    onClick: () -> Unit
) {
    val isCorrectLocation = device.location == schemeLocation

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrectLocation) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        border = if (!isCorrectLocation) CardDefaults.outlinedCardBorder() else null
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Иконка устройства
            Icon(
                imageVector = when (device.type) {
                    "Манометр", "Термометр", "Датчик давления" -> Icons.Default.Sensors
                    "Счетчик" -> Icons.Default.Speed
                    "Клапан", "Задвижка" -> Icons.Default.Tune
                    "Датчик" -> Icons.Default.Sensors
                    "Преобразователь" -> Icons.Default.ElectricBolt
                    "Регулятор" -> Icons.Default.Thermostat
                    else -> Icons.Default.Devices
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = if (isCorrectLocation) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )

            Column(modifier = Modifier.weight(1f)) {
                // Название и инвентарный номер
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

                // Локация с проверкой
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isCorrectLocation) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = device.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCorrectLocation) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.error
                    )
                }
            }

            // Статус устройства
            Surface(
                color = when (device.status) {
                    "В работе" -> Color(0xFFE8F5E9)
                    "Хранение" -> Color(0xFFE3F2FD)
                    "Утерян" -> Color(0xFFFFEBEE)
                    "Испорчен" -> Color(0xFFFFF3E0)
                    else -> Color(0xFFF5F5F5)
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = device.status,
                    color = when (device.status) {
                        "В работе" -> Color(0xFF2E7D32)
                        "Хранение" -> Color(0xFF1976D2)
                        "Утерян" -> Color(0xFFC62828)
                        "Испорчен" -> Color(0xFFEF6C00)
                        else -> Color(0xFF616161)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Индикатор соответствия
            if (!isCorrectLocation) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Локация не совпадает",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputDialog(
    position: Offset,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить текст") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Позиция: (${position.x.toInt()}, ${position.y.toInt()})")
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}