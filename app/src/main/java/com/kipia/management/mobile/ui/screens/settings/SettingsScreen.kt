package com.kipia.management.mobile.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kipia.management.mobile.repository.PreferencesRepository
import com.kipia.management.mobile.viewmodel.SettingsViewModel
import com.kipia.management.mobile.viewmodel.SyncState
import com.kipia.management.mobile.viewmodel.ThemeViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    themeViewModel: ThemeViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    updateBottomNavVisibility: (Boolean) -> Unit = {}
) {
    LaunchedEffect(Unit) { updateBottomNavVisibility(false) }
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

    when (val state = syncState) {
        is SyncState.ExportSuccess -> {
            AlertDialog(
                onDismissRequest = { settingsViewModel.resetState() },
                icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Экспорт завершён") },
                text = { Text("База данных и фотографии успешно сохранены в файл.") },
                confirmButton = {
                    TextButton(onClick = { settingsViewModel.resetState() }) { Text("OK") }
                }
            )
        }
        is SyncState.ImportSuccess -> {
            AlertDialog(
                onDismissRequest = { settingsViewModel.resetState() },
                icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Импорт завершён") },
                text = { Text(state.stats.toSummary()) },
                confirmButton = {
                    TextButton(onClick = { settingsViewModel.resetState() }) { Text("OK") }
                }
            )
        }
        is SyncState.Error -> {
            AlertDialog(
                onDismissRequest = { settingsViewModel.resetState() },
                icon = { Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                title = { Text("Ошибка") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { settingsViewModel.resetState() }) { Text("OK") }
                }
            )
        }
        else -> {}
    }

    // Единый отступ для всех карточек
    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 6.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 6.dp), // верхний и нижний отступ колонки
        verticalArrangement = Arrangement.spacedBy(6.dp) // равные отступы между карточками
    ) {
        // ─── Синхронизация ───────────────────────────────────────
        Card(modifier = cardModifier) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "Синхронизация",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Обменивайтесь данными между Android и ПК через ZIP-файл.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Последний экспорт: ${
                        lastExport?.let { dateFormat.format(Date(it)) } ?: "не выполнялся"
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Последний импорт: ${
                        lastImport?.let { dateFormat.format(Date(it)) } ?: "не выполнялся"
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val isLoading = syncState is SyncState.Loading

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                                .format(Date())
                            exportLauncher.launch("kipia_backup_$timestamp.zip")
                        },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Экспорт", maxLines = 1)
                    }

                    Button(
                        onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) },
                        enabled = !isLoading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Импорт", maxLines = 1)
                    }
                }

                if (isLoading) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
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
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "Внешний вид",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Тема приложения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = themeMode == PreferencesRepository.THEME_FOLLOW_SYSTEM,
                        onClick = { themeViewModel.setTheme(PreferencesRepository.THEME_FOLLOW_SYSTEM) },
                        icon = { Icon(Icons.Filled.SettingsBrightness, null, modifier = Modifier.size(16.dp)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        modifier = Modifier.weight(1f)
                    ) { Text("Системная", fontSize = 10.sp, maxLines = 1) }

                    SegmentedButton(
                        selected = themeMode == PreferencesRepository.THEME_LIGHT,
                        onClick = { themeViewModel.setTheme(PreferencesRepository.THEME_LIGHT) },
                        icon = { Icon(Icons.Filled.LightMode, null, modifier = Modifier.size(16.dp)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        modifier = Modifier.weight(1f)
                    ) { Text("Светлая", fontSize = 10.sp, maxLines = 1) }

                    SegmentedButton(
                        selected = themeMode == PreferencesRepository.THEME_DARK,
                        onClick = { themeViewModel.setTheme(PreferencesRepository.THEME_DARK) },
                        icon = { Icon(Icons.Filled.DarkMode, null, modifier = Modifier.size(16.dp)) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        modifier = Modifier.weight(1f)
                    ) { Text("Темная", fontSize = 10.sp, maxLines = 1) }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Динамические цвета", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = if (supportsDynamicColors) "Использовать цвета обоев системы"
                            else "Доступно на Android 12 и выше",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                alpha = if (supportsDynamicColors) 1f else 0.7f
                            )
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
        // ListItem имеет свой фон и острые углы — заменяем на Row внутри Card
        Card(modifier = cardModifier) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = "О приложении",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // Версия
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text("Версия", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "1.0.0",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Разработчик
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Filled.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column {
                        Text("Разработчик", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "KIPiA Management",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}