package com.kipia.management.mobile.ui.screens.reports

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipia.management.mobile.ui.components.topappbar.TopAppBarController
import com.kipia.management.mobile.ui.screens.reports.models.*
import com.kipia.management.mobile.ui.theme.Dimens
import com.kipia.management.mobile.viewmodel.ReportsViewModel

@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel(),
    topAppBarController: TopAppBarController? = null
) {
    val reports by viewModel.reports.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val filterOptions by viewModel.filterOptions.collectAsStateWithLifecycle()

    // Стабильные значения для LaunchedEffect
    val filterKey = remember(filter) {
        "${filter.status}_${filter.deviceType}_${filter.manufacturer}_${filter.location}_${filter.releaseYear}"
    }

    val filterOptionsKey = remember(filterOptions) {
        "${filterOptions.statuses.size}_${filterOptions.types.size}_${filterOptions.manufacturers.size}_${filterOptions.locations.size}_${filterOptions.years.size}"
    }

    // Настройка TopAppBar - используем стабильные ключи
    LaunchedEffect(filterKey, filterOptionsKey) {
        topAppBarController?.setForScreen(
            "reports_with_filter",
            mapOf(
                "title" to "Учет приборов КИПиА",
                "showBackButton" to false,
                "reportFilter" to filter,
                "availableStatuses" to filterOptions.statuses,
                "availableTypes" to filterOptions.types,
                "availableManufacturers" to filterOptions.manufacturers,
                "availableLocations" to filterOptions.locations,
                "availableYears" to filterOptions.years,
                "onFilterChange" to { newFilter: ReportFilter -> viewModel.setFilter(newFilter) }
            )
        )
    }

    // Показываем только сводный отчет (SummaryReport)
    val summaryReport = remember(reports) {
        reports.find { it is SummaryReport } as? SummaryReport
    }

    ReportsListContent(
        summaryReport = summaryReport,
        isLoading = isLoading,
        filter = filter,
        onRefresh = { viewModel.loadReports() },
        onClearFilter = { viewModel.clearFilter() }
    )
}

@Composable
private fun ReportsListContent(
    summaryReport: SummaryReport?,
    isLoading: Boolean,
    filter: ReportFilter,
    onRefresh: () -> Unit,
    onClearFilter: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            summaryReport == null -> EmptyReportsPlaceholder(
                onRefresh = onRefresh,
                hasFilter = !filter.isEmpty,
                onClearFilter = onClearFilter
            )

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Активные фильтры (как в SchemesScreen)
                    ActiveFiltersBadge(
                        filter = filter,
                        onClearFilters = onClearFilter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.screenPadding, vertical = Dimens.screenPadding)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Dimens.screenPadding),
                        contentPadding = PaddingValues(Dimens.screenPadding)
                    ) {
                        item {
                            SummaryReportCard(report = summaryReport)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFiltersBadge(
    filter: ReportFilter,
    onClearFilters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasActiveFilters = !filter.isEmpty

    if (hasActiveFilters) {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(Dimens.chipRadius),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spacingMedium),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FilterAlt,
                        contentDescription = null,
                        modifier = Modifier.size(Dimens.iconSizeXSmall),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(Dimens.spacingMedium))

                    Text(
                        text = buildActiveFiltersText(filter),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onClearFilters,
                    modifier = Modifier.size(Dimens.iconSizeSmall + 6.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Очистить фильтры",
                        modifier = Modifier.size(Dimens.iconSizeXSmall),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private fun buildActiveFiltersText(filter: ReportFilter): String {
    val filters = mutableListOf<String>()

    filter.status?.let { filters.add("Статус: $it") }
    filter.deviceType?.let { filters.add("Тип: $it") }
    filter.manufacturer?.let { filters.add("Производитель: $it") }
    filter.location?.let { filters.add("Локация: $it") }
    filter.releaseYear?.let { filters.add("Год: $it") }

    return if (filters.isEmpty()) {
        "Нет активных фильтров"
    } else {
        "Фильтры: ${filters.joinToString(", ")}"
    }
}

@Composable
private fun SummaryReportCard(report: SummaryReport) {
    val slices = listOf(
        DonutSliceData("В работе", report.inWork.toFloat(), Color(0xFF4CAF50)),
        DonutSliceData("Хранение", report.inStorage.toFloat(), Color(0xFFF58352)),
        DonutSliceData("Утерян", report.lost.toFloat(), Color(0xFF9E9E9E)),
        DonutSliceData("Испорчен", report.broken.toFloat(), Color(0xFFF44336))
    ).filter { it.value > 0f }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.spacingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Заголовок
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Assessment,
                    null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(Dimens.iconSizeMedium)
                )
                Spacer(modifier = Modifier.width(Dimens.spacingMedium))
                Text(
                    "Сводка по приборам",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(Dimens.spacingLarge))

            // Большая диаграмма
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                DonutChart(
                    slices = slices,
                    total = report.totalDevices.toFloat(),
                    strokeWidth = 24f
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${report.totalDevices}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "всего приборов",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(Dimens.spacingXLarge))

            // Легенда в виде сетки
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
                ) {
                    LegendChip("В работе", report.inWork, Color(0xFF4CAF50), Modifier.weight(1f))
                    LegendChip("Хранение", report.inStorage, Color(0xFFF58352), Modifier.weight(1f))
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spacingMedium)
                ) {
                    LegendChip("Утерян", report.lost, Color(0xFF9E9E9E), Modifier.weight(1f))
                    LegendChip("Испорчен", report.broken, Color(0xFFF44336), Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DonutChart(
    slices: List<DonutSliceData>,
    total: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 22f,
    gapDeg: Float = 3f
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        if (total <= 0f || slices.isEmpty()) {
            drawArc(
                color = Color.Gray.copy(alpha = 0.15f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            return@Canvas
        }

        val totalGap = gapDeg * slices.size
        val availableDeg = 360f - totalGap
        var startAngle = -90f

        slices.forEach { slice ->
            val sweep = (slice.value / total) * availableDeg
            if (sweep > 0f) {
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                startAngle += sweep + gapDeg
            }
        }
    }
}

@Composable
private fun LegendChip(
    label: String,
    count: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Dimens.spacingMedium + 2.dp, vertical = Dimens.spacingSmall + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spacingSmall)
        ) {
            Canvas(Modifier.size(Dimens.spacingMedium)) { drawCircle(color) }
            Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
            Text(
                "$count",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun EmptyReportsPlaceholder(
    onRefresh: () -> Unit,
    hasFilter: Boolean,
    onClearFilter: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            if (hasFilter) Icons.Default.FilterAltOff else Icons.Default.Assessment,
            contentDescription = null,
            modifier = Modifier.size(Dimens.iconSizeXXLarge),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Dimens.spacingLarge))
        Text(
            if (hasFilter) "Нет данных по выбранному фильтру" else "Нет данных для отчётов",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Dimens.spacingLarge))
        Button(onClick = onRefresh) { Text("Обновить") }
        if (hasFilter) {
            Spacer(Modifier.height(Dimens.spacingMedium))
            TextButton(onClick = onClearFilter) {
                Text("Сбросить фильтр")
            }
        }
    }
}

private data class DonutSliceData(val label: String, val value: Float, val color: Color)