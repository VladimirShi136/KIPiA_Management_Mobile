package com.kipia.management.mobile.ui.screens.reports.models

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

sealed class ReportType {
    object Summary : ReportType()
    object StatusDistribution : ReportType()
    object LocationDistribution : ReportType()
    object TypeDistribution : ReportType()
    object NeedsAttention : ReportType()
}

sealed class Report(
    val id: String,
    val title: String,
    val subtitle: String,
    val type: ReportType
)

@Immutable
data class SummaryReport(
    val totalDevices: Int,
    val inWork: Int,
    val inStorage: Int,
    val lost: Int,
    val broken: Int,
    val totalSchemes: Int,
    val mostCommonLocation: String?,
    val mostCommonType: String?
) : Report(
    id = "summary",
    title = "Сводка",
    subtitle = "Общая статистика",
    type = ReportType.Summary
)

@Immutable
data class StatusDistributionReport(
    val statuses: Map<String, Int>,
    val total: Int
) : Report(
    id = "status",
    title = "По статусам",
    subtitle = "Распределение по состояниям",
    type = ReportType.StatusDistribution
)

@Immutable
data class LocationDistributionReport(
    val locations: Map<String, Int>,
    val total: Int
) : Report(
    id = "location",
    title = "По локациям",
    subtitle = "Распределение по местам установки",
    type = ReportType.LocationDistribution
)

@Immutable
data class TypeDistributionReport(
    val types: Map<String, Int>,
    val total: Int
) : Report(
    id = "type",
    title = "По типам",
    subtitle = "Распределение по типам приборов",
    type = ReportType.TypeDistribution
)

@Immutable
data class NeedsAttentionReport(
    val lostDevices: List<DeviceInfo>,
    val brokenDevices: List<DeviceInfo>
) : Report(
    id = "attention",
    title = "Требуют внимания",
    subtitle = "Утерянные и испорченные приборы",
    type = ReportType.NeedsAttention
)

@Immutable
data class DeviceInfo(
    val id: Int,
    val displayName: String,
    val inventoryNumber: String,
    val location: String,
    val status: String
)

@Immutable
data class DistributionItem(
    val label: String,
    val count: Int,
    val total: Int,
    val color: Color
) {
    val percentage: Float get() = if (total > 0) count.toFloat() / total else 0f
    val percentageInt: Int get() = (percentage * 100).toInt()
}