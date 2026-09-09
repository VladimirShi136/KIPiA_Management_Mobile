package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import com.kipia.management.mobile.ui.components.scheme.shapes.*
import com.kipia.management.mobile.viewmodel.CanvasState
import com.kipia.management.mobile.viewmodel.EditorState
import kotlin.math.max
import kotlin.math.min

@Composable
fun ShapeLayer(
    shapes: List<ComposeShape>,
    canvasState: CanvasState,
    editorState: EditorState,
    modifier: Modifier = Modifier,
    key: Any? = null,
    debugMode: Boolean = false  // По умолчанию false
) {
    // Определяем тему через MaterialTheme (правильный способ)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val isDarkTheme = surfaceColor.luminance() < 0.5f

    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }

    // ...existing code...
    val visibleArea by remember(canvasState, canvasWidth, canvasHeight) {
        derivedStateOf {
            if (canvasWidth == 0 || canvasHeight == 0) return@derivedStateOf Rect.Zero

            Rect(
                left = -canvasState.offset.x / canvasState.scale,
                top = -canvasState.offset.y / canvasState.scale,
                right = (-canvasState.offset.x + canvasWidth) / canvasState.scale,
                bottom = (-canvasState.offset.y + canvasHeight) / canvasState.scale
            )
        }
    }

    // Фильтруем видимые фигуры
    val visibleShapes by remember(shapes, visibleArea) {
        derivedStateOf {
            shapes.filter { shape ->
                val shapeBounds = if (shape is ComposeLine) {
                    // Для линии используем реальные абсолютные координаты
                    Rect(
                        left = min(shape.startX, shape.endX) - shape.strokeWidth,
                        top = min(shape.startY, shape.endY) - shape.strokeWidth,
                        right = max(shape.startX, shape.endX) + shape.strokeWidth,
                        bottom = max(shape.startY, shape.endY) + shape.strokeWidth
                    )
                } else {
                    Rect(shape.x, shape.y, shape.x + shape.width, shape.y + shape.height)
                }
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
        // Рисуем каждую видимую фигуру
        visibleShapes.forEach { shape ->
            val isSelected = editorState.selection.selectedShapeId == shape.id

            if (shape is ComposeLine) {
                // Линия использует абсолютные координаты — не применяем translate по shape.x/y
                val scaledStartX = shape.startX * canvasState.scale + canvasState.offset.x
                val scaledStartY = shape.startY * canvasState.scale + canvasState.offset.y
                val scaledEndX = shape.endX * canvasState.scale + canvasState.offset.x
                val scaledEndY = shape.endY * canvasState.scale + canvasState.offset.y
                val scaledStrokeWidth = shape.strokeWidth * canvasState.scale

                val displayColor = shape.strokeColor

                if (isDarkTheme && shape.strokeColor.isBlack()) {
                    drawLine(
                        color = Color.White,
                        start = Offset(scaledStartX, scaledStartY),
                        end = Offset(scaledEndX, scaledEndY),
                        strokeWidth = scaledStrokeWidth + 4f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }

                drawLine(
                    color = displayColor,
                    start = Offset(scaledStartX, scaledStartY),
                    end = Offset(scaledEndX, scaledEndY),
                    strokeWidth = scaledStrokeWidth,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )

                if (isSelected) {
                    drawLine(
                        color = Color.Cyan.copy(alpha = 0.8f),
                        start = Offset(scaledStartX, scaledStartY),
                        end = Offset(scaledEndX, scaledEndY),
                        strokeWidth = 3f * canvasState.scale,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
            } else {
                // Остальные фигуры — как раньше
                val screenX = shape.x * canvasState.scale + canvasState.offset.x
                val screenY = shape.y * canvasState.scale + canvasState.offset.y
                val scaledWidth = shape.width * canvasState.scale
                val scaledHeight = shape.height * canvasState.scale
                val scaledStrokeWidth = shape.strokeWidth * canvasState.scale

                withTransform({
                    translate(screenX, screenY)
                    rotate(
                        degrees = shape.rotation,
                        pivot = Offset(scaledWidth / 2, scaledHeight / 2)
                    )
                }) {
                    drawShapeWithGlobalTransform(
                        shape,
                        scaledWidth,
                        scaledHeight,
                        scaledStrokeWidth,
                        canvasState.scale,
                        isDarkTheme
                    )
                    if (isSelected) drawSelectionMarker(
                        shape,
                        scaledWidth,
                        scaledHeight,
                        canvasState.scale
                    )
                }
            }
        }
    }
}

/**
 * Рисует фигуру (только саму фигуру, без маркеров выделения)
 */
private fun DrawScope.drawShapeWithGlobalTransform(
    shape: ComposeShape,
    scaledWidth: Float,
    scaledHeight: Float,
    scaledStrokeWidth: Float,
    scaleFactor: Float,
    isDarkTheme: Boolean
) {
    when (shape) {
        is ComposeRectangle -> {
            val scaledCornerRadius = shape.cornerRadius * scaleFactor
            
            if (isDarkTheme && shape.fillColor.isBlack()) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset.Zero,
                    size = Size(scaledWidth, scaledHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(scaledCornerRadius),
                    style = Stroke(width = 2f)
                )
            }

            if (isDarkTheme && shape.strokeColor.isBlack() && shape.strokeWidth > 0) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset.Zero,
                    size = Size(scaledWidth, scaledHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(scaledCornerRadius),
                    style = Stroke(width = scaledStrokeWidth + 2f)
                )
            }

            // Заливка
            if (shape.fillColor != Color.Transparent) {
                drawRoundRect(
                    color = shape.fillColor,
                    topLeft = Offset.Zero,
                    size = Size(scaledWidth, scaledHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(scaledCornerRadius)
                )
            }

            // Обводка
            if (shape.strokeColor != Color.Transparent && shape.strokeWidth > 0) {
                drawRoundRect(
                    color = shape.strokeColor,
                    topLeft = Offset.Zero,
                    size = Size(scaledWidth, scaledHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(scaledCornerRadius),
                    style = Stroke(width = scaledStrokeWidth)
                )
            }
        }

        is ComposeLine -> {
            val scaledStartX = shape.startX * scaleFactor
            val scaledStartY = shape.startY * scaleFactor
            val scaledEndX = shape.endX * scaleFactor
            val scaledEndY = shape.endY * scaleFactor
            
            drawLine(
                color = shape.strokeColor,
                start = Offset(scaledStartX, scaledStartY),
                end = Offset(scaledEndX, scaledEndY),
                strokeWidth = scaledStrokeWidth,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        is ComposeEllipse -> {
            if (isDarkTheme && shape.fillColor.isBlack()) {
                drawOval(
                    color = Color.White,
                    topLeft = Offset.Zero,
                    size = Size(scaledWidth, scaledHeight),
                    style = Stroke(width = 2f)
                )
            }

            if (isDarkTheme && shape.strokeColor.isBlack() && shape.strokeWidth > 0) {
                drawOval(
                    color = Color.White,
                    topLeft = Offset.Zero,
                    size = Size(scaledWidth, scaledHeight),
                    style = Stroke(width = scaledStrokeWidth + 2f)
                )
            }

            // Заливка
            if (shape.fillColor != Color.Transparent) {
                drawOval(
                    color = shape.fillColor,
                    topLeft = Offset.Zero,
                    size = Size(scaledWidth, scaledHeight)
                )
            }

            // Обводка
            if (shape.strokeColor != Color.Transparent && shape.strokeWidth > 0) {
                drawOval(
                    color = shape.strokeColor,
                    topLeft = Offset.Zero,
                    size = Size(scaledWidth, scaledHeight),
                    style = Stroke(width = scaledStrokeWidth)
                )
            }
        }

        is ComposeRhombus -> {
            val path = androidx.compose.ui.graphics.Path().apply {
                val centerX = scaledWidth / 2
                val centerY = scaledHeight / 2

                moveTo(0f, 0f)              // Левый верх (0,0)
                lineTo(centerX, centerY)    // Центр
                lineTo(0f, scaledHeight)    // Левый низ (0, height)
                close()

                moveTo(scaledWidth, 0f)          // Правый верх (width,0)
                lineTo(centerX, centerY)         // Центр
                lineTo(scaledWidth, scaledHeight) // Правый низ (width, height)
                close()
            }

            if (isDarkTheme && shape.fillColor.isBlack()) {
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = 2f)
                )
            }

            if (isDarkTheme && shape.strokeColor.isBlack() && shape.strokeWidth > 0) {
                drawPath(
                    path = path,
                    color = Color.White,
                    style = Stroke(width = scaledStrokeWidth + 2f)
                )
            }

            // Заливка
            if (shape.fillColor != Color.Transparent) {
                drawPath(
                    path = path,
                    color = shape.fillColor
                )
            }

            // Обводка
            if (shape.strokeColor != Color.Transparent && shape.strokeWidth > 0) {
                drawPath(
                    path = path,
                    color = shape.strokeColor,
                    style = Stroke(width = scaledStrokeWidth)
                )
            }
        }

        is ComposeText -> {
            if (isDarkTheme && shape.strokeColor.isBlack()) {
                drawIntoCanvas { canvas ->
                    val paint = android.graphics.Paint().apply {
                        style = android.graphics.Paint.Style.STROKE
                        color = Color.White.toArgb()
                        strokeWidth = 4f
                        textSize = shape.fontSize * scaleFactor
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.CENTER
                        if (shape.isBold) {
                            isFakeBoldText = true
                        }
                        if (shape.isItalic) {
                            textSkewX = -0.25f
                        }
                    }

                    val textBounds = android.graphics.Rect()
                    paint.getTextBounds(shape.text, 0, shape.text.length, textBounds)
                    val textY = scaledHeight / 2 + (textBounds.height() / 2)

                    canvas.nativeCanvas.drawText(
                        shape.text,
                        scaledWidth / 2,
                        textY,
                        paint
                    )
                }
            }

            // ТОЛЬКО ТЕКСТ, без фона и рамки
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = shape.strokeColor.toArgb()
                    textSize = shape.fontSize * scaleFactor
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    if (shape.isBold) {
                        isFakeBoldText = true
                    }
                    if (shape.isItalic) {
                        textSkewX = -0.25f
                    }
                }

                val textBounds = android.graphics.Rect()
                paint.getTextBounds(shape.text, 0, shape.text.length, textBounds)
                val textY = scaledHeight / 2 + (textBounds.height() / 2)

                canvas.nativeCanvas.drawText(
                    shape.text,
                    scaledWidth / 2,
                    textY,
                    paint
                )
            }
        }
    }
}

private fun Color.isBlack(): Boolean {
    return this == Color.Black || (this.red < 0.15f && this.green < 0.15f && this.blue < 0.15f)
}

/**
 * Рисует маркер выделения (синяя обводка по контуру фигуры)
 */
private fun DrawScope.drawSelectionMarker(
    shape: ComposeShape,
    scaledWidth: Float,
    scaledHeight: Float,
    scaleFactor: Float
) {
    when (shape) {
        is ComposeLine -> {
            val scaledStartX = shape.startX * scaleFactor
            val scaledStartY = shape.startY * scaleFactor
            val scaledEndX = shape.endX * scaleFactor
            val scaledEndY = shape.endY * scaleFactor

            // Синяя обводка вокруг линии
            drawLine(
                color = Color.Cyan.copy(alpha = 0.8f),
                start = Offset(scaledStartX, scaledStartY),
                end = Offset(scaledEndX, scaledEndY),
                strokeWidth = 3f * scaleFactor,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        }

        is ComposeEllipse -> {
            // Для эллипса - рисуем овальную обводку по контуру
            if (shape.fillColor != Color.Transparent) {
                // Рисуем полупрозрачную заливку для лучшей видимости выделения
                drawOval(
                    color = Color.Cyan.copy(alpha = 0.2f),
                    topLeft = Offset.Zero,
                    size = Size(scaledWidth, scaledHeight)
                )
            }

            // Рисуем синий контур по краю эллипса
            drawOval(
                color = Color.Cyan.copy(alpha = 0.8f),
                topLeft = Offset.Zero,
                size = Size(scaledWidth, scaledHeight),
                style = Stroke(width = 2f * scaleFactor)
            )
        }

        is ComposeRhombus -> {
            // Для ромба - рисуем контур в форме песочных часов
            val path = androidx.compose.ui.graphics.Path().apply {
                val centerX = scaledWidth / 2
                val centerY = scaledHeight / 2

                // Левый треугольник
                moveTo(0f, 0f)
                lineTo(centerX, centerY)
                lineTo(0f, scaledHeight)
                close()

                // Правый треугольник
                moveTo(scaledWidth, 0f)
                lineTo(centerX, centerY)
                lineTo(scaledWidth, scaledHeight)
                close()
            }

            // Рисуем полупрозрачную заливку
            if (shape.fillColor != Color.Transparent) {
                drawPath(
                    path = path,
                    color = Color.Cyan.copy(alpha = 0.2f)
                )
            }

            // Рисуем синий контур
            drawPath(
                path = path,
                color = Color.Cyan.copy(alpha = 0.8f),
                style = Stroke(width = 2f * scaleFactor)
            )
        }

        else -> {
            // Для остальных фигур (прямоугольник, текст) - синяя рамка
            drawRect(
                color = Color.Cyan.copy(alpha = 0.8f),
                topLeft = Offset.Zero,
                size = Size(scaledWidth, scaledHeight),
                style = Stroke(width = 2f * scaleFactor)
            )
        }
    }
}