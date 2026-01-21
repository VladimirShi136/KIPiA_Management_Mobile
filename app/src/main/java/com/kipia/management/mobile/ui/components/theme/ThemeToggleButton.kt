package com.kipia.management.mobile.ui.components.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.repository.PreferencesRepository
import com.kipia.management.mobile.viewmodel.ThemeViewModel

@Composable
fun ThemeToggleButton() {
    val themeViewModel: ThemeViewModel = hiltViewModel()
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()

    IconButton(
        onClick = {
            themeViewModel.toggleTheme()
        }
    ) {
        when (themeMode) {
            PreferencesRepository.THEME_LIGHT -> {
                Icon(
                    Icons.Filled.LightMode,
                    contentDescription = "Светлая тема",
                    tint = Color.White
                )
            }
            PreferencesRepository.THEME_DARK -> {
                Icon(
                    Icons.Filled.DarkMode,
                    contentDescription = "Темная тема",
                    tint = Color.White
                )
            }
            PreferencesRepository.THEME_FOLLOW_SYSTEM -> {
                Icon(
                    Icons.Filled.SettingsBrightness, // ← Иконка системной темы
                    contentDescription = "Системная тема",
                    tint = Color.White
                )
            }
        }
    }
}