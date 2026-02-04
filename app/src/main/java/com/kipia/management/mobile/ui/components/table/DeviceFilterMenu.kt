package com.kipia.management.mobile.ui.components.table

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import timber.log.Timber

@Composable
fun DeviceFilterMenu(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    locationFilter: String?,
    locations: List<String>,
    onLocationFilterChange: (String?) -> Unit,
    statusFilter: String?,
    onStatusFilterChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // ★★★★ Используем LaunchedEffect для управления фокусом ★★★★
    LaunchedEffect(showSearch) {
        if (showSearch) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp)
        ) {

            Icon(
                Icons.Default.FilterAlt,
                contentDescription = "Фильтры и поиск",
                tint = Color.White
            )

            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
            ) {
                val activeFilters = listOfNotNull(
                    if (searchQuery.isNotEmpty()) "1" else null,
                    if (locationFilter != null) "1" else null,
                    if (statusFilter != null) "1" else null
                ).size

                if (activeFilters > 0) {
                    Text(
                        text = activeFilters.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(280.dp)
        ) {
            // Заголовок
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Фильтры приборов",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                onClick = {},
                trailingIcon = {
                    Icon(Icons.Default.Tune, contentDescription = null)
                }
            )

            HorizontalDivider()

            // Поиск
            SearchMenuItem(
                searchQuery = searchQuery,
                showSearch = showSearch,
                onSearchQueryChange = onSearchQueryChange,
                onToggleSearch = {
                    showSearch = !showSearch
                },
                focusRequester = focusRequester,
                keyboardController = keyboardController
            )

            // Фильтр по местоположению с вложенным меню
            LocationFilterMenuItem(
                currentFilter = locationFilter,
                locations = locations,
                onItemSelected = { selectedLocation ->
                    onLocationFilterChange(selectedLocation)
                    expanded = false
                }
            )

            // Фильтр по статусу с вложенным меню
            StatusFilterMenuItem(
                currentFilter = statusFilter,
                onItemSelected = { selectedStatus ->
                    onStatusFilterChange(selectedStatus)
                    expanded = false
                }
            )

            HorizontalDivider()

            // Кнопка сброса фильтров
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Сбросить все фильтры",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    onSearchQueryChange("")
                    onLocationFilterChange(null)
                    onStatusFilterChange(null)
                    expanded = false
                    Timber.d("Все фильтры сброшены")
                },
                leadingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

@Composable
private fun SearchMenuItem(
    searchQuery: String,
    showSearch: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    focusRequester: FocusRequester,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?
) {
    DropdownMenuItem(
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Поиск приборов",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showSearch) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = { Text("Введите текст...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") }
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                            }
                        )
                    )
                }
            }
        },
        onClick = onToggleSearch,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        }
    )
}

@Composable
private fun LocationFilterMenuItem(
    currentFilter: String?,
    locations: List<String>,
    onItemSelected: (String?) -> Unit
) {
    var showSubMenu by remember { mutableStateOf(false) }

    // Основной пункт меню
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Местоположение",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                if (currentFilter != null) {
                    Text(
                        text = "✓",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        onClick = { showSubMenu = true },
        leadingIcon = {
            Icon(Icons.Default.LocationOn, contentDescription = null)
        },
        trailingIcon = {
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать")
        }
    )

    // Вложенное меню как отдельный композабл
    LocationSubMenu(
        showSubMenu = showSubMenu,
        onDismiss = { showSubMenu = false },
        currentFilter = currentFilter,
        locations = locations,
        onItemSelected = onItemSelected
    )
}

@Composable
private fun LocationSubMenu(
    showSubMenu: Boolean,
    onDismiss: () -> Unit,
    currentFilter: String?,
    locations: List<String>,
    onItemSelected: (String?) -> Unit
) {
    DropdownMenu(
        expanded = showSubMenu,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(200.dp)
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    "Все места",
                    fontWeight = if (currentFilter == null) FontWeight.Bold else FontWeight.Normal
                )
            },
            onClick = {
                onItemSelected(null)
                onDismiss()
            }
        )

        HorizontalDivider()

        locations.forEach { location ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            location,
                            fontWeight = if (currentFilter == location) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        // ★ Галочка ТОЛЬКО для выбранной локации ★
                        if (currentFilter == location) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onItemSelected(location)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun StatusFilterMenuItem(
    currentFilter: String?,
    onItemSelected: (String?) -> Unit
) {
    var showSubMenu by remember { mutableStateOf(false) }
    val statuses = listOf("В работе", "Хранение", "Утерян", "Испорчен")

    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Статус",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                if (currentFilter != null) {
                    Text(
                        text = "✓",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        onClick = { showSubMenu = true },
        leadingIcon = {
            Icon(Icons.Default.Flag, contentDescription = null)
        },
        trailingIcon = {
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать")
        }
    )

    // Вложенное меню как отдельный композабл
    StatusSubMenu(
        showSubMenu = showSubMenu,
        onDismiss = { showSubMenu = false },
        currentFilter = currentFilter,
        statuses = statuses,
        onItemSelected = onItemSelected
    )
}

@Composable
private fun StatusSubMenu(
    showSubMenu: Boolean,
    onDismiss: () -> Unit,
    currentFilter: String?,
    statuses: List<String>,
    onItemSelected: (String?) -> Unit
) {
    DropdownMenu(
        expanded = showSubMenu,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(200.dp)
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    "Все статусы",
                    fontWeight = if (currentFilter == null) FontWeight.Bold else FontWeight.Normal
                )
            },
            onClick = {
                onItemSelected(null)
                onDismiss()
            }
        )

        HorizontalDivider()

        statuses.forEach { status ->
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            status,
                            fontWeight = if (currentFilter == status) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        // ★ Галочка ТОЛЬКО для выбранного статуса ★
                        if (currentFilter == status) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onItemSelected(status)
                    onDismiss()
                }
            )
        }
    }
}