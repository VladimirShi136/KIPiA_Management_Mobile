package com.kipia.management.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Цвета статусов прибора
object DeviceStatusColors {
    // Основные цвета
    val Working = Color(0xFF4CAF50)      // Зеленый
    val Storage = Color(0xFFFF9800)      // Оранжевый
    val Lost = Color(0xFF9E9E9E)         // Серый
    val Broken = Color(0xFFF44336)       // Красный

    // Фоновые цвета (более светлые версии)
    val WorkingBackground = Color(0xFFE8F5E9)
    val StorageBackground = Color(0xFFFFF3E0)
    val LostBackground = Color(0xFFF5F5F5)
    val BrokenBackground = Color(0xFFFFEBEE)

    // Цвета текста
    val WorkingText = Color(0xFF2E7D32)
    val StorageText = Color(0xFFEF6C00)
    val LostText = Color(0xFF616161)
    val BrokenText = Color(0xFFD32F2F)
}

// Статусы прибора как enum
enum class DeviceStatus(val displayName: String, val color: Color, val backgroundColor: Color, val textColor: Color) {
    WORKING("В работе", DeviceStatusColors.Working, DeviceStatusColors.WorkingBackground, DeviceStatusColors.WorkingText),
    STORAGE("Хранение", DeviceStatusColors.Storage, DeviceStatusColors.StorageBackground, DeviceStatusColors.StorageText),
    LOST("Утерян", DeviceStatusColors.Lost, DeviceStatusColors.LostBackground, DeviceStatusColors.LostText),
    BROKEN("Испорчен", DeviceStatusColors.Broken, DeviceStatusColors.BrokenBackground, DeviceStatusColors.BrokenText);

    companion object {
        fun fromString(status: String): DeviceStatus {
            return when (status) {
                "В работе" -> WORKING
                "Хранение" -> STORAGE
                "Утерян" -> LOST
                "Испорчен" -> BROKEN
                else -> WORKING // По умолчанию
            }
        }

        val ALL_STATUSES = listOf(
            "В работе",
            "Хранение",
            "Утерян",
            "Испорчен"
        )
    }
}