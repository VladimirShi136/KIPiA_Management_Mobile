package com.kipia.management.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import timber.log.Timber


// ===== ОСНОВНАЯ ПАЛИТРА ИЗ ИКОНКИ =====
object AppColors {
    // Основные цвета из иконки
    val Coral = Color(0xFFF58352)    // Основной акцент (Primary) - #F58352
    val Peach = Color(0xFFE2A58C)    // Вторичный акцент (Secondary) - #E2A58C
    val IceBlue = Color(0xFFDBECF0)  // Третичный/фон - #DBECF0
    val LightGrayBlue = Color(0xFFB6C0C9) // Нейтральный - #B6C0C9

    // Дополнительные из градации
    val DarkBlue = Color(0xFF465261).also {
        Timber.d("🎨 AppColors.DarkBlue: ${it.toHex()}")
    }     // Для шапки - #465261
    val MediumDarkGray = Color(0xFF6C7884).also {
        Timber.d("🎨 AppColors.MediumDarkGray: ${it.toHex()}")
    } // Для навигации - #6C7884
    val MediumGray = Color(0xFF848C9B)    // Для второстепенного текста - #848C9B
    val Pinkish = Color(0xFFE4BEBE)       // Розоватый (исправленный) - #E4BEBE

    // Производные цвета (для состояний)
    val CoralDark = Color(0xFFD2693B)      // Для dark theme primary
    val CoralLight = Color(0xFFFF9C7A)     // Для light theme primary container
    val PeachLight = Color(0xFFF5D1C2)     // Для secondary container
    val IceBlueDark = Color(0xFFA8C8D0)    // Более темный ледяной голубой
}

// ===== СИСТЕМА ЦВЕТОВ ДЛЯ ТЕМЫ =====
object SystemColors {
    // Основная палитра на базе цвета иконки
    val Primary = AppColors.Coral
    val Secondary = AppColors.Peach
    val Tertiary = AppColors.IceBlue
    val Neutral = AppColors.LightGrayBlue

    // Фоны
    val Background = Color.White
    val Surface = Color.White
    val SurfaceVariant = AppColors.LightGrayBlue.copy(alpha = 0.2f)

    // Текст
    val OnPrimary = Color.White
    val OnSecondary = Color.Black
    val OnBackground = AppColors.DarkBlue
    val OnSurface = AppColors.DarkBlue
    val OnSurfaceVariant = AppColors.MediumDarkGray

    // Outline
    val Outline = AppColors.MediumDarkGray.copy(alpha = 0.3f)
    val OutlineVariant = AppColors.LightGrayBlue.copy(alpha = 0.1f)

    // Состояния
    val Error = Color(0xFFD32F2F)
    val Success = Color(0xFF388E3C)
    val Warning = Color(0xFFFFA000)
    val Info = Color(0xFF1976D2)

    // ★★★★ ЦВЕТА ДЛЯ TOP APP BAR ★★★★
    object TopAppBar {
        // Светлая тема
        val LightBackground = AppColors.DarkBlue.also {
            Timber.d("🎨 TopAppBar.LightBackground инициализирован: ${it.toHex()}")
        }
        val LightContent = Color.White.also {
            Timber.d("🎨 TopAppBar.LightContent инициализирован: ${it.toHex()}")
        }
        val LightBorder = Color.White.copy(alpha = 0.8f)

        // Темная тема (можно настроить позже)
        val DarkBackground = Color(0xFF1E2A3A).also {
            Timber.d("🎨 TopAppBar.DarkBackground инициализирован: ${it.toHex()}")
        }
        val DarkContent = Color.White.also {
            Timber.d("🎨 TopAppBar.DarkContent инициализирован: ${it.toHex()}")
        }
        val DarkBorder = Color.White.copy(alpha = 0.8f)
    }

    // ★★★★ ЦВЕТА ДЛЯ BOTTOM NAVIGATION ★★★★
    object BottomNav {
        // Светлая тема
        val LightBackground = AppColors.MediumDarkGray.also {
            Timber.d("🎨 BottomNav.LightBackground инициализирован: ${it.toHex()}")
        }
        val LightSelectedText = Color.White
        val LightUnselectedText = Color.White.copy(alpha = 0.8f)
        val LightBorder = Color.White.copy(alpha = 0.3f)

        // Темная тема (можно настроить позже)
        val DarkBackground = Color(0xFF4A5568).also {
            Timber.d("BottomNav.DarkBackground инициализирован: ${it.toHex()}")
        }         // Для dark theme
        val DarkSelectedText = Color.White
        val DarkUnselectedText = Color.White.copy(alpha = 0.8f)
        val DarkBorder = Color.White.copy(alpha = 0.3f)
    }

    // ★★★★ ЦВЕТА ДЛЯ КНОПОК BOTTOM NAV ★★★★
    object BottomNavButtons {
        val Devices = AppColors.Coral          // #F58352
        val Photos = AppColors.Peach           // #E2A58C
        val Schemes = AppColors.IceBlue        // #DBECF0
        val Reports = AppColors.LightGrayBlue  // #B6C0C9

        // Текст на кнопках
        val TextOnCoral = Color.Black
        val TextOnPeach = Color.Black
        val TextOnIceBlue = Color.Black
        val TextOnGrayBlue = Color.Black
    }

    // ★★★★ ЦВЕТА ДЛЯ ИКОНОК ★★★★
    object Icons {
        val Primary = AppColors.Coral
        val Secondary = AppColors.Peach
        val Tertiary = AppColors.IceBlue
        val OnPrimary = Color.White
        val OnSecondary = Color.Black
        val OnSurface = AppColors.DarkBlue
        val OnSurfaceVariant = AppColors.MediumDarkGray
        val Disabled = AppColors.MediumGray.copy(alpha = 0.5f)
        val Error = Color(0xFFD32F2F)
    }
}

// ===== ЦВЕТА СТАТУСОВ (обновленные под Material 3) =====
object DeviceStatusColors {
    // Основные цвета (семантические)
    val Total = Color(0xFF1D5A73)
    val Working = Color(0xFF4CAF50)      // Success - зеленый
    val Storage = AppColors.Coral        // Primary (гармония с темой!)
    val Lost = AppColors.MediumGray      // Outline/neutral
    val Broken = Color(0xFFF44336)       // Error - красный

    // Контейнеры (light theme)
    val WorkingContainer = Color(0xFFE8F5E9)
    val StorageContainer = AppColors.PeachLight
    val LostContainer = AppColors.IceBlue.copy(alpha = 0.3f)
    val BrokenContainer = Color(0xFFFFEBEE)

    // Текст (onContainer colors)
    val WorkingText = Color(0xFF2E7D32)
    val StorageText = AppColors.CoralDark
    val LostText = AppColors.MediumGray
    val BrokenText = Color(0xFFD32F2F)
}

// Статусы прибора как enum (обновляем для использования в композе)
enum class DeviceStatus(
    val displayName: String,
    val color: Color,              // Основной цвет статуса
    val containerColor: Color,     // Цвет фона (контейнера)
    val textColor: Color           // Цвет текста на контейнере
) {
    WORKING("В работе",
        DeviceStatusColors.Working,
        DeviceStatusColors.WorkingContainer,
        DeviceStatusColors.WorkingText),

    STORAGE("Хранение",
        DeviceStatusColors.Storage,
        DeviceStatusColors.StorageContainer,
        DeviceStatusColors.StorageText),

    LOST("Утерян",
        DeviceStatusColors.Lost,
        DeviceStatusColors.LostContainer,
        DeviceStatusColors.LostText),

    BROKEN("Испорчен",
        DeviceStatusColors.Broken,
        DeviceStatusColors.BrokenContainer,
        DeviceStatusColors.BrokenText);

    companion object {
        fun fromString(status: String): DeviceStatus {
            return when (status) {
                "В работе" -> WORKING
                "Хранение" -> STORAGE
                "Утерян" -> LOST
                "Испорчен" -> BROKEN
                else -> WORKING
            }
        }

        val ALL_STATUSES = listOf(
            "В работе",
            "Хранение",
            "Утерян",
            "Испорчен"
        )
    }
}

// ===== ДОПОЛНИТЕЛЬНЫЕ ВСПОМОГАТЕЛЬНЫЕ ЦВЕТА =====
object UtilityColors {
    // Тени и elevation
    val ShadowLight = Color.Black.copy(alpha = 0.1f)
    val ShadowMedium = Color.Black.copy(alpha = 0.2f)
    val ShadowDark = Color.Black.copy(alpha = 0.3f)

    // Overlay и затемнения
    val ScrimLight = Color.Black.copy(alpha = 0.3f)
    val ScrimMedium = Color.Black.copy(alpha = 0.5f)
    val ScrimDark = Color.Black.copy(alpha = 0.7f)

    // Полупрозрачные
    val White10 = Color.White.copy(alpha = 0.1f)
    val White20 = Color.White.copy(alpha = 0.2f)
    val White50 = Color.White.copy(alpha = 0.5f)
    val White80 = Color.White.copy(alpha = 0.8f)

    val Black10 = Color.Black.copy(alpha = 0.1f)
    val Black20 = Color.Black.copy(alpha = 0.2f)
    val Black50 = Color.Black.copy(alpha = 0.5f)
    val Black80 = Color.Black.copy(alpha = 0.8f)

    // Градиенты (опционально)
    val GradientStart = AppColors.Coral
    val GradientEnd = AppColors.Peach
    val GradientBlueStart = AppColors.IceBlue
    val GradientBlueEnd = AppColors.LightGrayBlue
}

// ===== ФУНКЦИИ ДЛЯ РАБОТЫ С ЦВЕТАМИ =====

/**
 * Получить цвет для кнопки BottomNav по индексу
 */
@Composable
fun getBottomNavButtonColor(index: Int): Color {
    return when (index) {
        0 -> SystemColors.BottomNavButtons.Devices    // Devices
        1 -> SystemColors.BottomNavButtons.Photos     // Photos
        2 -> SystemColors.BottomNavButtons.Schemes    // Schemes
        3 -> SystemColors.BottomNavButtons.Reports    // Reports
        else -> SystemColors.Primary
    }
}

/**
 * Получить цвет текста для кнопки BottomNav
 */
@Composable
fun getBottomNavTextColor(backgroundColor: Color): Color {
    return when (backgroundColor) {
        SystemColors.BottomNavButtons.Devices -> SystemColors.BottomNavButtons.TextOnCoral
        SystemColors.BottomNavButtons.Photos -> SystemColors.BottomNavButtons.TextOnPeach
        SystemColors.BottomNavButtons.Schemes -> SystemColors.BottomNavButtons.TextOnIceBlue
        SystemColors.BottomNavButtons.Reports -> SystemColors.BottomNavButtons.TextOnGrayBlue
        else -> MaterialTheme.colorScheme.onPrimary
    }
}

/**
 * Получить цвета для TopAppBar в зависимости от темы
 */
@Composable
fun getTopAppBarColors(isDarkTheme: Boolean = isSystemInDarkTheme()): Pair<Color, Color> {
    Timber.d("🎨 getTopAppBarColors() вызван: isDarkTheme=$isDarkTheme")

    val colors = if (isDarkTheme) {
        Timber.d("🎨 Используем DARK цвета: bg=${SystemColors.TopAppBar.DarkBackground.toHex()}, content=${SystemColors.TopAppBar.DarkContent.toHex()}")
        Pair(SystemColors.TopAppBar.DarkBackground, SystemColors.TopAppBar.DarkContent)
    } else {
        Timber.d("🎨 Используем LIGHT цвета: bg=${SystemColors.TopAppBar.LightBackground.toHex()}, content=${SystemColors.TopAppBar.LightContent.toHex()}")
        Pair(SystemColors.TopAppBar.LightBackground, SystemColors.TopAppBar.LightContent)
    }

    Timber.d("🎨 Выбран цвет TopAppBar: ${colors.first.toHex()}")
    return colors
}

/**
 * Получить цвета для BottomNav в зависимости от темы
 */
@Composable
fun getBottomNavColors(isDarkTheme: Boolean = isSystemInDarkTheme()): BottomNavColors {
    Timber.d("🎨 getBottomNavColors() вызван: isDarkTheme=$isDarkTheme")

    val colors = if (isDarkTheme) {
        BottomNavColors(
            background = SystemColors.BottomNav.DarkBackground,
            selectedText = SystemColors.BottomNav.DarkSelectedText,
            unselectedText = SystemColors.BottomNav.DarkUnselectedText,
            border = SystemColors.BottomNav.DarkBorder
        )
    } else {
        BottomNavColors(
            background = SystemColors.BottomNav.LightBackground,
            selectedText = SystemColors.BottomNav.LightSelectedText,
            unselectedText = SystemColors.BottomNav.LightUnselectedText,
            border = SystemColors.BottomNav.LightBorder
        )
    }

    Timber.d("🎨 Выбран цвет BottomNav: ${colors.background.toHex()}")
    return colors
}

// Вспомогательная функция
fun Color.toHex(): String {
    return String.format("#%08X", this.value.toInt())
}

data class BottomNavColors(
    val background: Color,
    val selectedText: Color,
    val unselectedText: Color,
    val border: Color
)