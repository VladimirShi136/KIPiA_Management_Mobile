package com.kipia.management.mobile.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "device_locations",
    primaryKeys = ["device_id", "scheme_id"],
    foreignKeys = [
        ForeignKey(
            entity = Device::class,
            parentColumns = ["id"],
            childColumns = ["device_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Scheme::class,
            parentColumns = ["id"],
            childColumns = ["scheme_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices =[
        Index(value = ["device_id"]),
        Index(value = ["scheme_id"])
    ]
)
data class DeviceLocation(
    @ColumnInfo(name = "device_id")
    val deviceId: Int,

    @ColumnInfo(name = "scheme_id")
    val schemeId: Int,

    @ColumnInfo(name = "x")
    val x: Double, // Используем Double для совместимости с JavaFX REAL

    @ColumnInfo(name = "y")
    val y: Double,

    @ColumnInfo(name = "rotation")
    val rotation: Double = 0.0,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = 0,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long = 0
) {
    fun isDeleted(): Boolean = deletedAt > 0

    fun withUpdatedNow(): DeviceLocation {
        return this.copy(updatedAt = System.currentTimeMillis())
    }

    fun asDeleted(): DeviceLocation {
        val now = System.currentTimeMillis()
        return this.copy(updatedAt = now, deletedAt = now)
    }
}