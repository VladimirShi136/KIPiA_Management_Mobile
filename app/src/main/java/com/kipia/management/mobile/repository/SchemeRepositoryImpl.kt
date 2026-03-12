package com.kipia.management.mobile.repository

import com.kipia.management.mobile.data.dao.SchemeDao
import com.kipia.management.mobile.data.entities.Scheme
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchemeRepositoryImpl @Inject constructor(
    private val schemeDao: SchemeDao
) : SchemeRepository {

    override fun getAllSchemes(): Flow<List<Scheme>> = schemeDao.getAllSchemes()

    override suspend fun getSchemeById(id: Int): Scheme? = schemeDao.getSchemeById(id)

    override suspend fun insertScheme(scheme: Scheme): Long = schemeDao.insertScheme(scheme)

    override suspend fun updateScheme(scheme: Scheme) = schemeDao.updateScheme(scheme)

    override suspend fun deleteScheme(scheme: Scheme) = schemeDao.deleteScheme(scheme)

    override suspend fun deleteSchemeById(id: Int) = schemeDao.deleteSchemeById(id)

    override suspend fun getSchemeByName(name: String): Scheme? {
        return schemeDao.getSchemeByName(name)
    }

    // Обёртки с авто-обновлением timestamp
    override suspend fun insertSchemeWithTimestamp(scheme: Scheme): Long {
        return schemeDao.insertScheme(scheme.withUpdatedNow())
    }

    override suspend fun updateSchemeWithTimestamp(scheme: Scheme) {
        schemeDao.updateScheme(scheme.withUpdatedNow())
    }

    // Для экспорта
    override suspend fun getAllSchemesForExport(): List<Scheme> = schemeDao.getAllSchemesForExport()

    // Для импорта
    override suspend fun importSchemes(schemes: List<Scheme>) {
        schemeDao.insertOrUpdateSchemes(schemes)
    }

    override suspend fun getMaxUpdatedAt(): Long? = schemeDao.getMaxUpdatedAt()
}