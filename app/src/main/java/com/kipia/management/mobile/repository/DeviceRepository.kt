package com.kipia.management.mobile.repository

import com.kipia.management.mobile.data.entities.Device
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    fun getAllDevices(): Flow<List<Device>>
    fun getDevicesByLocation(location: String): Flow<List<Device>>
    suspend fun getDeviceById(id: Int): Device?
    suspend fun insertDevice(device: Device): Long
    suspend fun updateDevice(device: Device)
    suspend fun deleteDevice(device: Device)
    suspend fun deleteDeviceById(id: Int)
    fun getAllLocations(): Flow<List<String>>
    suspend fun countDevicesByStatus(status: String): Int
    suspend fun getDeviceByInventoryNumber(inventoryNumber: String): Device?
    suspend fun validateInventoryNumber(inventoryNumber: String, excludeId: Int = 0): Boolean
}