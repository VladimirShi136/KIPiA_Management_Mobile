package com.kipia.management.mobile.ui.components.scheme.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import com.kipia.management.mobile.data.entities.ShapeData
import com.kipia.management.mobile.ui.components.scheme.shapes.*
import timber.log.Timber
import java.util.UUID
import kotlin.math.*

object ShapeUtils {
    const val DEVICE_ICON_SIZE = 45f

    // ====== ПРОВЕРКА ПОПАДАНИЯ ======

    fun transformPointToShapeSpace(point: Offset, x: Float, y: Float, width: Float, height: Float, rotation: Float): Offset {
        if (rotation == 0f) return Offset(point.x - x, point.y - y)
        
        val centerX = x + width / 2
        val centerY = y + height / 2
        val radians = Math.toRadians(-rotation.toDouble()).toFloat()
        
        val relX = point.x - centerX
        val relY = point.y - centerY
        
        val rotatedX = relX * cos(radians) - relY * sin(radians)
        val rotatedY = relX * sin(radians) + relY * cos(radians)
        
        return Offset(rotatedX + width / 2, rotatedY + height / 2)
    }

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

    // ====== ЦВЕТА (Совместимость с JavaFX и Android) ======

    fun parseHexColor(hex: String?, default: Color = Color.Transparent): Color {
        if (hex.isNullOrBlank()) return default
        
        // JavaFX Color.toString() обычно возвращает 0xRRGGBBAA
        // Android/Compose обычно использует #AARRGGBB
        val isJavaFXPrefix = hex.startsWith("0x")
        
        return try {
            val clean = hex.removePrefix("#").removePrefix("0x")
            when (clean.length) {
                6 -> Color(0xFF000000 or clean.toLong(16)) // RGB -> Непрозрачный ARGB
                8 -> {
                    // Пытаемся определить формат RGBA (JavaFX) или ARGB (Android)
                    // Heuristic: если строка из JavaFX или если она заканчивается на FF (непрозрачный), 
                    // но при этом в начале не FF, то это скорее всего RGBA.
                    val isLikelyRGBA = isJavaFXPrefix || (hex.startsWith("#") && hex.endsWith("FF") && !hex.startsWith("#FF"))
                    
                    if (isLikelyRGBA) {
                        // RGBA (JavaFX)
                        val r = clean.substring(0, 2).toInt(16)
                        val g = clean.substring(2, 4).toInt(16)
                        val b = clean.substring(4, 6).toInt(16)
                        val a = clean.substring(6, 8).toInt(16)
                        Color(r / 255f, g / 255f, b / 255f, a / 255f)
                    } else {
                        // ARGB (Android)
                        val a = clean.substring(0, 2).toInt(16)
                        val r = clean.substring(2, 4).toInt(16)
                        val g = clean.substring(4, 6).toInt(16)
                        val b = clean.substring(6, 8).toInt(16)
                        Color(r / 255f, g / 255f, b / 255f, a / 255f)
                    }
                }
                else -> Color(clean.toLong(16))
            }
        } catch (e: Exception) { default }
    }

    fun Color.toHex(): String {
        val a = (alpha * 255).toInt().coerceIn(0, 255)
        val r = (red * 255).toInt().coerceIn(0, 255)
        val g = (green * 255).toInt().coerceIn(0, 255)
        val b = (blue * 255).toInt().coerceIn(0, 255)
        return String.format("#%02X%02X%02X%02X", a, r, g, b)
    }
}

// ====== МАППЕРЫ (JavaFX -> Android Compose) ======

fun ShapeData.toComposeShape(): ComposeShape {
    val f = ShapeUtils.parseHexColor(fillColor, Color.Transparent)
    val s = ShapeUtils.parseHexColor(strokeColor, Color.Black)
    
    // Генерируем уникальный ID для JavaFX фигур
    val shapeId = id ?: "${type.lowercase()}_${UUID.randomUUID()}"
    
    // Приводим все координаты к Float для Compose
    val xF = x.toFloat(); val yF = y.toFloat()
    val wF = width.toFloat(); val hF = height.toFloat()
    val rF = rotation.toFloat()
    val swF = strokeWidth.toFloat()

    return when (type.uppercase()) {
        "RECTANGLE" -> ComposeRectangle(shapeId, xF, yF, wF, hF, rF, f, s, swF)
        "LINE" -> ComposeLine(shapeId, xF, yF, wF, hF, rF, f, s, swF, startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat())
        "ELLIPSE" -> ComposeEllipse(shapeId, xF, yF, wF, hF, rF, f, s, swF)
        "TEXT" -> {
            val style = fontStyle?.lowercase() ?: ""
            ComposeText(
                id = shapeId, x = xF, y = yF, width = wF, height = hF, rotation = rF,
                fillColor = f, strokeColor = s, strokeWidth = swF,
                text = text ?: "",
                fontSize = if (fontSize > 0) fontSize.toFloat() else 16f,
                textColor = if (fillColor != null) f else Color.Black,
                isBold = style.contains("bold"),
                isItalic = style.contains("italic"),
                fontFamily = fontFamily ?: "Arial"
            )
        }
        "RHOMBUS", "VALVE" -> ComposeRhombus(shapeId, xF, yF, wF, hF, rF, f, s, swF)
        else -> ComposeRectangle(shapeId, xF, yF, wF.coerceAtLeast(10f), hF.coerceAtLeast(10f))
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

    return ShapeData(
        type = t,
        id = id,
        x = x.toDouble(), y = y.toDouble(),
        width = width.toDouble(), height = height.toDouble(),
        rotation = rotation.toDouble(),
        fillColor = if (this is ComposeText) textColor.toHx() else fillColor.toHx(),
        strokeColor = strokeColor.toHx(),
        strokeWidth = strokeWidth.toDouble(),
        startX = if (this is ComposeLine) startX.toDouble() else 0.0,
        startY = if (this is ComposeLine) startY.toDouble() else 0.0,
        endX = if (this is ComposeLine) endX.toDouble() else 0.0,
        endY = if (this is ComposeLine) endY.toDouble() else 0.0,
        text = if (this is ComposeText) text else null,
        fontSize = if (this is ComposeText) fontSize.toDouble() else 0.0,
        fontStyle = style,
        fontFamily = if (this is ComposeText) fontFamily else "System"
    )
}
