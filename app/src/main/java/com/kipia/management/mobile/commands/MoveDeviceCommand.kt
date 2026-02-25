package com.kipia.management.mobile.commands

import androidx.compose.ui.geometry.Offset
import com.kipia.management.mobile.managers.Command
import com.kipia.management.mobile.managers.DeviceManager

class MoveDeviceCommand(
    private val deviceManager: DeviceManager,
    private val onStateChange: () -> Unit,
    private val deviceId: Int,
    private val delta: Offset
) : Command {
    private var previousPosition: Pair<Float, Float>? = null

    override fun execute() {
        // Сохраняем позицию ДО перемещения
        previousPosition = deviceManager.devices.value
            .find { it.deviceId == deviceId }
            ?.let { it.x to it.y }

        deviceManager.moveDevice(deviceId, delta)
        onStateChange()
    }

    override fun undo() {
        previousPosition?.let { (x, y) ->
            // Возвращаем на исходную позицию через moveDevice с обратным смещением
            val currentDevice = deviceManager.devices.value.find { it.deviceId == deviceId }
            currentDevice?.let { device ->
                val deltaX = x - device.x
                val deltaY = y - device.y
                if (deltaX != 0f || deltaY != 0f) {
                    deviceManager.moveDevice(deviceId, Offset(deltaX, deltaY))
                    onStateChange()
                }
            }
        }
    }
}