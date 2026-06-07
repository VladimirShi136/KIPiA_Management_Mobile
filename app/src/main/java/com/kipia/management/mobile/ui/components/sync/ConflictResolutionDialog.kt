package com.kipia.management.mobile.ui.components.sync

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.DeviceLocation
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.managers.SyncManager
import com.kipia.management.mobile.ui.theme.Dimens
import java.text.SimpleDateFormat
import java.util.*

/**
 * Полноэкранный диалог разрешения конфликтов при синхронизации.
 * Приведен к общему стилю приложения (шрифты, отступы, цветовые акценты).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConflictResolutionDialog(
    conflicts: List<SyncManager.ConflictInfo>,
    onDismiss: () -> Unit,
    onResolve: (List<SyncManager.ConflictResolution>) -> Unit
) {
    // По умолчанию выбираем REMOTE (импортируемые данные)
    val resolutions = remember {
        mutableStateListOf<SyncManager.ConflictResolution>().apply {
            addAll(List(conflicts.size) { SyncManager.ConflictResolution.REMOTE })
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Заголовок в стиле системных уведомлений об ошибках/предупреждениях
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Разрешение конфликтов",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Найдено ${conflicts.size} расхождений",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрыть")
                        }
                    },
                    actions = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = Dimens.spacingMedium)
                        )
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(Dimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
                ) {
                    itemsIndexed(conflicts) { index, conflict ->
                        ConflictItem(
                            conflict = conflict,
                            currentResolution = resolutions[index],
                            onResolutionChange = { resolutions[index] = it },
                            dateFormat = dateFormat
                        )
                    }
                }

                // Нижняя панель с кнопками
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(Dimens.screenPadding),
                        horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Dimens.chipRadius)
                        ) {
                            Text("Отмена")
                        }

                        Button(
                            onClick = { onResolve(resolutions.toList()) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(Dimens.chipRadius)
                        ) {
                            Text("Применить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConflictItem(
    conflict: SyncManager.ConflictInfo,
    currentResolution: SyncManager.ConflictResolution,
    onResolutionChange: (SyncManager.ConflictResolution) -> Unit,
    dateFormat: SimpleDateFormat
) {
    val fieldDiffs = remember(conflict) { getFieldDiffs(conflict) }
    val localTime = getTimestamp(conflict.local)
    val remoteTime = getTimestamp(conflict.remote)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.cardRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (currentResolution == SyncManager.ConflictResolution.SKIP)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (currentResolution == SyncManager.ConflictResolution.SKIP)
            BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        else null
    ) {
        Column(modifier = Modifier.padding(Dimens.cardPadding)) {
            // Заголовок объекта
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = getTypeLabel(conflict.type).uppercase(),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(modifier = Modifier.width(Dimens.spacingSmall))
                Text(
                    text = conflict.key,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                IconButton(
                    onClick = { onResolutionChange(SyncManager.ConflictResolution.SKIP) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (currentResolution == SyncManager.ConflictResolution.SKIP) Icons.Default.Close else Icons.Default.Warning,
                        contentDescription = "Пропустить",
                        tint = if (currentResolution == SyncManager.ConflictResolution.SKIP)
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = Dimens.spacingSmall),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)
            ) {
                // ЛОКАЛЬНАЯ ВЕРСИЯ
                ResolutionVersionColumn(
                    title = "ЛОКАЛЬНО",
                    subtitle = "На этом устройстве",
                    timestamp = localTime,
                    isNewer = localTime > remoteTime,
                    isSelected = currentResolution == SyncManager.ConflictResolution.LOCAL,
                    fieldDiffs = fieldDiffs,
                    useLocal = true,
                    dateFormat = dateFormat,
                    onClick = { onResolutionChange(SyncManager.ConflictResolution.LOCAL) },
                    modifier = Modifier.weight(1f)
                )

                // УДАЛЕННАЯ ВЕРСИЯ
                ResolutionVersionColumn(
                    title = "В АРХИВЕ",
                    subtitle = "Импорт. данные",
                    timestamp = remoteTime,
                    isNewer = remoteTime > localTime,
                    isSelected = currentResolution == SyncManager.ConflictResolution.REMOTE,
                    fieldDiffs = fieldDiffs,
                    useLocal = false,
                    dateFormat = dateFormat,
                    onClick = { onResolutionChange(SyncManager.ConflictResolution.REMOTE) },
                    modifier = Modifier.weight(1f)
                )
            }

            if (currentResolution == SyncManager.ConflictResolution.SKIP) {
                Text(
                    text = "Конфликт не разрешен. Объект будет пропущен.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .padding(top = Dimens.spacingSmall)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ResolutionVersionColumn(
    title: String,
    subtitle: String,
    timestamp: Long,
    isNewer: Boolean,
    isSelected: Boolean,
    fieldDiffs: List<FieldDiff>,
    useLocal: Boolean,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)

    Column(
        modifier = modifier
            .border(1.dp, borderColor, RoundedCornerShape(Dimens.cardRadius))
            .background(backgroundColor, RoundedCornerShape(Dimens.cardRadius))
            .padding(Dimens.spacingSmall)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isNewer) {
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    color = Color(0xFF4CAF50),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        "НОВЕЕ",
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = dateFormat.format(Date(timestamp)),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 4.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        HorizontalDivider(modifier = Modifier.padding(bottom = Dimens.spacingSmall), thickness = 0.5.dp)

        // Список различающихся полей
        fieldDiffs.forEach { diff ->
            val value = if (useLocal) diff.localValue else diff.remoteValue
            Column(modifier = Modifier.padding(bottom = 6.dp)) {
                Text(
                    text = diff.name,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (diff.isDifferent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Text(
                    text = value.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (diff.isDifferent) FontWeight.Bold else FontWeight.Normal,
                    color = if (diff.isDifferent) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isSelected) {
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(Dimens.chipRadius)
            ) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("Выбрано", style = MaterialTheme.typography.labelSmall)
            }
        } else {
            OutlinedButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentPadding = PaddingValues(0.dp),
                shape = RoundedCornerShape(Dimens.chipRadius)
            ) {
                Text("Выбрать", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

data class FieldDiff(
    val name: String,
    val localValue: String,
    val remoteValue: String,
    val isDifferent: Boolean
)

private fun getFieldDiffs(conflict: SyncManager.ConflictInfo): List<FieldDiff> {
    val diffs = mutableListOf<FieldDiff>()
    val local = conflict.local
    val remote = conflict.remote

    when {
        local is Device && remote is Device -> {
            diffs.add(FieldDiff("Статус", local.status, remote.status, local.status != remote.status))
            diffs.add(FieldDiff("Тип", local.type, remote.type, local.type != remote.type))
            diffs.add(FieldDiff("Имя", local.name ?: "", remote.name ?: "", local.name != remote.name))
            diffs.add(FieldDiff("Место", local.location, remote.location, local.location != remote.location))
            diffs.add(FieldDiff("Завод", local.manufacturer ?: "", remote.manufacturer ?: "", local.manufacturer != remote.manufacturer))
            diffs.add(FieldDiff("Год", local.year?.toString() ?: "", remote.year?.toString() ?: "", local.year != remote.year))
            diffs.add(FieldDiff("Предел", local.measurementLimit ?: "", remote.measurementLimit ?: "", local.measurementLimit != remote.measurementLimit))
            diffs.add(FieldDiff("Класс", local.accuracyClass?.toString() ?: "", remote.accuracyClass?.toString() ?: "", local.accuracyClass != remote.accuracyClass))
            diffs.add(FieldDiff("Кран", local.valveNumber ?: "", remote.valveNumber ?: "", local.valveNumber != remote.valveNumber))
            diffs.add(FieldDiff("Удален", if (local.isDeleted()) "Да" else "Нет", if (remote.isDeleted()) "Да" else "Нет", local.deletedAt != remote.deletedAt))
            diffs.add(FieldDiff("Фото", "${local.photos.size} шт", "${remote.photos.size} шт", local.photos != remote.photos))
        }
        local is Scheme && remote is Scheme -> {
            diffs.add(FieldDiff("Описание", local.description ?: "", remote.description ?: "", local.description != remote.description))
            diffs.add(FieldDiff("Данные (байт)", local.data.length.toString(), remote.data.length.toString(), local.data != remote.data))
            diffs.add(FieldDiff("Удален", if (local.isDeleted()) "Да" else "Нет", if (remote.isDeleted()) "Да" else "Нет", local.deletedAt != remote.deletedAt))
        }
        local is DeviceLocation && remote is DeviceLocation -> {
            diffs.add(FieldDiff("Координата X", "%.2f".format(local.x), "%.2f".format(remote.x), local.x != remote.x))
            diffs.add(FieldDiff("Координата Y", "%.2f".format(local.y), "%.2f".format(remote.y), local.y != remote.y))
            diffs.add(FieldDiff("Поворот", local.rotation.toString(), remote.rotation.toString(), local.rotation != remote.rotation))
            diffs.add(FieldDiff("Удален", if (local.isDeleted()) "Да" else "Нет", if (remote.isDeleted()) "Да" else "Нет", local.deletedAt != remote.deletedAt))
        }
    }
    return diffs
}

private fun getTypeLabel(type: String) = when(type) {
    "device" -> "Устройство"
    "scheme" -> "Схема"
    "location" -> "Размещение"
    else -> "Объект"
}

private fun getTimestamp(obj: Any): Long = when(obj) {
    is Device -> obj.updatedAt
    is Scheme -> obj.updatedAt
    is DeviceLocation -> obj.updatedAt
    else -> 0L
}
