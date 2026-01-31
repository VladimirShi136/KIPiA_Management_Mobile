package com.kipia.management.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.repository.PreferencesRepository
import com.kipia.management.mobile.viewmodel.ThemeViewModel

/**
 * Настройки для светлой / темной темы
 */
// ===== LIGHT COLOR SCHEME (на основе вашей иконки) =====
private val LightColorScheme = lightColorScheme(
    // Primary colors
    primary = AppColors.Coral,
    onPrimary = Color.White,
    primaryContainer = AppColors.CoralLight,
    onPrimaryContainer = AppColors.CoralDark,

    // Secondary colors
    secondary = AppColors.Peach,
    onSecondary = Color.Black,
    secondaryContainer = AppColors.PeachLight,
    onSecondaryContainer = AppColors.CoralDark,

    // Tertiary colors
    tertiary = AppColors.IceBlue,
    onTertiary = AppColors.DarkBlue,

    // Background & Surface
    background = Color.White,
    onBackground = AppColors.DarkBlue,          // #465261

    surface = Color.White,
    onSurface = AppColors.DarkBlue,             // #465261
    surfaceVariant = AppColors.LightGrayBlue.copy(alpha = 0.2f),
    onSurfaceVariant = AppColors.MediumDarkGray, // #6C7884

    outline = AppColors.MediumDarkGray.copy(alpha = 0.3f),
    outlineVariant = AppColors.LightGrayBlue.copy(alpha = 0.1f),

    // Error
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),

    // Neutral colors
    scrim = Color.Black.copy(alpha = 0.5f),
    surfaceTint = AppColors.Coral,

    // Inverse colors (для контрастных элементов)
    inversePrimary = AppColors.CoralLight,
    inverseSurface = AppColors.DarkBlue,
    inverseOnSurface = Color.White
)

// ===== DARK COLOR SCHEME =====
private val DarkColorScheme = darkColorScheme(
    // Primary colors (в темной теме делаем светлее)
    primary = AppColors.CoralLight,
    onPrimary = Color.Black,
    primaryContainer = AppColors.CoralDark,
    onPrimaryContainer = Color.White,

    // Secondary colors
    secondary = AppColors.PeachLight,
    onSecondary = Color.Black,
    secondaryContainer = AppColors.CoralDark,
    onSecondaryContainer = AppColors.PeachLight,

    // Tertiary colors
    tertiary = AppColors.DarkBlue.copy(alpha = 0.3f),
    onTertiary = Color.White,

    // Background & Surface
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),

    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),

    // Outline
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),

    // Error
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),

    // Neutral colors
    scrim = Color.Black.copy(alpha = 0.7f),
    surfaceTint = AppColors.CoralLight,

    // Inverse colors
    inversePrimary = AppColors.CoralDark,
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F)
)

@Composable
fun KIPiATheme(
    content: @Composable () -> Unit
) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColors by themeViewModel.dynamicColors.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isDarkTheme = when (themeMode) {
        PreferencesRepository.THEME_LIGHT -> false
        PreferencesRepository.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = remember(isDarkTheme, dynamicColors) {
        if (dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (isDarkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        } else {
            if (isDarkTheme) DarkColorScheme else LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}