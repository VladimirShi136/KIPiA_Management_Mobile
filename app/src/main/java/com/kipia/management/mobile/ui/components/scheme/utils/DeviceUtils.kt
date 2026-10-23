package com.kipia.management.mobile.ui.components.scheme.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
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
 * Рисует иконку прибора.
 *
 * Геометрия (согласована с JavaFX-версией и с внешним позиционированием
 * в DeviceLayer.kt):
 * - DeviceLayer.kt уже выставил origin этого DrawScope ровно в
 *   ЛЕВЫЙ ВЕРХНИЙ УГОЛ иконки (screenX - pivotX, screenY - pivotY).
 *   Поэтому здесь иконка рисуется от локальных координат (0, 0),
 *   БЕЗ дополнительного сдвига — раньше здесь был ещё translate(top = textGap),
 *   который задваивал тот же textGap, что уже вычтен снаружи, и из-за
 *   того, что это происходило ВНУТРИ rotate(...), задвоенный сдвиг
 *   поворачивался вместе с иконкой (проявлялось как смещение
 *   вниз/влево/вверх/вправо на 0°/90°/180°/270°).
 * - Pivot точка поворота — (pivotX, pivotY) = (22.5, 5) относительно
 *   левого верхнего угла иконки, т.е. на 5px "внутрь" иконки от её
 *   верхнего края — так же, как PIVOT_X/PIVOT_Y в JavaFX.
 * - Текст (номер крана) рисуется НАД иконкой, с базовой линией на
 *   -textGap относительно верхнего края иконки — так же, как
 *   valveText.setY(-5) в JavaFX.
 *
 * @param painter       объект для отрисовки иконки (загружается через painterResource)
 * @param isSelected    выделен ли прибор
 * @param scale         масштаб канваса
 * @param rotationDeg   угол поворота иконки в градусах
 * @param valveText     номер крана для отображения над иконкой
 * @param valveTextColor цвет текста номера крана
 */
fun DrawScope.drawDevice(
    painter: Painter,
    isSelected: Boolean,
    scale: Float = 1f,
    rotationDeg: Float = 0f,
    valveText: String? = null,
    valveTextColor: Color = Color.Black,
    deviceStatus: String = "В работе",
    wrenchPainter: Painter? = null
) {
    val iconSize = ICON_BASE_SIZE * scale       // 45 * scale
    val textGap = TEXT_GAP * scale               // 5 * scale
    val pivotX = iconSize / 2f                    // 22.5 * scale
    val pivotY = textGap                          // 5 * scale — точка поворота внутри иконки
    val iconCenterY = iconSize / 2f               // центр иконки для выделения (иконка теперь начинается с y=0)

    val isLost = deviceStatus == "Утерян"
    val isInRepair = deviceStatus == "В ремонте"

    val alpha = if (isLost) 0.4f else 1f

    // Иконка: рисуем с поворотом вокруг pivot (pivotX, pivotY).
    // Иконка занимает локальные координаты [0, iconSize] x [0, iconSize] —
    // БЕЗ дополнительного translate(top = textGap).
    rotate(degrees = rotationDeg, pivot = Offset(pivotX, pivotY)) {
        with(painter) {
            draw(size = Size(iconSize, iconSize), alpha = alpha)
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

    // Иконка гаечного ключа снизу для статуса "В ремонте"
    if (isInRepair && wrenchPainter != null) {
        val wrenchSize = 16f * scale
        val wrenchX = pivotX - wrenchSize / 2f
        val wrenchY = iconSize + 2f * scale
        rotate(degrees = rotationDeg, pivot = Offset(pivotX, pivotY)) {
            withTransform({ translate(wrenchX, wrenchY) }) {
                with(wrenchPainter) {
                    draw(size = Size(wrenchSize, wrenchSize))
                }
            }
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

            // Текст ВРАЩАЕТСЯ вместе с иконкой вокруг общего pivot (как в JavaFX,
            // где Rotate(angle, PIVOT_X, PIVOT_Y) применяется ко всей Group —
            // и иконке, и тексту сразу). Сам текст при этом остаётся
            // горизонтальным (глифы не поворачиваются), поворачивается только
            // его ПОЗИЦИЯ — это и есть "компенсация через обратный поворот".
            //
            // Локальная (нулевой угол) позиция текста: по центру X (pivotX).
            // Базовая линия НАД иконкой на -textGap — как valveText.setY(-5)
            // в JavaFX. НО: как и в JavaFX compensateTextRotation(), на 90°
            // и 270° отступ увеличивается вдвое (-10 вместо -5) — на этих
            // углах текст оказывается сбоку от иконки, и стандартного
            // зазора недостаточно, чтобы текст не наезжал на иконку.
            var normalizedAngle = rotationDeg % 360f
            if (normalizedAngle < 0f) normalizedAngle += 360f

            val localTextX = pivotX
            val localTextY = if (normalizedAngle == 90f || normalizedAngle == 270f) {
                -2f * textGap
            } else {
                -textGap
            }

            // Смещение от pivot до текста в неповёрнутом состоянии
            val dx = localTextX - pivotX   // = 0
            val dy = localTextY - pivotY   // = -textGap - textGap = -2*textGap

            // Поворачиваем это смещение на тот же угол, что и иконку
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