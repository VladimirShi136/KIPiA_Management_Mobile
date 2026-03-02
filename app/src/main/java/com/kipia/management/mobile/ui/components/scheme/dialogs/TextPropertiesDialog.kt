package com.kipia.management.mobile.ui.components.scheme.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeText

@Composable
fun TextPropertiesDialog(
    shape: ComposeText,
    onDismiss: () -> Unit,
    onUpdate: (ComposeText) -> Unit
) {
    var text by remember { mutableStateOf(shape.text) }
    var fontSize by remember { mutableFloatStateOf(shape.fontSize) }
    var isBold by remember { mutableStateOf(shape.isBold) }
    var isItalic by remember { mutableStateOf(shape.isItalic) }
    var rotation by remember { mutableStateOf(shape.rotation) }

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
                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 8.dp)
                ) {
                    // Заголовок
                    Text(
                        text = "Свойства текста",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))

                    // Текст
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Текст") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Размер шрифта
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Размер:", style = MaterialTheme.typography.bodyMedium)
                        Text("${fontSize.toInt()}px", style = MaterialTheme.typography.titleMedium)
                    }

                    Slider(
                        value = fontSize,
                        onValueChange = { fontSize = it },
                        valueRange = 8f..72f,
                        steps = 64,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { fontSize = (fontSize - 4f).coerceAtLeast(8f) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("-4")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { fontSize = (fontSize + 4f).coerceAtMost(72f) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("+4")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Стиль текста
                    Text("Стиль:", style = MaterialTheme.typography.bodyMedium)

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = isBold,
                            onClick = { isBold = !isBold },
                            label = { Text("Жирный") },
                            modifier = Modifier.weight(1f),
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
                            onClick = { isItalic = !isItalic },
                            label = { Text("Курсив") },
                            modifier = Modifier.weight(1f),
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

                    // Кнопка сброса поворота
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
                                val updatedText = shape.copy(
                                    text = text,
                                    fontSize = fontSize,
                                    isBold = isBold,
                                    isItalic = isItalic,
                                    rotation = rotation
                                )
                                // Обновляем размеры контейнера под новый текст
                                updatedText.width = (text.length * fontSize * 0.6f + 20f).coerceAtLeast(50f)
                                updatedText.height = fontSize * 1.5f
                                onUpdate(updatedText)
                                onDismiss()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Применить")
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}