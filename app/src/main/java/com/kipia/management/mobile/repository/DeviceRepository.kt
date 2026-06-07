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

    suspend fun getAllDevicesSync(): List<Device> = deviceDAO.getAllDevicesSync()

    fun getDeviceById(id: Int): Flow<Device?> = deviceDAO.getDeviceById(id)
    suspend fun getDeviceByIdSync(id: Int): Device? = deviceDAO.getDeviceByIdSync(id)
    
    suspend fun insertDevice(device: Device): Long = deviceDAO.insertDevice(device.withUpdatedNow())
    suspend fun updateDevice(device: Device): Int = deviceDAO.updateDevice(device.withUpdatedNow())
    
    // Алиасы для обратной совместимости с ViewModels
    suspend fun insertDeviceWithTimestamp(device: Device): Long = insertDevice(device)
    suspend fun updateDeviceWithTimestamp(device: Device): Int = updateDevice(device)

    // Soft delete
    suspend fun deleteDevice(device: Device): Int {
        deviceDAO.softDeleteDevice(device.id)
        return 1
    }

    suspend fun countDevicesInLocation(location: String): Int = deviceDAO.countDevicesByLocation(location)
    fun getAllLocations(): Flow<List<String>> = deviceDAO.getAllLocations()

    // Для экспорта
    suspend fun getAllDevicesForExport(): List<Device> = deviceDAO.getAllDevicesForExport()

    // Для импорта (merge)
    suspend fun importDevices(devices: List<Device>) {
        devices.forEach { deviceDAO.insertOrUpdateDevice(it) }
    }

    suspend fun getMaxUpdatedAt(): Long? = deviceDAO.getMaxUpdatedAt()
}