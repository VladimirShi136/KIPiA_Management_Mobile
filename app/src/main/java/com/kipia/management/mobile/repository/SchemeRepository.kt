package com.kipia.management.mobile.repository

import com.kipia.management.mobile.data.entities.Scheme
import kotlinx.coroutines.flow.Flow

interface SchemeRepository {
    fun getAllSchemes(): Flow<List<Scheme>>
    suspend fun getSchemeById(id: Int): Scheme?
    suspend fun insertScheme(scheme: Scheme): Long
    suspend fun updateScheme(scheme: Scheme)
    suspend fun deleteScheme(scheme: Scheme)
    suspend fun deleteSchemeById(id: Int)
}