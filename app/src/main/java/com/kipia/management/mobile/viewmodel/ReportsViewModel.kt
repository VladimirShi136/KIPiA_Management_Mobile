package com.kipia.management.mobile.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    init { loadReports() }

    fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val devices = deviceRepository.getAllDevicesSync()
                val schemes = schemeRepository.getAllSchemes().first()

                val inWork    = devices.count { it.status == "В работе" }
                val inStorage = devices.count { it.status == "Хранение" }
                val lost      = devices.count { it.status == "Утерян" }
                val broken    = devices.count { it.status == "Испорчен" }
                val total     = devices.size

                _reports.value = listOf(
                    // 1. Сводка
                    SummaryReport(
                        totalDevices = total,
                        inWork = inWork,
                        inStorage = inStorage,
                        lost = lost,
                        broken = broken,
                        totalSchemes = schemes.size,
                        mostCommonLocation = devices
                            .groupingBy { it.location }
                            .eachCount()
                            .maxByOrNull { it.value }?.key,
                        mostCommonType = devices
                            .groupingBy { it.type }
                            .eachCount()
                            .maxByOrNull { it.value }?.key
                    ),
                    // 2. По статусам
                    StatusDistributionReport(
                        statuses = mapOf(
                            "В работе"  to inWork,
                            "Хранение"  to inStorage,
                            "Утерян"    to lost,
                            "Испорчен"  to broken
                        ),
                        total = total
                    ),
                    // 3. По локациям
                    LocationDistributionReport(
                        locations = devices
                            .groupingBy { it.location }
                            .eachCount()
                            .toSortedMap(),
                        total = total
                    ),
                    // 4. По типам
                    TypeDistributionReport(
                        types = devices
                            .groupingBy { it.type }
                            .eachCount()
                            .toSortedMap(),
                        total = total
                    ),
                    // 5. Требуют внимания
                    NeedsAttentionReport(
                        lostDevices = devices
                            .filter { it.status == "Утерян" }
                            .map { DeviceInfo(it.id, it.getDisplayName(), it.inventoryNumber, it.location, it.status) },
                        brokenDevices = devices
                            .filter { it.status == "Испорчен" }
                            .map { DeviceInfo(it.id, it.getDisplayName(), it.inventoryNumber, it.location, it.status) }
                    )
                )
            } catch (_: Exception) {
                _reports.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun openReport(report: Report) { _currentReport.value = report }
    fun closeReport() { _currentReport.value = null }
}