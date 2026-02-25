package com.kipia.management.mobile.ui.components.scheme.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint as AndroidPaint
import androidx.compose.ui.graphics.toArgb
import com.kipia.management.mobile.viewmodel.CanvasState
import kotlin.math.*

object ShapeUtils {
    // ====== ТРАНСФОРМАЦИИ ======

    fun screenToCanvas(screenPoint: Offset, canvasState: CanvasState): Offset {
        val safeScale = if (canvasState.scale == 0f) 1f else canvasState.scale
        return Offset(
            x = (screenPoint.x - canvasState.offset.x) / safeScale,
            y = (screenPoint.y - canvasState.offset.y) / safeScale
        )
    }

    fun canvasToScreen(canvasPoint: Offset, canvasState: CanvasState): Offset {
        return Offset(
            x = canvasPoint.x * canvasState.scale + canvasState.offset.x,
            y = canvasPoint.y * canvasState.scale + canvasState.offset.y
        )
    }

    fun transformPointToShapeSpace(
        point: Offset,
        shapeX: Float,
        shapeY: Float,
        shapeWidth: Float,
        shapeHeight: Float,
        rotation: Float
    ): Offset {
        val centerX = shapeX + shapeWidth / 2
        val centerY = shapeY + shapeHeight / 2

        val localX = point.x - centerX
        val localY = point.y - centerY

        val radians = rotation * PI.toFloat() / 180f
        val rotatedX = localX * cos(-radians) - localY * sin(-radians)
        val rotatedY = localX * sin(-radians) + localY * cos(-radians)

        return Offset(rotatedX, rotatedY)
    }

    // ====== ПРОВЕРКА ПОПАДАНИЯ ТОЧКИ ======

    fun isPointInRectangle(point: Offset, width: Float, height: Float): Boolean {
        return point.x in 0f..width && point.y in 0f..height
    }

    fun isPointInEllipse(point: Offset, width: Float, height: Float): Boolean {
        return (point.x * point.x) / (width * width / 4) +
                (point.y * point.y) / (height * height / 4) <= 1
    }

    fun isPointInRhombus(point: Offset, width: Float, height: Float): Boolean {
        return (abs(point.x) / (width / 2) + abs(point.y) / (height / 2)) <= 1
    }

    fun isPointInLine(point: Offset, start: Offset, end: Offset, strokeWidth: Float): Boolean {
        val distance = distanceToSegment(point, start, end)
        return distance <= (strokeWidth + 5f)
    }

    private fun distanceToSegment(p: Offset, v: Offset, w: Offset): Float {
        val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
        if (l2 == 0f) return sqrt((p.x - v.x) * (p.x - v.x) + (p.y - v.y) * (p.y - v.y))

        var t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2
        t = t.coerceIn(0f, 1f)

        val projection = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
        val dx = p.x - projection.x
        val dy = p.y - projection.y
        return sqrt(dx * dx + dy * dy)
    }

    fun isPointInText(point: Offset, width: Float, height: Float): Boolean {
        return point.x in 0f..width && point.y in 0f..height
    }

    // ====== ОТРИСОВКА ФИГУР ======

    fun DrawScope.withShapeTransform(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float,
        block: DrawScope.() -> Unit
    ) {
        withTransform({
            translate(x, y)
            rotate(rotation, Offset(width / 2, height / 2))
        }) {
            block()
        }
    }

    fun DrawScope.drawRectangle(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float,
        fillColor: Color,
        strokeColor: Color,
        strokeWidth: Float,
        cornerRadius: Float
    ) {
        withShapeTransform(x, y, width, height, rotation) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset.Zero,
                size = androidx.compose.ui.geometry.Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
            )

            if (strokeWidth > 0 && strokeColor != Color.Transparent) {
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(width, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                )
            }
        }
    }

    /**
     * Отрисовывает линию с обводкой
     */
    fun DrawScope.drawLine(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        strokeColor: Color,
        strokeWidth: Float
    ) {
        withShapeTransform(x, y, width, height, rotation) {
            drawLine(
                color = strokeColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }
    }

    /**
     * Отрисовывает эллипс с заливкой и обводкой
     */
    fun DrawScope.drawEllipse(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float,
        fillColor: Color,
        strokeColor: Color,
        strokeWidth: Float
    ) {
        withShapeTransform(x, y, width, height, rotation) {
            drawOval(
                color = fillColor,
                topLeft = Offset.Zero,
                size = androidx.compose.ui.geometry.Size(width, height)
            )

            if (strokeWidth > 0 && strokeColor != Color.Transparent) {
                drawOval(
                    color = strokeColor,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(width, height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                )
            }
        }
    }

    /**
     * Отрисовывает ромб с заливкой и обводкой
     */
    fun DrawScope.drawRhombus(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float,
        fillColor: Color,
        strokeColor: Color,
        strokeWidth: Float
    ) {
        withShapeTransform(x, y, width, height, rotation) {
            val path = Path().apply {
                moveTo(width / 2, 0f)
                lineTo(width, height / 2)
                lineTo(width / 2, height)
                lineTo(0f, height / 2)
                close()
            }

            drawPath(
                path = path,
                color = fillColor
            )

            if (strokeWidth > 0 && strokeColor != Color.Transparent) {
                drawPath(
                    path = path,
                    color = strokeColor,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
                )
            }
        }
    }

    /**
     * Отрисовывает текст с фоном и обводкой
     */
    fun DrawScope.drawText(
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        rotation: Float,
        text: String,
        fontSize: Float,
        textColor: Color,
        fillColor: Color,
        strokeColor: Color,
        strokeWidth: Float,
        isBold: Boolean,
        isItalic: Boolean
    ) {
        withShapeTransform(x, y, width, height, rotation) {
            // Фон текста
            if (fillColor != Color.Transparent) {
                drawRect(
                    color = fillColor,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(width, height)
                )
            }

            // Отрисовка текста
            drawIntoCanvas { canvas ->
                val paint = AndroidPaint().apply {
                    color = textColor.toArgb()
                    textSize = fontSize
                    isAntiAlias = true
                    textAlign = AndroidPaint.Align.CENTER
                    if (isBold) {
                        isFakeBoldText = true
                    }
                    if (isItalic) {
                        textSkewX = -0.25f
                    }
                }

                val textBounds = android.graphics.Rect()
                paint.getTextBounds(text, 0, text.length, textBounds)
                val textY = height / 2 + (textBounds.height() / 2)

                canvas.nativeCanvas.drawText(
                    text,
                    width / 2,
                    textY,
                    paint
                )
            }

            // Обводка
            if (strokeWidth > 0 && strokeColor != Color.Transparent) {
                drawRect(
                    color = strokeColor,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(width, height),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
            }
        }
    }

    /**
     * Отрисовывает маркер выделения для фигуры
     */
    fun DrawScope.drawShapeSelectionMarker(
        width: Float,
        height: Float
    ) {
        drawRect(
            color = Color.Blue.copy(alpha = 0.3f),
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(width, height),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Ручки изменения размера
        val handleSize = 8f
        val handles = listOf(
            Offset(0f, 0f),
            Offset(width, 0f),
            Offset(0f, height),
            Offset(width, height)
        )

        handles.forEach { handle ->
            drawCircle(
                color = Color.Blue,
                radius = handleSize,
                center = handle
            )
        }

        // Маркер ресайза
        drawCircle(
            color = Color(0xFF2196F3),
            radius = 12f,
            center = Offset(width, height)
        )
    }

    /**
     * Отрисовывает маркер выделения для линии
     */
    fun DrawScope.drawLineShapeSelectionMarker(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float
    ) {
        drawLine(
            color = Color.Blue.copy(alpha = 0.3f),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 6f
        )

        // Ручки на концах линии
        val handleSize = 8f
        drawCircle(
            color = Color.Blue,
            radius = handleSize,
            center = Offset(startX, startY)
        )
        drawCircle(
            color = Color.Blue,
            radius = handleSize,
            center = Offset(endX, endY)
        )

        // Маркер ресайза на конце
        drawCircle(
            color = Color(0xFF2196F3),
            radius = 12f,
            center = Offset(endX, endY)
        )
    }

    /**
     * Отрисовывает маркер выделения для эллипса, ромба и текста
     */
    fun DrawScope.drawBasicShapeSelectionMarker(
        width: Float,
        height: Float
    ) {
        drawRect(
            color = Color.Blue.copy(alpha = 0.3f),
            topLeft = Offset.Zero,
            size = androidx.compose.ui.geometry.Size(width, height),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )

        // Маркер ресайза
        drawCircle(
            color = Color(0xFF2196F3),
            radius = 12f,
            center = Offset(width, height)
        )
    }
}