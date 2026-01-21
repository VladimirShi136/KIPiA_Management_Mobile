package com.kipia.management.mobile.repository

import com.kipia.management.mobile.data.dao.DeviceDao
import com.kipia.management.mobile.data.entities.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val deviceDAO: DeviceDao
) {
    fun getAllDevices(): Flow<List<Device>> = flow {
        try {
            val devices = deviceDAO.getAllDevices().first() // Или .firstOrNull(), если может быть null
            Timber.d("REPOSITORY: getAllDevices() вернул ${devices.size} устройств")
            devices.take(3).forEach { device ->
                Timber.d("  Repo device: ${device.inventoryNumber}, ${device.location}")
            }
            emit(devices) // ✅ emit внутри flow { } — работает!
        } catch (e: Exception) {
            Timber.e(e, "Ошибка получения устройств")
            emit(emptyList()) // ✅ Теперь это внутри flow { } — корректно!
        }
    }


    fun getDeviceById(id: Int): Flow<Device?> = deviceDAO.getDeviceById(id)

    suspend fun getDeviceByIdSync(id: Int): Device? {
        return deviceDAO.getDeviceByIdSync(id)
    }

    suspend fun insertDevice(device: Device) = deviceDAO.insertDevice(device)

    suspend fun deleteDevice(device: Device) = deviceDAO.deleteDevice(device)

    suspend fun updateDevice(device: Device) = deviceDAO.updateDevice(device)

    // Добавьте этот метод для отчетов
    suspend fun getAllDevicesSync(): List<Device> {
        return deviceDAO.getAllDevicesSync()
    }

    // ★★★★ ДОБАВЛЯЕМ: Получение всех уникальных местоположений ★★★★
    fun getAllLocations(): Flow<List<String>> = deviceDAO.getAllLocations()

    suspend fun getAllLocationsSync(): List<String> {
        return deviceDAO.getAllLocationsSync()
    }

    fun getDevicesByType(type: String): Flow<List<Device>> = deviceDAO.getDevicesByType(type)

    fun getDevicesByStatus(status: String): Flow<List<Device>> =
        deviceDAO.getDevicesByStatus(status)

    fun getDeviceTypes(): Flow<List<String>> = deviceDAO.getDeviceTypes()

    fun getDeviceStatuses(): Flow<List<String>> = deviceDAO.getDeviceStatuses()
}