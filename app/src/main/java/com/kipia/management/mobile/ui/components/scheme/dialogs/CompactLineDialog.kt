package com.kipia.management.mobile.ui.components.scheme.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeLine
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import timber.log.Timber
import kotlin.math.*

@Composable
fun CompactLineDialog(
    shape: ComposeLine,
    onDismiss: () -> Unit,
    onUpdate: (ComposeShape) -> Unit,
    modifier: Modifier = Modifier
) {
    DraggableCard(
        modifier = modifier
            .widthIn(min = 280.dp, max = 320.dp)
            .wrapContentHeight(),
        showDragHandle = true,
        onClose = onDismiss
    ) {
        var length by remember { mutableFloatStateOf(calculateLength(shape)) }
        var rotation by remember { mutableFloatStateOf(shape.rotation) }

        // Заголовок
        Text(
            text = "Настройки линии",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

        // Длина
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Длина:", style = MaterialTheme.typography.bodyMedium)
            Text("${length.toInt()}", style = MaterialTheme.typography.titleMedium)
        }

        Slider(
            value = length,
            onValueChange = { length = it },
            valueRange = 10f..500f,
            steps = 49,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { length = (length - 10f).coerceAtLeast(10f) },
                modifier = Modifier.weight(1f)
            ) {
                Text("-10")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { length += 10f },
                modifier = Modifier.weight(1f)
            ) {
                Text("+10")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Поворот фигуры
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Поворот:", style = MaterialTheme.typography.bodyMedium)
            Text("${rotation.toInt()}°", style = MaterialTheme.typography.titleMedium)
        }

        Slider(
            value = rotation,
            onValueChange = { rotation = it },
            valueRange = 0f..360f,
            steps = 36,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { rotation = (rotation - 15f).coerceIn(0f, 360f) },
                modifier = Modifier.weight(1f)
            ) {
                Text("-15°")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { rotation = (rotation + 15f).coerceIn(0f, 360f) },
                modifier = Modifier.weight(1f)
            ) {
                Text("+15°")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = { rotation = 0f },
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Сбросить поворот")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Кнопки действий
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) {
                Text("Отмена")
            }

            Button(
                onClick = {
                    Timber.d("🎯 Line dialog applying: length=$length, rotation=$rotation")
                    val updatedLine = createLineFromParams(shape, length, rotation)

                    Timber.d("   Original: pos=(${shape.x}, ${shape.y}), rot=${shape.rotation}")
                    Timber.d("   Updated: pos=(${updatedLine.x}, ${updatedLine.y}), rot=${updatedLine.rotation}")
                    Timber.d("   start=(${updatedLine.startX}, ${updatedLine.startY}), end=(${updatedLine.endX}, ${updatedLine.endY})")

                    onUpdate(updatedLine)
                    onDismiss()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Применить")
            }
        }
    }
}

// Вспомогательные функции
private fun calculateLength(line: ComposeLine): Float {
    val dx = line.endX - line.startX
    val dy = line.endY - line.startY
    return sqrt(dx * dx + dy * dy)
}

private fun createLineFromParams(
    originalLine: ComposeLine,
    length: Float,
    rotation: Float
): ComposeLine {
    // Базовая линия всегда горизонтальная (угол 0°)
    val dx = length
    val dy = 0f

    // Центр линии (используем текущий центр bounding box)
    val centerX = originalLine.x + originalLine.width / 2
    val centerY = originalLine.y + originalLine.height / 2

    // Вычисляем новые start и end относительно центра (горизонтальная линия)
    val startX = centerX - dx / 2
    val startY = centerY - dy / 2
    val endX = centerX + dx / 2
    val endY = centerY + dy / 2

    // Вычисляем новый bounding box
    val minX = min(startX, endX)
    val minY = min(startY, endY)
    val maxX = max(startX, endX)
    val maxY = max(startY, endY)

    val newWidth = (maxX - minX).coerceAtLeast(20f) + 20f // Добавляем отступ
    val newHeight = (maxY - minY).coerceAtLeast(20f) + 20f

    // Новые start и end относительно нового bounding box
    val newStartX = startX - minX
    val newStartY = startY - minY
    val newEndX = endX - minX
    val newEndY = endY - minY

    Timber.d("📐 createLineFromParams:")
    Timber.d("   center=($centerX, $centerY)")
    Timber.d("   dx=$dx, dy=$dy")
    Timber.d("   new bounding box: min=($minX, $minY), max=($maxX, $maxY)")
    Timber.d("   new width=$newWidth, height=$newHeight")
    Timber.d("   new start=($newStartX, $newStartY), end=($newEndX, $newEndY)")
    Timber.d("   rotation=$rotation")

    return originalLine.copy(
        x = minX,
        y = minY,
        startX = newStartX,
        startY = newStartY,
        endX = newEndX,
        endY = newEndY,
        width = newWidth,
        height = newHeight,
        rotation = rotation  // Сохраняем угол поворота
    )
}