package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.viewmodel.EditorMode

@Composable
fun ShapeToolbar(
    editorMode: EditorMode,
    selectedShape: ComposeShape?,
    onModeChanged: (EditorMode) -> Unit,
    onAddRectangle: () -> Unit,
    onAddLine: () -> Unit,
    onAddEllipse: () -> Unit,
    onAddText: () -> Unit,
    onDeleteShape: () -> Unit,
    onBringToFront: () -> Unit,
    onSendToBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Режим выбора
            IconButton(
                onClick = { onModeChanged(EditorMode.SELECT) },
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (editorMode == EditorMode.SELECT)
                        MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                )
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Выбор")
            }

            HorizontalDivider(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
            )

            // Инструменты фигур
            IconButton(
                onClick = onAddRectangle,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (editorMode == EditorMode.RECTANGLE)
                        MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                )
            ) {
                Icon(Icons.Default.CropSquare, contentDescription = "Прямоугольник")
            }

            IconButton(
                onClick = onAddLine,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (editorMode == EditorMode.LINE)
                        MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                )
            ) {
                Icon(Icons.Default.HorizontalRule, contentDescription = "Линия")
            }

            IconButton(
                onClick = onAddEllipse,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (editorMode == EditorMode.ELLIPSE)
                        MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                )
            ) {
                Icon(Icons.Default.Circle, contentDescription = "Эллипс")
            }

            IconButton(
                onClick = onAddText,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (editorMode == EditorMode.TEXT)
                        MaterialTheme.colorScheme.primaryContainer
                    else Color.Transparent
                )
            ) {
                Icon(Icons.Default.TextFields, contentDescription = "Текст")
            }

            HorizontalDivider(
                modifier = Modifier
                    .height(32.dp)
                    .width(1.dp)
            )

            // Действия с выбранной фигурой
            if (selectedShape != null) {
                IconButton(
                    onClick = onDeleteShape,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить")
                }

                IconButton(onClick = onBringToFront) {
                    Icon(Icons.Default.Layers, contentDescription = "На передний план")
                }

                IconButton(onClick = onSendToBack) {
                    Icon(Icons.Default.LayersClear, contentDescription = "На задний план")
                }
            }
        }
    }
}