package com.kipia.management.mobile.data.database

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    // Пример конвертера для Date, если будет нужно
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}