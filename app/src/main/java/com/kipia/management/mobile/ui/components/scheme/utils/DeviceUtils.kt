package com.kipia.management.mobile.ui.components.scheme.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb

/**
 * Иконный размер — базовый размер для отрисовки на схеме.
 */
private const val ICON_BASE_SIZE = 45f
private const val TEXT_GAP = 5f

private val SelectionColor = Color.Cyan
private val SelectionGlow  = Color(0x40_00FFFF)

/**
 * Рисует иконку прибора, используя переданный Painter (вектор или растр).
 *
 * Реализация соответствует Android-логике:
 * - ViewGroup высотой 50px (45px иконка + 5px текст сверху)
 * - Иконка внизу ViewGroup (gravity BOTTOM)
 * - Текст сверху ViewGroup (gravity TOP)
 * - Pivot point на границе иконки и текста: (22.5, 5) — между иконкой и текстом
 * - Текст не поворачивается (компенсация через обратный поворот)
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
    val iconSize = ICON_BASE_SIZE * scale       // 45 * scale
    val textGap = TEXT_GAP * scale               // 5 * scale
    val pivotX = iconSize / 2f                    // 22.5 * scale
    val pivotY = textGap                          // 5 * scale — граница между текстом (сверху) и иконкой (снизу)
    val iconCenterY = textGap + iconSize / 2f     // центр иконки для выделения

    // Иконка: рисуем с поворотом вокруг pivot (pivotX, pivotY)
    // Иконка размещена внизу ViewGroup: смещение по Y = textGap
    rotate(degrees = rotationDeg, pivot = Offset(pivotX, pivotY)) {
        translate(left = 0f, top = textGap) {
            with(painter) {
                draw(size = Size(iconSize, iconSize))
            }
        }

        if (isSelected) {
            val r = iconSize / 2f
            drawCircle(
                color  = SelectionGlow,
                radius = r + 5f * scale,
                center = Offset(pivotX, iconCenterY),
                style  = Stroke(width = 6f * scale)
            )
            drawCircle(
                color  = SelectionColor,
                radius = r + 2f * scale,
                center = Offset(pivotX, iconCenterY),
                style  = Stroke(width = 1.8f * scale)
            )
        }
    }

    // Текст: рисуем НЕ поворачиваемым, с компенсацией поворота
    if (!valveText.isNullOrBlank()) {
        drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                color = valveTextColor.toArgb()
                textSize = 12f * scale
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
            }

            // Позиция текста в локальных координатах (без поворота):
            // Текст по центру X (pivotX), сверху иконки (y = textGap / 2)
            val localTextX = pivotX
            val localTextY = textGap / 2f

            // Смещение от pivot до текста
            val dx = localTextX - pivotX  // = 0
            val dy = localTextY - pivotY  // = textGap/2 - iconSize/2

            // Применяем поворот ViewGroup к позиции текста
            // (текст рисуется горизонтальным, но его позиция вращается вместе с иконкой)
            val radians = Math.toRadians(rotationDeg.toDouble()).toFloat()
            val cos = kotlin.math.cos(radians)
            val sin = kotlin.math.sin(radians)

            val rotatedDx = dx * cos - dy * sin
            val rotatedDy = dx * sin + dy * cos

            val textX = pivotX + rotatedDx
            val textBaselineY = pivotY + rotatedDy

            canvas.nativeCanvas.drawText(valveText, textX, textBaselineY, paint)
        }
    }
}
