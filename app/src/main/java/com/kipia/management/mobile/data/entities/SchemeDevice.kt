package com.kipia.management.mobile.data.entities

/**
 * Модель прибора на схеме.
 * Вынесена в отдельный файл для устранения ошибок переопределения.
 */
data class SchemeDevice(
    val deviceId: Int,
    val x: Float,
    val y: Float,
    val rotation: Float = 0f,
    val zIndex: Int = 0
)
