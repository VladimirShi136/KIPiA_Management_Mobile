package com.kipia.management.mobile.ui.components.scheme.shapes

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.isPointInButterfly
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.isPointInEllipse
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.isPointInLine
import com.kipia.management.mobile.ui.components.scheme.utils.ShapeUtils.isPointInRectangle
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

    /**
     * ВНИМАНИЕ: Этот метод больше не используется для отрисовки!
     * Отрисовка происходит в ShapeLayer через drawShapeWithGlobalTransform.
     * Метод оставлен для обратной совместимости и может быть пустым.
     */
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

    // Метод больше не используется для отрисовки
    override fun draw(drawScope: DrawScope, isSelected: Boolean) {
        // Оставляем пустым или можно добавить комментарий
    }

    override fun contains(point: Offset): Boolean {
        val localPoint = transformPointToShapeSpace(point, x, y, width, height, rotation)
        return isPointInRectangle(localPoint, width, height)
    }

    override fun copy(): ComposeRectangle = ComposeRectangle(
        id = this.id,
        x = this.x,
        y = this.y,
        width = this.width,
        height = this.height,
        rotation = this.rotation,
        fillColor = this.fillColor,
        strokeColor = this.strokeColor,
        strokeWidth = this.strokeWidth,
        cornerRadius = this.cornerRadius
    )

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
    override var rotation: Float = 0f,  // Угол поворота всей фигуры
    override var fillColor: Color = Color.Transparent,
    override var strokeColor: Color = Color.Black,
    override var strokeWidth: Float = 2f,
    var startX: Float = 0f,
    var startY: Float = 0f,
    var endX: Float = 100f,
    var endY: Float = 0f
) : ComposeShape {

    override fun draw(drawScope: DrawScope, isSelected: Boolean) {
        // Не используется - отрисовка через ShapeLayer
    }

    override fun contains(point: Offset): Boolean {
        val localPoint = transformPointToShapeSpace(
            point = point,
            shapeX = x,
            shapeY = y,
            shapeWidth = width,
            shapeHeight = height,
            rotation = rotation  // Учитываем поворот
        )

        return isPointInLine(
            point = localPoint,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = strokeWidth
        )
    }

    override fun copy(): ComposeLine = ComposeLine(
        id = this.id,
        x = this.x,
        y = this.y,
        width = this.width,
        height = this.height,
        rotation = this.rotation,
        fillColor = this.fillColor,
        strokeColor = this.strokeColor,
        strokeWidth = this.strokeWidth,
        startX = this.startX,
        startY = this.startY,
        endX = this.endX,
        endY = this.endY
    )

    override fun copyWithId(): ComposeLine = this.copy(id = "line_${System.currentTimeMillis()}")
    override fun copyWithPosition(x: Float, y: Float): ComposeLine = this.copy(x = x, y = y)
    override fun copyWithFillColor(color: Color): ComposeLine = this.copy(fillColor = color)
    override fun copyWithStrokeColor(color: Color): ComposeLine = this.copy(strokeColor = color)
    override fun copyWithStrokeWidth(width: Float): ComposeLine = this.copy(strokeWidth = width)
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
        // Оставляем пустым
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

    override fun copy(): ComposeEllipse = ComposeEllipse(
        id = this.id,
        x = this.x,
        y = this.y,
        width = this.width,
        height = this.height,
        rotation = this.rotation,
        fillColor = this.fillColor,
        strokeColor = this.strokeColor,
        strokeWidth = this.strokeWidth
    )

    override fun copyWithId(): ComposeEllipse = this.copy(id = "ellipse_${System.currentTimeMillis()}")
    override fun copyWithPosition(x: Float, y: Float): ComposeEllipse = this.copy(x = x, y = y)
    override fun copyWithFillColor(color: Color): ComposeEllipse = this.copy(fillColor = color)
    override fun copyWithStrokeColor(color: Color): ComposeEllipse = this.copy(strokeColor = color)
    override fun copyWithStrokeWidth(width: Float): ComposeEllipse = this.copy(strokeWidth = width)
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
        // Оставляем пустым
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

    override fun copy(): ComposeText = ComposeText(
        id = this.id,
        x = this.x,
        y = this.y,
        width = this.width,
        height = this.height,
        rotation = this.rotation,
        fillColor = this.fillColor,
        strokeColor = this.strokeColor,  // Это поле должно быть!
        strokeWidth = this.strokeWidth,
        text = this.text,
        fontSize = this.fontSize,
        textColor = this.textColor,
        isBold = this.isBold,
        isItalic = this.isItalic
    )

    override fun copyWithId(): ComposeText = this.copy(id = "text_${System.currentTimeMillis()}")
    override fun copyWithPosition(x: Float, y: Float): ComposeText = this.copy(x = x, y = y)
    override fun copyWithFillColor(color: Color): ComposeText = this.copy(fillColor = color)
    override fun copyWithStrokeColor(color: Color): ComposeText = this.copy(
        strokeColor = color  // Убедитесь, что это правильно!
    )
    override fun copyWithStrokeWidth(width: Float): ComposeText = this.copy(strokeWidth = width)
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
        // Не используется - отрисовка через ShapeLayer
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

        // Проверка попадания в фигуру "песочные часы"
        return isPointInButterfly(localPoint, width, height)
    }

    override fun copy(): ComposeRhombus = ComposeRhombus(
        id = this.id,
        x = this.x,
        y = this.y,
        width = this.width,
        height = this.height,
        rotation = this.rotation,
        fillColor = this.fillColor,
        strokeColor = this.strokeColor,
        strokeWidth = this.strokeWidth
    )

    override fun copyWithId(): ComposeRhombus = this.copy(id = "rhombus_${System.currentTimeMillis()}")

    override fun copyWithPosition(x: Float, y: Float): ComposeRhombus = this.copy(x = x, y = y)

    override fun copyWithFillColor(color: Color): ComposeRhombus = this.copy(fillColor = color)

    override fun copyWithStrokeColor(color: Color): ComposeRhombus = this.copy(strokeColor = color)

    override fun copyWithStrokeWidth(width: Float): ComposeRhombus = this.copy(strokeWidth = width)
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
            strokeWidth = 2f,
            startX = 0f,
            startY = 0f,
            endX = 100f,
            endY = 0f,
            width = 100f,  // Установите width = endX, если линия должна занимать всю ширину
            height = 20f
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
            text = "",
            fillColor = Color.Transparent,
            strokeColor = Color.Black,
            strokeWidth = 1f,
            fontSize = 16f,
            textColor = Color.Black,
            isBold = false,
            isItalic = false,
            width = 50f,  // Минимальная ширина
            height = 24f  // Минимальная высота
        )
    }

    fun createRhombus(): ComposeRhombus {
        return ComposeRhombus(
            fillColor = Color.Transparent,
            strokeColor = Color.Black,
            strokeWidth = 2f
        )
    }

    fun duplicateShape(shape: ComposeShape): ComposeShape {
        return shape.copyWithId().apply {
            x = shape.x + 20f
            y = shape.y + 20f
        }
    }
}