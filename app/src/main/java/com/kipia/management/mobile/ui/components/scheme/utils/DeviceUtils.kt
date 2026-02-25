package com.kipia.management.mobile.ui.components.scheme.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import android.graphics.Paint
import com.kipia.management.mobile.data.entities.Device

fun DrawScope.drawDevice(device: Device, isSelected: Boolean) {
    // Рисуем устройство относительно текущей позиции (0,0)
    // Размер устройства
    val size = 60f

    // Основной цвет устройства по типу
    val deviceColor = when (device.type) {
        "Датчик" -> Color(0xFF2196F3) // Синий
        "Контроллер" -> Color(0xFF4CAF50) // Зеленый
        "Исполнительное устройство" -> Color(0xFFFF9800) // Оранжевый
        else -> Color(0xFF9C27B0) // Фиолетовый
    }

    // Основной прямоугольник
    drawRect(
        color = deviceColor,
        topLeft = Offset.Zero,
        size = Size(size, size)
    )

    // Обводка выделения
    if (isSelected) {
        drawRect(
            color = Color.White,
            topLeft = Offset(-2f, -2f),
            size = Size(size + 4f, size + 4f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
    }

    // Черная рамка
    drawRect(
        color = Color.Black,
        topLeft = Offset.Zero,
        size = Size(size, size),
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
    )

    // Кружок внутри
    drawCircle(
        color = Color.White,
        radius = size / 6,
        center = Offset(size / 2, size / 2)
    )

    // ID прибора (для отладки)
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 20f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.nativeCanvas.drawText(
            device.id.toString(),
            size / 2,
            size / 2 + 8,
            paint
        )
    }
}