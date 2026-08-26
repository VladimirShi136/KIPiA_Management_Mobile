package com.kipia.management.mobile.managers

import android.content.Context
import com.kipia.management.mobile.data.dao.DeviceDao
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Менеджер для проверки целостности базы данных и файловой системы.
 */
@Singleton
class DatabaseIntegrityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceDao: DeviceDao,
    private val photoManager: PhotoManager
) {

    data class IntegrityCheckResult(
        val isHealthy: Boolean,
        val missingPhotos: List<String> = emptyList(),           // Фото в БД, но нет на диске
        val orphanedFiles: List<String> = emptyList(),           // Файлы на диске, но не в БД
        val deletedDeviceFiles: List<String> = emptyList(),      // Файлы удаленных приборов
        val emptyFolders: List<String> = emptyList(),            // Пустые папки
        val issues: List<String> = emptyList()                   // Текстовые описания проблем
    ) {
        fun getSummary(): String {
            val issueCount = missingPhotos.size + orphanedFiles.size + deletedDeviceFiles.size + emptyFolders.size
            return if (isHealthy) {
                "✅ База данных в хорошем состоянии"
            } else {
                "⚠️ Найдено $issueCount проблем"
            }
        }

        fun getDetailedReport(): String {
            val report = mutableListOf<String>()
            report.add(getSummary())
            report.add("")

            if (missingPhotos.isNotEmpty()) {
                report.add("❌ Отсутствующие фото (${missingPhotos.size}):")
                missingPhotos.take(5).forEach { report.add("  • $it") }
                if (missingPhotos.size > 5) report.add("  • ... и еще ${missingPhotos.size - 5}")
            }

            if (orphanedFiles.isNotEmpty()) {
                report.add("🗑️ Сирот-файлы на диске (${orphanedFiles.size}):")
                orphanedFiles.take(5).forEach { report.add("  • $it") }
                if (orphanedFiles.size > 5) report.add("  • ... и еще ${orphanedFiles.size - 5}")
            }

            if (deletedDeviceFiles.isNotEmpty()) {
                report.add("🗂️ Файлы удаленных приборов (${deletedDeviceFiles.size}):")
                deletedDeviceFiles.take(5).forEach { report.add("  • $it") }
                if (deletedDeviceFiles.size > 5) report.add("  • ... и еще ${deletedDeviceFiles.size - 5}")
            }

            if (emptyFolders.isNotEmpty()) {
                report.add("📁 Пустые папки (${emptyFolders.size}):")
                emptyFolders.take(5).forEach { report.add("  • $it") }
                if (emptyFolders.size > 5) report.add("  • ... и еще ${emptyFolders.size - 5}")
            }

            if (issues.isNotEmpty()) {
                report.add("ℹ️ Другие проблемы:")
                issues.take(5).forEach { report.add("  • $it") }
                if (issues.size > 5) report.add("  • ... и еще ${issues.size - 5}")
            }

            return report.joinToString("\n")
        }
    }

    suspend fun checkIntegrity(): IntegrityCheckResult {
        return try {
            val allDevices = deviceDao.getAllDevicesForExport()
            val baseDir = photoManager.getBasePhotosDir()

            val missingPhotos = mutableListOf<String>()
            val allDiskFiles = mutableSetOf<String>()
            val deletedDeviceFiles = mutableListOf<String>()
            val emptyFolders = mutableListOf<String>()
            val issues = mutableListOf<String>()

            // 1. Проверяем фото в БД - существуют ли на диске
            for (device in allDevices) {
                for (photoFileName in device.photos) {
                    val photoFile = File(photoManager.getLocationDir(device.location), photoFileName)
                    if (!photoFile.exists()) {
                        missingPhotos.add("${device.location}/$photoFileName (${device.inventoryNumber})")
                    }
                }

                // Если прибор удален, его файлы должны быть удалены
                if (device.isDeleted()) {
                    val deviceLocationDir = photoManager.getLocationDir(device.location)
                    if (deviceLocationDir.exists()) {
                        for (photoFileName in device.photos) {
                            val photoFile = File(deviceLocationDir, photoFileName)
                            if (photoFile.exists()) {
                                deletedDeviceFiles.add("${device.location}/$photoFileName")
                            }
                        }
                    }
                }
            }

            // 2. Сканируем все файлы на диске и ищем сирот
            val orphanedFiles = mutableListOf<String>()
            if (baseDir.exists()) {
                for (locationFolder in baseDir.listFiles() ?: emptyArray()) {
                    if (!locationFolder.isDirectory) continue

                    val locationName = locationFolder.name
                    for (photoFile in locationFolder.listFiles() ?: emptyArray()) {
                        if (!photoFile.isFile) continue

                        val photoFileName = photoFile.name
                        allDiskFiles.add("${locationName}/$photoFileName")

                        // Проверяем, существует ли этот файл в БД активного устройства
                        val isInDatabase = allDevices.any { device ->
                            device.location.replace(Regex("[\\\\/:*?\"<>|]"), "_") == locationName &&
                                    !device.isDeleted() &&
                                    photoFileName in device.photos
                        }

                        if (!isInDatabase) {
                            orphanedFiles.add("${locationName}/$photoFileName")
                        }
                    }

                    // Проверяем пустые папки
                    if ((locationFolder.listFiles()?.isEmpty() == true)) {
                        emptyFolders.add(locationName)
                    }
                }
            }

            // 3. Проверяем логические ошибки
            for (device in allDevices) {
                // Прибор активен, но фото отсутствуют
                if (!device.isDeleted() && device.photos.isEmpty() && device.additionalInfo?.contains("фото") == true) {
                    issues.add("${device.inventoryNumber}: активный прибор, но фото отсутствуют")
                }

                // Timestamp в будущем
                if (device.updatedAt > System.currentTimeMillis() + 1000) {
                    issues.add("${device.inventoryNumber}: дата обновления в будущем")
                }
            }

            val isHealthy = missingPhotos.isEmpty() &&
                    orphanedFiles.isEmpty() &&
                    deletedDeviceFiles.isEmpty() &&
                    emptyFolders.isEmpty() &&
                    issues.isEmpty()

            IntegrityCheckResult(
                isHealthy = isHealthy,
                missingPhotos = missingPhotos,
                orphanedFiles = orphanedFiles,
                deletedDeviceFiles = deletedDeviceFiles,
                emptyFolders = emptyFolders,
                issues = issues
            )
        } catch (e: Exception) {
            IntegrityCheckResult(
                isHealthy = false,
                issues = listOf("Ошибка при проверке: ${e.message}")
            )
        }
    }

    suspend fun cleanupOrphanedFiles(orphanedFiles: List<String>): Result<String> = try {
        val baseDir = photoManager.getBasePhotosDir()
        var deletedCount = 0

        for (filePath in orphanedFiles) {
            val parts = filePath.split("/")
            if (parts.size == 2) {
                val locationFolder = File(baseDir, parts[0])
                val file = File(locationFolder, parts[1])
                if (file.exists() && file.delete()) {
                    deletedCount++
                }
            }
        }

        Result.success("Удалено $deletedCount сирот-файлов из ${orphanedFiles.size}")
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun cleanupEmptyFolders(emptyFolders: List<String>): Result<String> = try {
        val baseDir = photoManager.getBasePhotosDir()
        var deletedCount = 0

        for (folderName in emptyFolders) {
            val folder = File(baseDir, folderName)
            if (folder.exists() && folder.isDirectory && (folder.listFiles()?.isEmpty() == true)) {
                if (folder.delete()) {
                    deletedCount++
                }
            }
        }

        Result.success("Удалено $deletedCount пустых папок из ${emptyFolders.size}")
    } catch (e: Exception) {
        Result.failure(e)
    }
}

