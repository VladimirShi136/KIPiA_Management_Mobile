package com.kipia.management.mobile.managers

import androidx.compose.ui.geometry.Offset
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class ShapeManager {
    private val _shapes = MutableStateFlow<List<ComposeShape>>(emptyList())
    val shapes = _shapes.asStateFlow()

    fun addShape(shape: ComposeShape) {
        _shapes.update { current ->
            current + shape  // Просто добавляем в конец
        }
    }

    fun removeShape(shapeId: String) {
        _shapes.update { it.filter { it.id != shapeId } }
    }

    fun updateShape(shapeId: String, update: (ComposeShape) -> ComposeShape) {
        _shapes.update { shapes ->
            shapes.map { shape ->
                if (shape.id == shapeId) update(shape) else shape
            }
        }
    }

    fun moveShape(shapeId: String, delta: Offset) {
        _shapes.update { shapes ->
            shapes.map { shape ->
                if (shape.id == shapeId) {
                    shape.apply {
                        x += delta.x
                        y += delta.y
                    }
                } else shape
            }
        }
    }

    fun moveShapeTo(shape: ComposeShape, newX: Float, newY: Float) {
        _shapes.update { shapes ->
            shapes.map { s ->
                if (s.id == shape.id) {
                    shape.apply {
                        x = newX
                        y = newY
                    }
                } else s
            }
        }
    }

    fun findShapeAt(point: Offset): ComposeShape? {
        return _shapes.value.reversed().firstOrNull { it.contains(point) }
    }
}