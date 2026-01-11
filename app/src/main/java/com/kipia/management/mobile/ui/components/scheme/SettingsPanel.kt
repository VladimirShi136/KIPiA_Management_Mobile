package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.data.entities.SchemeData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    schemeData: SchemeData,
    onUpdateSchemeData: (SchemeData) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showGridSettings by remember { mutableStateOf(false) }
    var showCanvasSettings by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Настройки схемы",
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            HorizontalDivider()

            // Фон
            Text(
                text = "Фон",
                style = MaterialTheme.typography.labelLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Палитра цветов
                ColorPalette(
                    selectedColor = try { Color(android.graphics.Color.parseColor(schemeData.backgroundColor)) }
                    catch (_: Exception) { Color.White },
                    onColorSelected = { color ->
                        val hexColor = String.format("#%08X", color.toArgb())
                        onUpdateSchemeData(schemeData.copy(backgroundColor = hexColor))
                    }
                )

                IconButton(
                    onClick = { showColorPicker = true }
                ) {
                    Icon(Icons.Default.Palette, contentDescription = "Выбрать цвет")
                }
            }

            // Загрузка изображения
            Button(
                onClick = { /* TODO: Выбор изображения */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Загрузить фон")
            }

            HorizontalDivider()

            // Сетка
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Сетка",
                    style = MaterialTheme.typography.labelLarge
                )

                Switch(
                    checked = schemeData.gridEnabled,
                    onCheckedChange = {
                        onUpdateSchemeData(schemeData.copy(gridEnabled = it))
                    }
                )
            }

            if (schemeData.gridEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Размер:")
                    Slider(
                        value = schemeData.gridSize.toFloat(),
                        onValueChange = {
                            onUpdateSchemeData(schemeData.copy(gridSize = it.toInt()))
                        },
                        valueRange = 10f..200f,
                        steps = 19,
                        modifier = Modifier.weight(1f)
                    )
                    Text("${schemeData.gridSize}px", style = MaterialTheme.typography.bodySmall)
                }
            }

            HorizontalDivider()

            // Размер холста
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Размер холста",
                    style = MaterialTheme.typography.labelLarge
                )

                IconButton(onClick = { showCanvasSettings = true }) {
                    Icon(Icons.Default.Settings, contentDescription = "Настройки холста")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Ширина: ${schemeData.width}px")
                Text("Высота: ${schemeData.height}px")
            }

            if (showCanvasSettings) {
                CanvasSizeDialog(
                    currentWidth = schemeData.width,
                    currentHeight = schemeData.height,
                    onSizeChanged = { width, height ->
                        onUpdateSchemeData(schemeData.copy(width = width, height = height))
                    },
                    onDismiss = { showCanvasSettings = false }
                )
            }
        }
    }
}

@Composable
fun ColorPalette(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color.White,
        Color.LightGray,
        Color.DarkGray,
        Color.Black,
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Yellow,
        Color.Cyan,
        Color.Magenta
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        colors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = color,
                        shape = MaterialTheme.shapes.small
                    )
                    .border(
                        width = 2.dp,
                        color = if (color == selectedColor) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (color == selectedColor) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (color.luminance() > 0.5f) Color.Black else Color.White
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasSizeDialog(
    currentWidth: Int,
    currentHeight: Int,
    onSizeChanged: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var width by remember { mutableStateOf(currentWidth.toString()) }
    var height by remember { mutableStateOf(currentHeight.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Размер холста") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = width,
                    onValueChange = { width = it },
                    label = { Text("Ширина (px)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = height,
                    onValueChange = { height = it },
                    label = { Text("Высота (px)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Стандартные размеры:",
                    style = MaterialTheme.typography.labelMedium
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "A4" to Pair(2480, 3508),
                        "A3" to Pair(3508, 4960),
                        "Full HD" to Pair(1920, 1080),
                        "2K" to Pair(2560, 1440),
                        "4K" to Pair(3840, 2160)
                    ).forEach { (name, size) ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                width = size.first.toString()
                                height = size.second.toString()
                            },
                            label = { Text(name) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = width.toIntOrNull() ?: currentWidth
                    val h = height.toIntOrNull() ?: currentHeight
                    onSizeChanged(w, h)
                    onDismiss()
                }
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}