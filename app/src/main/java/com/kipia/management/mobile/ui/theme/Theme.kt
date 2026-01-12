package com.kipia.management.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.repository.PreferencesRepository
import com.kipia.management.mobile.viewmodel.ThemeViewModel

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006C4C),
    secondary = Color(0xFF4A6572),
    tertiary = Color(0xFF0057D9),
    background = Color(0xFFFDFDFD),
    surface = Color(0xFFFDFDFD),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6ED5A9),
    secondary = Color(0xFF95B8C7),
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

    // Определяем, темная ли тема
    val isDarkTheme = when (themeMode) {
        PreferencesRepository.THEME_LIGHT -> false
        PreferencesRepository.THEME_DARK -> true
        PreferencesRepository.THEME_FOLLOW_SYSTEM -> isSystemInDarkTheme()
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}