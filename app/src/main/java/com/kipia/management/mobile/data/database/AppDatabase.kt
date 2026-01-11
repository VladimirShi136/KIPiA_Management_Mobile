package com.kipia.management.mobile.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kipia.management.mobile.data.dao.*
import com.kipia.management.mobile.data.entities.*

@Database(
    entities = [Device::class, Scheme::class, DeviceLocation::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun deviceDao(): DeviceDao
    abstract fun schemeDao(): SchemeDao
    abstract fun deviceLocationDao(): DeviceLocationDao
}