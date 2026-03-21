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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.kipia.management.mobile.ui.components.dialogs.DeleteConfirmDialog
import com.kipia.management.mobile.ui.components.photos.PhotosFilterMenu
import com.kipia.management.mobile.ui.components.scheme.SchemesFilterMenu
import com.kipia.management.mobile.ui.components.table.DeviceFilterMenu
import com.kipia.management.mobile.ui.components.theme.ThemeToggleButton
import com.kipia.management.mobile.viewmodel.DevicesViewModel
import com.kipia.management.mobile.viewmodel.PhotosViewModel
import com.kipia.management.mobile.viewmodel.SchemesViewModel
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

    val borderColor = remember(colorScheme) {
        colorScheme.onPrimary.copy(alpha = 0.8f)
    }

    // Вызываем @Composable снаружи remember — результат стабилен пока цвета не меняются
    val appBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = topAppBarBg,
        titleContentColor = topAppBarContent,
        navigationIconContentColor = topAppBarContent,
        actionIconContentColor = topAppBarContent
    )

    LaunchedEffect(topAppBarState.title, topAppBarState.showBackButton) {
        Timber.d("TopAppBar обновлен: '${topAppBarState.title}'")
    }

    TopAppBar(
        title = {
            Text(
                text = topAppBarState.title,
                color = topAppBarContent,
                fontSize = 14.sp,
                modifier = Modifier
                    .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        },
        colors = appBarColors,
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
            KeyedTopAppBarActions(
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
    topAppBarState: TopAppBarData,
    topAppBarContent: Color,
    navController: NavController
) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    // Используем key чтобы гарантировать пересоздание при изменении типа экрана
    when {
        topAppBarState.showSchemeEditorActions -> {
            SchemeEditorActions(
                topAppBarState = topAppBarState,
                topAppBarContent = topAppBarContent
            )
        }

        // ★ ПОЛНОЭКРАННОЕ ФОТО
        topAppBarState.showPhotoActions -> {
            FullScreenPhotoActions(
                topAppBarState = topAppBarState,
                topAppBarContent = topAppBarContent
            )
        }

        currentRoute == "schemes" -> {
            SchemesScreenActions(
                topAppBarState = topAppBarState,
                topAppBarContent = topAppBarContent,
                navController = navController
            )
        }

        currentRoute == "photos" -> {
            PhotosScreenActions(
                topAppBarState = topAppBarState,
                topAppBarContent = topAppBarContent,
                navController = navController
            )
        }

        currentRoute == "devices" -> {
            DevicesScreenActions(
                topAppBarContent = topAppBarContent,
                navController = navController
            )
        }

        currentRoute == "reports" -> {
            // Те же кнопки что на devices/schemes — тема + настройки
            Row(verticalAlignment = Alignment.CenterVertically) {
                ThemeToggleButton(contentColor = topAppBarContent)
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { navController.navigate("settings") }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Настройки", tint = topAppBarContent)
                }
            }
        }

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

    val disabledTint = remember(topAppBarContent) {
        topAppBarContent.copy(alpha = 0.35f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        // ★ КНОПКА «ОЧИСТИТЬ СХЕМУ»
        if (topAppBarState.showClearButton) {
            IconButton(
                onClick = {
                    Timber.d("Кнопка очистки схемы нажата")
                    topAppBarState.onClearClick?.invoke()
                },
                enabled = topAppBarState.canClear
            ) {
                Icon(
                    Icons.Default.CleaningServices,
                    contentDescription = "Очистить схему",
                    tint = if (topAppBarState.canClear) topAppBarContent else disabledTint
                )
            }
        }

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
                    Icons.Default.Info,
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
    val schemesViewModel: SchemesViewModel = hiltViewModel()
    val uiState by schemesViewModel.uiState.collectAsStateWithLifecycle()

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (topAppBarState.showSchemesFilterMenu) {
            SchemesFilterMenu(
                searchQuery = uiState.searchQuery,        // ← из VM напрямую
                onSearchQueryChange = { schemesViewModel.setSearchQuery(it) },
                currentSort = uiState.sortBy,             // ← из VM напрямую
                onSortSelected = { schemesViewModel.setSortBy(it) },
                onResetAllFilters = { schemesViewModel.resetAllFilters() },
                modifier = Modifier.padding(end = 4.dp)
            )
        }
        ThemeToggleButton(contentColor = topAppBarContent)
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = { navController.navigate("settings") }) {
            Icon(Icons.Filled.Settings, contentDescription = "Настройки", tint = topAppBarContent)
        }
    }
}

@Composable
private fun PhotosScreenActions(
    topAppBarState: TopAppBarData,
    topAppBarContent: Color,
    navController: NavController
) {
    val photosViewModel: PhotosViewModel = hiltViewModel()
    val uiState by photosViewModel.uiState.collectAsStateWithLifecycle()

    Row(verticalAlignment = Alignment.CenterVertically) {
        PhotosFilterMenu(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { photosViewModel.setSearchQuery(it) },
            currentSort = uiState.sortBy,
            onSortSelected = { photosViewModel.setSortBy(it) },
            onResetAllFilters = { photosViewModel.resetAllFilters() },
            modifier = Modifier.padding(end = 4.dp)
        )
        ThemeToggleButton(contentColor = topAppBarContent)
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = { navController.navigate("settings") }) {
            Icon(Icons.Filled.Settings, contentDescription = "Настройки", tint = topAppBarContent)
        }
    }
}

@Composable
private fun DevicesScreenActions(
    topAppBarContent: Color,
    navController: NavController
) {
    val devicesViewModel: DevicesViewModel = hiltViewModel()
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
private fun FullScreenPhotoActions(
    topAppBarState: TopAppBarData,
    topAppBarContent: Color
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = { Icon(Icons.Filled.Info, null) },
            title = { Text("Информация о фото") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PhotoInfoRow("Файл", topAppBarState.photoFileName ?: "—")
                    PhotoInfoRow("Путь", topAppBarState.photoFilePath ?: "—")
                    PhotoInfoRow("Инв. №", topAppBarState.photoInventoryNumber ?: "—")
                    if (!topAppBarState.photoValveNumber.isNullOrBlank()) {
                        PhotoInfoRow("Кран №", topAppBarState.photoValveNumber)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) { Text("Закрыть") }
            }
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp)
    ) {
        IconButton(onClick = { showInfoDialog = true }) {
            Icon(Icons.Filled.Info, "Информация о фото", tint = topAppBarContent)
        }
        // Кнопка Delete вызывает onDeletePhotoClick → открывает диалог в FullScreenPhotoScreen
        IconButton(onClick = { topAppBarState.onDeletePhotoClick?.invoke() }) {
            Icon(Icons.Filled.Delete, "Удалить фото", tint = topAppBarContent)
        }
    }
}

@Composable
private fun PhotoInfoRow(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodySmall)
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