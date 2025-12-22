package com.kipia.management.mobile.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class Device(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "name")
    val name: String?,

    @ColumnInfo(name = "manufacturer")
    val manufacturer: String?,

    @ColumnInfo(name = "inventory_number")
    val inventoryNumber: String,

    @ColumnInfo(name = "year")
    val year: Int?,

    @ColumnInfo(name = "measurement_limit")
    val measurementLimit: String?,

    @ColumnInfo(name = "accuracy_class")
    val accuracyClass: Double?,

    @ColumnInfo(name = "location")
    val location: String,

    @ColumnInfo(name = "valve_number")
    val valveNumber: String?,

    @ColumnInfo(name = "status")
    val status: String = "В работе",

    @ColumnInfo(name = "additional_info")
    val additionalInfo: String?,

    @ColumnInfo(name = "photo_path")
    val photoPath: String?,  // Основное фото

    @ColumnInfo(name = "photos")
    val photos: String?  // JSON с путями к фото
)