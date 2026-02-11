package com.kipia.management.mobile.managers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape

class ShapeManager {
    private val shapes = mutableListOf<ComposeShape>()
    private var selectedShape: ComposeShape? = null

    // Флаг для предотвращения циклических обновлений
    var isUpdating = false
        private set

    fun addShape(shape: ComposeShape) {
        shapes.add(shape)
    }

    fun removeShape(shape: ComposeShape) {
        shapes.remove(shape)
        if (selectedShape == shape) {
            selectedShape = null
        }
    }

    fun clearShapes() {
        shapes.clear()
        selectedShape = null
    }

    fun selectShapeAt(point: Offset): ComposeShape? {
        // Ищем фигуру с наивысшим z-index (последняя в списке)
        val shape = shapes.reversed().firstOrNull { it.contains(point) }
        selectedShape = shape
        shapes.forEach { it.isSelected = (it == shape) }
        return shape
    }

    fun selectShape(shape: ComposeShape?) {
        selectedShape = shape
        shapes.forEach { it.isSelected = (it == shape) }
    }

    fun clearSelection() {
        selectedShape = null
        shapes.forEach { it.isSelected = false }
    }

    fun getSelectedShape(): ComposeShape? = selectedShape

    fun getAllShapes(): List<ComposeShape> = shapes.toList() // Возвращаем копию

    fun getMutableShapes(): MutableList<ComposeShape> = shapes

    fun moveShape(shape: ComposeShape, deltaX: Float, deltaY: Float) {
        shape.x += deltaX
        shape.y += deltaY
    }

    fun moveShapeTo(shape: ComposeShape, newX: Float, newY: Float) {
        shape.x = newX
        shape.y = newY
    }

    fun resizeShape(shape: ComposeShape, newWidth: Float, newHeight: Float) {
        when (shape) {
            is com.kipia.management.mobile.ui.components.scheme.shapes.ComposeLine -> {
                // Для линии изменяем конечную точку
                shape.endX = newWidth
                shape.endY = newHeight
                shape.width = maxOf(shape.startX, newWidth) + 10f
                shape.height = maxOf(shape.startY, newHeight) + 10f
            }
            else -> {
                shape.width = newWidth
                shape.height = newHeight
            }
        }
    }

    fun rotateShape(shape: ComposeShape, degrees: Float) {
        shape.rotation = (shape.rotation + degrees) % 360
    }

    fun rotateShapeTo(shape: ComposeShape, degrees: Float) {
        shape.rotation = degrees % 360
    }

    fun changeFillColor(shape: ComposeShape, color: Color) {
        shape.fillColor = color
    }

    fun changeStrokeColor(shape: ComposeShape, color: Color) {
        shape.strokeColor = color
    }

    fun changeStrokeWidth(shape: ComposeShape, width: Float) {
        shape.strokeWidth = width
    }

    fun getShapeIndex(shape: ComposeShape): Int {
        return shapes.indexOf(shape)
    }

    fun restoreShape(shape: ComposeShape, index: Int) {
        if (index in 0..shapes.size) {
            shapes.add(index, shape)
        } else {
            shapes.add(shape)
        }
    }

    fun bringToFront(shape: ComposeShape) {
        if (shapes.remove(shape)) {
            shapes.add(shape)
        }
    }

    fun sendToBack(shape: ComposeShape) {
        if (shapes.remove(shape)) {
            shapes.add(0, shape)
        }
    }

    fun updateShape(oldShape: ComposeShape, newShape: ComposeShape) {
        val index = shapes.indexOf(oldShape)
        if (index != -1) {
            shapes[index] = newShape
            if (selectedShape == oldShape) {
                selectedShape = newShape
            }
        }
    }
}