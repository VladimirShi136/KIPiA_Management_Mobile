package com.kipia.management.mobile.data

import com.kipia.management.mobile.data.database.AppDatabase
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.domain.usecase.SchemeSyncUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val database: AppDatabase,
    private val schemeSyncUseCase: SchemeSyncUseCase
) {

    suspend fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            val deviceDao = database.deviceDao()

            // Проверяем, пустая ли база устройств
            if (deviceDao.getAllDevicesSync().isEmpty()) {
                // Добавляем тестовые устройства
                val testDevices = createTestDevices()
                testDevices.forEach { device ->
                    // ★★★★ СОХРАНЯЕМ И СИНХРОНИЗИРУЕМ ★★★★
                    deviceDao.insertDevice(device)
                    schemeSyncUseCase.syncSchemeOnDeviceSave(device)
                }
                Timber.d("DatabaseInitializer: создано ${testDevices.size} тестовых устройств")
            }
        }
    }

    private fun createTestDevices(): List<Device> {
        return listOf(
            // Манометры
            Device(
                type = "Манометр",
                name = "МП4-УУ2",
                manufacturer = "Теплоконтроль",
                inventoryNumber = "МН-001-2024",
                year = 2023,
                measurementLimit = "0-10 МПа",
                accuracyClass = 1.0,
                location = "Цех №1, линия 5",
                valveNumber = "КШ-12",
                status = "В работе",
                additionalInfo = "Установлен на паровом котле. Последняя поверка: 15.01.2024",
                photos = null
            ),
            Device(
                type = "Манометр",
                name = "Манометр электроконтактный ЭКМ-100",
                manufacturer = "Манометр-Сервис",
                inventoryNumber = "МН-002-2024",
                year = 2022,
                measurementLimit = "0-6 МПа",
                accuracyClass = 1.5,
                location = "Котельная №2",
                valveNumber = "ВК-7",
                status = "Испорчен",
                additionalInfo = "Требуется замена контактной группы. В ремонте с 10.01.2024",
                photos = null
            ),

            // Термометры
            Device(
                type = "Термометр",
                name = "Термометр биметаллический ТБ-60",
                manufacturer = "Термоприбор",
                inventoryNumber = "ТМ-101-2024",
                year = 2024,
                measurementLimit = "0-300 °C",
                accuracyClass = 1.5,
                location = "Реактор Р-3",
                valveNumber = null,
                status = "В работе",
                additionalInfo = "Контроль температуры в реакторе полимеризации",
                photos = null
            ),
            Device(
                type = "Термометр",
                name = "Термометр жидкостный ТЛ-4",
                manufacturer = "Промтеплоавтоматика",
                inventoryNumber = "ТМ-102-2024",
                year = 2021,
                measurementLimit = "-20...+50 °C",
                accuracyClass = 2.0,
                location = "Холодильная камера №3",
                valveNumber = null,
                status = "Хранение",
                additionalInfo = "Запасной термометр. Поверен до 12.2025",
                photos = null
            ),

            // Счетчики
            Device(
                type = "Счетчик",
                name = "Счетчик газа СГМН-1 G6",
                manufacturer = "Эльстер Газэлектроника",
                inventoryNumber = "СЧ-201-2024",
                year = 2023,
                measurementLimit = "0,06-10 м³/ч",
                accuracyClass = 1.5,
                location = "Газовый узел, цех №2",
                valveNumber = "ГЗ-4",
                status = "В работе",
                additionalInfo = "Учет газа на технологические нужды. Пломбирован 05.12.2023",
                photos = null
            ),
            Device(
                type = "Счетчик",
                name = "Счетчик воды ВСХН-25",
                manufacturer = "Тепловодомер",
                inventoryNumber = "СЧ-202-2024",
                year = 2020,
                measurementLimit = "0,15-2,5 м³/ч",
                accuracyClass = 2.0,
                location = "Водозабор, насосная №1",
                valveNumber = "ВВ-8",
                status = "Утерян",
                additionalInfo = "Вышел из строя после гидроудара. Списан 15.11.2023",
                photos = null
            ),

            // Клапаны
            Device(
                type = "Клапан",
                name = "Клапан предохранительный КПП-50",
                manufacturer = "Армалит",
                inventoryNumber = "КЛ-301-2024",
                year = 2022,
                measurementLimit = "10 МПа",
                accuracyClass = null,
                location = "Паровой котел ПК-3",
                valveNumber = "КП-3",
                status = "В работе",
                additionalInfo = "Настройка давления срабатывания: 8,5 МПа. Проверен 20.12.2023",
                photos = null
            ),

            // Задвижки
            Device(
                type = "Задвижка",
                name = "Задвижка клиновая 30с41нж Ду100",
                manufacturer = "Завод трубопроводной арматуры",
                inventoryNumber = "ЗД-401-2024",
                year = 2021,
                measurementLimit = "Ру16",
                accuracyClass = null,
                location = "Магистральный трубопровод",
                valveNumber = "МТ-12",
                status = "В работе",
                additionalInfo = "Основная запорная арматура. Требуется ревизия сальникового уплотнения",
                photos = null
            ),

            // Датчики
            Device(
                type = "Датчик",
                name = "Датчик давления ДД-10",
                manufacturer = "Сенсорные системы",
                inventoryNumber = "ДТ-501-2024",
                year = 2024,
                measurementLimit = "0-1 МПа",
                accuracyClass = 0.5,
                location = "Система автоматизации",
                valveNumber = null,
                status = "В работе",
                additionalInfo = "Преобразователь 4-20 мА. Подключен к ПЛК Siemens",
                photos = null
            ),
            Device(
                type = "Датчик",
                name = "Датчик уровня УД-2",
                manufacturer = "Уровеньприбор",
                inventoryNumber = "ДТ-502-2024",
                year = 2023,
                measurementLimit = "0-5 м",
                accuracyClass = 0.2,
                location = "Емкость Е-7",
                valveNumber = null,
                status = "Испорчен",
                additionalInfo = "Нестабильные показания. Отправлен в лабораторию 08.01.2024",
                photos = null
            ),

            // Преобразователи
            Device(
                type = "Преобразователь",
                name = "Преобразователь частоты ПЧ-75",
                manufacturer = "Данфосс",
                inventoryNumber = "ПР-601-2024",
                year = 2023,
                measurementLimit = "0-75 кВт",
                accuracyClass = 0.1,
                location = "Насосный агрегат НА-4",
                valveNumber = null,
                status = "В работе",
                additionalInfo = "Управление скоростью насоса. Установлен в шкафу управления",
                photos = null
            ),

            // Регуляторы
            Device(
                type = "Регулятор",
                name = "Регулятор температуры ТРМ-10",
                manufacturer = "Овен",
                inventoryNumber = "РГ-701-2024",
                year = 2022,
                measurementLimit = "0-400 °C",
                accuracyClass = 0.5,
                location = "Сушильная камера",
                valveNumber = null,
                status = "В работе",
                additionalInfo = "ПИД-регулятор с термопарой типа K",
                photos = null
            ),

            // Другое
            Device(
                type = "Другое",
                name = "Фильтр сетчатый ФС-50",
                manufacturer = "Фильтрон",
                inventoryNumber = "ДР-801-2024",
                year = 2021,
                measurementLimit = "50 мкм",
                accuracyClass = null,
                location = "Вход водоподготовки",
                valveNumber = "ФВ-2",
                status = "В работе",
                additionalInfo = "Требуется чистка раз в месяц. Последняя чистка: 28.12.2023",
                photos = null
            ),
            Device(
                type = "Другое",
                name = "Ресивер РД-500",
                manufacturer = "Пневмоаппарат",
                inventoryNumber = "ДР-802-2024",
                year = 2020,
                measurementLimit = "1 м³, 1 МПа",
                accuracyClass = null,
                location = "Пневмосистема",
                valveNumber = null,
                status = "Хранение",
                additionalInfo = "Запасной ресивер. Прошел гидроиспытания 15.11.2023",
                photos = null
            )
        )
    }
}