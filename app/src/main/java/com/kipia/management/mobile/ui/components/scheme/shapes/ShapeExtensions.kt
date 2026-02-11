package com.kipia.management.mobile.ui.components.scheme.shapes

import androidx.compose.ui.graphics.Color

/**
 * Функции-расширения для безопасного копирования фигур с изменением свойств
 */

fun ComposeShape.copyWithFillColor(color: Color): ComposeShape {
    return when (this) {
        is ComposeRectangle -> this.copy(fillColor = color)
        is ComposeLine -> this.copy(fillColor = color)
        is ComposeEllipse -> this.copy(fillColor = color)
        is ComposeText -> this.copy(fillColor = color)
        is ComposeRhombus -> this.copy(fillColor = color)
        else -> this
    }
}

fun ComposeShape.copyWithStrokeColor(color: Color): ComposeShape {
    return when (this) {
        is ComposeRectangle -> this.copy(strokeColor = color)
        is ComposeLine -> this.copy(strokeColor = color)
        is ComposeEllipse -> this.copy(strokeColor = color)
        is ComposeText -> this.copy(strokeColor = color)
        is ComposeRhombus -> this.copy(strokeColor = color)
        else -> this
    }
}

fun ComposeShape.copyWithStrokeWidth(width: Float): ComposeShape {
    return when (this) {
        is ComposeRectangle -> this.copy(strokeWidth = width)
        is ComposeLine -> this.copy(strokeWidth = width)
        is ComposeEllipse -> this.copy(strokeWidth = width)
        is ComposeText -> this.copy(strokeWidth = width)
        is ComposeRhombus -> this.copy(strokeWidth = width)
        else -> this
    }
}

fun ComposeShape.copyWithPosition(x: Float, y: Float): ComposeShape {
    return when (this) {
        is ComposeRectangle -> this.copy(x = x, y = y)
        is ComposeLine -> this.copy(x = x, y = y)
        is ComposeEllipse -> this.copy(x = x, y = y)
        is ComposeText -> this.copy(x = x, y = y)
        is ComposeRhombus -> this.copy(x = x, y = y)
        else -> this
    }
}

fun ComposeShape.copyWithRotation(rotation: Float): ComposeShape {
    return when (this) {
        is ComposeRectangle -> this.copy(rotation = rotation)
        is ComposeLine -> this.copy(rotation = rotation)
        is ComposeEllipse -> this.copy(rotation = rotation)
        is ComposeText -> this.copy(rotation = rotation)
        is ComposeRhombus -> this.copy(rotation = rotation)
        else -> this
    }
}

fun ComposeShape.copyWithSize(width: Float, height: Float): ComposeShape {
    return when (this) {
        is ComposeRectangle -> this.copy(width = width, height = height)
        is ComposeLine -> {
            // Для линии сохраняем пропорции конечной точки
            val scaleX = width / this.width
            val scaleY = height / this.height
            this.copy(
                width = width,
                height = height,
                endX = this.endX * scaleX,
                endY = this.endY * scaleY
            )
        }
        is ComposeEllipse -> this.copy(width = width, height = height)
        is ComposeText -> this.copy(width = width, height = height)
        is ComposeRhombus -> this.copy(width = width, height = height)
        else -> this
    }
}

fun ComposeShape.copyWithZIndex(zIndex: Int): ComposeShape {
    return when (this) {
        is ComposeRectangle -> this.copy(zIndex = zIndex)
        is ComposeLine -> this.copy(zIndex = zIndex)
        is ComposeEllipse -> this.copy(zIndex = zIndex)
        is ComposeText -> this.copy(zIndex = zIndex)
        is ComposeRhombus -> this.copy(zIndex = zIndex)
        else -> this
    }
}

/**
 * Для ComposeLine дополнительные функции
 */
fun ComposeLine.copyWithEndPoint(endX: Float, endY: Float): ComposeLine {
    return this.copy(
        endX = endX,
        endY = endY,
        width = maxOf(this.startX, endX) + 10f,
        height = maxOf(this.startY, endY) + 10f
    )
}

fun ComposeLine.copyWithStartPoint(startX: Float, startY: Float): ComposeLine {
    return this.copy(
        startX = startX,
        startY = startY,
        width = maxOf(startX, this.endX) + 10f,
        height = maxOf(startY, this.endY) + 10f
    )
}

/**
 * Для ComposeText дополнительные функции
 */
fun ComposeText.copyWithText(text: String): ComposeText {
    val newWidth = text.length * 10f + 30f
    return this.copy(
        text = text,
        width = newWidth
    )
}

fun ComposeText.copyWithFontSize(fontSize: Float): ComposeText {
    return this.copy(fontSize = fontSize)
}

fun ComposeText.copyWithTextColor(color: Color): ComposeText {
    return this.copy(textColor = color)
}

fun ComposeText.copyWithTextStyle(isBold: Boolean, isItalic: Boolean): ComposeText {
    return this.copy(isBold = isBold, isItalic = isItalic)
}

/**
 * Для ComposeRectangle дополнительные функции
 */
fun ComposeRectangle.copyWithCornerRadius(cornerRadius: Float): ComposeRectangle {
    return this.copy(cornerRadius = cornerRadius)
}