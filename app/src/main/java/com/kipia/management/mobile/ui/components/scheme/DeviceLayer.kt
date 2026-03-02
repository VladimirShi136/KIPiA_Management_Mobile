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
import timber.log.Timber

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

    // Используем точный масштаб для всех расчетов!
    val visibleArea by remember(canvasState, canvasWidth, canvasHeight) {
        derivedStateOf {
            if (canvasWidth == 0 || canvasHeight == 0) return@derivedStateOf Rect.Zero

            Rect(
                left = -canvasState.offset.x / canvasState.scale,
                top = -canvasState.offset.y / canvasState.scale,
                right = (-canvasState.offset.x + canvasWidth) / canvasState.scale,
                bottom = (-canvasState.offset.y + canvasHeight) / canvasState.scale
            ).also {
                Timber.d("🔍 DeviceLayer visibleArea: $it, scale=${canvasState.scale}")
            }
        }
    }

    val device_size = 60f

    val visibleDevices by remember(devices, visibleArea) {
        derivedStateOf {
            devices.filter { schemeDevice ->
                val deviceRight = schemeDevice.x + device_size
                val deviceBottom = schemeDevice.y + device_size

                val isVisible = deviceRight >= visibleArea.left &&
                        schemeDevice.x <= visibleArea.right &&
                        deviceBottom >= visibleArea.top &&
                        schemeDevice.y <= visibleArea.bottom

                if (isVisible) {
                    Timber.d("   Device ${schemeDevice.deviceId} visible at (${schemeDevice.x}, ${schemeDevice.y})")
                }
                isVisible
            }
        }
    }

    val deviceMap by remember(allDevices) {
        derivedStateOf { allDevices.associateBy { it.id } }
    }

    LaunchedEffect(selectedDeviceId) {
        Timber.d("🔴🔴🔴 DeviceLayer: selectedDeviceId = $selectedDeviceId")
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                canvasWidth = size.width
                canvasHeight = size.height
                Timber.d("📐 DeviceLayer canvas size: $canvasWidth x $canvasHeight")
            }
    ) {
        onDrawingParams(canvasState.scale, canvasState.offset)

        Timber.d("Drawing ${visibleDevices.size} devices at scale=${canvasState.scale}, offset=${canvasState.offset}")

        visibleDevices.forEach { schemeDevice ->
            deviceMap[schemeDevice.deviceId]?.let { device ->
                val screenX = schemeDevice.x * canvasState.scale + canvasState.offset.x
                val screenY = schemeDevice.y * canvasState.scale + canvasState.offset.y
                val screenSize = device_size * canvasState.scale

                val isSelected = schemeDevice.deviceId == selectedDeviceId
                if (isSelected) {
                    Timber.d("✨✨✨ Рисуем ВЫДЕЛЕННЫЙ прибор ${device.id}")
                }

                Timber.d("   Drawing device ${device.id} at screen($screenX, $screenY)")

                withTransform({
                    translate(screenX, screenY)
                }) {
                    drawDevice(
                        device = device,
                        isSelected = schemeDevice.deviceId == selectedDeviceId,
                        scale = canvasState.scale
                    )
                }
            }
        }
    }
}