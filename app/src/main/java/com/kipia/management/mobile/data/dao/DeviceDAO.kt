package com.kipia.management.mobile.data.dao

import androidx.room.*
import com.kipia.management.mobile.data.entities.Device
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {

    @Query("SELECT * FROM devices ORDER BY location, inventory_number")
    fun getAllDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE location = :location ORDER BY inventory_number")
    fun getDevicesByLocation(location: String): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getDeviceById(id: Int): Device?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device): Long

    @Update
    suspend fun updateDevice(device: Device)

    @Delete
    suspend fun deleteDevice(device: Device)

    @Query("DELETE FROM devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Int)

    @Query("SELECT DISTINCT location FROM devices ORDER BY location")
    fun getAllLocations(): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM devices WHERE status = :status")
    suspend fun countDevicesByStatus(status: String): Int

    @Query("SELECT * FROM devices WHERE inventory_number = :inventoryNumber")
    suspend fun getDeviceByInventoryNumber(inventoryNumber: String): Device?
}