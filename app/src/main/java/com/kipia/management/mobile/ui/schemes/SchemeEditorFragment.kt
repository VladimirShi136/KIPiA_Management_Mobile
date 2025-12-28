package com.kipia.management.mobile.ui.schemes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kipia.management.mobile.adapters.DeviceDragAdapter
import com.kipia.management.mobile.databinding.FragmentSchemeEditorBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SchemeEditorFragment : Fragment() {

    private var _binding: FragmentSchemeEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: com.kipia.management.mobile.viewmodel.SchemeEditorViewModel by viewModels()
    private val args: SchemeEditorFragmentArgs by navArgs()

    private lateinit var deviceDragAdapter: DeviceDragAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSchemeEditorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupListeners()

        // Устанавливаем слушатели для CanvasView
        binding.canvasView.setOnDeviceDragListener { deviceId, x, y ->
            viewModel.saveDeviceLocation(deviceId, x, y)
        }

        binding.canvasView.setOnDeviceClickListener { deviceId ->
            showDeviceOptionsDialog(deviceId)
        }
    }

    private fun setupRecyclerView() {
        deviceDragAdapter = DeviceDragAdapter(
            devices = emptyList(),
            onDeviceDragStart = { device, view ->
                binding.canvasView.startDrag(device, view)
            }
        )

        binding.devicesRecyclerView.apply {
            adapter = deviceDragAdapter
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    // Вариант 1: через visibility
                    binding.loadingProgressBar.visibility =
                        if (isLoading) View.VISIBLE else View.GONE
                    binding.canvasContainer.visibility =
                        if (!isLoading) View.VISIBLE else View.GONE
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.devices.collect { devices ->
                    deviceDragAdapter = DeviceDragAdapter(
                        devices = devices,
                        onDeviceDragStart = { device, view ->
                            binding.canvasView.startDrag(device, view)
                        }
                    )
                    binding.devicesRecyclerView.adapter = deviceDragAdapter
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.deviceLocations.collect { locations ->
                    binding.canvasView.setDeviceLocations(locations)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.backgroundImage.collect { bitmap ->
                    bitmap?.let {
                        binding.canvasView.setBackgroundImage(it)
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.saveButton.setOnClickListener {
            saveScheme()
        }

        binding.cancelButton.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.canvasView.setOnLongClickListener { view ->
            showCanvasOptionsDialog(view)
            true
        }
    }

    private fun saveScheme() {
        // TODO: Реализовать сохранение схемы
        findNavController().navigateUp()
    }

    private fun showDeviceOptionsDialog(deviceId: Int) {
        val device = viewModel.getDeviceById(deviceId) ?: return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("${device.type}: ${device.inventoryNumber}")
            .setItems(arrayOf("Удалить с схемы", "Повернуть")) { _, which ->
                when (which) {
                    0 -> viewModel.deleteDeviceLocation(deviceId)
                    1 -> rotateDevice(deviceId)
                }
            }
            .show()
    }

    private fun showCanvasOptionsDialog(view: View) {
        val x = view.x
        val y = view.y

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Действия с холстом")
            .setItems(arrayOf("Добавить фон", "Очистить фон", "Сохранить позиции")) { _, which ->
                when (which) {
                    0 -> loadBackgroundImage()
                    1 -> binding.canvasView.clearBackgroundImage()
                    2 -> saveDevicePositions()
                }
            }
            .show()
    }

    private fun rotateDevice(deviceId: Int) {
        val currentLocation = viewModel.getLocationForDevice(deviceId)
        currentLocation?.let { location ->
            val newRotation = (location.rotation + 90) % 360
            viewModel.saveDeviceLocation(deviceId, location.x, location.y, newRotation)
        }
    }

    private fun loadBackgroundImage() {
        // TODO: Реализовать загрузку фонового изображения
    }

    private fun saveDevicePositions() {
        // Позиции уже сохраняются автоматически при перетаскивании
        // Можно показать сообщение об успешном сохранении
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}