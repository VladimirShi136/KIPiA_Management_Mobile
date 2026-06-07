package com.kipia.management.mobile.ui.components.scheme.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.kipia.management.mobile.data.entities.ShapeData
import com.kipia.management.mobile.ui.components.scheme.shapes.*
import timber.log.Timber
import java.util.UUID
import kotlin.math.*

object ShapeUtils {
    const val DEVICE_ICON_SIZE = 45f

    // ====== ПРОВЕРКА ПОПАДАНИЯ ======

    fun isPointInRectangle(point: Offset, width: Float, height: Float): Boolean {
        return point.x in 0f..width && point.y in 0f..height
    }

    fun isPointInEllipse(point: Offset, width: Float, height: Float): Boolean {
        val centerX = width / 2; val centerY = height / 2
        val relX = point.x - centerX; val relY = point.y - centerY
        val normX = relX / centerX; val normY = relY / centerY
        return (normX * normX + normY * normY) <= 1.0f
    }

    fun isPointInLine(point: Offset, start: Offset, end: Offset, strokeWidth: Float): Boolean {
        val hitRadius = max(strokeWidth * 3f, 20f)
        return distanceToSegment(point, start, end) <= hitRadius
    }

    fun isPointInText(point: Offset, width: Float, height: Float): Boolean {
        return point.x in 0f..width && point.y in 0f..height
    }

    fun isPointInButterfly(point: Offset, width: Float, height: Float): Boolean {
        val centerX = width / 2; val centerY = height / 2
        if (point.x !in 0.0..width.toDouble() || point.y < 0 || point.y > height) return false
        val inLeftTriangle = isPointInTriangle(point, Offset(0f, 0f), Offset(centerX, centerY), Offset(0f, height))
        if (inLeftTriangle) return true
        return isPointInTriangle(point, Offset(width, 0f), Offset(centerX, centerY), Offset(width, height))
    }

    private fun isPointInTriangle(point: Offset, a: Offset, b: Offset, c: Offset): Boolean {
        val v0 = Offset(c.x - a.x, c.y - a.y); val v1 = Offset(b.x - a.x, b.y - a.y); val v2 = Offset(point.x - a.x, point.y - a.y)
        val dot00 = v0.x * v0.x + v0.y * v0.y; val dot01 = v0.x * v1.x + v0.y * v1.y; val dot02 = v0.x * v2.x + v0.y * v2.y
        val dot11 = v1.x * v1.x + v1.y * v1.y; val dot12 = v1.x * v2.x + v1.y * v2.y
        val invDenom = 1f / (dot00 * dot11 - dot01 * dot01)
        val u = (dot11 * dot02 - dot01 * dot12) * invDenom; val v = (dot00 * dot12 - dot01 * dot02) * invDenom
        return (u >= 0) && (v >= 0) && (u + v <= 1)
    }

    private fun distanceToSegment(p: Offset, v: Offset, w: Offset): Float {
        val l2 = (v.x - w.x) * (v.x - w.x) + (v.y - w.y) * (v.y - w.y)
        if (l2 == 0f) return sqrt((p.x - v.x).pow(2f) + (p.y - v.y).pow(2f))
        var t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2
        t = t.coerceIn(0f, 1f)
        val projection = Offset(v.x + t * (w.x - v.x), v.y + t * (w.y - v.y))
        return sqrt((p.x - projection.x).pow(2f) + (p.y - projection.y).pow(2f))
    }

    // ====== ГРАНИЦЫ ======

    fun getShapeBounds(shape: ComposeShape): Rect {
        return when (shape) {
            is ComposeRectangle, is ComposeEllipse, is ComposeText -> getRotatedRectBounds(shape)
            is ComposeRhombus -> getRotatedRhombusBounds(shape)
            is ComposeLine -> getLineBounds(shape)
            else -> Rect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height)
        }
    }

    private fun getRotatedRectBounds(shape: ComposeShape): Rect {
        if (shape.rotation == 0f) return Rect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height)
        val centerX = shape.x + shape.width / 2; val centerY = shape.y + shape.height / 2
        val radians = Math.toRadians(shape.rotation.toDouble()).toFloat()
        val corners = listOf(
            Offset(-shape.width / 2, -shape.height / 2), Offset(shape.width / 2, -shape.height / 2),
            Offset(shape.width / 2, shape.height / 2), Offset(-shape.width / 2, shape.height / 2)
        )
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        corners.forEach { corner ->
            val rx = corner.x * cos(radians) - corner.y * sin(radians)
            val ry = corner.x * sin(radians) + corner.y * cos(radians)
            minX = min(minX, centerX + rx); minY = min(minY, centerY + ry)
            maxX = max(maxX, centerX + rx); maxY = max(maxY, centerY + ry)
        }
        return Rect(minX, minY, maxX, maxY)
    }

    private fun getRotatedRhombusBounds(shape: ComposeRhombus): Rect {
        val centerX = shape.x + shape.width / 2; val centerY = shape.y + shape.height / 2
        val radians = Math.toRadians(shape.rotation.toDouble()).toFloat()
        val vertices = listOf(Offset(0f, 0f), Offset(shape.width, 0f), Offset(shape.width / 2, shape.height / 2), Offset(0f, shape.height), Offset(shape.width, shape.height))
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE; var maxX = -Float.MAX_VALUE; var maxY = -Float.MAX_VALUE
        vertices.forEach { v ->
            val rx = (v.x - shape.width/2) * cos(radians) - (v.y - shape.height/2) * sin(radians)
            val ry = (v.x - shape.width/2) * sin(radians) + (v.y - shape.height/2) * cos(radians)
            minX = min(minX, centerX + rx); minY = min(minY, centerY + ry)
            maxX = max(maxX, centerX + rx); maxY = max(maxY, centerY + ry)
        }
        return Rect(minX, minY, maxX, maxY)
    }

    private fun getLineBounds(shape: ComposeLine): Rect {
        return Rect(min(shape.startX, shape.endX) - shape.strokeWidth, min(shape.startY, shape.endY) - shape.strokeWidth,
                    max(shape.startX, shape.endX) + shape.strokeWidth, max(shape.startY, shape.endY) + shape.strokeWidth)
    }

    fun isShapeWithinBounds(shape: ComposeShape, canvasWidth: Float, canvasHeight: Float): Boolean {
        val bounds = getShapeBounds(shape)
        return bounds.left >= 0 && bounds.top >= 0 && bounds.right <= canvasWidth && bounds.bottom <= canvasHeight
    }

    fun clampShape(shape: ComposeShape, canvasWidth: Float, canvasHeight: Float): ComposeShape {
        var s = shape
        if (s is ComposeLine) {
            val sX = s.startX.coerceIn(0f, canvasWidth); val sY = s.startY.coerceIn(0f, canvasHeight)
            val eX = s.endX.coerceIn(0f, canvasWidth); val eY = s.endY.coerceIn(0f, canvasHeight)
            s = s.copy(startX = sX, startY = sY, endX = eX, endY = eY, x = min(sX, eX), y = min(sY, eY), width = abs(eX-sX).coerceAtLeast(10f), height = abs(eY-sY).coerceAtLeast(10f))
        } else {
            s.width = s.width.coerceIn(10f, canvasWidth); s.height = s.height.coerceIn(10f, canvasHeight)
        }
        val bounds = getShapeBounds(s)
        var dx = 0f; var dy = 0f
        if (bounds.left < 0) dx = -bounds.left else if (bounds.right > canvasWidth) dx = canvasWidth - bounds.right
        if (bounds.top < 0) dy = -bounds.top else if (bounds.bottom > canvasHeight) dy = canvasHeight - bounds.bottom
        if (dx != 0f || dy != 0f) {
            s = if (s is ComposeLine) s.copy(startX = s.startX + dx, startY = s.startY + dy, endX = s.endX + dx, endY = s.endY + dy, x = s.x + dx, y = s.y + dy)
                else s.copyWithPosition(s.x + dx, s.y + dy)
        }
        return s
    }

    fun clampShapePosition(shape: ComposeShape, targetX: Float, targetY: Float, canvasWidth: Float, canvasHeight: Float): Offset {
        val temp = when (shape) {
            is ComposeRectangle -> shape.copy(x = targetX, y = targetY)
            is ComposeEllipse -> shape.copy(x = targetX, y = targetY)
            is ComposeRhombus -> shape.copy(x = targetX, y = targetY)
            is ComposeLine -> { val dx = targetX - shape.x; val dy = targetY - shape.y
                shape.copy(x = targetX, y = targetY, startX = shape.startX + dx, startY = shape.startY + dy, endX = shape.endX + dx, endY = shape.endY + dy) }
            is ComposeText -> shape.copy(x = targetX, y = targetY)
            else -> shape
        }
        val bounds = getShapeBounds(temp)
        var cx = targetX; var cy = targetY
        if (bounds.left < 0) cx -= bounds.left else if (bounds.right > canvasWidth) cx -= (bounds.right - canvasWidth)
        if (bounds.top < 0) cy -= bounds.top else if (bounds.bottom > canvasHeight) cy -= (bounds.bottom - canvasHeight)
        return Offset(cx, cy)
    }

    fun clampDevicePosition(x: Float, y: Float, canvasWidth: Float, canvasHeight: Float): Offset {
        return Offset(x.coerceIn(0f, (canvasWidth - DEVICE_ICON_SIZE).coerceAtLeast(0f)),
                      y.coerceIn(0f, (canvasHeight - DEVICE_ICON_SIZE).coerceAtLeast(0f)))
    }

    fun transformPointToShapeSpace(point: Offset, shapeX: Float, shapeY: Float, shapeWidth: Float, shapeHeight: Float, rotation: Float): Offset {
        val centerX = shapeX + shapeWidth / 2; val centerY = shapeY + shapeHeight / 2
        val localX = point.x - centerX; val localY = point.y - centerY
        val radians = rotation * PI.toFloat() / 180f
        val rotatedX = localX * cos(-radians) - localY * sin(-radians)
        val rotatedY = localX * sin(-radians) + localY * cos(-radians)
        return Offset(rotatedX + shapeWidth / 2, rotatedY + shapeHeight / 2)
    }

    // ====== ЦВЕТА (RGBA для JavaFX) ======

    fun parseHexColor(hex: String?, default: Color = Color.Transparent): Color {
        if (hex.isNullOrBlank()) return default
        return try {
            val clean = hex.removePrefix("#")
            if (clean.length == 8) { // RGBA
                val r = clean.substring(0,2).toInt(16); val g = clean.substring(2,4).toInt(16)
                val b = clean.substring(4,6).toInt(16); val a = clean.substring(6,8).toInt(16)
                Color(r/255f, g/255f, b/255f, a/255f)
            } else Color(0xFF000000 or clean.toLong(16))
        } catch (e: Exception) { default }
    }

    fun Color.toHex(): String {
        val r = (red * 255).toInt().coerceIn(0, 255); val g = (green * 255).toInt().coerceIn(0, 255)
        val b = (blue * 255).toInt().coerceIn(0, 255); val a = (alpha * 255).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X%02X", r, g, b, a)
    }
}

// ====== МАППЕРЫ ======

fun ShapeData.toComposeShape(): ComposeShape {
    val f = ShapeUtils.parseHexColor(fillColor, Color.Transparent)
    val s = ShapeUtils.parseHexColor(strokeColor, Color.Black)
    
    // Генерируем по-настоящему уникальный ID, если он пуст в базе (как в JavaFX)
    val shapeId = id ?: "${type.lowercase()}_${UUID.randomUUID()}"
    
    // Пытаемся взять значения из properties для совместимости
    val pText = (properties?.get("text") as? String) ?: text
    val pFontSize = (properties?.get("fontSize") as? Number)?.toFloat() ?: fontSize
    val pFontStyle = (properties?.get("fontStyle") as? String) ?: fontStyle
    val pFontFamily = (properties?.get("fontFamily") as? String) ?: fontFamily

    return when (type) {
        "RECTANGLE" -> ComposeRectangle(shapeId, x, y, width, height, rotation, f, s, strokeWidth)
        "LINE" -> ComposeLine(shapeId, x, y, width, height, rotation, f, s, strokeWidth, startX, startY, endX, endY)
        "ELLIPSE" -> ComposeEllipse(shapeId, x, y, width, height, rotation, f, s, strokeWidth)
        "TEXT" -> {
            val style = pFontStyle?.lowercase() ?: ""
            ComposeText(
                id = shapeId,
                x = x,
                y = y,
                width = width,
                height = height,
                rotation = rotation,
                fillColor = f,
                strokeColor = s,
                strokeWidth = strokeWidth,
                text = pText ?: "",
                fontSize = if (pFontSize > 0) pFontSize else 16f,
                textColor = if (fillColor != null) f else Color.Black,
                isBold = style.contains("bold"),
                isItalic = style.contains("italic"),
                fontFamily = pFontFamily ?: "Arial"
            )
        }
        "RHOMBUS", "VALVE" -> ComposeRhombus(shapeId, x, y, width, height, rotation, f, s, strokeWidth)
        else -> ComposeRectangle(shapeId, x, y, width, height)
    }
}

fun ComposeShape.toShapeData(): ShapeData {
    val t = when (this) {
        is ComposeRectangle -> "RECTANGLE"; is ComposeLine -> "LINE"; is ComposeEllipse -> "ELLIPSE"; is ComposeText -> "TEXT"; is ComposeRhombus -> "RHOMBUS"; else -> "UNKNOWN"
    }
    fun Color.toHx() = with(ShapeUtils) { this@toHx.toHex() }
    
    val style = if (this is ComposeText) {
        when {
            isBold && isItalic -> "Bold Italic"
            isBold -> "Bold"
            isItalic -> "Italic"
            else -> "Regular"
        }
    } else "Regular"

    // Создаем карту свойств для JavaFX
    val props = if (this is ComposeText) {
        mapOf(
            "text" to text,
            "fontSize" to fontSize.toDouble(),
            "fontStyle" to style,
            "fontFamily" to fontFamily
        )
    } else null
    
    return ShapeData(
        type = t,
        id = id,
        x = x,
        y = y,
        width = width,
        height = height,
        rotation = rotation,
        fillColor = if (this is ComposeText) textColor.toHx() else fillColor.toHx(),
        strokeColor = strokeColor.toHx(),
        strokeWidth = strokeWidth,
        properties = props,
        startX = if (this is ComposeLine) startX else 0f,
        startY = if (this is ComposeLine) startY else 0f,
        endX = if (this is ComposeLine) endX else 0f,
        endY = if (this is ComposeLine) endY else 0f,
        text = if (this is ComposeText) text else null,
        fontSize = if (this is ComposeText) fontSize else 0f,
        fontStyle = style,
        fontFamily = if (this is ComposeText) fontFamily else "System"
    )
}
