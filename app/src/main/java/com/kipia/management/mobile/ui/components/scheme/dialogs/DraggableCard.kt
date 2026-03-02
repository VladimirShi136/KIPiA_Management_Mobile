package com.kipia.management.mobile.ui.components.scheme.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun DraggableCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
    ),
    elevation: CardElevation = CardDefaults.cardElevation(8.dp),
    showDragHandle: Boolean = true,
    onDrag: (Offset) -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Флаг для отслеживания, перетаскивается ли сейчас карточка
    var isDragging by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                        onDrag(Offset(offsetX, offsetY))
                    }
                )
            }
    ) {
        Card(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .then(modifier),
            shape = shape,
            colors = colors,
            elevation = elevation
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (showDragHandle) {
                    // Визуальный индикатор для перетаскивания
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Три полоски - индикатор перетаскивания
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(3) { _ ->
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(4.dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(
                                                alpha = if (isDragging) 0.8f else 0.3f
                                            ),
                                            shape = RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                content()
            }
        }
    }
}