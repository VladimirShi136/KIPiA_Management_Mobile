package com.kipia.management.mobile.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.room.withTransaction
import com.kipia.management.mobile.data.dao.DeviceDao
import com.kipia.management.mobile.data.database.AppDatabase
import com.kipia.management.mobile.data.entities.Device
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import android.provider.OpenableColumns
import java.io.IOException

@Singleton
class PhotoManager @Inject constructor(
    @param:ApplicationContext val context: Context,
    private val deviceDao: DeviceDao,
    private val database: AppDatabase
) {
    companion object {
        private const val BASE_PHOTOS_DIR = "device_photos"
        private const val PHOTO_PREFIX = "device"
    }

    // === СТРУКТУРА ПАПОК (КАК В JAVA FX) ===

    /**
     * Получает базовую папку для фото
     */
    fun getBasePhotosDir(): File {
        return File(context.filesDir, BASE_PHOTOS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Получает папку для конкретного местоположения устройства
     */
    fun getLocationDir(location: String): File {
        val safeLocation = location.ifEmpty { "unknown" }
            .replace(Regex("[\\\\/:*?\"<>|]"), "_") // Безопасное имя папки
        return File(getBasePhotosDir(), safeLocation).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Генерирует имя файла как в Java FX: device_[id]_[timestamp]_[hash].jpg
     */
    private fun generateJavaFXFileName(deviceId: Int, originalName: String? = null): String {
        val timestamp = System.currentTimeMillis()
        val randomHash = UUID.randomUUID().toString().substring(0, 8)

        val baseName = originalName?.substringBeforeLast(".") ?: "photo"
        val safeBaseName = baseName.replace(Regex("[\\\\/:*?\"<>|]"), "_")

        val ext = originalName?.substringAfterLast(".", "jpg") ?: "jpg"

        return "${PHOTO_PREFIX}_${deviceId}_${safeBaseName}_${timestamp}_${randomHash}.$ext"
    }

    // === ОСНОВНЫЕ МЕТОДЫ С ИНТЕГРАЦИЕЙ БД ===

    /**
     * Сохраняет фото для устройства и обновляет БД
     */
    suspend fun savePhotoForDevice(
        device: Device,
        uri: Uri
    ): Result<PhotoSaveResult> = database.withTransaction {
        try {
            // 1. Генерируем имя файла
            val fileName = generateJavaFXFileName(
                device.id,
                getFileNameFromUri(uri)
            )

            // 2. Создаем папку location
            val locationDir = getLocationDir(device.location)

            // 3. Копируем файл
            val outputFile = File(locationDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@withTransaction Result.failure(
                IOException("Не удалось открыть файл из $uri")
            )

            if (!outputFile.exists()) {
                return@withTransaction Result.failure(
                    IOException("Файл не был создан: ${outputFile.absolutePath}")
                )
            }

            // 4. Обновляем Device в БД (добавляем фото в список)
            val updatedDevice = device.addPhoto(fileName)
            val rowsUpdated = deviceDao.updateDevice(updatedDevice)

            if (rowsUpdated <= 0) {
                // Откатываем - удаляем созданный файл
                outputFile.delete()
                return@withTransaction Result.failure(
                    IOException("Не удалось обновить устройство в БД")
                )
            }

            Result.success(
                PhotoSaveResult(
                    fileName = fileName,
                    fullPath = outputFile.absolutePath,
                    device = updatedDevice
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Удаляет фото устройства и обновляет БД
     */
    suspend fun deleteDevicePhoto(
        device: Device,
        fileName: String
    ): Boolean = database.withTransaction {
        try {
            var fileDeleted = true
            var dbUpdated = true

            // 1. Удаляем файл (если существует)
            val file = File(getLocationDir(device.location), fileName)
            if (file.exists()) {
                fileDeleted = file.delete()
            }

            // 2. Обновляем Device в БД (удаляем фото из списка)
            if (fileDeleted) {
                val updatedDevice = device.removePhoto(fileName)
                dbUpdated = deviceDao.updateDevice(updatedDevice) > 0
            }

            fileDeleted && dbUpdated

        } catch (_: Exception) {
            false
        }
    }

    /**
     * Получает полный путь к фото устройства
     */
    fun getFullPhotoPath(device: Device, fileName: String): String? {
        return try {
            File(getLocationDir(device.location), fileName).absolutePath
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Получает все существующие фото устройства
     */
    fun getDevicePhotos(device: Device): List<File> {
        return device.photos.map { fileName ->
            File(getLocationDir(device.location), fileName)
        }.filter { it.exists() }
    }

    /**
     * Получает все существующие фото устройства (пути)
     */
    fun getDevicePhotoPaths(device: Device): List<String> {
        return getDevicePhotos(device).map { it.absolutePath }
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    /**
     * Получает имя файла из Uri
     */
    private fun getFileNameFromUri(uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            cursor.getString(displayNameIndex)
                        } else {
                            null
                        }
                    } else {
                        null
                    }
                }
            }
            "file" -> {
                uri.lastPathSegment
            }
            else -> {
                // Пробуем извлечь из пути
                uri.toString().substringAfterLast("/")
            }
        }
    }

    /**
     * Сохраняет фото из Uri в постоянное хранилище приложения
     */
    fun savePhotoFromUri(uri: Uri): String? {
        // Старый метод - для совместимости
        return kotlin.runCatching {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = "IMG_${System.currentTimeMillis()}.jpg"
                val outputDir = File(context.filesDir, "device_photos")

                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }

                val outputFile = File(outputDir, fileName)
                val outputStream = outputFile.outputStream()

                inputStream.copyTo(outputStream)
                outputStream.close()

                outputFile.absolutePath
            }
        }.getOrNull()
    }

    /**
     * Поворачивает фото на указанный угол
     */
    fun rotatePhoto(photoPath: String, degrees: Float): String? {
        return try {
            val originalFile = File(photoPath)
            if (!originalFile.exists()) return null

            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            val bitmap = BitmapFactory.decodeFile(photoPath, options)
            val matrix = Matrix().apply { postRotate(degrees) }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            // Сохраняем повернутую версию
            val rotatedFile = File(context.filesDir, "rotated_${System.currentTimeMillis()}.jpg")
            FileOutputStream(rotatedFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            // Освобождаем память
            bitmap.recycle()
            rotatedBitmap.recycle()

            // Удаляем старый файл и переименовываем новый
            if (originalFile.delete()) {
                rotatedFile.renameTo(originalFile)
                originalFile.absolutePath
            } else {
                rotatedFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class PhotoSaveResult(
        val fileName: String,
        val fullPath: String,
        val device: Device
    )
}