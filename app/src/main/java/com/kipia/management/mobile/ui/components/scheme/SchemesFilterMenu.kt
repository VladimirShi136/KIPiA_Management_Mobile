package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Управление фокусом для поиска
    LaunchedEffect(showSearch) {
        if (showSearch) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Подсчет активных фильтров (поиск + сортировка)
    val activeFilters = remember(searchQuery, currentSort) {
        var count = 0
        if (searchQuery.isNotEmpty()) count++
        if (currentSort != SchemesSortBy.NAME_ASC) count++
        count
    }

    Box(modifier = modifier) {
        // Кнопка меню фильтров
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp)
        ) {
            // Иконка фильтра
            Icon(
                Icons.Default.FilterAlt,
                contentDescription = "Фильтры схем",
                tint = Color.White
            )

            // Бейдж с количеством активных фильтров
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

        // Основное выпадающее меню
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(250.dp)
        ) {
            // Заголовок меню
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

            // Пункт поиска
            SearchMenuItem(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearchClicked = { showSearch = true }
            )

            // Пункт сортировки с подменю
            SortMenuItem(
                currentSort = currentSort,
                onSortSelected = { sort ->
                    onSortSelected(sort)
                    expanded = false
                }
            )

            HorizontalDivider()

            // Кнопка сброса всех фильтров
            DropdownMenuItem(
                text = {
                    Text(
                        text = "Сбросить все фильтры",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    onResetAllFilters()
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

@Composable
private fun SearchMenuItem(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchClicked: () -> Unit
) {
    var showSearchField by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    if (showSearchField) {
        // Поле поиска внутри меню
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Поиск схем...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                ),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Поиск")
                },
                trailingIcon = {
                    IconButton(
                        onClick = { showSearchField = false }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }
            )
        }
    } else {
        // Кнопка поиска
        DropdownMenuItem(
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Поиск схем",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Галочка если поиск активен
                        if (searchQuery.isNotEmpty()) {
                            Text(
                                text = "✓",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        // Стрелка
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Поиск",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            onClick = { showSearchField = true },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            }
        )
    }
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
                    // Галочка если не стандартная сортировка
                    if (currentSort != SchemesSortBy.NAME_ASC) {
                        Text(
                            text = "✓",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    // Стрелка вниз
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
            Icon(Icons.Default.Sort, contentDescription = null)
        }
    )

    // Подменю сортировки
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
        else -> {} // NAME_ASC не показываем (это по умолчанию)
    }

    return if (filters.isEmpty()) {
        "Нет активных фильтров"
    } else {
        "Фильтры: ${filters.joinToString(", ")}"
    }
}