package com.kipia.management.mobile.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kipia_management.db"  // Имя такое же как в десктопной версии
                )
                    .fallbackToDestructiveMigration()  // Для начала используем это
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
