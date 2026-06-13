package com.kipia.management.mobile.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.commands.*
import com.kipia.management.mobile.data.entities.*
import com.kipia.management.mobile.managers.Command
import com.kipia.management.mobile.managers.CommandManager
import com.kipia.management.mobile.managers.DeviceManager
import com.kipia.management.mobile.managers.ShapeManager
import com.kipia.management.mobile.repository.DeviceLocationRepository
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.SchemeRepository
import com.kipia.management.mobile.ui.components.scheme.shapes.*
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils
import com.kipia.management.mobile.ui.components.scheme.utils.toComposeShape
import com.kipia.management.mobile.ui.components.scheme.utils.toShapeData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SchemeEditorViewModel @Inject constructor(
    private val schemeRepository: SchemeRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceLocationRepository: DeviceLocationRepository,
    private val shapeManager: ShapeManager,
    private val deviceManager: DeviceManager,
    private val commandManager: CommandManager
) : ViewModel() {

    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    val shapes = shapeManager.shapes
    val devices = deviceManager.devices
    val allDevices = deviceManager.allDevices

    val canUndo = commandManager.canUndo
    val canRedo = commandManager.canRedo

    val selection = _editorState.map { it.selection }.stateIn(viewModelScope, SharingStarted.Eagerly, SelectionState())

    val isSchemeEmpty = combine(shapes, devices) { s, d -> s.isEmpty() && d.isEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val availableDevices: StateFlow<List<Device>> = combine(
        deviceRepository.getAllDevices(),
        devices,
        _editorState.map { it.scheme.name }.distinctUntilChanged()
    ) { all, placed, schemeName ->
        val placedIds = placed.map { it.deviceId }.toSet()
        all.filter { device ->
            device.id !in placedIds &&
                    device.location.trim().equals(schemeName.trim(), ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun initScheme(schemeId: Int) {
        if (_editorState.value.uiState.isLoading) return
        if (schemeId > 0 && _editorState.value.scheme.id == schemeId) return

        viewModelScope.launch {
            _editorState.update { it.copy(uiState = it.uiState.copy(isLoading = true)) }
            try {
                deviceRepository.getAllDevices().firstOrNull()?.let {
                    deviceManager.setAllDevices(it)
                }

                val scheme = if (schemeId > 0) {
                    schemeRepository.getSchemeById(schemeId) ?: Scheme.createEmpty("Новая схема")
                } else {
                    Scheme.createEmpty("Новая схема")
                }

                val schemeData = scheme.getSchemeData()

                // 1. Загружаем фигуры
                shapeManager.clear()
                schemeData.shapes.forEach { shapeData ->
                    try {
                        shapeManager.addShape(shapeData.toComposeShape())
                    } catch (e: Exception) {
                        Timber.e(e, "Ошибка загрузки фигуры")
                    }
                }

                // 2. Загружаем приборы (с поддержкой миграции из JSON)
                deviceManager.clear()
                val locations = deviceLocationRepository.getLocationsForScheme(scheme.id)
                
                if (locations.isNotEmpty()) {
                    locations.forEach { loc ->
                        deviceManager.addDevice(loc.deviceId, Offset(loc.x.toFloat(), loc.y.toFloat()))
                        deviceManager.rotateDevice(loc.deviceId, loc.rotation.toFloat())
                    }
                } else if (schemeData.devices.isNotEmpty()) {
                    // Если в таблице пусто, берем из JSON (формат JavaFX или старый кэш)
                    schemeData.devices.forEach { sd ->
                        deviceManager.addDevice(sd.deviceId, Offset(sd.x, sd.y))
                        deviceManager.rotateDevice(sd.deviceId, sd.rotation)
                        
                        // Сохраняем в новую таблицу для ускорения доступа
                        deviceLocationRepository.saveLocation(
                            DeviceLocation(
                                deviceId = sd.deviceId, schemeId = scheme.id,
                                x = sd.x.toDouble(), y = sd.y.toDouble(), rotation = sd.rotation.toDouble()
                            )
                        )
                    }
                }

                _editorState.update {
                    it.copy(
                        scheme = scheme,
                        canvasState = CanvasState(
                            width = schemeData.width.toInt().coerceAtLeast(100),
                            height = schemeData.height.toInt().coerceAtLeast(100),
                            backgroundColor = ShapeUtils.parseHexColor(schemeData.backgroundColor, Color.White),
                            backgroundImage = schemeData.backgroundImage,
                            showGrid = schemeData.gridEnabled,
                            gridSize = schemeData.gridSize
                        ),
                        uiState = it.uiState.copy(isLoading = false, isDirty = false)
                    )
                }
                commandManager.clear()

            } catch (e: Exception) {
                Timber.e(e, "Ошибка инициализации схемы")
                _editorState.update { it.copy(uiState = it.uiState.copy(isLoading = false, error = e.message)) }
            }
        }
    }

    private fun markAsDirty() {
        if (!_editorState.value.uiState.isDirty) {
            _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
        }
    }

    fun addDevice(deviceId: Int, position: Offset) {
        val canvas = _editorState.value.canvasState
        val clampedPos = ShapeUtils.clampDevicePosition(
            position.x, position.y,
            canvas.width.toFloat(), canvas.height.toFloat()
        )
        commandManager.execute(
            AddDeviceCommand(deviceManager, { markAsDirty() }, deviceId, clampedPos)
        )
    }

    fun moveDevice(deviceId: Int, delta: Offset) {
        val canvas = _editorState.value.canvasState
        val device = devices.value.find { it.deviceId == deviceId } ?: return

        val newPos = ShapeUtils.clampDevicePosition(
            device.x + delta.x,
            device.y + delta.y,
            canvas.width.toFloat(),
            canvas.height.toFloat()
        )

        val adjustedDelta = Offset(newPos.x - device.x, newPos.y - device.y)
        if (adjustedDelta != Offset.Zero) {
            commandManager.execute(
                MoveDeviceCommand(deviceManager, { markAsDirty() }, deviceId, adjustedDelta)
            )
        }
    }

    fun rotateDevice(deviceId: Int, angleDeg: Float) {
        val currentDevice = devices.value.find { it.deviceId == deviceId } ?: return
        val previousAngle = currentDevice.rotation
        if (previousAngle == angleDeg) return

        commandManager.execute(object : Command {
            override fun execute() {
                deviceManager.rotateDevice(deviceId, angleDeg)
                markAsDirty()
            }
            override fun undo() {
                deviceManager.rotateDevice(deviceId, previousAngle)
                markAsDirty()
            }
        })
    }

    fun removeDevice(deviceId: Int) {
        commandManager.execute(
            RemoveDeviceCommand(deviceManager, { markAsDirty() }, deviceId)
        )
    }

    fun selectDeviceForPlacement(deviceId: Int) {
        _editorState.update { it.copy(uiState = it.uiState.copy(mode = EditorMode.DEVICE, pendingDeviceId = deviceId)) }
    }

    fun placeDeviceAtPosition(position: Offset) {
        val pendingDeviceId = _editorState.value.uiState.pendingDeviceId
        if (pendingDeviceId != null) {
            addDevice(pendingDeviceId, position)
            _editorState.update { it.copy(uiState = it.uiState.copy(mode = EditorMode.SELECT, pendingDeviceId = null)) }
        }
    }

    fun selectShapeForPlacement(shapeMode: EditorMode) {
        _editorState.update { it.copy(uiState = it.uiState.copy(mode = shapeMode, pendingShapeMode = shapeMode)) }
    }

    fun placeShapeAtPosition(position: Offset) {
        val pendingShapeMode = _editorState.value.uiState.pendingShapeMode ?: return
        if (pendingShapeMode == EditorMode.TEXT) {
            _editorState.update { it.copy(uiState = it.uiState.copy(showTextInputDialog = true, textInputPosition = position, pendingShapeMode = null)) }
        } else {
            addShape(pendingShapeMode, position)
            _editorState.update { it.copy(uiState = it.uiState.copy(mode = EditorMode.SELECT, pendingShapeMode = null)) }
        }
    }

    fun addShape(mode: EditorMode, position: Offset) {
        val canvas = _editorState.value.canvasState
        val template = ComposeShapeFactory.create(mode).apply {
            if (this is ComposeLine) {
                startX = position.x - 50f; startY = position.y
                endX = position.x + 50f; endY = position.y
                x = startX; y = startY; width = 100f; height = 20f
            } else {
                x = position.x - width / 2; y = position.y - height / 2
            }
        }
        val clampedShape = ShapeUtils.clampShape(template, canvas.width.toFloat(), canvas.height.toFloat())
        commandManager.execute(AddShapeCommand(shapeManager, { markAsDirty() }, clampedShape))
    }

    fun addTextShape(text: String, position: Offset, fontSize: Float) {
        val canvas = _editorState.value.canvasState
        val template = ComposeText(id = "text_${UUID.randomUUID()}", text = text, fontSize = fontSize, x = position.x, y = position.y)
        val clampedShape = ShapeUtils.clampShape(template, canvas.width.toFloat(), canvas.height.toFloat())
        commandManager.execute(AddShapeCommand(shapeManager, { markAsDirty() }, clampedShape))
    }

    fun moveShape(shapeId: String, delta: Offset) {
        val canvas = _editorState.value.canvasState
        val shape = shapes.value.find { it.id == shapeId } ?: return

        val newPos = ShapeUtils.clampShapePosition(
            shape,
            shape.x + delta.x,
            shape.y + delta.y,
            canvas.width.toFloat(),
            canvas.height.toFloat()
        )

        val adjustedDelta = Offset(newPos.x - shape.x, newPos.y - shape.y)
        if (adjustedDelta != Offset.Zero) {
            commandManager.execute(MoveShapeCommand(shapeManager, { markAsDirty() }, shapeId, adjustedDelta))
        }
    }

    fun updateShape(shape: ComposeShape) {
        val canvas = _editorState.value.canvasState
        val clampedShape = ShapeUtils.clampShape(shape, canvas.width.toFloat(), canvas.height.toFloat())
        val oldShape = shapes.value.find { it.id == clampedShape.id }?.copy() ?: return

        commandManager.execute(object : Command {
            override fun execute() {
                shapeManager.updateShape(clampedShape.id) { clampedShape }
                markAsDirty()
            }
            override fun undo() {
                shapeManager.updateShape(clampedShape.id) { oldShape }
                markAsDirty()
            }
        })
    }

    fun updateShapeFillColor(shapeId: String, color: Color) {
        val oldColor = shapes.value.find { it.id == shapeId }?.fillColor ?: Color.Transparent
        commandManager.execute(UpdateShapeFillColorCommand(shapeManager, _editorState, shapeId, color, oldColor))
    }

    fun updateShapeStrokeColor(shapeId: String, color: Color) {
        val oldColor = shapes.value.find { it.id == shapeId }?.strokeColor ?: Color.Black
        commandManager.execute(UpdateShapeStrokeColorCommand(shapeManager, _editorState, shapeId, color, oldColor))
    }

    fun duplicateShape(shapeId: String) {
        val original = shapes.value.find { it.id == shapeId } ?: return
        val canvas = _editorState.value.canvasState
        val duplicate = original.copyWithId().copyWithPosition(original.x + 20f, original.y + 20f)
        val clampedDuplicate = ShapeUtils.clampShape(duplicate, canvas.width.toFloat(), canvas.height.toFloat())
        commandManager.execute(AddShapeCommand(shapeManager, { markAsDirty() }, clampedDuplicate))
    }

    fun deleteSelectedShape() {
        val shapeId = _editorState.value.selection.selectedShapeId ?: return
        val shape = shapes.value.find { it.id == shapeId } ?: return
        commandManager.execute(DeleteShapeCommand(shapeManager, _editorState, shape))
    }

    fun clearSelection() {
        _editorState.update { it.copy(selection = SelectionState()) }
    }

    fun clearScheme() {
        val currentShapes = shapes.value.toList()
        val currentDevices = devices.value.toList()
        commandManager.execute(object : Command {
            override fun execute() {
                shapeManager.clear(); deviceManager.clear(); markAsDirty()
            }
            override fun undo() {
                currentShapes.forEach { shapeManager.addShape(it) }
                currentDevices.forEach { deviceManager.addDevice(it.deviceId, Offset(it.x, it.y)) }
                markAsDirty()
            }
        })
    }

    fun updateViewportSize(width: Int, height: Int) {
        _editorState.update { it.copy(uiState = it.uiState.copy(viewportWidth = width, viewportHeight = height)) }
    }

    fun updateCanvasTransform(scale: Float, offset: Offset) {
        _editorState.update { it.copy(canvasState = it.canvasState.copy(scale = scale, offset = offset)) }
    }

    fun setMode(mode: EditorMode) {
        _editorState.update { it.copy(uiState = it.uiState.copy(mode = mode)) }
    }

    fun selectShape(shapeId: String?) {
        _editorState.update { it.copy(selection = SelectionState(selectedShapeId = shapeId)) }
    }

    fun selectDevice(deviceId: Int?) {
        _editorState.update { it.copy(selection = SelectionState(selectedDeviceId = deviceId)) }
    }

    fun toggleShapeProperties() {
        _editorState.update { it.copy(uiState = it.uiState.copy(showShapeProperties = !it.uiState.showShapeProperties)) }
    }

    fun toggleSchemeProperties() {
        _editorState.update { it.copy(uiState = it.uiState.copy(showSchemeProperties = !it.uiState.showSchemeProperties)) }
    }

    fun toggleDeviceProperties() {
        _editorState.update { it.copy(uiState = it.uiState.copy(showDeviceProperties = !it.uiState.showDeviceProperties)) }
    }

    fun hideTextInputDialog() {
        _editorState.update { it.copy(uiState = it.uiState.copy(showTextInputDialog = false, mode = EditorMode.SELECT)) }
    }

    suspend fun saveScheme(): Boolean {
        val currentState = _editorState.value
        if (currentState.uiState.isLoading || currentState.uiState.isSaving || currentState.scheme.name.isBlank()) return false

        _editorState.update { it.copy(uiState = it.uiState.copy(isSaving = true)) }
        
        delay(1500)

        return try {
            val currentShapes = shapes.value
            val currentDevices = devices.value

            val schemeData = SchemeData(
                width = currentState.canvasState.width.toDouble(),
                height = currentState.canvasState.height.toDouble(),
                backgroundColor = with(ShapeUtils) { currentState.canvasState.backgroundColor.toHex() },
                backgroundImage = currentState.canvasState.backgroundImage,
                gridEnabled = currentState.canvasState.showGrid,
                gridSize = currentState.canvasState.gridSize,
                devices = currentDevices,
                shapes = currentShapes.map { it.toShapeData() }
            )

            val updatedScheme = currentState.scheme.setSchemeData(schemeData)
            val schemeId = if (currentState.scheme.id == 0) {
                schemeRepository.insertSchemeWithTimestamp(updatedScheme).toInt()
            } else {
                schemeRepository.updateSchemeWithTimestamp(updatedScheme)
                currentState.scheme.id
            }

            val oldLocations = deviceLocationRepository.getLocationsForScheme(schemeId)
            val currentDeviceIds = currentDevices.map { it.deviceId }.toSet()

            oldLocations.forEach { if (it.deviceId !in currentDeviceIds) deviceLocationRepository.deleteLocation(it) }

            currentDevices.forEach { device ->
                deviceLocationRepository.saveLocation(
                    DeviceLocation(
                        deviceId = device.deviceId, schemeId = schemeId,
                        x = device.x.toDouble(), y = device.y.toDouble(), rotation = device.rotation.toDouble()
                    )
                )
                deviceRepository.getDeviceByIdSync(device.deviceId)?.let { deviceRepository.updateDeviceWithTimestamp(it) }
            }

            _editorState.update { 
                it.copy(
                    scheme = updatedScheme.copy(id = schemeId), 
                    uiState = it.uiState.copy(isDirty = false, isSaving = false)
                ) 
            }
            true
        } catch (e: Exception) {
            Timber.e(e, "Ошибка сохранения схемы")
            _editorState.update { it.copy(uiState = it.uiState.copy(isSaving = false)) }
            false
        }
    }

    fun undo() { commandManager.undo(); markAsDirty() }
    fun redo() { commandManager.redo(); markAsDirty() }
}

enum class EditorMode { NONE, SELECT, RECTANGLE, LINE, ELLIPSE, TEXT, RHOMBUS, DEVICE, PAN_ZOOM }

data class EditorState(
    val scheme: Scheme = Scheme.createEmpty(),
    val canvasState: CanvasState = CanvasState(),
    val selection: SelectionState = SelectionState(),
    val uiState: SchemeEditorUiState = SchemeEditorUiState()
)

data class CanvasState(
    val width: Int = 2000,
    val height: Int = 1200,
    val backgroundColor: Color = Color.White,
    val backgroundImage: String? = null,
    val showGrid: Boolean = true,
    val gridSize: Int = 50,
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
    val dimOutsideBounds: Boolean = true
)

data class SelectionState(
    val selectedShapeId: String? = null,
    val selectedDeviceId: Int? = null
)

data class SchemeEditorUiState(
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDirty: Boolean = false,
    val error: String? = null,
    val mode: EditorMode = EditorMode.SELECT,
    val pendingDeviceId: Int? = null,
    val pendingShapeMode: EditorMode? = null,
    val showShapeProperties: Boolean = false,
    val showSchemeProperties: Boolean = false,
    val showDeviceProperties: Boolean = false,
    val showTextInputDialog: Boolean = false,
    val textInputPosition: Offset? = null,
    val viewportWidth: Int = 0,
    val viewportHeight: Int = 0
)
