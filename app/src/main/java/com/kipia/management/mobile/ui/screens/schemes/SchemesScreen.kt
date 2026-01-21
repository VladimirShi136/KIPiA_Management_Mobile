package com.kipia.management.mobile.ui.screens.schemes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.viewmodel.DeleteResult
import com.kipia.management.mobile.viewmodel.SchemeWithStatus
import com.kipia.management.mobile.viewmodel.SchemesViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemesScreen(
    onNavigateToSchemeEditor: (Int?) -> Unit,
    viewModel: SchemesViewModel = hiltViewModel()
) {
    val schemesWithStatus by viewModel.getSchemesWithStatus()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showDeleteDialog by remember { mutableStateOf<Scheme?>(null) }
    var showError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Схемы") },
                actions = {
                    // Сортировка
                    SortMenu(
                        currentSort = uiState.sortBy,
                        onSortSelected = { viewModel.setSortBy(it) }
                    )

                    // Поиск
                    SearchField(
                        searchQuery = uiState.searchQuery,
                        onSearchQueryChanged = { viewModel.setSearchQuery(it) }
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToSchemeEditor(null) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить схему")
            }
        }
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                LoadingState()
            }
            schemesWithStatus.isEmpty() -> {
                EmptySchemesState(
                    onCreateScheme = { onNavigateToSchemeEditor(null) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                SchemesList(
                    schemesWithStatus = schemesWithStatus,
                    searchQuery = uiState.searchQuery,
                    onSchemeClick = { scheme -> onNavigateToSchemeEditor(scheme.id) },
                    onEditScheme = { scheme -> onNavigateToSchemeEditor(scheme.id) },
                    onDeleteScheme = { scheme ->
                        // ★★★★ ПРОВЕРЯЕМ МОЖНО ЛИ УДАЛИТЬ ★★★★
                        if (schemesWithStatus.find { it.scheme.id == scheme.id }?.canDelete == true) {
                            showDeleteDialog = scheme
                        } else {
                            showError = "Нельзя удалить схему '$scheme.name'. " +
                                    "К ней привязаны приборы."
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }

        // Диалог подтверждения удаления
        if (showDeleteDialog != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Удаление схемы") },
                text = {
                    Column {
                        Text("Вы уверены, что хотите удалить схему:")
                        Spacer(modifier = Modifier.height(4.dp))
                        showDeleteDialog?.let { scheme ->
                            Text(
                                text = "'${scheme.name}'",
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            if (scheme.description?.isNotBlank() == true) {
                                Text(
                                    text = "Описание: ${scheme.description}",
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Это действие нельзя отменить.",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog?.let { scheme ->
                                scope.launch {
                                    val result = viewModel.deleteScheme(scheme)
                                    when (result) {
                                        is DeleteResult.Success -> {
                                            // Можно показать Snackbar
                                        }
                                        is DeleteResult.Error -> {
                                            showError = result.message
                                        }
                                    }
                                }
                            }
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Удалить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Отмена")
                    }
                }
            )
        }

        // Ошибка
        if (showError != null) {
            AlertDialog(
                onDismissRequest = { showError = null },
                title = { Text("Нельзя удалить схему") },
                text = { Text(showError!!) },
                confirmButton = {
                    TextButton(onClick = { showError = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
fun SortMenu(
    currentSort: SortBy,
    onSortSelected: (SortBy) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Сортировка")
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("По имени (А-Я)") },
                onClick = {
                    onSortSelected(SortBy.NAME_ASC)
                    expanded = false
                },
                leadingIcon = {
                    if (currentSort == SortBy.NAME_ASC) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                }
            )
            DropdownMenuItem(
                text = { Text("По имени (Я-А)") },
                onClick = {
                    onSortSelected(SortBy.NAME_DESC)
                    expanded = false
                },
                leadingIcon = {
                    if (currentSort == SortBy.NAME_DESC) {
                        Icon(Icons.Default.Check, contentDescription = null)
                    } else {
                        Spacer(modifier = Modifier.size(24.dp))
                    }
                }
            )
        }
    }
}

@Composable
fun SearchField(
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    if (expanded) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Поиск схем...") },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                singleLine = true
            )

            IconButton(onClick = {
                expanded = false
                onSearchQueryChanged("")
            }) {
                Icon(Icons.Default.Close, contentDescription = "Закрыть поиск")
            }
        }
    } else {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Search, contentDescription = "Поиск")
        }
    }
}

@Composable
fun SchemesList(
    schemesWithStatus: List<SchemeWithStatus>,
    searchQuery: String,
    onSchemeClick: (Scheme) -> Unit,
    onEditScheme: (Scheme) -> Unit,
    onDeleteScheme: (Scheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredSchemes = if (searchQuery.isBlank()) {
        schemesWithStatus
    } else {
        schemesWithStatus.filter { item ->
            item.scheme.name.contains(searchQuery, ignoreCase = true) ||
                    item.scheme.description?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(filteredSchemes, key = { it.scheme.id }) { item ->
            SchemeCard(
                scheme = item.scheme,
                deviceCount = item.deviceCount,
                canDelete = item.canDelete,
                onClick = { onSchemeClick(item.scheme) },
                onEdit = { onEditScheme(item.scheme) },
                onDelete = { onDeleteScheme(item.scheme) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeCard(
    scheme: Scheme,
    deviceCount: Int,
    canDelete: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val schemeData = scheme.getSchemeData()

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Заголовок с меню
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scheme.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Показываем количество приборов
                    if (deviceCount > 0) {
                        Text(
                            text = "$deviceCount приборов привязано к локации",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Меню")
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Редактировать") },
                            onClick = {
                                showMenu = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            },
                            enabled = canDelete
                        )
                    }
                }
            }

            // Индикатор если нельзя удалить
            if (!canDelete && deviceCount > 0) {
                Text(
                    text = "⚠️ Нельзя удалить: используется $deviceCount прибором(ами)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Информация о схеме
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${schemeData.devices.size} приборов на схеме",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(12.dp))

                Icon(
                    Icons.Default.GridOn,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (schemeData.gridEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Сетка ${schemeData.gridSize}px",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (schemeData.gridEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Описание
            if (!scheme.description.isNullOrBlank()) {
                Text(
                    text = scheme.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Предпросмотр фона (если есть изображение)
            schemeData.backgroundImage?.let { backgroundImage ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    AsyncImage(
                        model = backgroundImage,
                        contentDescription = "Фон схемы",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Загрузка схем...")
        }
    }
}

@Composable
fun EmptySchemesState(
    onCreateScheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.GridOn,
            contentDescription = "Нет схем",
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Нет схем",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Создайте первую схему для планировки помещений\nи размещения приборов",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onCreateScheme,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Создать схему")
        }
    }
}

enum class SortBy {
    NAME_ASC, NAME_DESC
}