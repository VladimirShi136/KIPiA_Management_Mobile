package com.kipia.management.mobile.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kipia.management.mobile.repository.PreferencesRepository
import com.kipia.management.mobile.viewmodel.ThemeViewModel

/**
 * Экран с настройками
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    updateBottomNavVisibility: (Boolean) -> Unit = {}
) {
    // Отключаем bottom navigation при входе на экран настроек
    LaunchedEffect(Unit) {
        updateBottomNavVisibility(false)
    }

    // Восстанавливаем при выходе
    DisposableEffect(Unit) {
        onDispose {
            updateBottomNavVisibility(true)
        }
    }

    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColors by themeViewModel.dynamicColors.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val supportsDynamicColors = themeViewModel.supportsDynamicColors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Настройки внешнего вида
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Внешний вид",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Выбор темы
                Text(
                    text = "Тема приложения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Используем SegmentedButton с текстом внутри
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Системная тема
                    SegmentedButton(
                        selected = themeMode == PreferencesRepository.THEME_FOLLOW_SYSTEM,
                        onClick = { themeViewModel.setTheme(PreferencesRepository.THEME_FOLLOW_SYSTEM) },
                        icon = {
                            Icon(
                                Icons.Filled.SettingsBrightness,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Системная",
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }

                    // Светлая тема
                    SegmentedButton(
                        selected = themeMode == PreferencesRepository.THEME_LIGHT,
                        onClick = { themeViewModel.setTheme(PreferencesRepository.THEME_LIGHT) },
                        icon = {
                            Icon(
                                Icons.Filled.LightMode,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Светлая",
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }

                    // Темная тема
                    SegmentedButton(
                        selected = themeMode == PreferencesRepository.THEME_DARK,
                        onClick = { themeViewModel.setTheme(PreferencesRepository.THEME_DARK) },
                        icon = {
                            Icon(
                                Icons.Filled.DarkMode,
                                null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Темная",
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Динамические цвета (Material You)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Динамические цвета",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (supportsDynamicColors) {
                                "Использовать цвета обоев системы"
                            } else {
                                "Доступно на Android 12 и выше"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (supportsDynamicColors) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            }
                        )
                    }

                    Switch(
                        checked = dynamicColors && supportsDynamicColors,
                        onCheckedChange = {
                            if (supportsDynamicColors) {
                                themeViewModel.toggleDynamicColors()
                            }
                        },
                        enabled = supportsDynamicColors
                    )
                }
            }
        }

        // Информация о приложении
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "О приложении",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text("Версия") },
                    supportingContent = { Text("1.0.0") }
                )

                ListItem(
                    leadingContent = {
                        Icon(
                            Icons.Filled.Code,
                            contentDescription = null
                        )
                    },
                    headlineContent = { Text("Разработчик") },
                    supportingContent = { Text("KIPiA Management") }
                )
            }
        }
    }
}