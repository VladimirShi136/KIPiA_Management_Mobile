package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.ui.components.scheme.shapes.copyWithFillColor
import com.kipia.management.mobile.ui.components.scheme.shapes.copyWithStrokeColor
import com.kipia.management.mobile.ui.components.scheme.shapes.copyWithStrokeWidth
import com.kipia.management.mobile.viewmodel.EditorMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomShapeToolbar(
    editorMode: EditorMode,
    selectedShape: ComposeShape?,
    onModeChanged: (EditorMode) -> Unit,
    onAddDevice: () -> Unit,
    onDeleteShape: (() -> Unit)? = null,
    onBringToFront: (() -> Unit)? = null,
    onSendToBack: (() -> Unit)? = null,
    onCopyShape: (() -> Unit)? = null,
    onDuplicateShape: (() -> Unit)? = null,
    onOpenShapeProperties: () -> Unit = {},
    onTogglePropertiesPanel: () -> Unit = {}, // НОВАЯ функция для toggle панели
    isPropertiesPanelVisible: Boolean = false, // Состояние панели
    onChangeFillColor: (Color) -> Unit = {},
    onChangeStrokeColor: (Color) -> Unit = {},
    onChangeStrokeWidth: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showShapeMenu by remember { mutableStateOf(false) }
    var showShapeOperationsMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    var showStrokeWidthDialog by remember { mutableStateOf(false) }
    var colorPickerType by remember { mutableStateOf("fill") }
    var strokeWidthValue by remember { mutableStateOf(2f) }

    val hasSelectedShape = selectedShape != null

    LaunchedEffect(selectedShape) {
        selectedShape?.let {
            strokeWidthValue = it.strokeWidth
        }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка SELECT с простым toggle
            ToolbarButton(
                icon = if (editorMode == EditorMode.SELECT)
                    Icons.Default.CheckBox
                else
                    Icons.Default.CheckBoxOutlineBlank,
                contentDescription = "Выбрать",
                isSelected = editorMode == EditorMode.SELECT,
                onClick = {
                    // Простой toggle: если SELECT - выключаем, если нет - включаем
                    if (editorMode == EditorMode.SELECT) {
                        onModeChanged(EditorMode.NONE)
                    } else {
                        onModeChanged(EditorMode.SELECT)
                    }
                },
                enabled = true
            )

            // Разделитель
            VerticalDivider()

            // Меню фигур
            Box {
                ToolbarButton(
                    icon = Icons.Default.ViewInAr,
                    contentDescription = "Фигуры",
                    isSelected = editorMode in listOf(
                        EditorMode.RECTANGLE,
                        EditorMode.LINE,
                        EditorMode.ELLIPSE,
                        EditorMode.RHOMBUS,
                        EditorMode.TEXT
                    ),
                    onClick = { showShapeMenu = true },
                    enabled = true
                )

                DropdownMenu(
                    expanded = showShapeMenu,
                    onDismissRequest = { showShapeMenu = false },
                    modifier = Modifier.width(200.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("Прямоугольник") },
                        onClick = {
                            onModeChanged(EditorMode.RECTANGLE)
                            showShapeMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.CropSquare, contentDescription = null)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Линия") },
                        onClick = {
                            onModeChanged(EditorMode.LINE)
                            showShapeMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.HorizontalRule, contentDescription = null)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Эллипс") },
                        onClick = {
                            onModeChanged(EditorMode.ELLIPSE)
                            showShapeMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Circle, contentDescription = null)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Ромб") },
                        onClick = {
                            onModeChanged(EditorMode.RHOMBUS)
                            showShapeMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Diamond, contentDescription = null)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Текст") },
                        onClick = {
                            onModeChanged(EditorMode.TEXT)
                            showShapeMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.TextFields, contentDescription = null)
                        }
                    )
                }
            }

            // Разделитель
            VerticalDivider()

            // Кнопка добавления прибора
            // Кнопка добавления прибора
            ToolbarButton(
                icon = Icons.Default.Add,
                contentDescription = "Добавить прибор",
                isSelected = editorMode == EditorMode.DEVICE,
                onClick = {
                    onModeChanged(EditorMode.DEVICE)
                    onAddDevice()
                },
                enabled = true
            )

            // Разделитель
            VerticalDivider()

            // КНОПКА СВОЙСТВ ФИГУРЫ (всегда видна, активна когда есть выделение)
            ToolbarButton(
                icon = Icons.Default.Settings,
                contentDescription = "Свойства фигуры",
                isSelected = isPropertiesPanelVisible && hasSelectedShape,
                onClick = {
                    if (hasSelectedShape) {
                        onTogglePropertiesPanel()
                    }
                },
                enabled = hasSelectedShape
            )

            // Разделитель
            if (hasSelectedShape) {
                VerticalDivider()
            }

            // МЕНЮ ОПЕРАЦИЙ С ФИГУРОЙ
            Box {
                ToolbarButton(
                    icon = Icons.Default.MoreVert,
                    contentDescription = "Операции с фигурой",
                    isSelected = false,
                    onClick = {
                        if (hasSelectedShape) {
                            showShapeOperationsMenu = true
                        }
                    },
                    enabled = hasSelectedShape
                )

                DropdownMenu(
                    expanded = showShapeOperationsMenu,
                    onDismissRequest = { showShapeOperationsMenu = false },
                    modifier = Modifier.width(240.dp)
                ) {
                    // Цвет заливки
                    DropdownMenuItem(
                        text = { Text("Цвет заливки") },
                        onClick = {
                            colorPickerType = "fill"
                            showColorPicker = true
                            showShapeOperationsMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.FormatColorFill, contentDescription = null)
                        }
                    )

                    // Цвет обводки
                    DropdownMenuItem(
                        text = { Text("Цвет обводки") },
                        onClick = {
                            colorPickerType = "stroke"
                            showColorPicker = true
                            showShapeOperationsMenu = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.BorderColor, contentDescription = null)
                        }
                    )

                    // Толщина обводки
                    DropdownMenuItem(
                        text = { Text("Толщина обводки") },
                        onClick = {
                            selectedShape?.let { shape ->
                                strokeWidthValue = shape.strokeWidth
                                showStrokeWidthDialog = true
                                showShapeOperationsMenu = false
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.LineWeight, contentDescription = null)
                        }
                    )

                    Divider()

                    // Копировать фигуру
                    onCopyShape?.let {
                        DropdownMenuItem(
                            text = { Text("Копировать") },
                            onClick = {
                                it()
                                showShapeOperationsMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                            }
                        )
                    }

                    // Дублировать фигуру
                    onDuplicateShape?.let {
                        DropdownMenuItem(
                            text = { Text("Дублировать") },
                            onClick = {
                                it()
                                showShapeOperationsMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.FileCopy, contentDescription = null)
                            }
                        )
                    }

                    Divider()

                    // На передний план
                    onBringToFront?.let {
                        DropdownMenuItem(
                            text = { Text("На передний план") },
                            onClick = {
                                it()
                                showShapeOperationsMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ArrowUpward, contentDescription = null)
                            }
                        )
                    }

                    // На задний план
                    onSendToBack?.let {
                        DropdownMenuItem(
                            text = { Text("На задний план") },
                            onClick = {
                                it()
                                showShapeOperationsMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ArrowDownward, contentDescription = null)
                            }
                        )
                    }

                    Divider()

                    // Удалить фигуру
                    onDeleteShape?.let {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Удалить",
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                it()
                                showShapeOperationsMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    // Диалог выбора цвета
    if (showColorPicker) {
        ColorPickerDialog(
            title = if (colorPickerType == "fill") "Цвет заливки" else "Цвет обводки",
            initialColor = when (colorPickerType) {
                "fill" -> selectedShape?.fillColor ?: Color.Transparent
                else -> selectedShape?.strokeColor ?: Color.Black
            },
            onColorSelected = { color ->
                if (colorPickerType == "fill") {
                    onChangeFillColor(color)
                } else {
                    onChangeStrokeColor(color)
                }
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showStrokeWidthDialog && selectedShape != null) {
        StrokeWidthDialog(
            initialWidth = selectedShape.strokeWidth,
            onWidthSelected = { width ->
                onChangeStrokeWidth(width)
                showStrokeWidthDialog = false
            },
            onDismiss = { showStrokeWidthDialog = false }
        )
    }
}

@Composable
fun VerticalDivider(
    modifier: Modifier = Modifier
) {
    Divider(
        modifier = modifier
            .height(32.dp)
            .width(1.dp),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    )
}

@Composable
private fun ToolbarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(40.dp)
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) {
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
    showTransparent: Boolean = true // Добавляем параметр
) {
    var selectedColor by remember { mutableStateOf(initialColor) }

    val colors = mutableListOf(
        Color.Black,
        Color.White,
        Color.Red,
        Color(0xFFFF9800), // Оранжевый
        Color.Yellow,
        Color.Green,
        Color(0xFF2196F3), // Синий
        Color(0xFF9C27B0), // Фиолетовый
        Color(0xFFE91E63), // Розовый
        Color(0xFF795548), // Коричневый
        Color(0xFF607D8B), // Серо-голубой
    )

    if (showTransparent) {
        colors.add(0, Color.Transparent)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Предпросмотр выбранного цвета
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(
                            color = selectedColor,
                            shape = CircleShape
                        )
                        .border(
                            width = 2.dp,
                            color = if (selectedColor == Color.Transparent)
                                MaterialTheme.colorScheme.outline
                            else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedColor == Color.Transparent) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Прозрачный",
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                // Сетка цветов - используем FlowRow из Compose Foundation
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    colors.forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = color,
                                    shape = CircleShape
                                )
                                .border(
                                    width = 3.dp,
                                    color = if (color == selectedColor)
                                        MaterialTheme.colorScheme.primary
                                    else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColor = color
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (color == selectedColor && color != Color.Transparent) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp),
                                    tint = if (color.luminance() > 0.5f) Color.Black else Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onColorSelected(selectedColor) }
            ) {
                Text("Выбрать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrokeWidthDialog(
    initialWidth: Float,
    onWidthSelected: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var width by remember { mutableStateOf(initialWidth) }

    val presetWidths = listOf(1f, 2f, 3f, 4f, 5f, 6f, 8f, 10f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Толщина обводки") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Предпросмотр
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .background(Color.Transparent),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                    ) {
                        drawLine(
                            color = Color.Black,
                            start = Offset(0f, size.height / 2),
                            end = Offset(size.width, size.height / 2),
                            strokeWidth = width,
                            cap = StrokeCap.Round
                        )
                    }
                }

                Text(
                    text = "${width.toInt()} px",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                // Слайдер
                Slider(
                    value = width,
                    onValueChange = { width = it },
                    valueRange = 1f..20f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )

                // Быстрый выбор
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    presetWidths.forEach { preset ->
                        FilterChip(
                            selected = width == preset,
                            onClick = { width = preset },
                            label = { Text("${preset.toInt()}") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onWidthSelected(width) }
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

@Composable
fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val horizontalSpacing = horizontalArrangement.spacing
            .takeIf { it is Dp }?.toPx()?.roundToInt() ?: 0
        val verticalSpacing = verticalArrangement.spacing
            .takeIf { it is Dp }?.toPx()?.roundToInt() ?: 0

        val rows = mutableListOf<MutableList<Placeable>>()
        var currentRow = mutableListOf<Placeable>()
        var currentRowWidth = 0
        val maxWidth = constraints.maxWidth

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)

            if (currentRowWidth + placeable.width + (if (currentRow.isEmpty()) 0 else horizontalSpacing) > maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }

            currentRow.add(placeable)
            currentRowWidth += placeable.width + (if (currentRow.size > 1) horizontalSpacing else 0)
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        val height = rows.sumOf { row ->
            row.maxOfOrNull { it.height } ?: 0
        } + (rows.size - 1) * verticalSpacing

        layout(maxWidth, height) {
            var y = 0
            rows.forEach { row ->
                val rowHeight = row.maxOfOrNull { it.height } ?: 0
                var x = 0

                row.forEach { placeable ->
                    placeable.placeRelative(x, y + (rowHeight - placeable.height) / 2)
                    x += placeable.width + horizontalSpacing
                }

                y += rowHeight + verticalSpacing
            }
        }
    }
}