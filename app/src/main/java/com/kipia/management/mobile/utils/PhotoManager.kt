package com.kipia.management.mobile.utils

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    /**
     * Проверяет разрешения для камеры
     */
    fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Проверяет разрешения для чтения медиа
     */
    fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Проверяет все необходимые разрешения
     */
    fun hasAllRequiredPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Получает список разрешений для запроса
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }
    }

    /**
     * Открывает настройки разрешений
     */
    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    /**
     * Создает временный файл для фото
     */
    fun createImageFile(): Uri? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ - используем MediaStore
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, generateFileName())
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KIPiA")
                }

                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            } else {
                // Android 9 и ниже - создаем файл
                val storageDir = File(
                    context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                    "KIPiA"
                )

                if (!storageDir.exists()) {
                    storageDir.mkdirs()
                }

                val imageFile = File.createTempFile(
                    "JPEG_${System.currentTimeMillis()}_",
                    ".jpg",
                    storageDir
                )

                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Сохраняет фото из Uri в постоянное хранилище приложения
     */
    suspend fun savePhotoFromUri(uri: Uri): String? {
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

    /**
     * Удаляет фото
     */
    fun deletePhoto(photoPath: String): Boolean {
        return try {
            val file = File(photoPath)
            if (file.exists()) {
                file.delete()
            } else {
                // Попробуем удалить через MediaStore для Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    context.contentResolver.delete(uri, null, null) > 0
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Получает папку для хранения фото устройств
     */
    fun getDevicePhotosDir(): File {
        val dir = File(context.filesDir, "device_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Генерирует имя файла
     */
    private fun generateFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "KIPiA_$timeStamp.jpg"
    }
}