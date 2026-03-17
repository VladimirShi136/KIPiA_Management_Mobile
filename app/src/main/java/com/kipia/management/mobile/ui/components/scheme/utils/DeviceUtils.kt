package com.kipia.management.mobile.ui.components.scheme.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin

/**
 * Иконный размер — точно как в JavaFX версии (DEFAULT_ICON_SIZE = 45.0).
 * В канвасе будет масштабироваться через scale, поэтому базовый размер
 * задаём в «схемных» единицах, а не в пикселях экрана.
 */
private const val ICON_BASE_SIZE = 45f

// Цветовая схема иконки манометра (нейтральная, как PNG в JavaFX)
private val IconFace       = Color(0xFFF5F5F5)   // светло-серое лицо
private val IconRim        = Color(0xFF607D8B)   // синевато-серый ободок
private val IconText       = Color(0xFF37474F)   // тёмный текст/стрелки
private val SelectionColor = Color.Cyan
private val SelectionGlow  = Color(0x40_00FFFF)  // полупрозрачный ореол

/**
 * Рисует иконку прибора (манометр, аналог manometer.png из JavaFX) в DrawScope.
 *
 * Вызывается внутри `withTransform { translate(screenX, screenY) }`,
 * поэтому рисует начиная с точки (0, 0).
 *
 * @param isSelected    выделен ли прибор
 * @param scale         масштаб канваса (чтобы толщины линий были стабильны)
 * @param rotationDeg   угол поворота иконки в градусах: 0 / 90 / 180 / 270
 */
fun DrawScope.drawDevice(
    isSelected: Boolean,
    scale: Float = 1f,
    rotationDeg: Float = 0f
) {
    val size = ICON_BASE_SIZE * scale
    val cx = size / 2f
    val cy = size / 2f
    val r  = size / 2f

    // Все вращение применяем вокруг центра иконки
    rotate(degrees = rotationDeg, pivot = Offset(cx, cy)) {

        // ── 1. Внешний ободок ──────────────────────────────────────────────
        drawCircle(
            color = IconRim,
            radius = r,
            center = Offset(cx, cy)
        )

        // ── 2. Белое лицо циферблата ───────────────────────────────────────
        drawCircle(
            color = IconFace,
            radius = r * 0.80f,
            center = Offset(cx, cy)
        )

        // ── 3. Деления (12 коротких черточек по кругу) ─────────────────────
        val tickCount  = 12
        val outerTick  = r * 0.78f
        val innerTick  = r * 0.65f
        val strokeW    = 1.2f * scale
        repeat(tickCount) { i ->
            val angle = Math.toRadians((i * 360.0 / tickCount) - 90.0)
            val x1 = cx + (outerTick * cos(angle)).toFloat()
            val y1 = cy + (outerTick * sin(angle)).toFloat()
            val x2 = cx + (innerTick * cos(angle)).toFloat()
            val y2 = cy + (innerTick * sin(angle)).toFloat()
            drawLine(
                color = IconText,
                start = Offset(x1, y1),
                end   = Offset(x2, y2),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        // ── 4. Стрелка (указывает на ~2 часа, как у типичного манометра) ──
        val arrowAngle = Math.toRadians(60.0 - 90.0) // 60° по часовой = ~2 часа
        val arrowLen   = r * 0.58f
        val arrowEndX  = cx + (arrowLen * cos(arrowAngle)).toFloat()
        val arrowEndY  = cy + (arrowLen * sin(arrowAngle)).toFloat()

        drawLine(
            color = Color(0xFFD32F2F),  // красная стрелка
            start = Offset(cx, cy),
            end   = Offset(arrowEndX, arrowEndY),
            strokeWidth = 2f * scale,
            cap = StrokeCap.Round
        )

        // ── 5. Центральная точка ──────────────────────────────────────────
        drawCircle(
            color = IconText,
            radius = 2.2f * scale,
            center = Offset(cx, cy)
        )

        // ── 6. Штуцер снизу (прямоугольный выступ) ────────────────────────
        val fitW = size * 0.18f
        val fitH = size * 0.14f
        drawRect(
            color = IconRim,
            topLeft = Offset(cx - fitW / 2f, size - fitH),
            size    = Size(fitW, fitH)
        )

        // ── 7. Выделение ───────────────────────────────────────────────────
        if (isSelected) {
            // Ореол (широкая полупрозрачная обводка)
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
                style  = Stroke(width = 1.5f * scale)
            )
        }
    }
}
