package com.kipia.management.mobile.ui.screens.schemes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.*
import com.kipia.management.mobile.ui.components.scheme.dialogs.ColorPickerDialog
import com.kipia.management.mobile.ui.components.scheme.dialogs.DraggableCard
import com.kipia.management.mobile.ui.components.scheme.dialogs.ShapePropertiesDialog
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeLine
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeText
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
    var showShapePropertiesDialog by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
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
        // Используем Box для наложения элементов
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Канвас на заднем плане (занимает весь экран)
            SchemeCanvasContainer(
                editorState = editorState,
                viewModel = viewModel,
                selectedDeviceForPlacement = selectedDeviceForPlacement,
                onDeviceForPlacementChange = { selectedDeviceForPlacement = it },
                onAddDeviceDialogChange = { showAddDeviceDialog = it },
                modifier = Modifier.fillMaxSize()
            )

            // Плавающая панель инструментов ВСЕГДА внизу
            FloatingBottomToolbar(
                canUndo = canUndo,
                canRedo = canRedo,
                editorState = editorState,
                viewModel = viewModel,
                onAddDeviceDialogChange = { showAddDeviceDialog = it },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            )

            // Панели свойств (плавающие сверху)
            ShapePropertiesPanel(
                editorState = editorState,
                shapes = viewModel.shapes.collectAsStateWithLifecycle().value,
                viewModel = viewModel,
                onShowPropertiesDialog = { showShapePropertiesDialog = it },
                onShowColorPicker = { type ->
                    editorState.selection.selectedShapeId?.let { shapeId ->
                        colorPickerTarget = Pair(shapeId, type)
                        showColorPicker = true
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )

            DevicePropertiesPanel(
                editorState = editorState,
                allDevices = viewModel.allDevices.collectAsStateWithLifecycle().value,
                devices = viewModel.devices.collectAsStateWithLifecycle().value,
                viewModel = viewModel,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
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
                    // Вместо немедленного добавления - выбираем для размещения
                    viewModel.selectDeviceForPlacement(device.id)
                    showAddDeviceDialog = false
                },
                onDismiss = {
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
                onConfirm = { text, fontSize ->
                    viewModel.addTextShape(
                        text,
                        editorState.uiState.textInputPosition!!,
                        fontSize
                    )
                    viewModel.hideTextInputDialog()
                }
            )
        }

        if (showColorPicker && colorPickerTarget != null) {
            val (shapeId, type) = colorPickerTarget!!
            val selectedShape = viewModel.shapes.collectAsStateWithLifecycle().value.find { it.id == shapeId }

            selectedShape?.let { shape ->
                ColorPickerDialog(
                    title = if (type == "fill") "Цвет заливки" else "Цвет обводки",
                    initialColor = if (type == "fill") shape.fillColor else shape.strokeColor,
                    onColorSelected = { color ->
                        if (type == "fill") {
                            viewModel.updateShapeFillColor(shapeId, color)
                        } else {
                            viewModel.updateShapeStrokeColor(shapeId, color)
                        }
                        showColorPicker = false
                        colorPickerTarget = null
                    },
                    onDismiss = {
                        showColorPicker = false
                        colorPickerTarget = null
                    }
                )
            }
        }

        // Диалог свойств фигуры (размер и поворот)
        if (showShapePropertiesDialog && editorState.selection.selectedShapeId != null) {
            val selectedShape = viewModel.shapes.collectAsStateWithLifecycle().value.find {
                it.id == editorState.selection.selectedShapeId
            }
            selectedShape?.let { shape ->
                ShapePropertiesDialog(
                    shape = shape,
                    onDismiss = { showShapePropertiesDialog = false },
                    onUpdate = { updatedShape ->
                        viewModel.updateShape(updatedShape)
                    }
                )
            }
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

    val onViewportSizeChanged = remember(viewModel) { { width: Int, height: Int ->
        viewModel.updateViewportSize(width, height)
    } }

    val onTransform = remember {
        { scale: Float, offset: Offset, _: Boolean ->
            viewModel.updateCanvasTransform(scale, offset)
        }
    }

    val onCanvasClick = remember(editorState.uiState.mode, editorState.uiState.pendingDeviceId, editorState.uiState.pendingShapeMode) {
        { position: Offset ->
            Timber.d("🖱️ onCanvasClick: mode=${editorState.uiState.mode}, position=$position")

            when {
                // Сначала проверяем pendingShapeMode (для фигур)
                editorState.uiState.pendingShapeMode != null -> {
                    viewModel.placeShapeAtPosition(position)
                }
                // Потом проверяем pendingDeviceId (для приборов)
                editorState.uiState.pendingDeviceId != null -> {
                    viewModel.placeDeviceAtPosition(position)
                }
                // Если нет pending объектов, обрабатываем обычные режимы
                else -> {
                    when (editorState.uiState.mode) {
                        EditorMode.RECTANGLE, EditorMode.LINE, EditorMode.ELLIPSE,
                        EditorMode.RHOMBUS, EditorMode.TEXT -> {
                            viewModel.addShape(editorState.uiState.mode, position)
                        }
                        EditorMode.DEVICE -> {
                            onAddDeviceDialogChange(true)
                        }
                        else -> {
                            Timber.d("🧹 Вызов clearSelection()")
                            viewModel.clearSelection()
                        }
                    }
                }
            }
        }
    }

    Box(modifier = modifier) {
        SchemeCanvas(
            editorState = editorState,
            canvasState = editorState.canvasState,
            shapes = shapes,
            devices = devices,
            allDevices = allDevices,
            availableDevices = availableDevices,
            onShapeClick = { shapeId -> viewModel.selectShape(shapeId) },
            onDeviceClick = { deviceId -> viewModel.selectDevice(deviceId) },
            onCanvasClick = onCanvasClick,  // ← Просто передаем лямбду
            onShapeDrag = { shapeId, delta -> viewModel.moveShape(shapeId, delta) },
            onDeviceDrag = { deviceId, delta -> viewModel.moveDevice(deviceId, delta) },
            onTransform = onTransform,
            onViewportSizeChanged = onViewportSizeChanged,
            modifier = modifier
        )
    }
}

@Composable
private fun ShapePropertiesPanel(
    editorState: EditorState,
    shapes: List<ComposeShape>,
    viewModel: SchemeEditorViewModel,
    onShowPropertiesDialog: (Boolean) -> Unit,
    onShowColorPicker: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedShape = editorState.selection.selectedShapeId?.let { id ->
        shapes.find { it.id == id }
    }

    // Добавляем отладку
    LaunchedEffect(selectedShape?.rotation) {
        if (selectedShape != null) {
            Timber.d("📐 Selected shape ${selectedShape.id} rotation=${selectedShape.rotation}")
        }
    }

    if (selectedShape != null &&
        editorState.uiState.showShapeProperties &&
        editorState.uiState.mode != EditorMode.PAN_ZOOM
    ) {
        DraggableCard(
            modifier = modifier.width(240.dp),
            showDragHandle = true
        ) {
            Text(
                "Свойства фигуры",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Для текста показываем специальную кнопку свойств текста
            if (selectedShape is ComposeText) {
                Button(
                    onClick = { onShowPropertiesDialog(true) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.TextFields, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Свойства текста")
                }
            } else {
                // Для остальных фигур - кнопка "Размер и поворот"
                Button(
                    onClick = { onShowPropertiesDialog(true) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Размер и поворот")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопка цвета заливки - ТОЛЬКО для фигур, у которых есть заливка (не для линии и не для текста)
            if (selectedShape !is ComposeLine && selectedShape !is ComposeText) {
                Button(
                    onClick = { onShowColorPicker("fill") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.ColorLens, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Цвет заливки")
                }

                Spacer(modifier = Modifier.height(8.dp))
            }

            // Кнопка цвета обводки - для всех фигур (кроме текста?)
            // Для текста цвет обводки - это цвет самого текста
            Button(
                onClick = { onShowColorPicker("stroke") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                Icon(Icons.Default.Brush, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (selectedShape is ComposeText) "Цвет текста" else "Цвет обводки")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопка закрытия
            TextButton(
                onClick = { viewModel.toggleShapeProperties() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
private fun DevicePropertiesPanel(
    editorState: EditorState,
    allDevices: List<Device>,
    devices: List<SchemeDevice>,
    viewModel: SchemeEditorViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDeviceInfo = editorState.selection.selectedDeviceId?.let { id ->
        val device = allDevices.find { it.id == id }
        val schemeDevice = devices.find { it.deviceId == id }
        if (device != null && schemeDevice != null) device to schemeDevice else null
    }

    if (selectedDeviceInfo != null &&
        editorState.uiState.showDeviceProperties &&
        editorState.uiState.mode != EditorMode.PAN_ZOOM
    ) {
        DraggableCard(
            modifier = modifier.width(280.dp),
            showDragHandle = true
        ) {
            Text(
                "Свойства прибора",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            val (device, schemeDevice) = selectedDeviceInfo

            // Информация о приборе
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = device.name ?: device.type,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Тип: ${device.type}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Инв. №${device.inventoryNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Позиция
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Позиция:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "(${schemeDevice.x.toInt()}, ${schemeDevice.y.toInt()})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Кнопка закрытия
            TextButton(
                onClick = { viewModel.toggleDeviceProperties() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
private fun FloatingBottomToolbar(
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

    val onModeChanged = { mode: EditorMode ->
        viewModel.setMode(mode)
        when (mode) {
            EditorMode.DEVICE -> onAddDeviceDialogChange(true)
            EditorMode.SELECT, EditorMode.PAN_ZOOM -> viewModel.clearSelection()
            else -> {}
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 8.dp
        )
    ) {
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
            onShapeMenuClick = {
                if (selectedShape != null) {
                    viewModel.toggleShapeProperties()
                }
            },
            onDeviceMenuClick = {
                if (selectedDeviceInfo != null) {
                    viewModel.toggleDeviceProperties()
                }
            },
            onDuplicateShape = { selectedShape?.let { viewModel.duplicateShape(it.id) } },
            onDeleteSelected = {
                if (selectedShape != null) viewModel.deleteSelectedShape()
                else selectedDeviceInfo?.first?.let { viewModel.removeDevice(it.id) }
            },
            onShapeSelectedForPlacement = { shapeMode ->  // ← НОВЫЙ КОЛБЭК
                viewModel.selectShapeForPlacement(shapeMode)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ============ ДИАЛОГИ ============

@Composable
private fun SimpleAddDeviceDialog(
    devices: List<Device>,
    schemeLocation: String,
    onDeviceSelected: (Device) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        ) {
            DraggableCard(
                modifier = Modifier
                    .widthIn(min = 260.dp, max = 300.dp)  // Уменьшенная ширина
                    .heightIn(max = 450.dp),  // Ограничение высоты
                showDragHandle = true
            ) {
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 8.dp)
                ) {
                    // Заголовок
                    Text(
                        text = "Выберите прибор",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Text(
                        text = "Схема: $schemeLocation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    if (devices.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Нет доступных приборов",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Все приборы уже размещены",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    } else {
                        // Счетчик доступных приборов
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = "Доступно: ${devices.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }

                        // Список приборов
                        devices.forEach { device ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        onDeviceSelected(device)
                                        onDismiss()
                                    },
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 1.dp,
                                    pressedElevation = 4.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = device.name ?: device.type,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${device.type} • №${device.inventoryNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = "Выбрать",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Кнопка закрытия
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text("Отмена")
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
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
    onConfirm: (String, Float) -> Unit  // Изменяем тип
) {
    var text by remember { mutableStateOf("") }
    var fontSize by remember { mutableFloatStateOf(16f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        ) {
            DraggableCard(
                modifier = Modifier
                    .widthIn(min = 260.dp, max = 300.dp)
                    .wrapContentHeight(),
                showDragHandle = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Добавить текст",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Текст") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = text.isBlank()  // Показываем ошибку если пусто
                    )

                    if (text.isBlank()) {
                        Text(
                            text = "Текст не может быть пустым",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Размер:", style = MaterialTheme.typography.bodyMedium)
                        Text("${fontSize.toInt()}px", style = MaterialTheme.typography.titleMedium)
                    }

                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 8f..72f,
                        steps = 64,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Отмена")
                        }

                        Button(
                            onClick = {
                                if (text.isNotBlank()) {
                                    onConfirm(text, fontSize)
                                }
                            },
                            enabled = text.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Добавить")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
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