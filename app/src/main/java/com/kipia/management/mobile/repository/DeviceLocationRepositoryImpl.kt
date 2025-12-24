package com.kipia.management.mobile.repository

import com.kipia.management.mobile.data.dao.DeviceLocationDao
import com.kipia.management.mobile.data.entities.DeviceLocation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceLocationRepositoryImpl @Inject constructor(
    private val deviceLocationDao: DeviceLocationDao
) : DeviceLocationRepository {

    override suspend fun getLocationsForScheme(schemeId: Int): List<DeviceLocation> =
        deviceLocationDao.getLocationsForScheme(schemeId)

    override suspend fun getLocation(deviceId: Int, schemeId: Int): DeviceLocation? =
        deviceLocationDao.getLocation(deviceId, schemeId)

    override suspend fun saveLocation(location: DeviceLocation) =
        deviceLocationDao.insertOrUpdateLocation(location)

    override suspend fun deleteLocation(location: DeviceLocation) =
        deviceLocationDao.deleteLocation(location)

    override suspend fun deleteAllLocationsForScheme(schemeId: Int) =
        deviceLocationDao.deleteAllLocationsForScheme(schemeId)

    override suspend fun deleteAllLocationsForDevice(deviceId: Int) =
        deviceLocationDao.deleteAllLocationsForDevice(deviceId)
}