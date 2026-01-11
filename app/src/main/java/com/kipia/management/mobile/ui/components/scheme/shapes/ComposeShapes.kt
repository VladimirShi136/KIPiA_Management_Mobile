package com.kipia.management.mobile.ui.components.scheme.shapes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint as AndroidPaint

// Базовый интерфейс фигуры
interface ComposeShape {
    val id: String
    var x: Float
    var y: Float
    var width: Float
    var height: Float
    var rotation: Float
    var fillColor: Color
    var strokeColor: Color
    var strokeWidth: Float
    var isSelected: Boolean

    fun draw(drawScope: DrawScope)
    fun contains(point: Offset): Boolean
    fun copy(): ComposeShape
}

// Прямоугольник - ИЗМЕНИТЬ val на var
data class ComposeRectangle(
    override val id: String = "rect_${System.currentTimeMillis()}",
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var width: Float = 100f,
    override var height: Float = 60f,
    override var rotation: Float = 0f,
    override var fillColor: Color = Color.Transparent,
    override var strokeColor: Color = Color.Black,
    override var strokeWidth: Float = 2f,
    override var isSelected: Boolean = false,
    var cornerRadius: Float = 0f  // ← ИЗМЕНИТЬ val на var
) : ComposeShape {

    override fun draw(drawScope: DrawScope) {
        drawScope.withTransform({
            translate(x, y)
            rotate(rotation, Offset(width / 2, height / 2))
        }) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset.Zero,
                size = androidx.compose.ui.geometry.Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
            )

            drawRoundRect(
                color = strokeColor,
                topLeft = Offset.Zero,
                size = androidx.compose.ui.geometry.Size(width, height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
            )

            if (isSelected) {
                drawRoundRect(
                    color = Color.Blue.copy(alpha = 0.3f),
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(width, height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                )

                // Ручки изменения размера
                val handleSize = 8f
                val handles = listOf(
                    Offset(0f, 0f), // top-left
                    Offset(width, 0f), // top-right
                    Offset(0f, height), // bottom-left
                    Offset(width, height) // bottom-right
                )

                handles.forEach { handle ->
                    drawCircle(
                        color = Color.Blue,
                        radius = handleSize,
                        center = handle
                    )
                }
            }
        }
    }

    override fun contains(point: Offset): Boolean {
        return point.x in x..(x + width) && point.y in y..(y + height)
    }

    override fun copy(): ComposeShape = this.copy(id = "rect_${System.currentTimeMillis()}")
}

// Линия - ИЗМЕНИТЬ val на var
data class ComposeLine(
    override val id: String = "line_${System.currentTimeMillis()}",
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var width: Float = 100f,
    override var height: Float = 2f,
    override var rotation: Float = 0f,
    override var fillColor: Color = Color.Transparent,
    override var strokeColor: Color = Color.Black,
    override var strokeWidth: Float = 2f,
    override var isSelected: Boolean = false,
    var startX: Float = 0f,      // ← ИЗМЕНИТЬ val на var
    var startY: Float = 0f,      // ← ИЗМЕНИТЬ val на var
    var endX: Float = 100f,      // ← ИЗМЕНИТЬ val на var
    var endY: Float = 0f         // ← ИЗМЕНИТЬ val на var
) : ComposeShape {

    override fun draw(drawScope: DrawScope) {
        drawScope.withTransform({
            translate(x, y)
        }) {
            drawLine(
                color = strokeColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth
            )

            if (isSelected) {
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
            }
        }
    }

    override fun contains(point: Offset): Boolean {
        val tolerance = 5f
        val lineStart = Offset(x + startX, y + startY)
        val lineEnd = Offset(x + endX, y + endY)

        val distance = distanceToSegment(point, lineStart, lineEnd)
        return distance <= tolerance
    }

    private fun distanceToSegment(p: Offset, v: Offset, w: Offset): Float {
        val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
        if (l2 == 0f) return (p - v).getDistance()

        var t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2
        t = t.coerceIn(0f, 1f)

        val projection = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
        return (p - projection).getDistance()
    }

    override fun copy(): ComposeShape = this.copy(id = "line_${System.currentTimeMillis()}")
}

// Эллипс - ВСЕ var, ошибок нет
data class ComposeEllipse(
    override val id: String = "ellipse_${System.currentTimeMillis()}",
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var width: Float = 80f,
    override var height: Float = 50f,
    override var rotation: Float = 0f,
    override var fillColor: Color = Color.Transparent,
    override var strokeColor: Color = Color.Black,
    override var strokeWidth: Float = 2f,
    override var isSelected: Boolean = false
) : ComposeShape {

    override fun draw(drawScope: DrawScope) {
        drawScope.withTransform({
            translate(x, y)
            rotate(rotation, Offset(width / 2, height / 2))
        }) {
            drawOval(
                color = fillColor,
                topLeft = Offset.Zero,
                size = androidx.compose.ui.geometry.Size(width, height)
            )

            drawOval(
                color = strokeColor,
                topLeft = Offset.Zero,
                size = androidx.compose.ui.geometry.Size(width, height),
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
            )

            if (isSelected) {
                drawOval(
                    color = Color.Blue.copy(alpha = 0.3f),
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(width, height)
                )
            }
        }
    }

    override fun contains(point: Offset): Boolean {
        val centerX = x + width / 2
        val centerY = y + height / 2
        val dx = point.x - centerX
        val dy = point.y - centerY

        return (dx * dx) / (width * width / 4) + (dy * dy) / (height * height / 4) <= 1
    }

    override fun copy(): ComposeShape = this.copy(id = "ellipse_${System.currentTimeMillis()}")
}

// Текст - ИЗМЕНИТЬ val на var
data class ComposeText(
    override val id: String = "text_${System.currentTimeMillis()}",
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var width: Float = 100f,
    override var height: Float = 30f,
    override var rotation: Float = 0f,
    override var fillColor: Color = Color.Transparent,
    override var strokeColor: Color = Color.Black,
    override var strokeWidth: Float = 1f,
    override var isSelected: Boolean = false,
    var text: String = "Текст",     // ← ИЗМЕНИТЬ val на var
    var fontSize: Float = 16f       // ← ИЗМЕНИТЬ val на var
) : ComposeShape {

    override fun draw(drawScope: DrawScope) {
        drawScope.withTransform({
            translate(x, y)
            rotate(rotation, Offset(width / 2, height / 2))
        }) {
            // Фон текста (если нужно)
            if (fillColor != Color.Transparent) {
                drawRect(
                    color = fillColor,
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(width, height)
                )
            }

            // Отрисовка текста через native canvas
            drawContext.canvas.nativeCanvas.drawText(
                text,
                0f, // X относительно текущей системы координат
                fontSize, // Y относительно текущей системы координат
                AndroidPaint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = fontSize
                    isAntiAlias = true
                }
            )

            if (isSelected) {
                drawRect(
                    color = Color.Blue.copy(alpha = 0.3f),
                    topLeft = Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(width, height)
                )
            }
        }
    }

    override fun contains(point: Offset): Boolean {
        return point.x in x..(x + width) && point.y in y..(y + height)
    }

    override fun copy(): ComposeShape = this.copy(id = "text_${System.currentTimeMillis()}")
}

// Ромб - ВСЕ var, ошибок нет
data class ComposeRhombus(
    override val id: String = "rhombus_${System.currentTimeMillis()}",
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var width: Float = 80f,
    override var height: Float = 60f,
    override var rotation: Float = 0f,
    override var fillColor: Color = Color.Transparent,
    override var strokeColor: Color = Color.Black,
    override var strokeWidth: Float = 2f,
    override var isSelected: Boolean = false
) : ComposeShape {

    override fun draw(drawScope: DrawScope) {
        drawScope.withTransform({
            translate(x, y)
            rotate(rotation, Offset(width / 2, height / 2))
        }) {
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

            drawPath(
                path = path,
                color = strokeColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth)
            )

            if (isSelected) {
                drawPath(
                    path = path,
                    color = Color.Blue.copy(alpha = 0.3f)
                )
            }
        }
    }

    override fun contains(point: Offset): Boolean {
        val localX = point.x - x - width / 2
        val localY = point.y - y - height / 2

        val a = width / 2
        val b = height / 2

        return (kotlin.math.abs(localX) / a + kotlin.math.abs(localY) / b) <= 1
    }

    override fun copy(): ComposeShape = this.copy(id = "rhombus_${System.currentTimeMillis()}")
}

// Фабрика фигур
object ComposeShapeFactory {
    fun createRectangle(): ComposeRectangle {
        return ComposeRectangle(
            fillColor = Color.Transparent,
            strokeColor = Color.Black,
            strokeWidth = 2f
        )
    }

    fun createLine(): ComposeLine {
        return ComposeLine(
            strokeColor = Color.Black,
            strokeWidth = 2f
        )
    }

    fun createEllipse(): ComposeEllipse {
        return ComposeEllipse(
            fillColor = Color.Transparent,
            strokeColor = Color.Black,
            strokeWidth = 2f
        )
    }

    fun createText(): ComposeText {
        return ComposeText(
            text = "Новый текст",
            fillColor = Color.Transparent,
            strokeColor = Color.Black
        )
    }

    fun createRhombus(): ComposeRhombus {
        return ComposeRhombus(
            fillColor = Color.Transparent,
            strokeColor = Color.Black,
            strokeWidth = 2f
        )
    }
}