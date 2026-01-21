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
import android.graphics.Color as AndroidColor // Android Color с алиасом
import androidx.core.graphics.toColorInt


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
    val data: String
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
            Gson().fromJson(data, SchemeData::class.java) ?: SchemeData()
        } catch (_: Exception) {
            SchemeData()
        }
    }

    fun setSchemeData(schemeData: SchemeData): Scheme {
        val json = Gson().toJson(schemeData)
        return this.copy(data = json)
    }
}

data class SchemeData(
    val width: Int = 1000,
    val height: Int = 1000,
    val backgroundColor: String = "#FFFFFF",
    val backgroundImage: String? = null,
    val gridEnabled: Boolean = true,
    val gridSize: Int = 50,
    val devices: List<SchemeDevice> = emptyList(),
    val shapes: List<ShapeData> = emptyList()
)

// Класс для сериализации/десериализации фигур
data class ShapeData(
    val type: String, // "rectangle", "line", "ellipse", "text"
    val id: String,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val rotation: Float = 0f,
    val fillColor: String = "#00000000", // ARGB hex
    val strokeColor: String = "#FF000000",
    val strokeWidth: Float = 2f,
    val properties: Map<String, Any> = emptyMap() // Дополнительные свойства
) {
    // Конвертация в ComposeShape
    fun toComposeShape(): ComposeShape {
        return when (type) {
            "rectangle" -> ComposeRectangle(
                id = id,
                x = x,
                y = y,
                width = width,
                height = height,
                rotation = rotation,
                fillColor = Color(fillColor.toColorInt()), // ← ИСПОЛЬЗУЙТЕ AndroidColor
                strokeColor = Color(strokeColor.toColorInt()),
                strokeWidth = strokeWidth,
                cornerRadius = properties["cornerRadius"] as? Float ?: 0f
            )
            "line" -> ComposeLine(
                id = id,
                x = x,
                y = y,
                width = width,
                height = height,
                rotation = rotation,
                fillColor = Color(fillColor.toColorInt()), // ← ИСПОЛЬЗУЙТЕ AndroidColor
                strokeColor = Color(strokeColor.toColorInt()),
                strokeWidth = strokeWidth,
                startX = properties["startX"] as? Float ?: 0f,
                startY = properties["startY"] as? Float ?: 0f,
                endX = properties["endX"] as? Float ?: width,
                endY = properties["endY"] as? Float ?: 0f
            )
            "ellipse" -> ComposeEllipse(
                id = id,
                x = x,
                y = y,
                width = width,
                height = height,
                rotation = rotation,
                fillColor = Color(fillColor.toColorInt()), // ← ИСПОЛЬЗУЙТЕ AndroidColor
                strokeColor = Color(strokeColor.toColorInt()),
                strokeWidth = strokeWidth
            )
            "text" -> ComposeText(
                id = id,
                x = x,
                y = y,
                width = width,
                height = height,
                rotation = rotation,
                fillColor = Color(fillColor.toColorInt()), // ← ИСПОЛЬЗУЙТЕ AndroidColor
                strokeColor = Color(strokeColor.toColorInt()),
                strokeWidth = strokeWidth,
                text = properties["text"] as? String ?: "",
                fontSize = properties["fontSize"] as? Float ?: 16f
            )
            "rhombus" -> ComposeRhombus(
                id = id,
                x = x,
                y = y,
                width = width,
                height = height,
                rotation = rotation,
                fillColor = Color(fillColor.toColorInt()), // ← ИСПОЛЬЗУЙТЕ AndroidColor
                strokeColor = Color(strokeColor.toColorInt()),
                strokeWidth = strokeWidth
            )
            else -> throw IllegalArgumentException("Unknown shape type: $type")
        }
    }
}

// Расширение для конвертации ComposeShape → ShapeData
fun ComposeShape.toShapeData(): ShapeData {
    return ShapeData(
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
                "fontSize" to fontSize
            )
            else -> emptyMap()
        }
    )
}

// Расширение для конвертации Compose Color в hex строку
private fun Color.toArgbHex(): String {
    // Создаем android.graphics.Color из Compose Color
    val androidColor = AndroidColor.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
    return String.format("#%08X", androidColor)
}

data class SchemeDevice(
    val deviceId: Int,
    val x: Float,
    val y: Float,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val zIndex: Int = 0
)