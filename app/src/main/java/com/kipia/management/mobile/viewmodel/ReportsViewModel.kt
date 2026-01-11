package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.repository.DeviceRepository
import com.kipia.management.mobile.repository.SchemeRepository
import com.kipia.management.mobile.ui.screens.reports.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadReports()
    }

    private fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // Загружаем данные для отчетов
                val devices = deviceRepository.getAllDevicesSync()

                // Получаем схемы из Flow
                val schemes = schemeRepository.getAllSchemes()
                    .first() // Берем первый элемент из Flow

                // Создаем стандартные отчеты
                val reportList = mutableListOf<Report>()

                // 1. Сводный отчет
                reportList.add(generateSummaryReport(devices, schemes))

                // 2. Список приборов
                reportList.add(generateDeviceListReport(devices))

                // 3. Распределение по статусам
                reportList.add(generateStatusDistributionReport(devices))

                // 4. Распределение по местам
                reportList.add(generateLocationDistributionReport(devices))

                // 5. Распределение по типам
                reportList.add(generateTypeDistributionReport(devices))

                _reports.value = reportList

            } catch (e: Exception) {
                _error.value = "Ошибка загрузки отчетов: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun generateSummaryReport(devices: List<Device>, schemes: List<Scheme>): SummaryReport {
        val totalDevices = devices.size
        val activeDevices = devices.count { it.status == "В работе" }
        val offlineDevices = devices.count { it.status in listOf("На ремонте", "Списан", "В резерве") }
        val totalSchemes = schemes.size

        val avgAccuracy = devices.mapNotNull { it.accuracyClass }
            .takeIf { it.isNotEmpty() }
            ?.average()

        // Самое частое место
        val mostCommonLocation = devices
            .groupingBy { it.location ?: "Не указано" }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        // Самый частый тип
        val mostCommonType = devices
            .groupingBy { it.type ?: "Не указан" }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        return SummaryReport(
            totalDevices = totalDevices,
            activeDevices = activeDevices,
            offlineDevices = offlineDevices,
            totalSchemes = totalSchemes,
            avgAccuracy = avgAccuracy,
            mostCommonLocation = mostCommonLocation,
            mostCommonType = mostCommonType
        )
    }

    private fun generateDeviceListReport(devices: List<Device>): DeviceListReport {
        return DeviceListReport(
            devices = devices.sortedBy { it.name ?: "" },
            sortBy = SortBy.NAME_ASC
        )
    }

    private fun generateStatusDistributionReport(devices: List<Device>): StatusDistributionReport {
        val statuses = devices
            .groupingBy { it.status ?: "Не указан" }
            .eachCount()
            .toSortedMap()

        return StatusDistributionReport(
            statuses = statuses,
            total = devices.size
        )
    }

    private fun generateLocationDistributionReport(devices: List<Device>): LocationDistributionReport {
        val locations = devices
            .groupingBy { it.location ?: "Не указано" }
            .eachCount()
            .toSortedMap()

        return LocationDistributionReport(
            locations = locations,
            total = devices.size
        )
    }

    private fun generateTypeDistributionReport(devices: List<Device>): TypeDistributionReport {
        val types = devices
            .groupingBy { it.type ?: "Не указан" }
            .eachCount()
            .toSortedMap()

        return TypeDistributionReport(
            types = types,
            total = devices.size
        )
    }

    fun openReport(report: Report) {
        _currentReport.value = report
    }

    fun closeReport() {
        _currentReport.value = null
    }

    fun refreshReports() {
        loadReports()
    }

    fun clearError() {
        _error.value = null
    }

    // Обновленная generateCustomReport функция
    suspend fun generateCustomReport(
        devices: List<Device>,
        type: ReportType,
        filter: String? = null
    ): Report {
        // Получаем схемы для summary отчета
        val schemes = schemeRepository.getAllSchemes().first()

        return when (type) {
            is ReportType.Summary -> generateSummaryReport(devices, schemes)
            is ReportType.DeviceList -> DeviceListReport(
                devices = devices.filter {
                    filter == null || (it.name?.contains(filter, ignoreCase = true) == true)
                }
            )
            is ReportType.StatusDistribution -> generateStatusDistributionReport(devices)
            is ReportType.LocationDistribution -> generateLocationDistributionReport(devices)
            is ReportType.TypeDistribution -> generateTypeDistributionReport(devices)
        }
    }

    // Альтернативный вариант с Flow для реального времени
    fun getReportsFlow(): Flow<List<Report>> {
        return combine(
            deviceRepository.getAllDevices(), // если есть Flow версия
            schemeRepository.getAllSchemes()
        ) { devices, schemes ->
            val reportList = mutableListOf<Report>()

            // 1. Сводный отчет
            reportList.add(generateSummaryReport(devices, schemes))

            // 2. Список приборов
            reportList.add(generateDeviceListReport(devices))

            // 3. Распределение по статусам
            reportList.add(generateStatusDistributionReport(devices))

            // 4. Распределение по местам
            reportList.add(generateLocationDistributionReport(devices))

            // 5. Распределение по типам
            reportList.add(generateTypeDistributionReport(devices))

            reportList
        }
    }
}