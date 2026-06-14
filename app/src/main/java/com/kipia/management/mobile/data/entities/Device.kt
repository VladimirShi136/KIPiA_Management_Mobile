package com.kipia.management.mobile.data.entities

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.kipia.management.mobile.ui.theme.DeviceStatus


/**
 * Модель устройства.
 * inventory_number является уникальным ключом для синхронизации.
 */
@Immutable // Аннотация для оптимизации. Это означает, что класс является неизменяемым.
@Entity(
    tableName = "devices",
    indices = [Index(value = ["inventory_number"], unique = true)]
)
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

    // фото
    @ColumnInfo(name = "photos")
    val photos: List<String> = emptyList(),

    // время обновления
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),  // timestamp в миллисекундах

    // время последней успешной синхронизации
    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = 0, // время последней успешной синхронизации

    // время удаления
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long = 0 // 0 если активен, >0 если удален
) {


    companion object {
        val STATUSES = DeviceStatus.ALL_STATUSES

        /**
         * Метод для создания пустого устройства.
         */
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

    /**
     * Метод для проверки, является ли устройство удаленным.
     */
    fun isDeleted(): Boolean = deletedAt > 0

    /**
     * Метод для получения отображаемого имени устройства.
     */
    fun getDisplayName(): String {
        return name ?: "$type №$inventoryNumber"
    }

    /**
     * Метод для добавления фото к устройству.
     */
    fun addPhoto(fileName: String): Device {
        return this.copy(photos = photos + fileName).withUpdatedNow()
    }

    /**
     * Метод для удаления фото у устройства.
     */
    fun removePhoto(fileName: String): Device {
        return this.copy(photos = photos - fileName).withUpdatedNow()
    }

    /**
     * Метод для обновления устройства.
     */
    fun withUpdatedNow(): Device {
        return this.copy(updatedAt = System.currentTimeMillis())
    }
}