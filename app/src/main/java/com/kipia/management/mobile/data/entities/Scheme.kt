package com.kipia.management.mobile.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson

@Entity(tableName = "schemes")
data class Scheme(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String?,

    @ColumnInfo(name = "data")
    val data: String  // JSON с данными схемы
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
        } catch (e: Exception) {
            SchemeData()
        }
    }

    fun setSchemeData(schemeData: SchemeData): Scheme {
        val json = Gson().toJson(schemeData)
        return this.copy(data = json)
    }

    fun isValid(): Boolean = name.isNotBlank()
}

data class SchemeData(
    val width: Int = 1000,
    val height: Int = 1000,
    val backgroundColor: String = "#FFFFFF",
    val backgroundImage: String? = null,
    val gridEnabled: Boolean = true,
    val gridSize: Int = 50,
    val devices: List<SchemeDevice> = emptyList()
)

data class SchemeDevice(
    val deviceId: Int,
    val x: Float,
    val y: Float,
    val rotation: Float = 0f,
    val scale: Float = 1f,
    val zIndex: Int = 0
)