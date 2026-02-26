package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.*
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.viewmodel.CanvasState
import com.kipia.management.mobile.viewmodel.EditorMode
import com.kipia.management.mobile.viewmodel.EditorState
import timber.log.Timber
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GestureLayer(
    editorState: EditorState,
    canvasState: CanvasState,
    shapes: List<ComposeShape>,
    devices: List<SchemeDevice>,
    onShapeClick: (String) -> Unit,
    onDeviceClick: (Int) -> Unit,
    onCanvasClick: (Offset) -> Unit,
    onShapeDrag: (String, Offset) -> Unit,
    onDeviceDrag: (Int, Offset) -> Unit,
    onTransform: (Float, Offset, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    key: Any? = null,
    debugMode: Boolean = false  // Включаем отладку
) {
    remember(key) { key }

    val currentMode = editorState.uiState.mode
    val isPanZoomMode = currentMode == EditorMode.PAN_ZOOM
    val dragTarget = remember { mutableStateOf<Pair<String, DragTargetType>?>(null) }

    // Отладочные состояния
    val lastTapPoint = remember { mutableStateOf<Offset?>(null) }
    val lastCalculatedTarget = remember { mutableStateOf<Pair<String, DragTargetType>?>(null) }

    // Стабилизируем масштаб
    val stableScale by remember(canvasState.scale) {
        derivedStateOf {
            (canvasState.scale / 0.05).roundToInt() * 0.05f
        }
    }

    // Сохраняем актуальные колбэки
    val currentOnShapeClick by rememberUpdatedState(onShapeClick)
    val currentOnDeviceClick by rememberUpdatedState(onDeviceClick)
    val currentOnCanvasClick by rememberUpdatedState(onCanvasClick)
    val currentOnShapeDrag by rememberUpdatedState(onShapeDrag)
    val currentOnDeviceDrag by rememberUpdatedState(onDeviceDrag)

    // Актуальные списки
    val currentShapes by rememberUpdatedState(shapes)
    val currentDevices by rememberUpdatedState(devices)

    // Состояние для transformable жестов
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        if (isPanZoomMode) {
            val newScale = (canvasState.scale * zoomChange).coerceIn(0.5f, 3.0f)
            val newOffset = canvasState.offset + panChange
            onTransform(newScale, newOffset, false)
        }
    }

    Box(
        modifier = modifier
            .run {
                if (isPanZoomMode) {
                    this.transformable(
                        state = transformState,
                        canPan = { true },
                        lockRotationOnZoomPan = true,
                        enabled = true
                    )
                } else {
                    this
                }
            }
            .pointerInput(currentMode, stableScale) {
                if (!isPanZoomMode) {
                    setupSelectionGestures(
                        scale = stableScale,
                        offset = canvasState.offset,  // Используем актуальный offset
                        shapesProvider = { currentShapes },
                        devicesProvider = { currentDevices },
                        dragTarget = dragTarget,
                        onShapeClick = { shapeId ->
                            currentOnShapeClick(shapeId)
                            if (debugMode) {
                                lastCalculatedTarget.value = Pair(shapeId, DragTargetType.SHAPE)
                            }
                        },
                        onDeviceClick = { deviceId ->
                            currentOnDeviceClick(deviceId)
                            if (debugMode) {
                                lastCalculatedTarget.value = Pair(deviceId.toString(), DragTargetType.DEVICE)
                            }
                        },
                        onCanvasClick = { canvasPoint ->
                            currentOnCanvasClick(canvasPoint)
                            if (debugMode) {
                                lastCalculatedTarget.value = null
                            }
                        },
                        onShapeDrag = currentOnShapeDrag,
                        onDeviceDrag = currentOnDeviceDrag,
                        debugMode = debugMode,
                        onTapPoint = { screenPoint ->
                            if (debugMode) {
                                lastTapPoint.value = screenPoint
                            }
                        }
                    )
                }
            }
    )

    // Отладочный слой
    if (debugMode) {
        DebugHitTestLayer(
            canvasState = canvasState,
            devices = devices,
            lastTapPoint = lastTapPoint.value,
            lastCalculatedTarget = lastCalculatedTarget.value,
            gestureScale = stableScale,  // передаем параметры из жестов
            gestureOffset = canvasState.offset,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private suspend fun PointerInputScope.setupSelectionGestures(
    scale: Float,
    offset: Offset,
    shapesProvider: () -> List<ComposeShape>,
    devicesProvider: () -> List<SchemeDevice>,
    dragTarget: MutableState<Pair<String, DragTargetType>?>,
    onShapeClick: (String) -> Unit,
    onDeviceClick: (Int) -> Unit,
    onCanvasClick: (Offset) -> Unit,
    onShapeDrag: (String, Offset) -> Unit,
    onDeviceDrag: (Int, Offset) -> Unit,
    debugMode: Boolean,
    onTapPoint: (Offset) -> Unit
) {
    val baseDeviceSize = 60f

    Timber.d("👆 setupSelectionGestures: scale=$scale, offset=$offset")

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        if (debugMode) {
            onTapPoint(down.position)
        }

        val currentShapes = shapesProvider()
        val currentDevices = devicesProvider()

        // Подробное логирование для отладки
        if (debugMode) {
            Timber.d("🎯 DEBUG ========== TAP DETECTED ==========")
            Timber.d("📱 Экранные координаты: (${down.position.x}, ${down.position.y})")
            Timber.d("📐 Параметры: scale=$scale, offset=(${offset.x}, ${offset.y})")

            currentDevices.forEach { device ->
                // Неправильная формула (текущая)
                val wrongScreenX = device.x + offset.x
                val wrongScreenY = device.y + offset.y

                // Правильная формула
                val correctScreenX = device.x * scale + offset.x
                val correctScreenY = device.y * scale + offset.y
                val screenSize = baseDeviceSize * scale

                val wrongRect = Rect(
                    left = wrongScreenX,
                    top = wrongScreenY,
                    right = wrongScreenX + screenSize,
                    bottom = wrongScreenY + screenSize
                )

                val correctRect = Rect(
                    left = correctScreenX,
                    top = correctScreenY,
                    right = correctScreenX + screenSize,
                    bottom = correctScreenY + screenSize
                )

                Timber.d("🔹 Device ${device.deviceId}:")
                Timber.d("   Мировые: (${device.x}, ${device.y})")
                Timber.d("   НЕПРАВИЛЬНЫЙ экран: ($wrongScreenX, $wrongScreenY) contains=${wrongRect.contains(down.position)}")
                Timber.d("   ПРАВИЛЬНЫЙ экран:    ($correctScreenX, $correctScreenY) contains=${correctRect.contains(down.position)}")
            }
        }

        // Используем правильную формулу для определения цели
        val target = findTargetCorrect(
            screenPoint = down.position,
            scale = scale,
            offset = offset,
            devices = currentDevices,
            shapes = currentShapes,
            baseDeviceSize = baseDeviceSize
        )

        Timber.d("🎯 Target found: $target")

        var dragStarted = false
        var lastDragTime = 0L

        do {
            val event = awaitPointerEvent(pass = PointerEventPass.Main)
            val changes = event.changes.filter { it.id == down.id }

            val currentTime = System.currentTimeMillis()

            when {
                changes.any { it.positionChanged() } && !dragStarted -> {
                    if (target != null) {
                        dragStarted = true
                        dragTarget.value = target
                    }
                }

                dragStarted -> {
                    val change = changes.firstOrNull { it.pressed } ?: continue
                    val dragAmount = change.positionChange()

                    if (dragAmount != Offset.Zero) {
                        if (currentTime - lastDragTime > 16) {
                            val canvasDelta = Offset(
                                x = dragAmount.x / scale,
                                y = dragAmount.y / scale
                            )

                            val currentTarget = dragTarget.value ?: continue

                            when (currentTarget.second) {
                                DragTargetType.SHAPE -> onShapeDrag(currentTarget.first, canvasDelta)
                                DragTargetType.DEVICE -> {
                                    currentTarget.first.toIntOrNull()?.let { deviceId ->
                                        onDeviceDrag(deviceId, canvasDelta)
                                    }
                                }
                            }
                            lastDragTime = currentTime
                        }
                        change.consume()
                    }
                }
            }

            changes.forEach { it.consume() }

        } while (event.changes.any { it.pressed })

        if (!dragStarted) {
            val canvasPoint = Offset(
                x = (down.position.x - offset.x) / scale,
                y = (down.position.y - offset.y) / scale
            )

            when {
                target == null -> {
                    Timber.d("📌 Пустой клик в canvas=$canvasPoint")
                    onCanvasClick(canvasPoint)
                }
                target.second == DragTargetType.SHAPE -> {
                    Timber.d("📌 Клик по фигуре: ${target.first}")
                    onShapeClick(target.first)
                }
                target.second == DragTargetType.DEVICE -> {
                    Timber.d("📌 Клик по прибору: ${target.first}")
                    target.first.toIntOrNull()?.let { deviceId ->
                        onDeviceClick(deviceId)
                    }
                }
            }
        }

        dragTarget.value = null
    }
}

private fun findTargetCorrect(
    screenPoint: Offset,
    scale: Float,
    offset: Offset,
    devices: List<SchemeDevice>,
    shapes: List<ComposeShape>,
    baseDeviceSize: Float
): Pair<String, DragTargetType>? {

    // 1. Проверяем приборы с ПРАВИЛЬНОЙ формулой
    for (device in devices.reversed()) {
        // (мировая_координата * scale) + offset
        val screenX = device.x * scale + offset.x
        val screenY = device.y * scale + offset.y
        val screenSize = baseDeviceSize * scale

        val deviceRect = Rect(
            left = screenX,
            top = screenY,
            right = screenX + screenSize,
            bottom = screenY + screenSize
        )

        if (deviceRect.contains(screenPoint)) {
            return Pair(device.deviceId.toString(), DragTargetType.DEVICE)
        }
    }

    // 2. Для фигур используем обратную трансформацию
    val canvasPoint = Offset(
        x = (screenPoint.x - offset.x) / scale,
        y = (screenPoint.y - offset.y) / scale
    )

    for (shape in shapes.reversed()) {
        if (shape.contains(canvasPoint)) {
            return Pair(shape.id, DragTargetType.SHAPE)
        }
    }

    return null
}

enum class DragTargetType { SHAPE, DEVICE }