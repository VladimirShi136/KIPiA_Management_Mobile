package com.kipia.management.mobile.ui.components.scheme.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.dialogs.DraggableCard
import com.kipia.management.mobile.viewmodel.EditorMode
import com.kipia.management.mobile.viewmodel.EditorState
import com.kipia.management.mobile.viewmodel.SchemeEditorViewModel

/**
 * Плавающая карточка свойств выбранного прибора.
 *
 * Показывает:
 *  - название, тип, инвентарный номер
 *  - текущие координаты (X, Y)
 *  - кнопки поворота: 0° / 90° / 180° / 270°  ← аналог контекстного меню из JavaFX
 *
 * Поворот сохраняется через [SchemeEditorViewModel.rotateDevice] и
 * записывается в [SchemeDevice.rotation] (Float, градусы).
 *
 * ⚠️ Если `SchemeDevice.rotation` ещё не существует — добавь поле:
 *
 *   @Entity(tableName = "scheme_devices")
 *   data class SchemeDevice(
 *       ...
 *       val rotation: Float = 0f   // ← добавить + миграция Room
 *   )
 *
 * ⚠️ В ViewModel нужен метод:
 *
 *   fun rotateDevice(deviceId: Int, angleDeg: Float) {
 *       // обновляет SchemeDevice.rotation и вызывает markDirty()
 *   }
 */
@Composable
fun DevicePropertiesPanel(
    editorState: EditorState,
    allDevices: List<Device>,
    devices: List<SchemeDevice>,
    viewModel: SchemeEditorViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDeviceInfo = editorState.selection.selectedDeviceId?.let { id ->
        val device       = allDevices.find { it.id == id }
        val schemeDevice = devices.find { it.deviceId == id }
        if (device != null && schemeDevice != null) device to schemeDevice else null
    }

    if (selectedDeviceInfo == null ||
        !editorState.uiState.showDeviceProperties ||
        editorState.uiState.mode == EditorMode.PAN_ZOOM
    ) return

    DraggableCard(
        modifier = modifier.width(280.dp),
        showDragHandle = true
    ) {
        val (device, schemeDevice) = selectedDeviceInfo

        // ── Заголовок ─────────────────────────────────────────────────────
        Text(
            text  = "Свойства прибора",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // ── Информация об устройстве ──────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text  = device.name ?: device.type,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text  = "Тип: ${device.type}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                Text(
                    text  = "Инв. №${device.inventoryNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Позиция ───────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(text = "Позиция:", style = MaterialTheme.typography.bodyMedium)
                Text(
                    text  = "(${schemeDevice.x.toInt()}, ${schemeDevice.y.toInt()})",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Поворот ───────────────────────────────────────────────────────
        // Точная копия логики createRotateMenu() из JavaFX DeviceIconService:
        //   0° стандартно / 90° вправо / 180° перевернуть / 270° влево
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector        = Icons.Default.RotateRight,
                        contentDescription = null,
                        modifier           = Modifier.size(16.dp),
                        tint               = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text  = "Поворот",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Текущий угол
                Text(
                    text  = "Текущий: ${schemeDevice.rotation.toInt()}°",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                // Кнопки поворота — 2 × 2
                Row(
                    modifier            = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RotationButton(
                        label       = "0°",
                        description = "Стандартно",
                        selected    = schemeDevice.rotation == 0f,
                        onClick     = { viewModel.rotateDevice(device.id, 0f) },
                        modifier    = Modifier.weight(1f)
                    )
                    RotationButton(
                        label       = "90°",
                        description = "Вправо",
                        selected    = schemeDevice.rotation == 90f,
                        onClick     = { viewModel.rotateDevice(device.id, 90f) },
                        modifier    = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RotationButton(
                        label       = "180°",
                        description = "Перевернуть",
                        selected    = schemeDevice.rotation == 180f,
                        onClick     = { viewModel.rotateDevice(device.id, 180f) },
                        modifier    = Modifier.weight(1f)
                    )
                    RotationButton(
                        label       = "270°",
                        description = "Влево",
                        selected    = schemeDevice.rotation == 270f,
                        onClick     = { viewModel.rotateDevice(device.id, 270f) },
                        modifier    = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── Закрыть ───────────────────────────────────────────────────────
        TextButton(
            onClick  = { viewModel.toggleDeviceProperties() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Закрыть")
        }
    }
}

/**
 * Кнопка выбора угла поворота.
 * Подсвечивается, если [selected] == true (соответствует текущему углу).
 */
@Composable
private fun RotationButton(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.surface

    val contentColor = if (selected)
        MaterialTheme.colorScheme.onPrimary
    else
        MaterialTheme.colorScheme.onSurface

    OutlinedButton(
        onClick  = onClick,
        modifier = modifier,
        colors   = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor   = contentColor
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text  = description,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
