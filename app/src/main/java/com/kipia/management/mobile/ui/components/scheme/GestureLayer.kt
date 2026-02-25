package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.*
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.screenToCanvas
import com.kipia.management.mobile.viewmodel.CanvasState
import com.kipia.management.mobile.viewmodel.EditorMode
import com.kipia.management.mobile.viewmodel.EditorState
import timber.log.Timber

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
    key: Any? = null
) {
    remember(key) { key }

    val currentMode = editorState.uiState.mode
    val isPanZoomMode = currentMode == EditorMode.PAN_ZOOM
    val dragTarget = remember { mutableStateOf<Pair<String, DragTargetType>?>(null) }

    // Состояние для transformable жестов - живёт вне композиции!
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        // Эта лямбда вызывается в ответ на жесты, но НЕ вызывает рекомпозицию сама по себе
        if (isPanZoomMode) {
            val newScale = (canvasState.scale * zoomChange).coerceIn(0.5f, 3.0f)
            val newOffset = canvasState.offset + panChange
            Timber.d("🔄 Transform: zoom=$newScale, pan=$newOffset")
            onTransform(newScale, newOffset, false)
        }
    }

    Box(
        modifier = modifier
            .run {
                if (isPanZoomMode) {
                    // РЕЖИМ PAN/ZOOM - используем transformable для непрерывных жестов
                    this.transformable(
                        state = transformState,
                        canPan = { true },  // Всегда разрешаем панорамирование
                        lockRotationOnZoomPan = true,  // Блокируем вращение при зуме
                        enabled = true
                    )
                } else {
                    // РЕЖИМ SELECT - обрабатываем через pointerInput
                    this
                }
            }
            .pointerInput(currentMode, canvasState.scale, canvasState.offset, shapes, devices) {
                Timber.d("🔍 pointerInput запущен, mode=$currentMode, isPanZoomMode=$isPanZoomMode")
                if (!isPanZoomMode) {
                    // Только в режиме SELECT обрабатываем нажатия через pointerInput
                    setupSelectionGestures(
                        canvasState = canvasState,
                        shapes = shapes,
                        devices = devices,
                        dragTarget = dragTarget,
                        onShapeClick = onShapeClick,
                        onDeviceClick = onDeviceClick,
                        onCanvasClick = onCanvasClick,
                        onShapeDrag = onShapeDrag,
                        onDeviceDrag = onDeviceDrag
                    )
                } else {
                    Timber.d("🚫 Пропускаем selection gestures - режим PAN/ZOOM")
                }
            }
    )
}

private suspend fun PointerInputScope.setupSelectionGestures(
    canvasState: CanvasState,
    shapes: List<ComposeShape>,
    devices: List<SchemeDevice>,
    dragTarget: MutableState<Pair<String, DragTargetType>?>,
    onShapeClick: (String) -> Unit,
    onDeviceClick: (Int) -> Unit,
    onCanvasClick: (Offset) -> Unit,
    onShapeDrag: (String, Offset) -> Unit,
    onDeviceDrag: (Int, Offset) -> Unit
) {
    val deviceSize = 60f
    var isDragging by mutableStateOf(false)

    Timber.d("👆 setupSelectionGestures started")

    // Единый обработчик pointerInput
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val canvasPoint = screenToCanvas(down.position, canvasState)
        val target = findTarget(canvasPoint, shapes, devices, deviceSize)

        Timber.d("🎯 Down at ${down.position}, target=$target")

        // Отслеживаем движение для определения drag
        var dragStarted = false

        do {
            val event = awaitPointerEvent()
            val changes = event.changes.filter { it.id == down.id }

            when {
                // Если палец двигается - это drag
                changes.any { it.positionChanged() } && !dragStarted -> {
                    if (target != null) {  // Проверяем один раз
                        Timber.d("🖱️ Drag started for $target")
                        dragStarted = true
                        isDragging = true
                        dragTarget.value = target
                    }
                }

                // Обработка drag
                dragStarted -> {
                    val change = changes.firstOrNull { it.pressed } ?: continue
                    val dragAmount = change.positionChange()

                    if (dragAmount != Offset.Zero) {
                        val canvasDelta = Offset(
                            x = dragAmount.x / canvasState.scale,
                            y = dragAmount.y / canvasState.scale
                        )

                        // Здесь target гарантированно не null, потому что dragStarted=true
                        // мы уже проверили target != null выше
                        val currentTarget = dragTarget.value ?: continue

                        Timber.d("🔄 Dragging: $canvasDelta")

                        when (currentTarget.second) {
                            DragTargetType.SHAPE -> onShapeDrag(currentTarget.first, canvasDelta)
                            DragTargetType.DEVICE -> {
                                currentTarget.first.toIntOrNull()?.let { deviceId ->
                                    onDeviceDrag(deviceId, canvasDelta)
                                }
                            }
                        }
                        change.consume()
                    }
                }
            }

            changes.forEach { it.consume() }

        } while (event.changes.any { it.pressed })

        // Если не было drag - это tap
        if (!dragStarted) {
            Timber.d("👆 Tap detected at ${down.position}")
            val tapCanvasPoint = screenToCanvas(down.position, canvasState)
            val tapTarget = findTarget(tapCanvasPoint, shapes, devices, deviceSize)

            when {
                tapTarget == null -> {
                    Timber.d("📌 Пустой клик, вызываем onCanvasClick")
                    onCanvasClick(tapCanvasPoint)
                }
                tapTarget.second == DragTargetType.SHAPE -> {
                    Timber.d("📌 Клик по фигуре: ${tapTarget.first}")
                    onShapeClick(tapTarget.first)
                }
                tapTarget.second == DragTargetType.DEVICE -> {
                    Timber.d("📌 Клик по прибору: ${tapTarget.first}")
                    tapTarget.first.toIntOrNull()?.let { deviceId ->
                        onDeviceClick(deviceId)
                    }
                }
            }
        }

        isDragging = false
        dragTarget.value = null
    }
}

private fun findTarget(
    canvasPoint: Offset,
    shapes: List<ComposeShape>,
    devices: List<SchemeDevice>,
    deviceSize: Float
): Pair<String, DragTargetType>? {
    // Сначала приборы (они выше)
    for (device in devices.reversed()) {
        val deviceRect = Rect(
            left = device.x,
            top = device.y,
            right = device.x + deviceSize,
            bottom = device.y + deviceSize
        )
        if (deviceRect.contains(canvasPoint)) {
            return Pair(device.deviceId.toString(), DragTargetType.DEVICE)
        }
    }

    // Потом фигуры
    for (shape in shapes.reversed()) {
        if (shape.contains(canvasPoint)) {
            return Pair(shape.id, DragTargetType.SHAPE)
        }
    }

    return null
}

enum class DragTargetType { SHAPE, DEVICE }