package com.kipia.management.mobile.ui.components.scheme.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeLine
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import timber.log.Timber
import kotlin.math.*

@Composable
fun CompactLineDialog(
    shape: ComposeLine,
    onDismiss: () -> Unit,
    onUpdate: (ComposeShape) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .wrapContentSize(Alignment.Center)
        ) {
            DraggableCard(
                modifier = Modifier
                    .widthIn(min = 280.dp, max = 320.dp)
                    .wrapContentHeight(),
                showDragHandle = true
            ) {
                var length by remember { mutableFloatStateOf(calculateLength(shape)) }
                var angle by remember { mutableFloatStateOf(calculateAngle(shape)) }
                var rotation by remember { mutableStateOf(shape.rotation) }

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

                // Угол наклона
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Угол наклона:", style = MaterialTheme.typography.bodyMedium)
                    Text("${angle.toInt()}°", style = MaterialTheme.typography.titleMedium)
                }

                Slider(
                    value = angle,
                    onValueChange = { angle = it },
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
                        onClick = { angle = (angle - 15f).coerceIn(0f, 360f) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-15°")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { angle = (angle + 15f).coerceIn(0f, 360f) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+15°")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Поворот фигуры
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Поворот фигуры:", style = MaterialTheme.typography.bodyMedium)
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
                        onClick = { rotation = (rotation - 45f).coerceIn(0f, 360f) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-45°")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { rotation = (rotation + 45f).coerceIn(0f, 360f) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+45°")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
                            Timber.d("🎯 Line dialog applying: length=$length, angle=$angle, rotation=$rotation")
                            val updatedLine = createLineFromParams(shape, length, angle, rotation)
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
    }
}

// Вспомогательные функции остаются без изменений
private fun calculateLength(line: ComposeLine): Float {
    val dx = line.endX - line.startX
    val dy = line.endY - line.startY
    return sqrt(dx * dx + dy * dy)
}

private fun calculateAngle(line: ComposeLine): Float {
    val dx = line.endX - line.startX
    val dy = line.endY - line.startY
    val angle = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()
    return if (angle < 0) angle + 360f else angle
}

private fun createLineFromParams(
    originalLine: ComposeLine,
    length: Float,
    angle: Float,
    rotation: Float
): ComposeLine {
    val rad = Math.toRadians(angle.toDouble())
    val dx = (length * cos(rad)).toFloat()
    val dy = (length * sin(rad)).toFloat()

    val centerX = (originalLine.startX + originalLine.endX) / 2
    val centerY = (originalLine.startY + originalLine.endY) / 2

    val startX = centerX - dx / 2
    val startY = centerY - dy / 2
    val endX = centerX + dx / 2
    val endY = centerY + dy / 2

    val newWidth = (abs(dx) + 20f).coerceAtLeast(50f)
    val newHeight = (abs(dy) + 20f).coerceAtLeast(20f)

    return originalLine.copy(
        startX = startX,
        startY = startY,
        endX = endX,
        endY = endY,
        width = newWidth,
        height = newHeight,
        rotation = rotation
    )
}