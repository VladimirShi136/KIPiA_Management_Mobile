package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.translate
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
    devices: List<SchemeDevice>,                    // Устройства на схеме (позиции)
    allDevices: List<Device>,                       // ВСЕ устройства из БД (данные)
    selectedDeviceId: Int?,
    canvasState: CanvasState,
    modifier: Modifier = Modifier,
    key: Any? = null
) {
    remember(key) { key }

    Timber.d("📱 DeviceLayer: devices=${devices.size}, allDevices=${allDevices.size}")

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

    Timber.d("📐 DeviceLayer: visibleArea=$visibleArea, offset=${canvasState.offset}, scale=$stableScale")

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
                    Timber.d("✅ Device ${schemeDevice.deviceId} visible at (${schemeDevice.x}, ${schemeDevice.y})")
                }

                isVisible
            }
        }
    }

    Timber.d("👁️ DeviceLayer: visibleDevices=${visibleDevices.size} из ${devices.size}")

    // Используем allDevices для поиска данных об устройстве, а не availableDevices
    val deviceMap by remember(allDevices) {
        derivedStateOf { allDevices.associateBy { it.id } }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                canvasWidth = size.width
                canvasHeight = size.height
                Timber.d("📏 Canvas size: ${size.width}x${size.height}")
            }
    ) {
        Timber.d("🎨 Drawing ${visibleDevices.size} devices")

        withTransform({
            translate(left = canvasState.offset.x, top = canvasState.offset.y)
            scale(scaleX = stableScale, scaleY = stableScale)
        }) {
            // Рисуем отладочную точку в центре
            drawCircle(
                color = androidx.compose.ui.graphics.Color.Red,
                radius = 10f,
                center = androidx.compose.ui.geometry.Offset(500f, 500f)
            )

            visibleDevices.forEach { schemeDevice ->
                deviceMap[schemeDevice.deviceId]?.let { device ->
                    translate(schemeDevice.x, schemeDevice.y) {
                        drawDevice(
                            device = device,
                            isSelected = schemeDevice.deviceId == selectedDeviceId
                        )
                    }
                } ?: Timber.w("❌ Device ${schemeDevice.deviceId} not found in allDevices. allDevices size=${allDevices.size}")
            }
        }
    }
}