package com.kipia.management.mobile.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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
 * Поле 'devices' теперь НЕ @Transient для совместимости с JavaFX версией при импорте.
 */
data class SchemeData(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("width") val width: Double = 2000.0,
    @SerializedName("height") val height: Double = 1200.0,
    @SerializedName("backgroundColor") val backgroundColor: String = "#FFFFFF",
    @SerializedName("backgroundImage") val backgroundImage: String? = null,
    @SerializedName("gridEnabled") val gridEnabled: Boolean = true,
    @SerializedName("gridSize") val gridSize: Int = 50,
    @SerializedName("devices") val devices: List<SchemeDevice> = emptyList(),
    @SerializedName("shapes") val shapes: List<ShapeData> = emptyList()
)

data class ShapeData(
    @SerializedName("type") val type: String,
    @SerializedName("id") val id: String? = null,
    @SerializedName("x") val x: Double = 0.0,
    @SerializedName("y") val y: Double = 0.0,
    @SerializedName("width") val width: Double = 0.0,
    @SerializedName("height") val height: Double = 0.0,
    @SerializedName("rotation") val rotation: Double = 0.0,
    @SerializedName("fillColor") val fillColor: String? = null,
    @SerializedName("strokeColor") val strokeColor: String? = null,
    @SerializedName("strokeWidth") val strokeWidth: Double = 2.0,
    @SerializedName("properties") val properties: Map<String, Any>? = null,
    @SerializedName("startX") val startX: Double = 0.0,
    @SerializedName("startY") val startY: Double = 0.0,
    @SerializedName("endX") val endX: Double = 0.0,
    @SerializedName("endY") val endY: Double = 0.0,
    @SerializedName("text") val text: String? = null,
    @SerializedName("fontSize") val fontSize: Double = 0.0,
    @SerializedName("fontStyle") val fontStyle: String? = null,
    @SerializedName("fontFamily") val fontFamily: String? = "System"
)
