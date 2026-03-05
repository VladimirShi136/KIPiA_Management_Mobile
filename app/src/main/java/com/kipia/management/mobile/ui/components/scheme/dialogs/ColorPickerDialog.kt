package com.kipia.management.mobile.ui.components.scheme.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Color,
    onColorSelected: (Color) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    DraggableCard(
        modifier = modifier
            .widthIn(min = 300.dp, max = 360.dp)
            .heightIn(max = 600.dp),
        showDragHandle = true,
        onClose = onDismiss  // Добавим кнопку закрытия
    ) {
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp)
        ) {
            // Заголовок с кнопкой закрытия
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            var selectedColor by remember { mutableStateOf(initialColor) }
            var hue by remember { mutableFloatStateOf(0f) }
            var saturation by remember { mutableFloatStateOf(1f) }
            var value by remember { mutableFloatStateOf(1f) }

            LaunchedEffect(initialColor) {
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(initialColor.toArgb(), hsv)
                hue = hsv[0]
                saturation = hsv[1]
                value = hsv[2]
            }

            // Hue (оттенок)
            Text("Оттенок", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = List(360) { i ->
                                Color.hsv(i.toFloat(), 1f, 1f)
                            }
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            Slider(
                value = hue,
                onValueChange = {
                    hue = it
                    selectedColor = Color.hsv(hue, saturation, value)
                },
                valueRange = 0f..360f,
                steps = 359,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Насыщенность
            Text("Насыщенность", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = List(100) { i ->
                                Color.hsv(hue, i / 100f, value)
                            }
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            Slider(
                value = saturation,
                onValueChange = {
                    saturation = it
                    selectedColor = Color.hsv(hue, saturation, value)
                },
                valueRange = 0f..1f,
                steps = 99,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Яркость
            Text("Яркость", style = MaterialTheme.typography.labelMedium)
            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                            colors = List(100) { i ->
                                Color.hsv(hue, saturation, i / 100f)
                            }
                        ),
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            Slider(
                value = value,
                onValueChange = {
                    value = it
                    selectedColor = Color.hsv(hue, saturation, value)
                },
                valueRange = 0f..1f,
                steps = 99,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Предпросмотр выбранного цвета
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = selectedColor)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Выбранный цвет",
                        color = if (selectedColor.luminance() > 0.5f) Color.Black else Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопки
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
                        onColorSelected(selectedColor)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Выбрать")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}