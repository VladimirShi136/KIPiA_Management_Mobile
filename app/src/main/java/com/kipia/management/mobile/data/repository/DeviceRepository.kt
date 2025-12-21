package com.kipia.management.mobile.data.repository

import com.kipia.management.data.dao.DeviceDao
import com.kipia.management.data.entities.Device
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DeviceRepository @Inject constructor(
    private val deviceDao: DeviceDao
) {
    fun getAllDevices(): Flow<List<Device>> = deviceDao.getAllDevices()

    fun getDevicesByLocation(location: String): Flow<List<Device>> =
        deviceDao.getDevicesByLocation(location)

    fun getAllLocations(): Flow<List<String>> = deviceDao.getAllLocations()

    suspend fun getDeviceById(deviceId: Int): Device? =
        deviceDao.getDeviceById(deviceId)

    suspend fun insertDevice(device: Device): Long =
        deviceDao.insertDevice(device)

    suspend fun updateDevice(device: Device) =
        deviceDao.updateDevice(device)

    suspend fun deleteDevice(device: Device) =
        deviceDao.deleteDevice(device)

    suspend fun getDeviceByInventoryNumber(inventoryNumber: String): Device? =
        deviceDao.getDeviceByInventoryNumber(inventoryNumber)
}