package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.ui.components.scheme.shapes.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShapePropertiesPanel(
    shape: ComposeShape?,
    onUpdateShape: (ComposeShape) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (shape == null) return

    var selectedTab by remember { mutableStateOf(0) }

    Card(
        modifier = modifier.width(320.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Иконка в зависимости от типа фигуры
                    Icon(
                        imageVector = when (shape) {
                            is ComposeRectangle -> Icons.Default.CropSquare
                            is ComposeLine -> Icons.Default.HorizontalRule
                            is ComposeEllipse -> Icons.Default.Circle
                            is ComposeText -> Icons.Default.TextFields
                            is ComposeRhombus -> Icons.Default.Diamond
                            else -> Icons.Default.ShapeLine
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = when (shape) {
                            is ComposeRectangle -> "Прямоугольник"
                            is ComposeLine -> "Линия"
                            is ComposeEllipse -> "Эллипс"
                            is ComposeText -> "Текст"
                            is ComposeRhombus -> "Ромб"
                            else -> "Фигура"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Закрыть",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Табы для переключения между разделами
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                edgePadding = 0.dp,
                divider = {},
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Оформление", "Позиция", "Текст").forEachIndexed { index, title ->
                    if (index == 2 && shape !is ComposeText) return@forEachIndexed

                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> AppearanceTab(shape = shape, onUpdateShape = onUpdateShape)
                1 -> PositionTab(shape = shape, onUpdateShape = onUpdateShape)
                2 -> {
                    if (shape is ComposeText) {
                        TextTab(shape = shape, onUpdateShape = onUpdateShape)
                    }
                }
            }
        }
    }
}

@Composable
fun AppearanceTab(
    shape: ComposeShape,
    onUpdateShape: (ComposeShape) -> Unit
) {
    var showFillColorPicker by remember { mutableStateOf(false) }
    var showStrokeColorPicker by remember { mutableStateOf(false) }
    var showStrokeWidthDialog by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Цвет заливки
        item {
            PropertySection(title = "Заливка") {
                ColorPropertyRow(
                    label = "Цвет",
                    color = shape.fillColor,
                    onClick = { showFillColorPicker = true }
                )
            }
        }

        // Цвет обводки
        item {
            PropertySection(title = "Обводка") {
                ColorPropertyRow(
                    label = "Цвет",
                    color = shape.strokeColor,
                    onClick = { showStrokeColorPicker = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                StrokeWidthPropertyRow(
                    width = shape.strokeWidth,
                    onClick = { showStrokeWidthDialog = true }
                )
            }
        }

        // Специфичные свойства для разных фигур
        when (shape) {
            is ComposeRectangle -> {
                item {
                    PropertySection(title = "Углы") {
                        CornerRadiusPropertyRow(
                            radius = shape.cornerRadius,
                            onRadiusChange = { newRadius ->
                                val updated = shape.copy(cornerRadius = newRadius)
                                onUpdateShape(updated)
                            }
                        )
                    }
                }
            }
            is ComposeLine -> {
                item {
                    PropertySection(title = "Концы линии") {
                        LineEndpointsRow(
                            shape = shape,
                            onUpdateShape = onUpdateShape
                        )
                    }
                }
            }
            is ComposeRhombus -> {
                // Специфичных свойств пока нет
            }
        }
    }

    // Диалоги
    if (showFillColorPicker) {
        ColorPickerDialog(
            title = "Цвет заливки",
            initialColor = shape.fillColor,
            onColorSelected = { color ->
                val updated = when (shape) {
                    is ComposeRectangle -> shape.copy(fillColor = color)
                    is ComposeLine -> shape.copy(fillColor = color)
                    is ComposeEllipse -> shape.copy(fillColor = color)
                    is ComposeText -> shape.copy(fillColor = color)
                    is ComposeRhombus -> shape.copy(fillColor = color)
                    else -> shape
                }
                onUpdateShape(updated)
                showFillColorPicker = false
            },
            onDismiss = { showFillColorPicker = false },
            showTransparent = true
        )
    }

    if (showFillColorPicker) {
        ColorPickerDialog(
            title = "Цвет заливки",
            initialColor = shape.fillColor,
            onColorSelected = { color ->
                val updated = when (shape) {
                    is ComposeRectangle -> shape.copy(fillColor = color)
                    is ComposeLine -> shape.copy(fillColor = color)
                    is ComposeEllipse -> shape.copy(fillColor = color)
                    is ComposeText -> shape.copy(fillColor = color)
                    is ComposeRhombus -> shape.copy(fillColor = color)
                    else -> shape
                }
                onUpdateShape(updated)
                showFillColorPicker = false
            },
            onDismiss = { showFillColorPicker = false },
            showTransparent = true // Добавляем параметр
        )
    }

    if (showStrokeWidthDialog) {
        StrokeWidthDialog(
            initialWidth = shape.strokeWidth,
            onWidthSelected = { width ->
                val updated = when (shape) {
                    is ComposeRectangle -> shape.copy(strokeWidth = width)
                    is ComposeLine -> shape.copy(strokeWidth = width)
                    is ComposeEllipse -> shape.copy(strokeWidth = width)
                    is ComposeText -> shape.copy(strokeWidth = width)
                    is ComposeRhombus -> shape.copy(strokeWidth = width)
                    else -> shape
                }
                onUpdateShape(updated)
                showStrokeWidthDialog = false
            },
            onDismiss = { showStrokeWidthDialog = false }
        )
    }
}

@Composable
fun PositionTab(
    shape: ComposeShape,
    onUpdateShape: (ComposeShape) -> Unit
) {
    var positionX by remember(shape.id) { mutableStateOf(shape.x.toString()) }
    var positionY by remember(shape.id) { mutableStateOf(shape.y.toString()) }
    var rotation by remember(shape.id) { mutableStateOf(shape.rotation) }
    var width by remember(shape.id) { mutableStateOf(shape.width.toString()) }
    var height by remember(shape.id) { mutableStateOf(shape.height.toString()) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PropertySection(title = "Позиция") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = positionX,
                        onValueChange = {
                            positionX = it
                            it.toFloatOrNull()?.let { x ->
                                val updated = when (shape) {
                                    is ComposeRectangle -> shape.copy(x = x)
                                    is ComposeLine -> shape.copy(x = x)
                                    is ComposeEllipse -> shape.copy(x = x)
                                    is ComposeText -> shape.copy(x = x)
                                    is ComposeRhombus -> shape.copy(x = x)
                                    else -> shape
                                }
                                onUpdateShape(updated)
                            }
                        },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        value = positionY,
                        onValueChange = {
                            positionY = it
                            it.toFloatOrNull()?.let { y ->
                                val updated = when (shape) {
                                    is ComposeRectangle -> shape.copy(y = y)
                                    is ComposeLine -> shape.copy(y = y)
                                    is ComposeEllipse -> shape.copy(y = y)
                                    is ComposeText -> shape.copy(y = y)
                                    is ComposeRhombus -> shape.copy(y = y)
                                    else -> shape
                                }
                                onUpdateShape(updated)
                            }
                        },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        item {
            PropertySection(title = "Размер") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = width,
                        onValueChange = {
                            width = it
                            it.toFloatOrNull()?.let { w ->
                                if (w >= 10) {
                                    val updated = when (shape) {
                                        is ComposeRectangle -> shape.copy(width = w)
                                        is ComposeLine -> shape.copy(width = w)
                                        is ComposeEllipse -> shape.copy(width = w)
                                        is ComposeText -> shape.copy(width = w)
                                        is ComposeRhombus -> shape.copy(width = w)
                                        else -> shape
                                    }
                                    onUpdateShape(updated)
                                }
                            }
                        },
                        label = { Text("Ширина") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        isError = width.toFloatOrNull()?.let { it < 10 } ?: true
                    )

                    OutlinedTextField(
                        value = height,
                        onValueChange = {
                            height = it
                            it.toFloatOrNull()?.let { h ->
                                if (h >= 10) {
                                    val updated = when (shape) {
                                        is ComposeRectangle -> shape.copy(height = h)
                                        is ComposeLine -> shape.copy(height = h)
                                        is ComposeEllipse -> shape.copy(height = h)
                                        is ComposeText -> shape.copy(height = h)
                                        is ComposeRhombus -> shape.copy(height = h)
                                        else -> shape
                                    }
                                    onUpdateShape(updated)
                                }
                            }
                        },
                        label = { Text("Высота") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        isError = height.toFloatOrNull()?.let { it < 10 } ?: true
                    )
                }

                if (shape is ComposeLine) {
                    Text(
                        text = "Для линии размер определяется положением концов",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        item {
            PropertySection(title = "Поворот") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val newRotation = (rotation - 15) % 360
                            rotation = newRotation
                            val updated = when (shape) {
                                is ComposeRectangle -> shape.copy(rotation = newRotation)
                                is ComposeLine -> shape.copy(rotation = newRotation)
                                is ComposeEllipse -> shape.copy(rotation = newRotation)
                                is ComposeText -> shape.copy(rotation = newRotation)
                                is ComposeRhombus -> shape.copy(rotation = newRotation)
                                else -> shape
                            }
                            onUpdateShape(updated)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.RotateLeft, contentDescription = "-15°")
                    }

                    Slider(
                        value = rotation,
                        onValueChange = {
                            rotation = it
                            val updated = when (shape) {
                                is ComposeRectangle -> shape.copy(rotation = it)
                                is ComposeLine -> shape.copy(rotation = it)
                                is ComposeEllipse -> shape.copy(rotation = it)
                                is ComposeText -> shape.copy(rotation = it)
                                is ComposeRhombus -> shape.copy(rotation = it)
                                else -> shape
                            }
                            onUpdateShape(updated)
                        },
                        valueRange = 0f..360f,
                        steps = 359,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(
                        onClick = {
                            val newRotation = (rotation + 15) % 360
                            rotation = newRotation
                            val updated = when (shape) {
                                is ComposeRectangle -> shape.copy(rotation = newRotation)
                                is ComposeLine -> shape.copy(rotation = newRotation)
                                is ComposeEllipse -> shape.copy(rotation = newRotation)
                                is ComposeText -> shape.copy(rotation = newRotation)
                                is ComposeRhombus -> shape.copy(rotation = newRotation)
                                else -> shape
                            }
                            onUpdateShape(updated)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = "+15°")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "0°",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${rotation.toInt()}°",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "360°",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TextTab(
    shape: ComposeText,
    onUpdateShape: (ComposeShape) -> Unit
) {
    var text by remember(shape.id) { mutableStateOf(shape.text) }
    var fontSize by remember(shape.id) { mutableStateOf(shape.fontSize) }
    var isBold by remember(shape.id) { mutableStateOf(shape.isBold) }
    var isItalic by remember(shape.id) { mutableStateOf(shape.isItalic) }
    var showTextColorPicker by remember { mutableStateOf(false) }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PropertySection(title = "Текст") {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        val updated = shape.copy(
                            text = it,
                            width = (it.length * 10f + 30f).coerceAtLeast(50f)
                        )
                        onUpdateShape(updated)
                    },
                    label = { Text("Содержимое") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        item {
            PropertySection(title = "Шрифт") {
                // Размер шрифта
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Размер:",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(60.dp)
                    )

                    Slider(
                        value = fontSize,
                        onValueChange = {
                            fontSize = it
                            val updated = shape.copy(fontSize = it)
                            onUpdateShape(updated)
                        },
                        valueRange = 8f..72f,
                        steps = 16,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        "${fontSize.toInt()}px",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Стиль текста
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isBold,
                        onClick = {
                            isBold = !isBold
                            val updated = shape.copy(isBold = isBold)
                            onUpdateShape(updated)
                        },
                        label = { Text("Жирный") },
                        leadingIcon = if (isBold) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )

                    FilterChip(
                        selected = isItalic,
                        onClick = {
                            isItalic = !isItalic
                            val updated = shape.copy(isItalic = isItalic)
                            onUpdateShape(updated)
                        },
                        label = { Text("Курсив") },
                        leadingIcon = if (isItalic) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )
                }
            }
        }

        item {
            PropertySection(title = "Цвет текста") {
                ColorPropertyRow(
                    label = "Цвет",
                    color = shape.textColor,
                    onClick = { showTextColorPicker = true }
                )
            }
        }
    }

    if (showTextColorPicker) {
        ColorPickerDialog(
            title = "Цвет текста",
            initialColor = shape.textColor,
            onColorSelected = { color ->
                val updated = shape.copy(textColor = color)
                onUpdateShape(updated)
                showTextColorPicker = false
            },
            onDismiss = { showTextColorPicker = false }
        )
    }
}

@Composable
fun PropertySection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            content()
        }
    }
}

@Composable
fun ColorPropertyRow(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Предпросмотр цвета
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (color == Color.Transparent) {
                            Modifier.background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.LightGray, Color.White)
                                )
                            )
                        } else {
                            Modifier.background(color)
                        }
                    )
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            Text(
                text = if (color == Color.Transparent) "Прозрачный" else color.toHexDisplay(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Изменить",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun StrokeWidthPropertyRow(
    width: Float,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Толщина",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Предпросмотр линии
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(24.dp)
                    .background(Color.Transparent)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawLine(
                        color = Color.Black,
                        start = Offset(0f, size.height / 2),
                        end = Offset(size.width, size.height / 2),
                        strokeWidth = width
                    )
                }
            }

            Text(
                text = "${width.toInt()} px",
                style = MaterialTheme.typography.bodySmall
            )

            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = "Изменить",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun CornerRadiusPropertyRow(
    radius: Float,
    onRadiusChange: (Float) -> Unit
) {
    var localRadius by remember { mutableStateOf(radius) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Радиус скругления",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "${localRadius.toInt()} px",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Slider(
            value = localRadius,
            onValueChange = {
                localRadius = it
                onRadiusChange(it)
            },
            valueRange = 0f..50f,
            steps = 25
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(0f, 8f, 16f, 24f).forEach { preset ->
                FilterChip(
                    selected = localRadius == preset,
                    onClick = {
                        localRadius = preset
                        onRadiusChange(preset)
                    },
                    label = { Text("${preset.toInt()}") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LineEndpointsRow(
    shape: ComposeLine,
    onUpdateShape: (ComposeShape) -> Unit
) {
    var startX by remember(shape.id) { mutableStateOf(shape.startX.toString()) }
    var startY by remember(shape.id) { mutableStateOf(shape.startY.toString()) }
    var endX by remember(shape.id) { mutableStateOf(shape.endX.toString()) }
    var endY by remember(shape.id) { mutableStateOf(shape.endY.toString()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Начальная точка",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = startX,
                onValueChange = {
                    startX = it
                    it.toFloatOrNull()?.let { x ->
                        val updated = shape.copy(startX = x)
                        onUpdateShape(updated)
                    }
                },
                label = { Text("X") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = startY,
                onValueChange = {
                    startY = it
                    it.toFloatOrNull()?.let { y ->
                        val updated = shape.copy(startY = y)
                        onUpdateShape(updated)
                    }
                },
                label = { Text("Y") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }

        Text(
            text = "Конечная точка",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = endX,
                onValueChange = {
                    endX = it
                    it.toFloatOrNull()?.let { x ->
                        val updated = shape.copy(
                            endX = x,
                            width = maxOf(shape.startX, x) + 10f
                        )
                        onUpdateShape(updated)
                    }
                },
                label = { Text("X") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
                value = endY,
                onValueChange = {
                    endY = it
                    it.toFloatOrNull()?.let { y ->
                        val updated = shape.copy(
                            endY = y,
                            height = maxOf(shape.startY, y) + 10f
                        )
                        onUpdateShape(updated)
                    }
                },
                label = { Text("Y") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// Вспомогательная функция для отображения цвета в HEX
private fun Color.toHexDisplay(): String {
    val red = (red * 255).toInt()
    val green = (green * 255).toInt()
    val blue = (blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}