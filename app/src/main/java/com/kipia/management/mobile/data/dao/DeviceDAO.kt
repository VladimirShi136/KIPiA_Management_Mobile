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
}