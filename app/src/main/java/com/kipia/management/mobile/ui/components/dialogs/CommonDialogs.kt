package com.kipia.management.mobile.ui.components.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ═══════════════════════════════════════════════════════════════════════════
// Базовый диалог удаления — единый стиль для всего приложения
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Универсальный диалог подтверждения удаления.
 * Используется везде где нужно подтвердить деструктивное действие.
 *
 * @param title       Заголовок диалога
 * @param itemName    Имя удаляемого объекта — выделяется красным жирным
 * @param message     Дополнительное сообщение под именем (опционально)
 * @param warning     Предупреждение внизу — "нельзя отменить" и т.п. (опционально)
 * @param confirmText Текст кнопки подтверждения
 * @param dismissText Текст кнопки отмены
 * @param icon        Иконка диалога (по умолчанию Delete)
 * @param onConfirm   Колбэк подтверждения
 * @param onDismiss   Колбэк отмены
 */
@Composable
fun DeleteConfirmDialog(
    title: String,
    itemName: String,
    message: String? = null,
    warning: String? = "Это действие нельзя отменить.",
    confirmText: String = "Удалить",
    dismissText: String = "Отмена",
    icon: ImageVector = Icons.Default.Delete,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Имя удаляемого объекта — всегда красным жирным
                Text(
                    text = itemName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.error
                )

                // Дополнительное сообщение
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Предупреждение
                if (warning != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = warning,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// Расширенный диалог удаления устройства — с доп. кнопкой "только устройство"
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Специальный диалог для удаления устройства когда оно последнее в локации.
 * Предлагает три варианта: удалить с схемой / только устройство / отмена.
 */
@Composable
fun DeviceDeleteWithSchemeDialog(
    deviceName: String,
    schemeName: String,
    onDeleteWithScheme: () -> Unit,
    onDeleteOnly: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text("Удаление устройства", style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = deviceName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Это последнее устройство в локации со схемой:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = schemeName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Что делать со схемой этой локации?",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDeleteWithScheme,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Text("Удалить со схемой") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDeleteOnly) { Text("Только устройство") }
                OutlinedButton(onClick = onDismiss) { Text("Отмена") }
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// Информационный диалог ошибки
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
        },
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            OutlinedButton(onClick = onDismiss) { Text("OK") }
        }
    )
}