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
    suspend fun insertDevice(device: Device): Long = deviceDAO.insertDevice(device)
    suspend fun updateDevice(device: Device): Int = deviceDAO.updateDevice(device)
    suspend fun deleteDevice(device: Device): Int = deviceDAO.deleteDevice(device)
    suspend fun countDevicesInLocation(location: String): Int = deviceDAO.countDevicesByLocation(location)
    fun getAllLocations(): Flow<List<String>> = deviceDAO.getAllLocations()
}