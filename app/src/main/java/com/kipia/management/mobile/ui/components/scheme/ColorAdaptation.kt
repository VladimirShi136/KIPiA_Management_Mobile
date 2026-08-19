package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.ui.graphics.Color
import kotlin.math.max
import kotlin.math.min

/**
 * Адаптирует цвет для отображения на темном фоне.
 * Сохраняет оригинальный оттенок, но увеличивает яркость для видимости.
 */
fun Color.adaptForDarkTheme(): Color {
    // Преобразуем RGB в HSB (Hue, Saturation, Brightness)
    val floatArray = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (this.red * 255).toInt(),
        (this.green * 255).toInt(),
        (this.blue * 255).toInt(),
        floatArray
    )

    val hue = floatArray[0]
    val saturation = floatArray[1]
    var brightness = floatArray[2]

    // Если цвет слишком темный (brightness < 40%), осветляем его
    // Если цвет имеет низкую насыщенность (серый), делаем его светлее
    brightness = when {
        brightness < 0.4f -> brightness + 0.4f  // Осветляем темные цвета
        saturation < 0.1f -> max(brightness, 0.6f)  // Серые цвета делаем светлее
        else -> max(brightness, 0.5f)  // Остальные цвета делаем видимыми
    }

    // Преобразуем HSB обратно в RGB
    val adaptedArgb = android.graphics.Color.HSVToColor(
        (this.alpha * 255).toInt(),
        floatArrayOf(hue, saturation, brightness)
    )

    return Color(adaptedArgb)
}

/**
 * Проверяет, является ли цвет "прозрачным" для целей отрисовки
 */
fun Color.isEffectivelyTransparent(): Boolean {
    return this == Color.Transparent || this.alpha < 0.1f
}

/**
 * Делает цвет темнее на определенный процент
 */
fun Color.darkenBy(percent: Float): Color {
    val floatArray = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (this.red * 255).toInt(),
        (this.green * 255).toInt(),
        (this.blue * 255).toInt(),
        floatArray
    )

    floatArray[2] = floatArray[2] * (1 - percent.coerceIn(0f, 1f))

    val darkened = android.graphics.Color.HSVToColor(
        (this.alpha * 255).toInt(),
        floatArray
    )

    return Color(darkened)
}

/**
 * Осветляет цвет на определенный процент
 */
fun Color.lightenBy(percent: Float): Color {
    val floatArray = FloatArray(3)
    android.graphics.Color.RGBToHSV(
        (this.red * 255).toInt(),
        (this.green * 255).toInt(),
        (this.blue * 255).toInt(),
        floatArray
    )

    floatArray[2] = floatArray[2] + (1 - floatArray[2]) * percent.coerceIn(0f, 1f)

    val lightened = android.graphics.Color.HSVToColor(
        (this.alpha * 255).toInt(),
        floatArray
    )

    return Color(lightened)
}

