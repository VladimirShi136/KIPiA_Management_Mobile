package com.kipia.management.mobile.data.dao

import androidx.room.*
import com.kipia.management.mobile.data.entities.Scheme
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс для взаимодействия с базой данных Room для схем.
 */
@Dao
interface SchemeDao {

    /**
     * Получение всех схем, не удаленных.
     */
    @Query("SELECT * FROM schemes WHERE deleted_at = 0 ORDER BY name")
    fun getAllSchemes(): Flow<List<Scheme>>

    /**
     * Получение схемы по его ID.
     */
    @Query("SELECT * FROM schemes WHERE id = :id AND deleted_at = 0")
    suspend fun getSchemeById(id: Int): Scheme?

    /**
     * Синхронное получение схемы по его ID.
     */
    @Query("SELECT * FROM schemes WHERE id = :id")
    suspend fun getAnySchemeByIdSync(id: Int): Scheme?

    /**
     * Вставка схемы.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheme(scheme: Scheme): Long

    /**
     * Обновление схемы.
     */
    @Update
    suspend fun updateScheme(scheme: Scheme)

    /**
     * Мягкое удаление: помечаем удаленным, обновляем время.
     */
    @Query("UPDATE schemes SET deleted_at = :timestamp, updated_at = :timestamp WHERE id = :id")
    suspend fun softDeleteScheme(id: Int, timestamp: Long = System.currentTimeMillis())

    /**
     * Физическое удаление: удаляем из таблицы.
     */
    @Delete
    suspend fun hardDeleteScheme(scheme: Scheme)

    /**
     * Получение схемы по имени.
     */
    @Query("SELECT * FROM schemes WHERE name = :name")
    suspend fun getSchemeByName(name: String): Scheme?

    /**
     * Синхронное получение всех схем.
     */
    @Query("SELECT * FROM schemes WHERE deleted_at = 0")
    suspend fun getAllSchemesSync(): List<Scheme>

    @Query("SELECT * FROM schemes")
    suspend fun getAllSchemesForExport(): List<Scheme>

    /**
     * Синхронное получение схемы по фильтру.
     */
    @Transaction // Добавляем аннотацию @Transaction для обеспечения атомарности
    suspend fun insertOrUpdateScheme(scheme: Scheme) {
        val existingScheme = getSchemeByName(scheme.name)
        if (existingScheme == null) {
            insertScheme(scheme)
        } else {
            if (scheme.updatedAt > existingScheme.updatedAt) {
                updateScheme(scheme.copy(id = existingScheme.id))
            }
        }
    }

    /**
     * Синхронное получение устройств по фильтру.
     */
    @Query("UPDATE schemes SET last_synced_at = :timestamp")
    suspend fun updateAllLastSyncedAt(timestamp: Long)

    /**
     * Синхронное получение устройств по фильтру.
     */
    @Query("SELECT MAX(updated_at) FROM schemes")
    suspend fun getMaxUpdatedAt(): Long?
}