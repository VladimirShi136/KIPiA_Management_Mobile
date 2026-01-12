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
import com.kipia.management.mobile.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel = hiltViewModel()
) {
    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()  // ← ИСПРАВЛЕНО
    val dynamicColors by themeViewModel.dynamicColors.collectAsStateWithLifecycle()  // ← ИСПРАВЛЕНО
    val scrollState = rememberScrollState()

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

                    // Выбор темы
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
                            selected = themeMode == 0,
                            onClick = { themeViewModel.setTheme(0) },
                            label = { Text("Системная") },
                            leadingIcon = if (themeMode == 0) {
                                { Icon(Icons.Filled.Check, null) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )

                        // Кнопка "Светлая"
                        FilterChip(
                            selected = themeMode == 1,
                            onClick = { themeViewModel.setTheme(1) },
                            label = { Text("Светлая") },
                            leadingIcon = if (themeMode == 1) {
                                { Icon(Icons.Filled.Check, null) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )

                        // Кнопка "Темная"
                        FilterChip(
                            selected = themeMode == 2,
                            onClick = { themeViewModel.setTheme(2) },
                            label = { Text("Темная") },
                            leadingIcon = if (themeMode == 2) {
                                { Icon(Icons.Filled.Check, null) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
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
                                text = "Использовать цвета обоев системы",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = dynamicColors,
                            onCheckedChange = { themeViewModel.toggleDynamicColors() }
                        )
                    }
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