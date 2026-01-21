package com.kipia.management.mobile.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kipia.management.mobile.R
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
    updateBottomNavVisibility: (Boolean) -> Unit = {} // ← НОВЫЙ ПАРАМЕТР
) {
    // ★★★★ ОТКЛЮЧАЕМ BOTTOM NAVIGATION ★★★★
    LaunchedEffect(Unit) {
        updateBottomNavVisibility(false)
    }

    // ★★★★ ВОССТАНАВЛИВАЕМ ПРИ ВЫХОДЕ ★★★★
    DisposableEffect(Unit) {
        onDispose {
            updateBottomNavVisibility(true)
        }
    }

    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColors by themeViewModel.dynamicColors.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val supportsDynamicColors = themeViewModel.supportsDynamicColors

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Настройки внешнего вида
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Внешний вид",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Выбор темы (оставляем как есть)
                    Text(
                        text = "Тема приложения",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Кнопка "Системная"
                        FilterChip(
                            selected = themeMode == PreferencesRepository.THEME_FOLLOW_SYSTEM,
                            onClick = { themeViewModel.setTheme(PreferencesRepository.THEME_FOLLOW_SYSTEM) },
                            label = { Text("Системная") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.SettingsBrightness, // ← Та же иконка
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            },
                            trailingIcon = if (themeMode == PreferencesRepository.THEME_FOLLOW_SYSTEM) {
                                { Icon(Icons.Filled.Check, null) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )

                        // Кнопка "Светлая"
                        FilterChip(
                            selected = themeMode == PreferencesRepository.THEME_LIGHT,
                            onClick = { themeViewModel.setTheme(PreferencesRepository.THEME_LIGHT) },
                            label = { Text("Светлая") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.LightMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            },
                            trailingIcon = if (themeMode == PreferencesRepository.THEME_LIGHT) {
                                { Icon(Icons.Filled.Check, null) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )

                        // Кнопка "Темная"
                        FilterChip(
                            selected = themeMode == PreferencesRepository.THEME_DARK,
                            onClick = { themeViewModel.setTheme(PreferencesRepository.THEME_DARK) },
                            label = { Text("Темная") },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.DarkMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            },
                            trailingIcon = if (themeMode == PreferencesRepository.THEME_DARK) {
                                { Icon(Icons.Filled.Check, null) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // динамические цвета
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

                // Информация о приложении
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
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
    }
}