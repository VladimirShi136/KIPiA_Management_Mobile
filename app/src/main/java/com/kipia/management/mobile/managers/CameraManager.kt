package com.kipia.management.mobile.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Менеджер для работы с камерой.
 * Отвечает за:
 * - проверку и запрос разрешения CAMERA
 * - создание временного файла и URI для съёмки
 * - запуск камеры или запрос разрешения (если его нет)
 *
 * Использование в Composable:
 *
 * val cameraManager = remember { CameraManager(context) }
 *
 * val permissionLauncher = rememberLauncherForActivityResult(
 *     ActivityResultContracts.RequestPermission()
 * ) { granted -> cameraManager.onPermissionResult(granted, cameraLauncher) }
 *
 * val cameraLauncher = rememberLauncherForActivityResult(
 *     ActivityResultContracts.TakePicture()
 * ) { success -> if (success) cameraManager.consumePendingUri()?.let { onPhoto(it) } }
 *
 * // Запуск:
 * cameraManager.launch(permissionLauncher, cameraLauncher)
 */
class CameraManager(private val context: Context) {

    /**
     * URI временного файла, созданного для текущей съёмки.
     * Хранится до момента получения результата от камеры.
     */
    private var pendingUri: Uri? = null

    // -------------------------------------------------------------------------
    // Публичное API
    // -------------------------------------------------------------------------

    /**
     * Основная точка входа — вызывать при нажатии «Сделать фото».
     *
     * Если разрешение уже есть — сразу запускает камеру.
     * Если нет — запрашивает разрешение; камера запустится в [onPermissionResult].
     *
     * @return false если не удалось создать временный файл (показать ошибку пользователю)
     */
    fun launch(
        permissionLauncher: ActivityResultLauncher<String>,
        cameraLauncher: ActivityResultLauncher<Uri>
    ): Boolean {
        val uri = createTempImageUri() ?: return false
        pendingUri = uri

        return if (hasPermission()) {
            cameraLauncher.launch(uri)
            true
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
            true
        }
    }

    /**
     * Вызывать из колбэка [ActivityResultContracts.RequestPermission].
     *
     * Если разрешение получено — запускает камеру с ранее созданным URI.
     * Если отказано — очищает pendingUri и возвращает false.
     *
     * @return true если камера запущена, false если разрешение отклонено
     */
    fun onPermissionResult(
        isGranted: Boolean,
        cameraLauncher: ActivityResultLauncher<Uri>
    ): Boolean {
        return if (isGranted) {
            val uri = pendingUri ?: return false
            cameraLauncher.launch(uri)
            true
        } else {
            pendingUri = null
            false
        }
    }

    /**
     * Вызывать из колбэка [ActivityResultContracts.TakePicture] при success = true.
     * Возвращает URI сохранённого фото и очищает внутреннее состояние.
     */
    fun consumePendingUri(): Uri? {
        val uri = pendingUri
        pendingUri = null
        return uri
    }

    /**
     * Проверяет наличие разрешения CAMERA без запроса.
     */
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    // -------------------------------------------------------------------------
    // Приватные утилиты
    // -------------------------------------------------------------------------

    /**
     * Создаёт временный JPEG-файл в кэше приложения и возвращает FileProvider URI.
     * Файл нужно удалить после сохранения итогового фото — см. [cleanupTempFile].
     */
    private fun createTempImageUri(): Uri? = runCatching {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val tempFile = File.createTempFile("CAM_${timestamp}_", ".jpg", context.cacheDir)
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }.getOrNull()

    /**
     * Удаляет временный файл, если он находится в кэше приложения.
     * Вызывать после успешного сохранения фото в постоянное хранилище.
     */
    fun cleanupTempFile(uri: Uri) {
        runCatching {
            if (uri.scheme == "file" || uri.scheme == "content") {
                val path = uri.path ?: return
                val file = File(path)
                if (file.canonicalPath.startsWith(context.cacheDir.canonicalPath)) {
                    file.delete()
                }
            }
        }
    }
}
