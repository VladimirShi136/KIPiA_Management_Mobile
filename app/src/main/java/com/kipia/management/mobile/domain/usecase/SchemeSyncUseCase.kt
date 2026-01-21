package com.kipia.management.mobile.domain.usecase

import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.SchemeRepository
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchemeSyncUseCase @Inject constructor(
    private val schemeRepository: SchemeRepository,
    private val deviceRepository: DeviceRepository
) {

    /**
     * При сохранении/обновлении устройства:
     * 1. Ищем схему с name = location устройства
     * 2. Если нет - создаем новую схему (как в Desktop)
     */
    suspend fun syncSchemeOnDeviceSave(device: Device) {
        val location = device.location
        if (location.isBlank()) return

        // Ищем схему по названию (название = локация)
        val existingScheme = schemeRepository.getSchemeByName(location)

        if (existingScheme == null) {
            // Создаем новую схему ТОЧНО КАК В DESKTOP
            val newScheme = Scheme(
                name = location,  // ★★★★ ИМЯ = ЛОКАЦИИ ★★★★
                description = "Автоматически созданная схема для локации $location",
                data = "{}"  // Пустая схема
            )
            schemeRepository.insertScheme(newScheme)
        }
        // Если схема уже существует - ничего не делаем
    }

    /**
     * При удалении устройства проверяем:
     * 1. Остались ли устройства с этой локацией
     * 2. Если нет - возвращаем схему для диалога
     */
    suspend fun checkSchemeOnDeviceDelete(device: Device): Scheme? {
        val location = device.location
        if (location.isBlank()) return null

        // Ищем схему для этой локации
        val scheme = schemeRepository.getSchemeByName(location) ?: return null

        // Проверяем сколько устройств осталось с этой локацией
        val allDevices = deviceRepository.getAllDevicesSync()
        val devicesAtLocation = allDevices.count { it.location == location }

        // devicesAtLocation = 1 значит текущее устройство - последнее
        return if (devicesAtLocation <= 1) {
            scheme  // Показывать диалог
        } else {
            null    // Не показывать диалог
        }
    }

    /**
     * ★★★★ ОБНОВЛЕННЫЙ МЕТОД - УДАЛЯЕТ СХЕМУ ЕСЛИ НЕТ ПРИБОРОВ ★★★★
     * Этот метод вызывается ПОСЛЕ удаления устройства
     */
    suspend fun deleteSchemeIfEmpty(schemeName: String): Boolean {
        Timber.d("УДАЛЕНИЕ СХЕМЫ: проверяем $schemeName")

        val allDevices = deviceRepository.getAllDevicesSync()
        Timber.d("Всего приборов в БД: ${allDevices.size}")

        val devicesAtLocation = allDevices.count { it.location == schemeName }
        Timber.d("Приборов с локацией '$schemeName': $devicesAtLocation")

        // ★★★★ КРИТИЧЕСКОЕ ИЗМЕНЕНИЕ: проверяем <= 0 ★★★★
        // Потому что устройство уже удалено!
        return if (devicesAtLocation == 0) {
            val scheme = schemeRepository.getSchemeByName(schemeName)
            if (scheme != null) {
                Timber.d("Удаляем схему '${scheme.name}' (ID: ${scheme.id})")
                try {
                    schemeRepository.deleteScheme(scheme)
                    Timber.d("✅ Схема успешно удалена")
                    true
                } catch (e: Exception) {
                    Timber.e(e, "❌ Ошибка при удалении схемы")
                    false
                }
            } else {
                Timber.w("Схема '$schemeName' не найдена в БД")
                false
            }
        } else {
            Timber.d("❌ Не удаляем схему '$schemeName': еще $devicesAtLocation приборов")
            false
        }
    }
}