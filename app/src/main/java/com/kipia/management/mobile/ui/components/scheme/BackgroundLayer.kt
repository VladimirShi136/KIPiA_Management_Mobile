package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.kipia.management.mobile.viewmodel.CanvasState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged

@Composable
fun BackgroundLayer(
    canvasState: CanvasState,
    modifier: Modifier = Modifier,
    key: Any? = null,
) {
    remember(key) { key }

    var viewportWidth by remember { mutableIntStateOf(0) }
    var viewportHeight by remember { mutableIntStateOf(0) }

    Box(modifier = modifier) {
        // Базовый фон всего экрана - ТЕМНЫЙ (на случай, если холст не закрывает всю область)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )

        // Контейнер для холста с белым фоном
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Белый фон холста (трансформируется вместе с холстом)
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { size ->
                        viewportWidth = size.width
                        viewportHeight = size.height
                    }
            ) {
                // Вычисляем границы холста в экранных координатах
                val leftBound = 0f * canvasState.scale + canvasState.offset.x
                val topBound = 0f * canvasState.scale + canvasState.offset.y
                val rightBound = canvasState.width * canvasState.scale + canvasState.offset.x
                val bottomBound = canvasState.height * canvasState.scale + canvasState.offset.y

                // Рисуем БЕЛЫЙ фон ТОЛЬКО внутри границ холста
                drawRect(
                    color = canvasState.backgroundColor,
                    topLeft = Offset(leftBound, topBound),
                    size = Size(
                        width = (rightBound - leftBound),
                        height = (bottomBound - topBound)
                    )
                )

                // Рисуем сетку (только внутри белого холста)
                if (canvasState.showGrid) {
                    drawGridInsideBounds(
                        canvasState = canvasState,
                        leftBound = leftBound,
                        topBound = topBound,
                        rightBound = rightBound,
                        bottomBound = bottomBound,
                        viewportWidth = viewportWidth,
                        viewportHeight = viewportHeight
                    )
                }

                // Рисуем затемнение ВНЕ границ холста
                if (canvasState.dimOutsideBounds) {
                    drawDimmingOutsideBounds(
                        leftBound = leftBound,
                        topBound = topBound,
                        rightBound = rightBound,
                        bottomBound = bottomBound,
                        viewportWidth = viewportWidth,
                        viewportHeight = viewportHeight
                    )
                }

                // Опционально: рисуем рамку холста (для отладки)
                drawRect(
                    color = Color.Blue.copy(alpha = 0.3f),
                    topLeft = Offset(leftBound, topBound),
                    size = Size(
                        width = (rightBound - leftBound),
                        height = (bottomBound - topBound)
                    ),
                    style = Stroke(width = 2f * canvasState.scale)
                )
            }

            // Фоновое изображение (если есть)
            if (!canvasState.backgroundImage.isNullOrBlank()) {
                AsyncImage(
                    model = canvasState.backgroundImage,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawGridInsideBounds(
    canvasState: CanvasState,
    leftBound: Float,
    topBound: Float,
    rightBound: Float,
    bottomBound: Float,
    viewportWidth: Int,
    viewportHeight: Int
) {
    val gridColor = Color.LightGray.copy(alpha = 0.4f)
    val gridSize = canvasState.gridSize
    val scale = canvasState.scale

    // Вычисляем видимую область пересечения холста с экраном
    val visibleLeft = leftBound.coerceIn(0f, viewportWidth.toFloat())
    val visibleRight = rightBound.coerceIn(0f, viewportWidth.toFloat())
    val visibleTop = topBound.coerceIn(0f, viewportHeight.toFloat())
    val visibleBottom = bottomBound.coerceIn(0f, viewportHeight.toFloat())

    // Если холст не виден, ничего не рисуем
    if (visibleLeft >= visibleRight || visibleTop >= visibleBottom) return

    // Вычисляем границы видимой области в мировых координатах
    val startWorldX = ((visibleLeft - leftBound) / scale).toInt()
    val startWorldY = ((visibleTop - topBound) / scale).toInt()
    val endWorldX = ((visibleRight - leftBound) / scale).toInt()
    val endWorldY = ((visibleBottom - topBound) / scale).toInt()

    // Рисуем вертикальные линии сетки
    var x = (startWorldX / gridSize) * gridSize
    while (x <= endWorldX) {
        val screenX = leftBound + x * scale
        // Проверяем, что линия внутри видимой области
        if (screenX in visibleLeft..visibleRight) {
            drawLine(
                color = gridColor,
                start = Offset(screenX, visibleTop),
                end = Offset(screenX, visibleBottom),
                strokeWidth = 1f * scale
            )
        }
        x += gridSize
    }

    // Рисуем горизонтальные линии сетки
    var y = (startWorldY / gridSize) * gridSize
    while (y <= endWorldY) {
        val screenY = topBound + y * scale
        // Проверяем, что линия внутри видимой области
        if (screenY in visibleTop..visibleBottom) {
            drawLine(
                color = gridColor,
                start = Offset(visibleLeft, screenY),
                end = Offset(visibleRight, screenY),
                strokeWidth = 1f * scale
            )
        }
        y += gridSize
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDimmingOutsideBounds(
    leftBound: Float,
    topBound: Float,
    rightBound: Float,
    bottomBound: Float,
    viewportWidth: Int,
    viewportHeight: Int
) {
    val dimColor = Color.Black.copy(alpha = 0.3f)
    val vpWidth = viewportWidth.toFloat()
    val vpHeight = viewportHeight.toFloat()

    // Затемнение слева от холста
    if (leftBound > 0) {
        drawRect(
            color = dimColor,
            topLeft = Offset(0f, 0f),
            size = Size(leftBound, vpHeight)
        )
    }

    // Затемнение справа от холста
    if (rightBound < vpWidth) {
        drawRect(
            color = dimColor,
            topLeft = Offset(rightBound, 0f),
            size = Size(vpWidth - rightBound, vpHeight)
        )
    }

    // Затемнение сверху от холста (только в области, где есть холст по горизонтали)
    if (topBound > 0) {
        val dimLeft = leftBound.coerceAtLeast(0f)
        val dimRight = rightBound.coerceAtMost(vpWidth)
        if (dimLeft < dimRight) {
            drawRect(
                color = dimColor,
                topLeft = Offset(dimLeft, 0f),
                size = Size(dimRight - dimLeft, topBound)
            )
        }
    }

    // Затемнение снизу от холста (только в области, где есть холст по горизонтали)
    if (bottomBound < vpHeight) {
        val dimLeft = leftBound.coerceAtLeast(0f)
        val dimRight = rightBound.coerceAtMost(vpWidth)
        if (dimLeft < dimRight) {
            drawRect(
                color = dimColor,
                topLeft = Offset(dimLeft, bottomBound),
                size = Size(dimRight - dimLeft, vpHeight - bottomBound)
            )
        }
    }
}