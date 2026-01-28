package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.domain.usecase.SchemeSyncUseCase
import com.kipia.management.mobile.repository.DeviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class DeviceDeleteViewModel @Inject constructor(
    private val schemeSyncUseCase: SchemeSyncUseCase,
    private val deviceRepository: DeviceRepository // ← ДОБАВЬ ЭТОТ КОНСТРУКТОР!
) : ViewModel() {

    private val _showDeleteDialog = MutableStateFlow<DeleteDialogData?>(null)
    val showDeleteDialog: StateFlow<DeleteDialogData?> = _showDeleteDialog

    /**
     * ВСЕГДА показывает диалог — но с разным контентом в зависимости от контекста.
     * Возвращает true, потому что диалог всегда показывается.
     */
    suspend fun checkAndShowDialog(device: Device): Boolean {
        println("DEBUG DeviceDeleteViewModel: Проверка диалога для устройства: ${device.id}, локация: ${device.location}")

        // 1. Получаем схему (если есть)
        val scheme = schemeSyncUseCase.checkSchemeOnDeviceDelete(device)

        // 2. Получаем количество приборов в локации
        val deviceCountInLocation = deviceRepository.countDevicesInLocation(device.location)
        val isLastInLocation = deviceCountInLocation == 1

        println("DEBUG DeviceDeleteViewModel: Локация: '${device.location}', приборов: $deviceCountInLocation, последний: $isLastInLocation")

        // 3. Показываем диалог ВСЕГДА
        _showDeleteDialog.value = DeleteDialogData(
            device = device,
            scheme = scheme,
            deviceCountInLocation = deviceCountInLocation,
            isLastInLocation = isLastInLocation
        )

        println("DEBUG DeviceDeleteViewModel: Диалог показан (всегда)")
        return true // ← ВСЕГДА true!
    }

    fun dismissDialog() {
        println("DEBUG DeviceDeleteViewModel: Закрытие диалога")
        _showDeleteDialog.value = null
    }
}

data class DeleteDialogData(
    val device: Device,
    val scheme: Scheme?,
    val deviceCountInLocation: Int, // ← НОВОЕ: количество приборов в локации
    val isLastInLocation: Boolean   // ← Удобный флаг
)