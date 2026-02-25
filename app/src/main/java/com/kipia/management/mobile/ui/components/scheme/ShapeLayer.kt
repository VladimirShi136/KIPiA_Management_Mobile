package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.viewmodel.CanvasState
import com.kipia.management.mobile.viewmodel.EditorState
import kotlin.math.roundToInt

@Composable
fun ShapeLayer(
    shapes: List<ComposeShape>,
    canvasState: CanvasState,
    editorState: EditorState,
    modifier: Modifier = Modifier,
    key: Any? = null
) {
    // Правильное использование remember
    remember(key) { key }

    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }

    val stableScale by remember(canvasState.scale) {
        derivedStateOf { (canvasState.scale / 0.05).roundToInt() * 0.05f }
    }

    val visibleArea by remember(canvasState, canvasWidth, canvasHeight, stableScale) {
        derivedStateOf {
            if (canvasWidth == 0 || canvasHeight == 0) return@derivedStateOf Rect.Zero

            Rect(
                left = -canvasState.offset.x / stableScale,
                top = -canvasState.offset.y / stableScale,
                right = (-canvasState.offset.x + canvasWidth) / stableScale,
                bottom = (-canvasState.offset.y + canvasHeight) / stableScale
            )
        }
    }

    val visibleShapes by remember(shapes, visibleArea) {
        derivedStateOf {
            shapes.filter { shape ->
                val shapeBounds = Rect(
                    shape.x, shape.y,
                    shape.x + shape.width,
                    shape.y + shape.height
                )
                shapeBounds.overlaps(visibleArea)
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                canvasWidth = size.width
                canvasHeight = size.height
            }
    ) {
        withTransform({
            translate(left = canvasState.offset.x, top = canvasState.offset.y)
            scale(scaleX = stableScale, scaleY = stableScale)
        }) {
            visibleShapes.forEach { shape ->
                val isSelected = editorState.selection.selectedShapeId == shape.id
                shape.draw(this, isSelected)
            }
        }
    }
}