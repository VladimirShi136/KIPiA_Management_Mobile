package com.kipia.management.mobile.ui.screens.schemes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.ui.components.scheme.*
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShapeFactory
import com.kipia.management.mobile.viewmodel.CanvasState
import com.kipia.management.mobile.viewmodel.EditorMode
import com.kipia.management.mobile.viewmodel.SchemeEditorViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeEditorScreen(
    schemeId: Int?,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: SchemeEditorViewModel = hiltViewModel(),
    topAppBarController: com.kipia.management.mobile.ui.components.topappbar.TopAppBarController? = null
) {
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val shapes by viewModel.shapes.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerType by remember { mutableStateOf("fill") }

    val scope = rememberCoroutineScope()

    val selectedShape = shapes.find { it.id == editorState.selection.selectedShapeId }
    val selectedDevice = if (editorState.selection.selectedDeviceId != null) {
        val device = availableDevices.find { it.id == editorState.selection.selectedDeviceId }
        val schemeDevice = devices.find { it.deviceId == editorState.selection.selectedDeviceId }
        if (device != null && schemeDevice != null) device to schemeDevice else null
    } else null

    LaunchedEffect(editorState.uiState.mode) {
        Timber.d("SchemeEditorScreen: Mode changed to ${editorState.uiState.mode}")
    }

    // ВРЕМЕННЫЕ ДИАЛОГИ
    if (showAddDeviceDialog) {
        SimpleAddDeviceDialog(
            devices = availableDevices.filter { it.location == editorState.scheme.name },
            schemeLocation = editorState.scheme.name,
            onDeviceSelected = { device ->
                val centerX = editorState.canvasState.width / 2f
                val centerY = editorState.canvasState.height / 2f
                viewModel.addDevice(device.id, Offset(centerX, centerY))
                showAddDeviceDialog = false
                viewModel.setMode(EditorMode.NONE)
            },
            onDismiss = {
                showAddDeviceDialog = false
                viewModel.setMode(EditorMode.NONE)
            }
        )
    }

    if (showExitDialog) {
        SimpleExitDialog(
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

    if (editorState.uiState.showTextInputDialog && editorState.uiState.textInputPosition != null) {
        SimpleTextInputDialog(
            position = editorState.uiState.textInputPosition!!,
            onDismiss = { viewModel.hideTextInputDialog() },
            onConfirm = { text ->
                val textShape = ComposeShapeFactory.createText().apply {
                    this.text = text
                    this.width = (text.length * 10f + 30f).coerceAtLeast(50f)
                    this.height = 40f
                }
                viewModel.addShape(textShape, editorState.uiState.textInputPosition!!)
                viewModel.hideTextInputDialog()
            }
        )
    }

    if (showColorPicker && editorState.selection.selectedShapeId != null) {
        val selectedShape = shapes.find { it.id == editorState.selection.selectedShapeId }
        SimpleColorPickerDialog(
            title = if (colorPickerType == "fill") "Цвет заливки" else "Цвет обводки",
            initialColor = when (colorPickerType) {
                "fill" -> selectedShape?.fillColor ?: Color.Transparent
                else -> selectedShape?.strokeColor ?: Color.Black
            },
            onColorSelected = { color ->
                if (colorPickerType == "fill") {
                    editorState.selection.selectedShapeId?.let { shapeId ->
                        viewModel.updateShapeFillColor(shapeId, color)
                    }
                } else {
                    editorState.selection.selectedShapeId?.let { shapeId ->
                        viewModel.updateShapeStrokeColor(shapeId, color)
                    }
                }
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Верхняя панель с UNDO/REDO
            CanvasControls(
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Основной холст
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                SchemeCanvas(
                    editorState = editorState,
                    shapes = shapes,
                    devices = devices,
                    availableDevices = availableDevices,
                    onShapeClick = { shapeId ->
                        viewModel.selectShape(shapeId)
                    },
                    onDeviceClick = { deviceId ->
                        viewModel.selectDevice(deviceId)
                    },
                    onCanvasClick = { position ->
                        when (editorState.uiState.mode) {
                            EditorMode.RECTANGLE -> {
                                viewModel.addShape(ComposeShapeFactory.createRectangle(), position)
                            }
                            EditorMode.LINE -> {
                                viewModel.addShape(ComposeShapeFactory.createLine(), position)
                            }
                            EditorMode.ELLIPSE -> {
                                viewModel.addShape(ComposeShapeFactory.createEllipse(), position)
                            }
                            EditorMode.RHOMBUS -> {
                                viewModel.addShape(ComposeShapeFactory.createRhombus(), position)
                            }
                            EditorMode.TEXT -> {
                                viewModel.showTextInputDialog(position)
                            }
                            EditorMode.DEVICE -> {
                                showAddDeviceDialog = true
                            }
                            else -> {
                                // В режиме NONE клик по пустому месту сбрасывает выделение
                                viewModel.clearSelection()
                            }
                        }
                    },
                    onShapeDrag = { shapeId, delta ->
                        viewModel.moveShape(shapeId, delta)
                    },
                    onDeviceDrag = { deviceId, delta ->
                        viewModel.moveDevice(deviceId, delta)
                    },
                    onTransform = { scale, offset ->
                        viewModel.updateCanvasTransform(scale, offset)
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Панель свойств фигуры
                if (editorState.uiState.showShapeProperties && editorState.selection.selectedShapeId != null) {
                    val selectedShape = shapes.find { it.id == editorState.selection.selectedShapeId }

                    Card(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .width(240.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Свойства фигуры",
                                style = MaterialTheme.typography.titleSmall
                            )

                            Divider()

                            Button(
                                onClick = {
                                    colorPickerType = "fill"
                                    showColorPicker = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = selectedShape?.fillColor ?: Color.Transparent
                                )
                            ) {
                                Text("Цвет заливки")
                            }

                            Button(
                                onClick = {
                                    colorPickerType = "stroke"
                                    showColorPicker = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = selectedShape?.strokeColor ?: Color.Black
                                )
                            ) {
                                Text("Цвет обводки")
                            }

                            Button(
                                onClick = { viewModel.deleteSelectedShape() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Удалить")
                            }

                            TextButton(
                                onClick = { viewModel.toggleShapeProperties() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Закрыть")
                            }
                        }
                    }
                }

                // Панель свойств устройства
                if (editorState.uiState.showDeviceProperties && editorState.selection.selectedDeviceId != null) {
                    val device = availableDevices.find { it.id == editorState.selection.selectedDeviceId }
                    val schemeDevice = devices.find { it.deviceId == editorState.selection.selectedDeviceId }

                    if (device != null && schemeDevice != null) {
                        Card(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .width(240.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Свойства прибора",
                                    style = MaterialTheme.typography.titleSmall
                                )

                                Text(
                                    text = "${device.name} (${device.type})",
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Text(
                                    text = "Инв. №${device.inventoryNumber}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Divider()

                                Text(
                                    text = "Позиция: (${schemeDevice.x.toInt()}, ${schemeDevice.y.toInt()})",
                                    style = MaterialTheme.typography.bodySmall
                                )

                                Button(
                                    onClick = { viewModel.removeDevice(device.id) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Удалить со схемы")
                                }

                                TextButton(
                                    onClick = { viewModel.toggleDeviceProperties() },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Закрыть")
                                }
                            }
                        }
                    }
                }

                // Подсказка режима
                ModeHint(
                    mode = editorState.uiState.mode,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                )
            }

            // Нижняя панель инструментов
            BottomShapeToolbar(
                editorMode = editorState.uiState.mode,
                selectedShape = selectedShape,
                selectedDevice = selectedDevice,
                onModeChanged = { mode ->
                    Timber.d("BottomShapeToolbar onModeChanged: $mode")
                    viewModel.setMode(mode)
                },
                onAddDevice = {
                    viewModel.setMode(EditorMode.DEVICE)
                    showAddDeviceDialog = true
                },
                onShapeMenuClick = {
                    viewModel.toggleShapeProperties()
                },
                onDeviceMenuClick = {
                    viewModel.toggleDeviceProperties()
                },
                onDuplicateShape = {
                    selectedShape?.let { viewModel.duplicateShape(it.id) }
                },
                onDeleteSelected = {
                    if (selectedShape != null) {
                        viewModel.deleteSelectedShape()
                    } else if (selectedDevice != null) {
                        viewModel.removeDevice(selectedDevice.first.id)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Диалог свойств схемы
        if (showPropertiesDialog) {
            SimpleSchemePropertiesDialog(
                scheme = editorState.scheme,
                canvasState = editorState.canvasState,
                onDismiss = { showPropertiesDialog = false }
            )
        }
    }
}

// ============ ВРЕМЕННЫЕ ДИАЛОГИ ============

@Composable
private fun SimpleAddDeviceDialog(
    devices: List<Device>,
    schemeLocation: String,
    onDeviceSelected: (Device) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить прибор") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Схема: $schemeLocation",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Нет доступных приборов для этой локации")
                    }
                } else {
                    devices.forEach { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDeviceSelected(device) },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = device.name ?: device.type,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = "${device.type} • Инв. №${device.inventoryNumber}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Добавить"
                                )
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

@Composable
private fun SimpleExitDialog(
    onSaveAndExit: () -> Unit,
    onExitWithoutSaving: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Сохранить изменения?") },
        text = { Text("У вас есть несохраненные изменения. Что вы хотите сделать?") },
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

@Composable
private fun SimpleTextInputDialog(
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

@Composable
private fun SimpleColorPickerDialog(
    title: String,
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }

    val colors = listOf(
        Color.Transparent,
        Color.Black,
        Color.White,
        Color.Red,
        Color(0xFFFF9800),
        Color.Yellow,
        Color.Green,
        Color(0xFF2196F3),
        Color(0xFF9C27B0),
        Color(0xFFE91E63)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Предпросмотр
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(selectedColor, CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Сетка цветов
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    colors.chunked(3).forEach { rowColors ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(color, CircleShape)
                                        .border(
                                            width = 3.dp,
                                            color = if (color == selectedColor)
                                                MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = color }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(selectedColor) }) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
private fun SimpleSchemePropertiesDialog(
    scheme: Scheme,
    canvasState: CanvasState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Свойства схемы") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Название: ${scheme.name}")
                Text("Описание: ${scheme.description ?: "Нет описания"}")
                Text("Размер: ${canvasState.width} x ${canvasState.height}")
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
private fun SimpleExportDialog(
    schemeName: String,
    onExportAsImage: () -> Unit,
    onExportAsPDF: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Экспорт схемы") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Экспортировать схему '$schemeName':")

                ListItem(
                    headlineContent = { Text("Как изображение (PNG)") },
                    leadingContent = { Icon(Icons.Default.Image, null) },
                    modifier = Modifier.clickable {
                        onExportAsImage()
                        onDismiss()
                    }
                )

                ListItem(
                    headlineContent = { Text("Как PDF документ") },
                    leadingContent = { Icon(Icons.Default.PictureAsPdf, null) },
                    modifier = Modifier.clickable {
                        onExportAsPDF()
                        onDismiss()
                    }
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
private fun ModeHint(
    mode: EditorMode,
    modifier: Modifier = Modifier
) {
    val hintText = when (mode) {
        EditorMode.NONE -> "Режим просмотра: нажмите на объект для выделения"
        EditorMode.RECTANGLE -> "Режим прямоугольника: нажмите на холст для создания"
        EditorMode.LINE -> "Режим линии: нажмите на холст для создания"
        EditorMode.ELLIPSE -> "Режим эллипса: нажмите на холст для создания"
        EditorMode.RHOMBUS -> "Режим ромба: нажмите на холст для создания"
        EditorMode.TEXT -> "Режим текста: нажмите на холст для добавления текста"
        EditorMode.DEVICE -> "Режим добавления приборов: нажмите на холст или кнопку +"
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = hintText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}