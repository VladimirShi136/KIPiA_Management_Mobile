package com.kipia.management.mobile.ui.components.scheme.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeEllipse
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeLine
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeRectangle
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeRhombus
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeText
import timber.log.Timber
import kotlin.math.*

object ShapeUtils {
    // ====== СУЩЕСТВУЮЩИЕ ФУНКЦИИ ======

    fun isPointInRectangle(point: Offset, width: Float, height: Float): Boolean {
        return point.x in 0f..width && point.y in 0f..height
    }

    fun isPointInEllipse(point: Offset, width: Float, height: Float): Boolean {
        val centerX = width / 2
        val centerY = height / 2

        val relX = point.x - centerX
        val relY = point.y - centerY

        val normX = relX / centerX
        val normY = relY / centerY

        return (normX * normX + normY * normY) <= 1.0f
    }

    fun isPointInLine(point: Offset, start: Offset, end: Offset, strokeWidth: Float): Boolean {
        val hitRadius = max(strokeWidth * 3f, 20f)
        val distance = distanceToSegment(point, start, end)
        return distance <= hitRadius
    }

    fun isPointInText(point: Offset, width: Float, height: Float): Boolean {
        return point.x in 0f..width && point.y in 0f..height
    }

    fun isPointInButterfly(point: Offset, width: Float, height: Float): Boolean {
        val centerX = width / 2
        val centerY = height / 2

        Timber.d("🦋 isPointInButterfly:")
        Timber.d("   point=(${point.x}, ${point.y})")
        Timber.d("   width=$width, height=$height")
        Timber.d("   center=($centerX, $centerY)")

        // Сначала проверяем bounding box для быстрого отсечения
        if (point.x !in 0.0..width.toDouble() || point.y < 0 || point.y > height) {
            Timber.d("   ❌ Outside bounding box")
            return false
        }

        // Проверяем левый треугольник
        val inLeftTriangle = isPointInTriangle(
            point = point,
            a = Offset(0f, 0f),
            b = Offset(centerX, centerY),
            c = Offset(0f, height)
        )

        if (inLeftTriangle) {
            Timber.d("   ✅ In left triangle")
            return true
        }

        // Проверяем правый треугольник
        val inRightTriangle = isPointInTriangle(
            point = point,
            a = Offset(width, 0f),
            b = Offset(centerX, centerY),
            c = Offset(width, height)
        )

        if (inRightTriangle) {
            Timber.d("   ✅ In right triangle")
        } else {
            Timber.d("   ❌ Not in any triangle")
        }

        return inRightTriangle
    }

    private fun isPointInTriangle(point: Offset, a: Offset, b: Offset, c: Offset): Boolean {
        val v0 = Offset(c.x - a.x, c.y - a.y)
        val v1 = Offset(b.x - a.x, b.y - a.y)
        val v2 = Offset(point.x - a.x, point.y - a.y)

        val dot00 = v0.x * v0.x + v0.y * v0.y
        val dot01 = v0.x * v1.x + v0.y * v1.y
        val dot02 = v0.x * v2.x + v0.y * v2.y
        val dot11 = v1.x * v1.x + v1.y * v1.y
        val dot12 = v1.x * v2.x + v1.y * v2.y

        val invDenom = 1f / (dot00 * dot11 - dot01 * dot01)
        val u = (dot11 * dot02 - dot01 * dot12) * invDenom
        val v = (dot00 * dot12 - dot01 * dot02) * invDenom

        return (u >= 0) && (v >= 0) && (u + v <= 1)
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

    // ====== НОВЫЕ ФУНКЦИИ ДЛЯ ГРАНИЦ ======

    /**
     * Получить реальные границы фигуры с учетом поворота
     */
    fun getShapeBounds(shape: ComposeShape): Rect {
        return when (shape) {
            is ComposeRectangle -> getRotatedRectBounds(shape)
            is ComposeEllipse -> getRotatedRectBounds(shape)
            is ComposeRhombus -> getRotatedRhombusBounds(shape)
            is ComposeLine -> getLineBounds(shape)
            is ComposeText -> getRotatedRectBounds(shape)
            else -> Rect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height)
        }
    }

    /**
     * Получить границы повернутого прямоугольника/эллипса/текста
     */
    private fun getRotatedRectBounds(shape: ComposeShape): Rect {
        if (shape.rotation == 0f) {
            return Rect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height)
        }

        val centerX = shape.x + shape.width / 2
        val centerY = shape.y + shape.height / 2
        val radians = Math.toRadians(shape.rotation.toDouble()).toFloat() // Конвертируем в Float

        // Четыре угла прямоугольника относительно центра
        val corners = listOf(
            Offset(-shape.width / 2, -shape.height / 2),
            Offset(shape.width / 2, -shape.height / 2),
            Offset(shape.width / 2, shape.height / 2),
            Offset(-shape.width / 2, shape.height / 2)
        )

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        corners.forEach { corner ->
            val rotatedX = corner.x * cos(radians) - corner.y * sin(radians)
            val rotatedY = corner.x * sin(radians) + corner.y * cos(radians)

            val worldX = centerX + rotatedX
            val worldY = centerY + rotatedY

            minX = min(minX, worldX)
            minY = min(minY, worldY)
            maxX = max(maxX, worldX)
            maxY = max(maxY, worldY)
        }

        return Rect(minX, minY, maxX, maxY)
    }

    /**
     * Получить границы ромба с учетом поворота
     */
    private fun getRotatedRhombusBounds(shape: ComposeRhombus): Rect {
        if (shape.rotation == 0f) {
            return Rect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height)
        }

        val centerX = shape.x + shape.width / 2
        val centerY = shape.y + shape.height / 2
        val radians = Math.toRadians(shape.rotation.toDouble()).toFloat()

        // Все вершины ромба (5 точек для точности)
        val vertices = listOf(
            Offset(0f, 0f),
            Offset(shape.width, 0f),
            Offset(shape.width / 2, shape.height / 2),
            Offset(0f, shape.height),
            Offset(shape.width, shape.height)
        )

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        vertices.forEach { vertex ->
            val relX = vertex.x - shape.width / 2
            val relY = vertex.y - shape.height / 2

            val rotatedX = relX * cos(radians) - relY * sin(radians)
            val rotatedY = relX * sin(radians) + relY * cos(radians)

            val worldX = centerX + rotatedX
            val worldY = centerY + rotatedY

            minX = min(minX, worldX)
            minY = min(minY, worldY)
            maxX = max(maxX, worldX)
            maxY = max(maxY, worldY)
        }

        return Rect(minX, minY, maxX, maxY)
    }

    /**
     * Получить границы линии с учетом поворота
     */
    private fun getLineBounds(shape: ComposeLine): Rect {
        // startX/endX — абсолютные координаты на холсте, используем их напрямую
        val minX = min(shape.startX, shape.endX) - shape.strokeWidth
        val minY = min(shape.startY, shape.endY) - shape.strokeWidth
        val maxX = max(shape.startX, shape.endX) + shape.strokeWidth
        val maxY = max(shape.startY, shape.endY) + shape.strokeWidth
        return Rect(minX, minY, maxX, maxY)
    }

    /**
     * Проверить, находится ли фигура в пределах канваса (с учетом поворота)
     */
    fun isShapeWithinBounds(shape: ComposeShape, canvasWidth: Float, canvasHeight: Float): Boolean {
        val bounds = getShapeBounds(shape)
        val result = bounds.left >= 0 && bounds.top >= 0 &&
                bounds.right <= canvasWidth && bounds.bottom <= canvasHeight

        Timber.d("📏 isShapeWithinBounds: $result, bounds=$bounds")
        return result
    }

    /**
     * Ограничить позицию фигуры границами канваса (с учетом поворота)
     */
    fun clampShapePosition(
        shape: ComposeShape,
        targetX: Float,
        targetY: Float,
        canvasWidth: Float,
        canvasHeight: Float
    ): Offset {
        // Создаем временную копию фигуры с новой позицией
        val tempShape = when (shape) {
            is ComposeRectangle -> shape.copy(x = targetX, y = targetY)
            is ComposeEllipse -> shape.copy(x = targetX, y = targetY)
            is ComposeRhombus -> shape.copy(x = targetX, y = targetY)
            is ComposeLine -> {
                val dx = targetX - shape.x
                val dy = targetY - shape.y
                shape.copy(
                    x = targetX, y = targetY,
                    startX = shape.startX + dx,
                    startY = shape.startY + dy,
                    endX = shape.endX + dx,
                    endY = shape.endY + dy
                )
            }
            is ComposeText -> shape.copy(x = targetX, y = targetY)
            else -> shape
        }

        val bounds = getShapeBounds(tempShape)

        var clampedX = targetX
        var clampedY = targetY

        // Корректируем позицию, чтобы bounds оставался в пределах канваса
        if (bounds.left < 0) {
            clampedX += -bounds.left
        } else if (bounds.right > canvasWidth) {
            clampedX -= (bounds.right - canvasWidth)
        }

        if (bounds.top < 0) {
            clampedY += -bounds.top
        } else if (bounds.bottom > canvasHeight) {
            clampedY -= (bounds.bottom - canvasHeight)
        }

        Timber.d("📐 clampShapePosition: ($targetX, $targetY) -> ($clampedX, $clampedY)")
        return Offset(clampedX, clampedY)
    }

    // ====== ТРАНСФОРМАЦИИ ======

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