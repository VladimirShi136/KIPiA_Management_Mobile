package com.kipia.management.mobile.data.dao

import androidx.room.*
import com.kipia.management.mobile.data.entities.Scheme
import kotlinx.coroutines.flow.Flow

@Dao
interface SchemeDao {

    @Query("SELECT * FROM schemes WHERE deleted_at = 0 ORDER BY name")
    fun getAllSchemes(): Flow<List<Scheme>>

    @Query("SELECT * FROM schemes WHERE id = :id AND deleted_at = 0")
    suspend fun getSchemeById(id: Int): Scheme?

    @Query("SELECT * FROM schemes WHERE id = :id")
    suspend fun getAnySchemeByIdSync(id: Int): Scheme?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheme(scheme: Scheme): Long

    @Update
    suspend fun updateScheme(scheme: Scheme)

    @Query("UPDATE schemes SET deleted_at = :timestamp, updated_at = :timestamp WHERE id = :id")
    suspend fun softDeleteScheme(id: Int, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun hardDeleteScheme(scheme: Scheme)

    @Query("SELECT * FROM schemes WHERE name = :name")
    suspend fun getSchemeByName(name: String): Scheme?

    @Query("SELECT * FROM schemes WHERE deleted_at = 0")
    suspend fun getAllSchemesSync(): List<Scheme>

    @Query("SELECT * FROM schemes")
    suspend fun getAllSchemesForExport(): List<Scheme>

    @Transaction
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

    @Query("UPDATE schemes SET last_synced_at = :timestamp")
    suspend fun updateAllLastSyncedAt(timestamp: Long)

    @Query("SELECT MAX(updated_at) FROM schemes")
    suspend fun getMaxUpdatedAt(): Long?
}