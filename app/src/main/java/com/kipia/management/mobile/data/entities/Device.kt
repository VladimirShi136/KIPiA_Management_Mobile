package com.kipia.management.mobile.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
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
    val photos: String?  // JSON с путями к фото
) {
    companion object {
        // Статусы из единого источника
        val STATUSES = DeviceStatus.ALL_STATUSES

        // Типы приборов из вашего кода
        val TYPES = listOf(
            "Манометр",
            "Термометр",
            "Счетчик",
            "Клапан",
            "Задвижка",
            "Датчик",
            "Преобразователь",
            "Регулятор",
            "Другое"
        )

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
            photos = null
        )
    }

    fun getPhotoList(): List<String> {
        return if (photos.isNullOrBlank()) {
            emptyList()
        } else {
            try {
                Gson().fromJson(photos, Array<String>::class.java).toList()
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    fun setPhotoList(photoList: List<String>): Device {
        val json = if (photoList.isNotEmpty()) {
            Gson().toJson(photoList)
        } else {
            null
        }
        return this.copy(photos = json)
    }

    fun getDisplayName(): String {
        return name ?: "$type №$inventoryNumber"
    }
}