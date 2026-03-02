package com.kipia.management.mobile.ui.components.scheme.utils

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

object ShapeUtils {
    // ====== ПРОВЕРКА ПОПАДАНИЯ ТОЧКИ (оставляем как есть) ======

    fun isPointInRectangle(point: Offset, width: Float, height: Float): Boolean {
        return point.x in 0f..width && point.y in 0f..height
    }

    fun isPointInEllipse(point: Offset, width: Float, height: Float): Boolean {
        return (point.x * point.x) / (width * width / 4) +
                (point.y * point.y) / (height * height / 4) <= 1
    }

    fun isPointInRhombus(point: Offset, width: Float, height: Float): Boolean {
        return (abs(point.x) / (width / 2) + abs(point.y) / (height / 2)) <= 1
    }

    fun isPointInLine(point: Offset, start: Offset, end: Offset, strokeWidth: Float): Boolean {
        val distance = distanceToSegment(point, start, end)
        return distance <= (strokeWidth + 5f)
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

    fun isPointInText(point: Offset, width: Float, height: Float): Boolean {
        return point.x in 0f..width && point.y in 0f..height
    }

    // ====== ТРАНСФОРМАЦИИ (оставляем для contains) ======

    fun transformPointToShapeSpace(
        point: Offset,
        shapeX: Float,
        shapeY: Float,
        shapeWidth: Float,
        shapeHeight: Float,
        rotation: Float
    ): Offset {
        val centerX = shapeX + shapeWidth / 2
        val centerY = shapeY + shapeHeight / 2

        val localX = point.x - centerX
        val localY = point.y - centerY

        val radians = rotation * PI.toFloat() / 180f
        val rotatedX = localX * cos(-radians) - localY * sin(-radians)
        val rotatedY = localX * sin(-radians) + localY * cos(-radians)

        return Offset(rotatedX + shapeWidth / 2, rotatedY + shapeHeight / 2)
    }
}