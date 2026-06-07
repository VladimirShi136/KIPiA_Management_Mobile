package com.kipia.management.mobile.ui.components.scheme.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.components.scheme.shapes.*
import com.kipia.management.mobile.ui.theme.Dimens
import kotlin.math.*

@Composable
fun EditorDialogHeader(
    title: String,
    icon: ImageVector? = null,
    onClose: (() -> Unit)? = null
) {
    Column {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            
            if (onClose != null) {
                IconButton(
                    onClick = onClose, 
                    modifier = Modifier.align(Alignment.CenterEnd).size(24.dp)
                ) {
                    Icon(Icons.Default.Close, "Закрыть", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(vertical = Dimens.spacingMedium),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun PropertySlider(
    label: String, 
    value: Float, 
    valueRange: ClosedFloatingPointRange<Float>, 
    onValueChange: (Float) -> Unit, 
    suffix: String = "",
    steps: Int = 0
) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${value.toInt()}$suffix", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value, 
            onValueChange = onValueChange, 
            valueRange = valueRange, 
            steps = steps,
            modifier = Modifier.height(32.dp)
        )
    }
}

@Composable
fun ShapePropertiesDialog(
    shape: ComposeShape,
    onDismiss: () -> Unit,
    onUpdate: (ComposeShape) -> Unit
) {
    when (shape) {
        is ComposeLine -> CompactLineDialog(shape, onDismiss, onUpdate)
        is ComposeText -> TextPropertiesDialog(shape, onDismiss, onUpdate)
        else -> CompactSizeRotationDialog(shape, onDismiss, onUpdate)
    }
}

@Composable
fun AddDeviceToSchemeDialog(
    availableDevices: List<Device>,
    onDismiss: () -> Unit,
    onDeviceSelected: (Device) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { EditorDialogHeader("Добавить прибор", onClose = onDismiss) },
        text = {
            if (availableDevices.isEmpty()) {
                Text("Нет доступных приборов для этой локации.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = Dimens.spacingSmall)) {
                        Text("Наименование", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary)
                        Text("Инв. №", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp), color = MaterialTheme.colorScheme.primary)
                    }
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                        items(availableDevices) { device ->
                            Column {
                                Row(modifier = Modifier.fillMaxWidth().clickable { onDeviceSelected(device) }.padding(vertical = Dimens.spacingMedium), verticalAlignment = Alignment.CenterVertically) {
                                    Text(device.getDisplayName(), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                    Text(device.inventoryNumber, style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(100.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun SimpleTextInputDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Float) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var fontSize by remember { mutableFloatStateOf(24f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { EditorDialogHeader("Добавить текст", onClose = onDismiss) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
                OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Текст") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                PropertySlider(label = "Размер шрифта", value = fontSize, valueRange = 12f..120f, onValueChange = { fontSize = it }, suffix = " px")
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(text, fontSize) }, enabled = text.isNotBlank()) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(Dimens.spacingSmall))
                Text("Добавить")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    DraggableCard(
        modifier = modifier.widthIn(min = 300.dp, max = 340.dp), 
        onClose = onDismiss
    ) {
        val scrollState = rememberScrollState()
        var selectedColor by remember { mutableStateOf(initialColor) }
        var hue by remember { mutableFloatStateOf(0f) }
        var saturation by remember { mutableFloatStateOf(1f) }
        var value by remember { mutableFloatStateOf(1f) }

        LaunchedEffect(initialColor) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
            hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
        }

        EditorDialogHeader(title, onClose = onDismiss)

        Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ColorSlider(label = "Оттенок", value = hue, valueRange = 0f..360f, onValueChange = { hue = it; selectedColor = Color.hsv(hue, saturation, value) }, gradientColors = List(360) { Color.hsv(it.toFloat(), 1f, 1f) })
            ColorSlider(label = "Насыщенность", value = saturation, valueRange = 0f..1f, onValueChange = { saturation = it; selectedColor = Color.hsv(hue, saturation, value) }, gradientColors = List(10) { Color.hsv(hue, it / 10f, value) })
            ColorSlider(label = "Яркость", value = value, valueRange = 0f..1f, onValueChange = { value = it; selectedColor = Color.hsv(hue, saturation, value) }, gradientColors = List(10) { Color.hsv(hue, saturation, it / 10f) })

            Card(modifier = Modifier.fillMaxWidth().height(50.dp).padding(vertical = 8.dp), shape = RoundedCornerShape(Dimens.chipRadius), colors = CardDefaults.cardColors(containerColor = selectedColor), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Образец", color = if (selectedColor.luminance() > 0.5f) Color.Black else Color.White, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Отмена") }
                Button(onClick = { onColorSelected(selectedColor); onDismiss() }, modifier = Modifier.weight(1f)) { Text("Выбрать") }
            }
        }
    }
}

@Composable
private fun ColorSlider(label: String, value: Float, valueRange: ClosedFloatingPointRange<Float>, onValueChange: (Float) -> Unit, gradientColors: List<Color>) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(modifier = Modifier.fillMaxWidth().height(12.dp).background(androidx.compose.ui.graphics.Brush.horizontalGradient(gradientColors), RoundedCornerShape(2.dp)))
        Slider(value = value, onValueChange = onValueChange, valueRange = valueRange, modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CompactLineDialog(shape: ComposeLine, onDismiss: () -> Unit, onUpdate: (ComposeShape) -> Unit, modifier: Modifier = Modifier) {
    DraggableCard(modifier = modifier.width(280.dp), onClose = onDismiss) {
        var length by remember { mutableFloatStateOf(calculateLineLength(shape)) }
        var rotation by remember { mutableFloatStateOf(shape.rotation) }
        EditorDialogHeader("Свойства линии", onClose = onDismiss)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
            PropertySlider(label = "Длина", value = length, valueRange = 10f..500f, onValueChange = { length = it })
            PropertySlider(
                label = "Поворот", 
                value = rotation, 
                valueRange = 0f..315f, 
                onValueChange = { rotation = it }, 
                suffix = "°",
                steps = 6 // 0, 45, 90, 135, 180, 225, 270, 315
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Отмена") }
                Button(onClick = { onUpdate(createLineFromParams(shape, length, rotation)); onDismiss() }, modifier = Modifier.weight(1f)) { Text("Готово") }
            }
        }
    }
}

@Composable
fun TextPropertiesDialog(shape: ComposeText, onDismiss: () -> Unit, onUpdate: (ComposeText) -> Unit, modifier: Modifier = Modifier) {
    DraggableCard(modifier = modifier.width(300.dp), onClose = onDismiss) {
        var text by remember { mutableStateOf(shape.text) }
        var fontSize by remember { mutableFloatStateOf(shape.fontSize) }
        var isBold by remember { mutableStateOf(shape.isBold) }
        var isItalic by remember { mutableStateOf(shape.isItalic) }
        var rotation by remember { mutableFloatStateOf(shape.rotation) }
        EditorDialogHeader("Свойства текста", onClose = onDismiss)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
            OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("Текст") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            PropertySlider(label = "Размер", value = fontSize, valueRange = 8f..72f, onValueChange = { fontSize = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = isBold, onClick = { isBold = !isBold }, label = { Text("Жирный") }, modifier = Modifier.weight(1f))
                FilterChip(selected = isItalic, onClick = { isItalic = !isItalic }, label = { Text("Курсив") }, modifier = Modifier.weight(1f))
            }
            PropertySlider(
                label = "Поворот", 
                value = rotation, 
                valueRange = 0f..315f, 
                onValueChange = { rotation = it }, 
                suffix = "°",
                steps = 6
            )
            Button(onClick = {
                val updated = shape.copy(text = text, fontSize = fontSize, isBold = isBold, isItalic = isItalic, rotation = rotation)
                updated.width = (text.length * fontSize * 0.6f + 20f).coerceAtLeast(50f)
                updated.height = fontSize * 1.5f
                onUpdate(updated); onDismiss()
            }, modifier = Modifier.fillMaxWidth()) { Text("Применить") }
        }
    }
}

@Composable
fun CompactSizeRotationDialog(shape: ComposeShape, onDismiss: () -> Unit, onUpdate: (ComposeShape) -> Unit, modifier: Modifier = Modifier) {
    DraggableCard(modifier = modifier.width(280.dp), onClose = onDismiss) {
        var width by remember { mutableFloatStateOf(shape.width) }
        var height by remember { mutableFloatStateOf(shape.height) }
        var rotation by remember { mutableFloatStateOf(shape.rotation) }
        EditorDialogHeader("Размер и поворот", onClose = onDismiss)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)) {
            PropertySlider(label = "Ширина", value = width, valueRange = 10f..1000f, onValueChange = { width = it })
            PropertySlider(label = "Высота", value = height, valueRange = 10f..1000f, onValueChange = { height = it })
            PropertySlider(
                label = "Поворот", 
                value = rotation, 
                valueRange = 0f..315f, 
                onValueChange = { rotation = it }, 
                suffix = "°",
                steps = 6
            )
            Button(onClick = {
                val updated = when (shape) {
                    is ComposeRectangle -> shape.copy(width = width, height = height, rotation = rotation)
                    is ComposeEllipse -> shape.copy(width = width, height = height, rotation = rotation)
                    is ComposeRhombus -> shape.copy(width = width, height = height, rotation = rotation)
                    else -> shape
                }
                onUpdate(updated); onDismiss()
            }, modifier = Modifier.fillMaxWidth()) { Text("Применить") }
        }
    }
}

private fun calculateLineLength(line: ComposeLine): Float {
    val dx = line.endX - line.startX; val dy = line.endY - line.startY
    return sqrt(dx * dx + dy * dy)
}

private fun createLineFromParams(originalLine: ComposeLine, length: Float, rotation: Float): ComposeLine {
    val centerX = (originalLine.startX + originalLine.endX) / 2f; val centerY = (originalLine.startY + originalLine.endY) / 2f
    val radians = Math.toRadians(rotation.toDouble())
    val dx = (length / 2f) * cos(radians).toFloat(); val dy = (length / 2f) * sin(radians).toFloat()
    val newStartX = centerX - dx; val newStartY = centerY - dy; val newEndX = centerX + dx; val newEndY = centerY + dy
    val minX = min(newStartX, newEndX) - originalLine.strokeWidth; val minY = min(newStartY, newEndY) - originalLine.strokeWidth
    val maxX = max(newStartX, newEndX) + originalLine.strokeWidth; val maxY = max(newStartY, newEndY) + originalLine.strokeWidth
    return originalLine.copy(startX = newStartX, startY = newStartY, endX = newEndX, endY = newEndY, x = minX, y = minY, width = (maxX - minX).coerceAtLeast(1f), height = (maxY - minY).coerceAtLeast(1f), rotation = rotation)
}
