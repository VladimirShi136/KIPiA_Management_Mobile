package com.kipia.management.mobile.ui.components.scheme.shapes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.drawBasicShapeSelectionMarker
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.drawEllipse
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.drawLine
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.drawLineShapeSelectionMarker
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.drawRectangle
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.drawRhombus
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.drawText
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.isPointInEllipse
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.isPointInLine
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.isPointInRhombus
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.isPointInText
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.transformPointToShapeSpace
import com.kipia.management.mobile.viewmodel.EditorMode

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

    fun draw(drawScope: DrawScope, isSelected: Boolean)
    fun contains(point: Offset): Boolean

    fun copy(): ComposeShape
    fun copyWithId(): ComposeShape
    fun copyWithPosition(x: Float, y: Float): ComposeShape
    fun copyWithFillColor(color: Color): ComposeShape
    fun copyWithStrokeColor(color: Color): ComposeShape
    fun copyWithStrokeWidth(width: Float): ComposeShape
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
    var cornerRadius: Float = 0f
) : ComposeShape {
    override fun draw(drawScope: DrawScope, isSelected: Boolean) {
        with(drawScope) {
            drawRectangle(
                x = x,
                y = y,
                width = width,
                height = height,
                rotation = rotation,
                fillColor = fillColor,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                cornerRadius = cornerRadius
            )

            if (isSelected) {
                drawBasicShapeSelectionMarker(width, height)
            }
        }
    }

    override fun contains(point: Offset): Boolean {
        val localPoint = transformPointToShapeSpace(point, x, y, width, height, rotation)
        return ShapeUtils.isPointInRectangle(localPoint, width, height)
    }

    override fun copy(): ComposeRectangle = this.copy(id = this.id)
    override fun copyWithId(): ComposeRectangle = this.copy(id = "rect_${System.currentTimeMillis()}")
    override fun copyWithPosition(x: Float, y: Float): ComposeRectangle = this.copy(x = x, y = y)
    override fun copyWithFillColor(color: Color): ComposeRectangle = this.copy(fillColor = color)
    override fun copyWithStrokeColor(color: Color): ComposeRectangle = this.copy(strokeColor = color)
    override fun copyWithStrokeWidth(width: Float): ComposeRectangle = this.copy(strokeWidth = width)
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
    var startX: Float = 0f,
    var startY: Float = 0f,
    var endX: Float = 100f,
    var endY: Float = 0f
) : ComposeShape {
    override fun draw(drawScope: DrawScope, isSelected: Boolean) {
        with(drawScope) {
            // Основная линия
            drawLine(
                x = x,
                y = y,
                width = width,
                height = height,
                rotation = rotation,
                startX = startX,
                startY = startY,
                endX = endX,
                endY = endY,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth
            )

            // Маркер выделения
            if (isSelected) {
                drawLineShapeSelectionMarker(
                    startX = startX,
                    startY = startY,
                    endX = endX,
                    endY = endY
                )
            }
        }
    }

    override fun contains(point: Offset): Boolean {
        val localPoint = transformPointToShapeSpace(
            point = point,
            shapeX = x,
            shapeY = y,
            shapeWidth = width,
            shapeHeight = height,
            rotation = rotation
        )

        // Вычисляем позиции концов линии в локальных координатах
        val lineStartX = startX - width / 2
        val lineStartY = startY - height / 2
        val lineEndX = endX - width / 2
        val lineEndY = endY - height / 2

        return isPointInLine(
            point = localPoint,
            start = Offset(lineStartX, lineStartY),
            end = Offset(lineEndX, lineEndY),
            strokeWidth = strokeWidth
        )
    }

    override fun copy(): ComposeLine =
        this.copy(id = this.id)  // Сохраняем ID для обновлений

    override fun copyWithId(): ComposeLine =
        this.copy(id = "line_${System.currentTimeMillis()}")  // Новый ID для дублирования

    override fun copyWithPosition(
        x: Float,
        y: Float
    ): ComposeLine = this.copy(x = x, y = y)

    // Реализация новых методов
    override fun copyWithFillColor(color: Color): ComposeLine =
        this.copy(fillColor = color)

    override fun copyWithStrokeColor(color: Color): ComposeLine =
        this.copy(strokeColor = color)

    override fun copyWithStrokeWidth(width: Float): ComposeLine =
        this.copy(strokeWidth = width)
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
    override var strokeWidth: Float = 2f
) : ComposeShape {
    override fun draw(drawScope: DrawScope, isSelected: Boolean) {
        with(drawScope) {
            // Основная фигура
            drawEllipse(
                x = x,
                y = y,
                width = width,
                height = height,
                rotation = rotation,
                fillColor = fillColor,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth
            )

            // Маркер выделения
            if (isSelected) {
                drawBasicShapeSelectionMarker(
                    width = width,
                    height = height
                )
            }
        }
    }

    override fun contains(point: Offset): Boolean {
        val localPoint = transformPointToShapeSpace(
            point = point,
            shapeX = x,
            shapeY = y,
            shapeWidth = width,
            shapeHeight = height,
            rotation = rotation
        )
        return isPointInEllipse(localPoint, width, height)
    }

    override fun copy(): ComposeEllipse =
        this.copy(id = this.id)  // Сохраняем ID для обновлений

    override fun copyWithId(): ComposeEllipse =
        this.copy(id = "ellipse_${System.currentTimeMillis()}")  // Новый ID для дублирования

    override fun copyWithPosition(
        x: Float,
        y: Float
    ): ComposeEllipse = this.copy(x = x, y = y)

    // Реализация новых методов
    override fun copyWithFillColor(color: Color): ComposeEllipse =
        this.copy(fillColor = color)

    override fun copyWithStrokeColor(color: Color): ComposeEllipse =
        this.copy(strokeColor = color)

    override fun copyWithStrokeWidth(width: Float): ComposeEllipse =
        this.copy(strokeWidth = width)
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
    var text: String = "Текст",
    var fontSize: Float = 16f,
    var textColor: Color = Color.Black,
    var isBold: Boolean = false,
    var isItalic: Boolean = false
) : ComposeShape {
    override fun draw(drawScope: DrawScope, isSelected: Boolean) {
        with(drawScope) {
            // Основной текст
            drawText(
                x = x,
                y = y,
                width = width,
                height = height,
                rotation = rotation,
                text = text,
                fontSize = fontSize,
                textColor = textColor,
                fillColor = fillColor,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth,
                isBold = isBold,
                isItalic = isItalic
            )

            // Маркер выделения
            if (isSelected) {
                drawBasicShapeSelectionMarker(
                    width = width,
                    height = height
                )
            }
        }
    }

    override fun contains(point: Offset): Boolean {
        val localPoint = transformPointToShapeSpace(
            point = point,
            shapeX = x,
            shapeY = y,
            shapeWidth = width,
            shapeHeight = height,
            rotation = rotation
        )
        return isPointInText(localPoint, width, height)
    }

    override fun copy(): ComposeText =
        this.copy(id = this.id)  // Сохраняем ID для обновлений

    override fun copyWithId(): ComposeText =
        this.copy(id = "text_${System.currentTimeMillis()}")  // Новый ID для дублирования

    override fun copyWithPosition(
        x: Float,
        y: Float
    ): ComposeText = this.copy(x = x, y = y)

    // Реализация новых методов
    override fun copyWithFillColor(color: Color): ComposeText =
        this.copy(fillColor = color)

    override fun copyWithStrokeColor(color: Color): ComposeText =
        this.copy(strokeColor = color)

    override fun copyWithStrokeWidth(width: Float): ComposeText =
        this.copy(strokeWidth = width)
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
    override var strokeWidth: Float = 2f
) : ComposeShape {
    override fun draw(drawScope: DrawScope, isSelected: Boolean) {
        with(drawScope) {
            // Основная фигура
            drawRhombus(
                x = x,
                y = y,
                width = width,
                height = height,
                rotation = rotation,
                fillColor = fillColor,
                strokeColor = strokeColor,
                strokeWidth = strokeWidth
            )

            // Маркер выделения
            if (isSelected) {
                drawBasicShapeSelectionMarker(
                    width = width,
                    height = height
                )
            }
        }
    }

    override fun contains(point: Offset): Boolean {
        val localPoint = transformPointToShapeSpace(
            point = point,
            shapeX = x,
            shapeY = y,
            shapeWidth = width,
            shapeHeight = height,
            rotation = rotation
        )
        return isPointInRhombus(localPoint, width, height)
    }

    override fun copy(): ComposeRhombus =
        this.copy(id = this.id)  // Сохраняем ID для обновлений

    override fun copyWithId(): ComposeRhombus =
        this.copy(id = "rhombus_${System.currentTimeMillis()}")  // Новый ID для дублирования

    override fun copyWithPosition(
        x: Float,
        y: Float
    ): ComposeRhombus = this.copy(x = x, y = y)

    // Реализация новых методов
    override fun copyWithFillColor(color: Color): ComposeRhombus =
        this.copy(fillColor = color)

    override fun copyWithStrokeColor(color: Color): ComposeRhombus =
        this.copy(strokeColor = color)

    override fun copyWithStrokeWidth(width: Float): ComposeRhombus =
        this.copy(strokeWidth = width)
}

// Фабрика фигур
object ComposeShapeFactory {

    fun create(shapeType: EditorMode): ComposeShape {
        return when (shapeType) {
            EditorMode.RECTANGLE -> createRectangle()
            EditorMode.LINE -> createLine()
            EditorMode.ELLIPSE -> createEllipse()
            EditorMode.TEXT -> createText()
            EditorMode.RHOMBUS -> createRhombus()
            else -> throw IllegalArgumentException("Unsupported shape type")
        }
    }

    fun createRectangle(): ComposeRectangle {
        return ComposeRectangle(
            fillColor = Color.Transparent,
            strokeColor = Color.Black,
            strokeWidth = 2f,
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
        )
    }

    fun createEllipse(): ComposeEllipse {
        return ComposeEllipse(
            fillColor = Color.Transparent,
            strokeColor = Color.Black,
            strokeWidth = 2f,
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
        )
    }

    fun createRhombus(): ComposeRhombus {
        return ComposeRhombus(
            fillColor = Color.Transparent,
            strokeColor = Color.Black,
            strokeWidth = 2f,
        )
    }

    fun duplicateShape(shape: ComposeShape): ComposeShape {
        return shape.copyWithId().apply {
            x = shape.x + 20f
            y = shape.y + 20f
        }
    }
}