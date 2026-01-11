package com.kipia.management.mobile.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.ui.screens.reports.components.*
import com.kipia.management.mobile.ui.screens.reports.models.*
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    report: Report,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(report.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            ReportDetailContent(
                report = report,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun ReportDetailContent(
    report: Report,
    modifier: Modifier = Modifier
) {
    when (report) {
        is SummaryReport -> SummaryReportContent(report)
        is DeviceListReport -> DeviceListReportContent(report)
        is StatusDistributionReport -> DistributionReportContent(report)
        is LocationDistributionReport -> DistributionReportContent(report)
        is TypeDistributionReport -> DistributionReportContent(report)
    }
}

@Composable
fun SummaryReportContent(report: SummaryReport) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Статистика приборов
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Статистика приборов",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Показатели в виде карточек
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatisticCard(
                        title = "Всего приборов",
                        value = report.totalDevices.toString(),
                        icon = Icons.Default.Devices,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )

                    StatisticCard(
                        title = "В работе",
                        value = report.activeDevices.toString(),
                        icon = Icons.Default.CheckCircle,
                        color = Color(0xFF4CAF50), // Green
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatisticCard(
                        title = "Неактивные",
                        value = report.offlineDevices.toString(),
                        icon = Icons.Default.Warning,
                        color = Color(0xFFF44336), // Red
                        modifier = Modifier.weight(1f)
                    )

                    StatisticCard(
                        title = "Средняя точность",
                        value = report.avgAccuracy?.let {
                            DecimalFormat("#.##").format(it)
                        } ?: "Н/Д",
                        icon = Icons.Default.PrecisionManufacturing,
                        color = Color(0xFF2196F3), // Blue
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Статистика схем
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Статистика схем",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                StatisticCard(
                    title = "Всего схем",
                    value = report.totalSchemes.toString(),
                    icon = Icons.Default.Map,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Самые частые значения
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Самые частые значения",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MostCommonItem(
                        label = "Самое частое место",
                        value = report.mostCommonLocation ?: "Нет данных",
                        icon = Icons.Default.LocationOn
                    )

                    MostCommonItem(
                        label = "Самый частый тип",
                        value = report.mostCommonType ?: "Нет данных",
                        icon = Icons.Default.Category
                    )
                }
            }
        }

        // Процентные соотношения
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Процентное соотношение",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                val activePercentage = if (report.totalDevices > 0) {
                    report.activeDevices.toFloat() / report.totalDevices
                } else 0f

                val offlinePercentage = if (report.totalDevices > 0) {
                    report.offlineDevices.toFloat() / report.totalDevices
                } else 0f

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PercentageBar(
                        label = "В работе",
                        percentage = activePercentage,
                        color = Color(0xFF4CAF50),
                        value = "${report.activeDevices} из ${report.totalDevices}"
                    )

                    PercentageBar(
                        label = "Неактивные",
                        percentage = offlinePercentage,
                        color = Color(0xFFF44336),
                        value = "${report.offlineDevices} из ${report.totalDevices}"
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceListReportContent(report: DeviceListReport) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Заголовок
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Список приборов",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = report.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Всего приборов: ${report.devices.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Список приборов
        items(report.devices, key = { it.id }) { device ->
            DeviceReportItem(device = device)
        }
    }
}

@Composable
fun DistributionReportContent(report: Report) {
    when (report) {
        is StatusDistributionReport -> GenericDistributionReport(
            report = report,
            title = "Распределение по статусам",
            data = report.statuses,
            total = report.total
        )
        is LocationDistributionReport -> GenericDistributionReport(
            report = report,
            title = "Распределение по местам",
            data = report.locations,
            total = report.total
        )
        is TypeDistributionReport -> GenericDistributionReport(
            report = report,
            title = "Распределение по типам",
            data = report.types,
            total = report.total
        )
        else -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Неизвестный тип отчета")
            }
        }
    }
}

@Composable
fun <T> GenericDistributionReport(
    report: Report,
    title: String,
    data: Map<T, Int>,
    total: Int
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Заголовок
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = report.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Всего записей: $total",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Круговая диаграмма (упрощенная)
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Диаграмма распределения",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Простая круговая диаграмма
                SimplePieChart(
                    data = data.map { (label, value) ->
                        ChartData(
                            label = label.toString(),
                            value = value,
                            color = getColorForIndex(data.keys.indexOf(label)),
                            percentage = value.toFloat() / total
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        // Таблица распределения
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Детальное распределение",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                data.entries.sortedByDescending { it.value }.forEach { (label, count) ->
                    val percentage = if (total > 0) {
                        (count.toFloat() / total * 100).toInt()
                    } else 0

                    DistributionRow(
                        label = label.toString(),
                        count = count,
                        percentage = percentage,
                        color = getColorForIndex(data.keys.indexOf(label)),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}