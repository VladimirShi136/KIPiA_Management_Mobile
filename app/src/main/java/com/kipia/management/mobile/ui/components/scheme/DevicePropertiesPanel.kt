package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicePropertiesPanel(
    device: Device?,
    schemeDevice: SchemeDevice?,
    onUpdatePosition: (Float, Float) -> Unit,
    onUpdateRotation: (Float) -> Unit,
    onUpdateScale: (Float) -> Unit,
    onRemoveDevice: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (device == null || schemeDevice == null) return

    var rotation by remember(schemeDevice.deviceId) { mutableStateOf(schemeDevice.rotation) }
    var scale by remember(schemeDevice.deviceId) { mutableStateOf(schemeDevice.scale) }
    var positionX by remember(schemeDevice.deviceId) { mutableStateOf(schemeDevice.x.toString()) }
    var positionY by remember(schemeDevice.deviceId) { mutableStateOf(schemeDevice.y.toString()) }

    Card(
        modifier = modifier.width(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Свойства прибора",
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            // Информация о приборе
            DeviceInfoSection(device)

            HorizontalDivider()  // Используем HorizontalDivider вместо Divider

            // Положение
            Text(
                text = "Положение",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = positionX,
                    onValueChange = {
                        positionX = it
                        it.toFloatOrNull()?.let { x ->
                            onUpdatePosition(x, schemeDevice.y)
                        }
                    },
                    label = { Text("X") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                OutlinedTextField(
                    value = positionY,
                    onValueChange = {
                        positionY = it
                        it.toFloatOrNull()?.let { y ->
                            onUpdatePosition(schemeDevice.x, y)
                        }
                    },
                    label = { Text("Y") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            HorizontalDivider()  // Используем HorizontalDivider вместо Divider

            // Вращение
            Text(
                text = "Вращение",
                style = MaterialTheme.typography.labelLarge
            )

            Slider(
                value = rotation,
                onValueChange = {
                    rotation = it
                    onUpdateRotation(it)
                },
                valueRange = 0f..360f,
                steps = 359
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0°")
                Text("${rotation.toInt()}°")
                Text("360°")
            }

            HorizontalDivider()  // Используем HorizontalDivider вместо Divider

            // Масштаб
            Text(
                text = "Масштаб",
                style = MaterialTheme.typography.labelLarge
            )

            Slider(
                value = scale,
                onValueChange = {
                    scale = it
                    onUpdateScale(it)
                },
                valueRange = 0.5f..2f,
                steps = 15
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("50%")
                Text("${(scale * 100).toInt()}%")
                Text("200%")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        // Сброс к значениям по умолчанию
                        rotation = 0f
                        scale = 1f
                        onUpdateRotation(0f)
                        onUpdateScale(1f)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Сброс")
                }

                Button(
                    onClick = onRemoveDevice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Удалить")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoSection(device: Device) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (device.type.lowercase()) {
                    "манометр", "pressure" -> Icons.Default.Speed
                    "термометр", "thermometer" -> Icons.Default.Thermostat
                    "счетчик", "counter" -> Icons.Default.Calculate
                    "клапан", "valve" -> Icons.Default.TapAndPlay
                    "датчик", "sensor" -> Icons.Default.Sensors
                    "регулятор", "controller" -> Icons.Default.Tune
                    else -> Icons.Default.Devices
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Исправляем Elvis operator - device.name может быть null
            val displayName = if (!device.name.isNullOrBlank()) {
                device.name
            } else {
                device.getDisplayName()
            }

            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Статус
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        device.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = when (device.status) {
                        "В работе" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                        "На ремонте" -> Color(0xFFFF9800).copy(alpha = 0.2f)
                        "Списан" -> Color(0xFFF44336).copy(alpha = 0.2f)
                        "В резерве" -> Color(0xFF9E9E9E).copy(alpha = 0.2f)
                        else -> Color.Gray.copy(alpha = 0.2f)
                    }
                )
            )

            // Тип прибора - используем InputChip или FilterChip вместо SuggestionChip
            FilterChip(
                selected = false,
                onClick = {},
                label = {
                    Text(
                        device.type,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        // Дополнительная информация
        if (!device.additionalInfo.isNullOrBlank()) {
            Text(
                text = device.additionalInfo,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Инвентарный номер и место
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Инвентарный: ${device.inventoryNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Место: ${device.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Номер крана, если есть
            device.valveNumber?.let { valveNumber ->
                if (valveNumber.isNotBlank()) {
                    Text(
                        text = "Кран: $valveNumber",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Технические характеристики
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            device.measurementLimit?.let { limit ->
                if (limit.isNotBlank()) {
                    Text(
                        text = "Предел: $limit",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            device.accuracyClass?.let { accuracy ->
                Text(
                    text = "Класс: $accuracy",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}