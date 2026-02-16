package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.shapes.*
import com.kipia.management.mobile.viewmodel.EditorMode
import com.kipia.management.mobile.viewmodel.EditorState
import kotlinx.coroutines.delay
import kotlin.math.sqrt
import timber.log.Timber

@Composable
fun SchemeCanvas(
    editorState: EditorState,
    shapes: List<ComposeShape>,
    devices: List<SchemeDevice>,
    availableDevices: List<Device>,
    onShapeClick: (String) -> Unit,
    onDeviceClick: (Int) -> Unit,
    onCanvasClick: (Offset) -> Unit,
    onShapeDrag: (String, Offset) -> Unit,
    onDeviceDrag: (Int, Offset) -> Unit,
    onTransform: (Float, Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    val canvasState = editorState.canvasState
    val density = LocalDensity.current

    var isDragging by remember { mutableStateOf(false) }
    var showZoomIndicator by remember { mutableStateOf(false) }

    val dragThreshold = with(density) { 5.dp.toPx() }

    // В SchemeCanvas.kt, внутри Box, добавьте:
    LaunchedEffect(canvasState.scale, canvasState.offset) {
        Timber.d("Canvas state changed - scale: ${canvasState.scale}, offset: ${canvasState.offset}")
    }

    LaunchedEffect(canvasState) {
        Timber.d("CanvasState CHANGED - scale: ${canvasState.scale}, offset: ${canvasState.offset}")
    }

    // Логируем начальное состояние
    LaunchedEffect(Unit) {
        Timber.d("SchemeCanvas initialized with mode: ${editorState.uiState.mode}")
    }

    LaunchedEffect(canvasState.scale) {
        showZoomIndicator = true
        Timber.d("Scale changed to: ${canvasState.scale}")
        delay(500)
        showZoomIndicator = false
    }

    fun screenToCanvas(screenPoint: Offset): Offset {
        val canvasPoint = Offset(
            (screenPoint.x - canvasState.offset.x) / canvasState.scale,
            (screenPoint.y - canvasState.offset.y) / canvasState.scale
        )
        Timber.v("Screen to canvas: $screenPoint -> $canvasPoint (scale: ${canvasState.scale}, offset: ${canvasState.offset})")
        return canvasPoint
    }

    fun Offset.distanceTo(other: Offset): Float {
        return sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y))
    }

    Box(
        modifier = modifier
            .background(canvasState.backgroundColor)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(editorState.uiState.mode) {
                Timber.d("Pointer input block started with mode: ${editorState.uiState.mode}")

                // Обычные переменные, не remember (мы в корутине)
                var isMultiTouch = false
                var initialDistance = 0f
                var initialScale = 1f

                awaitEachGesture {
                    Timber.d("--- New gesture started ---")

                    // ВАЖНО: получаем актуальное состояние канваса в начале каждого жеста
                    val startScale = canvasState.scale
                    val startOffset = canvasState.offset

                    Timber.d("Canvas state at gesture start - scale: $startScale, offset: $startOffset")

                    val down = awaitFirstDown(requireUnconsumed = false)
                    Timber.d("DOWN event at screen: ${down.position}, id: ${down.id}, pressed: ${down.pressed}")

                    var isDrag = false
                    var targetAtStart: Pair<String, DragTargetType>? = null

                    // Проверяем цель только если не в режиме добавления
                    if (editorState.uiState.mode == EditorMode.NONE) {
                        val canvasStart = screenToCanvas(down.position)
                        targetAtStart = findTarget(canvasStart, shapes, devices)

                        Timber.d("Target at start: $targetAtStart at canvas position: $canvasStart")

                        if (targetAtStart != null) {
                            val (id, type) = targetAtStart
                            Timber.i("TARGET FOUND - Clicking: $type with id: $id")
                            when (type) {
                                DragTargetType.SHAPE -> {
                                    Timber.d("Calling onShapeClick with id: $id")
                                    onShapeClick(id)
                                }
                                DragTargetType.DEVICE -> {
                                    Timber.d("Calling onDeviceClick with id: $id")
                                    onDeviceClick(id.toInt())
                                }
                            }
                        }
                    } else {
                        Timber.d("Skipping target detection - in mode: ${editorState.uiState.mode}")
                    }

                    // Сбрасываем флаги мультитач для нового жеста
                    isMultiTouch = false

                    // Собираем все последующие события
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        Timber.v("Pointer event: ${event.changes.size} fingers, changes: ${event.changes.map { "${it.id}:${it.pressed}" }}")

                        // Обработка мультитач (зум)
                        if (event.changes.size >= 2) {
                            Timber.d("MULTITOUCH detected with ${event.changes.size} fingers")

                            if (!isMultiTouch) {
                                isMultiTouch = true
                                initialDistance = calculateDistance(event)
                                initialScale = startScale  // Используем сохраненный масштаб
                                Timber.d("Multi-touch started - initialDistance: $initialDistance, initialScale: $initialScale")
                            } else {
                                val newDistance = calculateDistance(event)
                                if (newDistance > 0 && initialDistance > 0) {
                                    val scaleFactor = newDistance / initialDistance
                                    val newScale = (initialScale * scaleFactor).coerceIn(0.1f, 5f)

                                    val centroid = calculateCentroid(event)
                                    val centroidCanvas = screenToCanvas(centroid)
                                    val newOffset = centroid - (centroidCanvas * newScale)

                                    if (newScale != canvasState.scale) {
                                        Timber.i("ZOOM - factor: $scaleFactor, newScale: $newScale (current: ${canvasState.scale})")
                                        onTransform(newScale, newOffset)
                                    }
                                }
                            }
                            event.changes.forEach { it.consume() }
                            continue
                        }

                        // Обработка одиночного касания
                        if (!isMultiTouch && event.changes.size == 1) {
                            val change = event.changes.first()

                            when {
                                // Начало драга
                                change.pressed && !isDrag -> {
                                    val dragDistance = change.position.distanceTo(down.position)
                                    Timber.v("Drag distance: $dragDistance, threshold: $dragThreshold")

                                    if (dragDistance > dragThreshold) {
                                        isDrag = true
                                        isDragging = true
                                        Timber.i("DRAG STARTED - distance: $dragDistance, target: $targetAtStart")
                                    }
                                }

                                // Перемещение объекта (драг)
                                isDrag && targetAtStart != null && editorState.uiState.mode == EditorMode.NONE -> {
                                    val delta = change.position - change.previousPosition
                                    val canvasDelta = Offset(
                                        delta.x / startScale,  // Используем сохраненный масштаб
                                        delta.y / startScale
                                    )

                                    val (id, type) = targetAtStart
                                    Timber.v("DRAGGING $type id:$id - screen delta: $delta, canvas delta: $canvasDelta")

                                    when (type) {
                                        DragTargetType.SHAPE -> {
                                            onShapeDrag(id, canvasDelta)
                                        }
                                        DragTargetType.DEVICE -> {
                                            onDeviceDrag(id.toInt(), canvasDelta)
                                        }
                                    }
                                }

                                // Панорамирование (когда нет цели или режим добавления)
                                change.pressed && (targetAtStart == null || editorState.uiState.mode != EditorMode.NONE) -> {
                                    val delta = change.position - change.previousPosition
                                    if (delta != Offset.Zero) {
                                        // ВАЖНО: используем startOffset и startScale
                                        // При панорамировании мы двигаем канвас, поэтому новый оффсет = старый оффсет + дельта
                                        val newOffset = startOffset + delta
                                        Timber.i("PANNING - delta: $delta, newOffset: $newOffset, start scale: $startScale")
                                        onTransform(startScale, newOffset)
                                    }
                                }
                            }
                        }

                    } while (!event.changes.all { it.changedToUp() })

                    // Завершение жеста
                    Timber.d("Gesture ended - isDrag: $isDrag, isMultiTouch: $isMultiTouch, targetAtStart: $targetAtStart, mode: ${editorState.uiState.mode}")

                    if (!isDrag && !isMultiTouch && targetAtStart == null && editorState.uiState.mode != EditorMode.NONE) {
                        val canvasPoint = screenToCanvas(down.position)
                        Timber.i("ADD MODE CLICK - Creating object at canvas: $canvasPoint, mode: ${editorState.uiState.mode}")
                        onCanvasClick(canvasPoint)
                    } else if (!isDrag && !isMultiTouch && targetAtStart == null && editorState.uiState.mode == EditorMode.NONE) {
                        Timber.i("EMPTY CANVAS CLICK - Clearing selection")
                        onCanvasClick(Offset.Unspecified)
                    } else if (!isDrag && !isMultiTouch && targetAtStart != null) {
                        Timber.i("SINGLE TAP on target - already handled at down event")
                    }

                    isDragging = false
                    Timber.d("--- Gesture fully processed ---")
                }
            }
            .graphicsLayer {
                translationX = canvasState.offset.x
                translationY = canvasState.offset.y
                scaleX = canvasState.scale
                scaleY = canvasState.scale
                Timber.v("GraphicsLayer applied - scale: $scaleX, offset: ($translationX, $translationY)")
            }
    ) {
        // Фон
        canvasState.backgroundImage?.let {
            AsyncImage(
                model = it,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Фигуры и устройства
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Фигуры
            shapes.sortedBy { it.zIndex }.forEach { shape ->
                withTransform({
                    translate(shape.x, shape.y)
                    rotate(shape.rotation, Offset(shape.width / 2, shape.height / 2))
                }) {
                    drawShape(shape)
                    if (shape.id == editorState.selection.selectedShapeId) {
                        drawShapeSelection(shape)
                    }
                }
            }

            // Устройства
            devices.sortedBy { it.zIndex }.forEach { schemeDevice ->
                availableDevices.find { it.id == schemeDevice.deviceId }?.let { device ->
                    val isSelected = schemeDevice.deviceId == editorState.selection.selectedDeviceId
                    withTransform({
                        translate(schemeDevice.x, schemeDevice.y)
                        rotate(schemeDevice.rotation, Offset(40f, 40f))
                        scale(scaleX = schemeDevice.scale, scaleY = schemeDevice.scale)
                    }) {
                        drawDevice(device, isSelected)
                    }
                }
            }
        }

        // Индикатор зума
        if (showZoomIndicator) {
            Card(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${(canvasState.scale * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        // Индикатор режима
        if (editorState.uiState.mode != EditorMode.NONE) {
            ModeChip(
                mode = editorState.uiState.mode,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(8.dp)
            )
        }
    }
}

// Вспомогательные функции с логированием
private fun calculateDistance(event: PointerEvent): Float {
    if (event.changes.size < 2) return 0f
    val points = event.changes.map { it.position }
    val distance = points[0].distanceTo(points[1])
    Timber.v("Distance between fingers: $distance")
    return distance
}

private fun calculateCentroid(event: PointerEvent): Offset {
    val centroid = event.changes.map { it.position }
        .reduce { acc, pos -> acc + pos }
        .let { Offset(it.x / event.changes.size, it.y / event.changes.size) }
    Timber.v("Centroid: $centroid")
    return centroid
}

private fun Offset.distanceTo(other: Offset): Float {
    return sqrt((x - other.x) * (x - other.x) + (y - other.y) * (y - other.y))
}

// Остальные функции без изменений...
private fun findTarget(
    point: Offset,
    shapes: List<ComposeShape>,
    devices: List<SchemeDevice>
): Pair<String, DragTargetType>? {
    // Проверяем фигуры
    shapes.reversed().find { it.contains(point) }?.let { shape ->
        Timber.d("Found shape at $point: ${shape.id}")
        return Pair(shape.id, DragTargetType.SHAPE)
    }

    // Проверяем устройства
    devices.reversed().find { device ->
        point.x in device.x..(device.x + 80) &&
                point.y in device.y..(device.y + 80)
    }?.let { device ->
        Timber.d("Found device at $point: ${device.deviceId}")
        return Pair(device.deviceId.toString(), DragTargetType.DEVICE)
    }

    Timber.d("No target found at $point")
    return null
}

private enum class DragTargetType {
    SHAPE, DEVICE
}

private fun DrawScope.drawShape(shape: ComposeShape) {
    when (shape) {
        is ComposeRectangle -> {
            drawRoundRect(
                color = shape.fillColor,
                size = Size(shape.width, shape.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(shape.cornerRadius)
            )
            drawRoundRect(
                color = shape.strokeColor,
                size = Size(shape.width, shape.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(shape.cornerRadius),
                style = Stroke(shape.strokeWidth)
            )
        }
        is ComposeLine -> {
            drawLine(
                color = shape.strokeColor,
                start = Offset(shape.startX, shape.startY),
                end = Offset(shape.endX, shape.endY),
                strokeWidth = shape.strokeWidth,
                cap = StrokeCap.Round
            )
        }
        is ComposeEllipse -> {
            drawOval(
                color = shape.fillColor,
                size = Size(shape.width, shape.height)
            )
            drawOval(
                color = shape.strokeColor,
                size = Size(shape.width, shape.height),
                style = Stroke(shape.strokeWidth)
            )
        }
        is ComposeText -> {
            if (shape.fillColor != Color.Transparent) {
                drawRect(
                    color = shape.fillColor,
                    size = Size(shape.width, shape.height)
                )
            }

            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = shape.textColor.toArgb()
                    textSize = shape.fontSize
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    if (shape.isBold) isFakeBoldText = true
                    if (shape.isItalic) textSkewX = -0.25f
                }

                val textBounds = android.graphics.Rect()
                paint.getTextBounds(shape.text, 0, shape.text.length, textBounds)
                val textY = shape.height / 2 + (textBounds.height() / 2)

                canvas.nativeCanvas.drawText(
                    shape.text,
                    shape.width / 2,
                    textY,
                    paint
                )
            }
        }
        is ComposeRhombus -> {
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(shape.width / 2, 0f)
                lineTo(shape.width, shape.height / 2)
                lineTo(shape.width / 2, shape.height)
                lineTo(0f, shape.height / 2)
                close()
            }
            drawPath(path, shape.fillColor)
            drawPath(path, shape.strokeColor, style = Stroke(shape.strokeWidth))
        }
    }
}

private fun DrawScope.drawShapeSelection(shape: ComposeShape) {
    when (shape) {
        is ComposeLine -> {
            drawLine(
                color = Color.Blue.copy(alpha = 0.3f),
                start = Offset(shape.startX, shape.startY),
                end = Offset(shape.endX, shape.endY),
                strokeWidth = shape.strokeWidth + 6f
            )
            drawCircle(Color.Blue, 6f, Offset(shape.startX, shape.startY))
            drawCircle(Color.Blue, 6f, Offset(shape.endX, shape.endY))
        }
        else -> {
            drawRect(
                color = Color.Blue.copy(alpha = 0.3f),
                topLeft = Offset(-4f, -4f),
                size = Size(shape.width + 8f, shape.height + 8f),
                style = Stroke(2f)
            )
        }
    }
}

private fun DrawScope.drawDevice(device: Device, isSelected: Boolean) {
    drawRoundRect(
        color = Color(0xFF2196F3),
        size = Size(80f, 80f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
    )

    // Безопасно получаем первый символ
    val displayChar = device.name?.firstOrNull()?.toString() ?:
    device.type.firstOrNull()?.toString() ?: "?"

    drawIntoCanvas { canvas ->
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 30f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.nativeCanvas.drawText(displayChar, 40f, 50f, paint)
    }

    if (isSelected) {
        drawRoundRect(
            color = Color.Blue,
            topLeft = Offset(-3f, -3f),
            size = Size(86f, 86f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f),
            style = Stroke(3f)
        )
    }

    drawRect(
        color = Color.White.copy(alpha = 0.8f),
        topLeft = Offset(10f, 10f),
        size = Size(60f, 60f)
    )
}

@Composable
private fun ModeChip(
    mode: EditorMode,
    modifier: Modifier = Modifier
) {
    val text = when (mode) {
        EditorMode.RECTANGLE -> "Режим: Прямоугольник"
        EditorMode.LINE -> "Режим: Линия"
        EditorMode.ELLIPSE -> "Режим: Эллипс"
        EditorMode.RHOMBUS -> "Режим: Ромб"
        EditorMode.TEXT -> "Режим: Текст"
        EditorMode.DEVICE -> "Режим: Добавление прибора"
        else -> ""
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}