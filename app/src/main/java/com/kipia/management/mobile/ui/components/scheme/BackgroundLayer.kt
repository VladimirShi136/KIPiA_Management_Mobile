package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.kipia.management.mobile.viewmodel.CanvasState

@Composable
fun BackgroundLayer(
    canvasState: CanvasState,
    modifier: Modifier = Modifier,
    key: Any? = null
) {
    // Правильное использование remember - сохраняем ключ
    remember(key) { key }

    Box(modifier = modifier) {
        // Фоновый цвет
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(canvasState.backgroundColor)
        )

        // Фоновое изображение
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