package com.kipia.management.mobile.ui.components.scheme.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.kipia.management.mobile.data.entities.Device

fun DrawScope.drawDevice(
    device: Device,
    isSelected: Boolean,
    scale: Float = 1f
) {
    val baseSize = 60f
    val size = baseSize * scale

    val deviceColor = when (device.type) {
        "Датчик" -> Color(0xFF2196F3)
        "Контроллер" -> Color(0xFF4CAF50)
        "Исполнительное устройство" -> Color(0xFFFF9800)
        else -> Color(0xFF9C27B0)
    }

    // Основной прямоугольник
    drawRect(
        color = deviceColor,
        topLeft = Offset.Zero,
        size = Size(size, size)
    )

    // Если выделен - рисуем градиентную обводку через несколько слоев
    if (isSelected) {
        val pulse = ((System.currentTimeMillis() % 2000) / 2000f * 0.5f + 0.5f)

        // Внешняя обводка (полупрозрачная)
        drawRect(
            color = Color.White.copy(alpha = pulse * 0.3f),
            topLeft = Offset(-4f * scale, -4f * scale),
            size = Size(size + 8f * scale, size + 8f * scale),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f * scale)
        )

        // Основная светящаяся обводка
        drawRect(
            color = Color.Cyan.copy(alpha = pulse),
            topLeft = Offset(-2f * scale, -2f * scale),
            size = Size(size + 4f * scale, size + 4f * scale),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f * scale)
        )
    } else {
        // Обычная тонкая рамка
        drawRect(
            color = Color.Black.copy(alpha = 0.3f),
            topLeft = Offset.Zero,
            size = Size(size, size),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f * scale)
        )
    }

    // Минималистичный кружок
    drawCircle(
        color = Color.White.copy(alpha = 0.8f),
        radius = size / 8,
        center = Offset(size / 2, size / 2)
    )
}