package com.kipia.management.mobile.viewmodel

import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.DeviceLocation
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.data.entities.SchemeData
import com.kipia.management.mobile.data.entities.toShapeData
import com.kipia.management.mobile.managers.*
import com.kipia.management.mobile.repository.DeviceLocationRepository
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.SchemeRepository
import com.kipia.management.mobile.ui.components.scheme.shapes.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

// Data классы для состояния
data class CanvasState(
    val scale: Float = 1f,
    val offset: Offset = Offset.Zero,
    val width: Int = 1000,
    val height: Int = 1000,
    val backgroundColor: Color = Color.White,
    val backgroundImage: String? = null,
    val gridEnabled: Boolean = false,
    val gridSize: Int = 50
) {
    // Простое преобразование координат без матриц
    fun screenToCanvas(screenPoint: Offset): Offset {
        return Offset(
            (screenPoint.x - offset.x) / scale,
            (screenPoint.y - offset.y) / scale
        )
    }

    fun canvasToScreen(canvasPoint: Offset): Offset {
        return Offset(
            canvasPoint.x * scale + offset.x,
            canvasPoint.y * scale + offset.y
        )
    }

    fun copyWithTransform(scale: Float, offset: Offset): CanvasState {
        return this.copy(scale = scale, offset = offset)
    }
}

data class SelectionState(
    val selectedShapeId: String? = null,
    val selectedDeviceId: Int? = null
)

data class EditorUIState(
    val mode: EditorMode = EditorMode.NONE, // Меняем SELECT на NONE
    val isDirty: Boolean = false,
    val showShapeProperties: Boolean = false,
    val showDeviceProperties: Boolean = false,
    val showTextInputDialog: Boolean = false,
    val textInputPosition: Offset? = null
)

data class EditorState(
    val scheme: Scheme = Scheme.createEmpty(),
    val canvasState: CanvasState = CanvasState(),
    val selection: SelectionState = SelectionState(),
    val uiState: EditorUIState = EditorUIState()
)

@HiltViewModel
class SchemeEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val schemeRepository: SchemeRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceLocationRepository: DeviceLocationRepository
) : ViewModel() {

    private val schemeId: Int? = savedStateHandle.get<String>("schemeId")?.toIntOrNull()

    // Менеджеры
    private val shapeManager = ShapeManager()
    private val deviceManager = DeviceManager()
    private val commandManager = CommandManager()

    // Единый UI State
    private val _editorState = MutableStateFlow(EditorState())
    val editorState = _editorState.asStateFlow()

    // Отдельные Flow для производительности
    val shapes = shapeManager.shapes
    val devices = deviceManager.devices
    val availableDevices = deviceManager.availableDevices
    val canUndo = commandManager.canUndo
    val canRedo = commandManager.canRedo

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // Загружаем все устройства
                val allDevices = deviceRepository.getAllDevicesSync()
                deviceManager.setAvailableDevices(allDevices)

                // Загружаем схему
                schemeId?.let { id ->
                    val scheme = schemeRepository.getSchemeById(id)
                    scheme?.let {
                        val schemeData = it.getSchemeData()
                        _editorState.update { state ->
                            state.copy(
                                scheme = it,
                                canvasState = state.canvasState.copy(
                                    width = schemeData.width,
                                    height = schemeData.height,
                                    backgroundColor = parseColor(schemeData.backgroundColor ?: "#FFFFFFFF"),
                                    backgroundImage = schemeData.backgroundImage
                                ),
                                uiState = state.uiState.copy(isDirty = false)
                            )
                        }

                        // Загружаем фигуры
                        schemeData.shapes.forEach { shapeData ->
                            shapeManager.addShape(shapeData.toComposeShape())
                        }

                        // Загружаем устройства схемы
                        val locations = deviceLocationRepository.getLocationsForScheme(id)
                        locations.forEach { location ->
                            deviceManager.addDevice(
                                location.deviceId,
                                Offset(location.x, location.y)
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ============ ТРАНСФОРМАЦИЯ КАНВАСА ============

    fun updateCanvasTransform(scale: Float, offset: Offset) {
        Timber.d("ViewModel.updateCanvasTransform - scale: $scale, offset: $offset")

        // Получаем текущее состояние
        val currentState = _editorState.value

        // Создаем новое состояние с обновленными значениями
        val newCanvasState = currentState.canvasState.copy(
            scale = scale,
            offset = offset
        )

        // Обновляем состояние
        _editorState.update { state ->
            state.copy(canvasState = newCanvasState)
        }

        Timber.d("CanvasState updated - new scale: ${newCanvasState.scale}, offset: ${newCanvasState.offset}")
    }

    // ============ ДЕЙСТВИЯ С ФИГУРАМИ ============

    fun addShape(shape: ComposeShape, position: Offset) {
        val newShape = shape.copyWithId().apply {
            x = position.x
            y = position.y
        }

        commandManager.execute(object : Command {
            override fun execute() {
                shapeManager.addShape(newShape)
                _editorState.update {
                    it.copy(
                        uiState = it.uiState.copy(
                            mode = EditorMode.NONE, // Меняем SELECT на NONE
                            isDirty = true
                        )
                    )
                }
            }

            override fun undo() {
                shapeManager.removeShape(newShape.id)
                _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
            }
        })
    }

    fun moveShape(shapeId: String, delta: Offset) {
        shapeManager.moveShape(shapeId, delta)
        _editorState.update {
            it.copy(
                uiState = it.uiState.copy(isDirty = true)
            )
        }
    }

    fun deleteSelectedShape() {
        val shapeId = _editorState.value.selection.selectedShapeId ?: return

        commandManager.execute(object : Command {
            private val shape = shapes.value.find { it.id == shapeId }

            override fun execute() {
                shapeManager.removeShape(shapeId)
                _editorState.update { state ->
                    state.copy(
                        selection = SelectionState(),
                        uiState = state.uiState.copy(
                            isDirty = true,
                            showShapeProperties = false
                        )
                    )
                }
            }

            override fun undo() {
                shape?.let { shapeManager.addShape(it) }
                _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
            }
        })
    }

    fun updateShapeFillColor(shapeId: String, color: Color) {
        shapeManager.updateShape(shapeId) { shape ->
            shape.copyWithFillColor(color)
        }
        _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
    }

    fun updateShapeStrokeColor(shapeId: String, color: Color) {
        shapeManager.updateShape(shapeId) { shape ->
            shape.copyWithStrokeColor(color)
        }
        _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
    }

    fun updateShapeStrokeWidth(shapeId: String, width: Float) {
        shapeManager.updateShape(shapeId) { shape ->
            shape.copyWithStrokeWidth(width)
        }
        _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
    }

    fun duplicateShape(shapeId: String) {
        val originalShape = shapes.value.find { it.id == shapeId } ?: return
        val duplicate = ComposeShapeFactory.duplicateShape(originalShape)
        duplicate.x = originalShape.x + 20
        duplicate.y = originalShape.y + 20

        commandManager.execute(object : Command {
            override fun execute() {
                shapeManager.addShape(duplicate)
                _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
            }

            override fun undo() {
                shapeManager.removeShape(duplicate.id)
                _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
            }
        })
    }

    // ============ ДЕЙСТВИЯ С УСТРОЙСТВАМИ ============

    fun addDevice(deviceId: Int, position: Offset) {
        commandManager.execute(object : Command {
            override fun execute() {
                deviceManager.addDevice(deviceId, position)
                _editorState.update {
                    it.copy(
                        uiState = it.uiState.copy(
                            mode = EditorMode.NONE, // Меняем SELECT на NONE
                            isDirty = true
                        )
                    )
                }
            }

            override fun undo() {
                deviceManager.removeDevice(deviceId)
                _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
            }
        })
    }

    fun moveDevice(deviceId: Int, delta: Offset) {
        deviceManager.updateDevicePosition(deviceId, delta)
        _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
    }

    fun removeDevice(deviceId: Int) {
        commandManager.execute(object : Command {
            override fun execute() {
                deviceManager.removeDevice(deviceId)
                _editorState.update { state ->
                    state.copy(
                        selection = SelectionState(),
                        uiState = state.uiState.copy(
                            isDirty = true,
                            showDeviceProperties = false
                        )
                    )
                }
            }

            override fun undo() {
                // Для undo нужно сохранять позицию, но для простоты пока так
                _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
            }
        })
    }

    fun updateDeviceRotation(deviceId: Int, rotation: Float) {
        deviceManager.updateDevice(deviceId) { device ->
            device.copy(rotation = rotation)
        }
        _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
    }

    // ============ ВЫДЕЛЕНИЕ ============

    fun selectShape(shapeId: String?) {
        _editorState.update { state ->
            state.copy(
                selection = SelectionState(
                    selectedShapeId = shapeId,
                    selectedDeviceId = null
                ),
                uiState = state.uiState.copy(
                    showShapeProperties = false,
                    showDeviceProperties = false
                )
            )
        }
    }

    fun selectDevice(deviceId: Int?) {
        _editorState.update { state ->
            state.copy(
                selection = SelectionState(
                    selectedDeviceId = deviceId,
                    selectedShapeId = null
                ),
                uiState = state.uiState.copy(
                    showShapeProperties = false,
                    showDeviceProperties = false
                )
            )
        }
    }

    fun clearSelection() {
        _editorState.update { state ->
            state.copy(
                selection = SelectionState(),
                uiState = state.uiState.copy(
                    showShapeProperties = false,
                    showDeviceProperties = false
                )
            )
        }
    }

    fun toggleShapeProperties() {
        _editorState.update { state ->
            state.copy(
                uiState = state.uiState.copy(
                    showShapeProperties = !state.uiState.showShapeProperties
                )
            )
        }
    }

    fun toggleDeviceProperties() {
        _editorState.update { state ->
            state.copy(
                uiState = state.uiState.copy(
                    showDeviceProperties = !state.uiState.showDeviceProperties
                )
            )
        }
    }

    // ============ РЕЖИМЫ ============

    fun setMode(mode: EditorMode) {
        Timber.d("ViewModel.setMode: $mode")
        if (mode == EditorMode.NONE) {
            // Логируем стек, чтобы узнать, кто вызывает сброс
            Throwable().printStackTrace()
        }
        _editorState.update { state ->
            state.copy(
                uiState = state.uiState.copy(mode = mode)
            )
        }
    }

    // ============ ТЕКСТОВЫЙ ДИАЛОГ ============

    fun showTextInputDialog(position: Offset) {
        _editorState.update { state ->
            state.copy(
                uiState = state.uiState.copy(
                    showTextInputDialog = true,
                    textInputPosition = position
                )
            )
        }
    }

    fun hideTextInputDialog() {
        _editorState.update { state ->
            state.copy(
                uiState = state.uiState.copy(
                    showTextInputDialog = false,
                    textInputPosition = null
                )
            )
        }
    }

    // ============ СОХРАНЕНИЕ ============

    suspend fun saveScheme(): Boolean {
        return try {
            val currentState = _editorState.value
            val currentShapes = shapes.value
            val currentDevices = devices.value

            val schemeData = SchemeData(
                width = currentState.canvasState.width,
                height = currentState.canvasState.height,
                backgroundColor = currentState.canvasState.backgroundColor.toHex(),
                backgroundImage = currentState.canvasState.backgroundImage,
                gridEnabled = currentState.canvasState.gridEnabled,
                gridSize = currentState.canvasState.gridSize,
                devices = currentDevices,
                shapes = currentShapes.map { it.toShapeData() }
            )

            val updatedScheme = currentState.scheme.setSchemeData(schemeData)

            val id = if (currentState.scheme.id == 0) {
                schemeRepository.insertScheme(updatedScheme).toInt()
            } else {
                schemeRepository.updateScheme(updatedScheme)
                currentState.scheme.id
            }

            // Сохраняем позиции устройств
            currentDevices.forEach { device ->
                deviceLocationRepository.saveLocation(
                    DeviceLocation(
                        deviceId = device.deviceId,
                        schemeId = id,
                        x = device.x,
                        y = device.y,
                        rotation = device.rotation
                    )
                )
            }

            _editorState.update {
                it.copy(
                    uiState = it.uiState.copy(isDirty = false)
                )
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ============ UNDO/REDO ============

    fun undo() {
        commandManager.undo()
        _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
    }

    fun redo() {
        commandManager.redo()
        _editorState.update { it.copy(uiState = it.uiState.copy(isDirty = true)) }
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ============

    private fun Color.toHex(): String {
        val alpha = (alpha * 255).toInt().coerceIn(0, 255)
        val red = (red * 255).toInt().coerceIn(0, 255)
        val green = (green * 255).toInt().coerceIn(0, 255)
        val blue = (blue * 255).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
    }

    private fun parseColor(colorHex: String): Color {
        return try {
            val cleanHex = colorHex.removePrefix("#")
            val longColor = when (cleanHex.length) {
                6 -> (0xFF000000.toInt() or cleanHex.toLong(16).toInt()).toLong()
                8 -> cleanHex.toLong(16)
                else -> 0xFFFFFFFFL
            }
            Color(longColor)
        } catch (e: Exception) {
            Color.White
        }
    }
}

// Добавляем EditorMode
enum class EditorMode {
    NONE,       // Ничего не делаем (основной режим)
    RECTANGLE,  // Добавление прямоугольника
    LINE,       // Добавление линии
    ELLIPSE,    // Добавление эллипса
    TEXT,       // Добавление текста
    RHOMBUS,    // Добавление ромба
    DEVICE      // Добавление прибора
}