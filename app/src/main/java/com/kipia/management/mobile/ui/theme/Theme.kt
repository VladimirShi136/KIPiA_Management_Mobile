package com.kipia.management.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Настройте свои цвета здесь
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006C4C), // Зелёный для управления
    secondary = Color(0xFF4A6572), // Стальной синий
    tertiary = Color(0xFFB71C1C) // Красный для аварийных состояний
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50), // Светло-зелёный
    secondary = Color(0xFF78909C),
    tertiary = Color(0xFFEF5350)
)

@Composable
fun KIPiATheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}