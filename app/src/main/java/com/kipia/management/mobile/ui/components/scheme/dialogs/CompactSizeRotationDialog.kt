package com.kipia.management.mobile.ui.components.scheme.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kipia.management.mobile.ui.components.scheme.shapes.*
import timber.log.Timber

@Composable
fun CompactSizeRotationDialog(
    shape: ComposeShape,
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
                var width by remember { mutableStateOf(shape.width) }
                var height by remember { mutableStateOf(shape.height) }
                var rotation by remember { mutableStateOf(shape.rotation) }

                val widthText = remember(width) { width.toInt().toString() }
                val heightText = remember(height) { height.toInt().toString() }

                // Заголовок
                Text(
                    text = "Размер и поворот",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                // Размеры
                OutlinedTextField(
                    value = widthText,
                    onValueChange = {
                        width = it.toIntOrNull()?.coerceIn(10, 1000)?.toFloat() ?: width
                    },
                    label = { Text("Ширина") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = heightText,
                    onValueChange = {
                        height = it.toIntOrNull()?.coerceIn(10, 1000)?.toFloat() ?: height
                    },
                    label = { Text("Высота") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Поворот
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

                // Кнопка сброса
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
                            Timber.d("🎯 Dialog applying: rotation=$rotation, width=$width, height=$height")
                            val updatedShape = when (shape) {
                                is ComposeRectangle -> shape.copy(
                                    width = width,
                                    height = height,
                                    rotation = rotation
                                )
                                is ComposeLine -> shape.copy(
                                    width = width,
                                    height = height,
                                    rotation = rotation
                                )
                                is ComposeEllipse -> shape.copy(
                                    width = width,
                                    height = height,
                                    rotation = rotation
                                )
                                is ComposeRhombus -> shape.copy(
                                    width = width,
                                    height = height,
                                    rotation = rotation
                                )
                                is ComposeText -> shape.copy(
                                    width = width,
                                    height = height,
                                    rotation = rotation
                                )
                                else -> shape
                            }
                            onUpdate(updatedShape)
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