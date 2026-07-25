package com.kipia.management.mobile.ui.components.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kipia.management.mobile.ui.screens.reports.models.ReportFilter

@Composable
fun ReportsFilterMenu(
    filter: ReportFilter,
    availableStatuses: List<String>,
    availableTypes: List<String>,
    availableManufacturers: List<String>,
    availableLocations: List<String>,
    availableYears: List<Int>,
    onFilterChange: (ReportFilter) -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = Color.White
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                Icons.Default.FilterAlt,
                contentDescription = "Фильтры отчёта",
                tint = contentColor
            )

            Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.offset(x = 8.dp, y = (-8).dp)
            ) {
                if (filter.activeCount > 0) {
                    Text(
                        text = filter.activeCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                } else {
                    Text(
                        text = "",
                        fontSize = 0.sp,
                        modifier = Modifier.padding(0.dp)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 0.dp),
            modifier = Modifier.width(320.dp)
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Фильтры отчёта",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (filter.activeCount > 0) {
                            AssistChip(
                                onClick = {},
                                label = {
                                    Text(
                                        "Активно: ${filter.activeCount}",
                                        fontSize = 11.sp
                                    )
                                },
                                enabled = false,
                                modifier = Modifier
                            )
                        }
                    }
                },
                onClick = {}
            )

            HorizontalDivider()

            SimpleSubFilterMenuItem(
                label = "Статус",
                icon = Icons.Default.Flag,
                currentValue = filter.status,
                options = availableStatuses,
                allLabel = "Все статусы",
                onSelected = { onFilterChange(filter.copy(status = it)) },
                onCloseParent = { expanded = false }
            )

            SimpleSubFilterMenuItem(
                label = "Тип прибора",
                icon = Icons.Default.Category,
                currentValue = filter.deviceType,
                options = availableTypes,
                allLabel = "Все типы",
                onSelected = { onFilterChange(filter.copy(deviceType = it)) },
                onCloseParent = { expanded = false }
            )

            SimpleSubFilterMenuItem(
                label = "Производитель",
                icon = Icons.Default.Business,
                currentValue = filter.manufacturer,
                options = availableManufacturers,
                allLabel = "Все производители",
                onSelected = { onFilterChange(filter.copy(manufacturer = it)) },
                onCloseParent = { expanded = false }
            )

            SimpleSubFilterMenuItem(
                label = "Местоположение",
                icon = Icons.Default.LocationOn,
                currentValue = filter.location,
                options = availableLocations,
                allLabel = "Все локации",
                onSelected = { onFilterChange(filter.copy(location = it)) },
                onCloseParent = { expanded = false }
            )

            SimpleSubFilterMenuItem(
                label = "Год выпуска",
                icon = Icons.Default.CalendarMonth,
                currentValue = filter.releaseYear?.toString(),
                options = availableYears.map { it.toString() },
                allLabel = "Все годы",
                onSelected = {
                    onFilterChange(filter.copy(releaseYear = it?.toIntOrNull()))
                },
                onCloseParent = { expanded = false }
            )

            HorizontalDivider()

            DropdownMenuItem(
                text = {
                    Text(
                        text = "Сбросить все фильтры",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                enabled = !filter.isEmpty,
                onClick = {
                    onFilterChange(ReportFilter.Empty)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun SimpleSubFilterMenuItem(
    label: String,
    icon: ImageVector,
    currentValue: String?,
    options: List<String>,
    allLabel: String,
    onSelected: (String?) -> Unit,
    onCloseParent: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {

                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f)
                )

                if (currentValue != null) {
                    Text(
                        text = currentValue.take(20) +
                                if (currentValue.length > 20) "…" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        },
        onClick = {
            if (options.isNotEmpty()) {
                expanded = !expanded
            }
        },
        enabled = options.isNotEmpty(),
        trailingIcon = {
            if (options.isNotEmpty()) {
                Icon(
                    if (expanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = null
                )
            } else {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null
                )
            }
        }
    )

    if (expanded) {

        DropdownMenuItem(
            text = {
                Text(
                    text = allLabel,
                    modifier = Modifier.padding(start = 32.dp),
                    fontWeight =
                        if (currentValue == null)
                            FontWeight.Bold
                        else
                            FontWeight.Normal
                )
            },
            onClick = {
                onSelected(null)
                expanded = false
                onCloseParent()
            }
        )

        HorizontalDivider()

        options.forEach { option ->

            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = option,
                            modifier = Modifier.weight(1f),
                            fontWeight =
                                if (currentValue == option)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal
                        )

                        if (currentValue == option) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                onClick = {
                    onSelected(option)
                    expanded = false
                    onCloseParent()
                }
            )
        }

        HorizontalDivider()
    }
}
