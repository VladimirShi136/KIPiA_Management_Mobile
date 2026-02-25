package com.kipia.management.mobile.ui.shared

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationManager @Inject constructor() {
    sealed class Notification {
        data class DeviceSaved(val deviceName: String) : Notification()
        data class DeviceDeleted(val deviceName: String, val withScheme: Boolean = false) : Notification()
        data class SchemeSaved(val schemeName: String) : Notification()
        data class Error(val message: String) : Notification()
        object None : Notification() // ← Пустое состояние
    }

    // ★★★★ Добавляем буфер и replay ★★★★
    private val _notification = MutableSharedFlow<Notification>(
        replay = 1, // ← Сохраняет последнее значение для новых подписчиков
        extraBufferCapacity = 10 // ← Дополнительный буфер
    )
    val notification: SharedFlow<Notification> = _notification.asSharedFlow()

    suspend fun notifyDeviceSaved(deviceName: String) {
        println("DEBUG NotificationManager: Отправка уведомления о сохранении '$deviceName'")
        _notification.emit(Notification.DeviceSaved(deviceName))
    }

    suspend fun notifyDeviceDeleted(deviceName: String, withScheme: Boolean = false) {
        println("DEBUG NotificationManager: Отправка уведомления об удалении '$deviceName' withScheme=$withScheme")
        _notification.emit(Notification.DeviceDeleted(deviceName, withScheme))
    }

    suspend fun notifySchemeSaved(schemeName: String) {
        Timber.d("NotificationManager: Отправка уведомления о сохранении схемы '$schemeName'")
        _notification.emit(Notification.SchemeSaved(schemeName))
    }

    suspend fun notifyError(message: String) {
        println("DEBUG NotificationManager: Отправка уведомления об ошибке '$message'")
        _notification.emit(Notification.Error(message))
    }

    // ★★★★ Метод для очистки replay cache ★★★★
    suspend fun clearLastNotification() {
        println("DEBUG NotificationManager: Очистка последнего уведомления")
        // Отправляем пустое уведомление, чтобы перезаписать replay cache
        _notification.emit(Notification.None)
    }
}