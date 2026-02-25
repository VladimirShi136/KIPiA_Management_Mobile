package com.kipia.management.mobile.ui.screens.schemes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.*
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.viewmodel.CanvasState
import com.kipia.management.mobile.viewmodel.EditorMode
import com.kipia.management.mobile.viewmodel.EditorState
import com.kipia.management.mobile.viewmodel.SchemeEditorViewModel
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeEditorScreen(
    schemeId: Int,
    onNavigateBack: () -> Unit,
    viewModel: SchemeEditorViewModel = hiltViewModel(),
    topAppBarController: TopAppBarController? = null,
    notificationManager: NotificationManager
) {
    // Только необходимые состояния
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()

    // Локальные состояния диалогов
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerType by remember { mutableStateOf("fill") }
    var selectedDeviceForPlacement by remember { mutableStateOf<Device?>(null) }

    val scope = rememberCoroutineScope()

    // Колбэки
    val onBackClick = remember {
        {
            if (editorState.uiState.isDirty) showExitDialog = true
            else onNavigateBack()
        }
    }

    val onSaveClick = remember {
        {
            scope.launch {
                if (viewModel.saveScheme()) {
                    notificationManager.notifySchemeSaved(editorState.scheme.name)
                    onNavigateBack()
                } else {
                    notificationManager.notifyError("Ошибка при сохранении")
                }
            }
        }
    }

    val onPropertiesClick = remember { { showPropertiesDialog = true } }

    // BackHandler
    BackHandler { onBackClick() }

    // TopAppBar
    LaunchedEffect(schemeId, editorState.uiState.isDirty) {
        topAppBarController?.setForScreen(
            screenRoute = "scheme_editor",
            additionalParams = mapOf(
                "title" to "Редактор",
                "canSave" to true,
                "isDirty" to editorState.uiState.isDirty,
                "onBackClick" to onBackClick,
                "onSaveClick" to onSaveClick,
                "onPropertiesClick" to onPropertiesClick
            )
        )
    }

    // Очистка TopAppBar при выходе
    DisposableEffect(Unit) {
        onDispose {
            topAppBarController?.updateState(
                topAppBarController.state.value.copy(
                    showSchemeEditorActions = false,
                    onBackClick = null,
                    onSaveClick = null,
                    onPropertiesClick = null
                )
            )
        }
    }

    // Очистка выделения при PAN_ZOOM
    LaunchedEffect(editorState.uiState.mode) {
        if (editorState.uiState.mode == EditorMode.PAN_ZOOM) {
            viewModel.clearSelection()
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                SchemeCanvasContainer(
                    editorState = editorState,
                    viewModel = viewModel,
                    selectedDeviceForPlacement = selectedDeviceForPlacement,
                    onDeviceForPlacementChange = { selectedDeviceForPlacement = it },
                    onAddDeviceDialogChange = { showAddDeviceDialog = it },
                    modifier = Modifier.fillMaxSize()
                )

                ShapePropertiesPanel(
                    editorState = editorState,
                    shapes = viewModel.shapes.collectAsStateWithLifecycle().value,
                    viewModel = viewModel,
                    onColorPickerChange = { showColorPicker = it },
                    onColorPickerTypeChange = { colorPickerType = it }
                )

                DevicePropertiesPanel(
                    editorState = editorState,
                    allDevices = viewModel.allDevices.collectAsStateWithLifecycle().value,
                    devices = viewModel.devices.collectAsStateWithLifecycle().value,
                    viewModel = viewModel
                )
            }

            BottomShapeToolbarContainer(
                canUndo = canUndo,
                canRedo = canRedo,
                editorState = editorState,
                viewModel = viewModel,
                onAddDeviceDialogChange = { showAddDeviceDialog = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Text(
                text = "Режим: ${editorState.uiState.mode}",
                modifier = Modifier.padding(8.dp)
            )
        }

        // Диалоги
        if (showPropertiesDialog) {
            SimpleSchemePropertiesDialog(
                scheme = editorState.scheme,
                canvasState = editorState.canvasState,
                onDismiss = { showPropertiesDialog = false }
            )
        }

        if (showAddDeviceDialog) {
            SimpleAddDeviceDialog(
                devices = viewModel.availableDevices.collectAsStateWithLifecycle().value,
                schemeLocation = editorState.scheme.name,
                onDeviceSelected = { device ->
                    viewModel.addDevice(device.id, Offset(500f, 500f))
                    showAddDeviceDialog = false
                    viewModel.setMode(EditorMode.SELECT)
                },
                onDismiss = {
                    selectedDeviceForPlacement = null
                    viewModel.setMode(EditorMode.SELECT)
                    showAddDeviceDialog = false
                }
            )
        }

        if (showExitDialog) {
            SimpleExitDialog(
                onSaveAndExit = {
                    scope.launch {
                        if (viewModel.saveScheme()) {
                            notificationManager.notifySchemeSaved(editorState.scheme.name)
                            onNavigateBack()
                        } else {
                            notificationManager.notifyError("Ошибка при сохранении")
                        }
                    }
                },
                onExitWithoutSaving = onNavigateBack,
                onDismiss = { showExitDialog = false }
            )
        }

        if (editorState.uiState.showTextInputDialog && editorState.uiState.textInputPosition != null) {
            SimpleTextInputDialog(
                position = editorState.uiState.textInputPosition!!,
                onDismiss = { viewModel.hideTextInputDialog() },
                onConfirm = { text ->
                    viewModel.addTextShape(text, editorState.uiState.textInputPosition!!)
                    viewModel.hideTextInputDialog()
                }
            )
        }

        if (showColorPicker && editorState.selection.selectedShapeId != null) {
            val selectedShape = viewModel.shapes.collectAsStateWithLifecycle().value.find {
                it.id == editorState.selection.selectedShapeId
            }
            SimpleColorPickerDialog(
                title = if (colorPickerType == "fill") "Цвет заливки" else "Цвет обводки",
                initialColor = when (colorPickerType) {
                    "fill" -> selectedShape?.fillColor ?: Color.Transparent
                    else -> selectedShape?.strokeColor ?: Color.Black
                },
                onColorSelected = { color ->
                    if (colorPickerType == "fill") {
                        viewModel.updateShapeFillColor(editorState.selection.selectedShapeId!!, color)
                    } else {
                        viewModel.updateShapeStrokeColor(editorState.selection.selectedShapeId!!, color)
                    }
                    showColorPicker = false
                },
                onDismiss = { showColorPicker = false }
            )
        }
    }
}

// ============ ОСНОВНЫЕ КОМПОНЕНТЫ ============

@Composable
private fun SchemeCanvasContainer(
    editorState: EditorState,
    viewModel: SchemeEditorViewModel,
    selectedDeviceForPlacement: Device?,
    onDeviceForPlacementChange: (Device?) -> Unit,
    onAddDeviceDialogChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shapes by viewModel.shapes.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()
    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()

    val onTransform = remember { { scale: Float, offset: Offset, _: Boolean ->
        viewModel.updateCanvasTransform(scale, offset)
    } }

    val onCanvasClick = remember(editorState.uiState.mode, selectedDeviceForPlacement) { { position: Offset ->
        Timber.d("🖱️ onCanvasClick: mode=${editorState.uiState.mode}, position=$position")
        when (editorState.uiState.mode) {
            EditorMode.RECTANGLE, EditorMode.LINE, EditorMode.ELLIPSE,
            EditorMode.RHOMBUS, EditorMode.TEXT -> viewModel.addShape(editorState.uiState.mode, position)
            EditorMode.DEVICE -> {
                if (selectedDeviceForPlacement != null) {
                    viewModel.addDevice(selectedDeviceForPlacement.id, position)
                    onDeviceForPlacementChange(null)
                    viewModel.setMode(EditorMode.SELECT)
                } else {
                    onAddDeviceDialogChange(true)
                }
            }
            else -> {
                Timber.d("🧹 Вызов clearSelection()")
                viewModel.clearSelection()  // ← Это должно вызываться в SELECT режиме
            }
        }
    } }

    SchemeCanvas(
        editorState = editorState,
        canvasState = editorState.canvasState,
        shapes = shapes,
        devices = devices,
        allDevices = allDevices,
        availableDevices = availableDevices,
        onShapeClick = { shapeId -> viewModel.selectShape(shapeId) },
        onDeviceClick = { deviceId -> viewModel.selectDevice(deviceId) },
        onCanvasClick = onCanvasClick,
        onShapeDrag = { shapeId, delta -> viewModel.moveShape(shapeId, delta) },
        onDeviceDrag = { deviceId, delta -> viewModel.moveDevice(deviceId, delta) },
        onTransform = onTransform,
        modifier = modifier
    )
}

@Composable
private fun ShapePropertiesPanel(
    editorState: EditorState,
    shapes: List<ComposeShape>,
    viewModel: SchemeEditorViewModel,
    onColorPickerChange: (Boolean) -> Unit,
    onColorPickerTypeChange: (String) -> Unit
) {
    val selectedShape = editorState.selection.selectedShapeId?.let { id ->
        shapes.find { it.id == id }
    }

    if (selectedShape != null &&
        editorState.uiState.showShapeProperties &&
        editorState.uiState.mode != EditorMode.PAN_ZOOM) {

        Card(
            modifier = Modifier
                .padding(16.dp)
                .width(240.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Свойства фигуры", style = MaterialTheme.typography.titleSmall)
                HorizontalDivider()

                Button(
                    onClick = {
                        onColorPickerTypeChange("fill")
                        onColorPickerChange(true)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedShape.fillColor
                    )
                ) {
                    Text("Цвет заливки")
                }

                Button(
                    onClick = {
                        onColorPickerTypeChange("stroke")
                        onColorPickerChange(true)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = selectedShape.strokeColor
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
}

@Composable
private fun DevicePropertiesPanel(
    editorState: EditorState,
    allDevices: List<Device>,
    devices: List<SchemeDevice>,
    viewModel: SchemeEditorViewModel
) {
    val selectedDeviceInfo = editorState.selection.selectedDeviceId?.let { id ->
        val device = allDevices.find { it.id == id }
        val schemeDevice = devices.find { it.deviceId == id }
        if (device != null && schemeDevice != null) device to schemeDevice else null
    }

    if (selectedDeviceInfo != null &&
        editorState.uiState.showDeviceProperties &&
        editorState.uiState.mode != EditorMode.PAN_ZOOM) {

        Card(
            modifier = Modifier
                .padding(16.dp)
                .width(240.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Свойства прибора", style = MaterialTheme.typography.titleSmall)

                val (device, schemeDevice) = selectedDeviceInfo
                Text("${device.name} (${device.type})", style = MaterialTheme.typography.bodyMedium)
                Text("Инв. №${device.inventoryNumber}", style = MaterialTheme.typography.bodySmall)

                HorizontalDivider()

                Text("Позиция: (${schemeDevice.x.toInt()}, ${schemeDevice.y.toInt()})")

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

@Composable
private fun BottomShapeToolbarContainer(
    canUndo: Boolean,
    canRedo: Boolean,
    editorState: EditorState,
    viewModel: SchemeEditorViewModel,
    onAddDeviceDialogChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shapes by viewModel.shapes.collectAsStateWithLifecycle()
    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()

    val selectedShape = editorState.selection.selectedShapeId?.let { id ->
        shapes.find { it.id == id }
    }

    val selectedDeviceInfo = editorState.selection.selectedDeviceId?.let { id ->
        val device = allDevices.find { it.id == id }
        val schemeDevice = devices.find { it.deviceId == id }
        if (device != null && schemeDevice != null) device to schemeDevice else null
    }

    val onModeChanged = remember(viewModel, onAddDeviceDialogChange) { { mode: EditorMode ->
        viewModel.setMode(mode)
        when (mode) {
            EditorMode.DEVICE -> onAddDeviceDialogChange(true)
            EditorMode.SELECT, EditorMode.PAN_ZOOM -> viewModel.clearSelection()
            else -> {}
        }
    } }

    BottomShapeToolbar(
        canUndo = canUndo,
        canRedo = canRedo,
        onUndo = { viewModel.undo() },
        onRedo = { viewModel.redo() },
        editorMode = editorState.uiState.mode,
        selectedShape = selectedShape,
        selectedDevice = selectedDeviceInfo,
        onModeChanged = onModeChanged,
        onAddDevice = { viewModel.setMode(EditorMode.DEVICE) },
        onShapeMenuClick = { viewModel.toggleShapeProperties() },
        onDeviceMenuClick = { viewModel.toggleDeviceProperties() },
        onDuplicateShape = { selectedShape?.let { viewModel.duplicateShape(it.id) } },
        onDeleteSelected = {
            if (selectedShape != null) viewModel.deleteSelectedShape()
            else selectedDeviceInfo?.first?.let { viewModel.removeDevice(it.id) }
        },
        modifier = modifier
    )
}

// ============ ДИАЛОГИ ============

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
                Text("Схема: $schemeLocation", style = MaterialTheme.typography.titleSmall)
                Text("Доступно приборов: ${devices.size}", style = MaterialTheme.typography.bodySmall)

                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Info, null, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Нет доступных приборов для этой локации")
                            Text("Все приборы уже размещены на схеме")
                        }
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
                                    Text(device.name ?: device.type)
                                    Text("${device.type} • Инв. №${device.inventoryNumber}")
                                }
                                Icon(Icons.Default.Add, null)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
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
        confirmButton = { Button(onClick = onSaveAndExit) { Text("Сохранить и выйти") } },
        dismissButton = { TextButton(onClick = onExitWithoutSaving) { Text("Выйти без сохранения") } }
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
            Column {
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
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
        Color.Transparent, Color.Black, Color.White, Color.Red,
        Color(0xFFFF9800), Color.Yellow, Color.Green,
        Color(0xFF2196F3), Color(0xFF9C27B0), Color(0xFFE91E63)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(selectedColor, shape = RoundedCornerShape(30.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    colors.chunked(3).forEach { rowColors ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowColors.forEach { color ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(color, RoundedCornerShape(30.dp))
                                        .clickable { selectedColor = color }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onColorSelected(selectedColor) }) { Text("Выбрать") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
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
            Column {
                Text("Название: ${scheme.name}")
                Text("Описание: ${scheme.description ?: "Нет описания"}")
                Text("Размер: ${canvasState.width} x ${canvasState.height}")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } }
    )
}