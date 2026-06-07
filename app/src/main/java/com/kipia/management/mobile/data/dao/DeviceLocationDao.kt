package com.kipia.management.mobile.data.dao

import androidx.room.*
import com.kipia.management.mobile.data.entities.DeviceLocation

@Dao
interface DeviceLocationDao {

    @Query("SELECT * FROM device_locations WHERE scheme_id = :schemeId AND deleted_at = 0")
    suspend fun getLocationsForScheme(schemeId: Int): List<DeviceLocation>

    @Query("SELECT * FROM device_locations WHERE device_id = :deviceId AND scheme_id = :schemeId AND deleted_at = 0")
    suspend fun getLocation(deviceId: Int, schemeId: Int): DeviceLocation?

    @Query("SELECT * FROM device_locations WHERE device_id = :deviceId AND scheme_id = :schemeId")
    suspend fun getAnyLocationSync(deviceId: Int, schemeId: Int): DeviceLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLocation(location: DeviceLocation)

    @Query("UPDATE device_locations SET deleted_at = :timestamp, updated_at = :timestamp WHERE device_id = :deviceId AND scheme_id = :schemeId")
    suspend fun softDeleteLocation(deviceId: Int, schemeId: Int, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun hardDeleteLocation(location: DeviceLocation)

    @Query("UPDATE device_locations SET deleted_at = :timestamp, updated_at = :timestamp WHERE scheme_id = :schemeId")
    suspend fun softDeleteAllLocationsForScheme(schemeId: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE device_locations SET deleted_at = :timestamp, updated_at = :timestamp WHERE device_id = :deviceId")
    suspend fun softDeleteAllLocationsForDevice(deviceId: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE device_locations SET last_synced_at = :timestamp")
    suspend fun updateAllLastSyncedAt(timestamp: Long)

    @Query("SELECT * FROM device_locations")
    suspend fun getAllLocationsForExport(): List<DeviceLocation>
}