package com.kipia.management.mobile.utils

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.*
import android.graphics.BitmapFactory
import timber.log.Timber
import java.io.FileOutputStream

class PhotoManager(
    private val fragment: Fragment,
    private val onPhotoTaken: (Uri) -> Unit,
    private val onPhotoSelected: (Uri) -> Unit
) {

    private var currentPhotoUri: Uri? = null

    // Контракт для съемки фото
    private val takePhotoLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            currentPhotoUri?.let { onPhotoTaken(it) }
        }
    }

    // Контракт для выбора фото из галереи
    private val pickPhotoLauncher = fragment.registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { onPhotoSelected(it) }
    }

    fun takePhoto() {
        if (checkCameraPermission()) {
            createImageFile()?.let { uri ->
                currentPhotoUri = uri
                takePhotoLauncher.launch(uri)
            }
        }
    }

    fun pickPhotoFromGallery() {
        if (checkStoragePermission()) {
            pickPhotoLauncher.launch("image/*")
        }
    }

    private fun checkCameraPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(
                fragment.requireContext(),
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            fragment.requestPermissions(
                arrayOf(android.Manifest.permission.CAMERA),
                PERMISSION_REQUEST_CAMERA
            )
            false
        }
    }

    private fun checkStoragePermission(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        } else {
            arrayOf(
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        }

        return if (permissions.all { permission ->
                ContextCompat.checkSelfPermission(
                    fragment.requireContext(),
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            true
        } else {
            fragment.requestPermissions(permissions, PERMISSION_REQUEST_STORAGE)
            false
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): Uri? {
        val context = fragment.requireContext()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Для Android 10+ используем MediaStore
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, generateFileName())
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            // Для старых версий создаем файл
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
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
    }

    private fun generateFileName(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return "KIPiA_$timeStamp.jpg"
    }

    companion object {
        const val PERMISSION_REQUEST_CAMERA = 1001
        const val PERMISSION_REQUEST_STORAGE = 1002

        fun copyPhotoToAppStorage(context: Context, sourceUri: Uri): String? {
            return try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                val fileName = generateFileName()
                val outputDir = File(context.filesDir, "device_photos")

                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }

                val outputFile = File(outputDir, fileName)
                val outputStream = outputFile.outputStream()

                inputStream?.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }

                outputFile.absolutePath
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    fun rotatePhoto(context: Context, originalPath: String, degrees: Float): String? {
        return try {
            val originalFile = File(originalPath)
            if (!originalFile.exists()) return null

            val bitmap = BitmapFactory.decodeFile(originalPath)
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

            rotatedFile.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Error rotating photo")
            null
        }
    }
}