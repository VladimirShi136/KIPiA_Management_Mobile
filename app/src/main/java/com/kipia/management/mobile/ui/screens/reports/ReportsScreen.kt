package com.kipia.management.mobile.ui.screens.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.ui.screens.reports.models.*
import com.kipia.management.mobile.viewmodel.ReportsViewModel

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
    topAppBarController: TopAppBarController? = null
) {
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val currentReport by viewModel.currentReport.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(currentReport) {
        if (currentReport != null) {
            topAppBarController?.setForScreen(
                "report_detail",
                mapOf(
                    "title" to currentReport!!.title,
                    "onBackClick" to { viewModel.closeReport() }  // ← ключевое
                )
            )
        } else {
            topAppBarController?.setForScreen("reports")
        }
    }

    if (currentReport != null) {
        ReportDetailScreen(report = currentReport!!)   // без onBack — кнопка назад в TopAppBar
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                reports.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Assessment,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("Нет данных для отчётов",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadReports() }) {
                            Text("Обновить")
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        items(reports, key = { it.id }) { report ->
                            ReportCard(
                                report = report,
                                onClick = { viewModel.openReport(report) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportCard(report: Report, onClick: () -> Unit) {
    val (icon, color) = reportIconAndColor(report)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Иконка с цветным фоном
            Surface(
                color = color.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(report.title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(report.subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)

                // Превью данных
                Spacer(Modifier.height(4.dp))
                ReportCardPreview(report = report, color = color)
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReportCardPreview(report: Report, color: Color) {
    when (report) {
        is SummaryReport -> Text(
            "${report.totalDevices} приборов · ${report.totalSchemes} схем",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        is StatusDistributionReport -> Text(
            "В работе: ${report.statuses["В работе"] ?: 0} · Проблемных: ${(report.statuses["Утерян"] ?: 0) + (report.statuses["Испорчен"] ?: 0)}",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        is LocationDistributionReport -> Text(
            "${report.locations.size} локаций · ${report.total} приборов",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        is TypeDistributionReport -> Text(
            "${report.types.size} типов · ${report.total} приборов",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
        is NeedsAttentionReport -> {
            val count = report.lostDevices.size + report.brokenDevices.size
            Text(
                if (count == 0) "Всё в порядке ✓"
                else "$count приборов требуют внимания",
                style = MaterialTheme.typography.labelSmall,
                color = if (count == 0) Color(0xFF4CAF50) else color
            )
        }
    }
}

private fun reportIconAndColor(report: Report): Pair<ImageVector, Color> = when (report) {
    is SummaryReport -> Icons.Default.Assessment to Color(0xFF2196F3)
    is StatusDistributionReport -> Icons.Default.PieChart to Color(0xFF4CAF50)
    is LocationDistributionReport -> Icons.Default.LocationOn to Color(0xFFFF9800)
    is TypeDistributionReport -> Icons.Default.Category to Color(0xFF9C27B0)
    is NeedsAttentionReport -> Icons.Default.Warning to Color(0xFFF44336)
}