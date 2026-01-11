package com.kipia.management.mobile.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.data.entities.SchemeData
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.data.entities.toShapeData
import com.kipia.management.mobile.managers.AddShapeCommand
import com.kipia.management.mobile.managers.CommandManager
import com.kipia.management.mobile.managers.DeleteShapeCommand
import com.kipia.management.mobile.managers.MoveShapeCommand
import com.kipia.management.mobile.managers.ShapeManager
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.SchemeRepository
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShapeFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchemeEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val schemeRepository: SchemeRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    // Получаем schemeId из SavedStateHandle
    private val schemeId: Int? = savedStateHandle["schemeId"]

    private val commandManager = CommandManager()
    private val _uiState = MutableStateFlow(SchemeEditorUiState())
    val uiState: StateFlow<SchemeEditorUiState> = _uiState.asStateFlow()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    // Менеджер фигур
    private val shapeManager = ShapeManager()

    // Текущий режим редактора
    private var _editorMode = MutableStateFlow(EditorMode.SELECT)
    val editorMode: StateFlow<EditorMode> = _editorMode.asStateFlow()

    init {
        loadDevices()
        loadScheme()
    }

    private fun loadScheme() {
        viewModelScope.launch {
            // Загружаем схему только если schemeId не null
            schemeId?.let { id ->
                val scheme = schemeRepository.getSchemeById(id)
                scheme?.let {
                    _uiState.update { state ->
                        state.copy(
                            scheme = it,
                            schemeData = it.getSchemeData(),
                            isNewScheme = false
                        )
                    }
                    // Загружаем фигуры из данных схемы
                    loadShapesFromSchemeData(it.getSchemeData())
                }
            } ?: run {
                // Если schemeId null, создаем новую схему
                _uiState.update { state ->
                    state.copy(
                        isNewScheme = true
                    )
                }
            }
        }
    }

    private fun loadDevices() {
        viewModelScope.launch {
            _devices.value = deviceRepository.getAllDevicesSync()
        }
    }

    fun updateSchemeName(name: String) {
        _uiState.update { state ->
            state.copy(
                scheme = state.scheme.copy(name = name),
                isDirty = true
            )
        }
    }

    fun updateSchemeDescription(description: String) {
        _uiState.update { state ->
            state.copy(
                scheme = state.scheme.copy(description = description.takeIf { it.isNotBlank() }),
                isDirty = true
            )
        }
    }

    fun updateSchemeData(schemeData: SchemeData) {
        _uiState.update { state ->
            state.copy(
                schemeData = schemeData,
                isDirty = true
            )
        }
    }

    fun addDevice(deviceId: Int, position: Offset) {
        _uiState.update { state ->
            val existingDevice = state.schemeData.devices.find { it.deviceId == deviceId }
            if (existingDevice != null) {
                // Обновляем позицию существующего устройства
                val updatedDevices = state.schemeData.devices.map {
                    if (it.deviceId == deviceId) {
                        it.copy(x = position.x, y = position.y)
                    } else it
                }
                state.copy(
                    schemeData = state.schemeData.copy(devices = updatedDevices),
                    isDirty = true
                )
            } else {
                // Добавляем новое устройство
                val newDevice = SchemeDevice(
                    deviceId = deviceId,
                    x = position.x,
                    y = position.y,
                    zIndex = state.schemeData.devices.size
                )
                val updatedDevices = state.schemeData.devices + newDevice
                state.copy(
                    schemeData = state.schemeData.copy(devices = updatedDevices),
                    isDirty = true
                )
            }
        }
    }

    fun updateDevicePosition(deviceId: Int, position: Offset) {
        _uiState.update { state ->
            val updatedDevices = state.schemeData.devices.map {
                if (it.deviceId == deviceId) {
                    it.copy(x = position.x, y = position.y)
                } else it
            }
            state.copy(
                schemeData = state.schemeData.copy(devices = updatedDevices),
                isDirty = true
            )
        }
    }

    fun updateDeviceRotation(deviceId: Int, rotation: Float) {
        _uiState.update { state ->
            val updatedDevices = state.schemeData.devices.map {
                if (it.deviceId == deviceId) {
                    it.copy(rotation = rotation)
                } else it
            }
            state.copy(
                schemeData = state.schemeData.copy(devices = updatedDevices),
                isDirty = true
            )
        }
    }

    fun removeDevice(deviceId: Int) {
        _uiState.update { state ->
            val updatedDevices = state.schemeData.devices.filter { it.deviceId != deviceId }
            state.copy(
                schemeData = state.schemeData.copy(devices = updatedDevices),
                isDirty = true
            )
        }
    }

    fun setBackgroundColor(color: Color) {
        _uiState.update { state ->
            state.copy(
                schemeData = state.schemeData.copy(backgroundColor = color.toHex()),
                isDirty = true
            )
        }
    }

    fun setBackgroundImage(imageUri: String?) {
        _uiState.update { state ->
            state.copy(
                schemeData = state.schemeData.copy(backgroundImage = imageUri),
                isDirty = true
            )
        }
    }

    fun toggleGrid() {
        _uiState.update { state ->
            state.copy(
                schemeData = state.schemeData.copy(gridEnabled = !state.schemeData.gridEnabled),
                isDirty = true
            )
        }
    }

    fun updateGridSize(size: Int) {
        _uiState.update { state ->
            state.copy(
                schemeData = state.schemeData.copy(gridSize = size),
                isDirty = true
            )
        }
    }

    fun setCanvasSize(width: Int, height: Int) {
        _uiState.update { state ->
            state.copy(
                schemeData = state.schemeData.copy(width = width, height = height),
                isDirty = true
            )
        }
    }

    fun setSelectedDevice(deviceId: Int?) {
        _uiState.update { state ->
            state.copy(selectedDeviceId = deviceId)
        }
    }

    suspend fun saveScheme(): Boolean {
        return try {
            val schemeWithData = _uiState.value.scheme.setSchemeData(_uiState.value.schemeData)
            if (_uiState.value.isNewScheme) {
                schemeRepository.insertScheme(schemeWithData)
            } else {
                schemeRepository.updateScheme(schemeWithData)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // Методы для работы с фигурами
    fun setEditorMode(mode: EditorMode) {
        _editorMode.value = mode
        shapeManager.clearSelection()
    }

    fun addRectangle() {
        val rectangle = ComposeShapeFactory.createRectangle()
        rectangle.x = _uiState.value.schemeData.width / 2f - 50
        rectangle.y = _uiState.value.schemeData.height / 2f - 30
        shapeManager.addShape(rectangle)
        updateShapesInSchemeData()
    }

    fun addLine() {
        val line = ComposeShapeFactory.createLine()
        line.x = _uiState.value.schemeData.width / 2f - 50
        line.y = _uiState.value.schemeData.height / 2f
        shapeManager.addShape(line)
        updateShapesInSchemeData()
    }

    fun addEllipse() {
        val ellipse = ComposeShapeFactory.createEllipse()
        ellipse.x = _uiState.value.schemeData.width / 2f - 40
        ellipse.y = _uiState.value.schemeData.height / 2f - 25
        shapeManager.addShape(ellipse)
        updateShapesInSchemeData()
    }

    fun addText() {
        val text = ComposeShapeFactory.createText()
        text.x = _uiState.value.schemeData.width / 2f - 50
        text.y = _uiState.value.schemeData.height / 2f - 15
        shapeManager.addShape(text)
        updateShapesInSchemeData()
    }

    fun selectShapeAt(point: Offset) {
        shapeManager.selectShapeAt(point)
        updateShapesInSchemeData()
    }

    fun deleteSelectedShape() {
        shapeManager.getSelectedShape()?.let { selectedShape ->
            shapeManager.removeShape(selectedShape)
            updateShapesInSchemeData()
        }
    }

    fun moveSelectedShape(deltaX: Float, deltaY: Float) {
        shapeManager.moveSelectedShape(deltaX, deltaY)
        updateShapesInSchemeData()
    }

    private fun updateShapesInSchemeData() {
        val shapesData = shapeManager.getAllShapes().map { it.toShapeData() }
        _uiState.update { state ->
            state.copy(
                schemeData = state.schemeData.copy(shapes = shapesData),
                isDirty = true
            )
        }
    }

    fun loadShapesFromSchemeData(schemeData: SchemeData) {
        shapeManager.clearShapes() // Используем новый метод

        schemeData.shapes.forEach { shapeData ->
            shapeManager.addShape(shapeData.toComposeShape())
        }
    }

    fun addRectangleWithUndo() {
        val rectangle = ComposeShapeFactory.createRectangle()
        rectangle.x = _uiState.value.schemeData.width / 2f - 50
        rectangle.y = _uiState.value.schemeData.height / 2f - 30

        val command = AddShapeCommand(shapeManager, rectangle)
        commandManager.execute(command)
        updateShapesInSchemeData()
    }

    fun deleteSelectedShapeWithUndo() {
        shapeManager.getSelectedShape()?.let { selectedShape ->
            val command = DeleteShapeCommand(shapeManager, selectedShape)
            commandManager.execute(command)
            updateShapesInSchemeData()
        }
    }

    fun moveSelectedShapeWithUndo(deltaX: Float, deltaY: Float) {
        shapeManager.getSelectedShape()?.let { shape ->
            val oldX = shape.x
            val oldY = shape.y
            val newX = oldX + deltaX
            val newY = oldY + deltaY

            val command = MoveShapeCommand(shape, oldX, oldY, newX, newY)
            commandManager.execute(command)
            updateShapesInSchemeData()
        }
    }

    fun undo() {
        commandManager.undo()
        updateShapesInSchemeData()
    }

    fun redo() {
        commandManager.redo()
        updateShapesInSchemeData()
    }

    fun canUndo(): Boolean = commandManager.canUndo()
    fun canRedo(): Boolean = commandManager.canRedo()

    // Добавляем метод для установки схемы при создании
    fun initializeForNewScheme() {
        _uiState.update { state ->
            state.copy(
                scheme = Scheme.createEmpty(),
                schemeData = SchemeData(),
                isNewScheme = true
            )
        }
    }
}

data class SchemeEditorUiState(
    val scheme: Scheme = Scheme.createEmpty(),
    val schemeData: SchemeData = SchemeData(),
    val isNewScheme: Boolean = true,
    val isDirty: Boolean = false,
    val selectedDeviceId: Int? = null,
    val showPropertiesPanel: Boolean = false,
    val showAddDeviceDialog: Boolean = false,
    val showBackgroundPicker: Boolean = false,
    val showGridSettings: Boolean = false,
    val showCanvasSettings: Boolean = false,
    val selectedShape: ComposeShape? = null,
    val editorMode: EditorMode = EditorMode.SELECT
)

enum class EditorMode {
    SELECT,     // Режим выбора
    RECTANGLE,  // Добавление прямоугольника
    LINE,       // Добавление линии
    ELLIPSE,    // Добавление эллипса
    TEXT,       // Добавление текста
    RHOMBUS,    // Добавление бабочки (ромба)
    DEVICE      // Добавление прибора
}

private fun Color.toHex(): String {
    val alpha = (alpha * 255).toInt()
    val red = (red * 255).toInt()
    val green = (green * 255).toInt()
    val blue = (blue * 255).toInt()
    return String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
}