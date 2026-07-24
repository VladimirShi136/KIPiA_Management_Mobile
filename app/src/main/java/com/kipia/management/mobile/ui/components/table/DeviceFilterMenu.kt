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
import androidx.compose.ui.unit.DpOffset
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
    modifier: Modifier = Modifier,
    hasData: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

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
                } else {
                    Text(
                        text = "",
                        fontSize = 0.sp,
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 0.dp),
            modifier = Modifier.width(320.dp)
        ) {
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

            SearchMenuItem(
                searchQuery = searchQuery,
                showSearch = showSearch,
                onSearchQueryChange = onSearchQueryChange,
                onToggleSearch = { if (hasData) showSearch = !showSearch },
                isEnabled = hasData,
                focusRequester = focusRequester,
                keyboardController = keyboardController
            )

            LocationFilterMenuItem(
                currentFilter = locationFilter,
                locations = locations,
                onItemSelected = { selectedLocation ->
                    onLocationFilterChange(selectedLocation)
                    expanded = false
                },
                isEnabled = hasData
            )

            StatusFilterMenuItem(
                currentFilter = statusFilter,
                onItemSelected = { selectedStatus ->
                    onStatusFilterChange(selectedStatus)
                    expanded = false
                },
                isEnabled = hasData
            )

            HorizontalDivider()

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
    isEnabled: Boolean,
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
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
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
        enabled = isEnabled,
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        trailingIcon = {
            if (!isEnabled) {
                Icon(Icons.Default.Lock, contentDescription = "Нет данных")
            }
        }
    )
}

@Composable
private fun LocationFilterMenuItem(
    currentFilter: String?,
    locations: List<String>,
    onItemSelected: (String?) -> Unit,
    isEnabled: Boolean
) {
    var showSubMenu by remember { mutableStateOf(false) }
    val hasSubData = isEnabled && locations.isNotEmpty()

    Box(modifier = Modifier.fillMaxWidth()) {
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
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            },
            onClick = { if (hasSubData) showSubMenu = true },
            enabled = hasSubData,
            leadingIcon = {
                Icon(Icons.Default.LocationOn, contentDescription = null)
            },
            trailingIcon = {
                if (hasSubData) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать")
                } else {
                    Icon(Icons.Default.Lock, contentDescription = "Нет данных")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (hasSubData) {
            DropdownMenu(
                expanded = showSubMenu,
                onDismissRequest = { showSubMenu = false },
                offset = DpOffset(0.dp, 0.dp),
                modifier = Modifier.width(320.dp)
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
                        showSubMenu = false
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
                            showSubMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusFilterMenuItem(
    currentFilter: String?,
    onItemSelected: (String?) -> Unit,
    isEnabled: Boolean
) {
    var showSubMenu by remember { mutableStateOf(false) }
    val statuses = listOf("В работе", "Хранение", "Утерян", "Испорчен")

    Box(modifier = Modifier.fillMaxWidth()) {
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
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }
            },
            onClick = { if (isEnabled) showSubMenu = true },
            enabled = isEnabled,
            leadingIcon = {
                Icon(Icons.Default.Flag, contentDescription = null)
            },
            trailingIcon = {
                if (isEnabled) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать")
                } else {
                    Icon(Icons.Default.Lock, contentDescription = "Нет данных")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        if (isEnabled) {
            DropdownMenu(
                expanded = showSubMenu,
                onDismissRequest = { showSubMenu = false },
                offset = DpOffset(0.dp, 0.dp),
                modifier = Modifier.width(320.dp)
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
                        showSubMenu = false
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
                            showSubMenu = false
                        }
                    )
                }
            }
        }
    }
}
