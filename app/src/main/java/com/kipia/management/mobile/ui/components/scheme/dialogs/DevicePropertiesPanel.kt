package com.kipia.management.mobile.ui.components.scheme.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.viewmodel.EditorMode
import com.kipia.management.mobile.viewmodel.EditorState
import com.kipia.management.mobile.viewmodel.SchemeEditorViewModel

@Composable
fun DevicePropertiesPanel(
    editorState: EditorState,
    allDevices: List<Device>,
    devices: List<SchemeDevice>,
    viewModel: SchemeEditorViewModel,
    onEditDevice: (Int) -> Unit,
    onViewDetails: (Int) -> Unit,
    onViewPhotos: (Int) -> Unit,
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
        modifier = modifier.width(320.dp),
        onClose = { viewModel.toggleDeviceProperties() }
    ) {
        val (device, schemeDevice) = selectedDeviceInfo

        EditorDialogHeader(title = "Свойства прибора", onClose = { viewModel.toggleDeviceProperties() })

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Инфо-карточка
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text  = device.getDisplayName(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text  = "Инв. №: ${device.inventoryNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Ряд кнопок действий
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Изменить
                    Button(
                        onClick = { onEditDevice(device.id) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Изменить", style = MaterialTheme.typography.labelMedium)
                    }

                    // Фото
                    Button(
                        onClick = { onViewPhotos(device.id) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Фото", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Кнопка данных (широкая)
                OutlinedButton(
                    onClick = { onViewDetails(device.id) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Icon(Icons.Default.Info, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Просмотр данных прибора", 
                        style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Управление поворотом
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), thickness = 0.5.dp)
            
            Text(
                text = "Поворот",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RotationButton("0°", schemeDevice.rotation == 0f, { viewModel.rotateDevice(device.id, 0f) }, Modifier.weight(1f))
                RotationButton("90°", schemeDevice.rotation == 90f, { viewModel.rotateDevice(device.id, 90f) }, Modifier.weight(1f))
                RotationButton("180°", schemeDevice.rotation == 180f, { viewModel.rotateDevice(device.id, 180f) }, Modifier.weight(1f))
                RotationButton("270°", schemeDevice.rotation == 270f, { viewModel.rotateDevice(device.id, 270f) }, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RotationButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    OutlinedButton(
        onClick  = onClick,
        modifier = modifier,
        colors   = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor   = contentColor
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
