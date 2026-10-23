package com.kipia.management.mobile.managers

import androidx.compose.ui.geometry.Offset
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.SchemeDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceManager @Inject constructor() {
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

    fun rotateDevice(deviceId: Int, angleDeg: Float) {
        _devices.update { list ->
            list.map { device ->
                if (device.deviceId == deviceId) device.copy(rotation = angleDeg) else device
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

    /**
     * Удаляет из схемы приборы, чья текущая локация не совпадает с именем схемы.
     * Вызывается при сохранении схемы, чтобы не оставлять "висячие" приборы.
     */
    fun syncDevicesToScheme(schemeName: String) {
        val schemeNameNormalized = schemeName.trim()
        val toRemove = _devices.value.filter { schemeDevice ->
            val device = _allDevices.value.find { it.id == schemeDevice.deviceId }
            device == null || !device.location.trim().equals(schemeNameNormalized, ignoreCase = true)
        }
        if (toRemove.isNotEmpty()) {
            Timber.d("🧹 DeviceManager.syncDevicesToScheme: удаляю ${toRemove.size} приборов, не принадлежащих схеме '$schemeName'")
            toRemove.forEach { Timber.d("   Удаляю прибор ID=${it.deviceId}") }
            _devices.update { current ->
                current.filter { it.deviceId !in toRemove.map { sd -> sd.deviceId } }
            }
        }
    }
}