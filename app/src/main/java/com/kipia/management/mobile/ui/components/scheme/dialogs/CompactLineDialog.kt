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
    // Центр линии в мировых координатах — не меняется
    val centerX = (originalLine.startX + originalLine.endX) / 2f
    val centerY = (originalLine.startY + originalLine.endY) / 2f

    // Вычисляем новые абсолютные координаты с учётом угла поворота
    val radians = Math.toRadians(rotation.toDouble())
    val dx = (length / 2f) * cos(radians).toFloat()
    val dy = (length / 2f) * sin(radians).toFloat()

    val newStartX = centerX - dx
    val newStartY = centerY - dy
    val newEndX   = centerX + dx
    val newEndY   = centerY + dy

    // x/y/width/height — только для bounds-проверок
    val minX = min(newStartX, newEndX) - originalLine.strokeWidth
    val minY = min(newStartY, newEndY) - originalLine.strokeWidth
    val maxX = max(newStartX, newEndX) + originalLine.strokeWidth
    val maxY = max(newStartY, newEndY) + originalLine.strokeWidth

    return originalLine.copy(
        startX   = newStartX,
        startY   = newStartY,
        endX     = newEndX,
        endY     = newEndY,
        x        = minX,
        y        = minY,
        width    = (maxX - minX).coerceAtLeast(1f),
        height   = (maxY - minY).coerceAtLeast(1f),
        rotation = rotation
    )
}