package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice

@Composable
fun DeviceOnCanvas(
    device: Device,
    schemeDevice: SchemeDevice,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDrag: (Offset) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(80.dp)
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDrag = { change, dragAmount ->
                        onDrag(dragAmount)
                    },
                    onDragEnd = { isDragging = false }
                )
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .rotate(schemeDevice.rotation),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            ),
            border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
            elevation = if (isDragging) CardDefaults.cardElevation(defaultElevation = 8.dp)
            else CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Иконка устройства
                Icon(
                    imageVector = when (device.type) {
                        "Манометр", "Термометр", "Датчик давления" -> Icons.Default.Sensors
                        "Счетчик" -> Icons.Default.Speed
                        "Клапан", "Задвижка" -> Icons.Default.Tune
                        "Датчик" -> Icons.Default.Sensors
                        "Преобразователь" -> Icons.Default.ElectricBolt
                        "Регулятор" -> Icons.Default.Thermostat
                        else -> Icons.Default.Devices
                    },
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Инвентарный номер (сокращенный)
                Text(
                    text = device.inventoryNumber.takeLast(4),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        // Маркер для перетаскивания (только если выбрано)
        if (isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 8.dp, y = (-8).dp)
                    .size(16.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
            )
        }
    }
}