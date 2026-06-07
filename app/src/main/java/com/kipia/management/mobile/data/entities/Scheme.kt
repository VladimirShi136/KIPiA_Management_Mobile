package com.kipia.management.mobile.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import timber.log.Timber

@Entity(
    tableName = "schemes",
    indices = [Index(value = ["name"], unique = true)]
)
data class Scheme(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String?,
    @ColumnInfo(name = "data")
    val data: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = 0,
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long = 0
) {
    companion object {
        fun createEmpty(name: String = ""): Scheme = Scheme(
            name = name,
            description = null,
            data = "{}"
        )
    }

    fun isDeleted(): Boolean = deletedAt > 0

    fun getSchemeData(): SchemeData {
        if (data.isBlank() || data == "{}") return SchemeData()
        return try {
            val result = Gson().fromJson(data, SchemeData::class.java)
            result ?: SchemeData()
        } catch (e: Exception) {
            Timber.e(e, "Ошибка парсинга SchemeData JSON: $data")
            SchemeData()
        }
    }

    fun setSchemeData(schemeData: SchemeData): Scheme {
        return this.copy(data = Gson().toJson(schemeData))
    }

    fun withUpdatedNow(): Scheme = this.copy(updatedAt = System.currentTimeMillis())
}

/**
 * Структура данных схемы. 
 * Поле 'devices' помечено @Transient, чтобы Gson не сохранял его в JSON (совместимость с JavaFX).
 */
data class SchemeData(
    val version: Int = 1,
    val width: Int = 2000,
    val height: Int = 1200,
    val backgroundColor: String = "#FFFFFF",
    val backgroundImage: String? = null,
    val gridEnabled: Boolean = true,
    val gridSize: Int = 50,
    @Transient val devices: List<SchemeDevice> = emptyList(),
    val shapes: List<ShapeData> = emptyList()
)

data class ShapeData(
    val type: String,
    val id: String? = null,
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val rotation: Float = 0f,
    val fillColor: String? = null,
    val strokeColor: String? = null,
    val strokeWidth: Float = 2f,
    val properties: Map<String, Any>? = null,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val endX: Float = 0f,
    val endY: Float = 0f,
    val text: String? = null,
    val fontSize: Float = 0f,
    val fontStyle: String? = null,
    val fontFamily: String? = "System"
)
