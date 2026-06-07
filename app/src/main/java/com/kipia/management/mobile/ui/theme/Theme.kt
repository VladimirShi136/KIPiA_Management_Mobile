package com.kipia.management.mobile.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.repository.PreferencesRepository
import com.kipia.management.mobile.viewmodel.ThemeViewModel

private val LightColorScheme = lightColorScheme(
    primary = AppColors.Coral,
    onPrimary = Color.White,
    primaryContainer = AppColors.CoralLight,
    onPrimaryContainer = AppColors.CoralDark,
    secondary = AppColors.Peach,
    onSecondary = Color.Black,
    secondaryContainer = AppColors.PeachLight,
    onSecondaryContainer = AppColors.CoralDark,
    tertiary = AppColors.CoralLight,
    onTertiary = AppColors.DarkBlue,
    background = Color.White,
    onBackground = AppColors.DarkBlue,
    surface = Color.White,
    onSurface = AppColors.DarkBlue,
    surfaceVariant = AppColors.LightGrayBlue.copy(alpha = 0.2f),
    onSurfaceVariant = AppColors.MediumDarkGray,
    outline = AppColors.MediumDarkGray.copy(alpha = 0.3f),
    outlineVariant = AppColors.LightGrayBlue.copy(alpha = 0.1f),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
    onError = Color.White,
    onErrorContainer = Color(0xFF410002),
    scrim = Color.Black.copy(alpha = 0.5f),
    surfaceTint = AppColors.Coral,
    inversePrimary = AppColors.CoralLight,
    inverseSurface = AppColors.DarkBlue,
    inverseOnSurface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.CoralLight,
    onPrimary = Color.Black,
    primaryContainer = AppColors.CoralDark,
    onPrimaryContainer = Color.White,
    secondary = AppColors.PeachLight,
    onSecondary = Color.Black,
    secondaryContainer = AppColors.CoralDark,
    onSecondaryContainer = AppColors.PeachLight,
    tertiary = AppColors.Coral,
    onTertiary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onError = Color(0xFF690005),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black.copy(alpha = 0.7f),
    surfaceTint = AppColors.CoralLight,
    inversePrimary = AppColors.CoralDark,
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF1C1B1F)
)

/**
 * Основная точка входа для темы. 
 * В режиме Preview НЕ использует ViewModel.
 */
@Composable
fun KIPiATheme(
    isDarkTheme: Boolean? = null,
    dynamicColor: Boolean? = null,
    content: @Composable () -> Unit
) {
    val isInPreview = LocalInspectionMode.current
    
    if (isInPreview) {
        KIPiAThemeBase(
            darkTheme = isDarkTheme ?: isSystemInDarkTheme(),
            dynamicColor = dynamicColor ?: false,
            content = content
        )
    } else {
        KIPiAThemeWithViewModel(isDarkTheme, dynamicColor, content)
    }
}

@Composable
private fun KIPiAThemeWithViewModel(
    forcedDarkTheme: Boolean? = null,
    forcedDynamicColor: Boolean? = null,
    content: @Composable () -> Unit
) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColorsEnabled by themeViewModel.dynamicColors.collectAsStateWithLifecycle()

    val darkTheme = forcedDarkTheme ?: when (themeMode) {
        PreferencesRepository.THEME_LIGHT -> false
        PreferencesRepository.THEME_DARK -> true
        else -> isSystemInDarkTheme()
    }

    KIPiAThemeBase(
        darkTheme = darkTheme,
        dynamicColor = forcedDynamicColor ?: dynamicColorsEnabled,
        content = content
    )
}

@Composable
private fun KIPiAThemeBase(
    darkTheme: Boolean,
    dynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
