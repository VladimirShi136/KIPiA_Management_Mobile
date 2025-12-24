package com.kipia.management.mobile.repository

import com.kipia.management.mobile.data.entities.DeviceLocation

interface DeviceLocationRepository {
    suspend fun getLocationsForScheme(schemeId: Int): List<DeviceLocation>
    suspend fun getLocation(deviceId: Int, schemeId: Int): DeviceLocation?
    suspend fun saveLocation(location: DeviceLocation)
    suspend fun deleteLocation(location: DeviceLocation)
    suspend fun deleteAllLocationsForScheme(schemeId: Int)
    suspend fun deleteAllLocationsForDevice(deviceId: Int)
}