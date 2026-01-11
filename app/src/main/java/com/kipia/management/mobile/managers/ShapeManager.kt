package com.kipia.management.mobile.managers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape

class ShapeManager {
    private val shapes = mutableListOf<ComposeShape>()
    private var selectedShape: ComposeShape? = null

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

    fun clearSelection() {
        selectedShape = null
        shapes.forEach { it.isSelected = false }
    }

    fun getSelectedShape(): ComposeShape? = selectedShape

    fun getAllShapes(): List<ComposeShape> = shapes

    fun getMutableShapes(): MutableList<ComposeShape> = shapes

    fun moveSelectedShape(deltaX: Float, deltaY: Float) {
        selectedShape?.let { shape ->
            shape.x += deltaX
            shape.y += deltaY
        }
    }

    fun resizeSelectedShape(newWidth: Float, newHeight: Float) {
        selectedShape?.let { shape ->
            shape.width = newWidth
            shape.height = newHeight
        }
    }

    fun rotateSelectedShape(degrees: Float) {
        selectedShape?.let { shape ->
            shape.rotation = (shape.rotation + degrees) % 360
        }
    }

    fun changeFillColor(color: Color) {
        selectedShape?.let { shape ->
            shape.fillColor = color
        }
    }

    fun changeStrokeColor(color: Color) {
        selectedShape?.let { shape ->
            shape.strokeColor = color
        }
    }

    fun changeStrokeWidth(width: Float) {
        selectedShape?.let { shape ->
            shape.strokeWidth = width
        }
    }

    /**
     * Восстанавливает фигуру на определенной позиции (для undo)
     */
    fun restoreShape(shape: ComposeShape, index: Int) {
        if (index in 0..shapes.size) {
            shapes.add(index, shape)
        } else {
            shapes.add(shape)
        }
    }
}