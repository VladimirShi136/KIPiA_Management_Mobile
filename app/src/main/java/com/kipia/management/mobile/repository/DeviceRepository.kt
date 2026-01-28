package com.kipia.management.mobile.repository

import com.kipia.management.mobile.data.dao.DeviceDao
import com.kipia.management.mobile.data.entities.Device
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceRepository @Inject constructor(
    private val deviceDAO: DeviceDao
) {
    // ★★★★ ДОБАВЬТЕ МЕТОД ДЛЯ ПРИНУДИТЕЛЬНОГО ОБНОВЛЕНИЯ ★★★★
    private val _refreshTrigger = MutableStateFlow(0)
    fun getAllDevices(): Flow<List<Device>> = combine(
        deviceDAO.getAllDevices(),
        _refreshTrigger
    ) { devices, _ ->
        println("DEBUG Repository getAllDevices: Обновление, устройств: ${devices.size}")
        devices
    }

    fun triggerRefresh() {
        _refreshTrigger.value++
    }

    fun getDeviceById(id: Int): Flow<Device?> = deviceDAO.getDeviceById(id)

    suspend fun getDeviceByIdSync(id: Int): Device? {
        return deviceDAO.getDeviceByIdSync(id)
    }

    suspend fun insertDevice(device: Device): Long {  // ← ИЗМЕНЕНО: возвращаем Long
        return deviceDAO.insertDevice(device)
    }

    suspend fun deleteDevice(device: Device): Int {
        println("DEBUG Repository: Удаление устройства ID: ${device.id}")
        val result = deviceDAO.deleteDevice(device)
        println("DEBUG Repository: Результат удаления: $result строк")

        // ★★★★ ТРИГГЕРИМ ОБНОВЛЕНИЕ ПОСЛЕ УДАЛЕНИЯ ★★★★
        triggerRefresh()

        return result
    }

    suspend fun updateDevice(device: Device): Int {  // ← ИЗМЕНЕНО: возвращаем Int
        return deviceDAO.updateDevice(device)
    }

    // Добавьте этот метод для отчетов
    suspend fun getAllDevicesSync(): List<Device> {
        return deviceDAO.getAllDevicesSync()
    }

    suspend fun getDeviceCount(): Int {
        return deviceDAO.getAllDevicesSync().size
    }

    suspend fun countDevicesInLocation(location: String): Int {
        return deviceDAO.countDevicesByLocation(location)
    }

    // ★★★★ ДОБАВЛЯЕМ: Получение всех уникальных местоположений ★★★★
    fun getAllLocations(): Flow<List<String>> = deviceDAO.getAllLocations()
}