package com.kipia.management.mobile.data.database

import androidx.room.TypeConverter

class Converters {
    // ДЛЯ СОВМЕСТИМОСТИ с JavaFX - используем ТОТ ЖЕ разделитель ";"
    /**
     * List<String> → "photo1.jpg;photo2.jpg;photo3.jpg"
     * ТОТ ЖЕ ФОРМАТ ЧТО В JAVA FX!
     */
    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return list?.joinToString(";") ?: ""
    }

    /**
     * "photo1.jpg;photo2.jpg;photo3.jpg" → List<String>
     */
    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(";").filter { it.isNotBlank() }
    }
}