package com.kipia.management.mobile.domain.usecase

import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.SchemeRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Класс для синхронизации схемы с устройством при:
 * 1. Сохранении/обновлении устройства
 * 2. Удалении устройства
 */
@Singleton
class SchemeSyncUseCase @Inject constructor(
    private val schemeRepository: SchemeRepository,
    private val deviceRepository: DeviceRepository
) {

    /**
     * При сохранении/обновлении устройства:
     * 1. Ищем схему с name = location устройства (включая удаленные)
     * 2. Если нет - создаем новую. Если есть и удалена - воскрешаем.
     */
    suspend fun syncSchemeOnDeviceSave(device: Device) {
        val location = device.location
        if (location.isBlank()) return

        // Ищем схему по названию (включая удаленные через репозиторий/DAO)
        val existingScheme = schemeRepository.getSchemeByName(location)

        if (existingScheme == null) {
            val newScheme = Scheme(
                name = location,
                description = "Автоматически созданная схема для локации $location",
                data = "{}"
            )
            schemeRepository.insertScheme(newScheme)
        } else if (existingScheme.isDeleted()) {
            // Если схема существует, но помечена как удаленная - восстанавливаем её
            schemeRepository.insertScheme(existingScheme.copy(deletedAt = 0).withUpdatedNow())
            Timber.d("SchemeSyncUseCase: схема '$location' восстановлена")
        }
    }

    /**
     * При удалении устройства проверяем:
     * 1. Остались ли активные устройства с этой локацией
     * 2. Если нет - возвращаем схему для диалога
     */
    suspend fun checkSchemeOnDeviceDelete(device: Device): Scheme? {
        val location = device.location
        if (location.isBlank()) return null

        val scheme = schemeRepository.getSchemeByName(location) ?: return null
        if (scheme.isDeleted()) return null

        // Проверяем только активные (не удаленные) устройства
        val activeDevices = deviceRepository.getAllDevicesSync()
        val devicesAtLocation = activeDevices.count { it.location == location }

        return if (devicesAtLocation <= 1) {
            scheme
        } else {
            null
        }
    }

    /**
     * Удаляет схему (soft delete), если в этой локации больше нет приборов.
     */
    suspend fun deleteSchemeIfEmpty(schemeName: String): Boolean {
        Timber.d("УДАЛЕНИЕ СХЕМЫ: проверяем $schemeName")

        val activeDevices = deviceRepository.getAllDevicesSync()
        val devicesAtLocation = activeDevices.count { it.location == schemeName }

        if (devicesAtLocation == 0) {
            val scheme = schemeRepository.getSchemeByName(schemeName)
            if (scheme != null && !scheme.isDeleted()) {
                try {
                    schemeRepository.deleteScheme(scheme)
                    Timber.d("✅ Схема '$schemeName' помечена как удаленная")
                    return true
                } catch (e: Exception) {
                    Timber.e(e, "❌ Ошибка при удалении схемы")
                }
            }
        }
        return false
    }
}