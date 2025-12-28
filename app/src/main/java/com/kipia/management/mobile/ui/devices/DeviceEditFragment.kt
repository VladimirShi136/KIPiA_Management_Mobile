package com.kipia.management.mobile.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.databinding.FragmentDeviceEditBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DeviceEditFragment : Fragment() {

    private var _binding: FragmentDeviceEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: com.kipia.management.mobile.viewmodel.DeviceEditViewModel by viewModels()
    private val args: DeviceEditFragmentArgs by navArgs()

    private val statuses = listOf("В работе", "В ремонте", "В резерве", "Списан")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinner()
        setupObservers()
        setupListeners()

        // ИСПРАВЛЕНО: Передаем deviceId в ViewModel для загрузки
        viewModel.loadDevice(args.deviceId)
    }

    private fun setupSpinner() {
        // Создаем адаптер для AutoCompleteTextView
        val adapter = android.widget.ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            statuses
        )
        binding.spinnerStatus.setAdapter(adapter)
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.device.collect { device ->
                    device?.let { populateForm(it) }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // ИСПРАВЛЕНО: Собираем Flow ошибок
                viewModel.validationErrors.collect { errors ->
                    showErrors(errors)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // ИСПРАВЛЕНО: Собираем Flow успешного сохранения
                viewModel.saveSuccess.collect { success ->
                    if (success) {
                        findNavController().navigateUp()
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.isVisible = isLoading
                    binding.content.isVisible = !isLoading
                }
            }
        }
    }

    private fun setupListeners() {
        binding.buttonSave.setOnClickListener {
            saveDevice()
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.buttonDelete.setOnClickListener {
            deleteDevice()
        }
    }

    private fun populateForm(device: Device) {
        binding.apply {
            editInventoryNumber.setText(device.inventoryNumber)
            editType.setText(device.type)
            editName.setText(device.name ?: "")
            editManufacturer.setText(device.manufacturer ?: "")
            editYear.setText(device.year?.toString() ?: "")
            editLocation.setText(device.location)

            // Устанавливаем значение в AutoCompleteTextView
            spinnerStatus.setText(device.status, false)

            editAccuracyClass.setText(device.accuracyClass?.toString() ?: "")
            editMeasurementLimit.setText(device.measurementLimit ?: "")
            editValveNumber.setText(device.valveNumber ?: "")
            editAdditionalInfo.setText(device.additionalInfo ?: "")

            // Показываем кнопку удаления только для существующих устройств
            buttonDelete.isVisible = device.id != 0
        }
    }

    private fun saveDevice() {
        val device = Device(
            id = args.deviceId,
            inventoryNumber = binding.editInventoryNumber.text.toString(),
            type = binding.editType.text.toString(),
            name = binding.editName.text.toString().takeIf { it.isNotBlank() },
            manufacturer = binding.editManufacturer.text.toString().takeIf { it.isNotBlank() },
            year = binding.editYear.text.toString().toIntOrNull(),
            location = binding.editLocation.text.toString(),
            status = binding.spinnerStatus.text.toString(),
            accuracyClass = binding.editAccuracyClass.text.toString().toDoubleOrNull(),
            measurementLimit = binding.editMeasurementLimit.text.toString().takeIf { it.isNotBlank() },
            valveNumber = binding.editValveNumber.text.toString().takeIf { it.isNotBlank() },
            additionalInfo = binding.editAdditionalInfo.text.toString().takeIf { it.isNotBlank() },
            photoPath = null,
            photos = null
        )

        // ИСПРАВЛЕНО: Вызываем правильный метод ViewModel
        viewModel.saveDevice(device)
    }

    private fun deleteDevice() {
        viewModel.deleteDevice()
    }

    private fun showErrors(errors: Map<String, String>) {
        binding.layoutInventoryNumber.error = errors["inventoryNumber"]
        binding.layoutType.error = errors["type"]
        binding.layoutLocation.error = errors["location"]

        // Фокусируемся на первом поле с ошибкой
        errors.entries.firstOrNull()?.let { (field, _) ->
            when (field) {
                "inventoryNumber" -> binding.editInventoryNumber.requestFocus()
                "type" -> binding.editType.requestFocus()
                "location" -> binding.editLocation.requestFocus()
            }
        }
    }

    private fun clearErrors() {
        binding.apply {
            layoutInventoryNumber.error = null
            layoutType.error = null
            layoutLocation.error = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}