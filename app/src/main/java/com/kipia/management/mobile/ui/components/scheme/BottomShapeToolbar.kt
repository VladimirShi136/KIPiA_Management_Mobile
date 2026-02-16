package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.viewmodel.EditorMode
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomShapeToolbar(
    editorMode: EditorMode,
    selectedShape: ComposeShape?,
    selectedDevice: Pair<Device, SchemeDevice>?,
    onModeChanged: (EditorMode) -> Unit,
    onAddDevice: () -> Unit,
    onShapeMenuClick: () -> Unit,
    onDeviceMenuClick: () -> Unit,
    onDuplicateShape: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showShapeCreationMenu by remember { mutableStateOf(false) }
    var showShapeActionsMenu by remember { mutableStateOf(false) }
    var showDeviceActionsMenu by remember { mutableStateOf(false) }

    val hasSelectedShape = selectedShape != null
    val hasSelectedDevice = selectedDevice != null

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
            // Кнопка создания фигур (всегда доступна)
            ShapeCreationButton(
                isActive = editorMode in listOf(
                    EditorMode.RECTANGLE,
                    EditorMode.LINE,
                    EditorMode.ELLIPSE,
                    EditorMode.RHOMBUS,
                    EditorMode.TEXT
                ),
                onClick = {
                    Timber.d("ShapeCreationButton clicked, showing menu")
                    showShapeCreationMenu = true
                }
            )

            VerticalDivider()

            // Кнопка добавления устройства
            IconButton(
                onClick = {
                    onModeChanged(EditorMode.DEVICE)
                    onAddDevice()
                },
                modifier = Modifier.size(40.dp),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (editorMode == EditorMode.DEVICE)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface
                )
            ) {
                Icon(
                    Icons.Default.DeviceHub,
                    contentDescription = "Добавить прибор",
                    modifier = Modifier.size(20.dp)
                )
            }

            // Контекстные действия (появляются при выделении)
            if (hasSelectedShape) {
                VerticalDivider()

                ShapeActionsMenu(
                    onMenuClick = onShapeMenuClick,
                    onDuplicate = onDuplicateShape,
                    onDelete = onDeleteSelected
                )
            }

            if (hasSelectedDevice) {
                VerticalDivider()

                DeviceActionsMenu(
                    onMenuClick = onDeviceMenuClick,
                    onDelete = onDeleteSelected
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Индикатор выделения (опционально)
            if (hasSelectedShape || hasSelectedDevice) {
                Badge(
                    modifier = Modifier.padding(end = 4.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = "1",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }

    // Выпадающее меню создания фигур
    DropdownMenu(
        expanded = showShapeCreationMenu,
        onDismissRequest = { showShapeCreationMenu = false },
        modifier = Modifier.width(240.dp)
    ) {
        DropdownMenuItem(
            text = { Text("Прямоугольник") },
            onClick = {
                Timber.d("Rectangle selected, changing mode to RECTANGLE")
                onModeChanged(EditorMode.RECTANGLE)
                showShapeCreationMenu = false
            },
            leadingIcon = {
                Icon(Icons.Default.CropSquare, contentDescription = null)
            }
        )

        DropdownMenuItem(
            text = { Text("Линия") },
            onClick = {
                onModeChanged(EditorMode.LINE)
                showShapeCreationMenu = false
            },
            leadingIcon = {
                Icon(Icons.Default.HorizontalRule, contentDescription = null)
            }
        )

        DropdownMenuItem(
            text = { Text("Эллипс") },
            onClick = {
                onModeChanged(EditorMode.ELLIPSE)
                showShapeCreationMenu = false
            },
            leadingIcon = {
                Icon(Icons.Default.Circle, contentDescription = null)
            }
        )

        DropdownMenuItem(
            text = { Text("Ромб") },
            onClick = {
                onModeChanged(EditorMode.RHOMBUS)
                showShapeCreationMenu = false
            },
            leadingIcon = {
                Icon(Icons.Default.Diamond, contentDescription = null)
            }
        )

        DropdownMenuItem(
            text = { Text("Текст") },
            onClick = {
                onModeChanged(EditorMode.TEXT)
                showShapeCreationMenu = false
            },
            leadingIcon = {
                Icon(Icons.Default.TextFields, contentDescription = null)
            }
        )
    }
}

@Composable
private fun ShapeCreationButton(
    isActive: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = if (isActive)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = "Создать фигуру",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun ShapeActionsMenu(
    onMenuClick: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.ShapeLine,
                contentDescription = "Действия с фигурой",
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.width(240.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Свойства") },
                onClick = {
                    onMenuClick()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Info, contentDescription = null)
                }
            )

            DropdownMenuItem(
                text = { Text("Дублировать") },
                onClick = {
                    onDuplicate()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.FileCopy, contentDescription = null)
                }
            )

            Divider()

            DropdownMenuItem(
                text = {
                    Text(
                        "Удалить",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    onDelete()
                    showMenu = false
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

@Composable
private fun DeviceActionsMenu(
    onMenuClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { showMenu = true },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.Default.Devices,
                contentDescription = "Действия с прибором",
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.width(240.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Свойства") },
                onClick = {
                    onMenuClick()
                    showMenu = false
                },
                leadingIcon = {
                    Icon(Icons.Default.Info, contentDescription = null)
                }
            )

            Divider()

            DropdownMenuItem(
                text = {
                    Text(
                        "Удалить со схемы",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                onClick = {
                    onDelete()
                    showMenu = false
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