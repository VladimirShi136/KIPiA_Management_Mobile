package com.kipia.management.mobile.repository

import com.kipia.management.mobile.data.dao.DeviceLocationDao
import com.kipia.management.mobile.data.dao.SchemeDao
import com.kipia.management.mobile.data.entities.Scheme
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchemeRepositoryImpl @Inject constructor(
    private val schemeDao: SchemeDao,
    private val deviceLocationDao: DeviceLocationDao
) : SchemeRepository {

    override fun getAllSchemes(): Flow<List<Scheme>> = schemeDao.getAllSchemes()

    override suspend fun getSchemeById(id: Int): Scheme? = schemeDao.getSchemeById(id)

    override suspend fun insertScheme(scheme: Scheme): Long = 
        schemeDao.insertScheme(scheme.withUpdatedNow())

    override suspend fun updateScheme(scheme: Scheme) = 
        schemeDao.updateScheme(scheme.withUpdatedNow())

    override suspend fun deleteScheme(scheme: Scheme) {
        val now = System.currentTimeMillis()
        schemeDao.softDeleteScheme(scheme.id, now)
        // Каскадное мягкое удаление всех позиций приборов на этой схеме
        deviceLocationDao.softDeleteAllLocationsForScheme(scheme.id, now)
    }

    override suspend fun getSchemeByName(name: String): Scheme? {
        return schemeDao.getSchemeByName(name)
    }

    override suspend fun insertSchemeWithTimestamp(scheme: Scheme): Long = insertScheme(scheme)
    override suspend fun updateSchemeWithTimestamp(scheme: Scheme) = updateScheme(scheme)

    override suspend fun getAllSchemesForExport(): List<Scheme> = schemeDao.getAllSchemesForExport()

    override suspend fun importSchemes(schemes: List<Scheme>) {
        schemes.forEach { schemeDao.insertOrUpdateScheme(it) }
    }

    override suspend fun getMaxUpdatedAt(): Long? = schemeDao.getMaxUpdatedAt()
}