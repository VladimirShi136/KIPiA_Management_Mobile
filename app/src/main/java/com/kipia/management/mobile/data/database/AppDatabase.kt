package com.kipia.management.mobile.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kipia.management.mobile.data.dao.DeviceDao
import com.kipia.management.mobile.data.dao.SchemeDao
import com.kipia.management.mobile.data.dao.DeviceLocationDao
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.data.entities.DeviceLocation

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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kipia_management.db" // Имя такое же как в десктопной версии!
                )
                    .fallbackToDestructiveMigration() // для начала, потом замените на миграции
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}