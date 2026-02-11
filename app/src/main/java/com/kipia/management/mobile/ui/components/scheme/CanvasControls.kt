package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CanvasControls(
    canUndo: Boolean,
    canRedo: Boolean,
    selectedShape: ComposeShape?,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToggleGrid: () -> Unit,
    onShowGridSettings: () -> Unit,
    onZoomIn: (() -> Unit)? = null,
    onZoomOut: (() -> Unit)? = null,
    onResetView: (() -> Unit)? = null,
    onShowLayers: () -> Unit,
    onShowAlignment: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showZoomMenu by remember { mutableStateOf(false) }

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
            // UNDO/REDO КНОПКИ
            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Отменить",
                    modifier = Modifier.size(20.dp),
                    tint = if (canUndo)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Повторить",
                    modifier = Modifier.size(20.dp),
                    tint = if (canRedo)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }

            // Разделитель
            Divider(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            // Кнопки зума (опционально)
            if (onZoomIn != null && onZoomOut != null && onResetView != null) {
                Box {
                    IconButton(
                        onClick = { showZoomMenu = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ZoomIn,
                            contentDescription = "Зум",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showZoomMenu,
                        onDismissRequest = { showZoomMenu = false },
                        modifier = Modifier.width(180.dp)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Увеличить") },
                            onClick = {
                                onZoomIn()
                                showZoomMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ZoomIn, contentDescription = null)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Уменьшить") },
                            onClick = {
                                onZoomOut()
                                showZoomMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.ZoomOut, contentDescription = null)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Сбросить вид") },
                            onClick = {
                                onResetView()
                                showZoomMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.FilterCenterFocus, contentDescription = null)
                            }
                        )
                    }
                }

                Divider(
                    modifier = Modifier
                        .height(32.dp)
                        .width(1.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            }

            // Меню настроек холста
            Box {
                IconButton(
                    onClick = { expanded = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = "Настройки холста",
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.width(240.dp)
                ) {
                    // Сетка
                    DropdownMenuItem(
                        text = { Text("Включить сетку") },
                        onClick = {
                            onToggleGrid()
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.GridOn, contentDescription = null)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Настройки сетки") },
                        onClick = {
                            onShowGridSettings()
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Settings, contentDescription = null)
                        }
                    )

                    Divider()

                    // Слои и выравнивание
                    DropdownMenuItem(
                        text = { Text("Управление слоями") },
                        onClick = {
                            onShowLayers()
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Layers, contentDescription = null)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Выравнивание") },
                        onClick = {
                            onShowAlignment()
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.AlignHorizontalCenter, contentDescription = null)
                        }
                    )

                    Divider()

                    // Информация
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text("Размер холста")
                                Text(
                                    text = "Используйте жесты для зума и панорамирования",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = { expanded = false },
                        enabled = false
                    )
                }
            }
        }
    }
}