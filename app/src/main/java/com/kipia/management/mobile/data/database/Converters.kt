package com.kipia.management.mobile.data.database

import androidx.room.TypeConverter

/**
 * Конвертер для преобразования списков строк в строку и обратно.
 */
class Converters {
    // ДЛЯ СОВМЕСТИМОСТИ с JavaFX - используем ТОТ ЖЕ разделитель ";"
    /**
     * Метод для конвертации списка строк в строку.
     * List<String> → "photo1.jpg;photo2.jpg;photo3.jpg"
     * ТОТ ЖЕ ФОРМАТ ЧТО В JAVA FX!
     */
    @TypeConverter // @TypeConverter дает понять Room, что это конвертер
    fun fromStringList(list: List<String>?): String {
        return list?.joinToString(";") ?: ""
    }

    /**
     * Метод для конвертации строки в список строк.
     * "photo1.jpg;photo2.jpg;photo3.jpg" → List<String>
     */
    @TypeConverter // @TypeConverter дает понять Room, что это конвертер
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(";").filter { it.isNotBlank() }
    }
}