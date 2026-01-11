package com.kipia.management.mobile.ui.screens.reports.models

import androidx.compose.ui.graphics.Color
import com.kipia.management.mobile.data.entities.Device

// Типы отчетов (как в JavaFX)
sealed class ReportType {
    object Summary : ReportType()            // Сводный отчет
    object DeviceList : ReportType()         // Список приборов
    object StatusDistribution : ReportType() // Распределение по статусам
    object LocationDistribution : ReportType() // Распределение по местам
    object TypeDistribution : ReportType()   // Распределение по типам
}

// Базовый класс отчета
sealed class Report(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: ReportType,
    val createdAt: Long = System.currentTimeMillis()
)

// Сводный отчет
data class SummaryReport(
    val totalDevices: Int,
    val activeDevices: Int,
    val offlineDevices: Int,
    val totalSchemes: Int,
    val avgAccuracy: Double?,
    val mostCommonLocation: String?,
    val mostCommonType: String?
) : Report(
    id = "summary_${System.currentTimeMillis()}",
    title = "Сводный отчет",
    subtitle = "Общая статистика по системе",
    type = ReportType.Summary
)

// Отчет "Список приборов"
data class DeviceListReport(
    val devices: List<Device>,
    val filter: String? = null,
    val sortBy: SortBy = SortBy.NAME_ASC
) : Report(
    id = "devices_${System.currentTimeMillis()}",
    title = "Список приборов",
    subtitle = filter?.let { "Фильтр: $it" } ?: "Все приборы",
    type = ReportType.DeviceList
)

// Отчет "Распределение по статусам"
data class StatusDistributionReport(
    val statuses: Map<String, Int>, // Статус -> Количество
    val total: Int
) : Report(
    id = "status_${System.currentTimeMillis()}",
    title = "Распределение по статусам",
    subtitle = "Статистика по состояниям приборов",
    type = ReportType.StatusDistribution
)

// Отчет "Распределение по местам"
data class LocationDistributionReport(
    val locations: Map<String, Int>, // Место -> Количество
    val total: Int
) : Report(
    id = "location_${System.currentTimeMillis()}",
    title = "Распределение по местам",
    subtitle = "Статистика по местам установки",
    type = ReportType.LocationDistribution
)

// Отчет "Распределение по типам"
data class TypeDistributionReport(
    val types: Map<String, Int>, // Тип -> Количество
    val total: Int
) : Report(
    id = "type_${System.currentTimeMillis()}",
    title = "Распределение по типам",
    subtitle = "Статистика по типам приборов",
    type = ReportType.TypeDistribution
)

// Параметры сортировки
enum class SortBy {
    NAME_ASC, NAME_DESC,
    LOCATION_ASC, LOCATION_DESC,
    TYPE_ASC, TYPE_DESC,
    STATUS_ASC, STATUS_DESC
}

// Модель для диаграммы
data class ChartData(
    val label: String,
    val value: Int,
    val color: Color,
    val percentage: Float // 0-1
)

// Модель для статистики
data class StatisticItem(
    val label: String,
    val value: String,
    val iconResId: Int? = null,
    val color: Color = Color.Unspecified
)