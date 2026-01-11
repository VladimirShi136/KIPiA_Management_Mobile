package com.kipia.management.mobile.data.entities

import androidx.compose.ui.graphics.Color
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson


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


    @ColumnInfo(name = "photo_path")
    val photoPath: String?,  // Основное фото

    @ColumnInfo(name = "photos")
    val photos: String?  // JSON с путями к фото
) {
    companion object {
        // Статусы из вашего кода
        val STATUSES = listOf(
            "В работе",
            "На ремонте",
            "Списан",
            "В резерве"
        )

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
            photoPath = null,
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

    fun getStatusColorHex(): String {
        return when (status) {
            "В работе" -> "#4CAF50"      // Зеленый
            "На ремонте" -> "#FF9800"    // Оранжевый
            "Списан" -> "#F44336"        // Красный
            "В резерве" -> "#9E9E9E"     // Серый
            else -> "#757575"
        }
    }

    fun getMainPhoto(): String? {
        return photoPath ?: getPhotoList().firstOrNull()
    }

    fun hasPhotos(): Boolean {
        return !photoPath.isNullOrBlank() || !photos.isNullOrBlank()
    }

    fun isValid(): Boolean {
        return type.isNotBlank() &&
                inventoryNumber.isNotBlank() &&
                location.isNotBlank()
    }

    fun copyWithField(
        fieldName: String,
        value: Any?
    ): Device {
        return when (fieldName) {
            "type" -> copy(type = value as String)
            "name" -> copy(name = value as? String)
            "manufacturer" -> copy(manufacturer = value as? String)
            "inventoryNumber" -> copy(inventoryNumber = value as String)
            "year" -> copy(year = value as? Int)
            "measurementLimit" -> copy(measurementLimit = value as? String)
            "accuracyClass" -> copy(accuracyClass = value as? Double)
            "location" -> copy(location = value as String)
            "valveNumber" -> copy(valveNumber = value as? String)
            "status" -> copy(status = value as String)
            "additionalInfo" -> copy(additionalInfo = value as? String)
            "photoPath" -> copy(photoPath = value as? String)
            else -> this
        }
    }

    fun getPhotoCount(): Int {
        return getPhotoList().size + if (photoPath != null) 1 else 0
    }

    fun getAllPhotos(): List<String> {
        val allPhotos = mutableListOf<String>()

        photoPath?.let { allPhotos.add(it) }
        allPhotos.addAll(getPhotoList())

        return allPhotos.distinct() // На случай дублирования
    }

    fun removePhoto(photoPathToRemove: String): Device {
        return if (this.photoPath == photoPathToRemove) {
            // Удаляем основное фото
            this.copy(photoPath = null)
        } else {
            // Удаляем из дополнительных фото
            val updatedPhotos = getPhotoList().toMutableList().apply {
                remove(photoPathToRemove)
            }
            setPhotoList(updatedPhotos)
        }
    }

    fun Device.getStatusColor(): Color {
        return when (status) {
            "В работе" -> Color(0xFF4CAF50)      // Зеленый
            "На ремонте" -> Color(0xFFFF9800)    // Оранжевый
            "Списан" -> Color(0xFFF44336)        // Красный
            "В резерве" -> Color(0xFF9E9E9E)     // Серый
            else -> Color.Gray
        }
    }

    fun Device.getStatusColorCompose(): androidx.compose.ui.graphics.Color {
        return androidx.compose.ui.graphics.Color(
            android.graphics.Color.parseColor(getStatusColorHex())
        )
    }

}