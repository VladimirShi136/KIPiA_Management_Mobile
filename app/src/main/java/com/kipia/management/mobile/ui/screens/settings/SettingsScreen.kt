package com.kipia.management.mobile.ui.screens.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import com.kipia.management.mobile.ui.theme.Dimens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kipia.management.mobile.BuildConfig
import com.kipia.management.mobile.repository.PreferencesRepository
import com.kipia.management.mobile.ui.components.sync.ConflictResolutionDialog
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.viewmodel.SettingsViewModel
import com.kipia.management.mobile.viewmodel.SyncState
import com.kipia.management.mobile.viewmodel.ThemeViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Экран настроек.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    topAppBarController: TopAppBarController,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    updateBottomNavVisibility: (Boolean) -> Unit = {}
) {
    LaunchedEffect(Unit) {
        updateBottomNavVisibility(false)
        topAppBarController.setForScreen(
            "settings",
            mapOf(
                "title" to "Настройки",
                "showBackButton" to true,
                "onBackClick" to { navController.popBackStack() }
            )
        )
    }
    
    DisposableEffect(Unit) { onDispose { updateBottomNavVisibility(true) } }

    val themeMode by themeViewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColors by themeViewModel.dynamicColors.collectAsStateWithLifecycle()
    val syncState by settingsViewModel.syncState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val supportsDynamicColors = themeViewModel.supportsDynamicColors
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val lastExport by settingsViewModel.lastExportTimestamp.collectAsStateWithLifecycle()
    val lastImport by settingsViewModel.lastImportTimestamp.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }
    val context = LocalContext.current

    // Для скрытого меню
    var debugClickCount by remember { mutableIntStateOf(0) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { settingsViewModel.exportDatabase(it) }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { pendingImportUri = it }
    }

    // 1. Диалог подтверждения импорта
    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("Подтвердите импорт") },
            text = {
                Text("Данные из файла будут объединены с текущей базой. Более новые записи заменят старые. Продолжить?")
            },
            confirmButton = {
                TextButton(onClick = {
                    settingsViewModel.importDatabase(pendingImportUri!!)
                    pendingImportUri = null
                }) { Text("Импортировать") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Отмена") }
            }
        )
    }

    // 2. Обработка состояний синхронизации (Результаты и Конфликты)
    when (val state = syncState) {
        is SyncState.ExportSuccess -> {
            SyncResultDialog(
                title = "Экспорт завершён",
                message = "База данных и фотографии успешно сохранены в файл.",
                icon = Icons.Filled.CheckCircle,
                iconColor = MaterialTheme.colorScheme.primary,
                onDismiss = { settingsViewModel.resetState() }
            )
        }
        is SyncState.ImportSuccess -> {
            SyncResultDialog(
                title = "Импорт завершён",
                message = state.stats.toSummary(),
                icon = Icons.Filled.CheckCircle,
                iconColor = MaterialTheme.colorScheme.primary,
                onDismiss = { settingsViewModel.resetState() }
            )
        }
        is SyncState.Error -> {
            SyncResultDialog(
                title = "Ошибка",
                message = state.message,
                icon = Icons.Filled.Error,
                iconColor = MaterialTheme.colorScheme.error,
                onDismiss = { settingsViewModel.resetState() }
            )
        }
        is SyncState.ConflictsDetected -> {
            ConflictResolutionDialog(
                conflicts = state.conflicts,
                onDismiss = { settingsViewModel.resetState() },
                onResolve = { resolutions ->
                    settingsViewModel.resolveConflicts(resolutions)
                }
            )
        }
        is SyncState.GhostDevicesDetected -> {
            AlertDialog(
                onDismissRequest = { settingsViewModel.resetState() },
                icon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Найдена корзина") },
                text = {
                    Text("В импортируемых данных найдено ${state.ghostDevices.size} удаленных приборов, которых нет в вашей базе. " +
                            "Хотите восстановить их в корзину или пропустить?")
                },
                confirmButton = {
                    TextButton(onClick = { settingsViewModel.resolveGhostDevices(true) }) {
                        Text("Восстановить")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { settingsViewModel.resolveGhostDevices(false) }) {
                        Text("Пропустить")
                    }
                }
            )
        }
        else -> {}
    }

    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.screenPadding)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = Dimens.spacingMedium),
        verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
    ) {
        // ─── Синхронизация ───────────────────────────────────────
        Card(modifier = cardModifier) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(
                    text = "Синхронизация",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = Dimens.spacingMedium)
                )
                Text(
                    text = "Обменивайтесь данными между Android и ПК через ZIP-файл.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimens.spacingLarge)
                )
                Text(
                    text = "Последний экспорт: ${lastExport?.let { dateFormat.format(Date(it)) } ?: "не выполнялся"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimens.spacingSmall)
                )
                Text(
                    text = "Последний импорт: ${lastImport?.let { dateFormat.format(Date(it)) } ?: "не выполнялся"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimens.spacingLarge)
                )

                val isLoading = syncState is SyncState.Loading

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
                ) {
                    OutlinedButton(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            exportLauncher.launch("kipia_backup_$timestamp.zip")
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Upload, null, modifier = Modifier.size(Dimens.iconSizeSmall))
                        Spacer(Modifier.width(Dimens.spacingMedium))
                        Text("Экспорт", maxLines = 1)
                    }

                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Download, null, modifier = Modifier.size(Dimens.iconSizeSmall))
                        Spacer(Modifier.width(Dimens.spacingMedium))
                        Text("Импорт", maxLines = 1)
                    }
                }

                if (isLoading) {
                    Spacer(Modifier.height(Dimens.spacingFab))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(Dimens.iconSizeXSmall), strokeWidth = 2.dp)
                        Text(
                            text = (syncState as SyncState.Loading).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // ─── Внешний вид ─────────────────────────────────────────
        Card(modifier = cardModifier) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(
                    text = "Внешний вид",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = Dimens.spacingLarge)
                )

                Text(
                    text = "Тема приложения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = Dimens.spacingMedium)
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = themeMode == PreferencesRepository.THEME_FOLLOW_SYSTEM,
                        onClick = { themeViewModel.setTheme(PreferencesRepository.THEME_FOLLOW_SYSTEM) },
                        icon = { Icon(Icons.Filled.SettingsBrightness, null, modifier = Modifier.size(Dimens.iconSizeSmall)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        modifier = Modifier.weight(1f)
                    ) { Text("Система", fontSize = 10.sp, maxLines = 1) }

                    SegmentedButton(
                        selected = themeMode == PreferencesRepository.THEME_LIGHT,
                        onClick = { themeViewModel.setTheme(PreferencesRepository.THEME_LIGHT) },
                        icon = { Icon(Icons.Filled.LightMode, null, modifier = Modifier.size(Dimens.iconSizeSmall)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        modifier = Modifier.weight(1f)
                    ) { Text("Светлая", fontSize = 10.sp, maxLines = 1) }

                    SegmentedButton(
                        selected = themeMode == PreferencesRepository.THEME_DARK,
                        onClick = { themeViewModel.setTheme(PreferencesRepository.THEME_DARK) },
                        icon = { Icon(Icons.Filled.DarkMode, null, modifier = Modifier.size(Dimens.iconSizeSmall)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        modifier = Modifier.weight(1f)
                    ) { Text("Темная", fontSize = 10.sp, maxLines = 1) }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingLarge))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Динамические цвета", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (supportsDynamicColors) "Использовать цвета системы" else "Доступно на Android 12+",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (supportsDynamicColors) 1f else 0.7f)
                        )
                    }
                    Switch(
                        checked = dynamicColors && supportsDynamicColors,
                        onCheckedChange = { if (supportsDynamicColors) themeViewModel.toggleDynamicColors() },
                        enabled = supportsDynamicColors
                    )
                }
            }
        }

        // ─── О приложении ─────────────────────────────────────────
        Card(modifier = cardModifier) {
            Column(modifier = Modifier.padding(Dimens.cardPadding)) {
                Text(
                    text = "О приложении",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = Dimens.spacingFab)
                )

                AboutRow(
                    icon = Icons.Filled.Info,
                    label = "Версия",
                    value = BuildConfig.VERSION_NAME,
                    onClick = {
                        debugClickCount++
                        if (debugClickCount >= 7) {
                            debugClickCount = 0
                            navController.navigate("debug_settings")
                        } else if (debugClickCount > 3) {
                            Toast.makeText(context, "Еще ${7 - debugClickCount} нажатий до меню разработчика", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                AboutRow(
                    icon = Icons.Filled.Code,
                    label = "Исходный код",
                    value = "KIPiA_Management_Mobile",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/VladimirShi136/KIPiA_Management_Mobile"))
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun AboutRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.cardRadius))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = Dimens.spacingMedium, horizontal = Dimens.spacingSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingLarge)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = if (label == "Исходный код") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SyncResultDialog(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(icon, null, tint = iconColor) },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
    )
}
