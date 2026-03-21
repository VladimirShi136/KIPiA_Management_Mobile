package com.kipia.management.mobile.ui.components.photos

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
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import timber.log.Timber

// Сортировка для галереи фото — по названию локации
enum class PhotosSortBy {
    NAME_ASC, NAME_DESC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotosFilterMenu(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    currentSort: PhotosSortBy,
    onSortSelected: (PhotosSortBy) -> Unit,
    onResetAllFilters: () -> Unit,
    modifier: Modifier = Modifier
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

    val activeFilters = remember(searchQuery, currentSort) {
        var count = 0
        if (searchQuery.isNotEmpty()) count++
        if (currentSort != PhotosSortBy.NAME_ASC) count++
        count
    }

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
                    Timber.d("Все фильтры фото сброшены")
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
    keyboardController: SoftwareKeyboardController?
) {
    DropdownMenuItem(
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Поиск по локации",
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
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        ),
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
    currentSort: PhotosSortBy,
    onSortSelected: (PhotosSortBy) -> Unit
) {
    var showSubMenu by remember { mutableStateOf(false) }

    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Сортировка локаций",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (currentSort != PhotosSortBy.NAME_ASC) {
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
    currentSort: PhotosSortBy,
    onSortSelected: (PhotosSortBy) -> Unit
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
                        fontWeight = if (currentSort == PhotosSortBy.NAME_ASC) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentSort == PhotosSortBy.NAME_ASC) {
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
                onSortSelected(PhotosSortBy.NAME_ASC)
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
                        fontWeight = if (currentSort == PhotosSortBy.NAME_DESC) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (currentSort == PhotosSortBy.NAME_DESC) {
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
                onSortSelected(PhotosSortBy.NAME_DESC)
                onDismiss()
            }
        )
    }
}

@Composable
fun PhotosActiveFiltersBadge(
    searchQuery: String,
    currentSort: PhotosSortBy,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActiveFilters = searchQuery.isNotEmpty() || currentSort != PhotosSortBy.NAME_ASC

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
    currentSort: PhotosSortBy
): String {
    val filters = mutableListOf<String>()

    if (searchQuery.isNotEmpty()) {
        filters.add("Поиск: \"$searchQuery\"")
    }

    when (currentSort) {
        PhotosSortBy.NAME_DESC -> filters.add("Сортировка: Я → А")
        else -> {}
    }

    return if (filters.isEmpty()) {
        "Нет активных фильтров"
    } else {
        "Фильтры: ${filters.joinToString(", ")}"
    }
}