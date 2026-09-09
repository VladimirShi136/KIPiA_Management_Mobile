package com.kipia.management.mobile.ui.components.scheme.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb

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
 * @param valveText     номер крана для отображения под иконкой
 * @param valveTextColor цвет текста номера крана
 */
fun DrawScope.drawDevice(
    painter: Painter,
    isSelected: Boolean,
    scale: Float = 1f,
    rotationDeg: Float = 0f,
    valveText: String? = null,
    valveTextColor: Color = Color.Black
) {
    val size = ICON_BASE_SIZE * scale
    val cx = size / 2f
    val cy = size / 2f

    rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {
        with(painter) {
            draw(size = Size(size, size))
        }
    }

    if (isSelected) {
        val r = size / 2f
        drawCircle(
            color  = SelectionGlow,
            radius = r + 5f * scale,
            center = Offset(cx, cy),
            style  = Stroke(width = 6f * scale)
        )
        drawCircle(
            color  = SelectionColor,
            radius = r + 2f * scale,
            center = Offset(cx, cy),
            style  = Stroke(width = 1.8f * scale)
        )
    }

    if (!valveText.isNullOrBlank()) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                color = valveTextColor.toArgb()
                textSize = 11f * scale
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }

            val textBounds = android.graphics.Rect()
            paint.getTextBounds(valveText, 0, valveText.length, textBounds)
            val textY = -textBounds.bottom - 2f * scale

            canvas.nativeCanvas.drawText(valveText, cx, textY, paint)
        }
    }
}
