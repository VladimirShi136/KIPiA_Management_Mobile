package com.kipia.management.mobile.data.dao

import androidx.room.*
import com.kipia.management.mobile.data.entities.Scheme
import kotlinx.coroutines.flow.Flow

@Dao
interface SchemeDao {

    @Query("SELECT * FROM schemes ORDER BY name")
    fun getAllSchemes(): Flow<List<Scheme>>

    @Query("SELECT * FROM schemes WHERE id = :id")
    suspend fun getSchemeById(id: Int): Scheme?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheme(scheme: Scheme): Long

    @Update
    suspend fun updateScheme(scheme: Scheme)

    @Delete
    suspend fun deleteScheme(scheme: Scheme)

    @Query("DELETE FROM schemes WHERE id = :id")
    suspend fun deleteSchemeById(id: Int)

    @Query("SELECT * FROM schemes WHERE name = :name")
    suspend fun getSchemeByName(name: String): Scheme?

    @Query("SELECT * FROM schemes")
    suspend fun getAllSchemesSync(): List<Scheme>
}