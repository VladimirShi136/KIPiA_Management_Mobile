package com.kipia.management.mobile.managers

import androidx.compose.ui.geometry.Offset
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class DeviceManager {
    private val _devices = MutableStateFlow<List<SchemeDevice>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _allDevices = MutableStateFlow<List<Device>>(emptyList())
    val allDevices = _allDevices.asStateFlow()

    fun setAllDevices(devices: List<Device>) {
        _allDevices.value = devices
        Timber.d("📦 DeviceManager: загружено ${devices.size} приборов в allDevices")
        Timber.d("   IDs: ${devices.map { it.id }}")
    }

    fun addDevice(deviceId: Int, position: Offset) {
        Timber.d("📥 DeviceManager.addDevice: ID=$deviceId, позиция=$position")

        _devices.update { current ->
            if (current.none { it.deviceId == deviceId }) {
                val newDevice = SchemeDevice(
                    deviceId = deviceId,
                    x = position.x,
                    y = position.y,
                    zIndex = current.size
                )
                Timber.d("   Добавляем новое устройство: ${newDevice}")
                current + newDevice
            } else {
                Timber.w("   Устройство $deviceId уже существует")
                current
            }
        }

        Timber.d("   После добавления: ${_devices.value.size} устройств")
    }

    fun removeDevice(deviceId: Int) {
        _devices.update { current ->
            current.filter { it.deviceId != deviceId }
        }
    }

    fun moveDevice(deviceId: Int, delta: Offset) {
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

    fun getDevicePosition(deviceId: Int): Offset? {
        return _devices.value.find { it.deviceId == deviceId }?.let {
            Offset(it.x, it.y)
        }
    }

    fun clear() {
        _devices.value = emptyList()
    }
}