package com.kipia.management.mobile.managers

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ShapeManager {
    private val _shapes = MutableStateFlow<List<ComposeShape>>(emptyList())
    val shapes = _shapes.asStateFlow()

    fun addShape(shape: ComposeShape) {
        _shapes.update { it + shape }
    }

    fun removeShape(shapeId: String) {
        _shapes.update { it.filter { shape -> shape.id != shapeId } }
    }

    fun updateShape(shapeId: String, update: (ComposeShape) -> ComposeShape) {
        _shapes.update { shapes ->
            shapes.map { shape ->
                if (shape.id == shapeId) update(shape) else shape
            }
        }
    }

    fun moveShape(shapeId: String, delta: Offset) {
        updateShape(shapeId) { shape ->
            shape.copyWithPosition(
                x = shape.x + delta.x,
                y = shape.y + delta.y
            )
        }
    }

    fun updateStrokeColor(shapeId: String, color: Color) {
        updateShape(shapeId) { it.copyWithStrokeColor(color) }
    }

    fun updateFillColor(shapeId: String, color: Color) {
        updateShape(shapeId) { it.copyWithFillColor(color) }
    }

    fun updateStrokeWidth(shapeId: String, width: Float) {
        updateShape(shapeId) { it.copyWithStrokeWidth(width) }
    }

    fun clear() {
        _shapes.value = emptyList()
    }
}