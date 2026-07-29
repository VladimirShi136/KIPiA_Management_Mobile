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
        data class SyncSuccess(val message: String) : Notification()
        data class SyncError(val message: String) : Notification()
        object None : Notification()
    }

    private val _notification = MutableSharedFlow<Notification>(
        replay = 1,
        extraBufferCapacity = 10
    )
    val notification: SharedFlow<Notification> = _notification.asSharedFlow()

    suspend fun notifyDeviceSaved(deviceName: String) {
        _notification.emit(Notification.DeviceSaved(deviceName))
    }

    suspend fun notifyDeviceDeleted(deviceName: String, withScheme: Boolean = false) {
        _notification.emit(Notification.DeviceDeleted(deviceName, withScheme))
    }

    suspend fun notifySchemeSaved(schemeName: String) {
        _notification.emit(Notification.SchemeSaved(schemeName))
    }

    suspend fun notifyError(message: String) {
        _notification.emit(Notification.Error(message))
    }

    suspend fun notifySyncSuccess(message: String) {
        _notification.emit(Notification.SyncSuccess(message))
    }

    suspend fun notifySyncError(message: String) {
        _notification.emit(Notification.SyncError(message))
    }

    suspend fun clearLastNotification() {
        _notification.emit(Notification.None)
    }
}
