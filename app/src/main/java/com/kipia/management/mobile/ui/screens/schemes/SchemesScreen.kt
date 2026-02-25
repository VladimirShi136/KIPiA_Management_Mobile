package com.kipia.management.mobile.ui.screens.schemes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.ui.components.scheme.SchemesActiveFiltersBadge
import com.kipia.management.mobile.ui.shared.NotificationManager
import com.kipia.management.mobile.viewmodel.DeleteResult
import com.kipia.management.mobile.viewmodel.SchemeWithStatus
import com.kipia.management.mobile.viewmodel.SchemesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemesScreen(
    onNavigateToSchemeEditor: (Int) -> Unit,
    updateBottomNavVisibility: (Boolean) -> Unit = {},
    topAppBarController: com.kipia.management.mobile.ui.components.topappbar.TopAppBarController? = null,
    viewModel: SchemesViewModel = hiltViewModel(),
    notificationManager: NotificationManager
) {
    val schemesWithStatus by viewModel.getSchemesWithStatus()
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val context = LocalContext.current

    var showDeleteDialog by remember { mutableStateOf<Scheme?>(null) }
    var showError by remember { mutableStateOf<String?>(null) }

    // ★ ЛОГИКА СКРЫТИЯ BOTTOM NAV ПРИ ПРОКРУТКЕ (как в PhotosScreen)
    val shouldShowBottomNav by remember(scrollState) {
        derivedStateOf {
            with(scrollState) {
                firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
            }
        }
    }

    // ★ Обновляем видимость BottomNav
    LaunchedEffect(shouldShowBottomNav) {
        Timber.d("SchemesScreen: BottomNav видимость = $shouldShowBottomNav")
        updateBottomNavVisibility(shouldShowBottomNav)
    }

    // ★ ЛОГИКА ДЛЯ КНОПКИ "НАВЕРХ"
    val showScrollToTopButton by remember(shouldShowBottomNav) {
        derivedStateOf {
            !shouldShowBottomNav
        }
    }

    // ★ Настраиваем TopAppBar через контроллер
    LaunchedEffect(topAppBarController, uiState) {
        topAppBarController?.setForScreen("schemes", buildMap { //
            put("title", "Учет приборов КИПиА")
            put("searchQuery", uiState.searchQuery)
            put("currentSort", uiState.sortBy)
            put("onSearchQueryChange", { query: String ->
                viewModel.setSearchQuery(query)
            })
            put("onSortSelected", { sortBy: SchemesSortBy ->
                viewModel.setSortBy(sortBy)
            })
            put("onResetAllFilters", {
                viewModel.resetAllFilters()
            })
            put("showThemeToggle", true)
            put("showSettingsIcon", true)
        })
    }

    LaunchedEffect(Unit) {
        notificationManager.notification.collect { notification ->
            // Пропускаем пустые уведомления
            if (notification is NotificationManager.Notification.None) {
                return@collect
            }

            val message = when (notification) {
                is NotificationManager.Notification.SchemeSaved -> {
                    "Схема '${notification.schemeName}' сохранена"
                }
                is NotificationManager.Notification.Error -> {
                    "Ошибка: ${notification.message}"
                }
                // Можно добавить обработку других типов, если нужно
                else -> null
            }

            if (message != null) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                    // Очищаем replay cache после показа
                    delay(100)
                    notificationManager.clearLastNotification()
                }
            }
        }
    }


    val scrollToTop: () -> Unit = {
        scope.launch {
            scrollState.animateScrollToItem(0)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Основной контент
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)  // Тот же padding, что в DevicesScreen
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .add(WindowInsets(bottom = 0.dp))
                )
        ) {
            // Активные фильтры
            SchemesActiveFiltersBadge(
                searchQuery = uiState.searchQuery,
                currentSort = uiState.sortBy,
                onClearFilters = { viewModel.resetAllFilters() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            )

            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                schemesWithStatus.isEmpty() -> {
                    EmptySchemesState(
                        modifier = Modifier.fillMaxSize()
                    )
                }
                else -> {
                    SchemesList(
                        schemesWithStatus = schemesWithStatus,
                        scrollState = scrollState,
                        onSchemeClick = { scheme -> onNavigateToSchemeEditor(scheme.id) },
                        onEditScheme = { scheme -> onNavigateToSchemeEditor(scheme.id) },
                        onDeleteScheme = { scheme ->
                            if (schemesWithStatus.find { it.scheme.id == scheme.id }?.canDelete == true) {
                                showDeleteDialog = scheme
                            } else {
                                showError = "Нельзя удалить схему '${scheme.name}'. " +
                                        "К ней привязаны приборы."
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // ★★★★ COLUMN ДЛЯ ВЕРТИКАЛЬНОГО РАСПОЛОЖЕНИЯ КНОПОК (как в DevicesScreen) ★★★★
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 46.dp,
                    bottom = 30.dp
                )
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ★★★★ КНОПКА "ВВЕРХ" (появляется когда навигация скрыта) ★★★★
            AnimatedVisibility(
                visible = showScrollToTopButton,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = scrollToTop,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Наверх",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Snackbar для уведомлений (поверх всего)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        // Диалог подтверждения удаления
        showDeleteDialog?.let { scheme ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Удаление схемы") },
                text = {
                    Column {
                        Text("Вы уверены, что хотите удалить схему:")
                        Spacer(modifier = Modifier.height(4.dp))
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
                            scope.launch {
                                when (val result = viewModel.deleteScheme(scheme)) {
                                    is DeleteResult.Success -> {
                                        // Можно показать Snackbar
                                    }
                                    is DeleteResult.Error -> {
                                        showError = result.message
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
        showError?.let { error ->
            AlertDialog(
                onDismissRequest = { showError = null },
                title = { Text("Нельзя удалить схему") },
                text = { Text(error) },
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
fun SchemesList(
    schemesWithStatus: List<SchemeWithStatus>,
    scrollState: LazyListState,
    onSchemeClick: (Scheme) -> Unit,
    onEditScheme: (Scheme) -> Unit,
    onDeleteScheme: (Scheme) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = scrollState,
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(schemesWithStatus.size, key = { index -> schemesWithStatus[index].scheme.id }) { index ->
            val item = schemesWithStatus[index]
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
            text = "Схемы создаются автоматически на основе мест установки приборов.\nДобавьте прибор с новой локацией, чтобы создать схему.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

enum class SchemesSortBy {
    NAME_ASC, NAME_DESC
}