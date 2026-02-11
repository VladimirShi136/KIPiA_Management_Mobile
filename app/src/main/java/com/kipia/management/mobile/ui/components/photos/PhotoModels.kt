package com.kipia.management.mobile.ui.components.photos

import com.kipia.management.mobile.data.entities.Device

enum class DisplayMode {
    GROUPED,  // Сгруппировано по локациям
    FLAT      // Плоский список (как сейчас)
}

data class PhotoItem(
    val fileName: String,
    val fullPath: String,
    val device: Device
)