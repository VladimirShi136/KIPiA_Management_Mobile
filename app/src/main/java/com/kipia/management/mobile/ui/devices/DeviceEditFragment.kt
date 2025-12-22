package com.kipia.management.mobile.ui.devices

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kipia.management.mobile.R
import com.kipia.management.mobile.databinding.FragmentDeviceEditBinding
import com.kipia.management.mobile.viewmodel.DeviceEditViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DeviceEditFragment : Fragment() {

    private var _binding: FragmentDeviceEditBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DeviceEditViewModel by viewModels()

    private val statusList = listOf(
        "В работе",
        "В ремонте",
        "В резерве",
        "Списан"
    )

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
        setupValidation()
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            statusList
        )
        binding.spinnerStatus.setAdapter(adapter)
    }

    private fun setupObservers() {
        // Наблюдаем за прибором
        viewModel.device.observe(viewLifecycleOwner) { device ->
            device?.let { populateForm(it) }
        }

        // Наблюдаем за ошибками валидации
        viewModel.validationErrors.observe(viewLifecycleOwner) { errors ->
            showValidationErrors(errors)
        }

        // Наблюдаем за успешным сохранением
        viewModel.saveSuccess.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().popBackStack()
            }
        }

        // Наблюдаем за загрузкой
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.content.isVisible = !isLoading
        }
    }

    private fun setupListeners() {
        binding.buttonSave.setOnClickListener {
            saveDevice()
        }

        binding.buttonCancel.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.buttonDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun setupValidation() {
        // Реальная валидация при вводе
        binding.editInventoryNumber.doAfterTextChanged {
            validateField("inventoryNumber", it.toString())
        }

        binding.editType.doAfterTextChanged {
            validateField("type", it.toString())
        }

        binding.editLocation.doAfterTextChanged {
            validateField("location", it.toString())
        }
    }

    private fun populateForm(device: com.kipia.management.mobile.data.entities.Device) {
        binding.apply {
            editInventoryNumber.setText(device.inventoryNumber)
            editType.setText(device.type)
            editName.setText(device.name)
            editManufacturer.setText(device.manufacturer)
            editYear.setText(device.year?.toString() ?: "")
            editLocation.setText(device.location)
            spinnerStatus.setText(device.status, false)
            editAccuracyClass.setText(device.accuracyClass?.toString() ?: "")
            editMeasurementLimit.setText(device.measurementLimit)
            editValveNumber.setText(device.valveNumber)
            editAdditionalInfo.setText(device.additionalInfo)

            // Показываем кнопку удаления только для существующих приборов
            buttonDelete.isVisible = device.id > 0
        }
    }

    private fun saveDevice() {
        lifecycleScope.launch {
            val isValid = viewModel.validateAndSave(
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
                additionalInfo = binding.editAdditionalInfo.text.toString().takeIf { it.isNotBlank() }
            )

            if (!isValid) {
                // Прокручиваем к первой ошибке
                val firstErrorField = when {
                    binding.layoutInventoryNumber.error != null -> binding.editInventoryNumber
                    binding.layoutType.error != null -> binding.editType
                    binding.layoutLocation.error != null -> binding.editLocation
                    else -> null
                }

                firstErrorField?.requestFocus()
            }
        }
    }

    private fun validateField(fieldName: String, value: String) {
        val error = when (fieldName) {
            "inventoryNumber" -> if (value.isBlank()) getString(R.string.validation_inventory_number_required) else null
            "type" -> if (value.isBlank()) getString(R.string.validation_type_required) else null
            "location" -> if (value.isBlank()) getString(R.string.validation_location_required) else null
            else -> null
        }

        when (fieldName) {
            "inventoryNumber" -> binding.layoutInventoryNumber.error = error
            "type" -> binding.layoutType.error = error
            "location" -> binding.layoutLocation.error = error
        }
    }

    private fun showValidationErrors(errors: Map<String, String>) {
        binding.apply {
            layoutInventoryNumber.error = when (errors["inventoryNumber"]) {
                "required" -> getString(R.string.validation_inventory_number_required)
                "unique" -> getString(R.string.validation_inventory_number_unique)
                else -> null
            }

            layoutType.error = if (errors.containsKey("type")) {
                getString(R.string.validation_type_required)
            } else null

            layoutLocation.error = if (errors.containsKey("location")) {
                getString(R.string.validation_location_required)
            } else null
        }
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_delete_title)
            .setMessage(getString(R.string.dialog_delete_message, viewModel.device.value?.inventoryNumber))
            .setPositiveButton(R.string.dialog_delete_confirm) { _, _ ->
                viewModel.deleteDevice()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}