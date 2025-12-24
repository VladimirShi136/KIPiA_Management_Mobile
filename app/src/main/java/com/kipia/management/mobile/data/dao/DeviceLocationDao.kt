package com.kipia.management.mobile.data.dao

import androidx.room.*
import com.kipia.management.mobile.data.entities.DeviceLocation

@Dao
interface DeviceLocationDao {

    @Query("SELECT * FROM device_locations WHERE scheme_id = :schemeId")
    suspend fun getLocationsForScheme(schemeId: Int): List<DeviceLocation>

    @Query("SELECT * FROM device_locations WHERE device_id = :deviceId AND scheme_id = :schemeId")
    suspend fun getLocation(deviceId: Int, schemeId: Int): DeviceLocation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLocation(location: DeviceLocation)

    @Delete
    suspend fun deleteLocation(location: DeviceLocation)

    @Query("DELETE FROM device_locations WHERE scheme_id = :schemeId")
    suspend fun deleteAllLocationsForScheme(schemeId: Int)

    @Query("DELETE FROM device_locations WHERE device_id = :deviceId")
    suspend fun deleteAllLocationsForDevice(deviceId: Int)
}