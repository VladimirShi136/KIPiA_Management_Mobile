package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeData
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.viewmodel.EditorMode
import android.graphics.Color as AndroidColor
import android.graphics.Paint as AndroidPaint

@Composable
fun SchemeCanvas(
    schemeData: SchemeData,
    devices: List<Device>,
    schemeDevices: List<SchemeDevice>,
    shapes: List<ComposeShape>,
    editorMode: EditorMode,
    onDeviceDrag: (Int, Offset) -> Unit,
    onDeviceClick: (Int) -> Unit,
    onShapeClick: (Offset) -> Unit,
    onShapeDrag: (Float, Float) -> Unit,
    onCanvasClick: () -> Unit,
    selectedDeviceId: Int?,
    selectedShape: ComposeShape?,
    modifier: Modifier = Modifier
) {
    var canvasSize by remember { mutableStateOf(Size.Zero) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var scale by remember { mutableStateOf(1f) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale *= zoomChange
        offset += panChange
        scale = scale.coerceIn(0.1f, 5f)
    }

    BoxWithConstraints(
        modifier = modifier
            .background(color = Color.LightGray.copy(alpha = 0.2f))
            .clipToBounds()
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .transformable(transformableState)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start ->
                            when (editorMode) {
                                EditorMode.SELECT -> {
                                    // Конвертируем координаты с учетом масштаба и смещения
                                    val canvasPoint = Offset(
                                        (start.x - offset.x) / scale,
                                        (start.y - offset.y) / scale
                                    )
                                    onShapeClick(canvasPoint)
                                }
                                EditorMode.RECTANGLE -> {
                                    // TODO: Добавить прямоугольник через ViewModel
                                }
                                // ... другие режимы
                                else -> {}
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (editorMode == EditorMode.SELECT && selectedShape != null) {
                                // Перемещение выбранной фигуры
                                val deltaX = dragAmount.x / scale
                                val deltaY = dragAmount.y / scale
                                onShapeDrag(deltaX, deltaY)
                            }
                        },
                        onDragEnd = {
                            // Дополнительная логика при завершении драга
                        }
                    )
                }
        ) {
            canvasSize = size

            withTransform({
                translate(left = offset.x, top = offset.y)
                scale(scale, scale)
            }) {
                // Рисуем фон
                drawBackground(schemeData)

                // Рисуем сетку если включена
                if (schemeData.gridEnabled) {
                    drawGrid(schemeData)
                }

                // Рисуем фигуры
                shapes.forEach { shape ->
                    shape.draw(this)
                }

                // Рисуем приборы
                schemeDevices.forEach { schemeDevice ->
                    val device = devices.find { it.id == schemeDevice.deviceId }
                    device?.let {
                        drawDevice(
                            device = it,
                            schemeDevice = schemeDevice,
                            isSelected = schemeDevice.deviceId == selectedDeviceId
                        )
                    }
                }
            }
        }

        // Контролы навигации
        CanvasControls(
            offset = offset,
            scale = scale,
            onResetView = {
                offset = Offset.Zero
                scale = 1f
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

// ЭТО НЕ @Composable функция! Это функция DrawScope
private fun DrawScope.drawBackground(schemeData: SchemeData) {
    // Фоновый цвет
    try {
        val color = Color(AndroidColor.parseColor(schemeData.backgroundColor))
        // Исправлено: добавляем topLeft = Offset.Zero
        drawRect(
            color = color,
            topLeft = Offset.Zero,
            size = Size(schemeData.width.toFloat(), schemeData.height.toFloat())
        )
    } catch (_: Exception) {
        drawRect(
            color = Color.White,
            topLeft = Offset.Zero,
            size = Size(schemeData.width.toFloat(), schemeData.height.toFloat())
        )
    }
}

private fun DrawScope.drawGrid(schemeData: SchemeData) {
    val gridSize = schemeData.gridSize
    val gridColor = Color.Gray.copy(alpha = 0.3f)

    // Вертикальные линии
    for (x in 0..schemeData.width step gridSize) {
        drawLine(
            color = gridColor,
            start = Offset(x.toFloat(), 0f),
            end = Offset(x.toFloat(), schemeData.height.toFloat()),
            strokeWidth = 1f
        )
    }

    // Горизонтальные линии
    for (y in 0..schemeData.height step gridSize) {
        drawLine(
            color = gridColor,
            start = Offset(0f, y.toFloat()),
            end = Offset(schemeData.width.toFloat(), y.toFloat()),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawDevice(
    device: Device,
    schemeDevice: SchemeDevice,
    isSelected: Boolean
) {
    val deviceSize = 80f

    withTransform({
        translate(left = schemeDevice.x, top = schemeDevice.y)
        rotate(schemeDevice.rotation, pivot = Offset(deviceSize / 2, deviceSize / 2))
        scale(schemeDevice.scale, schemeDevice.scale, pivot = Offset(deviceSize / 2, deviceSize / 2))
    }) {
        // Тело прибора
        drawRoundRect(
            color = deviceColorByType(device.type),
            topLeft = Offset.Zero,
            size = Size(deviceSize, deviceSize),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Выделение если выбран
        if (isSelected) {
            drawRoundRect(
                color = Color.Blue.copy(alpha = 0.3f),
                topLeft = Offset.Zero,
                size = Size(deviceSize, deviceSize),
                cornerRadius = CornerRadius(8f, 8f),
                style = Stroke(width = 3f)
            )
        }

        // Иконка прибора (упрощенная)
        val iconSize = deviceSize * 0.6f
        val iconOffset = Offset((deviceSize - iconSize) / 2, (deviceSize - iconSize) / 2)

        drawRect(
            color = Color.White.copy(alpha = 0.8f),
            topLeft = iconOffset,
            size = Size(iconSize, iconSize)
        )

        // Название прибора - используем drawIntoCanvas для Android Paint
        drawIntoCanvas { canvas ->
            val deviceName = device.name ?: device.getDisplayName()
            canvas.nativeCanvas.drawText(
                deviceName,
                deviceSize / 2, // X координата относительно текущей системы координат
                deviceSize + 15, // Y координата
                AndroidPaint().apply {
                    color = AndroidColor.BLACK
                    textSize = 12f
                    textAlign = AndroidPaint.Align.CENTER
                }
            )
        }

        // Индикатор статуса
        val statusColor = when (device.status) {
            "В работе" -> Color(0xFF4CAF50) // Green
            "На ремонте" -> Color(0xFFFF9800) // Orange
            "Списан" -> Color(0xFFF44336) // Red
            "В резерве" -> Color(0xFF9E9E9E) // Grey
            else -> Color.Gray
        }

        drawCircle(
            color = statusColor,
            radius = 6f,
            center = Offset(deviceSize - 10f, 10f)
        )
    }
}

private fun deviceColorByType(type: String): Color {
    return when (type.lowercase()) {
        "манометр" -> Color(0xFF2196F3) // Blue
        "термометр" -> Color(0xFFF44336) // Red
        "счетчик" -> Color(0xFF4CAF50) // Green
        "клапан" -> Color(0xFFFF9800) // Orange
        "задвижка" -> Color(0xFF9C27B0) // Purple
        "датчик" -> Color(0xFF00BCD4) // Cyan
        "преобразователь" -> Color(0xFF795548) // Brown
        "регулятор" -> Color(0xFF607D8B) // Blue Grey
        "другое" -> Color(0xFF757575) // Grey
        else -> Color(0xFF757575) // Grey
    }
}

@Composable
fun CanvasControls(
    offset: Offset,
    scale: Float,
    onResetView: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Кнопка сброса вида
        FloatingActionButton(
            onClick = onResetView,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close, // ИЗМЕНЕНО
                contentDescription = "Сброс вида"
            )
        }

        // Индикатор масштаба
        Card(
            modifier = Modifier.size(48.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "${(scale * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        // Индикатор положения
        Card(
            modifier = Modifier.size(48.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "${offset.x.toInt()}, ${offset.y.toInt()}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}