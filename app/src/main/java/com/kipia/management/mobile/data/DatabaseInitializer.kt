package com.kipia.management.mobile.data

import com.google.gson.Gson
import com.kipia.management.mobile.data.database.AppDatabase
import com.kipia.management.mobile.data.entities.Device
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val database: AppDatabase
) {

    fun initialize() {
        CoroutineScope(Dispatchers.IO).launch {
            val deviceDao = database.deviceDao()
            val schemeDao = database.schemeDao()

            // Проверяем, пустая ли база устройств
            if (deviceDao.getAllDevicesSync().isEmpty()) {
                // Добавляем тестовые устройства
                val testDevices = createTestDevices()
                testDevices.forEach { deviceDao.insertDevice(it) }
            }

            // Проверяем, пустая ли база схем
            if (schemeDao.getAllSchemesSync().isEmpty()) {
                // Добавляем тестовые схемы
                val testSchemes = createTestSchemes()
                testSchemes.forEach { schemeDao.insertScheme(it) }
            }
        }
    }

    private fun createTestDevices(): List<Device> {
        return listOf(
            // Манометры
            Device(
                type = "Манометр",
                name = "Манометр общепромышленный МП-100",
                manufacturer = "Теплоконтроль",
                inventoryNumber = "МН-001-2024",
                year = 2023,
                measurementLimit = "0-10 МПа",
                accuracyClass = 1.0,
                location = "Цех №1, линия 5",
                valveNumber = "КШ-12",
                status = "В работе",
                additionalInfo = "Установлен на паровом котле. Последняя поверка: 15.01.2024",
                photoPath = null,
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
                status = "На ремонте",
                additionalInfo = "Требуется замена контактной группы. В ремонте с 10.01.2024",
                photoPath = null,
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
                photoPath = null,
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
                status = "В резерве",
                additionalInfo = "Запасной термометр. Поверен до 12.2025",
                photoPath = null,
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
                photoPath = null,
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
                status = "Списан",
                additionalInfo = "Вышел из строя после гидроудара. Списан 15.11.2023",
                photoPath = null,
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
                photoPath = null,
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
                photoPath = null,
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
                photoPath = null,
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
                status = "На ремонте",
                additionalInfo = "Нестабильные показания. Отправлен в лабораторию 08.01.2024",
                photoPath = null,
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
                photoPath = null,
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
                photoPath = null,
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
                photoPath = null,
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
                status = "В резерве",
                additionalInfo = "Запасной ресивер. Прошел гидроиспытания 15.11.2023",
                photoPath = null,
                photos = null
            )
        )
    }

    private fun createTestSchemes(): List<com.kipia.management.mobile.data.entities.Scheme> {
        val gson = Gson()

        return listOf(
            // Схема 1: Технологическая схема цеха №1
            com.kipia.management.mobile.data.entities.Scheme(
                name = "Технологическая схема цеха №1",
                description = "Основная схема оборудования цеха переработки. Включает реакторы, теплообменники и насосы.",
                data = gson.toJson(
                    com.kipia.management.mobile.data.entities.SchemeData(
                        width = 1200,
                        height = 800,
                        backgroundColor = "#FFFFFFFF",
                        gridEnabled = true,
                        gridSize = 50,
                        devices = listOf(
                            com.kipia.management.mobile.data.entities.SchemeDevice(
                                deviceId = 3, // Термометр ТБ-60 (из тестовых устройств)
                                x = 300f,
                                y = 200f,
                                rotation = 0f,
                                scale = 1.0f,
                                zIndex = 1
                            ),
                            com.kipia.management.mobile.data.entities.SchemeDevice(
                                deviceId = 7, // Клапан КПП-50
                                x = 500f,
                                y = 350f,
                                rotation = 90f,
                                scale = 0.8f,
                                zIndex = 2
                            )
                        ),
                        shapes = listOf(
                            // Реактор
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "rectangle",
                                id = "reactor_1",
                                x = 200f,
                                y = 150f,
                                width = 200f,
                                height = 300f,
                                fillColor = "#66ADD8E6", // Светло-голубой с прозрачностью
                                strokeColor = "#FF000080", // Темно-синий
                                strokeWidth = 3f,
                                properties = mapOf("cornerRadius" to 20f)
                            ),
                            // Трубопровод
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "line",
                                id = "pipeline_1",
                                x = 400f,
                                y = 300f,
                                width = 200f,
                                height = 0f,
                                strokeColor = "#FF000000", // Черный
                                strokeWidth = 4f,
                                properties = mapOf(
                                    "startX" to 0f,
                                    "startY" to 0f,
                                    "endX" to 200f,
                                    "endY" to 0f
                                )
                            ),
                            // Насос
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "ellipse",
                                id = "pump_1",
                                x = 600f,
                                y = 250f,
                                width = 100f,
                                height = 100f,
                                fillColor = "#66FFA500", // Оранжевый с прозрачностью
                                strokeColor = "#FFFF8C00", // Темно-оранжевый
                                strokeWidth = 2f
                            ),
                            // Текст "Реактор Р-3"
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "text",
                                id = "label_reactor",
                                x = 220f,
                                y = 470f,
                                width = 160f,
                                height = 30f,
                                fillColor = "#FF000000", // Черный
                                strokeWidth = 0f,
                                properties = mapOf(
                                    "text" to "Реактор Р-3",
                                    "fontSize" to 14f
                                )
                            )
                        )
                    )
                )
            ),

            // Схема 2: Трубопроводы котельной
            com.kipia.management.mobile.data.entities.Scheme(
                name = "Трубопроводы котельной",
                description = "Схема паровых и водяных трубопроводов котельной №2 с указанием запорной арматуры.",
                data = gson.toJson(
                    com.kipia.management.mobile.data.entities.SchemeData(
                        width = 1000,
                        height = 700,
                        backgroundColor = "#FFF5F5F5",
                        gridEnabled = true,
                        gridSize = 40,
                        devices = listOf(
                            com.kipia.management.mobile.data.entities.SchemeDevice(
                                deviceId = 1, // Манометр МП-100
                                x = 150f,
                                y = 100f,
                                rotation = 0f,
                                scale = 1.2f,
                                zIndex = 1
                            ),
                            com.kipia.management.mobile.data.entities.SchemeDevice(
                                deviceId = 8, // Задвижка 30с41нж
                                x = 700f,
                                y = 400f,
                                rotation = 45f,
                                scale = 1.0f,
                                zIndex = 2
                            )
                        ),
                        shapes = listOf(
                            // Котел
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "rectangle",
                                id = "boiler",
                                x = 100f,
                                y = 50f,
                                width = 150f,
                                height = 250f,
                                fillColor = "#66FF6347", // Томатный с прозрачностью
                                strokeColor = "#FF8B0000", // Темно-красный
                                strokeWidth = 3f
                            ),
                            // Паровая магистраль
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "line",
                                id = "steam_main",
                                x = 250f,
                                y = 150f,
                                width = 500f,
                                height = 0f,
                                strokeColor = "#FFFF4500", // Оранжево-красный
                                strokeWidth = 6f,
                                properties = mapOf(
                                    "startX" to 0f,
                                    "startY" to 0f,
                                    "endX" to 500f,
                                    "endY" to 0f
                                )
                            ),
                            // Задвижка (ромб)
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "rhombus",
                                id = "valve_1",
                                x = 450f,
                                y = 130f,
                                width = 40f,
                                height = 40f,
                                fillColor = "#6600CED1", // Бирюзовый с прозрачностью
                                strokeColor = "#FF008B8B", // Темно-бирюзовый
                                strokeWidth = 2f
                            ),
                            // Водяной трубопровод
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "line",
                                id = "water_pipe",
                                x = 300f,
                                y = 400f,
                                width = 400f,
                                height = 100f,
                                strokeColor = "#FF1E90FF", // DodgerBlue
                                strokeWidth = 4f,
                                properties = mapOf(
                                    "startX" to 0f,
                                    "startY" to 0f,
                                    "endX" to 400f,
                                    "endY" to 100f
                                )
                            ),
                            // Текст "Пар 10 атм"
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "text",
                                id = "steam_label",
                                x = 350f,
                                y = 120f,
                                width = 100f,
                                height = 25f,
                                fillColor = "#FF8B0000", // Темно-красный
                                strokeWidth = 0f,
                                properties = mapOf(
                                    "text" to "Пар 10 атм",
                                    "fontSize" to 12f
                                )
                            )
                        )
                    )
                )
            ),

            // Схема 3: Электрическая схема ЩУ-1
            com.kipia.management.mobile.data.entities.Scheme(
                name = "Электрическая схема ЩУ-1",
                description = "Принципиальная схема щита управления насосной станцией с датчиками и преобразователями.",
                data = gson.toJson(
                    com.kipia.management.mobile.data.entities.SchemeData(
                        width = 800,
                        height = 600,
                        backgroundColor = "#FFFFFFFF",
                        backgroundImage = null,
                        gridEnabled = false,
                        gridSize = 30,
                        devices = listOf(
                            com.kipia.management.mobile.data.entities.SchemeDevice(
                                deviceId = 9, // Датчик давления ДД-10
                                x = 200f,
                                y = 150f,
                                rotation = 0f,
                                scale = 0.9f,
                                zIndex = 1
                            ),
                            com.kipia.management.mobile.data.entities.SchemeDevice(
                                deviceId = 11, // Преобразователь ПЧ-75
                                x = 500f,
                                y = 300f,
                                rotation = 0f,
                                scale = 1.1f,
                                zIndex = 2
                            )
                        ),
                        shapes = listOf(
                            // Шкаф управления
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "rectangle",
                                id = "cabinet",
                                x = 50f,
                                y = 50f,
                                width = 700f,
                                height = 500f,
                                fillColor = "#66D3D3D3", // Светло-серый с прозрачностью
                                strokeColor = "#FF696969", // DimGray
                                strokeWidth = 4f
                            ),
                            // Автоматический выключатель
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "rectangle",
                                id = "breaker",
                                x = 100f,
                                y = 100f,
                                width = 80f,
                                height = 40f,
                                fillColor = "#66FFD700", // Золотой с прозрачностью
                                strokeColor = "#FFDAA520", // Goldenrod
                                strokeWidth = 2f
                            ),
                            // Контактор
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "ellipse",
                                id = "contactor",
                                x = 300f,
                                y = 200f,
                                width = 60f,
                                height = 60f,
                                fillColor = "#669ACD32", // Желто-зеленый с прозрачностью
                                strokeColor = "#FF6B8E23", // OliveDrab
                                strokeWidth = 2f
                            ),
                            // Проводка
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "line",
                                id = "wire_1",
                                x = 180f,
                                y = 120f,
                                width = 120f,
                                height = 80f,
                                strokeColor = "#FF000000", // Черный
                                strokeWidth = 1.5f,
                                properties = mapOf(
                                    "startX" to 0f,
                                    "startY" to 0f,
                                    "endX" to 120f,
                                    "endY" to 80f
                                )
                            ),
                            // Текст "Щит управления"
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "text",
                                id = "title",
                                x = 300f,
                                y = 30f,
                                width = 200f,
                                height = 40f,
                                fillColor = "#FF000080", // Navy
                                strokeWidth = 0f,
                                properties = mapOf(
                                    "text" to "ЩИТ УПРАВЛЕНИЯ",
                                    "fontSize" to 18f
                                )
                            ),
                            // Текст "АВ-1"
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "text",
                                id = "breaker_label",
                                x = 105f,
                                y = 110f,
                                width = 70f,
                                height = 20f,
                                fillColor = "#FF000000", // Черный
                                strokeWidth = 0f,
                                properties = mapOf(
                                    "text" to "АВ-1",
                                    "fontSize" to 10f
                                )
                            )
                        )
                    )
                )
            ),

            // Схема 4: Водоснабжение насосной
            com.kipia.management.mobile.data.entities.Scheme(
                name = "Водоснабжение насосной №1",
                description = "Схема водопроводных сетей и оборудования насосной станции.",
                data = gson.toJson(
                    com.kipia.management.mobile.data.entities.SchemeData(
                        width = 900,
                        height = 650,
                        backgroundColor = "#FFE6F3FF",
                        gridEnabled = true,
                        gridSize = 25,
                        devices = listOf(
                            com.kipia.management.mobile.data.entities.SchemeDevice(
                                deviceId = 5, // Счетчик газа СГМН-1
                                x = 400f,
                                y = 200f,
                                rotation = 0f,
                                scale = 1.0f,
                                zIndex = 1
                            ),
                            com.kipia.management.mobile.data.entities.SchemeDevice(
                                deviceId = 13, // Фильтр ФС-50
                                x = 600f,
                                y = 350f,
                                rotation = 0f,
                                scale = 0.8f,
                                zIndex = 2
                            )
                        ),
                        shapes = listOf(
                            // Насосная станция
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "rectangle",
                                id = "pump_station",
                                x = 100f,
                                y = 100f,
                                width = 250f,
                                height = 150f,
                                fillColor = "#6687CEEB", // SkyBlue с прозрачностью
                                strokeColor = "#FF4682B4", // SteelBlue
                                strokeWidth = 3f,
                                properties = mapOf("cornerRadius" to 10f)
                            ),
                            // Водонапорная башня
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "ellipse",
                                id = "water_tower",
                                x = 650f,
                                y = 100f,
                                width = 120f,
                                height = 120f,
                                fillColor = "#66B0C4DE", // LightSteelBlue
                                strokeColor = "#FF778899", // LightSlateGray
                                strokeWidth = 3f
                            ),
                            // Трубопровод подачи
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "line",
                                id = "supply_pipe",
                                x = 350f,
                                y = 175f,
                                width = 300f,
                                height = 0f,
                                strokeColor = "#FF1E90FF", // DodgerBlue
                                strokeWidth = 5f,
                                properties = mapOf(
                                    "startX" to 0f,
                                    "startY" to 0f,
                                    "endX" to 300f,
                                    "endY" to 0f
                                )
                            ),
                            // Стрелка направления
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "rhombus",
                                id = "arrow",
                                x = 500f,
                                y = 170f,
                                width = 20f,
                                height = 30f,
                                fillColor = "#FFFF0000", // Красный
                                strokeColor = "#FF8B0000", // DarkRed
                                strokeWidth = 1f
                            ),
                            // Текст "Насосная"
                            com.kipia.management.mobile.data.entities.ShapeData(
                                type = "text",
                                id = "pump_label",
                                x = 150f,
                                y = 260f,
                                width = 150f,
                                height = 30f,
                                fillColor = "#FF000080", // Navy
                                strokeWidth = 0f,
                                properties = mapOf(
                                    "text" to "Насосная",
                                    "fontSize" to 16f
                                )
                            )
                        )
                    )
                )
            )
        )
    }
}