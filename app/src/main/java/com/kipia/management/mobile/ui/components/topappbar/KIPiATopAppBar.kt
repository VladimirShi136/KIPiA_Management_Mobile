package com.kipia.management.mobile.ui.components.topappbar

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kipia.management.mobile.ui.components.photos.PhotosFilterMenu
import com.kipia.management.mobile.ui.components.scheme.SchemesFilterMenu
import com.kipia.management.mobile.ui.components.table.DeviceFilterMenu
import com.kipia.management.mobile.ui.components.theme.ThemeToggleButton
import com.kipia.management.mobile.ui.screens.schemes.SchemesSortBy
import com.kipia.management.mobile.viewmodel.DevicesViewModel
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KIPiATopAppBar(
    topAppBarState: TopAppBarData,
    topAppBarBg: Color,
    topAppBarContent: Color,
    onBackClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    // Логируем только при изменении заголовка или важных параметров
    LaunchedEffect(topAppBarState.title, topAppBarState.showBackButton) {
        Timber.d("═══════════════════════════════════════════")
        Timber.d("TopAppBar обновлен: '${topAppBarState.title}'")
        Timber.d("  showBackButton: ${topAppBarState.showBackButton}")
        Timber.d("  showSchemesFilterMenu: ${topAppBarState.showSchemesFilterMenu}")
        Timber.d("  showSchemeEditorActions: ${topAppBarState.showSchemeEditorActions}")
        Timber.d("═══════════════════════════════════════════")
    }

    TopAppBar(
        title = {
            Text(
                text = topAppBarState.title,
                color = topAppBarContent,
                fontSize = 14.sp,
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = colorScheme.onPrimary.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = topAppBarBg,
            titleContentColor = topAppBarContent,
            navigationIconContentColor = topAppBarContent,
            actionIconContentColor = topAppBarContent
        ),
        navigationIcon = {
            if (topAppBarState.showBackButton) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = topAppBarContent
                    )
                }
            }
        },
        actions = {
            // Используем remember для избежания лишних рекомпозиций внутри actions
            KeyedTopAppBarActions(
                key = topAppBarState.hashCode(), // Уникальный ключ для состояния
                topAppBarState = topAppBarState,
                topAppBarContent = topAppBarContent,
                navController = navController
            )
        },
        modifier = modifier
    )
}

@Composable
private fun KeyedTopAppBarActions(
    key: Int,
    topAppBarState: TopAppBarData,
    topAppBarContent: Color,
    navController: NavController
) {
    // Используем key чтобы гарантировать пересоздание при изменении типа экрана
    when {
        // Редактор схем
        topAppBarState.showSchemeEditorActions -> {
            SchemeEditorActions(
                topAppBarState = topAppBarState,
                topAppBarContent = topAppBarContent
            )
        }

        // Экран схем с фильтрами
        navController.currentDestination?.route == "schemes" -> {
            SchemesScreenActions(
                topAppBarState = topAppBarState,
                topAppBarContent = topAppBarContent,
                navController = navController
            )
        }

        // Экран фото
        navController.currentDestination?.route == "photos" -> {
            PhotosScreenActions(
                topAppBarState = topAppBarState,
                topAppBarContent = topAppBarContent,
                navController = navController
            )
        }

        // Главный экран приборов
        navController.currentDestination?.route == "devices" -> {
            DevicesScreenActions(
                topAppBarContent = topAppBarContent,
                navController = navController
            )
        }

        // Экран с кнопкой назад (device_edit, device_detail и т.д.)
        topAppBarState.showBackButton -> {
            BackButtonScreenActions(
                topAppBarState = topAppBarState,
                topAppBarContent = topAppBarContent
            )
        }
    }
}

@Composable
private fun SchemeEditorActions(
    topAppBarState: TopAppBarData,
    topAppBarContent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        // Кнопка свойств схемы
        if (topAppBarState.showSchemeEditorActions) {
            IconButton(
                onClick = {
                    Timber.d("Кнопка свойств нажата")
                    topAppBarState.onPropertiesClick?.invoke()
                },
                enabled = topAppBarState.onPropertiesClick != null
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Свойства схемы",
                    tint = topAppBarContent
                )
            }
        }

        // Кнопка сохранения
        if (topAppBarState.canSave) {
            IconButton(
                onClick = {
                    Timber.d("Кнопка сохранения нажата")
                    topAppBarState.onSaveClick?.invoke()
                },
                enabled = topAppBarState.onSaveClick != null
            ) {
                Icon(
                    Icons.Default.Save,
                    contentDescription = "Сохранить",
                    tint = topAppBarContent
                )
            }
        }
    }
}

@Composable
private fun SchemesScreenActions(
    topAppBarState: TopAppBarData,
    topAppBarContent: Color,
    navController: NavController
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Фильтры для схем
        if (topAppBarState.showSchemesFilterMenu) {
            SchemesFilterMenu(
                searchQuery = topAppBarState.schemesSearchQuery ?: "",
                onSearchQueryChange = { query ->
                    topAppBarState.onSchemesSearchQueryChange?.invoke(query)
                },
                currentSort = topAppBarState.schemesCurrentSort ?: SchemesSortBy.NAME_ASC,
                onSortSelected = { sortBy ->
                    topAppBarState.onSchemesSortSelected?.invoke(sortBy)
                },
                onResetAllFilters = {
                    topAppBarState.onSchemesResetAllFilters?.invoke()
                },
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        ThemeToggleButton(contentColor = topAppBarContent)
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = { navController.navigate("settings") }) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Настройки",
                tint = topAppBarContent
            )
        }
    }
}

@Composable
private fun PhotosScreenActions(
    topAppBarState: TopAppBarData,
    topAppBarContent: Color,
    navController: NavController
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        PhotosFilterMenu(
            selectedLocation = topAppBarState.selectedLocation,
            selectedDeviceId = topAppBarState.selectedDeviceId,
            locations = topAppBarState.locations,
            devices = topAppBarState.devices,
            onLocationFilterChange = { location ->
                topAppBarState.onLocationFilterChange?.invoke(location)
            },
            onDeviceFilterChange = { deviceId ->
                topAppBarState.onDeviceFilterChange?.invoke(deviceId)
            },
            onResetAllFilters = {
                // Добавьте логику сброса если нужно
            },
            modifier = Modifier.padding(end = 4.dp)
        )

        ThemeToggleButton(contentColor = topAppBarContent)
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = { navController.navigate("settings") }) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Настройки",
                tint = topAppBarContent
            )
        }
    }
}

@Composable
private fun DevicesScreenActions(
    topAppBarContent: Color,
    navController: NavController
) {
    val devicesViewModel: DevicesViewModel = hiltViewModel()

    // Подписываемся только на нужные стейты
    val searchQuery by devicesViewModel.searchQuery.collectAsStateWithLifecycle()
    val allLocations by devicesViewModel.allLocations.collectAsStateWithLifecycle()
    val uiState by devicesViewModel.uiState.collectAsStateWithLifecycle()

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        DeviceFilterMenu(
            searchQuery = searchQuery,
            onSearchQueryChange = { devicesViewModel.setSearchQuery(it) },
            locationFilter = uiState.locationFilter,
            locations = allLocations,
            onLocationFilterChange = { devicesViewModel.setLocationFilter(it) },
            statusFilter = uiState.statusFilter,
            onStatusFilterChange = { devicesViewModel.setStatusFilter(it) },
            modifier = Modifier.padding(end = 4.dp)
        )
        ThemeToggleButton(contentColor = topAppBarContent)
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = { navController.navigate("settings") }) {
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Настройки",
                tint = topAppBarContent
            )
        }
    }
}

@Composable
private fun BackButtonScreenActions(
    topAppBarState: TopAppBarData,
    topAppBarContent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        // Кнопка редактирования
        if (topAppBarState.showEditButton) {
            IconButton(
                onClick = { topAppBarState.onEditClick?.invoke() },
                enabled = topAppBarState.onEditClick != null
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Редактировать",
                    tint = topAppBarContent
                )
            }
        }

        // Кнопка сохранения
        if (topAppBarState.showSaveButton) {
            IconButton(
                onClick = { topAppBarState.onSaveClick?.invoke() },
                enabled = topAppBarState.onSaveClick != null
            ) {
                Icon(
                    Icons.Filled.Save,
                    contentDescription = "Сохранить",
                    tint = topAppBarContent
                )
            }
        }

        // Кнопка удаления
        if (topAppBarState.showDeleteButton) {
            IconButton(
                onClick = { topAppBarState.onDeleteClick?.invoke() },
                enabled = topAppBarState.onDeleteClick != null
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Удалить",
                    tint = topAppBarContent
                )
            }
        }

        // Кнопка добавления
        if (topAppBarState.showAddButton) {
            IconButton(
                onClick = { topAppBarState.onAddClick?.invoke() },
                enabled = topAppBarState.onAddClick != null
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Добавить",
                    tint = topAppBarContent
                )
            }
        }
    }
}