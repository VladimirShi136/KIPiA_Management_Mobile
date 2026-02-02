package com.kipia.management.mobile.ui.components.photos

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kipia.management.mobile.data.entities.Device
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosFilterMenu(
    selectedLocation: String?,
    selectedDeviceId: Int?,
    locations: List<String>,
    devices: List<Device>,
    onLocationFilterChange: (String?) -> Unit,
    onDeviceFilterChange: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // ★★★★ Используем LaunchedEffect для управления фокусом ★★★★
    LaunchedEffect(showSearch) {
        if (showSearch) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Подсчет активных фильтров
    val activeFilters = listOfNotNull(
        if (selectedLocation != null) "1" else null,
        if (selectedDeviceId != null) "1" else null,
        if (searchQuery.isNotEmpty()) "1" else null
    ).size

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.FilterAlt,
                contentDescription = "Фильтры фото",
                tint = Color.White
            )

            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
            ) {
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
                        text = "Фильтры фото",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                onClick = {},
                trailingIcon = {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                }
            )

            HorizontalDivider()

            // Поиск (как в DeviceFilterMenu)
            SearchMenuItem(
                searchQuery = searchQuery,
                showSearch = showSearch,
                onSearchQueryChange = { newQuery ->
                    searchQuery = newQuery
                    // Можно добавить поиск по фото
                    Timber.d("Поиск фото: $newQuery")
                },
                onToggleSearch = {
                    showSearch = !showSearch
                },
                focusRequester = focusRequester,
                keyboardController = keyboardController
            )

            // Фильтр по местоположению с вложенным меню
            LocationFilterMenuItem(
                currentFilter = selectedLocation,
                locations = locations,
                onItemSelected = { selectedLocation ->
                    onLocationFilterChange(selectedLocation)
                }
            )

            // Фильтр по прибору с вложенным меню
            DeviceFilterMenuItem(
                currentFilter = selectedDeviceId,
                devices = devices,
                onItemSelected = { selectedDeviceId ->
                    onDeviceFilterChange(selectedDeviceId)
                }
            )

            HorizontalDivider()

            // Кнопка сброса фильтров (как в DeviceFilterMenu)
            if (activeFilters > 0) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Сбросить все фильтры",
                            color = MaterialTheme.colorScheme.error
                        )
                    },
                    onClick = {
                        // Сброс всех фильтров
                        searchQuery = ""
                        onLocationFilterChange(null)
                        onDeviceFilterChange(null)
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
                        text = "Поиск фото",
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
                        placeholder = { Text("Название, прибор...") },
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
                    Text(
                        location,
                        fontWeight = if (currentFilter == location) FontWeight.Bold else FontWeight.Normal
                    )
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
private fun DeviceFilterMenuItem(
    currentFilter: Int?,
    devices: List<Device>,
    onItemSelected: (Int?) -> Unit
) {
    var showSubMenu by remember { mutableStateOf(false) }

    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Прибор",
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
            Icon(Icons.Default.Devices, contentDescription = null)
        },
        trailingIcon = {
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Выбрать")
        }
    )

    DeviceSubMenu(
        showSubMenu = showSubMenu,
        onDismiss = { showSubMenu = false },
        currentFilter = currentFilter,
        devices = devices,
        onItemSelected = onItemSelected
    )
}

@Composable
private fun DeviceSubMenu(
    showSubMenu: Boolean,
    onDismiss: () -> Unit,
    currentFilter: Int?,
    devices: List<Device>,
    onItemSelected: (Int?) -> Unit
) {
    DropdownMenu(
        expanded = showSubMenu,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(200.dp)
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    "Все приборы",
                    fontWeight = if (currentFilter == null) FontWeight.Bold else FontWeight.Normal
                )
            },
            onClick = {
                onItemSelected(null)
                onDismiss()
            }
        )

        HorizontalDivider()

        devices.forEach { device ->
            DropdownMenuItem(
                text = {
                    Text(
                        device.getDisplayName(),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        fontWeight = if (currentFilter == device.id) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    onItemSelected(device.id)
                    onDismiss()
                }
            )
        }
    }
}