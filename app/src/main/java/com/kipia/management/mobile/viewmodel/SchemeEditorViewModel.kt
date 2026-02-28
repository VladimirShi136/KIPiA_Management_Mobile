package com.kipia.management.mobile.viewmodel

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.commands.AddDeviceCommand
import com.kipia.management.mobile.commands.AddShapeCommand
import com.kipia.management.mobile.commands.DeleteShapeCommand
import com.kipia.management.mobile.commands.MoveDeviceCommand
import com.kipia.management.mobile.commands.MoveShapeCommand
import com.kipia.management.mobile.commands.RemoveDeviceCommand
import com.kipia.management.mobile.commands.UpdateShapeFillColorCommand
import com.kipia.management.mobile.commands.UpdateShapeStrokeColorCommand
import com.kipia.management.mobile.data.entities.Device
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
    val width: Int = 2000,
    val height: Int = 1200,
    val backgroundColor: Color = Color.White,
    val backgroundImage: String? = null,
    val gridEnabled: Boolean = true,
    val gridSize: Int = 50,
    val showGrid: Boolean = true,
    val dimOutsideBounds: Boolean = true,
    val viewportWidth: Int = 0,  // Добавим
    val viewportHeight: Int = 0   // Добавим
)

data class SelectionState(
    val selectedShapeId: String? = null,
    val selectedDeviceId: Int? = null
)

data class EditorUIState(
    val mode: EditorMode = EditorMode.NONE,
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
    savedStateHandle: SavedStateHandle,
    private val schemeRepository: SchemeRepository,
    private val deviceRepository: DeviceRepository,
    private val deviceLocationRepository: DeviceLocationRepository
) : ViewModel() {

    private val schemeId: Int? = savedStateHandle.get<String>("schemeId")?.toIntOrNull()

    private val shapeManager = ShapeManager()
    private val deviceManager = DeviceManager()
    private val commandManager = CommandManager()

    // ============ ОСНОВНЫЕ СОСТОЯНИЯ ============

    private val _editorState = MutableStateFlow(EditorState())
    val editorState = _editorState.asStateFlow()

    // Отдельные потоки для данных, которые могут часто меняться
    val shapes = shapeManager.shapes
    val allDevices = deviceManager.allDevices
    val devices = deviceManager.devices
    val canUndo = commandManager.canUndo
    val canRedo = commandManager.canRedo

    // ============ availableDevices ============

    val availableDevices: StateFlow<List<Device>> = combine(
        deviceManager.allDevices,
        deviceManager.devices.map { it -> it.map { it.deviceId }.toSet() }.distinctUntilChanged(),
        _editorState.map { it.scheme.name }.distinctUntilChanged()
    ) { allDevices, placedIds, schemeName ->
        Timber.d("🔄 availableDevices recompute:")
        Timber.d("   allDevices.size = ${allDevices.size}")
        Timber.d("   placedIds = $placedIds")
        Timber.d("   schemeName = '$schemeName'")

        val result = allDevices.filter { device ->
            val condition = device.id !in placedIds && device.location == schemeName
            if (condition) {
                Timber.d("   ✅ Device ${device.id} '${device.name}' подходит (location='${device.location}')")
            } else {
                Timber.d("   ❌ Device ${device.id} не подходит: inPlaced=${device.id in placedIds}, location='${device.location}' != '$schemeName'")
            }
            condition
        }

        Timber.d("   Результат: ${result.size} устройств доступно")
        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // ============ ЗАГРУЗКА ДАННЫХ ============

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                // 1. Сначала загружаем все устройства
                val allDevices = deviceRepository.getAllDevicesSync()
                deviceManager.setAllDevices(allDevices)

                // 2. Загружаем схему и обновляем editorState с правильным scheme.name
                schemeId?.let { id ->
                    val scheme = schemeRepository.getSchemeById(id)
                    scheme?.let {
                        val schemeData = it.getSchemeData()

                        // Сначала обновляем editorState с правильным именем схемы
                        _editorState.update { state ->
                            state.copy(
                                scheme = it,
                                canvasState = state.canvasState.copy(
                                    width = schemeData.width,
                                    height = schemeData.height,
                                    backgroundColor = parseColor(schemeData.backgroundColor),
                                    backgroundImage = schemeData.backgroundImage
                                )
                            )
                        }

                        // Даем время на обновление state
                        delay(50)

                        // 3. Теперь загружаем фигуры и устройства
                        schemeData.shapes.forEach { shapeData ->
                            shapeManager.addShape(shapeData.toComposeShape())
                        }

                        val locations = deviceLocationRepository.getLocationsForScheme(id)
                        locations.forEach { location ->
                            deviceManager.addDevice(
                                location.deviceId,
                                Offset(location.x, location.y)
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Ошибка логируется в репозитории
            }
        }
    }

    // ============ ТРАНСФОРМАЦИЯ КАНВАСА ============

    fun updateCanvasTransform(scale: Float, offset: Offset, resetOffset: Boolean = false) {
        val newScale = scale.coerceIn(0.5f, 3.0f)

        // Если сброс вида - центрируем холст
        val newOffset = if (resetOffset) {
            calculateCenteredOffset(newScale)
        } else {
            clampOffsetToBounds(offset, newScale)
        }

        val currentState = _editorState.value.canvasState
        if (currentState.scale == newScale && currentState.offset == newOffset) return

        _editorState.update { state ->
            state.copy(
                canvasState = state.canvasState.copy(
                    scale = newScale,
                    offset = newOffset
                )
            )
        }
    }

    // Функция для обновления viewport размеров
    fun updateViewportSize(width: Int, height: Int) {
        _editorState.update { state ->
            state.copy(
                canvasState = state.canvasState.copy(
                    viewportWidth = width,
                    viewportHeight = height
                )
            )
        }
    }

    // Ограничение offset границами холста
    private fun clampOffsetToBounds(offset: Offset, scale: Float): Offset {
        val viewportWidth = _editorState.value.canvasState.viewportWidth.toFloat()
        val viewportHeight = _editorState.value.canvasState.viewportHeight.toFloat()

        if (viewportWidth == 0f || viewportHeight == 0f) return offset

        val canvasWidth = _editorState.value.canvasState.width * scale
        val canvasHeight = _editorState.value.canvasState.height * scale

        Timber.d("🔒 clampOffsetToBounds:")
        Timber.d("   viewport=($viewportWidth, $viewportHeight)")
        Timber.d("   canvasSize=($canvasWidth, $canvasHeight)")
        Timber.d("   offset in=$offset")

        val panLimit = 200f

        // Правильные границы для offset.x
        val minX: Float
        val maxX: Float

        if (canvasWidth <= viewportWidth) {
            // Холст меньше экрана - центрируем и разрешаем небольшой выход
            minX = (viewportWidth - canvasWidth) / 2f - panLimit
            maxX = (viewportWidth - canvasWidth) / 2f + panLimit
        } else {
            // Холст больше экрана - разрешаем панорамирование с ограничениями
            minX = viewportWidth - canvasWidth - panLimit  // левая граница
            maxX = panLimit  // правая граница
            // ВАЖНО: minX должно быть МЕНЬШЕ maxX
            // При canvasWidth > viewportWidth: minX отрицательный и меньше чем -panLimit
            // maxX положительный, поэтому minX < maxX гарантированно
        }

        // Аналогично для Y
        val minY: Float
        val maxY: Float

        if (canvasHeight <= viewportHeight) {
            minY = (viewportHeight - canvasHeight) / 2f - panLimit
            maxY = (viewportHeight - canvasHeight) / 2f + panLimit
        } else {
            minY = viewportHeight - canvasHeight - panLimit
            maxY = panLimit
        }

        // Дополнительная проверка на корректность диапазона
        require(minX <= maxX) { "minX=$minX > maxX=$maxX" }
        require(minY <= maxY) { "minY=$minY > maxY=$maxY" }

        val clampedX = offset.x.coerceIn(minX, maxX)
        val clampedY = offset.y.coerceIn(minY, maxY)

        Timber.d("   range X: [$minX, $maxX]")
        Timber.d("   range Y: [$minY, $maxY]")
        Timber.d("   clamped=($clampedX, $clampedY)")

        return Offset(clampedX, clampedY)
    }

    // Центрирование холста
    private fun calculateCenteredOffset(scale: Float): Offset {
        val viewportWidth = _editorState.value.canvasState.viewportWidth.toFloat()
        val viewportHeight = _editorState.value.canvasState.viewportHeight.toFloat()

        if (viewportWidth == 0f || viewportHeight == 0f) return Offset.Zero

        val canvasWidth = _editorState.value.canvasState.width * scale
        val canvasHeight = _editorState.value.canvasState.height * scale

        Timber.d("🎯 calculateCenteredOffset: scale=$scale")
        Timber.d("   viewport=($viewportWidth, $viewportHeight)")
        Timber.d("   canvasSize=($canvasWidth, $canvasHeight)")

        // Если холст меньше экрана - центрируем
        // Если холст больше экрана - показываем левый верхний угол
        return Offset(
            x = ((viewportWidth - canvasWidth) / 2f).coerceAtLeast(0f),
            y = ((viewportHeight - canvasHeight) / 2f).coerceAtLeast(0f)
        )
    }

    // Функция для сброса вида (будет вызываться из UI)
    fun resetView() {
        val newScale = 1f
        val newOffset = calculateCenteredOffset(newScale)
        updateCanvasTransform(newScale, newOffset, resetOffset = true)
    }

    // ============ МЕТОДЫ ДЛЯ ОБНОВЛЕНИЯ ДАННЫХ ============

    fun markAsDirty() {
        if (!_editorState.value.uiState.isDirty) {
            _editorState.update { state ->
                state.copy(
                    uiState = state.uiState.copy(isDirty = true)
                )
            }
        }
    }

    // ============ ДЕЙСТВИЯ С ФИГУРАМИ ============

    fun addShape(shapeType: EditorMode, position: Offset) {
        val newShape = ComposeShapeFactory.create(shapeType).apply {
            if (this is ComposeText) {
                x = position.x
                y = position.y
            } else {
                x = position.x - width / 2
                y = position.y - height / 2
            }
        }

        commandManager.execute(
            AddShapeCommand(
                shapeManager = shapeManager,
                onStateChange = { markAsDirty() },
                shape = newShape
            )
        )
    }

    fun addTextShape(text: String, position: Offset) {
        val textShape = ComposeShapeFactory.createText().apply {
            this.text = text
            this.width = (text.length * 10f + 30f).coerceAtLeast(50f)
            this.height = 40f
            x = position.x
            y = position.y
        }

        commandManager.execute(
            AddShapeCommand(
                shapeManager = shapeManager,
                onStateChange = { markAsDirty() },
                shape = textShape
            )
        )
    }

    fun moveShape(shapeId: String, delta: Offset) {
        commandManager.execute(
            MoveShapeCommand(
                shapeManager = shapeManager,
                onStateChange = { markAsDirty() },
                shapeId = shapeId,
                delta = delta
            )
        )
    }

    fun deleteSelectedShape() {
        val shapeId = _editorState.value.selection.selectedShapeId ?: return
        val shape = shapes.value.find { it.id == shapeId } ?: return

        commandManager.execute(
            DeleteShapeCommand(
                shapeManager = shapeManager,
                editorState = _editorState,
                shape = shape
            )
        )
    }

    fun updateShapeFillColor(shapeId: String, color: Color) {
        val shape = shapes.value.find { it.id == shapeId } ?: return

        commandManager.execute(
            UpdateShapeFillColorCommand(
                shapeManager = shapeManager,
                editorState = _editorState,
                shapeId = shapeId,
                newColor = color,
                oldColor = shape.fillColor
            )
        )
    }

    fun updateShapeStrokeColor(shapeId: String, color: Color) {
        val shape = shapes.value.find { it.id == shapeId } ?: return

        commandManager.execute(
            UpdateShapeStrokeColorCommand(
                shapeManager = shapeManager,
                editorState = _editorState,
                shapeId = shapeId,
                newColor = color,
                oldColor = shape.strokeColor
            )
        )
    }

    fun duplicateShape(shapeId: String) {
        val originalShape = shapes.value.find { it.id == shapeId } ?: return
        val duplicate = ComposeShapeFactory.duplicateShape(originalShape)
        duplicate.x = originalShape.x + 20
        duplicate.y = originalShape.y + 20

        commandManager.execute(object : Command {
            override fun execute() {
                shapeManager.addShape(duplicate)
                markAsDirty()
            }

            override fun undo() {
                shapeManager.removeShape(duplicate.id)
                markAsDirty()
            }
        })
    }

    // ============ ДЕЙСТВИЯ С УСТРОЙСТВАМИ ============

    fun addDevice(deviceId: Int, position: Offset) {
        Timber.d("📱 Добавление устройства ID=$deviceId на позицию $position")

        val device = availableDevices.value.find { it.id == deviceId }
        if (device == null) {
            Timber.e("❌ Устройство с ID=$deviceId не найдено в availableDevices")
            Timber.e("   availableDevices: ${availableDevices.value.map { it.id }}")
            return
        }

        Timber.d("✅ Устройство найдено: ${device.name}")
        Timber.d("   Текущее кол-во устройств на схеме: ${deviceManager.devices.value.size}")

        commandManager.execute(
            AddDeviceCommand(
                deviceManager = deviceManager,
                onStateChange = { markAsDirty() },
                deviceId = deviceId,
                position = position
            )
        )

        // Проверяем после добавления
        viewModelScope.launch {
            delay(100) // Даем время на обновление
            Timber.d("📊 После добавления: устройств на схеме = ${deviceManager.devices.value.size}")
            deviceManager.devices.value.forEach {
                Timber.d("   - ID=${it.deviceId} на (${it.x}, ${it.y})")
            }
        }
    }

    fun moveDevice(deviceId: Int, delta: Offset) {
        // Получаем текущую позицию устройства
        val currentDevice = devices.value.find { it.deviceId == deviceId } ?: return

        // Вычисляем новую позицию
        val newX = currentDevice.x + delta.x
        val newY = currentDevice.y + delta.y

        // Ограничиваем границами холста с учетом размера устройства
        val deviceSize = 60f
        val maxX = _editorState.value.canvasState.width - deviceSize
        val maxY = _editorState.value.canvasState.height - deviceSize

        val clampedX = newX.coerceIn(0f, maxX)
        val clampedY = newY.coerceIn(0f, maxY)

        // Если позиция изменилась после ограничения, корректируем delta
        val clampedDelta = Offset(clampedX - currentDevice.x, clampedY - currentDevice.y)

        Timber.d("📦 moveDevice: ID=$deviceId")
        Timber.d("   from (${currentDevice.x}, ${currentDevice.y})")
        Timber.d("   delta in=$delta")
        Timber.d("   new raw=($newX, $newY)")
        Timber.d("   bounds: x=[0, $maxX], y=[0, $maxY]")
        Timber.d("   clamped=($clampedX, $clampedY)")
        Timber.d("   delta out=$clampedDelta")

        commandManager.execute(
            MoveDeviceCommand(
                deviceManager = deviceManager,
                onStateChange = { markAsDirty() },
                deviceId = deviceId,
                delta = clampedDelta  // Используем скорректированную delta
            )
        )
    }

    fun removeDevice(deviceId: Int) {
        commandManager.execute(
            RemoveDeviceCommand(
                deviceManager = deviceManager,
                onStateChange = { markAsDirty() },
                deviceId = deviceId
            )
        )
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
                    showShapeProperties = shapeId != null,
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
                    showDeviceProperties = deviceId != null
                )
            )
        }
    }

    fun clearSelection() {
        Timber.d("🧹 clearSelection() called")
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
        _editorState.update { state ->
            state.copy(
                uiState = state.uiState.copy(mode = mode)
            )
        }
    }

    // ============ ТЕКСТОВЫЙ ДИАЛОГ ============

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
        } catch (_: Exception) {
            false
        }
    }

    // ============ UNDO/REDO ============

    fun undo() {
        commandManager.undo()
        markAsDirty()
    }

    fun redo() {
        commandManager.redo()
        markAsDirty()
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
        } catch (_: Exception) {
            Color.White
        }
    }
}

enum class EditorMode {
    NONE, SELECT, RECTANGLE, LINE, ELLIPSE, TEXT, RHOMBUS, DEVICE, PAN_ZOOM
}