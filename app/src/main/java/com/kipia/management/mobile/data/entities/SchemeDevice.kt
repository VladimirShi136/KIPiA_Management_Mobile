package com.kipia.management.mobile.data.entities

import com.google.gson.annotations.SerializedName

/**
 * Модель прибора на схеме.
 * Вынесена в отдельный файл для устранения ошибок переопределения.
 */
data class SchemeDevice(
    @SerializedName("deviceId") val deviceId: Int,
    @SerializedName("x") val x: Float,
    @SerializedName("y") val y: Float,
    @SerializedName("rotation") val rotation: Float = 0f,
    @SerializedName("zIndex") val zIndex: Int = 0
)
