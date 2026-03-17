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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val deviceDao: DeviceDao,
    private val schemeDao: SchemeDao,
    private val deviceLocationDao: DeviceLocationDao,
    private val photoManager: PhotoManager
) {

    companion object {
        private const val DB_NAME = "kipia_management.db"
        private const val PHOTOS_DIR = "device_photos"
        private const val TEMP_DIR = "sync_temp"
    }

    // ─────────────────────────────────────────────
    // ЭКСПОРТ
    // ─────────────────────────────────────────────

    /**
     * Экспортирует БД + фото в ZIP-файл по указанному Uri.
     * Uri получается через ActivityResultLauncher (ACTION_CREATE_DOCUMENT) в ViewModel.
     */
    suspend fun exportToZip(outputUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // WAL checkpoint — сбрасываем все незафиксированные страницы в основной файл БД
            // database.close() использовать нельзя: Room после этого не переоткрывается
            database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")

            val dbFile = context.getDatabasePath(DB_NAME)
            val photosDir = photoManager.getBasePhotosDir()

            context.contentResolver.openOutputStream(outputUri)?.use { outStream ->
                ZipOutputStream(outStream.buffered()).use { zip ->

                    // 1. Добавляем файл БД
                    if (dbFile.exists()) {
                        zip.putNextEntry(ZipEntry(DB_NAME))
                        dbFile.inputStream().use { it.copyTo(zip) }
                        zip.closeEntry()
                        Timber.d("SyncManager: добавлена БД ${dbFile.length()} байт")
                    }

                    // 2. Добавляем фото (сохраняем структуру папок)
                    if (photosDir.exists()) {
                        addDirToZip(zip, photosDir, PHOTOS_DIR)
                        Timber.d("SyncManager: добавлена папка фото")
                    }
                }
            } ?: return@withContext Result.failure(Exception("Не удалось открыть выходной файл"))

            Timber.d("SyncManager: экспорт завершён")
            Result.success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "SyncManager: ошибка экспорта")
            Result.failure(e)
        }
    }

    private fun addDirToZip(zip: ZipOutputStream, dir: File, zipPath: String) {
        dir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val relativePath = "$zipPath/${file.relativeTo(dir).path}"
                    .replace("\\", "/") // Windows-safe
                zip.putNextEntry(ZipEntry(relativePath))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }

    // ─────────────────────────────────────────────
    // ИМПОРТ
    // ─────────────────────────────────────────────

    /**
     * Импортирует ZIP-файл: распаковывает во временную папку,
     * делает merge БД и фото.
     */
    suspend fun importFromZip(inputUri: Uri): Result<SyncStats> = withContext(Dispatchers.IO) {
        val tempDir = File(context.cacheDir, TEMP_DIR)
        try {
            // Очищаем temp
            tempDir.deleteRecursively()
            tempDir.mkdirs()

            // Распаковываем ZIP
            context.contentResolver.openInputStream(inputUri)?.use { inStream ->
                ZipInputStream(inStream.buffered()).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val target = sanitizePath(tempDir, entry.name)
                        if (target != null) {
                            if (entry.isDirectory) {
                                target.mkdirs()
                            } else {
                                target.parentFile?.mkdirs()
                                FileOutputStream(target).use { zip.copyTo(it) }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            } ?: return@withContext Result.failure(Exception("Не удалось открыть ZIP-файл"))

            // Merge
            val importedDb = File(tempDir, DB_NAME)
            val importedPhotos = File(tempDir, PHOTOS_DIR)

            val stats = mergeDatabase(importedDb)
            mergePhotos(importedPhotos)

            Timber.d("SyncManager: импорт завершён — $stats")
            Result.success(stats)

        } catch (e: Exception) {
            Timber.e(e, "SyncManager: ошибка импорта")
            Result.failure(e)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /** Защита от Zip Slip: проверяем что target находится внутри destDir */
    private fun sanitizePath(destDir: File, entryName: String): File? {
        val target = File(destDir, entryName).canonicalFile
        return if (target.path.startsWith(destDir.canonicalPath)) target else null
    }

    // ─────────────────────────────────────────────
    // MERGE БД
    // ─────────────────────────────────────────────

    private suspend fun mergeDatabase(importedDbFile: File): SyncStats {
        if (!importedDbFile.exists()) {
            Timber.w("SyncManager: файл БД для импорта не найден")
            return SyncStats()
        }

        // Читаем данные из импортируемой БД напрямую через SQLite
        val importedData = readImportedDatabase(importedDbFile)

        var devicesAdded = 0
        var devicesUpdated = 0
        var schemesAdded = 0
        var schemesUpdated = 0
        var locationsAdded = 0

        // Карты перепривязки id: oldId (из импортируемой БД) → newId (в нашей БД)
        // Нужны чтобы правильно вставить device_locations с корректными внешними ключами
        val deviceIdMap = mutableMapOf<Int, Int>()  // importedDeviceId → localDeviceId
        val schemeIdMap = mutableMapOf<Int, Int>()  // importedSchemeId → localSchemeId

        // Merge devices — ключ: inventoryNumber
        importedData.devices.forEach { importedDevice ->
            val existing = deviceDao.getAllDevicesSync()
                .find { it.inventoryNumber == importedDevice.inventoryNumber }

            if (existing == null) {
                val newId = deviceDao.insertDevice(importedDevice.copy(id = 0)).toInt()
                deviceIdMap[importedDevice.id] = newId
                devicesAdded++
            } else {
                deviceIdMap[importedDevice.id] = existing.id
                if (importedDevice.updatedAt > existing.updatedAt) {
                    deviceDao.updateDevice(importedDevice.copy(id = existing.id))
                    devicesUpdated++
                }
            }
        }

        // Merge schemes — ключ: name
        importedData.schemes.forEach { importedScheme ->
            val existing = schemeDao.getSchemeByName(importedScheme.name)

            if (existing == null) {
                val newId = schemeDao.insertScheme(importedScheme.copy(id = 0)).toInt()
                schemeIdMap[importedScheme.id] = newId
                schemesAdded++
            } else {
                schemeIdMap[importedScheme.id] = existing.id

                val existingIsEmpty = existing.data.length <= 2 // "{}" = 2 символа
                val importedHasData = importedScheme.data.length > 2

                if (importedScheme.updatedAt > existing.updatedAt
                    || (importedHasData && existingIsEmpty)) {
                    schemeDao.updateScheme(importedScheme.copy(id = existing.id))
                    schemesUpdated++
                }
            }
        }

        // Merge device_locations — перепривязываем id через карты, добавляем только отсутствующие
        importedData.locations.forEach { importedLocation ->
            val localDeviceId = deviceIdMap[importedLocation.deviceId]
            val localSchemeId = schemeIdMap[importedLocation.schemeId]

            if (localDeviceId == null || localSchemeId == null) {
                Timber.w("SyncManager: пропущена локация — device ${importedLocation.deviceId} или scheme ${importedLocation.schemeId} не найдены в карте")
                return@forEach
            }

            val existing = deviceLocationDao.getLocation(localDeviceId, localSchemeId)
            if (existing == null) {
                deviceLocationDao.insertOrUpdateLocation(
                    importedLocation.copy(deviceId = localDeviceId, schemeId = localSchemeId)
                )
                locationsAdded++
            }
        }

        Timber.d("SyncManager: merge завершён — +$devicesAdded dev, ~$devicesUpdated dev, +$schemesAdded sch, ~$schemesUpdated sch, +$locationsAdded loc")

        return SyncStats(
            devicesAdded = devicesAdded,
            devicesUpdated = devicesUpdated,
            schemesAdded = schemesAdded,
            schemesUpdated = schemesUpdated,
            locationsAdded = locationsAdded
        )
    }

    /**
     * Читает данные из импортированной БД через прямой SQLite (android.database.sqlite).
     * Room нельзя использовать для произвольного файла, поэтому используем низкоуровневый API.
     */
    private fun readImportedDatabase(dbFile: File): ImportedData {
        val devices = mutableListOf<Device>()
        val schemes = mutableListOf<Scheme>()
        val locations = mutableListOf<DeviceLocation>()

        val db = android.database.sqlite.SQLiteDatabase.openDatabase(
            dbFile.absolutePath,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READONLY
        )

        db.use {
            // Читаем devices
            it.rawQuery("SELECT * FROM devices", null).use { cursor ->
                while (cursor.moveToNext()) {
                    try {
                        val photosRaw = cursor.getString(cursor.getColumnIndexOrThrow("photos")) ?: ""
                        val photos = photosRaw.split(";").filter { p -> p.isNotBlank() }

                        devices.add(
                            Device(
                                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                                type = cursor.getString(cursor.getColumnIndexOrThrow("type")) ?: "",
                                name = cursor.getString(cursor.getColumnIndexOrThrow("name")),
                                manufacturer = cursor.getString(cursor.getColumnIndexOrThrow("manufacturer")),
                                inventoryNumber = cursor.getString(cursor.getColumnIndexOrThrow("inventory_number")) ?: "",
                                year = cursor.getInt(cursor.getColumnIndexOrThrow("year")).takeIf { _ ->
                                    !cursor.isNull(cursor.getColumnIndexOrThrow("year"))
                                },
                                measurementLimit = cursor.getString(cursor.getColumnIndexOrThrow("measurement_limit")),
                                accuracyClass = cursor.getDouble(cursor.getColumnIndexOrThrow("accuracy_class")).takeIf { _ ->
                                    !cursor.isNull(cursor.getColumnIndexOrThrow("accuracy_class"))
                                },
                                location = cursor.getString(cursor.getColumnIndexOrThrow("location")) ?: "",
                                valveNumber = cursor.getString(cursor.getColumnIndexOrThrow("valve_number")),
                                status = cursor.getString(cursor.getColumnIndexOrThrow("status")) ?: "В работе",
                                additionalInfo = cursor.getString(cursor.getColumnIndexOrThrow("additional_info")),
                                photos = photos,
                                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
                            )
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "SyncManager: пропущена запись devices")
                    }
                }
            }

            // Читаем schemes
            it.rawQuery("SELECT * FROM schemes", null).use { cursor ->
                while (cursor.moveToNext()) {
                    try {
                        schemes.add(
                            Scheme(
                                id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                                name = cursor.getString(cursor.getColumnIndexOrThrow("name")) ?: "",
                                description = cursor.getString(cursor.getColumnIndexOrThrow("description")),
                                data = cursor.getString(cursor.getColumnIndexOrThrow("data")) ?: "{}",
                                updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow("updated_at"))
                            )
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "SyncManager: пропущена запись schemes")
                    }
                }
            }

            // Читаем device_locations
            it.rawQuery("SELECT * FROM device_locations", null).use { cursor ->
                while (cursor.moveToNext()) {
                    try {
                        locations.add(
                            DeviceLocation(
                                deviceId = cursor.getInt(cursor.getColumnIndexOrThrow("device_id")),
                                schemeId = cursor.getInt(cursor.getColumnIndexOrThrow("scheme_id")),
                                x = cursor.getFloat(cursor.getColumnIndexOrThrow("x")),
                                y = cursor.getFloat(cursor.getColumnIndexOrThrow("y")),
                                rotation = cursor.getFloat(cursor.getColumnIndexOrThrow("rotation"))
                            )
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "SyncManager: пропущена запись device_locations")
                    }
                }
            }
        }

        Timber.d("SyncManager: прочитано из импортируемой БД — ${devices.size} устройств, ${schemes.size} схем, ${locations.size} локаций")
        return ImportedData(devices, schemes, locations)
    }

    // ─────────────────────────────────────────────
    // MERGE ФОТО
    // ─────────────────────────────────────────────

    private fun mergePhotos(importedPhotosDir: File) {
        if (!importedPhotosDir.exists()) return

        val targetBase = photoManager.getBasePhotosDir()

        importedPhotosDir.walkTopDown().forEach { srcFile ->
            if (srcFile.isFile) {
                val relative = srcFile.relativeTo(importedPhotosDir)
                val destFile = File(targetBase, relative.path)

                if (!destFile.exists()) {
                    destFile.parentFile?.mkdirs()
                    srcFile.copyTo(destFile)
                    Timber.d("SyncManager: скопировано фото ${relative.path}")
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // DATA CLASSES
    // ─────────────────────────────────────────────

    private data class ImportedData(
        val devices: List<Device>,
        val schemes: List<Scheme>,
        val locations: List<DeviceLocation>
    )

    data class SyncStats(
        val devicesAdded: Int = 0,
        val devicesUpdated: Int = 0,
        val schemesAdded: Int = 0,
        val schemesUpdated: Int = 0,
        val locationsAdded: Int = 0
    ) {
        fun isEmpty() = devicesAdded == 0 && devicesUpdated == 0 &&
                schemesAdded == 0 && schemesUpdated == 0 && locationsAdded == 0

        fun toSummary(): String = buildString {
            if (devicesAdded > 0) appendLine("• Устройств добавлено: $devicesAdded")
            if (devicesUpdated > 0) appendLine("• Устройств обновлено: $devicesUpdated")
            if (schemesAdded > 0) appendLine("• Схем добавлено: $schemesAdded")
            if (schemesUpdated > 0) appendLine("• Схем обновлено: $schemesUpdated")
            if (locationsAdded > 0) appendLine("• Размещений добавлено: $locationsAdded")
            if (isEmpty()) append("Новых данных не найдено")
        }.trimEnd()
    }
}