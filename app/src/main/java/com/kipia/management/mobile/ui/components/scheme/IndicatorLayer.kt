package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kipia.management.mobile.viewmodel.CanvasState
import com.kipia.management.mobile.viewmodel.EditorMode
import com.kipia.management.mobile.viewmodel.EditorState

@Composable
fun IndicatorLayer(
    canvasState: CanvasState,
    editorState: EditorState,
    showZoomIndicator: Boolean,
    modifier: Modifier = Modifier,
    key: Any? = null
) {
    // Правильное использование remember - сохраняем ключ, но не присваиваем Unit
    remember(key) { key }

    Box(modifier = modifier) {
        // Индикатор масштаба
        if (showZoomIndicator) {
            ZoomIndicator(
                scale = canvasState.scale,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }

        // Индикатор режима
        when (editorState.uiState.mode) {
            EditorMode.PAN_ZOOM -> {
                ModeIndicator(
                    text = "🔍 Режим панорамирования/зума",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                )
            }
            EditorMode.SELECT -> {
                ModeIndicator(
                    text = "👆 Режим выделения",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                )
            }
            EditorMode.RECTANGLE, EditorMode.LINE,
            EditorMode.ELLIPSE, EditorMode.RHOMBUS,
            EditorMode.TEXT, EditorMode.DEVICE -> {
                ModeChip(
                    mode = editorState.uiState.mode,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp)
                )
            }
            else -> { /* NONE - ничего не показываем */ }
        }
    }
}

@Composable
fun ZoomIndicator(
    scale: Float,
    modifier: Modifier = Modifier
) {
    val displayScale = (scale * 100).toInt()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "${displayScale}%",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ModeIndicator(
    text: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun ModeChip(
    mode: EditorMode,
    modifier: Modifier = Modifier
) {
    val text = when (mode) {
        EditorMode.RECTANGLE -> "Режим: Прямоугольник"
        EditorMode.LINE -> "Режим: Линия"
        EditorMode.ELLIPSE -> "Режим: Эллипс"
        EditorMode.RHOMBUS -> "Режим: Ромб"
        EditorMode.TEXT -> "Режим: Текст"
        EditorMode.DEVICE -> "Режим: Добавление прибора"
        else -> ""
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}