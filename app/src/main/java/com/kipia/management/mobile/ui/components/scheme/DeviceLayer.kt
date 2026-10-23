package com.kipia.management.mobile.ui.components.scheme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import com.kipia.management.mobile.R
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.ui.components.scheme.utils.drawDevice
import com.kipia.management.mobile.viewmodel.CanvasState
import timber.log.Timber

/**
 * Геометрия иконки прибора — должна быть идентична константам
 * ICON_BASE_SIZE / TEXT_GAP в DeviceUtils.kt и PIVOT_X / PIVOT_Y
 * в JavaFX-версии (DeviceLocation.java), иначе схемы снова разъедутся
 * между платформами.
 *
 * (x, y) прибора в БД — это ТОЧКА ПРИВЯЗКИ (pivot): граница между
 * иконкой и текстом номера крана над ней, а не левый верхний угол
 * и не центр иконки.
 */
private const val DEVICE_ICON_BASE_SIZE = 45f
private const val DEVICE_TEXT_GAP = 5f

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

    val manometerPainter = painterResource(id = R.drawable.ic_manometer)
    val wrenchPainter = painterResource(id = R.drawable.ic_wrench)

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

    val deviceSize = DEVICE_ICON_BASE_SIZE

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

    val textColor = LocalContentColor.current

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
            deviceMap[schemeDevice.deviceId]?.let { device ->
                // Экранные координаты pivot-точки прибора (той самой,
                // что хранится в БД и синхронизируется с JavaFX).
                val screenX = schemeDevice.x * canvasState.scale + canvasState.offset.x
                val screenY = schemeDevice.y * canvasState.scale + canvasState.offset.y

                // Смещение pivot -> левый верхний угол блока (иконка+текст).
                // ВАЖНО: поворот здесь больше НЕ применяется вручную —
                // это делает drawDevice() через встроенный Compose rotate()
                // вокруг того же pivot. Раньше здесь была ручная sin/cos
                // компенсация, которая накладывалась поверх внутреннего
                // поворота drawDevice() и приводила к двойному повороту:
                // на rotation=0 это давало константный сдвиг по Y ровно
                // на высоту иконки (45 * scale), а на rotation != 0
                // сама точка поворота "плавала" по экрану.
                val pivotX = (DEVICE_ICON_BASE_SIZE * canvasState.scale) / 2f
                val pivotY = DEVICE_TEXT_GAP * canvasState.scale

                val adjustedX = screenX - pivotX
                val adjustedY = screenY - pivotY

                Timber.d(
                    "DEVICE_COORDS [ID=${schemeDevice.deviceId}] DB: x=%.1f y=%.1f rot=%.0f° | screen: x=%.1f y=%.1f | adjusted: x=%.1f y=%.1f | canvas: scale=%.2f offset=(%.1f,%.1f)",
                    schemeDevice.x, schemeDevice.y, schemeDevice.rotation,
                    screenX, screenY,
                    adjustedX, adjustedY,
                    canvasState.scale, canvasState.offset.x, canvasState.offset.y
                )

                withTransform({ translate(adjustedX, adjustedY) }) {
                    drawDevice(
                        painter        = manometerPainter,
                        isSelected     = schemeDevice.deviceId == selectedDeviceId,
                        scale          = canvasState.scale,
                        rotationDeg    = schemeDevice.rotation,
                        valveText      = device.valveNumber,
                        valveTextColor = textColor,
                        deviceStatus   = device.status,
                        wrenchPainter  = wrenchPainter
                    )
                }
            }
        }
    }
}