package com.kipia.management.mobile.commands

import androidx.compose.ui.geometry.Offset
import com.kipia.management.mobile.managers.Command
import com.kipia.management.mobile.data.entities.SchemeDevice
import com.kipia.management.mobile.managers.DeviceManager
import timber.log.Timber

class AddDeviceCommand(
    private val deviceManager: DeviceManager,
    private val onStateChange: () -> Unit,
    private val deviceId: Int,
    private val position: Offset
) : Command {
    private var addedDevice: SchemeDevice? = null

    override fun execute() {
        Timber.d("➕ AddDeviceCommand.execute: ID=$deviceId, позиция=$position")

        addedDevice = SchemeDevice(
            deviceId = deviceId,
            x = position.x,
            y = position.y,
            zIndex = deviceManager.devices.value.size
        )

        Timber.d("   Текущее кол-во до добавления: ${deviceManager.devices.value.size}")
        deviceManager.addDevice(deviceId, position)
        Timber.d("   Кол-во после добавления: ${deviceManager.devices.value.size}")

        onStateChange()
    }

    override fun undo() {
        Timber.d("➖ AddDeviceCommand.undo: ID=$deviceId")
        addedDevice?.let {
            deviceManager.removeDevice(it.deviceId)
            Timber.d("   Устройство удалено")
        }
        onStateChange()
    }
}