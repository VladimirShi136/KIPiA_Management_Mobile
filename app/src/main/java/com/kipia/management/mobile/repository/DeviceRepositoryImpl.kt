package com.kipia.management.mobile.repository

import com.kipia.management.mobile.data.dao.DeviceDao
import com.kipia.management.mobile.data.entities.Device
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepositoryImpl @Inject constructor(
    private val deviceDao: DeviceDao
) : DeviceRepository {

    override fun getAllDevices(): Flow<List<Device>> = deviceDao.getAllDevices()

    override fun getDevicesByLocation(location: String): Flow<List<Device>> =
        deviceDao.getDevicesByLocation(location)

    override suspend fun getDeviceById(id: Int): Device? = deviceDao.getDeviceById(id)

    override suspend fun insertDevice(device: Device): Long = deviceDao.insertDevice(device)

    override suspend fun updateDevice(device: Device) = deviceDao.updateDevice(device)

    override suspend fun deleteDevice(device: Device) = deviceDao.deleteDevice(device)

    override suspend fun deleteDeviceById(id: Int) = deviceDao.deleteDeviceById(id)

    override fun getAllLocations(): Flow<List<String>> = deviceDao.getAllLocations()

    override suspend fun countDevicesByStatus(status: String): Int =
        deviceDao.countDevicesByStatus(status)

    override suspend fun getDeviceByInventoryNumber(inventoryNumber: String): Device? =
        deviceDao.getDeviceByInventoryNumber(inventoryNumber)

    override suspend fun validateInventoryNumber(inventoryNumber: String, excludeId: Int): Boolean {
        val existing = deviceDao.getDeviceByInventoryNumber(inventoryNumber)
        return existing == null || existing.id == excludeId
    }
}