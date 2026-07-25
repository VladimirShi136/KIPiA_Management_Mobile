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
    var locationExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }
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
                onClick = {}
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
                expanded = locationExpanded,
                onExpandedChange = { locationExpanded = it },
                onItemSelected = { selectedLocation ->
                    onLocationFilterChange(selectedLocation)
                    expanded = false
                },
                isEnabled = hasData
            )

            StatusFilterMenuItem(
                currentFilter = statusFilter,
                expanded = statusExpanded,
                onExpandedChange = { statusExpanded = it },
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
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onItemSelected: (String?) -> Unit,
    isEnabled: Boolean
) {
    val hasSubData = isEnabled && locations.isNotEmpty()

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
        onClick = {
            if (hasSubData) {
                onExpandedChange(!expanded)
            }
        },
        enabled = hasSubData,
        trailingIcon = {
            if (hasSubData) {
                Icon(
                    if (expanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    null
                )
            } else {
                Icon(Icons.Default.Lock, null)
            }
        }
    )

    if (expanded) {

        DropdownMenuItem(
            text = {
                Text(
                    "Все места",
                    modifier = Modifier.padding(start = 32.dp),
                    fontWeight = if (currentFilter == null)
                        FontWeight.Bold
                    else
                        FontWeight.Normal
                )
            },
            onClick = {
                onItemSelected(null)
                onExpandedChange(false)
            }
        )

        HorizontalDivider()

        locations.forEach { location ->

            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            location,
                            modifier = Modifier.weight(1f),
                            fontWeight =
                                if (location == currentFilter)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal
                        )

                        if (location == currentFilter) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                onClick = {
                    onItemSelected(location)
                    onExpandedChange(false)
                }
            )
        }

        HorizontalDivider()
    }
}

@Composable
private fun StatusFilterMenuItem(
    currentFilter: String?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onItemSelected: (String?) -> Unit,
    isEnabled: Boolean
) {
    val statuses = listOf(
        "В работе",
        "Хранение",
        "Утерян",
        "Испорчен"
    )

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
        onClick = {
            if (isEnabled) {
                onExpandedChange(!expanded)
            }
        },
        enabled = isEnabled,
        trailingIcon = {
            if (isEnabled) {
                Icon(
                    if (expanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    null
                )
            } else {
                Icon(Icons.Default.Lock, null)
            }
        }
    )

    if (expanded) {

        DropdownMenuItem(
            text = {
                Text(
                    "Все статусы",
                    modifier = Modifier.padding(start = 32.dp),
                    fontWeight =
                        if (currentFilter == null)
                            FontWeight.Bold
                        else
                            FontWeight.Normal
                )
            },
            onClick = {
                onItemSelected(null)
                onExpandedChange(false)
            }
        )

        HorizontalDivider()

        statuses.forEach { status ->

            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            status,
                            modifier = Modifier.weight(1f),
                            fontWeight =
                                if (status == currentFilter)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal
                        )

                        if (status == currentFilter) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                onClick = {
                    onItemSelected(status)
                    onExpandedChange(false)
                }
            )
        }

        HorizontalDivider()
    }
}
