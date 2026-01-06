package com.kipia.management.mobile.ui.screens.schemes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.viewmodel.SchemesViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemesScreen(
    onNavigateToSchemeEditor: (Int?) -> Unit,
    viewModel: SchemesViewModel = hiltViewModel()
) {
    val schemes by viewModel.schemes.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            schemes.isEmpty() -> {
                EmptySchemesState(
                    onCreateScheme = { onNavigateToSchemeEditor(null) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                SchemesList(
                    schemes = schemes,
                    searchQuery = uiState.searchQuery,
                    onSchemeClick = { scheme -> onNavigateToSchemeEditor(scheme.id) },
                    onEditScheme = { scheme -> onNavigateToSchemeEditor(scheme.id) },
                    onDeleteScheme = { scheme -> viewModel.deleteScheme(scheme) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
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
            Icon(Icons.Default.Sort, contentDescription = "Сортировка")
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
                    Icon(
                        if (currentSort == SortBy.NAME_ASC) Icons.Default.Check else null,
                        contentDescription = null
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("По имени (Я-А)") },
                onClick = {
                    onSortSelected(SortBy.NAME_DESC)
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        if (currentSort == SortBy.NAME_DESC) Icons.Default.Check else null,
                        contentDescription = null
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("По дате (новые)") },
                onClick = {
                    onSortSelected(SortBy.DATE_DESC)
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        if (currentSort == SortBy.DATE_DESC) Icons.Default.Check else null,
                        contentDescription = null
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("По дате (старые)") },
                onClick = {
                    onSortSelected(SortBy.DATE_ASC)
                    expanded = false
                },
                leadingIcon = {
                    Icon(
                        if (currentSort == SortBy.DATE_ASC) Icons.Default.Check else null,
                        contentDescription = null
                    )
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
    schemes: List<Scheme>,
    searchQuery: String,
    onSchemeClick: (Scheme) -> Unit,
    onEditScheme: (Scheme) -> Unit,
    onDeleteScheme: (Scheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredSchemes = if (searchQuery.isBlank()) {
        schemes
    } else {
        schemes.filter { scheme ->
            scheme.name.contains(searchQuery, ignoreCase = true) ||
                    scheme.description?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(filteredSchemes, key = { it.id }) { scheme ->
            SchemeCard(
                scheme = scheme,
                onClick = { onSchemeClick(scheme) },
                onEdit = { onEditScheme(scheme) },
                onDelete = { onDeleteScheme(scheme) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeCard(
    scheme: Scheme,
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
                Text(
                    text = scheme.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

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
                            }
                        )
                    }
                }
            }

            // Премиум схема
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
                    text = "${schemeData.devices.size} приборов",
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

            // Даты создания и обновления
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Создано: ${formatDate(scheme.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (scheme.updatedAt > scheme.createdAt) {
                    Text(
                        text = "Изменено: ${formatDate(scheme.updatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun formatDate(timestamp: Long): String {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
    return dateFormat.format(Date(timestamp))
}

enum class SortBy {
    NAME_ASC, NAME_DESC, DATE_ASC, DATE_DESC
}