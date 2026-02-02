package com.kipia.management.mobile.utils

import android.net.Uri
import androidx.core.content.FileProvider
import com.kipia.management.mobile.data.entities.Device
import java.io.File

/**
 * Extension функции для работы с фото
 */

// Для Device
val Device.photoCount: Int get() = photos.size
val Device.hasPhotos: Boolean get() = photos.isNotEmpty()

fun Device.addPhotoName(fileName: String): Device {
    return this.copy(photos = photos + fileName)
}

fun Device.removePhotoName(fileName: String): Device {
    return this.copy(photos = photos - fileName)
}

// Для PhotoManager
fun PhotoManager.getDevicePhotoFiles(device: Device): List<File> {
    return device.photos.map { fileName ->
        File(getLocationDir(device.location), fileName)
    }.filter { it.exists() }
}

fun PhotoManager.getDevicePhotoUris(device: Device): List<Uri> {
    return getDevicePhotoFiles(device).map { file ->
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}