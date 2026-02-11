package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeData
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.shapes.*
import com.kipia.management.mobile.viewmodel.EditorMode
import kotlin.math.pow
import kotlin.math.sqrt
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeCanvas(
    schemeData: SchemeData,
    devices: List<Device>,
    schemeDevices: List<SchemeDevice>,
    shapes: List<ComposeShape>,
    editorMode: EditorMode,
    onDeviceDrag: (Int, Offset) -> Unit,
    onDeviceClick: (Int) -> Unit,
    onShapeClick: (ComposeShape) -> Unit,
    onCanvasClick: (Offset) -> Unit,
    onShapeDrag: (String, Offset) -> Unit,
    onShapeResize: (String, Float, Float) -> Unit,
    selectedDeviceId: Int?,
    selectedShape: ComposeShape?,
    modifier: Modifier = Modifier
) {
    var localCanvasScale by remember { mutableStateOf(1f) }
    var localCanvasOffset by remember { mutableStateOf(Offset.Zero) }

    // Состояния перетаскивания
    var isDragging by remember { mutableStateOf(false) }
    var isDraggingObject by remember { mutableStateOf(false) }
    var draggedObjectId by remember { mutableStateOf<String?>(null) }
    var dragObjectType by remember { mutableStateOf<DragObjectType?>(null) }

    // Состояния ресайза
    var isResizing by remember { mutableStateOf(false) }
    var resizeStartSize by remember { mutableStateOf(Size(0f, 0f)) }
    var resizeStartEndPoint by remember { mutableStateOf(Offset.Zero) }

    // Индикатор зума
    var isZooming by remember { mutableStateOf(false) }

    val backgroundColor = remember(schemeData.backgroundColor) {
        parseColor(schemeData.backgroundColor ?: "#FFFFFFFF")
    }

    LaunchedEffect(isZooming) {
        if (isZooming) {
            delay(500)
            isZooming = false
        }
    }

    fun screenToLocal(point: Offset): Offset {
        return Offset(
            (point.x - localCanvasOffset.x) / localCanvasScale,
            (point.y - localCanvasOffset.y) / localCanvasScale
        )
    }

    fun isInResizeHandle(point: Offset, shape: ComposeShape): Boolean {
        return when (shape) {
            is ComposeLine -> {
                val end = Offset(shape.x + shape.endX, shape.y + shape.endY)
                (point - end).getDistance() <= 20f
            }
            else -> {
                val shapeEnd = Offset(shape.x + shape.width, shape.y + shape.height)
                (point - shapeEnd).getDistance() <= 20f
            }
        }
    }

    fun findShapeAtPoint(point: Offset): ComposeShape? {
        val found = shapes.reversed().firstOrNull { shape ->
            val contains = when (shape) {
                is ComposeRectangle -> pointInRectangle(point, shape, 10f)
                is ComposeLine -> pointInLine(point, shape, shape.strokeWidth + 10f)
                is ComposeEllipse -> pointInEllipse(point, shape)
                is ComposeText -> pointInText(point, shape, 10f)
                is ComposeRhombus -> pointInRhombus(point, shape)
                else -> false
            }
            if (contains) {
                println("🔍 findShapeAtPoint: FOUND shape ${shape.id} at (${shape.x}, ${shape.y}) with size ${shape.width}x${shape.height}")
            }
            contains
        }

        if (found == null) {
            println("🔍 findShapeAtPoint: NO SHAPE found at $point. Shapes count: ${shapes.size}")
            shapes.forEach { shape ->
                println("   ➤ Shape ${shape.id}: x=${shape.x}, y=${shape.y}, w=${shape.width}, h=${shape.height}")
            }
        }

        return found
    }

    fun findDeviceAtPoint(point: Offset): SchemeDevice? {
        return schemeDevices.firstOrNull { schemeDevice ->
            point.x in schemeDevice.x..(schemeDevice.x + 80) &&
                    point.y in schemeDevice.y..(schemeDevice.y + 80)
        }
    }

    Box(
        modifier = modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            // ОБРАБОТЧИК ТАПОВ
            .pointerInput(editorMode) {
                detectTapGestures(
                    onTap = { offset ->
                        val localPoint = screenToLocal(offset)
                        println("SchemeCanvas: Tap at $localPoint, mode=$editorMode")

                        when (editorMode) {
                            EditorMode.RECTANGLE,
                            EditorMode.LINE,
                            EditorMode.ELLIPSE,
                            EditorMode.RHOMBUS,
                            EditorMode.TEXT -> {
                                onCanvasClick(localPoint)
                            }
                            EditorMode.SELECT -> {
                                val shapeAtPoint = findShapeAtPoint(localPoint)
                                if (shapeAtPoint != null) {
                                    println("SchemeCanvas: Tap on shape ${shapeAtPoint.id}")
                                    onShapeClick(shapeAtPoint)
                                } else {
                                    val deviceAtPoint = findDeviceAtPoint(localPoint)
                                    if (deviceAtPoint != null) {
                                        println("SchemeCanvas: Tap on device ${deviceAtPoint.deviceId}")
                                        onDeviceClick(deviceAtPoint.deviceId)
                                    } else {
                                        println("SchemeCanvas: Tap on empty space")
                                        onCanvasClick(localPoint)
                                    }
                                }
                            }
                            else -> {}
                        }
                    },
                    onDoubleTap = { _ ->
                        localCanvasScale = 1f
                        localCanvasOffset = Offset.Zero
                        isZooming = true
                    }
                )
            }
            // ОБРАБОТЧИК ПЕРЕТАСКИВАНИЯ
            .pointerInput(editorMode) {
                if (editorMode != EditorMode.SELECT) {
                    return@pointerInput // Перетаскивание и ресайз ТОЛЬКО в SELECT режиме
                }

                detectDragGestures(
                    onDragStart = { offset ->
                        val localPoint = screenToLocal(offset)
                        isDragging = true
                        isDraggingObject = false
                        isResizing = false

                        // 1. Ресайз (только для фигур)
                        selectedShape?.let { shape ->
                            if (isInResizeHandle(localPoint, shape)) {
                                isResizing = true
                                isDraggingObject = true
                                draggedObjectId = shape.id
                                dragObjectType = DragObjectType.SHAPE

                                when (shape) {
                                    is ComposeLine -> {
                                        resizeStartEndPoint = Offset(shape.endX, shape.endY)
                                    }
                                    else -> {
                                        resizeStartSize = Size(shape.width, shape.height)
                                    }
                                }
                                return@detectDragGestures
                            }
                        }

                        // 2. Фигура
                        val shapeAtPoint = findShapeAtPoint(localPoint)
                        if (shapeAtPoint != null) {
                            isDraggingObject = true
                            draggedObjectId = shapeAtPoint.id
                            dragObjectType = DragObjectType.SHAPE
                            return@detectDragGestures
                        }

                        // 3. Устройство
                        val deviceAtPoint = findDeviceAtPoint(localPoint)
                        if (deviceAtPoint != null) {
                            isDraggingObject = true
                            draggedObjectId = deviceAtPoint.deviceId.toString()
                            dragObjectType = DragObjectType.DEVICE
                            return@detectDragGestures
                        }

                        // Если ничего не выбрано — не начинаем перетаскивание
                    },
                    onDrag = { _, dragAmount ->
                        val localDrag = dragAmount / localCanvasScale

                        when {
                            isResizing -> {
                                draggedObjectId?.let { shapeId ->
                                    selectedShape?.let { shape ->
                                        if (shape.id == shapeId) {
                                            when (shape) {
                                                is ComposeLine -> {
                                                    val newEndX = resizeStartEndPoint.x + localDrag.x
                                                    val newEndY = resizeStartEndPoint.y + localDrag.y
                                                    onShapeResize(shapeId, newEndX, newEndY)
                                                }
                                                else -> {
                                                    val newWidth = maxOf(20f, resizeStartSize.width + localDrag.x)
                                                    val newHeight = maxOf(20f, resizeStartSize.height + localDrag.y)
                                                    onShapeResize(shapeId, newWidth, newHeight)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            isDraggingObject && dragObjectType == DragObjectType.SHAPE -> {
                                draggedObjectId?.let { shapeId ->
                                    onShapeDrag(shapeId, localDrag)
                                }
                            }
                            isDraggingObject && dragObjectType == DragObjectType.DEVICE -> {
                                draggedObjectId?.toIntOrNull()?.let { deviceId ->
                                    onDeviceDrag(deviceId, localDrag)
                                }
                            }
                            else -> {
                                // В SELECT режиме — НЕ ПАНОРИРОВАТЬ ХОЛСТ!
                                // Это важно: в SELECT режиме пользователь не может перемещать холст!
                            }
                        }
                    },
                    onDragEnd = {
                        isDragging = false
                        isDraggingObject = false
                        isResizing = false
                        draggedObjectId = null
                        dragObjectType = null
                    },
                    onDragCancel = {
                        isDragging = false
                        isDraggingObject = false
                        isResizing = false
                        draggedObjectId = null
                        dragObjectType = null
                    }
                )
            }
            // ОБРАБОТЧИК ЗУМА
            .pointerInput(editorMode) {
                if (editorMode != EditorMode.NONE) {
                    return@pointerInput // Зум только в режиме NONE
                }

                detectTransformGestures(
                    panZoomLock = false,
                    onGesture = { _, pan, zoom, _ ->
                        isZooming = true
                        localCanvasScale = (localCanvasScale * zoom).coerceIn(0.1f, 5f)
                        localCanvasOffset += pan / localCanvasScale
                    }
                )
            }
    ) {
        // Контейнер с трансформациями
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = localCanvasScale
                    scaleY = localCanvasScale
                    translationX = localCanvasOffset.x
                    translationY = localCanvasOffset.y
                }
        ) {
            // Сетка
            if (schemeData.gridEnabled) {
                GridLayer(
                    gridSize = schemeData.gridSize,
                    canvasScale = localCanvasScale,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Фоновое изображение
            schemeData.backgroundImage?.let { imageUri ->
                AsyncImage(
                    model = imageUri,
                    contentDescription = "Фон схемы",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Фигуры
            ShapesLayer(
                shapes = shapes,
                selectedShape = selectedShape,
                editorMode = editorMode,
                modifier = Modifier.fillMaxSize()
            )

            // Устройства
            DevicesLayer(
                devices = devices,
                schemeDevices = schemeDevices,
                selectedDeviceId = selectedDeviceId,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Индикатор зума
        if (isZooming) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(0.7f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "${(localCanvasScale * 100).toInt()}%",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// ============ ОТДЕЛЬНЫЕ СЛОИ ============

@Composable
fun GridLayer(
    gridSize: Int,
    canvasScale: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val gridSizeFloat = gridSize.toFloat()
        val width = size.width / canvasScale
        val height = size.height / canvasScale

        for (x in 0..(width / gridSizeFloat).toInt()) {
            val xPos = x * gridSizeFloat
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(xPos, 0f),
                end = Offset(xPos, height),
                strokeWidth = 1f / canvasScale
            )
        }

        for (y in 0..(height / gridSizeFloat).toInt()) {
            val yPos = y * gridSizeFloat
            drawLine(
                color = Color.Gray.copy(alpha = 0.3f),
                start = Offset(0f, yPos),
                end = Offset(width, yPos),
                strokeWidth = 1f / canvasScale
            )
        }
    }
}

@Composable
fun ShapesLayer(
    shapes: List<ComposeShape>,
    selectedShape: ComposeShape?,
    editorMode: EditorMode,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        shapes.sortedBy { it.zIndex }.forEach { shape ->
            withTransform({
                translate(left = shape.x, top = shape.y)
                rotate(shape.rotation, pivot = Offset(shape.width / 2, shape.height / 2))
            }) {
                // Отрисовка самой фигуры
                when (shape) {
                    is ComposeRectangle -> {
                        drawRoundRect(
                            color = shape.fillColor,
                            topLeft = Offset.Zero,
                            size = Size(shape.width, shape.height),
                            cornerRadius = CornerRadius(shape.cornerRadius)
                        )
                        drawRoundRect(
                            color = shape.strokeColor,
                            topLeft = Offset.Zero,
                            size = Size(shape.width, shape.height),
                            cornerRadius = CornerRadius(shape.cornerRadius),
                            style = Stroke(width = shape.strokeWidth)
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
                            topLeft = Offset.Zero,
                            size = Size(shape.width, shape.height)
                        )
                        drawOval(
                            color = shape.strokeColor,
                            topLeft = Offset.Zero,
                            size = Size(shape.width, shape.height),
                            style = Stroke(width = shape.strokeWidth)
                        )
                    }
                    is ComposeText -> {
                        if (shape.fillColor != Color.Transparent) {
                            drawRect(
                                color = shape.fillColor,
                                topLeft = Offset.Zero,
                                size = Size(shape.width, shape.height)
                            )
                        }

                        drawIntoCanvas { canvas ->
                            val paint = AndroidPaint().apply {
                                color = shape.textColor.toArgb()
                                textSize = shape.fontSize
                                isAntiAlias = true
                                textAlign = AndroidPaint.Align.CENTER
                                if (shape.isBold) {
                                    isFakeBoldText = true
                                }
                                if (shape.isItalic) {
                                    textSkewX = -0.25f
                                }
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

                        if (shape.strokeWidth > 0 && shape.strokeColor != Color.Transparent) {
                            drawRect(
                                color = shape.strokeColor,
                                topLeft = Offset.Zero,
                                size = Size(shape.width, shape.height),
                                style = Stroke(width = shape.strokeWidth)
                            )
                        }
                    }
                    is ComposeRhombus -> {
                        val path = Path().apply {
                            moveTo(shape.width / 2, 0f)
                            lineTo(shape.width, shape.height / 2)
                            lineTo(shape.width / 2, shape.height)
                            lineTo(0f, shape.height / 2)
                            close()
                        }

                        drawPath(path = path, color = shape.fillColor)
                        drawPath(
                            path = path,
                            color = shape.strokeColor,
                            style = Stroke(width = shape.strokeWidth)
                        )
                    }
                }

                // Отрисовка выделения
                if (selectedShape?.id == shape.id) {
                    when (shape) {
                        is ComposeLine -> {
                            drawLine(
                                color = Color.Blue.copy(alpha = 0.5f),
                                start = Offset(shape.startX, shape.startY),
                                end = Offset(shape.endX, shape.endY),
                                strokeWidth = shape.strokeWidth + 4f,
                                cap = StrokeCap.Round
                            )

                            if (editorMode == EditorMode.SELECT) {
                                val handleSize = 8f
                                drawCircle(
                                    color = Color.Blue,
                                    radius = handleSize,
                                    center = Offset(shape.startX, shape.startY)
                                )
                                drawCircle(
                                    color = Color.Blue,
                                    radius = handleSize,
                                    center = Offset(shape.endX, shape.endY)
                                )
                                drawCircle(
                                    color = Color(0xFF2196F3),
                                    radius = 12f,
                                    center = Offset(shape.endX, shape.endY)
                                )
                            }
                        }
                        else -> {
                            drawRect(
                                color = Color.Blue.copy(alpha = 0.3f),
                                topLeft = Offset(-5f, -5f),
                                size = Size(shape.width + 10f, shape.height + 10f),
                                style = Stroke(width = 2f)
                            )

                            if (editorMode == EditorMode.SELECT) {
                                val handleSize = 8f
                                val handles = listOf(
                                    Offset(0f, 0f),
                                    Offset(shape.width, 0f),
                                    Offset(0f, shape.height),
                                    Offset(shape.width, shape.height)
                                )

                                handles.forEach { handle ->
                                    drawCircle(
                                        color = Color.Blue,
                                        radius = handleSize,
                                        center = handle
                                    )
                                }

                                drawCircle(
                                    color = Color(0xFF2196F3),
                                    radius = 12f,
                                    center = Offset(shape.width, shape.height)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DevicesLayer(
    devices: List<Device>,
    schemeDevices: List<SchemeDevice>,
    selectedDeviceId: Int?,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        schemeDevices.forEach { schemeDevice ->
            val device = devices.find { it.id == schemeDevice.deviceId }
            device?.let {
                val isSelected = selectedDeviceId == device.id

                withTransform({
                    translate(left = schemeDevice.x, top = schemeDevice.y)
                    rotate(schemeDevice.rotation, pivot = Offset(40f, 40f))
                }) {
                    val deviceColor = Color(0xFF2196F3)

                    drawRoundRect(
                        color = deviceColor,
                        topLeft = Offset.Zero,
                        size = Size(80f, 80f),
                        cornerRadius = CornerRadius(8f)
                    )

                    if (isSelected) {
                        drawRoundRect(
                            color = Color.Blue.copy(alpha = 0.3f),
                            topLeft = Offset(-3f, -3f),
                            size = Size(86f, 86f),
                            cornerRadius = CornerRadius(8f),
                            style = Stroke(width = 3f)
                        )
                    }

                    val iconSize = 60f
                    val iconOffset = Offset(10f, 10f)
                    drawRect(
                        color = Color.White.copy(alpha = 0.8f),
                        topLeft = iconOffset,
                        size = Size(iconSize, iconSize)
                    )

                    drawIntoCanvas { canvas ->
                        val deviceName = device.name ?: device.type
                        val paint = AndroidPaint().apply {
                            color = AndroidColor.BLACK
                            textSize = 12f
                            textAlign = AndroidPaint.Align.CENTER
                            isAntiAlias = true
                        }

                        canvas.nativeCanvas.drawText(
                            deviceName.take(10),
                            40f,
                            95f,
                            paint
                        )
                    }

                    val statusColor = when (device.status) {
                        "В работе" -> Color(0xFF4CAF50)
                        "На ремонте" -> Color(0xFFFF9800)
                        "Списан" -> Color(0xFFF44336)
                        "В резерве" -> Color(0xFF9E9E9E)
                        else -> Color.Gray
                    }

                    drawCircle(
                        color = statusColor,
                        radius = 6f,
                        center = Offset(70f, 10f)
                    )
                }
            }
        }
    }
}

// ============ ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ ============

sealed class DragObjectType {
    object SHAPE : DragObjectType()
    object DEVICE : DragObjectType()
}

private fun Offset.getDistance(): Float {
    return sqrt(x * x + y * y)
}

private fun parseColor(colorHex: String): Color {
    return try {
        val cleanHex = colorHex.removePrefix("#")
        val longColor = when (cleanHex.length) {
            6 -> "FF$cleanHex"
            8 -> cleanHex
            else -> "FFFFFFFF"
        }.toLong(16)

        Color(longColor)
    } catch (e: Exception) {
        Color.White
    }
}

private fun pointInRectangle(
    point: Offset,
    rectangle: ComposeRectangle,
    tolerance: Float
): Boolean {
    val extendedRect = Rect(
        left = rectangle.x - tolerance,
        top = rectangle.y - tolerance,
        right = rectangle.x + rectangle.width + tolerance,
        bottom = rectangle.y + rectangle.height + tolerance
    )
    return extendedRect.contains(point)
}

private fun pointInLine(
    point: Offset,
    line: ComposeLine,
    tolerance: Float
): Boolean {
    val start = Offset(line.x + line.startX, line.y + line.startY)
    val end = Offset(line.x + line.endX, line.y + line.endY)
    val distance = distanceToSegment(point, start, end)
    return distance <= tolerance
}

private fun distanceToSegment(p: Offset, v: Offset, w: Offset): Float {
    val l2 = (v.x - w.x).pow(2) + (v.y - w.y).pow(2)
    if (l2 == 0f) return sqrt((p.x - v.x).pow(2) + (p.y - v.y).pow(2))

    var t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2
    t = t.coerceIn(0f, 1f)

    val projection = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
    return sqrt((p.x - projection.x).pow(2) + (p.y - projection.y).pow(2))
}

private fun pointInEllipse(point: Offset, ellipse: ComposeEllipse): Boolean {
    val centerX = ellipse.x + ellipse.width / 2
    val centerY = ellipse.y + ellipse.height / 2
    val dx = point.x - centerX
    val dy = point.y - centerY
    return (dx * dx) / (ellipse.width * ellipse.width / 4) +
            (dy * dy) / (ellipse.height * ellipse.height / 4) <= 1
}

private fun pointInText(point: Offset, text: ComposeText, tolerance: Float): Boolean {
    return point.x in (text.x - tolerance)..(text.x + text.width + tolerance) &&
            point.y in (text.y - tolerance)..(text.y + text.height + tolerance)
}

private fun pointInRhombus(point: Offset, rhombus: ComposeRhombus): Boolean {
    val localX = point.x - rhombus.x - rhombus.width / 2
    val localY = point.y - rhombus.y - rhombus.height / 2
    val a = rhombus.width / 2
    val b = rhombus.height / 2
    return (kotlin.math.abs(localX) / a + kotlin.math.abs(localY) / b) <= 1
}