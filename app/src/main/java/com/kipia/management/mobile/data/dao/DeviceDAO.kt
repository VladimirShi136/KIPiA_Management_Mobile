package com.kipia.management.mobile.data.dao

import androidx.room.*
import com.kipia.management.mobile.data.entities.Device
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceDao {
    @Query("SELECT * FROM devices WHERE deleted_at = 0 ORDER BY inventory_number")
    fun getAllDevices(): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE id = :id AND deleted_at = 0")
    fun getDeviceById(id: Int): Flow<Device?>

    @Query("SELECT * FROM devices WHERE id = :id AND deleted_at = 0")
    suspend fun getDeviceByIdSync(id: Int): Device?

    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getAnyDeviceByIdSync(id: Int): Device?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device): Long

    @Update
    suspend fun updateDevice(device: Device): Int

    /**
     * Мягкое удаление: помечаем удаленным, обновляем время.
     */
    @Query("UPDATE devices SET deleted_at = :timestamp, updated_at = :timestamp WHERE id = :id")
    suspend fun softDeleteDevice(id: Int, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun hardDeleteDevice(device: Device): Int

    @Query("SELECT * FROM devices WHERE type LIKE :type AND deleted_at = 0 ORDER BY inventory_number")
    fun getDevicesByType(type: String): Flow<List<Device>>

    @Query("SELECT * FROM devices WHERE status = :status AND deleted_at = 0 ORDER BY inventory_number")
    fun getDevicesByStatus(status: String): Flow<List<Device>>

    @Query("SELECT DISTINCT type FROM devices WHERE deleted_at = 0 ORDER BY type")
    fun getDeviceTypes(): Flow<List<String>>

    @Query("SELECT DISTINCT status FROM devices WHERE deleted_at = 0 ORDER BY status")
    fun getDeviceStatuses(): Flow<List<String>>

    @Query("SELECT * FROM devices WHERE deleted_at = 0")
    suspend fun getAllDevicesSync(): List<Device>

    @Query("SELECT DISTINCT location FROM devices WHERE location IS NOT NULL AND location != '' AND deleted_at = 0 ORDER BY location")
    fun getAllLocations(): Flow<List<String>>

    @Query("SELECT DISTINCT location FROM devices WHERE location IS NOT NULL AND location != '' AND deleted_at = 0 ORDER BY location")
    suspend fun getAllLocationsSync(): List<String>

    @Query("SELECT COUNT(*) FROM devices WHERE location = :location AND deleted_at = 0")
    suspend fun countDevicesByLocation(location: String): Int

    @Query("SELECT * FROM devices")
    suspend fun getAllDevicesForExport(): List<Device>

    @Query("SELECT * FROM devices WHERE inventory_number = :inventoryNumber")
    suspend fun getDeviceByInventorySync(inventoryNumber: String): Device?

    @Transaction
    suspend fun insertOrUpdateDevice(device: Device) {
        val existingDevice = getDeviceByInventorySync(device.inventoryNumber)
        if (existingDevice == null) {
            insertDevice(device)
        } else {
            if (device.updatedAt > existingDevice.updatedAt) {
                updateDevice(device.copy(id = existingDevice.id))
            }
        }
    }

    @Query("UPDATE devices SET last_synced_at = :timestamp")
    suspend fun updateAllLastSyncedAt(timestamp: Long)

    @Query("SELECT MAX(updated_at) FROM devices")
    suspend fun getMaxUpdatedAt(): Long?
}