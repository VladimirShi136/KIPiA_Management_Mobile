package com.kipia.management.mobile.ui.screens.schemes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.ui.components.dialogs.DeleteConfirmDialog
import com.kipia.management.mobile.ui.components.dialogs.InfoDialog
import com.kipia.management.mobile.ui.components.dialogs.SavingOverlay
import com.kipia.management.mobile.ui.components.dialogs.UnsavedChangesDialog
import com.kipia.management.mobile.ui.components.scheme.*
import com.kipia.management.mobile.ui.components.scheme.dialogs.*
import com.kipia.management.mobile.ui.components.scheme.shapes.*
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.viewmodel.EditorMode
import com.kipia.management.mobile.viewmodel.EditorState
import com.kipia.management.mobile.viewmodel.SchemeEditorViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SchemeEditorScreen(
    schemeId: Int,
    onNavigateBack: () -> Unit,
    topAppBarController: com.kipia.management.mobile.ui.components.topappbar.TopAppBarController,
    notificationManager: NotificationManager,
    onNavigateToDeviceEdit: (Int) -> Unit = {},
    onNavigateToDeviceDetail: (Int) -> Unit = {},
    onNavigateToDevicePhotos: (Int) -> Unit = {},
    viewModel: SchemeEditorViewModel = hiltViewModel()
) {
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val shapes by viewModel.shapes.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle(initialValue = false)
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle(initialValue = false)

    var showColorPicker by remember { mutableStateOf(false) }
    var colorPickerTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showShapePropertiesDialog by remember { mutableStateOf(false) }
    var showAddDeviceDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()

    val handleSave = {
        scope.launch {
            val success = viewModel.saveScheme()
            if (success) {
                notificationManager.notifySchemeSaved(editorState.scheme.name)
            } else {
                notificationManager.notifyError("Ошибка при сохранении схемы")
            }
        }
    }

    val handleBack = {
        if (editorState.uiState.isDirty) {
            showExitConfirmDialog = true
        } else {
            onNavigateBack()
        }
    }

    // Перехват системной кнопки "Назад"
    BackHandler(enabled = true) {
        handleBack()
    }

    LaunchedEffect(schemeId) {
        viewModel.initScheme(schemeId)
    }

    LaunchedEffect(canUndo, canRedo, editorState.uiState.isDirty) {
        topAppBarController.setForScreen(
            "scheme_editor",
            mapOf(
                "canSave" to true,
                "canUndo" to canUndo,
                "canRedo" to canRedo,
                "isDirty" to editorState.uiState.isDirty,
                "onSaveClick" to { handleSave() },
                "onUndoClick" to { viewModel.undo() },
                "onRedoClick" to { viewModel.redo() },
                "onBackClick" to { handleBack() },
                "onPropertiesClick" to { viewModel.toggleSchemeProperties() },
                "onClearClick" to { showClearConfirmDialog = true }
            )
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SchemeCanvasContainer(
            editorState = editorState,
            viewModel = viewModel,
            onAddDeviceDialogChange = { showAddDeviceDialog = it },
            modifier = Modifier.fillMaxSize()
        )

        ShapePropertiesPanel(
            editorState = editorState,
            shapes = shapes,
            viewModel = viewModel,
            onShowPropertiesDialog = { showShapePropertiesDialog = it },
            onShowColorPicker = { type ->
                val selectedId = editorState.selection.selectedShapeId
                if (selectedId != null) {
                    colorPickerTarget = selectedId to type
                    showColorPicker = true
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
        )

        DevicePropertiesPanel(
            editorState = editorState,
            devices = devices,
            allDevices = allDevices,
            viewModel = viewModel,
            onEditDevice = onNavigateToDeviceEdit,
            onViewDetails = onNavigateToDeviceDetail,
            onViewPhotos = onNavigateToDevicePhotos,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 80.dp, end = 16.dp)
        )

        FloatingBottomToolbar(
            canUndo = canUndo,
            canRedo = canRedo,
            editorState = editorState,
            viewModel = viewModel,
            onAddDeviceDialogChange = { showAddDeviceDialog = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
        )

        // Индикатор процесса сохранения (унифицированный оверлей)
        SavingOverlay(
            isSaving = editorState.uiState.isSaving,
            text = "Сохранение схемы..."
        )

        if (showClearConfirmDialog) {
            DeleteConfirmDialog(
                title = "Очистить схему?",
                itemName = "Все элементы схемы будут удалены.",
                message = "Вы уверены, что хотите полностью очистить схему?",
                warning = "Это действие можно отменить с помощью кнопки 'Отмена' в панели управления.",
                confirmText = "Очистить",
                onConfirm = {
                    viewModel.clearScheme()
                    showClearConfirmDialog = false
                },
                onDismiss = { showClearConfirmDialog = false }
            )
        }

        if (showExitConfirmDialog) {
            UnsavedChangesDialog(
                onDismiss = { showExitConfirmDialog = false },
                onConfirmExit = {
                    showExitConfirmDialog = false
                    onNavigateBack()
                },
                onSaveAndExit = {
                    showExitConfirmDialog = false
                    scope.launch {
                        val success = viewModel.saveScheme()
                        if (success) {
                            notificationManager.notifySchemeSaved(editorState.scheme.name)
                            onNavigateBack()
                        } else {
                            notificationManager.notifyError("Ошибка при сохранении")
                        }
                    }
                }
            )
        }

        if (editorState.uiState.showSchemeProperties) {
            val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }
            InfoDialog(
                title = "Информация о схеме",
                icon = Icons.Default.Info,
                onDismiss = { viewModel.toggleSchemeProperties() },
                confirmText = "Закрыть",
                content = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Название: ${editorState.scheme.name}")
                        Text("Описание: ${editorState.scheme.description ?: "нет"}")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        Text("Размер холста: ${editorState.canvasState.width} x ${editorState.canvasState.height}")
                        
                        Text("Обновлено: ${if (editorState.scheme.updatedAt > 0) dateFormat.format(editorState.scheme.updatedAt) else "неизвестно"}")
                        Text("Синхронизировано: ${if (editorState.scheme.lastSyncedAt > 0) dateFormat.format(editorState.scheme.lastSyncedAt) else "никогда"}")

                        Text("Статус: ${if (editorState.uiState.isDirty) "Есть несохраненные изменения" else "Все изменения сохранены"}")
                    }
                }
            )
        }

        if (showAddDeviceDialog) {
            val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()
            AddDeviceToSchemeDialog(
                availableDevices = availableDevices,
                onDismiss = { showAddDeviceDialog = false },
                onDeviceSelected = { device ->
                    viewModel.selectDeviceForPlacement(device.id)
                    showAddDeviceDialog = false
                }
            )
        }

        if (editorState.uiState.showTextInputDialog && editorState.uiState.textInputPosition != null) {
            SimpleTextInputDialog(
                onDismiss = { viewModel.hideTextInputDialog() },
                onConfirm = { text, fontSize ->
                    viewModel.addTextShape(text, editorState.uiState.textInputPosition!!, fontSize)
                    viewModel.hideTextInputDialog()
                }
            )
        }

        if (showColorPicker && colorPickerTarget != null) {
            val (shapeId, type) = colorPickerTarget!!
            val selectedShape = shapes.find { it.id == shapeId }

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
                    },
                    onDismiss = {
                        showColorPicker = false
                        colorPickerTarget = null
                    }
                )
            }
        }

        if (showShapePropertiesDialog && editorState.selection.selectedShapeId != null) {
            val selectedShape = shapes.find {
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

@Composable
private fun SchemeCanvasContainer(
    editorState: EditorState,
    viewModel: SchemeEditorViewModel,
    onAddDeviceDialogChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shapes by viewModel.shapes.collectAsStateWithLifecycle()
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val availableDevices by viewModel.availableDevices.collectAsStateWithLifecycle()
    val allDevices by viewModel.allDevices.collectAsStateWithLifecycle()

    SchemeCanvas(
        editorState = editorState,
        canvasState = editorState.canvasState,
        shapes = shapes,
        devices = devices,
        allDevices = allDevices,
        availableDevices = availableDevices,
        onShapeClick = { viewModel.selectShape(it) },
        onDeviceClick = { viewModel.selectDevice(it) },
        onCanvasClick = { position ->
            when {
                editorState.uiState.pendingShapeMode != null -> viewModel.placeShapeAtPosition(position)
                editorState.uiState.pendingDeviceId != null -> viewModel.placeDeviceAtPosition(position)
                else -> {
                    when (editorState.uiState.mode) {
                        EditorMode.RECTANGLE, EditorMode.LINE, EditorMode.ELLIPSE,
                        EditorMode.RHOMBUS, EditorMode.TEXT -> viewModel.addShape(editorState.uiState.mode, position)
                        EditorMode.DEVICE -> onAddDeviceDialogChange(true)
                        else -> viewModel.clearSelection()
                    }
                }
            }
        },
        onShapeDrag = { id, delta -> viewModel.moveShape(id, delta) },
        onDeviceDrag = { id, delta -> viewModel.moveDevice(id, delta) },
        onTransform = { scale, offset, _ -> viewModel.updateCanvasTransform(scale, offset) },
        onViewportSizeChanged = { w, h -> viewModel.updateViewportSize(w, h) },
        modifier = modifier
    )
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

    if (selectedShape != null &&
        editorState.uiState.showShapeProperties &&
        editorState.uiState.mode != EditorMode.PAN_ZOOM
    ) {
        DraggableCard(
            modifier = modifier.width(260.dp),
            onClose = { viewModel.toggleShapeProperties() } 
        ) {
            EditorDialogHeader(
                title = "Свойства фигуры", 
                onClose = { viewModel.toggleShapeProperties() }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (selectedShape is ComposeText) {
                    Button(
                        onClick = { onShowPropertiesDialog(true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.TextFields, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Свойства текста")
                    }
                } else {
                    Button(
                        onClick = { onShowPropertiesDialog(true) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Размер и поворот")
                    }
                }

                if (selectedShape !is ComposeLine && selectedShape !is ComposeText) {
                    Button(
                        onClick = { onShowColorPicker("fill") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.ColorLens, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Цвет заливки")
                    }
                }

                Button(
                    onClick = { onShowColorPicker("stroke") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Brush, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedShape is ComposeText) "Цвет текста" else "Цвет обводки")
                }
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

    BottomShapeToolbar(
        canUndo = canUndo,
        canRedo = canRedo,
        onUndo = { viewModel.undo() },
        onRedo = { viewModel.redo() },
        editorMode = editorState.uiState.mode,
        selectedShape = selectedShape,
        selectedDevice = selectedDeviceInfo,
        onModeChanged = { mode ->
            viewModel.setMode(mode)
            if (mode == EditorMode.DEVICE) onAddDeviceDialogChange(true)
            if (mode == EditorMode.SELECT || mode == EditorMode.PAN_ZOOM) viewModel.clearSelection()
        },
        onAddDevice = { onAddDeviceDialogChange(true) },
        onShapeMenuClick = { viewModel.toggleShapeProperties() },
        onDeviceMenuClick = { viewModel.toggleDeviceProperties() },
        onDuplicateShape = { 
            editorState.selection.selectedShapeId?.let { id ->
                viewModel.duplicateShape(id)
            }
        },
        onDeleteSelected = {
            editorState.selection.selectedShapeId?.let { _ ->
                viewModel.deleteSelectedShape()
            }
            editorState.selection.selectedDeviceId?.let { id ->
                viewModel.removeDevice(id)
            }
        },
        onShapeSelectedForPlacement = { mode ->
            viewModel.selectShapeForPlacement(mode)
        },
        modifier = modifier
    )
}
