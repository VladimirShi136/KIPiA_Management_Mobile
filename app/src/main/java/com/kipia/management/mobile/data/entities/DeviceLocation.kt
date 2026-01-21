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
    val x: Float,

    @ColumnInfo(name = "y")
    val y: Float,

    @ColumnInfo(name = "rotation")
    val rotation: Float = 0f
)