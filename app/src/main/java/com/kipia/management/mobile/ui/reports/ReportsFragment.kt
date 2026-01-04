package com.kipia.management.mobile.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.kipia.management.mobile.R
import com.kipia.management.mobile.adapters.DeviceAdapter
import com.kipia.management.mobile.databinding.FragmentReportsBinding
import com.kipia.management.mobile.viewmodel.ReportsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReportsViewModel by viewModels()
    private lateinit var deviceAdapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFilterSpinners()
        setupListeners()
        setupPieChart()
        setupObservers()
    }

    private fun setupRecyclerView() {
        deviceAdapter = DeviceAdapter(
            onItemClick = { device ->
                // TODO: Переход к деталям прибора из отчетов
                // findNavController().navigate(...)
            }
        )

        binding.recyclerViewDevices.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
    }

    private fun setupFilterSpinners() {
        // Настройка спиннера для местоположений
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.availableLocations.collect { locations ->
                val locationAdapter = android.widget.ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf("Все местоположения") + locations
                )
                binding.spinnerLocation.setAdapter(locationAdapter)
            }
        }

        // Настройка спиннера для статусов
        val statusAdapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Все статусы") + viewModel.availableStatuses
        )
        binding.spinnerStatus.setAdapter(statusAdapter)
    }

    private fun setupListeners() {
        binding.buttonApplyFilters.setOnClickListener {
            applyFilters()
        }

        binding.buttonClearFilters.setOnClickListener {
            clearFilters()
        }

        binding.spinnerLocation.setOnItemClickListener { _, _, position, _ ->
            if (position > 0) {
                val selected = binding.spinnerLocation.adapter.getItem(position) as String
                viewModel.setLocationFilter(selected)
            }
        }

        binding.spinnerStatus.setOnItemClickListener { _, _, position, _ ->
            if (position > 0) {
                val selected = binding.spinnerStatus.adapter.getItem(position) as String
                viewModel.setStatusFilter(selected)
            }
        }
    }

    private fun setupPieChart() {
        with(binding.pieChartStatus) {
            setUsePercentValues(true)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            transparentCircleRadius = 45f
            setDrawEntryLabels(false)
            setExtraOffsets(5f, 10f, 5f, 10f)

            // Анимация
            animateY(1000, Easing.EaseInOutCubic)

            // Легенда
            legend.isEnabled = true
            legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
            legend.setDrawInside(false)
            legend.yOffset = 10f
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredDevices.collect { devices ->
                    deviceAdapter.submitList(devices)

                    // Обновляем статистику
                    updateStatistics(devices.size)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.statistics.collect { stats ->
                    updateStatisticsDisplay(stats)
                    updatePieChart(stats.devicesByStatus)
                }
            }
        }
    }

    private fun applyFilters() {
        val location = if (binding.spinnerLocation.text.toString() != "Все местоположения") {
            binding.spinnerLocation.text.toString()
        } else null

        val status = if (binding.spinnerStatus.text.toString() != "Все статусы") {
            binding.spinnerStatus.text.toString()
        } else null

        viewModel.setLocationFilter(location)
        viewModel.setStatusFilter(status)
    }

    private fun clearFilters() {
        binding.spinnerLocation.setText("Все местоположения", false)
        binding.spinnerStatus.setText("Все статусы", false)
        viewModel.clearFilters()
    }

    private fun updateStatistics(totalDevices: Int) {
        binding.textTotalDevices.text = totalDevices.toString()
    }

    private fun updateStatisticsDisplay(stats: com.kipia.management.mobile.viewmodel.ReportStatistics) {
        binding.textTotalDevices.text = stats.totalDevices.toString()

        // Количество приборов "В работе"
        val inWorkCount = stats.devicesByStatus["В работе"] ?: 0
        binding.textInWork.text = inWorkCount.toString()

        // Средний год
        stats.averageYear?.let {
            binding.textAverageYear.text = "%.0f".format(it)
        } ?: run {
            binding.textAverageYear.text = "-"
        }
    }

    private fun updatePieChart(devicesByStatus: Map<String, Int>) {
        if (devicesByStatus.isEmpty()) {
            binding.pieChartStatus.data = null
            binding.pieChartStatus.invalidate()
            return
        }

        val entries = mutableListOf<PieEntry>()
        val colors = mutableListOf<Int>()

        // Цвета для статусов
        val statusColors = mapOf(
            "В работе" to requireContext().getColor(R.color.green_500),
            "В ремонте" to requireContext().getColor(R.color.yellow_500),
            "В резерве" to requireContext().getColor(R.color.blue_500),
            "Списан" to requireContext().getColor(R.color.red_500)
        )

        devicesByStatus.forEach { (status, count) ->
            if (count > 0) {
                entries.add(PieEntry(count.toFloat(), status))
                colors.add(statusColors[status] ?: requireContext().getColor(R.color.purple_500))
            }
        }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.sliceSpace = 3f
        dataSet.selectionShift = 5f
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = requireContext().getColor(android.R.color.white)

        val pieData = PieData(dataSet)
        pieData.setValueFormatter(PercentFormatter(binding.pieChartStatus))
        pieData.setValueTextSize(11f)

        binding.pieChartStatus.data = pieData
        binding.pieChartStatus.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun AutoCompleteTextView.setOnItemClickListener(listener: (String) -> Unit) {
        setOnItemClickListener { parent, _, position, _ ->
            val item = parent?.getItemAtPosition(position) as? String
            item?.let { listener(it) }
        }
    }
}