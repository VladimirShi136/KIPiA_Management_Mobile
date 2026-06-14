package com.kipia.management.mobile.data.dao

import androidx.room.*
import com.kipia.management.mobile.data.entities.DeviceLocation

/**
 * Интерфейс для взаимодействия с базой данных Room для локаций устройств.
 */
@Dao
interface DeviceLocationDao {

    /**
     * Получение всех локаций устройств.
     */
    @Query("SELECT * FROM device_locations WHERE scheme_id = :schemeId AND deleted_at = 0")
    suspend fun getLocationsForScheme(schemeId: Int): List<DeviceLocation>

    /**
     * Получение локации устройства по его ID.
     */
    @Query("SELECT * FROM device_locations WHERE device_id = :deviceId AND scheme_id = :schemeId AND deleted_at = 0")
    suspend fun getLocation(deviceId: Int, schemeId: Int): DeviceLocation?

    /**
     * Вставка локации устройства.
     */
    @Query("SELECT * FROM device_locations WHERE device_id = :deviceId AND scheme_id = :schemeId")
    suspend fun getAnyLocationSync(deviceId: Int, schemeId: Int): DeviceLocation?

    /**
     * Вставка локации устройства.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLocation(location: DeviceLocation)

    /**
     * Мягкое удаление: помечаем удаленным, обновляем время.
     */
    @Query("UPDATE device_locations SET deleted_at = :timestamp, updated_at = :timestamp WHERE device_id = :deviceId AND scheme_id = :schemeId")
    suspend fun softDeleteLocation(deviceId: Int, schemeId: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * Физическое удаление: удаляем из таблицы.
     */
    @Delete
    suspend fun hardDeleteLocation(location: DeviceLocation)

    /**
     * Мягкое удаление: помечаем удаленным, обновляем время.
     */
    @Query("UPDATE device_locations SET deleted_at = :timestamp, updated_at = :timestamp WHERE scheme_id = :schemeId")
    suspend fun softDeleteAllLocationsForScheme(schemeId: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * Мягкое удаление: помечаем удаленным, обновляем время.
     */
    @Query("UPDATE device_locations SET deleted_at = :timestamp, updated_at = :timestamp WHERE device_id = :deviceId")
    suspend fun softDeleteAllLocationsForDevice(deviceId: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * Синхронное получение устройств по фильтру.
     */
    @Query("UPDATE device_locations SET last_synced_at = :timestamp")
    suspend fun updateAllLastSyncedAt(timestamp: Long)

    /**
     * Синхронное получение устройств по фильтру.
     */
    @Query("SELECT * FROM device_locations")
    suspend fun getAllLocationsForExport(): List<DeviceLocation>
}