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
import com.kipia.management.mobile.managers.*
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.SchemeRepository
import com.kipia.management.mobile.ui.components.scheme.shapes.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SchemeEditorViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val schemeRepository: SchemeRepository,
    private val deviceRepository: DeviceRepository
) : ViewModel() {

    private val schemeId: Int? = run {
        val schemeIdString = savedStateHandle.get<String>("schemeId")
        schemeIdString?.toIntOrNull()
    }

    private val commandManager = CommandManager()
    private val shapeManager = ShapeManager()

    private var isUpdatingFromManager = false
    private var isUpdatingFromSchemeData = false

    // Debounce для обновления при перетаскивании
    private var updateJob: Job? = null

    // Флаг для предотвращения вызова selectShape при перетаскивании
    private var isDraggingShape = false

    // UI State
    private val _uiState = MutableStateFlow(SchemeEditorUiState())
    val uiState: StateFlow<SchemeEditorUiState> = _uiState.asStateFlow()

    // Устройства
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices: StateFlow<List<Device>> = _devices.asStateFlow()

    private val _devicesForScheme = MutableStateFlow<List<Device>>(emptyList())
    val devicesForScheme: StateFlow<List<Device>> = _devicesForScheme.asStateFlow()

    private val _errorState = MutableStateFlow<ErrorState?>(null)
    val errorState: StateFlow<ErrorState?> = _errorState.asStateFlow()

    private val _editorMode = MutableStateFlow(EditorMode.SELECT)
    val editorMode: StateFlow<EditorMode> = _editorMode.asStateFlow()

    // Флаг для отображения панели свойств
    private val _showPropertiesPanel = MutableStateFlow(false)
    val showPropertiesPanel: StateFlow<Boolean> = _showPropertiesPanel.asStateFlow()

    init {
        loadDevices()
        loadScheme()
    }

    // ============ ЗАГРУЗКА ДАННЫХ ============
    private fun loadScheme() {
        viewModelScope.launch {
            try {
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
                        loadShapesFromSchemeData(it.getSchemeData())
                        updateDevicesForScheme()
                    }
                } ?: run {
                    _uiState.update { state ->
                        state.copy(
                            scheme = Scheme.createEmpty(),
                            schemeData = SchemeData(),
                            isNewScheme = true
                        )
                    }
                    updateDevicesForScheme()
                }
            } catch (e: Exception) {
                _errorState.value = ErrorState("Ошибка загрузки схемы: ${e.message}")
            }
        }
    }

    private fun loadDevices() {
        viewModelScope.launch {
            try {
                val allDevices = deviceRepository.getAllDevicesSync()
                _devices.value = allDevices
                updateDevicesForScheme()

                if (!_uiState.value.isNewScheme) {
                    deviceRepository.getAllDevices()
                        .onEach { devicesList ->
                            _devices.value = devicesList
                            updateDevicesForScheme()
                        }
                        .launchIn(viewModelScope)
                }
            } catch (e: Exception) {
                _errorState.value = ErrorState("Ошибка загрузки устройств: ${e.message}")
            }
        }
    }

    private fun updateDevicesForScheme() {
        val schemeLocation = _uiState.value.scheme.name
        val filtered = if (schemeLocation.isNotBlank()) {
            _devices.value.filter { it.location == schemeLocation }
        } else {
            _devices.value
        }
        _devicesForScheme.value = filtered
    }

    // ============ СИНХРОНИЗАЦИЯ ФИГУР ============
    private fun updateShapesInSchemeData() {
        if (isUpdatingFromSchemeData) return
        isUpdatingFromManager = true
        try {
            val shapesData = shapeManager.getAllShapes().map { it.toShapeData() }
            _uiState.update { state ->
                state.copy(
                    schemeData = state.schemeData.copy(shapes = shapesData),
                    isDirty = true
                )
            }
        } catch (e: Exception) {
            _errorState.value = ErrorState("Ошибка обновления фигур: ${e.message}")
        } finally {
            isUpdatingFromManager = false
        }
    }

    private fun loadShapesFromSchemeData(schemeData: SchemeData) {
        if (isUpdatingFromManager) return
        isUpdatingFromSchemeData = true
        try {
            shapeManager.clearShapes()
            schemeData.shapes.forEach { shapeData ->
                shapeManager.addShape(shapeData.toComposeShape())
            }
        } catch (e: Exception) {
            _errorState.value = ErrorState("Ошибка загрузки фигур: ${e.message}")
        } finally {
            isUpdatingFromSchemeData = false
        }
    }

    fun getShapes(): List<ComposeShape> = shapeManager.getAllShapes()

    // ============ УПРАВЛЕНИЕ ФИГУРАМИ ============
    fun addRectangleAt(position: Offset) {
        viewModelScope.launch {
            println("ViewModel: addRectangleAt at $position")
            val rectangle = ComposeShapeFactory.createRectangle()
            rectangle.x = position.x
            rectangle.y = position.y
            rectangle.width = 100f
            rectangle.height = 60f
            rectangle.zIndex = shapeManager.getAllShapes().size

            addShapeWithUndo(rectangle)

            // АВТОМАТИЧЕСКИ ПЕРЕКЛЮЧАЕМСЯ В SELECT после создания
            setEditorMode(EditorMode.SELECT)
        }
    }

    fun addLineAt(position: Offset) {
        viewModelScope.launch {
            val line = ComposeShapeFactory.createLine()
            line.x = position.x
            line.y = position.y
            line.startX = 0f
            line.startY = 0f
            line.endX = 100f
            line.endY = 0f
            line.width = 110f
            line.height = 20f
            line.zIndex = shapeManager.getAllShapes().size

            addShapeWithUndo(line)

            setEditorMode(EditorMode.SELECT)
        }
    }

    fun addEllipseAt(position: Offset) {
        viewModelScope.launch {
            val ellipse = ComposeShapeFactory.createEllipse()
            ellipse.x = position.x
            ellipse.y = position.y
            ellipse.width = 80f
            ellipse.height = 50f
            ellipse.zIndex = shapeManager.getAllShapes().size

            addShapeWithUndo(ellipse)

            setEditorMode(EditorMode.SELECT)
        }
    }

    fun addRhombusAt(position: Offset) {
        viewModelScope.launch {
            val rhombus = ComposeShapeFactory.createRhombus()
            rhombus.x = position.x
            rhombus.y = position.y
            rhombus.width = 80f
            rhombus.height = 60f
            rhombus.zIndex = shapeManager.getAllShapes().size

            addShapeWithUndo(rhombus)

            setEditorMode(EditorMode.SELECT)
        }
    }

    fun addTextAt(position: Offset, text: String) {
        viewModelScope.launch {
            val textShape = ComposeShapeFactory.createText()
            textShape.x = position.x
            textShape.y = position.y
            textShape.text = text
            textShape.width = (text.length * 10f + 30f).coerceAtLeast(50f)
            textShape.height = 40f
            textShape.zIndex = shapeManager.getAllShapes().size

            addShapeWithUndo(textShape)

            setEditorMode(EditorMode.SELECT)
        }
    }

    private fun addShapeWithUndo(shape: ComposeShape) {
        val command = AddShapeCommand(
            shapeManager = shapeManager,
            shape = shape,
            onShapeAdded = {
                updateShapesInSchemeData()
                // Убираем selectShape отсюда!
            }
        )
        commandManager.execute(command)
    }

    fun deleteSelectedShape() {
        viewModelScope.launch {
            _uiState.value.selectedShape?.let { shape ->
                val command = DeleteShapeCommand(
                    shapeManager = shapeManager,
                    shape = shape,
                    onShapeDeleted = {
                        updateShapesInSchemeData()
                        clearSelection()
                    }
                )
                commandManager.execute(command)
            }
        }
    }

    fun duplicateSelectedShape() {
        viewModelScope.launch {
            _uiState.value.selectedShape?.let { shape ->
                val duplicate = ComposeShapeFactory.duplicateShape(shape)
                duplicate.zIndex = shapeManager.getAllShapes().size

                val command = AddShapeCommand(
                    shapeManager = shapeManager,
                    shape = duplicate,
                    onShapeAdded = {
                        updateShapesInSchemeData()
                        selectShape(duplicate) // При дублировании выделяем новую фигуру
                    }
                )
                commandManager.execute(command)
            }
        }
    }

    fun moveShape(shapeId: String, delta: Offset) {
        val shape = shapeManager.getAllShapes().find { it.id == shapeId }
        shape?.let {
            val oldX = it.x
            val oldY = it.y
            val newX = oldX + delta.x
            val newY = oldY + delta.y
            val schemeData = _uiState.value.schemeData
            val boundedX = newX.coerceIn(0f, schemeData.width.toFloat() - it.width)
            val boundedY = newY.coerceIn(0f, schemeData.height.toFloat() - it.height)

            // ✅ НЕМЕДЛЕННО обновляем позицию
            it.x = boundedX
            it.y = boundedY

            // ✅ ВАЖНО: ОБНОВИТЬ UI STATE — чтобы SchemeCanvas получил актуальные данные
            if (_uiState.value.selectedShape?.id == shapeId) {
                _uiState.update { state ->
                    state.copy(selectedShape = it)
                }
            }

            // ✅ ✅ ✅ НОВЫЙ КЛЮЧЕВОЙ ШАГ: СРАЗУ ОБНОВИТЬ schemeData.shapes!
            updateShapesInSchemeData() // ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ← ←

            updateJob?.cancel()
            updateJob = viewModelScope.launch {
                delay(300)
                val command = MoveShapeCommand(
                    shapeManager = shapeManager,
                    shape = it,
                    oldX = oldX,
                    oldY = oldY,
                    newX = boundedX,
                    newY = boundedY,
                    onShapeMoved = {}
                )
                commandManager.execute(command)
            }
        }
    }

    fun resizeShape(shapeId: String, newWidth: Float, newHeight: Float) {
        val shape = shapeManager.getAllShapes().find { it.id == shapeId }
        shape?.let {
            val oldWidth = it.width
            val oldHeight = it.height
            val boundedWidth = when (it) {
                is ComposeLine -> newWidth.coerceIn(20f, 500f)
                else -> newWidth.coerceIn(20f, 500f)
            }
            val boundedHeight = when (it) {
                is ComposeLine -> newHeight.coerceIn(20f, 500f)
                else -> newHeight.coerceIn(20f, 500f)
            }

            // НЕМЕДЛЕННО обновляем размер
            when (it) {
                is ComposeLine -> {
                    it.endX = boundedWidth
                    it.endY = boundedHeight
                    it.width = maxOf(it.startX, boundedWidth) + 10f
                    it.height = maxOf(it.startY, boundedHeight) + 10f
                }
                else -> {
                    it.width = boundedWidth
                    it.height = boundedHeight
                }
            }

            // ✅ ОБНОВИТЬ selectedShape
            if (_uiState.value.selectedShape?.id == shapeId) {
                _uiState.update { state ->
                    state.copy(selectedShape = it)
                }
            }

            updateShapesInSchemeData()

            updateJob?.cancel()
            updateJob = viewModelScope.launch {
                delay(300)
                val command = ResizeShapeCommand(
                    shapeManager = shapeManager,
                    shape = it,
                    oldWidth = oldWidth,
                    oldHeight = oldHeight,
                    newWidth = boundedWidth,
                    newHeight = boundedHeight,
                    onShapeResized = {}
                )
                commandManager.execute(command)
            }
        }
    }

    fun updateShapeProperties(updatedShape: ComposeShape) {
        viewModelScope.launch {
            val oldShape = shapeManager.getAllShapes().find { it.id == updatedShape.id }
            oldShape?.let {
                val command = UpdateShapePropertiesCommand(
                    shapeManager = shapeManager,
                    oldShape = oldShape,
                    newShape = updatedShape,
                    onShapeUpdated = {
                        updateShapesInSchemeData()
                        _uiState.update { state ->
                            state.copy(selectedShape = updatedShape)
                        }
                    }
                )
                commandManager.execute(command)
            }
        }
    }

    // ============ ВЫДЕЛЕНИЕ ============

    /**
     * Выделяет фигуру и показывает панель свойств
     */
    fun selectShape(shape: ComposeShape?) {
        if (isDraggingShape) {
            println("ViewModel: Skipping selectShape during drag (isDraggingShape = true)")
            return
        }

        val oldSelectedId = _uiState.value.selectedShape?.id
        val newSelectedId = shape?.id

        println("ViewModel: selectShape called | old: $oldSelectedId | new: $newSelectedId")

        // Обновляем выделение в ShapeManager
        shapeManager.selectShape(shape)

        // Обновляем UI State — всегда с актуальной ссылкой
        _uiState.update { state ->
            state.copy(
                selectedShape = shape,
                selectedDeviceId = null,
                showDevicePropertiesPanel = false
            )
        }

        // ✅ ВЫКЛЮЧАЕМ ПАНЕЛЬ СВОЙСТВ — по ТЗ!
        _showPropertiesPanel.value = false

        // Лог: проверяем, что фигура действительно выделена в ShapeManager
        val managerSelected = shapeManager.getSelectedShape()
        println("ViewModel: ShapeManager selected = ${managerSelected?.id} | matches ViewModel? ${managerSelected?.id == newSelectedId}")
    }

    /**
     * Устанавливает флаг перетаскивания
     */
    @Suppress("unused")
    fun setDraggingShape(isDragging: Boolean) {
        isDraggingShape = isDragging
    }

    /**
     * Переключает панель свойств для выделенной фигуры
     */
    fun togglePropertiesPanel() {
        if (_uiState.value.selectedShape != null) {
            _showPropertiesPanel.value = !_showPropertiesPanel.value
            println("ViewModel: Toggle properties panel to ${_showPropertiesPanel.value}")
        }
    }

    fun findShapeAt(position: Offset): ComposeShape? {
        return shapeManager.selectShapeAt(position)
    }

    fun bringShapeToFront() {
        viewModelScope.launch {
            _uiState.value.selectedShape?.let { shape ->
                val command = BringToFrontCommand(
                    shapeManager = shapeManager,
                    shape = shape,
                    onOrderChanged = {
                        updateShapesInSchemeData()
                    }
                )
                commandManager.execute(command)
            }
        }
    }

    fun sendShapeToBack() {
        viewModelScope.launch {
            _uiState.value.selectedShape?.let { shape ->
                val command = SendToBackCommand(
                    shapeManager = shapeManager,
                    shape = shape,
                    onOrderChanged = {
                        updateShapesInSchemeData()
                    }
                )
                commandManager.execute(command)
            }
        }
    }

    // ============ УПРАВЛЕНИЕ УСТРОЙСТВАМИ ============
    fun addDevice(deviceId: Int, position: Offset) {
        viewModelScope.launch {
            try {
                val device = _devices.value.find { it.id == deviceId } ?: return@launch

                val schemeLocation = _uiState.value.scheme.name
                if (schemeLocation.isNotBlank() && device.location != schemeLocation) {
                    _errorState.value = ErrorState("Устройство не соответствует локации схемы")
                    return@launch
                }

                val schemeData = _uiState.value.schemeData
                val boundedX = position.x.coerceIn(0f, schemeData.width.toFloat() - 80f)
                val boundedY = position.y.coerceIn(0f, schemeData.height.toFloat() - 80f)

                _uiState.update { state ->
                    val existingDevice = state.schemeData.devices.find { it.deviceId == deviceId }
                    if (existingDevice != null) {
                        val updatedDevices = state.schemeData.devices.map {
                            if (it.deviceId == deviceId) {
                                it.copy(x = boundedX, y = boundedY)
                            } else it
                        }
                        state.copy(
                            schemeData = state.schemeData.copy(devices = updatedDevices),
                            isDirty = true
                        )
                    } else {
                        val newDevice = SchemeDevice(
                            deviceId = deviceId,
                            x = boundedX,
                            y = boundedY,
                            zIndex = state.schemeData.devices.size
                        )
                        val updatedDevices = state.schemeData.devices + newDevice
                        state.copy(
                            schemeData = state.schemeData.copy(devices = updatedDevices),
                            isDirty = true
                        )
                    }
                }
            } catch (e: Exception) {
                _errorState.value = ErrorState("Ошибка добавления устройства: ${e.message}")
            }
        }
    }

    fun updateDevicePosition(deviceId: Int, position: Offset) {
        val schemeData = _uiState.value.schemeData
        val boundedX = position.x.coerceIn(0f, schemeData.width.toFloat() - 80f)
        val boundedY = position.y.coerceIn(0f, schemeData.height.toFloat() - 80f)

        _uiState.update { state ->
            val updatedDevices = state.schemeData.devices.map {
                if (it.deviceId == deviceId) {
                    it.copy(x = boundedX, y = boundedY)
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
                    it.copy(rotation = rotation.coerceIn(0f, 360f))
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
                selectedDeviceId = null,
                showDevicePropertiesPanel = false,
                isDirty = true
            )
        }
    }

    fun setSelectedDevice(deviceId: Int?) {
        shapeManager.clearSelection()
        _uiState.update { state ->
            state.copy(
                selectedDeviceId = deviceId,
                selectedShape = null,
                showDevicePropertiesPanel = deviceId != null,
                showShapePropertiesPanel = false
            )
        }
        if (deviceId != null) {
            _editorMode.value = EditorMode.SELECT
        }
    }

    // ============ УПРАВЛЕНИЕ СХЕМОЙ ============
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
        loadShapesFromSchemeData(schemeData)
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
                schemeData = state.schemeData.copy(gridSize = size.coerceIn(10, 200)),
                isDirty = true
            )
        }
    }

    fun setCanvasSize(width: Int, height: Int) {
        _uiState.update { state ->
            state.copy(
                schemeData = state.schemeData.copy(
                    width = width.coerceIn(500, 5000),
                    height = height.coerceIn(500, 5000)
                ),
                isDirty = true
            )
        }
    }

    // ============ РЕДАКТОР И РЕЖИМЫ ============
    fun setEditorMode(mode: EditorMode) {
        println("ViewModel: setEditorMode $mode")

        // Если переключаемся СО ВСЕХ режимов НА SELECT — не сбрасываем выделение
        // Но если переключаемся С SELECT НА ДРУГОЙ — сбрасываем
        if (_editorMode.value == EditorMode.SELECT && mode != EditorMode.SELECT) {
            clearSelection()
        } else if (_editorMode.value != EditorMode.SELECT && mode != EditorMode.SELECT) {
            clearSelection() // Сбрасываем при смене инструментов (RECTANGLE → LINE)
        }

        if (mode == EditorMode.SELECT && _editorMode.value == EditorMode.SELECT) {
            _editorMode.value = EditorMode.NONE
            clearSelection()
            println("ViewModel: SELECT mode disabled")
        } else {
            _editorMode.value = mode
            if (mode != EditorMode.SELECT) {
                clearSelection()
            }
        }
    }

    fun handleCanvasClick(position: Offset) {
        when (_editorMode.value) {
            EditorMode.RECTANGLE -> addRectangleAt(position)
            EditorMode.LINE -> addLineAt(position)
            EditorMode.ELLIPSE -> addEllipseAt(position)
            EditorMode.RHOMBUS -> addRhombusAt(position)
            EditorMode.TEXT -> showTextInputDialog(position)
            EditorMode.SELECT -> {
                val shapeAtPoint = findShapeAt(position)
                if (shapeAtPoint != null) {
                    selectShape(shapeAtPoint)
                } else {
                    clearSelection()
                }
            }
            else -> {}
        }
    }

    fun clearSelection() {
        println("ViewModel: clearSelection called")
        shapeManager.clearSelection()
        _uiState.update { state ->
            state.copy(
                selectedShape = null,
                selectedDeviceId = null,
                showShapePropertiesPanel = false,
                showDevicePropertiesPanel = false
            )
        }
        _showPropertiesPanel.value = false
    }

    fun showTextInputDialog(position: Offset) {
        _uiState.update { state ->
            state.copy(
                textInputPosition = position,
                showTextInputDialog = true
            )
        }
    }

    fun hideTextInputDialog() {
        _uiState.update { state ->
            state.copy(
                showTextInputDialog = false,
                textInputPosition = null
            )
        }
    }

    // ============ UNDO/REDO ============
    fun undo() {
        commandManager.undo()
        updateShapesInSchemeData()
        refreshSelectedShape()
    }

    fun redo() {
        commandManager.redo()
        updateShapesInSchemeData()
        refreshSelectedShape()
    }

    fun canUndo(): Boolean = commandManager.canUndo()
    fun canRedo(): Boolean = commandManager.canRedo()

    private fun refreshSelectedShape() {
        val selectedShapeId = _uiState.value.selectedShape?.id
        selectedShapeId?.let { id ->
            val refreshedShape = shapeManager.getAllShapes().find { it.id == id }
            _uiState.update { state ->
                state.copy(selectedShape = refreshedShape)
            }
        }
    }

    // ============ СОХРАНЕНИЕ ============
    suspend fun saveScheme(): Boolean {
        return try {
            updateShapesInSchemeData()
            val schemeWithData = _uiState.value.scheme.setSchemeData(_uiState.value.schemeData)
            if (_uiState.value.isNewScheme) {
                schemeRepository.insertScheme(schemeWithData)
                _uiState.update { it.copy(isNewScheme = false, isDirty = false) }
            } else {
                schemeRepository.updateScheme(schemeWithData)
                _uiState.update { it.copy(isDirty = false) }
            }
            _errorState.value = null
            true
        } catch (e: Exception) {
            _errorState.value = ErrorState("Ошибка сохранения: ${e.message}")
            false
        }
    }

    // ============ ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ============
    private fun Color.toHex(): String {
        val alpha = (alpha * 255).toInt()
        val red = (red * 255).toInt()
        val green = (green * 255).toInt()
        val blue = (blue * 255).toInt()
        return String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
    }

    fun clearError() {
        _errorState.value = null
    }
}

// Добавляем EditorMode.NONE
enum class EditorMode {
    NONE,       // Ничего не делаем
    SELECT,     // Режим выбора
    RECTANGLE,  // Добавление прямоугольника
    LINE,       // Добавление линии
    ELLIPSE,    // Добавление эллипса
    TEXT,       // Добавление текста
    RHOMBUS,    // Добавление ромба
    DEVICE      // Добавление прибора
}

data class SchemeEditorUiState(
    val scheme: Scheme = Scheme.createEmpty(),
    val schemeData: SchemeData = SchemeData(),
    val isNewScheme: Boolean = true,
    val isDirty: Boolean = false,

    // Выделение
    val selectedDeviceId: Int? = null,
    val selectedShape: ComposeShape? = null,

    // Панели
    val showShapePropertiesPanel: Boolean = false,
    val showDevicePropertiesPanel: Boolean = false,
    val showBackgroundPicker: Boolean = false,
    val showGridSettings: Boolean = false,
    val showCanvasSettings: Boolean = false,
    val showTextInputDialog: Boolean = false,

    // Позиция для диалогов
    val textInputPosition: Offset? = null
)

data class ErrorState(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)