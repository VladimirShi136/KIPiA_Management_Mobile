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
import androidx.compose.ui.text.style.TextAlign
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
import com.kipia.management.mobile.ui.theme.Dimens
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
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    var showDeleteDialog by remember { mutableStateOf<Scheme?>(null) }
    var showError by remember { mutableStateOf<String?>(null) }

    val shouldShowBottomNav by remember(scrollState) {
        derivedStateOf {
            with(scrollState) {
                firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0
            }
        }
    }

    LaunchedEffect(shouldShowBottomNav) {
        updateBottomNavVisibility(shouldShowBottomNav)
    }

    val showScrollToTopButton = !shouldShowBottomNav

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

    // Уведомления теперь обрабатываются глобально в MainActivity

    val scrollToTop: () -> Unit = {
        scope.launch { scrollState.animateScrollToItem(0) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .only(WindowInsetsSides.Bottom)
                        .add(WindowInsets(bottom = 0.dp))
                )
        ) {
            SchemesActiveFiltersBadge(
                searchQuery = uiState.searchQuery,
                currentSort = uiState.sortBy,
                onClearFilters = { viewModel.resetAllFilters() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.screenPadding, vertical = Dimens.screenPadding)
            )

            when {
                uiState.isLoading -> LoadingState()
                schemesWithStatus.isEmpty() -> EmptySchemesState(modifier = Modifier.weight(1f))
                else -> SchemesList(
                    schemesWithStatus = schemesWithStatus,
                    scrollState = scrollState,
                    onSchemeClick = { scheme -> onNavigateToSchemeEditor(scheme.id) },
                    onEditScheme = { scheme -> onNavigateToSchemeEditor(scheme.id) },
                    onDeleteScheme = { scheme ->
                        if (schemesWithStatus.find { it.scheme.id == scheme.id }?.canDelete == true) {
                            showDeleteDialog = scheme
                        } else {
                            showError = "Нельзя удалить схему '${scheme.name}'. К ней привязаны приборы."
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // ── FAB-кнопки (наверх + добавить) ───────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Dimens.spacingXLarge + Dimens.spacingMedium, bottom = Dimens.fabBottomPadding)
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(Dimens.spacingFab)
        ) {
            AnimatedVisibility(
                visible = showScrollToTopButton,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                FloatingActionButton(
                    onClick = scrollToTop,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(Dimens.fabSize)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward,
                        contentDescription = "Наверх",
                        modifier = Modifier.size(Dimens.iconSizeMedium)
                    )
                }
            }
        }

        // Диалог удаления
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

        // Диалог ошибки
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
        verticalArrangement = Arrangement.spacedBy(Dimens.screenPadding),
        contentPadding = PaddingValues(Dimens.screenPadding)
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
                .padding(Dimens.spacingLarge)
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
                        modifier = Modifier.size(Dimens.iconSizeXLarge)
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
                    modifier = Modifier.padding(top = Dimens.spacingMedium)
                )
            }

            if (!scheme.description.isNullOrBlank()) {
                Text(
                    text = scheme.description,
                    style = typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = Dimens.spacingSmall)
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingSmall))
            Text(
                text = "Обновлено: $formattedDate",
                style = typography.bodySmall,
                color = subtitleColor
            )

            val bgImage = schemeData.backgroundImage
            if (bgImage != null) {
                Spacer(modifier = Modifier.height(Dimens.cardPadding))
                Card(
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(Dimens.chipRadius)
                ) {
                    AsyncImage(
                        model = bgImage,
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(Dimens.spacingLarge))
            Text("Загрузка схем...")
        }
    }
}

@Composable
fun EmptySchemesState(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimens.spacingXXLarge),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.GridOn,
                contentDescription = null,
                modifier = Modifier.size(Dimens.iconSizeXXLarge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(Dimens.spacingLarge))

            Text(
                text = "Нет схем",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(Dimens.spacingMedium))

            Text(
                text = "Схемы создаются автоматически на основе мест установки приборов.\nДобавьте прибор с новой локацией, чтобы создать схему.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

enum class SchemesSortBy {
    NAME_ASC, NAME_DESC
}