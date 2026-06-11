package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.SchemeRepository
import com.kipia.management.mobile.ui.screens.reports.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val deviceRepository: DeviceRepository,
    private val schemeRepository: SchemeRepository
) : ViewModel() {

    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> = _reports.asStateFlow()

    private val _currentReport = MutableStateFlow<Report?>(null)
    val currentReport: StateFlow<Report?> = _currentReport.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _filter = MutableStateFlow(ReportFilter.Empty)
    val filter: StateFlow<ReportFilter> = _filter.asStateFlow()

    private val _filterOptions = MutableStateFlow(ReportFilterOptions())
    val filterOptions: StateFlow<ReportFilterOptions> = _filterOptions.asStateFlow()

    private val MIN_LOADING_TIME = 600L

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()
            try {
                val devices = deviceRepository.getAllDevicesSync()
                val schemes = schemeRepository.getAllSchemes().first()

                val deviceInfoList = devices.map { device ->
                    DeviceInfo(
                        id = device.id,
                        displayName = device.getDisplayName(),
                        inventoryNumber = device.inventoryNumber,
                        location = device.location,
                        status = device.status,
                        type = device.type,
                        manufacturer = device.manufacturer ?: "",
                        releaseYear = device.year
                    )
                }

                _reports.value = buildReportsFromDevices(deviceInfoList, schemes.size)
                _filterOptions.value = buildFilterOptions(deviceInfoList)
            } catch (_: Exception) {
                _reports.value = emptyList()
            } finally {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < MIN_LOADING_TIME) delay(MIN_LOADING_TIME - elapsed)
                _isLoading.value = false
            }
        }
    }

    fun loadFilteredReports(filter: ReportFilter) {
        viewModelScope.launch {
            _isLoading.value = true
            val startTime = System.currentTimeMillis()
            try {
                val devices = deviceRepository.getAllDevicesSync()
                val schemes = schemeRepository.getAllSchemes().first()

                val filteredDevices = devices.filter { device ->
                    (filter.status == null || device.status == filter.status) &&
                            (filter.deviceType == null || device.type == filter.deviceType) &&
                            (filter.manufacturer == null || device.manufacturer == filter.manufacturer) &&
                            (filter.location == null || device.location == filter.location) &&
                            (filter.releaseYear == null || device.year == filter.releaseYear)
                }

                val deviceInfoList = filteredDevices.map { device ->
                    DeviceInfo(
                        id = device.id,
                        displayName = device.getDisplayName(),
                        inventoryNumber = device.inventoryNumber,
                        location = device.location,
                        status = device.status,
                        type = device.type,
                        manufacturer = device.manufacturer ?: "",
                        releaseYear = device.year
                    )
                }

                _reports.value = buildReportsFromDevices(deviceInfoList, schemes.size)
            } catch (_: Exception) {
                _reports.value = emptyList()
            } finally {
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < MIN_LOADING_TIME) delay(MIN_LOADING_TIME - elapsed)
                _isLoading.value = false
            }
        }
    }

    private fun buildReportsFromDevices(devices: List<DeviceInfo>, totalSchemes: Int): List<Report> {
        val inWork = devices.count { it.status == "В работе" }
        val inStorage = devices.count { it.status == "Хранение" }
        val lost = devices.count { it.status == "Утерян" }
        val broken = devices.count { it.status == "Испорчен" }
        val total = devices.size

        return listOf(
            SummaryReport(
                totalDevices = total,
                inWork = inWork,
                inStorage = inStorage,
                lost = lost,
                broken = broken,
                totalSchemes = totalSchemes,
                mostCommonLocation = devices.groupingBy { it.location }.eachCount().maxByOrNull { it.value }?.key,
                mostCommonType = devices.groupingBy { it.type }.eachCount().maxByOrNull { it.value }?.key
            ),
            StatusDistributionReport(
                statuses = mapOf(
                    "В работе" to inWork,
                    "Хранение" to inStorage,
                    "Утерян" to lost,
                    "Испорчен" to broken
                ),
                total = total
            ),
            LocationDistributionReport(
                locations = devices.groupingBy { it.location }.eachCount().toSortedMap(),
                total = total
            ),
            TypeDistributionReport(
                types = devices.groupingBy { it.type }.eachCount().toSortedMap(),
                total = total
            )
        )
    }

    private fun buildFilterOptions(devices: List<DeviceInfo>): ReportFilterOptions {
        return ReportFilterOptions(
            statuses = devices.map { it.status }.distinct().sorted(),
            types = devices.map { it.type }.filter { it.isNotEmpty() }.distinct().sorted(),
            manufacturers = devices.map { it.manufacturer }.filter { it.isNotEmpty() }.distinct().sorted(),
            locations = devices.map { it.location }.distinct().sorted(),
            years = devices.mapNotNull { it.releaseYear }.distinct().sortedDescending()
        )
    }

    fun openReport(report: Report) {
        _currentReport.value = report
    }

    fun closeReport() {
        _currentReport.value = null
    }

    fun setFilter(newFilter: ReportFilter) {
        _filter.value = newFilter
        if (newFilter.isEmpty) {
            loadReports()
        } else {
            loadFilteredReports(newFilter)
        }
    }

    fun clearFilter() {
        setFilter(ReportFilter.Empty)
    }
}

data class ReportFilterOptions(
    val statuses: List<String> = emptyList(),
    val types: List<String> = emptyList(),
    val manufacturers: List<String> = emptyList(),
    val locations: List<String> = emptyList(),
    val years: List<Int> = emptyList()
)
