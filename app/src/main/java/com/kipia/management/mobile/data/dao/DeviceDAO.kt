package com.kipia.management.mobile.data.dao

import androidx.room.*
import com.kipia.management.mobile.data.entities.Device
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для взаимодействия с базой данных Room для устройств.
 */
@Dao
interface DeviceDao {
    /**
     * Получение всех устройств, не удаленных.
     */
    @Query("SELECT * FROM devices WHERE deleted_at = 0 ORDER BY inventory_number")
    fun getAllDevices(): Flow<List<Device>>

    /**
     * Получение устройства по его ID.
     */
    @Query("SELECT * FROM devices WHERE id = :id AND deleted_at = 0")
    fun getDeviceById(id: Int): Flow<Device?>

    /**
     * Синхронное получение устройства по его ID.
     */
    @Query("SELECT * FROM devices WHERE id = :id AND deleted_at = 0")
    suspend fun getDeviceByIdSync(id: Int): Device?

    /**
     * Синхронное получение устройства по его инвентарному номеру.
     */
    @Query("SELECT * FROM devices WHERE id = :id")
    suspend fun getAnyDeviceByIdSync(id: Int): Device?

    /**
     * Вставка устройства.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: Device): Long

    /**
     * Обновление устройства.
     */
    @Update
    suspend fun updateDevice(device: Device): Int

    /**
     * Мягкое удаление: помечаем удаленным, обновляем время.
     */
    @Query("UPDATE devices SET deleted_at = :timestamp, updated_at = :timestamp WHERE id = :id")
    suspend fun softDeleteDevice(id: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * Физическое удаление: удаляем из таблицы.
     */
    @Delete
    suspend fun hardDeleteDevice(device: Device): Int

    /**
     * Получение устройств по типу.
     */
    @Query("SELECT * FROM devices WHERE type LIKE :type AND deleted_at = 0 ORDER BY inventory_number")
    fun getDevicesByType(type: String): Flow<List<Device>>

    /**
     * Получение устройств по статусу.
     */
    @Query("SELECT * FROM devices WHERE status = :status AND deleted_at = 0 ORDER BY inventory_number")
    fun getDevicesByStatus(status: String): Flow<List<Device>>

    /**
     * Получение устройств по типу и статусу.
     */
    /**
     * Получение типов устройств.
     */
    @Query("SELECT DISTINCT type FROM devices WHERE deleted_at = 0 AND type != '' ORDER BY type")
    fun getDeviceTypes(): Flow<List<String>>

    /**
     * Получение производителей устройств.
     */
    @Query("SELECT DISTINCT manufacturer FROM devices WHERE deleted_at = 0 AND manufacturer IS NOT NULL AND manufacturer != '' ORDER BY manufacturer")
    fun getDeviceManufacturers(): Flow<List<String>>

    /**
     * Получение названий (моделей) устройств.
     */
    @Query("SELECT DISTINCT name FROM devices WHERE deleted_at = 0 AND name IS NOT NULL AND name != '' ORDER BY name")
    fun getDeviceNames(): Flow<List<String>>

    /**
     * Получение пределов измерений.
     */
    @Query("SELECT DISTINCT measurement_limit FROM devices WHERE deleted_at = 0 AND measurement_limit IS NOT NULL AND measurement_limit != '' ORDER BY measurement_limit")
    fun getDeviceMeasurementLimits(): Flow<List<String>>

    /**
     * Получение номеров кранов.
     */
    @Query("SELECT DISTINCT valve_number FROM devices WHERE deleted_at = 0 AND valve_number IS NOT NULL AND valve_number != '' ORDER BY valve_number")
    fun getDeviceValveNumbers(): Flow<List<String>>

    /**
     * Получение статусов устройств.
     */
    @Query("SELECT DISTINCT status FROM devices WHERE deleted_at = 0 ORDER BY status")
    fun getDeviceStatuses(): Flow<List<String>>

    @Query("SELECT * FROM devices WHERE deleted_at = 0")
    suspend fun getAllDevicesSync(): List<Device>

    @Query("SELECT DISTINCT location FROM devices WHERE location IS NOT NULL AND location != '' AND deleted_at = 0 ORDER BY location")
    fun getAllLocations(): Flow<List<String>>

    /**
     * Синхронное получение устройств по фильтру.
     */
    @Query("SELECT DISTINCT location FROM devices WHERE location IS NOT NULL AND location != '' AND deleted_at = 0 ORDER BY location")
    suspend fun getAllLocationsSync(): List<String>

    @Query("SELECT COUNT(*) FROM devices WHERE location = :location AND deleted_at = 0")
    suspend fun countDevicesByLocation(location: String): Int

    @Query("SELECT * FROM devices")
    suspend fun getAllDevicesForExport(): List<Device>

    /**
     * Синхронное получение устройства по его инвентарному номеру.
     */
    @Query("SELECT * FROM devices WHERE inventory_number = :inventoryNumber")
    suspend fun getDeviceByInventorySync(inventoryNumber: String): Device?

    /**
     * Синхронное получение устройства по его ID.
     */
    @Transaction // Добавляем аннотацию @Transaction для обеспечения атомарности
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

    /**
     * Синхронное получение устройств по фильтру.
     */
    @Query("UPDATE devices SET last_synced_at = :timestamp")
    suspend fun updateAllLastSyncedAt(timestamp: Long)

    /**
     * Синхронное получение устройств по фильтру.
     */
    @Query("SELECT MAX(updated_at) FROM devices")
    suspend fun getMaxUpdatedAt(): Long?
}