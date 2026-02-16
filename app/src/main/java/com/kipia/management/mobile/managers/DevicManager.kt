package com.kipia.management.mobile.managers

import androidx.compose.ui.geometry.Offset
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DeviceManager {
    private val _devices = MutableStateFlow<List<SchemeDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<Device>>(emptyList())
    val availableDevices = _availableDevices.asStateFlow()

    fun setAvailableDevices(devices: List<Device>) {
        _availableDevices.value = devices
    }

    fun addDevice(deviceId: Int, position: Offset) {
        _devices.update { current ->
            current + SchemeDevice(
                deviceId = deviceId,
                x = position.x,
                y = position.y,
                zIndex = current.size
            )
        }
    }

    fun updateDevicePosition(deviceId: Int, delta: Offset) {
        _devices.update { devices ->
            devices.map { device ->
                if (device.deviceId == deviceId) {
                    device.copy(
                        x = device.x + delta.x,
                        y = device.y + delta.y
                    )
                } else device
            }
        }
    }

    fun updateDevice(deviceId: Int, update: (SchemeDevice) -> SchemeDevice) {
        _devices.update { devices ->
            devices.map { device ->
                if (device.deviceId == deviceId) update(device) else device
            }
        }
    }

    fun removeDevice(deviceId: Int) {
        _devices.update { it.filter { it.deviceId != deviceId } }
    }

    fun findDeviceAt(point: Offset): SchemeDevice? {
        return _devices.value.reversed().firstOrNull { device ->
            point.x in device.x..(device.x + 80) &&
                    point.y in device.y..(device.y + 80)
        }
    }
}