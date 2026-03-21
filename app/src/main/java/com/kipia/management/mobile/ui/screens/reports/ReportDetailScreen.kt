package com.kipia.management.mobile.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.ui.screens.reports.models.*

@Composable
fun ReportDetailScreen(report: Report) {
    when (report) {
        is SummaryReport -> SummaryReportDetail(report)
        is StatusDistributionReport -> DistributionDetail(
            items = report.statuses.map { (label, count) ->
                DistributionItem(label, count, report.total, statusColor(label))
            }.sortedByDescending { it.count }
        )

        is LocationDistributionReport -> DistributionDetail(
            items = report.locations.map { (label, count) ->
                DistributionItem(label, count, report.total, Color(0xFFFF9800))
            }.sortedByDescending { it.count }
        )

        is TypeDistributionReport -> DistributionDetail(
            items = report.types.map { (label, count) ->
                DistributionItem(label, count, report.total, Color(0xFF9C27B0))
            }.sortedByDescending { it.count }
        )

        is NeedsAttentionReport -> NeedsAttentionDetail(report)
    }
}

// ── Сводка ───────────────────────────────────────────────────────────────────

@Composable
private fun SummaryReportDetail(report: SummaryReport) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            // Статусы — 4 карточки 2x2
            SectionCard(title = "Приборы по статусам") {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusMiniCard(
                        "В работе",
                        report.inWork,
                        Color(0xFF4CAF50),
                        Modifier.weight(1f)
                    )
                    StatusMiniCard(
                        "Хранение",
                        report.inStorage,
                        Color(0xFFF58352),
                        Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusMiniCard("Утерян", report.lost, Color(0xFF9E9E9E), Modifier.weight(1f))
                    StatusMiniCard(
                        "Испорчен",
                        report.broken,
                        Color(0xFFF44336),
                        Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            SectionCard(title = "Общая информация") {
                InfoRow(Icons.Default.Devices, "Всего приборов", "${report.totalDevices}")
                InfoRow(Icons.Default.Map, "Схем локаций", "${report.totalSchemes}")
                if (report.mostCommonLocation != null)
                    InfoRow(Icons.Default.LocationOn, "Топ локация", report.mostCommonLocation)
                if (report.mostCommonType != null)
                    InfoRow(Icons.Default.Category, "Топ тип", report.mostCommonType)
            }
        }
    }
}

// ── Распределение (локации / типы / статусы) ─────────────────────────────────

@Composable
private fun DistributionDetail(items: List<DistributionItem>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            DistributionItemRow(item)
        }
    }
}

@Composable
private fun DistributionItemRow(item: DistributionItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${item.count} (${item.percentageInt}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = item.color
                )
            }
            // Прогресс-бар
            LinearProgressIndicator(
                progress = { item.percentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = item.color,
                trackColor = item.color.copy(alpha = 0.15f)
            )
        }
    }
}

// ── Требуют внимания ─────────────────────────────────────────────────────────

@Composable
private fun NeedsAttentionDetail(report: NeedsAttentionReport) {
    val allDevices = remember(report) {
        report.lostDevices + report.brokenDevices
    }

    if (allDevices.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CheckCircle, null,
                    modifier = Modifier.size(64.dp), tint = Color(0xFF4CAF50)
                )
                Spacer(Modifier.height(12.dp))
                Text("Всё в порядке", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Нет утерянных или испорченных приборов",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (report.lostDevices.isNotEmpty()) {
            item {
                Text(
                    "Утерянные — ${report.lostDevices.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(report.lostDevices, key = { "lost_${it.id}" }) { device ->
                AttentionDeviceCard(device, Color(0xFF9E9E9E))
            }
        }

        if (report.brokenDevices.isNotEmpty()) {
            item {
                Text(
                    "Испорченные — ${report.brokenDevices.size}",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFFF44336),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(report.brokenDevices, key = { "broken_${it.id}" }) { device ->
                AttentionDeviceCard(device, Color(0xFFF44336))
            }
        }
    }
}

// ── Вспомогательные composable ───────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text(
                title, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun StatusMiniCard(label: String, count: Int, color: Color, modifier: Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "$count", style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold, color = color
            )
            Text(
                label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String, value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            icon, null, modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            label, style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AttentionDeviceCard(device: DeviceInfo, color: Color) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Warning, null,
                        tint = color, modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.displayName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${device.inventoryNumber} · ${device.location}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun statusColor(status: String) = when (status) {
    "В работе" -> Color(0xFF4CAF50)
    "Хранение" -> Color(0xFFF58352)
    "Утерян" -> Color(0xFF9E9E9E)
    "Испорчен" -> Color(0xFFF44336)
    else -> Color(0xFF607D8B)
}