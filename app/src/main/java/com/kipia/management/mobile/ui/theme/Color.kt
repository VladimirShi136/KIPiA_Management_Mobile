package com.kipia.management.mobile.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember


// ===== ОСНОВНАЯ ПАЛИТРА ИЗ ИКОНКИ =====
object AppColors {
    // Основные цвета из иконки
    val Coral = Color(0xFFF58352)    // Основной акцент (Primary) - #F58352
    val Peach = Color(0xFFE2A58C)    // Вторичный акцент (Secondary) - #E2A58C
    val IceBlue = Color(0xFFDBECF0)  // Третичный/фон - #DBECF0
    val LightGrayBlue = Color(0xFFB6C0C9) // Нейтральный - #B6C0C9

    // Дополнительные из градации
    val DarkBlue = Color(0xFF465261)     // Для шапки - #465261
    val MediumDarkGray = Color(0xFF6C7884) // Для навигации - #6C7884
    val MediumGray = Color(0xFF848C9B)    // Для второстепенного текста - #848C9B
    val Pinkish = Color(0xFFE4BEBE)       // Розоватый - #E4BEBE

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
        val LightBackground = AppColors.DarkBlue
        val LightContent = Color.White
        val LightBorder = Color.White.copy(alpha = 0.8f)

        val DarkBackground = Color(0xFF1E2A3A)
        val DarkContent = Color.White
        val DarkBorder = Color.White.copy(alpha = 0.8f)
    }

    // ★★★★ ЦВЕТА ДЛЯ BOTTOM NAVIGATION ★★★★
    object BottomNav {
        val LightBackground = AppColors.MediumDarkGray
        val LightSelectedText = Color.White
        val LightUnselectedText = Color.White.copy(alpha = 0.8f)
        val LightBorder = Color.White.copy(alpha = 0.3f)

        val DarkBackground = Color(0xFF4A5568)
        val DarkSelectedText = Color.White
        val DarkUnselectedText = Color.White.copy(alpha = 0.8f)
        val DarkBorder = Color.White.copy(alpha = 0.3f)
    }

    // ★★★★ ЦВЕТА ДЛЯ КНОПОК BOTTOM NAV ★★★★
    object BottomNavButtons {
        val Devices = AppColors.Coral
        val Photos = AppColors.Peach
        val Schemes = AppColors.IceBlue
        val Reports = AppColors.LightGrayBlue

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

// ===== ЦВЕТА СТАТУСОВ =====
object DeviceStatusColors {
    val Total = Color(0xFF1D5A73)
    val Working = Color(0xFF4CAF50)
    val Storage = AppColors.Coral
    val Lost = AppColors.MediumGray
    val Broken = Color(0xFFF44336)

    val WorkingContainer = Color(0xFFE8F5E9)
    val StorageContainer = AppColors.PeachLight
    val LostContainer = AppColors.IceBlue.copy(alpha = 0.3f)
    val BrokenContainer = Color(0xFFFFEBEE)

    val WorkingText = Color(0xFF2E7D32)
    val StorageText = AppColors.CoralDark
    val LostText = AppColors.MediumGray
    val BrokenText = Color(0xFFD32F2F)
}

enum class DeviceStatus(
    val displayName: String,
    val color: Color,
    val containerColor: Color,
    val textColor: Color
) {
    WORKING("В работе", DeviceStatusColors.Working, DeviceStatusColors.WorkingContainer, DeviceStatusColors.WorkingText),
    STORAGE("Хранение", DeviceStatusColors.Storage, DeviceStatusColors.StorageContainer, DeviceStatusColors.StorageText),
    LOST("Утерян", DeviceStatusColors.Lost, DeviceStatusColors.LostContainer, DeviceStatusColors.LostText),
    BROKEN("Испорчен", DeviceStatusColors.Broken, DeviceStatusColors.BrokenContainer, DeviceStatusColors.BrokenText);

    companion object {
        fun fromString(status: String): DeviceStatus = when (status) {
            "В работе" -> WORKING
            "Хранение" -> STORAGE
            "Утерян" -> LOST
            "Испорчен" -> BROKEN
            else -> WORKING
        }
        val ALL_STATUSES = listOf("В работе", "Хранение", "Утерян", "Испорчен")
    }
}

object UtilityColors {
    val ShadowLight = Color.Black.copy(alpha = 0.1f)
    val ShadowMedium = Color.Black.copy(alpha = 0.2f)
    val ShadowDark = Color.Black.copy(alpha = 0.3f)
    val ScrimLight = Color.Black.copy(alpha = 0.3f)
    val ScrimMedium = Color.Black.copy(alpha = 0.5f)
    val ScrimDark = Color.Black.copy(alpha = 0.7f)
}

fun getBottomNavButtonColor(index: Int): Color = when (index) {
    0 -> SystemColors.BottomNavButtons.Devices
    1 -> SystemColors.BottomNavButtons.Photos
    2 -> SystemColors.BottomNavButtons.Schemes
    3 -> SystemColors.BottomNavButtons.Reports
    else -> SystemColors.Primary
}

fun getBottomNavTextColor(backgroundColor: Color): Color = when (backgroundColor) {
    SystemColors.BottomNavButtons.Devices -> SystemColors.BottomNavButtons.TextOnCoral
    SystemColors.BottomNavButtons.Photos -> SystemColors.BottomNavButtons.TextOnPeach
    SystemColors.BottomNavButtons.Schemes -> SystemColors.BottomNavButtons.TextOnIceBlue
    SystemColors.BottomNavButtons.Reports -> SystemColors.BottomNavButtons.TextOnGrayBlue
    else -> Color.White
}

@Composable
fun getTopAppBarColors(isDarkTheme: Boolean = isSystemInDarkTheme()): Pair<Color, Color> {
    return remember(isDarkTheme) {
        if (isDarkTheme) {
            Pair(SystemColors.TopAppBar.DarkBackground, SystemColors.TopAppBar.DarkContent)
        } else {
            Pair(SystemColors.TopAppBar.LightBackground, SystemColors.TopAppBar.LightContent)
        }
    }
}

@Composable
fun getBottomNavColors(isDarkTheme: Boolean = isSystemInDarkTheme()): BottomNavColors {
    return remember(isDarkTheme) {
        if (isDarkTheme) {
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
    }
}

fun Color.toHex(): String = String.format("#%08X", (this.value shr 32).toInt())

data class BottomNavColors(
    val background: Color,
    val selectedText: Color,
    val unselectedText: Color,
    val border: Color
)
