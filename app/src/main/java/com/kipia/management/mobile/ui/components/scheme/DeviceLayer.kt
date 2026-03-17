package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.utils.drawDevice
import com.kipia.management.mobile.viewmodel.CanvasState

@Composable
fun DeviceLayer(
    devices: List<SchemeDevice>,
    allDevices: List<Device>,
    selectedDeviceId: Int?,
    canvasState: CanvasState,
    onDrawingParams: (scale: Float, offset: Offset) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    key: Any? = null
) {
    remember(key) { key }

    var canvasWidth  by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }

    val visibleArea by remember(canvasState, canvasWidth, canvasHeight) {
        derivedStateOf {
            if (canvasWidth == 0 || canvasHeight == 0) return@derivedStateOf Rect.Zero
            Rect(
                left   = -canvasState.offset.x / canvasState.scale,
                top    = -canvasState.offset.y / canvasState.scale,
                right  = (-canvasState.offset.x + canvasWidth) / canvasState.scale,
                bottom = (-canvasState.offset.y + canvasHeight) / canvasState.scale
            )
        }
    }

    // Базовый размер иконки в схемных единицах (совпадает с ICON_BASE_SIZE в DeviceUtils)
    val deviceSize = 45f

    val visibleDevices by remember(devices, visibleArea) {
        derivedStateOf {
            devices.filter { sd ->
                sd.x + deviceSize >= visibleArea.left &&
                sd.x             <= visibleArea.right &&
                sd.y + deviceSize >= visibleArea.top  &&
                sd.y             <= visibleArea.bottom
            }
        }
    }

    val deviceMap by remember(allDevices) {
        derivedStateOf { allDevices.associateBy { it.id } }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                canvasWidth  = size.width
                canvasHeight = size.height
            }
    ) {
        onDrawingParams(canvasState.scale, canvasState.offset)

        visibleDevices.forEach { schemeDevice ->
            deviceMap[schemeDevice.deviceId]?.let { _ ->
                val screenX = schemeDevice.x * canvasState.scale + canvasState.offset.x
                val screenY = schemeDevice.y * canvasState.scale + canvasState.offset.y

                withTransform({ translate(screenX, screenY) }) {
                    drawDevice(
                        isSelected  = schemeDevice.deviceId == selectedDeviceId,
                        scale       = canvasState.scale,
                        // SchemeDevice.rotation хранит угол в градусах (0 / 90 / 180 / 270)
                        rotationDeg = schemeDevice.rotation
                    )
                }
            }
        }
    }
}
