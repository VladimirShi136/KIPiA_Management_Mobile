package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kipia.management.mobile.ui.screens.schemes.SchemesSortBy
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemesFilterMenu(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentSort: SchemesSortBy,
    onSortSelected: (SchemesSortBy) -> Unit,
    onResetAllFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }

    // focusRequester и keyboardController живут здесь и передаются вниз — как в DeviceFilterMenu
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(showSearch) {
        if (showSearch) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val activeFilters = remember(searchQuery, currentSort) {
        var count = 0
        if (searchQuery.isNotEmpty()) count++
        if (currentSort != SchemesSortBy.NAME_ASC) count++
        count
    }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.FilterAlt,
                contentDescription = "Фильтры схем",
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
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Фильтры схем",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                onClick = {},
                trailingIcon = {
                    Icon(Icons.Default.Map, contentDescription = null)
                }
            )

            HorizontalDivider()

            // focusRequester и keyboardController передаются как параметры — как в DeviceFilterMenu
            SearchMenuItem(
                searchQuery = searchQuery,
                showSearch = showSearch,
                onSearchQueryChange = onSearchQueryChange,
                onToggleSearch = { showSearch = !showSearch },
                focusRequester = focusRequester,
                keyboardController = keyboardController
            )

            SortMenuItem(
                currentSort = currentSort,
                onSortSelected = { sort ->
                    onSortSelected(sort)
                    expanded = false
                }
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
                    onResetAllFilters()
                    showSearch = false
                    expanded = false
                    Timber.d("Все фильтры схем сброшены")
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

// Структура идентична DeviceFilterMenu.SearchMenuItem:
// TextField рендерится внутри того же DropdownMenuItem (не в отдельном if/else composable),
// focusRequester приходит снаружи и уже привязан через LaunchedEffect в родителе
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
                        text = "Поиск схем",
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
                        placeholder = { Text("Введите название схемы...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Очистить")
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
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
private fun SortMenuItem(
    currentSort: SchemesSortBy,
    onSortSelected: (SchemesSortBy) -> Unit
) {
    var showSubMenu by remember { mutableStateOf(false) }

    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Сортировка схем",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (currentSort != SchemesSortBy.NAME_ASC) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Выбрать сортировку",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        },
        onClick = { showSubMenu = true },
        leadingIcon = {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null)
        }
    )

    SortSubMenu(
        showSubMenu = showSubMenu,
        onDismiss = { showSubMenu = false },
        currentSort = currentSort,
        onSortSelected = onSortSelected
    )
}

@Composable
private fun SortSubMenu(
    showSubMenu: Boolean,
    onDismiss: () -> Unit,
    currentSort: SchemesSortBy,
    onSortSelected: (SchemesSortBy) -> Unit
) {
    DropdownMenu(
        expanded = showSubMenu,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(200.dp)
    ) {
        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "А → Я",
                        fontWeight = if (currentSort == SchemesSortBy.NAME_ASC) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentSort == SchemesSortBy.NAME_ASC) {
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
                onSortSelected(SchemesSortBy.NAME_ASC)
                onDismiss()
            }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Я → А",
                        fontWeight = if (currentSort == SchemesSortBy.NAME_DESC) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentSort == SchemesSortBy.NAME_DESC) {
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
                onSortSelected(SchemesSortBy.NAME_DESC)
                onDismiss()
            }
        )
    }
}

@Composable
fun SchemesActiveFiltersBadge(
    searchQuery: String,
    currentSort: SchemesSortBy,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActiveFilters = searchQuery.isNotEmpty() || currentSort != SchemesSortBy.NAME_ASC

    if (hasActiveFilters) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = buildActiveFiltersText(searchQuery, currentSort),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onClearFilters,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Очистить фильтры",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun buildActiveFiltersText(
    searchQuery: String,
    currentSort: SchemesSortBy
): String {
    val filters = mutableListOf<String>()

    if (searchQuery.isNotEmpty()) {
        filters.add("Поиск: \"$searchQuery\"")
    }

    when (currentSort) {
        SchemesSortBy.NAME_DESC -> filters.add("Сортировка: Я → А")
        else -> {}
    }

    return if (filters.isEmpty()) {
        "Нет активных фильтров"
    } else {
        "Фильтры: ${filters.joinToString(", ")}"
    }
}