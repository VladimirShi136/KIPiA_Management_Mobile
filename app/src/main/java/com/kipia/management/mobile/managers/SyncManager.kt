package com.kipia.management.mobile.managers

import android.content.Context
import android.net.Uri
import com.kipia.management.mobile.data.dao.DeviceDao
import com.kipia.management.mobile.data.dao.DeviceLocationDao
import com.kipia.management.mobile.data.dao.SchemeDao
import com.kipia.management.mobile.data.database.AppDatabase
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.DeviceLocation
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.domain.usecase.SchemeSyncUseCase
import com.kipia.management.mobile.repository.PreferencesRepository
import com.kipia.management.mobile.ui.shared.NotificationManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import androidx.room.withTransaction

sealed class SyncState {
    object Idle : SyncState()
    data class Loading(val message: String) : SyncState()
    object ExportSuccess : SyncState()
    data class ImportSuccess(val stats: SyncManager.SyncStats) : SyncState()
    data class Error(val message: String) : SyncState()
    data class ConflictsDetected(
        val conflicts: List<SyncManager.ConflictInfo>,
        val initialStats: SyncManager.SyncStats
    ) : SyncState()
    data class GhostDevicesDetected(
        val ghostDevices: List<Device>,
        val initialStats: SyncManager.SyncStats,
        val inputUri: Uri
    ) : SyncState()
}

@Singleton
class SyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val deviceDao: DeviceDao,
    private val schemeDao: SchemeDao,
    private val deviceLocationDao: DeviceLocationDao,
    private val photoManager: PhotoManager,
    private val schemeSyncUseCase: SchemeSyncUseCase,
    private val preferencesRepository: PreferencesRepository,
    private val notificationManager: NotificationManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    companion object {
        private const val DB_NAME = "kipia_management.db"
        private const val PHOTOS_DIR = "device_photos"
        private const val TEMP_DIR = "sync_temp"
    }

    data class ConflictInfo(
        val type: String,
        val key: String,
        val local: Any,
        val remote: Any,
        val base: Any? = null
    )

    enum class ConflictResolution {
        LOCAL, REMOTE, SKIP
    }

    fun resetState() {
        _syncState.value = SyncState.Idle
    }

    fun startExport(outputUri: Uri) {
        // Устанавливаем статус синхронно, чтобы сервис не закрылся сразу
        _syncState.value = SyncState.Loading("Экспорт...")
        scope.launch {
            val result = withContext(Dispatchers.IO) { exportToZip(outputUri) }
            result.fold(
                onSuccess = {
                    preferencesRepository.saveLastExportTimestamp(System.currentTimeMillis())
                    _syncState.value = SyncState.ExportSuccess
                    notificationManager.notifySyncSuccess("Экспорт успешно завершен")
                },
                onFailure = { e ->
                    _syncState.value = SyncState.Error(e.message ?: "Ошибка экспорта")
                    notificationManager.notifySyncError("Ошибка экспорта: ${e.message}")
                }
            )
        }
    }

    fun startImport(inputUri: Uri, importDeleted: Boolean = false) {
        // Устанавливаем статус синхронно
        _syncState.value = SyncState.Loading("Импорт...")
        scope.launch {
            val result = withContext(Dispatchers.IO) { importFromZip(inputUri, importDeleted) }
            result.fold(
                onSuccess = { stats ->
                    when {
                        stats.conflicts.isNotEmpty() -> {
                            _syncState.value = SyncState.ConflictsDetected(stats.conflicts, stats)
                        }
                        stats.ghostDeletedDevices.isNotEmpty() && !importDeleted -> {
                            _syncState.value = SyncState.GhostDevicesDetected(stats.ghostDeletedDevices, stats, inputUri)
                        }
                        else -> {
                            preferencesRepository.saveLastImportTimestamp(System.currentTimeMillis())
                            _syncState.value = SyncState.ImportSuccess(stats)
                            notificationManager.notifySyncSuccess("Импорт успешно завершен")
                        }
                    }
                },
                onFailure = { e ->
                    _syncState.value = SyncState.Error(e.message ?: "Ошибка импорта")
                    notificationManager.notifySyncError("Ошибка импорта: ${e.message}")
                }
            )
        }
    }

    fun resolveGhostDevices(importThem: Boolean) {
        val currentState = _syncState.value
        if (currentState is SyncState.GhostDevicesDetected) {
            if (importThem) {
                startImport(currentState.inputUri, importDeleted = true)
            } else {
                scope.launch {
                    preferencesRepository.saveLastImportTimestamp(System.currentTimeMillis())
                    _syncState.value = SyncState.ImportSuccess(currentState.initialStats)
                    notificationManager.notifySyncSuccess("Импорт завершен (удаленные пропущены)")
                }
            }
        }
    }

    fun resolveConflicts(resolutions: List<ConflictResolution>) {
        val currentState = _syncState.value
        if (currentState is SyncState.ConflictsDetected) {
            _syncState.value = SyncState.Loading("Применение решений...")
            scope.launch {
                val result = withContext(Dispatchers.IO) { 
                    applyConflictResolutionsInternal(currentState.conflicts, resolutions) 
                }
                result.fold(
                    onSuccess = {
                        preferencesRepository.saveLastImportTimestamp(System.currentTimeMillis())
                        _syncState.value = SyncState.ImportSuccess(currentState.initialStats)
                        notificationManager.notifySyncSuccess("Конфликты разрешены, импорт завершен")
                    },
                    onFailure = { e ->
                        _syncState.value = SyncState.Error(e.message ?: "Ошибка разрешения конфликтов")
                        notificationManager.notifySyncError("Ошибка разрешения конфликтов")
                    }
                )
            }
        }
    }

    private suspend fun exportToZip(outputUri: Uri): Result<Unit> {
        try {
            val db = database.openHelper.writableDatabase
            db.query("PRAGMA wal_checkpoint(TRUNCATE)", emptyArray()).use { cursor ->
                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(0)
                    Timber.d("SyncManager: WAL checkpoint status = $status")
                }
            }

            val dbFile = context.getDatabasePath(DB_NAME)
            val photosDir = photoManager.getBasePhotosDir()

            if (!dbFile.exists()) return Result.failure(Exception("БД не найдена"))

            context.contentResolver.openOutputStream(outputUri)?.use { outStream ->
                ZipOutputStream(outStream.buffered()).use { zip ->
                    zip.putNextEntry(ZipEntry(DB_NAME))
                    dbFile.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()

                    if (photosDir.exists()) {
                        addDirToZip(zip, photosDir, PHOTOS_DIR)
                    }
                }
            } ?: return Result.failure(Exception("Ошибка открытия файла"))

            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка экспорта")
            return Result.failure(e)
        }
    }

    private fun addDirToZip(zip: ZipOutputStream, dir: File, zipPath: String) {
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relativePath = "$zipPath/${file.relativeTo(dir).path}".replace("\\", "/")
                zip.putNextEntry(ZipEntry(relativePath))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    private suspend fun importFromZip(inputUri: Uri, importDeleted: Boolean = false): Result<SyncStats> {
        val tempDir = File(context.cacheDir, TEMP_DIR)
        try {
            tempDir.deleteRecursively()
            tempDir.mkdirs()

            context.contentResolver.openInputStream(inputUri)?.use { inStream ->
                ZipInputStream(inStream.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val target = sanitizePath(tempDir, entry.name)
                        if (target != null) {
                            if (entry.isDirectory) target.mkdirs()
                            else {
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { zip.copyTo(it) }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return Result.failure(Exception("Ошибка открытия архива"))

            val importedDb = File(tempDir, DB_NAME)
            val importedPhotos = File(tempDir, PHOTOS_DIR)

            if (!importedDb.exists()) throw Exception("БД не найдена в архиве")

            val importedData = readAndValidateImportedDatabase(importedDb)
            
            val stats = database.withTransaction {
                performMerge(importedData, importedPhotos, importDeleted)
            }

            updateLastSyncedTimestamps(stats)

            return Result.success(stats)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка импорта")
            return Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun readAndValidateImportedDatabase(dbFile: File): ImportedData {
        val devices = mutableListOf<Device>()
        val schemes = mutableListOf<Scheme>()
        val locations = mutableListOf<DeviceLocation>()

        android.database.sqlite.SQLiteDatabase.openDatabase(
            dbFile.absolutePath, 
            null, 
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
        ).use { db ->
            val requiredTables = listOf("devices", "schemes", "device_locations")
            requiredTables.forEach { table ->
                db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { cursor ->
                    if (!cursor.moveToFirst()) {
                        throw Exception("Файл импорта поврежден (таблица $table не найдена)")
                    }
                }
            }

            db.rawQuery("SELECT * FROM devices", null).use { c ->
                while (c.moveToNext()) {
                    devices.add(parseDevice(c))
                }
            }
            db.rawQuery("SELECT * FROM schemes", null).use { c ->
                while (c.moveToNext()) {
                    schemes.add(parseScheme(c))
                }
            }
            db.rawQuery("SELECT * FROM device_locations", null).use { c ->
                while (c.moveToNext()) {
                    locations.add(parseLocation(c))
                }
            }
        }
        return ImportedData(devices, schemes, locations)
    }

    private fun parseDevice(c: android.database.Cursor) = Device(
        id = c.getInt(c.getColumnIndexOrThrow("id")),
        type = c.getString(c.getColumnIndexOrThrow("type")) ?: "",
        name = c.getString(c.getColumnIndexOrThrow("name")),
        manufacturer = c.getString(c.getColumnIndexOrThrow("manufacturer")),
        inventoryNumber = c.getString(c.getColumnIndexOrThrow("inventory_number")) ?: "",
        year = if (c.isNull(c.getColumnIndexOrThrow("year"))) null else c.getInt(c.getColumnIndexOrThrow("year")),
        measurementLimit = c.getString(c.getColumnIndexOrThrow("measurement_limit")),
        accuracyClass = if (c.isNull(c.getColumnIndexOrThrow("accuracy_class"))) null else c.getDouble(c.getColumnIndexOrThrow("accuracy_class")),
        location = c.getString(c.getColumnIndexOrThrow("location")) ?: "",
        valveNumber = c.getString(c.getColumnIndexOrThrow("valve_number")),
        status = c.getString(c.getColumnIndexOrThrow("status")) ?: "В работе",
        additionalInfo = c.getString(c.getColumnIndexOrThrow("additional_info")),
        photos = (c.getString(c.getColumnIndexOrThrow("photos")) ?: "").split(";").filter { it.isNotBlank() },
        updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at")),
        lastSyncedAt = c.getLong(c.getColumnIndexOrThrow("last_synced_at")),
        deletedAt = c.getLong(c.getColumnIndexOrThrow("deleted_at"))
    )

    private fun parseScheme(c: android.database.Cursor) = Scheme(
        id = c.getInt(c.getColumnIndexOrThrow("id")),
        name = c.getString(c.getColumnIndexOrThrow("name")) ?: "",
        description = c.getString(c.getColumnIndexOrThrow("description")),
        data = c.getString(c.getColumnIndexOrThrow("data")) ?: "{}",
        updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at")),
        lastSyncedAt = c.getLong(c.getColumnIndexOrThrow("last_synced_at")),
        deletedAt = c.getLong(c.getColumnIndexOrThrow("deleted_at"))
    )

    private fun parseLocation(c: android.database.Cursor) = DeviceLocation(
        deviceId = c.getInt(c.getColumnIndexOrThrow("device_id")),
        schemeId = c.getInt(c.getColumnIndexOrThrow("scheme_id")),
        x = c.getDouble(c.getColumnIndexOrThrow("x")),
        y = c.getDouble(c.getColumnIndexOrThrow("y")),
        rotation = c.getDouble(c.getColumnIndexOrThrow("rotation")),
        updatedAt = c.getLong(c.getColumnIndexOrThrow("updated_at")),
        lastSyncedAt = c.getLong(c.getColumnIndexOrThrow("last_synced_at")),
        deletedAt = c.getLong(c.getColumnIndexOrThrow("deleted_at"))
    )

    private fun sanitizePath(destDir: File, entryName: String): File? {
        val target = File(destDir, entryName).canonicalFile
        return if (target.path.startsWith(destDir.canonicalPath)) target else null
    }

    private suspend fun performMerge(importedData: ImportedData, importedPhotosDir: File, importDeleted: Boolean = false): SyncStats {
        val stats = SyncStats()
        val deviceIdMap = mutableMapOf<Int, Int>()
        val schemeIdMap = mutableMapOf<Int, Int>()

        val localDevices = deviceDao.getAllDevicesForExport().associateBy { it.inventoryNumber }
        
        importedData.devices.forEach { imported ->
            val existing = localDevices[imported.inventoryNumber]
            if (existing == null) {
                if (imported.isDeleted()) {
                    if (importDeleted) {
                        val newId = deviceDao.insertDevice(imported.copy(id = 0, lastSyncedAt = 0)).toInt()
                        deviceIdMap[imported.id] = newId
                        stats.devicesAdded++
                        stats.changedDevices.add(imported.copy(id = newId))
                    } else {
                        stats.ghostDeletedDevices.add(imported)
                    }
                } else {
                    val newId = deviceDao.insertDevice(imported.copy(id = 0, lastSyncedAt = 0)).toInt()
                    deviceIdMap[imported.id] = newId
                    stats.devicesAdded++
                    stats.changedDevices.add(imported.copy(id = newId))
                }
            } else {
                deviceIdMap[imported.id] = existing.id
                val remoteChanged = imported.updatedAt > imported.lastSyncedAt
                if (remoteChanged) {
                    syncRemovedPhotos(existing, imported)
                    val toUpdate = imported.copy(id = existing.id)
                    deviceDao.updateDevice(toUpdate)
                    stats.devicesUpdated++
                    stats.changedDevices.add(toUpdate)
                } else {
                    stats.changedDevices.add(existing)
                }
            }
        }

        val localSchemes = schemeDao.getAllSchemesForExport().associateBy { it.name }

        importedData.schemes.forEach { imported ->
            val existing = localSchemes[imported.name]
            if (existing == null) {
                if (!imported.isDeleted()) {
                    val fixed = fixSchemeDataIds(imported, deviceIdMap)
                    val newId = schemeDao.insertScheme(fixed.copy(id = 0, lastSyncedAt = 0)).toInt()
                    schemeIdMap[imported.id] = newId
                    stats.schemesAdded++
                    stats.changedSchemes.add(fixed.copy(id = newId))
                }
            } else {
                schemeIdMap[imported.id] = existing.id
                val remoteChanged = imported.updatedAt > imported.lastSyncedAt
                if (remoteChanged) {
                    val fixed = fixSchemeDataIds(imported, deviceIdMap)
                    val toUpdate = fixed.copy(id = existing.id)
                    schemeDao.updateScheme(toUpdate)
                    stats.schemesUpdated++
                    stats.changedSchemes.add(toUpdate)
                } else {
                    stats.changedSchemes.add(existing)
                }
            }
        }

        importedData.locations.forEach { imported ->
            val localDeviceId = deviceIdMap[imported.deviceId] ?: return@forEach
            val localSchemeId = schemeIdMap[imported.schemeId] ?: return@forEach

            val existing = deviceLocationDao.getAnyLocationSync(localDeviceId, localSchemeId)
            if (existing == null) {
                if (!imported.isDeleted()) {
                    val toInsert = imported.copy(deviceId = localDeviceId, schemeId = localSchemeId, lastSyncedAt = 0)
                    deviceLocationDao.insertOrUpdateLocation(toInsert)
                    stats.locationsAdded++
                    stats.changedLocations.add(toInsert)
                }
            } else {
                val remoteChanged = imported.updatedAt > imported.lastSyncedAt
                if (remoteChanged) {
                    val toUpdate = imported.copy(deviceId = localDeviceId, schemeId = localSchemeId)
                    deviceLocationDao.insertOrUpdateLocation(toUpdate)
                    stats.locationsUpdated++
                    stats.changedLocations.add(toUpdate)
                } else {
                    stats.changedLocations.add(existing)
                }
            }
        }

        stats.photosAdded = mergePhotos(importedPhotosDir)
        updateDevicePhotosAfterImport(stats.changedDevices)
        
        return stats
    }

    private fun fixSchemeDataIds(scheme: Scheme, deviceIdMap: Map<Int, Int>): Scheme {
        val schemeData = scheme.getSchemeData()
        val devicesInJson = schemeData.devices
        
        if (devicesInJson.isEmpty()) return scheme
        
        val updatedDevices = devicesInJson.map { sd ->
            val localId = deviceIdMap[sd.deviceId]
            if (localId != null) sd.copy(deviceId = localId) else sd
        }
        return scheme.setSchemeData(schemeData.copy(devices = updatedDevices))
    }

    private suspend fun updateDevicePhotosAfterImport(devices: List<Device>) {
        devices.forEach { device ->
            val actualPhotos = device.photos.filter { photoName ->
                File(photoManager.getBasePhotosDir(), photoName).exists()
            }
            if (actualPhotos.size != device.photos.size) {
                deviceDao.updateDevice(device.copy(photos = actualPhotos))
            }
        }
    }

    private suspend fun applyConflictResolutionsInternal(
        conflicts: List<ConflictInfo>,
        resolutions: List<ConflictResolution>
    ): Result<Unit> {
        try {
            database.withTransaction {
                val stats = SyncStats()
                conflicts.forEachIndexed { index, conflict ->
                    val resolution = resolutions.getOrNull(index) ?: ConflictResolution.SKIP
                    if (resolution == ConflictResolution.SKIP) return@forEachIndexed

                    when (conflict.type) {
                        "device" -> {
                            val local = conflict.local as Device
                            val remote = conflict.remote as Device
                            if (resolution == ConflictResolution.REMOTE) {
                                val resolved = remote.copy(id = local.id)
                                deviceDao.updateDevice(resolved)
                                stats.changedDevices.add(resolved)
                            } else stats.changedDevices.add(local)
                        }
                        "scheme" -> {
                            val local = conflict.local as Scheme
                            val remote = conflict.remote as Scheme
                            if (resolution == ConflictResolution.REMOTE) {
                                val resolved = remote.copy(id = local.id)
                                schemeDao.updateScheme(resolved)
                                stats.changedSchemes.add(resolved)
                            } else stats.changedSchemes.add(local)
                        }
                    }
                }
                updateLastSyncedTimestamps(stats)
            }
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Ошибка разрешения конфликтов")
            return Result.failure(e)
        }
    }

    private suspend fun updateLastSyncedTimestamps(stats: SyncStats) {
        val now = System.currentTimeMillis()
        stats.changedDevices.forEach { deviceDao.updateDevice(it.copy(lastSyncedAt = now)) }
        stats.changedSchemes.forEach { schemeDao.updateScheme(it.copy(lastSyncedAt = now)) }
        stats.changedLocations.forEach { deviceLocationDao.insertOrUpdateLocation(it.copy(lastSyncedAt = now)) }
    }

    private fun syncRemovedPhotos(local: Device, imported: Device) {
        val removed = local.photos.toSet() - imported.photos.toSet()
        val dir = photoManager.getBasePhotosDir()
        removed.forEach { File(dir, it).delete() }
    }

    private fun mergePhotos(dir: File): Int {
        if (!dir.exists()) return 0
        var count = 0
        val target = photoManager.getBasePhotosDir()
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val dest = File(target, file.relativeTo(dir).path)
                if (!dest.exists()) {
                    dest.parentFile?.mkdirs()
                    file.copyTo(dest)
                    count++
                }
            }
        }
        return count
    }

    private data class ImportedData(val devices: List<Device>, val schemes: List<Scheme>, val locations: List<DeviceLocation>)

    data class SyncStats(
        var devicesAdded: Int = 0, var devicesUpdated: Int = 0,
        var schemesAdded: Int = 0, var schemesUpdated: Int = 0,
        var locationsAdded: Int = 0, var locationsUpdated: Int = 0,
        var photosAdded: Int = 0,
        val changedDevices: MutableList<Device> = mutableListOf(),
        val changedSchemes: MutableList<Scheme> = mutableListOf(),
        val changedLocations: MutableList<DeviceLocation> = mutableListOf(),
        val conflicts: MutableList<ConflictInfo> = mutableListOf(),
        val ghostDeletedDevices: MutableList<Device> = mutableListOf()
    ) {
        fun isEmpty() = devicesAdded == 0 && devicesUpdated == 0 && schemesAdded == 0 &&
                schemesUpdated == 0 && locationsAdded == 0 && locationsUpdated == 0 && photosAdded == 0 && conflicts.isEmpty() && ghostDeletedDevices.isEmpty()

        fun toSummary(): String {
            val sb = StringBuilder()
            if (devicesAdded > 0) sb.append("Приборов добавлено: $devicesAdded\n")
            if (devicesUpdated > 0) sb.append("Приборов обновлено: $devicesUpdated\n")
            if (schemesAdded > 0) sb.append("Схем добавлено: $schemesAdded\n")
            if (schemesUpdated > 0) sb.append("Схем обновлено: $schemesUpdated\n")
            if (locationsAdded > 0) sb.append("Локаций добавлено: $locationsAdded\n")
            if (locationsUpdated > 0) sb.append("Локаций обновлено: $locationsUpdated\n")
            if (photosAdded > 0) sb.append("Фотографий добавлено: $photosAdded\n")
            if (ghostDeletedDevices.isNotEmpty()) sb.append("Обнаружено удаленных приборов: ${ghostDeletedDevices.size}\n")
            if (conflicts.isNotEmpty()) sb.append("Обнаружено конфликтов: ${conflicts.size}\n")
            
            return if (sb.isEmpty()) "Изменений не обнаружено." else sb.toString().trim()
        }
    }
}
