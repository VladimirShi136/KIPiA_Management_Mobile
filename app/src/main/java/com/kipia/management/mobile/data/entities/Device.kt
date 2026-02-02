package com.kipia.management.mobile.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kipia.management.mobile.ui.theme.DeviceStatus


/**
 * Модель устройства
 */
@Entity(tableName = "devices")
data class Device(
    // идентификатор
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // тип прибора
    @ColumnInfo(name = "type")
    val type: String,

    // название
    @ColumnInfo(name = "name")
    val name: String?,

    // производитель
    @ColumnInfo(name = "manufacturer")
    val manufacturer: String?,

    // инвентарный номер
    @ColumnInfo(name = "inventory_number")
    val inventoryNumber: String,

    // год производства
    @ColumnInfo(name = "year")
    val year: Int?,

    // предел измерения
    @ColumnInfo(name = "measurement_limit")
    val measurementLimit: String?,

    // класс точности
    @ColumnInfo(name = "accuracy_class")
    val accuracyClass: Double?,

    // местоположение
    @ColumnInfo(name = "location")
    val location: String,

    // номер крана
    @ColumnInfo(name = "valve_number")
    val valveNumber: String?,

    // статус
    @ColumnInfo(name = "status")
    val status: String = "В работе",

    // дополнительная информация
    @ColumnInfo(name = "additional_info")
    val additionalInfo: String?,

    @ColumnInfo(name = "photos")
    val photos: List<String> = emptyList()  // Room сам конвертирует через TypeConverter
) {
    companion object {
        // Статусы из единого источника
        val STATUSES = DeviceStatus.ALL_STATUSES

        fun createEmpty(): Device = Device(
            type = "",
            name = null,
            manufacturer = null,
            inventoryNumber = "",
            year = null,
            measurementLimit = null,
            accuracyClass = null,
            location = "",
            valveNumber = null,
            status = "В работе",
            additionalInfo = null,
            photos = emptyList()
        )
    }

    fun getDisplayName(): String {
        return name ?: "$type №$inventoryNumber"
    }

    fun addPhoto(fileName: String): Device {
        return this.copy(photos = photos + fileName)
    }

    fun removePhoto(fileName: String): Device {
        return this.copy(photos = photos - fileName)
    }
}