package com.kipia.management.mobile.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Единая система отступов и размеров для всего проекта.
 *
 * Использование:
 *   .padding(Dimens.screenPadding)
 *   Arrangement.spacedBy(Dimens.spacingMedium)
 *   shape = RoundedCornerShape(Dimens.cardRadius)
 */
object Dimens {

    // ── Экранный padding ──────────────────────────────────────────────────────
    // 8dp — оптимально для проекта с таблицей данных:
    // не съедает ширину колонок, но даёт визуальное "дыхание"
    val screenPadding = 8.dp

    // ── Отступы между элементами ──────────────────────────────────────────────
    val spacingXSmall  = 2.dp   // микро: между числом и подписью в StatItem
    val spacingSmall   = 4.dp   // малый: внутри компактных карточек, бейджи
    val spacingMedium  = 8.dp   // средний: между карточками/секциями на экране
    val spacingLarge   = 16.dp  // крупный: внутри карточек с контентом
    val spacingXLarge  = 24.dp  // очень крупный: между смысловыми блоками экрана
    val spacingXXLarge = 32.dp  // максимальный: пустые состояния, центрирование

    // ── Внутренний padding карточек ───────────────────────────────────────────
    val cardPadding      = 10.dp  // стандартный padding внутри Card (DeviceDetail, Settings)
    val cardPaddingLarge = 16.dp  // крупный padding внутри Card (фото-карточки, диалоги)

    // ── Скругления ────────────────────────────────────────────────────────────
    val cardRadius  = 12.dp  // основные карточки
    val chipRadius  = 8.dp   // бейджи, статус-чипы, фильтры
    val thumbRadius = 8.dp   // миниатюры фото

    // ── Иконки ────────────────────────────────────────────────────────────────
    val iconSizeXSmall  = 16.dp  // мелкие иконки в тексте/кнопках
    val iconSizeSmall   = 18.dp  // иконки в кнопках с текстом
    val iconSizeMedium  = 24.dp  // стандартные иконки
    val iconSizeLarge   = 32.dp  // крупные иконки в списках
    val iconSizeXLarge  = 48.dp  // FAB, иконки пустых состояний
    val iconSizeXXLarge = 64.dp  // большие иконки пустых состояний

    // ── FAB и плавающие кнопки ────────────────────────────────────────────────
    val fabSize          = 48.dp  // стандартный FAB
    val fabSizeLarge     = 56.dp  // акцентный FAB (сброс трансформации)
    val spacingFab       = 12.dp  // между FAB-кнопками
    val fabBottomPadding = 24.dp  // отступ снизу для панели FAB

    // ── Специфичные для таблицы приборов ─────────────────────────────────────
    val tableCellPaddingHorizontal = 8.dp
    val tableHeaderHeight          = 40.dp
    val tableRowHeight             = 40.dp
    val tableStatusColWidth        = 100.dp
    val tableActionsColWidth       = 80.dp
}