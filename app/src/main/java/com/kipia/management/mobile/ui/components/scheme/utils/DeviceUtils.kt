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
import kotlin.math.sqrt

/**
 * Иконный размер — точно как в JavaFX версии (DEFAULT_ICON_SIZE = 45.0).
 * В канвасе будет масштабироваться через scale, поэтому базовый размер
 * задаём в «схемных» единицах, а не в пикселях экрана.
 */
private const val ICON_BASE_SIZE = 45f

// Цветовая схема иконки манометра
private val IconFace       = Color(0xFFF5F5F5)   // светло-серое лицо
private val IconRim        = Color(0xFF607D8B)   // синевато-серый ободок
private val IconText       = Color(0xFF37474F)   // тёмный текст/стрелки
private val IconShadow     = Color(0x40000000)   // полупрозрачная тень
private val SelectionColor = Color.Cyan
private val SelectionGlow  = Color(0x40_00FFFF)  // полупрозрачный ореол

/**
 * Рисует иконку манометра (классический манометр с ниппелем/штуцером снизу).
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

        // ── 1. Тень для объема ─────────────────────────────────────────────
        drawCircle(
            color = IconShadow,
            radius = r + 1f * scale,
            center = Offset(cx + 2f * scale, cy + 2f * scale)
        )

        // ── 2. Внешний ободок ──────────────────────────────────────────────
        drawCircle(
            color = IconRim,
            radius = r,
            center = Offset(cx, cy)
        )

        // ── 3. Белое лицо циферблата ───────────────────────────────────────
        drawCircle(
            color = IconFace,
            radius = r * 0.82f,
            center = Offset(cx, cy)
        )

        // ── 4. Внутренний ободок (блик) ────────────────────────────────────
        drawCircle(
            color = IconRim.copy(alpha = 0.5f),
            radius = r * 0.75f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.5f * scale)
        )

        // ── 5. Деления (12 основных и 4 дополнительных) ────────────────────
        val tickCount = 12
        val extraTickCount = 4  // дополнительные деления между основными

        // Основные деления (12 штук)
        val outerTick = r * 0.78f
        val innerTick = r * 0.62f
        val majorStroke = 1.5f * scale

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
                strokeWidth = majorStroke,
                cap = StrokeCap.Round
            )
        }

        // Дополнительные деления (между основными)
        val minorOuterTick = r * 0.73f
        val minorInnerTick = r * 0.65f
        val minorStroke = 0.8f * scale

        repeat(tickCount * extraTickCount) { i ->
            val angle = Math.toRadians((i * 360.0 / (tickCount * extraTickCount)) - 90.0)
            // Пропускаем основные деления
            if (i % extraTickCount != 0) {
                val x1 = cx + (minorOuterTick * cos(angle)).toFloat()
                val y1 = cy + (minorOuterTick * sin(angle)).toFloat()
                val x2 = cx + (minorInnerTick * cos(angle)).toFloat()
                val y2 = cy + (minorInnerTick * sin(angle)).toFloat()
                drawLine(
                    color = IconText.copy(alpha = 0.6f),
                    start = Offset(x1, y1),
                    end   = Offset(x2, y2),
                    strokeWidth = minorStroke,
                    cap = StrokeCap.Round
                )
            }
        }

        // ── 6. Цифровые метки (0, 30, 60, 90, 120, 150) ───────────────────
        // В реальном коде здесь можно добавить текст, но для простоты оставляем только деления

        // ── 7. Стрелка (динамическая, но пока фиксированная на 2 часа) ────
        val arrowAngle = Math.toRadians(60.0 - 90.0) // 60° = ~2 часа
        val arrowLen   = r * 0.62f
        val arrowEndX  = cx + (arrowLen * cos(arrowAngle)).toFloat()
        val arrowEndY  = cy + (arrowLen * sin(arrowAngle)).toFloat()

        // Тень стрелки
        drawLine(
            color = IconShadow,
            start = Offset(cx + 1f * scale, cy + 1f * scale),
            end   = Offset(arrowEndX + 1f * scale, arrowEndY + 1f * scale),
            strokeWidth = 2.5f * scale,
            cap = StrokeCap.Round
        )

        // Основная стрелка
        drawLine(
            color = Color(0xFFD32F2F),
            start = Offset(cx, cy),
            end   = Offset(arrowEndX, arrowEndY),
            strokeWidth = 2.2f * scale,
            cap = StrokeCap.Round
        )

        // Контрастная окантовка стрелки
        drawLine(
            color = Color(0xFFFFCDD2),
            start = Offset(cx, cy),
            end   = Offset(arrowEndX, arrowEndY),
            strokeWidth = 0.8f * scale,
            cap = StrokeCap.Round
        )

        // ── 8. Центральная точка с бликом ──────────────────────────────────
        drawCircle(
            color = IconText,
            radius = 2.5f * scale,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.7f),
            radius = 1.2f * scale,
            center = Offset(cx - 0.8f * scale, cy - 0.8f * scale)
        )

        // ── 9. ШТУЦЕР (НИППЕЛЬ) - улучшенная версия ───────────────────────
        val fittingWidth = size * 0.22f
        val fittingHeight = size * 0.18f
        val fittingX = cx - fittingWidth / 2f
        val fittingY = size - fittingHeight

        // Основная часть штуцера
        drawRect(
            color = IconRim,
            topLeft = Offset(fittingX, fittingY),
            size = Size(fittingWidth, fittingHeight)
        )

        // Блик на штуцере
        drawRect(
            color = Color.White.copy(alpha = 0.3f),
            topLeft = Offset(fittingX + 2f * scale, fittingY + 2f * scale),
            size = Size(fittingWidth - 4f * scale, fittingHeight * 0.4f)
        )

        // Нижняя закругленная часть (ниппель)
        val nippleWidth = fittingWidth * 0.6f
        val nippleHeight = fittingHeight * 0.5f
        val nippleX = cx - nippleWidth / 2f
        val nippleY = size - nippleHeight * 0.8f

        drawRoundRect(
            color = IconRim,
            topLeft = Offset(nippleX, nippleY),
            size = Size(nippleWidth, nippleHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(nippleWidth / 2f, nippleHeight / 2f)
        )

        // Отверстие в ниппеле (технологическое)
        val holeSize = nippleWidth * 0.3f
        drawCircle(
            color = Color(0xFF1A1A1A),
            radius = holeSize / 2f,
            center = Offset(cx, size - nippleHeight * 0.4f),
            style = Stroke(width = 1f * scale)
        )

        // ── 10. Выделение (если прибор выбран) ────────────────────────────
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
                style  = Stroke(width = 1.8f * scale)
            )
        }
    }
}