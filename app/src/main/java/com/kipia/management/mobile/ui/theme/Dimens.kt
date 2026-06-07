package com.kipia.management.mobile.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Единая система отступов и размеров для всего проекта.
 */
object Dimens {

    // ── Экранный padding ──────────────────────────────────────────────────────
    val screenPadding = 8.dp

    // ── Отступы между элементами ──────────────────────────────────────────────
    val spacingXXSmall = 2.dp   // микро
    val spacingXSmall  = 4.dp   // малый
    val spacingSmall   = 6.dp   // средний-малый (для диалогов)
    val spacingMedium  = 8.dp   // средний
    val spacingLarge   = 12.dp  // между кнопками в диалогах
    val spacingXLarge  = 16.dp  // крупный
    val spacingXXLarge = 24.dp  // очень крупный
    val spacingMax     = 32.dp  // максимальный

    // ── Внутренний padding карточек и диалогов ────────────────────────────────
    val cardPadding      = 10.dp
    val cardPaddingLarge = 16.dp
    val dialogPadding    = 20.dp

    // ── Скругления ────────────────────────────────────────────────────────────
    val cardRadius  = 12.dp
    val chipRadius  = 8.dp
    val thumbRadius = 8.dp

    // ── Иконки ────────────────────────────────────────────────────────────────
    val iconSizeXXSmall = 14.dp
    val iconSizeXSmall  = 16.dp
    val iconSizeSmall   = 18.dp
    val iconSizeMedium  = 24.dp
    val iconSizeLarge   = 32.dp
    val iconSizeXLarge  = 48.dp
    val iconSizeXXLarge = 64.dp

    // ── FAB и плавающие кнопки ────────────────────────────────────────────────
    val fabSize          = 48.dp
    val fabSizeLarge     = 56.dp
    val spacingFab       = 12.dp
    val fabBottomPadding = 24.dp

    // ── Специфичные для таблицы приборов ─────────────────────────────────────
    val tableCellPaddingHorizontal = 8.dp
    val tableHeaderHeight          = 40.dp
    val tableRowHeight             = 40.dp
    val tableStatusColWidth        = 100.dp
    val tableActionsColWidth       = 80.dp
}