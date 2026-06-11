package com.kipia.management.mobile.ui.components.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.ui.theme.Dimens

// ═══════════════════════════════════════════════════════════════════════════
// Стили
// ═══════════════════════════════════════════════════════════════════════════
val ButtonShape = RoundedCornerShape(Dimens.cardRadius)
val ButtonBorder = BorderStroke(1.dp, Color.LightGray)

// Цвета для иконок
val DarkPurple = Color(0xFF4A148C)
val Orange = Color(0xFFFFA500)
val DeepDarkBlue = Color(0xFF000040)

@Composable
fun InfoDialog(
    title: String,
    message: String? = null,
    content: (@Composable () -> Unit)? = null,
    icon: ImageVector? = Icons.Default.Warning,
    iconTint: Color = Orange,
    confirmText: String = "OK",
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let { { Icon(imageVector = it, contentDescription = null, tint = iconTint, modifier = Modifier.size(Dimens.iconSizeLarge)) } },
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (message != null) {
                    Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                content?.invoke()
            }
        },
        confirmButton = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                OutlinedButton(onClick = onDismiss, shape = ButtonShape, border = ButtonBorder) { Text(confirmText) }
            }
        }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "Подтвердить",
    dismissText: String = "Отмена",
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = icon?.let { { Icon(imageVector = it, contentDescription = null, tint = iconTint, modifier = Modifier.size(Dimens.iconSizeLarge)) } }
            ?: {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(Dimens.iconSizeLarge).background(DarkPurple, CircleShape)) {
                    Text("?", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OutlinedButton(onClick = onDismiss, shape = ButtonShape, border = ButtonBorder) { Text(dismissText) }
                Spacer(modifier = Modifier.width(Dimens.spacingXLarge))
                Button(onClick = onConfirm, shape = ButtonShape) { Text(confirmText) }
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    title: String,
    itemName: String,
    message: String? = null,
    warning: String? = "Это действие нельзя отменить.",
    confirmText: String = "Удалить",
    dismissText: String = "Отмена",
    iconTint: Color = MaterialTheme.colorScheme.error,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = iconTint, modifier = Modifier.size(Dimens.iconSizeLarge)) },
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
                Text(text = itemName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                if (message != null) Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                if (warning != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(Dimens.iconSizeXSmall))
                        Spacer(modifier = Modifier.width(Dimens.spacingSmall))
                        Text(text = warning, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OutlinedButton(onClick = onDismiss, shape = ButtonShape, border = ButtonBorder) { Text(dismissText) }
                Spacer(modifier = Modifier.width(Dimens.spacingXLarge))
                Button(onClick = onConfirm, shape = ButtonShape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) { Text(confirmText) }
            }
        }
    )
}

@Composable
fun DeviceDeleteConfirmDialog(
    deviceName: String,
    photoCount: Int,
    message: String? = null,
    onConfirm: (deletePhotos: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var deletePhotos by remember { mutableStateOf(photoCount > 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(Dimens.iconSizeLarge)) },
        title = { Text(text = "Удаление прибора", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
                Text(text = deviceName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                if (message != null) Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                if (photoCount > 0) {
                    Spacer(modifier = Modifier.height(Dimens.spacingXSmall))
                    Surface(
                        onClick = { deletePhotos = !deletePhotos },
                        shape = RoundedCornerShape(Dimens.chipRadius),
                        color = if (deletePhotos) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (deletePhotos) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(Dimens.cardPadding)) {
                            Checkbox(checked = deletePhotos, onCheckedChange = { deletePhotos = it }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error))
                            Column {
                                Text(text = "Удалить фотографии ($photoCount шт.)", style = MaterialTheme.typography.bodyMedium, color = if (deletePhotos) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                Text(text = "Файлы будут физически удалены с устройства", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(top = Dimens.spacingXSmall)) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(Dimens.iconSizeXXSmall))
                    Spacer(modifier = Modifier.width(Dimens.spacingSmall))
                    Text(text = "Это действие нельзя отменить", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                OutlinedButton(onClick = onDismiss, shape = ButtonShape, border = ButtonBorder) { Text("Отмена") }
                Spacer(modifier = Modifier.width(Dimens.spacingLarge))
                Button(onClick = { onConfirm(deletePhotos) }, shape = ButtonShape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Удалить") }
            }
        }
    )
}

@Composable
fun DeviceDeleteWithSchemeDialog(
    deviceName: String,
    schemeName: String,
    photoCount: Int,
    onConfirm: (deletePhotos: Boolean, deleteScheme: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var deletePhotos by remember { mutableStateOf(photoCount > 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(Dimens.iconSizeLarge)) },
        title = { Text(text = "Удаление прибора", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
                Text(text = deviceName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(text = "Это последнее устройство в локации со схемой:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Text(text = schemeName, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                
                if (photoCount > 0) {
                    Spacer(modifier = Modifier.height(Dimens.spacingXSmall))
                    Surface(
                        onClick = { deletePhotos = !deletePhotos },
                        shape = RoundedCornerShape(Dimens.chipRadius),
                        color = if (deletePhotos) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else Color.Transparent,
                        border = BorderStroke(1.dp, if (deletePhotos) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(Dimens.cardPadding)) {
                            Checkbox(checked = deletePhotos, onCheckedChange = { deletePhotos = it }, colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.error))
                            Column {
                                Text(text = "Удалить фотографии ($photoCount шт.)", style = MaterialTheme.typography.bodyMedium, color = if (deletePhotos) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                                Text(text = "Файлы будут физически удалены с устройства", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingMedium))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(Dimens.iconSizeXSmall))
                    Spacer(modifier = Modifier.width(Dimens.spacingSmall))
                    Text(text = "Что делать со схемой?", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.spacingXSmall)) {
                Button(onClick = { onConfirm(deletePhotos, true) }, shape = ButtonShape, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)) { Text("Удалить со схемой") }
                OutlinedButton(onClick = { onConfirm(deletePhotos, false) }, shape = ButtonShape, border = ButtonBorder) { Text("Только устройство") }
                OutlinedButton(onClick = onDismiss, shape = ButtonShape, border = ButtonBorder) { Text("Отмена") }
            }
        }
    )
}

@Composable
fun DeviceDeleteDialog(
    device: Device,
    scheme: Scheme?,
    deviceCountInLocation: Int,
    isLastInLocation: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (deletePhotos: Boolean, deleteScheme: Boolean) -> Unit
) {
    val photoCount = device.photos.size
    if (isLastInLocation && scheme != null) {
        DeviceDeleteWithSchemeDialog(
            deviceName = "${device.getDisplayName()} (${device.inventoryNumber})",
            schemeName = "'${scheme.name}'",
            photoCount = photoCount,
            onConfirm = onConfirm,
            onDismiss = onDismiss
        )
    } else {
        DeviceDeleteConfirmDialog(
            deviceName = "${device.getDisplayName()} (${device.inventoryNumber})",
            photoCount = photoCount,
            message = if (!isLastInLocation) "В локации '${device.location}' останется ещё ${deviceCountInLocation - 1} приборов." else "Это последнее устройство в локации '${device.location}'.",
            onConfirm = { deletePhotos -> onConfirm(deletePhotos, false) },
            onDismiss = onDismiss
        )
    }
}

@Composable
fun UnsavedChangesDialog(
    onDismiss: () -> Unit,
    onConfirmExit: () -> Unit,
    onSaveAndExit: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Warning, null, tint = Orange, modifier = Modifier.size(Dimens.iconSizeLarge)) },
        title = { Text(text = "Несохраненные изменения", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { Text("Вы внесли изменения. Сохранить их перед выходом?", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.spacingXSmall)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    OutlinedButton(onClick = onDismiss, shape = ButtonShape, border = ButtonBorder) { Text("Отмена") }
                    Spacer(modifier = Modifier.width(Dimens.spacingMedium))
                    Button(onClick = onSaveAndExit, shape = ButtonShape) { Text("Сохранить") }
                }
                OutlinedButton(onClick = onConfirmExit, shape = ButtonShape, border = ButtonBorder) {
                    Text("Выйти без сохранения", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}

@Composable
fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onTakePhoto: () -> Unit,
    onChooseFromGallery: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.PhotoCamera, null, tint = DeepDarkBlue, modifier = Modifier.size(Dimens.iconSizeLarge)) },
        title = { Text(text = "Добавить фото", style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { Text("Выберите источник изображения:", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
            ) {
                Button(
                    onClick = onTakePhoto,
                    shape = ButtonShape,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) { Text("Камера") }
                
                Button(
                    onClick = onChooseFromGallery,
                    shape = ButtonShape,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) { Text("Галерея") }
                
                OutlinedButton(
                    onClick = onDismiss,
                    shape = ButtonShape,
                    border = ButtonBorder,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) { Text("Отмена") }
            }
        }
    )
}

@Composable
fun ErrorDialog(title: String, message: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(Dimens.iconSizeLarge).background(Color.Red, CircleShape)) {
                Text("×", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        },
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { OutlinedButton(onClick = onDismiss, shape = ButtonShape, border = ButtonBorder) { Text("OK") } } }
    )
}

/**
 * Универсальный индикатор загрузки/сохранения, перекрывающий экран
 */
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    text: String = "Загрузка...",
    backgroundColor: Color = Color.Black.copy(alpha = 0.45f)
) {
    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .pointerInput(Unit) {},
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingLarge)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun SavingOverlay(isSaving: Boolean, text: String = "Сохранение...") {
    LoadingOverlay(isLoading = isSaving, text = text)
}
