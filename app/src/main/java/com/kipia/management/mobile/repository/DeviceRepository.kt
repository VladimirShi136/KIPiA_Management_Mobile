package com.kipia.management.mobile.repository

import com.kipia.management.mobile.data.dao.DeviceDao
import com.kipia.management.mobile.data.entities.Device
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val deviceDAO: DeviceDao
) {
    fun getAllDevices(): Flow<List<Device>> = deviceDAO.getAllDevices()

    // Для синхронных операций оставляем как есть
    suspend fun getAllDevicesSync(): List<Device> = deviceDAO.getAllDevicesSync()

    // Остальные методы без изменений
    fun getDeviceById(id: Int): Flow<Device?> = deviceDAO.getDeviceById(id)
    suspend fun getDeviceByIdSync(id: Int): Device? = deviceDAO.getDeviceByIdSync(id)
    suspend fun insertDevice(device: Device): Long = deviceDAO.insertDevice(device)
    suspend fun updateDevice(device: Device): Int = deviceDAO.updateDevice(device)
    suspend fun deleteDevice(device: Device): Int = deviceDAO.deleteDevice(device)
    suspend fun countDevicesInLocation(location: String): Int = deviceDAO.countDevicesByLocation(location)
    fun getAllLocations(): Flow<List<String>> = deviceDAO.getAllLocations()

    // Обёртка, которая автоматически обновляет timestamp
    suspend fun insertDeviceWithTimestamp(device: Device): Long {
        return deviceDAO.insertDevice(device.withUpdatedNow())
    }

    suspend fun updateDeviceWithTimestamp(device: Device): Int {
        return deviceDAO.updateDevice(device.withUpdatedNow())
    }

    // Для экспорта
    suspend fun getAllDevicesForExport(): List<Device> = deviceDAO.getAllDevicesForExport()

    // Для импорта (merge)
    suspend fun importDevices(devices: List<Device>) {
        deviceDAO.insertOrUpdateDevices(devices)
    }

    suspend fun getMaxUpdatedAt(): Long? = deviceDAO.getMaxUpdatedAt()
}