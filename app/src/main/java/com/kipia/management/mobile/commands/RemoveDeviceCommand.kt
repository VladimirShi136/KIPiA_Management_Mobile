package com.kipia.management.mobile.commands

import androidx.compose.ui.geometry.Offset
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.managers.Command
import com.kipia.management.mobile.managers.DeviceManager

/**
 * Команда для удаления устройства со схемы.
 */
class RemoveDeviceCommand(
    private val deviceManager: DeviceManager,
    private val onStateChange: () -> Unit,
    private val deviceId: Int
) : Command {
    private var removedDevice: SchemeDevice? = null

    /**
     * Выполнить команду
     */
    override fun execute() {
        removedDevice = deviceManager.devices.value.find { it.deviceId == deviceId }
        deviceManager.removeDevice(deviceId)
        onStateChange()
    }

    /**
     * Отменить команду
     */
    override fun undo() {
        removedDevice?.let { device ->
            deviceManager.addDevice(device.deviceId, Offset(device.x, device.y))
            onStateChange()
        }
    }
}