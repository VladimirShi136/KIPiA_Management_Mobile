package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.ui.components.scheme.shapes.*



@Composable
fun ShapePropertiesPanel(
    shape: ComposeShape?,
    onUpdateShape: (ComposeShape) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (shape == null) return

    // Используем remember с ключом, но без делегата
    val fillColorState = remember(shape.id) { mutableStateOf(shape.fillColor) }
    val strokeColorState = remember(shape.id) { mutableStateOf(shape.strokeColor) }
    val strokeWidthState = remember(shape.id) { mutableStateOf(shape.strokeWidth) }

    Card(
        modifier = modifier.width(300.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    text = "Свойства фигуры",
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            HorizontalDivider()

            // Тип фигуры
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (shape) {
                        is ComposeRectangle -> Icons.Default.CropSquare
                        is ComposeLine -> Icons.Default.HorizontalRule
                        is ComposeEllipse -> Icons.Default.Circle
                        is ComposeText -> Icons.Default.TextFields
                        is ComposeRhombus -> Icons.Default.Diamond
                        else -> Icons.Default.ShapeLine
                    },
                    contentDescription = null
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = when (shape) {
                        is ComposeRectangle -> "Прямоугольник"
                        is ComposeLine -> "Линия"
                        is ComposeEllipse -> "Эллипс"
                        is ComposeText -> "Текст"
                        is ComposeRhombus -> "Ромб"
                        else -> "Фигура"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
            }

            HorizontalDivider()

            // Цвет заливки
            Text(
                text = "Заливка",
                style = MaterialTheme.typography.labelLarge
            )

            ColorPicker(
                selectedColor = fillColorState.value,
                onColorSelected = { color ->
                    fillColorState.value = color
                    shape.fillColor = color
                    onUpdateShape(shape)
                },
                showTransparent = true
            )

            HorizontalDivider()

            // Цвет обводки
            Text(
                text = "Обводка",
                style = MaterialTheme.typography.labelLarge
            )

            ColorPicker(
                selectedColor = strokeColorState.value,
                onColorSelected = { color ->
                    strokeColorState.value = color
                    shape.strokeColor = color
                    onUpdateShape(shape)
                }
            )

            // Толщина обводки
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Толщина:")
                Slider(
                    value = strokeWidthState.value,
                    onValueChange = { width ->
                        strokeWidthState.value = width
                        shape.strokeWidth = width
                        onUpdateShape(shape)
                    },
                    valueRange = 1f..20f,
                    steps = 19,
                    modifier = Modifier.weight(1f)
                )
                Text("${strokeWidthState.value.toInt()}px")
            }

            // Специфичные свойства для каждой фигуры
            when (shape) {
                is ComposeRectangle -> {
                    HorizontalDivider()
                    Text(
                        text = "Закругление углов",
                        style = MaterialTheme.typography.labelLarge
                    )

                    val cornerRadiusState = remember { mutableStateOf(shape.cornerRadius) }

                    Slider(
                        value = cornerRadiusState.value,
                        onValueChange = { radius ->
                            cornerRadiusState.value = radius
                            shape.cornerRadius = radius
                            onUpdateShape(shape)
                        },
                        valueRange = 0f..50f
                    )
                }

                is ComposeText -> {
                    HorizontalDivider()
                    Text(
                        text = "Текст",
                        style = MaterialTheme.typography.labelLarge
                    )

                    val textState = remember { mutableStateOf(shape.text) }
                    val fontSizeState = remember { mutableStateOf(shape.fontSize) }

                    OutlinedTextField(
                        value = textState.value,
                        onValueChange = { newText ->
                            textState.value = newText
                            shape.text = newText
                            onUpdateShape(shape)
                        },
                        label = { Text("Текст") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Размер шрифта:")
                        Slider(
                            value = fontSizeState.value,
                            onValueChange = { size ->
                                fontSizeState.value = size
                                shape.fontSize = size
                                onUpdateShape(shape)
                            },
                            valueRange = 8f..72f,
                            steps = 16,
                            modifier = Modifier.weight(1f)
                        )
                        Text("${fontSizeState.value.toInt()}px")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    showTransparent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = mutableListOf(
        Color.Black,
        Color.DarkGray,
        Color.Gray,
        Color.LightGray,
        Color.White,
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Yellow,
        Color.Cyan,
        Color.Magenta,
        Color(0xFF4CAF50), // Material Green
        Color(0xFF2196F3), // Material Blue
        Color(0xFF9C27B0), // Material Purple
        Color(0xFFFF9800), // Material Orange
        Color(0xFF607D8B), // Material Blue Grey
    )

    if (showTransparent) {
        colors.add(0, Color.Transparent)
    }

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                    )
                    .clickable { onColorSelected(color) },
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

                if (color == Color.Transparent) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.LightGray, Color.DarkGray)
                                )
                            )
                    )
                }
            }
        }

        // Кнопка выбора произвольного цвета
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                )
                .clickable {
                    // TODO: Открыть диалог выбора цвета
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Palette,
                contentDescription = "Выбрать цвет",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}