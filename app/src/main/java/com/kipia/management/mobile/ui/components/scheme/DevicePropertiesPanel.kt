package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.theme.DeviceStatus

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
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            // Информация о приборе
            DeviceInfoSection(device)

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))  // Используем HorizontalDivider вместо Divider

            // Положение
            Text(
                text = "Положение",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant // ДОБАВЛЕНО

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
                    label = {
                        Text(
                            "X",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )

                OutlinedTextField(
                    value = positionY,
                    onValueChange = {
                        positionY = it
                        it.toFloatOrNull()?.let { y ->
                            onUpdatePosition(schemeDevice.x, y)
                        }
                    },
                    label = {
                        Text(
                            "Y",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))  // Используем HorizontalDivider вместо Divider

            // Вращение
            Text(
                text = "Вращение",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Slider(
                value = rotation,
                onValueChange = {
                    rotation = it
                    onUpdateRotation(it)
                },
                valueRange = 0f..360f,
                steps = 359,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "0°",
                    color = MaterialTheme.colorScheme.onSurfaceVariant // ДОБАВЛЕНО
                )
                Text(
                    "${rotation.toInt()}°",
                    color = MaterialTheme.colorScheme.primary // ДОБАВЛЕНО
                )
                Text(
                    "360°",
                    color = MaterialTheme.colorScheme.onSurfaceVariant // ДОБАВЛЕНО
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))  // Используем HorizontalDivider вместо Divider

            // Масштаб
            Text(
                text = "Масштаб",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Slider(
                value = scale,
                onValueChange = {
                    scale = it
                    onUpdateScale(it)
                },
                valueRange = 0.5f..2f,
                steps = 15,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.secondary,
                    activeTrackColor = MaterialTheme.colorScheme.secondary,
                    inactiveTrackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "50%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant // ДОБАВЛЕНО
                )
                Text(
                    "${(scale * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.secondary // ДОБАВЛЕНО
                )
                Text(
                    "200%",
                    color = MaterialTheme.colorScheme.onSurfaceVariant // ДОБАВЛЕНО
                )
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
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Сброс")
                }

                Button(
                    onClick = onRemoveDevice,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError // ДОБАВЛЕНО
                    )
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
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
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
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Статус - используем DeviceStatus
            val deviceStatus = DeviceStatus.fromString(device.status)

            // ★★★★ ИСПРАВЛЕННЫЙ ВАРИАНТ AssistChip ★★★★
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        device.status.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = deviceStatus.textColor
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = deviceStatus.containerColor, // containerColor вместо backgroundColor
                    labelColor = deviceStatus.textColor
                ),
            )

            // Тип прибора - используем InputChip или FilterChip вместо SuggestionChip
            FilterChip(
                selected = false,
                onClick = {},
                label = {
                    Text(
                        device.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant // ДОБАВЛЕНО
                    )
                }
            }

            device.accuracyClass?.let { accuracy ->
                Text(
                    text = "Класс: $accuracy",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant // ДОБАВЛЕНО
                )
            }
        }
    }
}