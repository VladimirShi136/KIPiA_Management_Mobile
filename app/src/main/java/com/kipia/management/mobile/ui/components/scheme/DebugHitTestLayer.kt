package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.viewmodel.CanvasState

@Composable
fun DebugHitTestLayer(
    canvasState: CanvasState,
    devices: List<SchemeDevice>,
    lastTapPoint: Offset? = null,
    lastCalculatedTarget: Pair<String, DragTargetType>? = null,
    gestureScale: Float,  // масштаб из GestureLayer
    gestureOffset: Offset, // смещение из GestureLayer
    modifier: Modifier = Modifier
) {
    val baseDeviceSize = 60f

    Canvas(modifier = modifier.fillMaxSize()) {
        // Рисуем ТОЛЬКО отладочную информацию в экранных координатах

        // 1. Hit-боксы из GestureLayer (зеленые)
        devices.forEach { device ->
            val screenX = device.x * gestureScale + gestureOffset.x
            val screenY = device.y * gestureScale + gestureOffset.y
            val screenSize = baseDeviceSize * gestureScale

            drawRect(
                color = Color.Green.copy(alpha = 0.5f),
                topLeft = Offset(screenX, screenY),
                size = androidx.compose.ui.geometry.Size(screenSize, screenSize),
                style = Stroke(width = 3f)
            )
        }

        // 2. Точка касания
        lastTapPoint?.let { tap ->
            drawCircle(
                color = Color.Yellow,
                radius = 10f,
                center = tap
            )

            drawLine(
                color = Color.Yellow,
                start = tap.copy(x = tap.x - 15, y = tap.y - 15),
                end = tap.copy(x = tap.x + 15, y = tap.y + 15),
                strokeWidth = 2f
            )
            drawLine(
                color = Color.Yellow,
                start = tap.copy(x = tap.x - 15, y = tap.y + 15),
                end = tap.copy(x = tap.x + 15, y = tap.y - 15),
                strokeWidth = 2f
            )
        }

        // 3. Информация о цели
        lastCalculatedTarget?.let { target ->
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.YELLOW
                    textSize = 30f
                    textAlign = android.graphics.Paint.Align.LEFT
                }
                canvas.nativeCanvas.drawText(
                    "Target: ${target.first} (${target.second})",
                    50f,
                    100f,
                    paint
                )
            }
        }
    }
}