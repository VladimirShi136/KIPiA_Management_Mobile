package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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

// Список режимов создания фигур - выносим в константу
private val SHAPE_CREATION_MODES = listOf(
    EditorMode.RECTANGLE,
    EditorMode.LINE,
    EditorMode.ELLIPSE,
    EditorMode.RHOMBUS,
    EditorMode.TEXT
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomShapeToolbar(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
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

    // Оптимизированные состояния
    val hasSelectedShape by remember(selectedShape) {
        derivedStateOf { selectedShape != null }
    }
    val hasSelectedDevice by remember(selectedDevice) {
        derivedStateOf { selectedDevice != null }
    }

    // Оптимизация для isShapeCreationMode
    val isShapeCreationMode by remember(editorMode) {
        derivedStateOf { editorMode in SHAPE_CREATION_MODES }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Стабильные кнопки с фиксированными ключами
            item(key = "undo") {
                IconButton(onClick = onUndo, enabled = canUndo) {
                    Icon(Icons.AutoMirrored.Filled.Undo, null)
                }
            }

            item(key = "redo") {
                IconButton(onClick = onRedo, enabled = canRedo) {
                    Icon(Icons.AutoMirrored.Filled.Redo, null)
                }
            }

            item(key = "select") {
                IconButton(onClick = { onModeChanged(EditorMode.SELECT) }) {
                    Icon(Icons.Default.ArrowOutward, null)
                }
            }

            item(key = "panzoom") {
                IconButton(onClick = { onModeChanged(EditorMode.PAN_ZOOM) }) {
                    Icon(Icons.Default.ZoomIn, null)
                }
            }

            item(key = "shape_creation") {
                ShapeCreationButton(
                    isActive = isShapeCreationMode,
                    onClick = { showShapeCreationMenu = true }
                )
            }

            item(key = "add_device") {
                IconButton(onClick = {
                    onModeChanged(EditorMode.DEVICE)
                    onAddDevice()
                }) {
                    Icon(Icons.Default.DeviceHub, null)
                }
            }

            // Динамические элементы с ключами на основе ID
            if (hasSelectedShape) {
                item(key = "shape_actions_${selectedShape?.id ?: "none"}") {
                    ShapeActionsMenu(
                        onMenuClick = onShapeMenuClick,
                        onDuplicate = onDuplicateShape,
                        onDelete = onDeleteSelected
                    )
                }
            }

            if (hasSelectedDevice) {
                item(key = "device_actions_${selectedDevice?.first?.id ?: "none"}") {
                    DeviceActionsMenu(
                        onMenuClick = onDeviceMenuClick,
                        onDelete = onDeleteSelected
                    )
                }
            }
        }
    }

    // Выпадающее меню создания фигур
    ShapeCreationDropdownMenu(
        expanded = showShapeCreationMenu,
        onDismiss = { showShapeCreationMenu = false },
        onModeSelected = onModeChanged
    )
}

@Composable
private fun ShapeCreationButton(
    isActive: Boolean,
    onClick: () -> Unit
) {
    // Используем remember для цвета, чтобы не пересоздавать при каждой рекомпозиции
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = contentColor
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
private fun ShapeCreationDropdownMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onModeSelected: (EditorMode) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.width(240.dp)
    ) {
        // Используем список для итерации вместо повторяющегося кода
        val menuItems = remember {
            listOf(
                Triple(EditorMode.RECTANGLE, "Прямоугольник", Icons.Default.CropSquare),
                Triple(EditorMode.LINE, "Линия", Icons.Default.HorizontalRule),
                Triple(EditorMode.ELLIPSE, "Эллипс", Icons.Default.Circle),
                Triple(EditorMode.RHOMBUS, "Ромб", Icons.Default.Diamond),
                Triple(EditorMode.TEXT, "Текст", Icons.Default.TextFields)
            )
        }

        menuItems.forEach { (mode, label, icon) ->
            DropdownMenuItem(
                text = { Text(label) },
                onClick = {
                    onModeSelected(mode)
                    onDismiss()
                },
                leadingIcon = {
                    Icon(icon, contentDescription = null)
                }
            )
        }
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