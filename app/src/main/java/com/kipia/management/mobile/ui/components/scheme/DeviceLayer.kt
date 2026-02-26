package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.utils.drawDevice
import com.kipia.management.mobile.viewmodel.CanvasState
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
fun DeviceLayer(
    devices: List<SchemeDevice>,
    allDevices: List<Device>,
    selectedDeviceId: Int?,
    canvasState: CanvasState,
    onDrawingParams: (scale: Float, offset: Offset) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
    key: Any? = null,
    debugMode: Boolean = true
) {
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

    val device_size = 60f

    val visibleDevices by remember(devices, visibleArea) {
        derivedStateOf {
            devices.filter { schemeDevice ->
                val deviceRight = schemeDevice.x + device_size
                val deviceBottom = schemeDevice.y + device_size

                deviceRight >= visibleArea.left &&
                        schemeDevice.x <= visibleArea.right &&
                        deviceBottom >= visibleArea.top &&
                        schemeDevice.y <= visibleArea.bottom
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
                canvasWidth = size.width
                canvasHeight = size.height
            }
    ) {
        onDrawingParams(stableScale, canvasState.offset)

        Timber.d("Drawing ${visibleDevices.size} devices at scale=$stableScale, offset=${canvasState.offset}")

        visibleDevices.forEach { schemeDevice ->
            deviceMap[schemeDevice.deviceId]?.let { device ->
                val screenX = schemeDevice.x * stableScale + canvasState.offset.x
                val screenY = schemeDevice.y * stableScale + canvasState.offset.y
                val screenSize = device_size * stableScale

                Timber.d("Drawing device ${device.id} at screen($screenX, $screenY)")

                // Просто translate, без scale
                withTransform({
                    translate(screenX, screenY)
                }) {
                    // Передаем scale в drawDevice
                    drawDevice(
                        device = device,
                        isSelected = schemeDevice.deviceId == selectedDeviceId,
                        scale = stableScale  // ← передаем масштаб
                    )
                }

                if (debugMode) {
                    drawRect(
                        color = Color.Green.copy(alpha = 0.5f),
                        topLeft = Offset(screenX, screenY),
                        size = Size(screenSize, screenSize),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )
                }
            }
        }
    }
}