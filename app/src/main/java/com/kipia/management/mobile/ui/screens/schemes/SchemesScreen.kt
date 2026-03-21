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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.ui.components.dialogs.DeleteConfirmDialog
import com.kipia.management.mobile.ui.components.dialogs.ErrorDialog
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
    val showScrollToTopButton = !shouldShowBottomNav

    // ★ Настраиваем TopAppBar через контроллер
    LaunchedEffect(topAppBarController) {
        topAppBarController?.setForScreen("schemes", buildMap {
            put("title", "Учет приборов КИПиА")
            put("showThemeToggle", true)
            put("showSettingsIcon", true)
            put("onSearchQueryChange", { query: String -> viewModel.setSearchQuery(query) })
            put("onSortSelected", { sortBy: SchemesSortBy -> viewModel.setSortBy(sortBy) })
            put("onResetAllFilters", { viewModel.resetAllFilters() })
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
            DeleteConfirmDialog(
                title = "Удаление схемы",
                itemName = "'${scheme.name}'",
                message = if (scheme.description?.isNotBlank() == true)
                    "Описание: ${scheme.description}" else null,
                onConfirm = {
                    scope.launch {
                        when (val result = viewModel.deleteScheme(scheme)) {
                            is DeleteResult.Error -> showError = result.message
                            else -> {}
                        }
                    }
                    showDeleteDialog = null
                },
                onDismiss = { showDeleteDialog = null }
            )
        }

        // Ошибка
        showError?.let { error ->
            ErrorDialog(
                title = "Нельзя удалить схему",
                message = error,
                onDismiss = { showError = null }
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(6.dp)
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
    val schemeData = remember(scheme.id) { scheme.getSchemeData() }

    // Кэшируем стили — пересчитываются только при смене темы
    val typography = MaterialTheme.typography
    val colorScheme = MaterialTheme.colorScheme

    val titleStyle = remember(typography) {
        typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    }
    val subtitleColor = remember(colorScheme) {
        colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    }
    val cardColor = remember(colorScheme) {
        colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    // Кэшируем форматирование даты — пересчитывается только при смене updatedAt
    val formattedDate = remember(scheme.updatedAt) {
        java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(scheme.updatedAt))
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scheme.name,
                        style = titleStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (deviceCount > 0) {
                        Text(
                            text = "$deviceCount приборов привязано к локации",
                            style = typography.bodySmall,
                            color = colorScheme.primary
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
                            onClick = { showMenu = false; onEdit() },
                            leadingIcon = { Icon(Icons.Default.Edit, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить") },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null) },
                            enabled = canDelete
                        )
                    }
                }
            }

            if (!canDelete && deviceCount > 0) {
                Text(
                    text = "⚠️ Нельзя удалить: используется $deviceCount прибором(ами)",
                    style = typography.labelSmall,
                    color = colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (!scheme.description.isNullOrBlank()) {
                Text(
                    text = scheme.description,
                    style = typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Обновлено: $formattedDate",
                style = typography.bodySmall,
                color = subtitleColor
            )

            schemeData.backgroundImage?.let { backgroundImage ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
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