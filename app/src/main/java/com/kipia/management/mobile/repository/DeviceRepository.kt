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

    fun getDeviceById(id: Int): Flow<Device?> = deviceDAO.getDeviceById(id)

    suspend fun insertDevice(device: Device) = deviceDAO.insertDevice(device)

    suspend fun deleteDevice(device: Device) = deviceDAO.deleteDevice(device)

    suspend fun updateDevice(device: Device) = deviceDAO.updateDevice(device)

    fun getDevicesByType(type: String): Flow<List<Device>> = deviceDAO.getDevicesByType(type)

    fun getDevicesByStatus(status: String): Flow<List<Device>> = deviceDAO.getDevicesByStatus(status)

    fun getDeviceTypes(): Flow<List<String>> = deviceDAO.getDeviceTypes()

    fun getDeviceStatuses(): Flow<List<String>> = deviceDAO.getDeviceStatuses()
}