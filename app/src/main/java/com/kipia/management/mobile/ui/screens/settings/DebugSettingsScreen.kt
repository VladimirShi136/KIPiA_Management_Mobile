package com.kipia.management.mobile.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kipia.management.mobile.managers.DatabaseIntegrityManager
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.ui.theme.Dimens
import com.kipia.management.mobile.viewmodel.DebugSettingsViewModel

/**
 * Экран для разработчиков.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugSettingsScreen(
    navController: NavController,
    topAppBarController: TopAppBarController,
    updateBottomNavVisibility: (Boolean) -> Unit = {},
    viewModel: DebugSettingsViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        updateBottomNavVisibility(false)
        topAppBarController.setForScreen(
            "debug_settings",
            mapOf(
                "title" to "Инженерное меню",
                "showBackButton" to true,
                "onBackClick" to { navController.popBackStack() }
            )
        )
    }
    
    DisposableEffect(Unit) {
        onDispose { }
    }

    val scrollState = rememberScrollState()
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.screenPadding)

    val isCheckingIntegrity by viewModel.isCheckingIntegrity.collectAsStateWithLifecycle()
    val integrityResult by viewModel.integrityResult.collectAsStateWithLifecycle()
    var showIntegrityDialog by remember { mutableStateOf(false) }
    var showCleanupDialog by remember { mutableStateOf(false) }

    if (showIntegrityDialog && integrityResult != null) {
        IntegrityCheckDialog(
            result = integrityResult!!,
            onDismiss = { showIntegrityDialog = false },
            onCleanup = {
                showCleanupDialog = true
                showIntegrityDialog = false
            }
        )
    }

    if (showCleanupDialog && integrityResult != null) {
        CleanupConfirmDialog(
            result = integrityResult!!,
            viewModel = viewModel,
            onDismiss = { showCleanupDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = Dimens.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
    ) {
        Card(modifier = cardModifier) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(
                    text = "Функции обслуживания",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = Dimens.spacingMedium)
                )
                
                ListItem(
                    headlineContent = { Text("Очистка мягкоудаленных файлов") },
                    supportingContent = { Text("Будет добавлено позже") },
                    leadingContent = { Icon(Icons.Default.DeleteSweep, null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.cardRadius))
                        .clickable { /* Будет добавлено позже */ }
                )
                
                HorizontalDivider(modifier = Modifier.padding(vertical = Dimens.spacingSmall))
                
                ListItem(
                    headlineContent = { Text("Просмотр всех фото в папке") },
                    supportingContent = { Text("Будет добавлено позже") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                    trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.cardRadius))
                        .clickable { /* Будет добавлено позже */ }
                )
            }
        }
        
        Card(modifier = cardModifier) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(
                    text = "База данных",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = Dimens.spacingMedium)
                )
                
                ListItem(
                    headlineContent = { Text("Проверка целостности") },
                    supportingContent = {
                        Text(if (isCheckingIntegrity) "Проверка..." else "Проверить консистентность БД и файлов")
                    },
                    leadingContent = { Icon(Icons.Default.Storage, null) },
                    trailingContent = {
                        if (isCheckingIntegrity) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    },
                     modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(Dimens.cardRadius))
                    // .clickable(enabled = !isCheckingIntegrity) {
                    //     viewModel.checkIntegrity()
                    //     showIntegrityDialog = true
                    // }
                )
            }
        }
    }
}

@Composable
private fun IntegrityCheckDialog(
    result: DatabaseIntegrityManager.IntegrityCheckResult,
    onDismiss: () -> Unit,
    onCleanup: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (result.isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                null,
                tint = if (result.isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        },
        title = { Text(result.getSummary()) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = result.getDetailedReport(),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        },
        confirmButton = {
            if (!result.isHealthy && (result.orphanedFiles.isNotEmpty() || result.emptyFolders.isNotEmpty())) {
                Button(onClick = onCleanup) {
                    Text("Очистить")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Закрыть") }
        }
    )
}

@Composable
private fun CleanupConfirmDialog(
    result: DatabaseIntegrityManager.IntegrityCheckResult,
    viewModel: DebugSettingsViewModel,
    onDismiss: () -> Unit
) {
    val cleanupMessage by viewModel.cleanupMessage.collectAsStateWithLifecycle()
    val isCleaningUp by viewModel.isCleaningUp.collectAsStateWithLifecycle()

    LaunchedEffect(cleanupMessage) {
        if (cleanupMessage != null) {
            // Сообщение показано, закрываем диалог через 2 секунды
        }
    }

    if (cleanupMessage != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Очистка завершена") },
            text = { Text(cleanupMessage!!) },
            confirmButton = {
                TextButton(onClick = onDismiss) { Text("OK") }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Default.DeleteSweep, null) },
            title = { Text("Подтвердите очистку") },
            text = {
                Text(buildString {
                    append("Будут удалены:\n\n")
                    if (result.orphanedFiles.isNotEmpty()) {
                        append("• ${result.orphanedFiles.size} сирот-файлов\n")
                    }
                    if (result.emptyFolders.isNotEmpty()) {
                        append("• ${result.emptyFolders.size} пустых папок\n")
                    }
                    append("\nЭто невозможно отменить!")
                })
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.performCleanup(result) },
                    enabled = !isCleaningUp
                ) {
                    if (isCleaningUp) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Удалить")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Отмена") }
            }
        )
    }
}

