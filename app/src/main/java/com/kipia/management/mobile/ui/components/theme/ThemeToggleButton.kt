package com.kipia.management.mobile.ui.components.theme

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.viewmodel.ThemeViewModel

@Composable
fun ThemeToggleButton(
    modifier: Modifier = Modifier,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle() // ← ИСПРАВЛЕНО

    IconButton(
        onClick = { themeViewModel.toggleTheme() },
        modifier = modifier
    ) {
        Icon(
            imageVector = when (themeMode) {
                1 -> Icons.Filled.LightMode // Светлая
                2 -> Icons.Filled.DarkMode  // Темная
                else -> Icons.Filled.SettingsBrightness // Системная
            },
            contentDescription = "Переключить тему",
            modifier = Modifier.size(24.dp)
        )
    }
}