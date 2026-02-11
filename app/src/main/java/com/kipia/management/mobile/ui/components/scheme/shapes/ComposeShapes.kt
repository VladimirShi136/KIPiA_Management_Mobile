package com.kipia.management.mobile.ui.components.scheme.shapes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import android.graphics.Paint as AndroidPaint
import kotlin.math.abs
import kotlin.math.sqrt

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
    var zIndex: Int  // Добавлено для слоев

    fun draw(drawScope: DrawScope)
    fun contains(point: Offset): Boolean
    fun copy(): ComposeShape
    fun copyWithId(): ComposeShape  // Для создания дубликатов с новым ID
}

// Прямоугольник
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
    override var zIndex: Int = 0,
    var cornerRadius: Float = 0f
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
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius),
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
        }
    }

    override fun contains(point: Offset): Boolean {
        // Упрощенная проверка без учета поворота
        return point.x in x..(x + width) && point.y in y..(y + height)
    }

    override fun copy(): ComposeRectangle =
        this.copy(id = this.id)  // Сохраняем ID для обновлений

    override fun copyWithId(): ComposeRectangle =
        this.copy(id = "rect_${System.currentTimeMillis()}")  // Новый ID для дублирования
}

// Линия
data class ComposeLine(
    override val id: String = "line_${System.currentTimeMillis()}",
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var width: Float = 100f,
    override var height: Float = 20f,
    override var rotation: Float = 0f,
    override var fillColor: Color = Color.Transparent,
    override var strokeColor: Color = Color.Black,
    override var strokeWidth: Float = 2f,
    override var isSelected: Boolean = false,
    override var zIndex: Int = 0,
    var startX: Float = 0f,
    var startY: Float = 0f,
    var endX: Float = 100f,
    var endY: Float = 0f
) : ComposeShape {

    override fun draw(drawScope: DrawScope) {
        drawScope.withTransform({
            translate(x, y)
            rotate(rotation, Offset(width / 2, height / 2))
        }) {
            drawLine(
                color = strokeColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
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

                // Маркер ресайза на конце
                drawCircle(
                    color = Color(0xFF2196F3),
                    radius = 12f,
                    center = Offset(endX, endY)
                )
            }
        }
    }

    override fun contains(point: Offset): Boolean {
        val tolerance = strokeWidth + 5f
        val lineStart = Offset(x + startX, y + startY)
        val lineEnd = Offset(x + endX, y + endY)

        val distance = distanceToSegment(point, lineStart, lineEnd)
        return distance <= tolerance
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

    override fun copy(): ComposeLine =
        this.copy(id = this.id)  // Сохраняем ID для обновлений

    override fun copyWithId(): ComposeLine =
        this.copy(id = "line_${System.currentTimeMillis()}")  // Новый ID для дублирования
}

// Эллипс
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
    override var isSelected: Boolean = false,
    override var zIndex: Int = 0
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
    }

    override fun contains(point: Offset): Boolean {
        val centerX = x + width / 2
        val centerY = y + height / 2
        val dx = point.x - centerX
        val dy = point.y - centerY

        return (dx * dx) / (width * width / 4) + (dy * dy) / (height * height / 4) <= 1
    }

    override fun copy(): ComposeEllipse =
        this.copy(id = this.id)  // Сохраняем ID для обновлений

    override fun copyWithId(): ComposeEllipse =
        this.copy(id = "ellipse_${System.currentTimeMillis()}")  // Новый ID для дублирования
}

// Текст
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
    override var zIndex: Int = 0,
    var text: String = "Текст",
    var fontSize: Float = 16f,
    var textColor: Color = Color.Black,
    var isBold: Boolean = false,
    var isItalic: Boolean = false
) : ComposeShape {

    override fun draw(drawScope: DrawScope) {
        drawScope.withTransform({
            translate(x, y)
            rotate(rotation, Offset(width / 2, height / 2))
        }) {
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

            if (isSelected) {
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
    }

    override fun contains(point: Offset): Boolean {
        return point.x in x..(x + width) && point.y in y..(y + height)
    }

    override fun copy(): ComposeText =
        this.copy(id = this.id)  // Сохраняем ID для обновлений

    override fun copyWithId(): ComposeText =
        this.copy(id = "text_${System.currentTimeMillis()}")  // Новый ID для дублирования
}

// Ромб
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
    override var isSelected: Boolean = false,
    override var zIndex: Int = 0
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
                    color = Color.Blue.copy(alpha = 0.3f),
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
    }

    override fun contains(point: Offset): Boolean {
        val localX = point.x - x - width / 2
        val localY = point.y - y - height / 2

        val a = width / 2
        val b = height / 2

        return (abs(localX) / a + abs(localY) / b) <= 1
    }

    override fun copy(): ComposeRhombus =
        this.copy(id = this.id)  // Сохраняем ID для обновлений

    override fun copyWithId(): ComposeRhombus =
        this.copy(id = "rhombus_${System.currentTimeMillis()}")  // Новый ID для дублирования
}

// Фабрика фигур
object ComposeShapeFactory {
    fun createRectangle(): ComposeRectangle {
        return ComposeRectangle(
            fillColor = Color.Transparent,
            strokeColor = Color.Black,
            strokeWidth = 2f,
            zIndex = 0
        )
    }

    fun createLine(): ComposeLine {
        return ComposeLine(
            strokeColor = Color.Black,
            strokeWidth = 2f,
            startX = 0f,
            startY = 0f,
            endX = 100f,
            endY = 0f,
            width = 110f,
            height = 20f,
            zIndex = 0
        )
    }

    fun createEllipse(): ComposeEllipse {
        return ComposeEllipse(
            fillColor = Color.Transparent,
            strokeColor = Color.Black,
            strokeWidth = 2f,
            zIndex = 0
        )
    }

    fun createText(): ComposeText {
        return ComposeText(
            text = "Новый текст",
            fillColor = Color.Transparent,
            strokeColor = Color.Black,
            strokeWidth = 1f,
            fontSize = 16f,
            textColor = Color.Black,
            isBold = false,
            isItalic = false,
            zIndex = 0
        )
    }

    fun createRhombus(): ComposeRhombus {
        return ComposeRhombus(
            fillColor = Color.Transparent,
            strokeColor = Color.Black,
            strokeWidth = 2f,
            zIndex = 0
        )
    }

    fun duplicateShape(shape: ComposeShape): ComposeShape {
        return shape.copyWithId().apply {
            x = shape.x + 20f
            y = shape.y + 20f
        }
    }
}