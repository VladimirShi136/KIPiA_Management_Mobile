package com.kipia.management.mobile.ui.components.scheme.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.painter.Painter

/**
 * Иконный размер — базовый размер для отрисовки на схеме.
 */
private const val ICON_BASE_SIZE = 45f

private val SelectionColor = Color.Cyan
private val SelectionGlow  = Color(0x40_00FFFF)

/**
 * Рисует иконку прибора, используя переданный Painter (вектор или растр).
 *
 * @param painter       объект для отрисовки иконки (загружается через painterResource)
 * @param isSelected    выделен ли прибор
 * @param scale         масштаб канваса
 * @param rotationDeg   угол поворота иконки в градусах
 */
fun DrawScope.drawDevice(
    painter: Painter,
    isSelected: Boolean,
    scale: Float = 1f,
    rotationDeg: Float = 0f
) {
    val size = ICON_BASE_SIZE * scale
    val cx = size / 2f
    val cy = size / 2f

    // Применяем вращение вокруг центра
    rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {
        // Отрисовка самой иконки
        with(painter) {
            draw(size = Size(size, size))
        }
    }

    // Отрисовка выделения (если прибор выбран)
    if (isSelected) {
        val r = size / 2f
        // Ореол
        drawCircle(
            color  = SelectionGlow,
            radius = r + 5f * scale,
            center = Offset(cx, cy),
            style  = Stroke(width = 6f * scale)
        )
        // Чёткая обводка
        drawCircle(
            color  = SelectionColor,
            radius = r + 2f * scale,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.8f * scale)
        )
    }
}
