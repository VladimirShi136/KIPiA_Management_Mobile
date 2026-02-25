package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.shapes.ComposeShape
import com.kipia.management.mobile.viewmodel.CanvasState
import com.kipia.management.mobile.viewmodel.EditorState
import timber.log.Timber
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SchemeCanvas(
    editorState: EditorState,
    canvasState: CanvasState,
    shapes: List<ComposeShape>,
    devices: List<SchemeDevice>,
    allDevices: List<Device>,
    availableDevices: List<Device>,
    onShapeClick: (String) -> Unit,
    onDeviceClick: (Int) -> Unit,
    onCanvasClick: (Offset) -> Unit,
    onShapeDrag: (String, Offset) -> Unit,
    onDeviceDrag: (Int, Offset) -> Unit,
    onTransform: (Float, Offset, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    // Стабилизируем масштаб для фоновых элементов
    val stableScale by remember(canvasState.scale) {
        derivedStateOf {
            (canvasState.scale / 0.05).roundToInt() * 0.05f
        }
    }

    // Ключи для каждого слоя
    val backgroundKey = remember(canvasState.backgroundColor, canvasState.width, canvasState.height, canvasState.backgroundImage, stableScale) {
        "bg_${canvasState.backgroundColor}_${canvasState.width}x${canvasState.height}_$stableScale"
    }

    val shapeKey = remember(shapes, editorState.selection.selectedShapeId) {
        "shapes_${shapes.size}_${editorState.selection.selectedShapeId}"
    }

    val deviceKey = remember(devices, availableDevices, editorState.selection.selectedDeviceId) {
        "devices_${devices.size}_${editorState.selection.selectedDeviceId}"
    }

    val gestureKey = remember(editorState.uiState.mode, editorState.selection) {
        "gesture_${editorState.uiState.mode}_${editorState.selection.selectedShapeId}_${editorState.selection.selectedDeviceId}"
    }

    val indicatorKey = remember(editorState.uiState.mode, canvasState.scale) {
        "indicator_${editorState.uiState.mode}_${(canvasState.scale * 100).toInt()}"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
    ) {
        // Фон (самый нижний слой)
        BackgroundLayer(
            canvasState = canvasState,
            modifier = Modifier.matchParentSize(),
            key = backgroundKey
        )

        // Фигуры (второй слой)
        ShapeLayer(
            shapes = shapes,
            canvasState = canvasState,
            editorState = editorState,
            modifier = Modifier.matchParentSize(),
            key = shapeKey
        )

        // Устройства (третий слой) - теперь с key!
        DeviceLayer(
            devices = devices,
            allDevices = allDevices,
            selectedDeviceId = editorState.selection.selectedDeviceId,
            canvasState = canvasState,
            modifier = Modifier.matchParentSize(),
            key = deviceKey  // ✅ Добавил использование key
        )

        // Жесты (верхний слой, прозрачный для ввода)
        GestureLayer(
            editorState = editorState,
            canvasState = canvasState,
            shapes = shapes,
            devices = devices,
            onShapeClick = onShapeClick,
            onDeviceClick = onDeviceClick,
            onCanvasClick = onCanvasClick,
            onShapeDrag = onShapeDrag,
            onDeviceDrag = onDeviceDrag,
            onTransform = onTransform,
            modifier = Modifier
                .matchParentSize()
                .pointerInteropFilter { motionEvent ->
                    // Отладочный лог
                    Timber.d("📱 GestureLayer получил событие: ${motionEvent.actionMasked}")
                    false // Возвращаем false, чтобы не блокировать
                },
            key = gestureKey
        )

        // Индикаторы (поверх всего)
        IndicatorLayer(
            canvasState = canvasState,
            editorState = editorState,
            showZoomIndicator = true,
            modifier = Modifier.matchParentSize(),
            key = indicatorKey
        )
    }
}