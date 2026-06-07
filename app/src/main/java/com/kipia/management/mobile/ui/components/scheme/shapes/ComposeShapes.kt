package com.kipia.management.mobile.ui.components.scheme.shapes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils
import com.kipia.management.mobile.viewmodel.EditorMode
import java.util.UUID

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
    override val id: String = "rect_${UUID.randomUUID()}",
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

    override fun draw(drawScope: DrawScope, isSelected: Boolean) {}

    override fun contains(point: Offset): Boolean {
        val localPoint = ShapeUtils.transformPointToShapeSpace(point, x, y, width, height, rotation)
        return ShapeUtils.isPointInRectangle(localPoint, width, height)
    }

    override fun copy(): ComposeRectangle = copy(id = id)
    override fun copyWithId(): ComposeRectangle = copy(id = "rect_${UUID.randomUUID()}")
    override fun copyWithPosition(x: Float, y: Float): ComposeRectangle = copy(x = x, y = y)
    override fun copyWithFillColor(color: Color): ComposeRectangle = copy(fillColor = color)
    override fun copyWithStrokeColor(color: Color): ComposeRectangle = copy(strokeColor = color)
    override fun copyWithStrokeWidth(width: Float): ComposeRectangle = copy(strokeWidth = width)
}

// Линия
data class ComposeLine(
    override val id: String = "line_${UUID.randomUUID()}",
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

    override fun draw(drawScope: DrawScope, isSelected: Boolean) {}

    override fun contains(point: Offset): Boolean {
        return ShapeUtils.isPointInLine(
            point = point,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth
        )
    }

    override fun copy(): ComposeLine = copy(id = id)
    override fun copyWithId(): ComposeLine = copy(id = "line_${UUID.randomUUID()}")
    override fun copyWithPosition(x: Float, y: Float): ComposeLine {
        val dx = x - this.x
        val dy = y - this.y
        return copy(x = x, y = y, startX = startX + dx, startY = startY + dy, endX = endX + dx, endY = endY + dy)
    }
    override fun copyWithFillColor(color: Color): ComposeLine = copy(fillColor = color)
    override fun copyWithStrokeColor(color: Color): ComposeLine = copy(strokeColor = color)
    override fun copyWithStrokeWidth(width: Float): ComposeLine = copy(strokeWidth = width)
}

// Эллипс
data class ComposeEllipse(
    override val id: String = "ellipse_${UUID.randomUUID()}",
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var width: Float = 80f,
    override var height: Float = 50f,
    override var rotation: Float = 0f,
    override var fillColor: Color = Color.Transparent,
    override var strokeColor: Color = Color.Black,
    override var strokeWidth: Float = 2f
) : ComposeShape {

    override fun draw(drawScope: DrawScope, isSelected: Boolean) {}

    override fun contains(point: Offset): Boolean {
        val localPoint = ShapeUtils.transformPointToShapeSpace(point, x, y, width, height, rotation)
        return ShapeUtils.isPointInEllipse(localPoint, width, height)
    }

    override fun copy(): ComposeEllipse = copy(id = id)
    override fun copyWithId(): ComposeEllipse = copy(id = "ellipse_${UUID.randomUUID()}")
    override fun copyWithPosition(x: Float, y: Float): ComposeEllipse = copy(x = x, y = y)
    override fun copyWithFillColor(color: Color): ComposeEllipse = copy(fillColor = color)
    override fun copyWithStrokeColor(color: Color): ComposeEllipse = copy(strokeColor = color)
    override fun copyWithStrokeWidth(width: Float): ComposeEllipse = copy(strokeWidth = width)
}

// Текст
data class ComposeText(
    override val id: String = "text_${UUID.randomUUID()}",
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
    var fontFamily: String = "Arial",
    var isBold: Boolean = false,
    var isItalic: Boolean = false
) : ComposeShape {

    override fun draw(drawScope: DrawScope, isSelected: Boolean) {}

    override fun contains(point: Offset): Boolean {
        val localPoint = ShapeUtils.transformPointToShapeSpace(point, x, y, width, height, rotation)
        return ShapeUtils.isPointInText(localPoint, width, height)
    }

    override fun copy(): ComposeText = copy(id = id)
    override fun copyWithId(): ComposeText = copy(id = "text_${UUID.randomUUID()}")
    override fun copyWithPosition(x: Float, y: Float): ComposeText = copy(x = x, y = y)
    override fun copyWithFillColor(color: Color): ComposeText = copy(fillColor = color)
    override fun copyWithStrokeColor(color: Color): ComposeText = copy(strokeColor = color)
    override fun copyWithStrokeWidth(width: Float): ComposeText = copy(strokeWidth = width)
}

// Ромб
data class ComposeRhombus(
    override val id: String = "rhombus_${UUID.randomUUID()}",
    override var x: Float = 0f,
    override var y: Float = 0f,
    override var width: Float = 80f,
    override var height: Float = 60f,
    override var rotation: Float = 0f,
    override var fillColor: Color = Color.Transparent,
    override var strokeColor: Color = Color.Black,
    override var strokeWidth: Float = 2f
) : ComposeShape {

    override fun draw(drawScope: DrawScope, isSelected: Boolean) {}

    override fun contains(point: Offset): Boolean {
        val localPoint = ShapeUtils.transformPointToShapeSpace(point, x, y, width, height, rotation)
        return ShapeUtils.isPointInButterfly(localPoint, width, height)
    }

    override fun copy(): ComposeRhombus = copy(id = id)
    override fun copyWithId(): ComposeRhombus = copy(id = "rhombus_${UUID.randomUUID()}")
    override fun copyWithPosition(x: Float, y: Float): ComposeRhombus = copy(x = x, y = y)
    override fun copyWithFillColor(color: Color): ComposeRhombus = copy(fillColor = color)
    override fun copyWithStrokeColor(color: Color): ComposeRhombus = copy(strokeColor = color)
    override fun copyWithStrokeWidth(width: Float): ComposeRhombus = copy(strokeWidth = width)
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
            else -> throw IllegalArgumentException("Unsupported shape type: $shapeType")
        }
    }

    fun createRectangle(): ComposeRectangle = ComposeRectangle()
    fun createLine(): ComposeLine = ComposeLine()
    fun createEllipse(): ComposeEllipse = ComposeEllipse()
    fun createText(): ComposeText = ComposeText()
    fun createRhombus(): ComposeRhombus = ComposeRhombus()
}