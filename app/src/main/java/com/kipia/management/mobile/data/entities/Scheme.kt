package com.kipia.management.mobile.data.entities

import androidx.compose.ui.graphics.Color // Compose Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeEllipse
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeLine
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeRectangle
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeRhombus
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeText
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


/**
 * Модель схемы
 */
@Entity(tableName = "schemes")
data class Scheme(

    // идентификатор
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // название
    @ColumnInfo(name = "name")
    val name: String,

    // описание
    @ColumnInfo(name = "description")
    val description: String?,

    // JSON с данными схемы
    @ColumnInfo(name = "data")
    val data: String,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun createEmpty(name: String = ""): Scheme = Scheme(
            name = name,
            description = null,
            data = "{}"
        )
    }

    fun getSchemeData(): SchemeData {
        return try {
            val result = Gson().fromJson(data, SchemeData::class.java)
            Timber.d("getSchemeData: shapes count = ${result?.shapes?.size}, data length = ${data.length}")
            result?.shapes?.forEachIndexed { i, shape ->
                Timber.d("  shape[$i]: type=${shape.type}, x=${shape.x}, y=${shape.y}")
            }
            result ?: SchemeData()
        } catch (e: Exception) {
            Timber.e(e, "getSchemeData FAILED, data=$data")
            SchemeData()
        }
    }

    fun setSchemeData(schemeData: SchemeData): Scheme {
        val json = Gson().toJson(schemeData)
        return this.copy(data = json)
    }

    // метод для обновления времени
    fun withUpdatedNow(): Scheme {
        return this.copy(updatedAt = System.currentTimeMillis())
    }
}

data class SchemeData(
    val width: Int = 2000,
    val height: Int = 1200,
    val backgroundColor: String = "#FFFFFF",
    val backgroundImage: String? = null,
    val gridEnabled: Boolean = true,
    val gridSize: Int = 50,
    val devices: List<SchemeDevice> = emptyList(),
    val shapes: List<ShapeData> = emptyList()
)

// Класс для сериализации/десериализации фигур.
data class ShapeData(
    val type: String,               // JavaFX: "LINE", Android: "line" — нормализуем через .lowercase()
    val id: String? = null,         // отсутствует в JavaFX формате — генерируем при чтении
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val rotation: Float = 0f,
    val fillColor: String? = null,
    val strokeColor: String? = null,
    val strokeWidth: Float = 2f,
    val properties: Map<String, Any>? = null,
    val transform: TransformData? = null,
    val layer: LayerData? = null,
    val isLocked: Boolean = false,
    val isVisible: Boolean = true,
    // ── Поля JavaFX плоского формата ───────────────────────────────
    val startX: Float = 0f,   // линия
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val text: String? = null,       // текст
    val fontSize: Float = 0f,
    val fontFamily: String? = null,
    val fontStyle: String? = null   // "Regular" | "Bold" | "Italic" | "Bold Italic"
) {
    private val normalizedType get() = type.lowercase()

    fun toComposeShape(): ComposeShape {
        val shapeId = id?.takeIf { it.isNotBlank() }
            ?: "${normalizedType}_${x.toInt()}_${y.toInt()}_${System.nanoTime()}"

        Timber.d("💾 Загрузка фигуры: type=$type→$normalizedType id=$shapeId pos=($x,$y) rot=$rotation")

        return when (normalizedType) {
            "rectangle" -> ComposeRectangle(
                id = shapeId, x = x, y = y, width = width, height = height,
                rotation = rotation,
                fillColor = parseColor(fillColor),
                strokeColor = parseColor(strokeColor),
                strokeWidth = strokeWidth,
                cornerRadius = (properties?.get("cornerRadius") as? Double)?.toFloat()
                    ?: properties?.get("cornerRadius") as? Float ?: 0f
            )

            "line" -> {
                val sX = startX.takeIf { it != 0f || startY != 0f }
                    ?: (properties?.get("startX") as? Double)?.toFloat()
                    ?: properties?.get("startX") as? Float ?: x
                val sY = startY.takeIf { it != 0f || startX != 0f }
                    ?: (properties?.get("startY") as? Double)?.toFloat()
                    ?: properties?.get("startY") as? Float ?: y
                val eX = endX.takeIf { it != 0f || endY != 0f }
                    ?: (properties?.get("endX") as? Double)?.toFloat()
                    ?: properties?.get("endX") as? Float ?: (x + width)
                val eY = endY.takeIf { it != 0f || endX != 0f }
                    ?: (properties?.get("endY") as? Double)?.toFloat()
                    ?: properties?.get("endY") as? Float ?: y

                // Вычисляем bounding box линии из абсолютных координат
                val lineX = min(sX, eX)
                val lineY = min(sY, eY)
                val lineW = max(abs(eX - sX), strokeWidth) // минимум strokeWidth чтобы не было 0
                val lineH = max(abs(eY - sY), strokeWidth)

                ComposeLine(
                    id = shapeId,
                    x = lineX, y = lineY,
                    width = lineW, height = lineH,
                    rotation = rotation,
                    fillColor = parseColor(fillColor),
                    strokeColor = parseColor(strokeColor),
                    strokeWidth = strokeWidth,
                    startX = sX, startY = sY, endX = eX, endY = eY
                )
            }

            "ellipse" -> ComposeEllipse(
                id = shapeId, x = x, y = y, width = width, height = height,
                rotation = rotation,
                fillColor = parseColor(fillColor),
                strokeColor = parseColor(strokeColor),
                strokeWidth = strokeWidth
            )

            "rhombus" -> ComposeRhombus(
                id = shapeId, x = x, y = y, width = width, height = height,
                rotation = rotation,
                fillColor = parseColor(fillColor),
                strokeColor = parseColor(strokeColor),
                strokeWidth = strokeWidth
            )

            "text" -> {
                // JavaFX: text/fontSize в корне; Android: в properties
                val resolvedText = text
                    ?: properties?.get("text") as? String ?: ""
                val resolvedFontSize = when {
                    fontSize > 0f -> fontSize
                    else -> when (val v = properties?.get("fontSize")) {
                        is Double -> v.toFloat()
                        is Float -> v
                        is Int -> v.toFloat()
                        else -> 16f
                    }
                }
                // JavaFX fontStyle: "Bold Italic" → isBold + isItalic
                val style = fontStyle ?: ""
                val isBold = style.contains("Bold", ignoreCase = true)
                        || properties?.get("isBold") as? Boolean ?: false
                val isItalic = style.contains("Italic", ignoreCase = true)
                        || properties?.get("isItalic") as? Boolean ?: false
                // JavaFX использует strokeColor как цвет текста
                val textColorStr = properties?.get("textColor") as? String ?: strokeColor

                ComposeText(
                    id = shapeId, x = x, y = y, width = width, height = height,
                    rotation = rotation,
                    fillColor = parseColor(fillColor),
                    strokeColor = parseColor(strokeColor),
                    strokeWidth = strokeWidth,
                    text = resolvedText,
                    fontSize = resolvedFontSize,
                    textColor = parseColor(textColorStr),
                    isBold = isBold,
                    isItalic = isItalic
                )
            }

            else -> {
                Timber.w("⚠️ Неизвестный тип фигуры: '$type' — пропускаем")
                throw IllegalArgumentException("Unknown shape type: $type")
            }
        }
    }
}

data class TransformData(
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val rotation: Float = 0f,
    val skewX: Float = 0f,
    val skewY: Float = 0f
)

data class LayerData(
    val zIndex: Int,
    val groupId: String? = null,
    val isGroup: Boolean = false
)

// Добавьте функцию парсинга в SchemeData.kt или в отдельный утилитарный файл:
fun parseColor(colorHex: String?): Color {
    if (colorHex == null) return Color.Transparent
    return try {
        val cleanHex = colorHex.removePrefix("#")
        val longColor = when (cleanHex.length) {
            6 -> "FF$cleanHex"
            8 -> cleanHex
            else -> "FFFFFFFF"
        }.toLong(16)
        Color(longColor)
    } catch (_: Exception) {
        Color.Transparent
    }
}

// Расширение для конвертации ComposeShape → ShapeData
fun ComposeShape.toShapeData(): ShapeData {
    Timber.d("💾 Сохранение фигуры в JSON:")
    Timber.d("   id: $id")
    Timber.d("   rotation: $rotation")
    Timber.d("   position: ($x, $y)")

    val shapeData = ShapeData(
        type = when (this) {
            is ComposeRectangle -> "rectangle"
            is ComposeLine -> "line"
            is ComposeEllipse -> "ellipse"
            is ComposeText -> "text"
            is ComposeRhombus -> "rhombus"
            else -> "unknown"
        },
        id = id,
        x = x,
        y = y,
        width = width,
        height = height,
        rotation = rotation,
        fillColor = this.fillColor.toArgbHex(),
        strokeColor = this.strokeColor.toArgbHex(),
        strokeWidth = strokeWidth,
        properties = when (this) {
            is ComposeRectangle -> mapOf("cornerRadius" to cornerRadius)
            is ComposeLine -> mapOf(
                "startX" to startX,
                "startY" to startY,
                "endX" to endX,
                "endY" to endY
            )
            is ComposeText -> mapOf(
                "text" to text,
                "fontSize" to fontSize,
                "textColor" to textColor.toArgbHex(),
                "isBold" to isBold,
                "isItalic" to isItalic
            )
            else -> emptyMap()
        }
    )

    Timber.d("   properties: ${shapeData.properties}")
    return shapeData
}

// Расширение для конвертации Compose Color в hex строку
// И функцию toArgbHex() в ComposeShape расширении:
private fun Color.toArgbHex(): String {
    val alpha = (alpha * 255).toInt().and(0xFF)
    val red = (red * 255).toInt().and(0xFF)
    val green = (green * 255).toInt().and(0xFF)
    val blue = (blue * 255).toInt().and(0xFF)
    return String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
}

data class SchemeDevice(
    val deviceId: Int,
    val x: Float,
    val y: Float,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val zIndex: Int = 0
)