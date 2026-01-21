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

// Кастомные цветовые схемы
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF456381),
    secondary = Color(0xFF527091),
    tertiary = Color(0xFF0057D9),
    background = Color(0xFFFDFDFD),
    surface = Color(0xFFFDFDFD),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF1A237E),
    secondary = Color(0xFF283593),
    tertiary = Color(0xFF6D9EFF),
    background = Color(0xFF1C1B1F),
    surface = Color(0xFF1C1B1F),
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color(0xFFE6E1E5),
    onSurface = Color(0xFFE6E1E5)
)

@Composable
fun KIPiATheme(
    content: @Composable () -> Unit
) {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColors by themeViewModel.dynamicColors.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Определяем, темная ли тема
    val isDarkTheme = when (themeMode) {
        PreferencesRepository.THEME_LIGHT -> false // Светлая
        PreferencesRepository.THEME_DARK -> true   // Темная
        else -> isSystemInDarkTheme()              // Системная
    }

    // Выбираем цветовую схему
    val colorScheme = remember(isDarkTheme, dynamicColors) {
        if (dynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Используем динамические цвета если включены и поддерживаются
            if (isDarkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        } else {
            // Используем кастомные цвета
            if (isDarkTheme) DarkColorScheme else LightColorScheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}