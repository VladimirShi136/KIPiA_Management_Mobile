package com.kipia.management.mobile.data.dao

import androidx.room.*
import com.kipia.management.mobile.data.entities.Device
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices ORDER BY inventory_number")
    fun getAllDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE id = :id")
    fun getDeviceById(id: Int): Flow<Device?>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getDeviceByIdSync(id: Int): Device?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device)

    @Delete
    suspend fun deleteDevice(device: Device)

    @Update
    suspend fun updateDevice(device: Device)

    @Query("SELECT * FROM devices WHERE type LIKE :type ORDER BY inventory_number")
    fun getDevicesByType(type: String): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE status = :status ORDER BY inventory_number")
    fun getDevicesByStatus(status: String): Flow<List<Device>>

    @Query("SELECT DISTINCT type FROM devices ORDER BY type")
    fun getDeviceTypes(): Flow<List<String>>

    @Query("SELECT DISTINCT status FROM devices ORDER BY status")
    fun getDeviceStatuses(): Flow<List<String>>

    @Query("SELECT * FROM devices")
    suspend fun getAllDevicesSync(): List<Device>

    // ★★★★ ДОБАВЛЯЕМ: Получение уникальных местоположений ★★★★
    @Query("SELECT DISTINCT location FROM devices WHERE location IS NOT NULL AND location != '' ORDER BY location")
    fun getAllLocations(): Flow<List<String>>

    @Query("SELECT DISTINCT location FROM devices WHERE location IS NOT NULL AND location != '' ORDER BY location")
    suspend fun getAllLocationsSync(): List<String>
}