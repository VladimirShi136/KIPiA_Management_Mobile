package com.kipia.management.mobile.ui.schemes

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kipia.management.mobile.adapters.DeviceDragAdapter
import com.kipia.management.mobile.databinding.FragmentSchemeEditorBinding
import com.kipia.management.mobile.viewmodel.SchemeEditorViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File

@AndroidEntryPoint
class SchemeEditorFragment : Fragment() {

    private var _binding: FragmentSchemeEditorBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SchemeEditorViewModel by viewModels()
    private val args: SchemeEditorFragmentArgs by navArgs()

    private lateinit var deviceAdapter: DeviceDragAdapter

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

        setupToolbar()
        setupCanvas()
        setupDevicesPanel()
        setupObservers()
        setupListeners()

        // Загружаем данные схемы
        viewLifecycleOwner.lifecycleScope.launch {
            // Если schemeId = 0, значит создаем новую схему
            if (args.schemeId > 0) {
                // TODO: Загрузить существующую схему
                // Сейчас просто показываем заглушку
                binding.titleTextView.text = "Редактор схемы #${args.schemeId}"
            } else {
                binding.titleTextView.text = "Новая схема"
                // TODO: Загрузить изображение по умолчанию или предложить выбрать
            }
        }
    }

    private fun setupToolbar() {
        // Можно добавить меню в toolbar для дополнительных действий
        // binding.toolbar.inflateMenu(R.menu.menu_scheme_editor)
    }

    private fun setupCanvas() {
        // Настраиваем обработку перетаскивания на Canvas
        binding.canvasView.onDevicePositionChanged = { deviceId, x, y ->
            viewModel.saveDeviceLocation(deviceId, x, y, 0f)
            Toast.makeText(
                requireContext(),
                "Позиция прибора сохранена",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Загрузка фонового изображения (заглушка - можно добавить позже)
        // loadBackgroundImage()
    }

    private fun setupDevicesPanel() {
        deviceAdapter = DeviceDragAdapter(
            devices = emptyList(),
            onDeviceDragStart = { device, view ->
                // Начинаем перетаскивание прибора на схему
                startDeviceDrag(device, view)
            }
        )

        binding.devicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = deviceAdapter
        }
    }

    private fun setupObservers() {
        // Наблюдаем за загрузкой данных
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.loadingProgressBar.isVisible = isLoading
            binding.canvasContainer.isVisible = !isLoading
        }

        // Наблюдаем за списком приборов
        viewModel.devices.observe(viewLifecycleOwner) { devices ->
            deviceAdapter = DeviceDragAdapter(
                devices = devices,
                onDeviceDragStart = { device, view ->
                    startDeviceDrag(device, view)
                }
            )
            binding.devicesRecyclerView.adapter = deviceAdapter
        }

        // Наблюдаем за расположениями приборов
        viewModel.deviceLocations.observe(viewLifecycleOwner) { locations ->
            binding.canvasView.deviceLocations = locations
            binding.canvasView.invalidate() // Перерисовываем канвас
        }

        // Наблюдаем за фоновым изображением
        viewModel.backgroundImage.observe(viewLifecycleOwner) { bitmap ->
            bitmap?.let {
                binding.canvasView.backgroundBitmap = it
            }
        }
    }

    private fun setupListeners() {
        // Кнопка сохранения
        binding.saveButton.setOnClickListener {
            saveScheme()
        }

        // Кнопка отмены
        binding.cancelButton.setOnClickListener {
            showCancelConfirmation()
        }

        // Долгое нажатие на канвас для контекстного меню
        binding.canvasView.setOnLongClickListener { view ->
            showCanvasContextMenu(view)
            true
        }
    }

    private fun startDeviceDrag(device: com.kipia.management.mobile.data.entities.Device, sourceView: View) {
        // Создаем "призрачное" изображение для перетаскивания
        sourceView.buildDrawingCache()
        val bitmap = Bitmap.createBitmap(sourceView.drawingCache)
        sourceView.destroyDrawingCache()

        // Показываем пользователю, что можно перетащить на схему
        Toast.makeText(
            requireContext(),
            "Перетащите прибор на схему",
            Toast.LENGTH_SHORT
        ).show()

        // Устанавливаем слушатель для добавления прибора при клике на канвас
        binding.canvasView.setOnClickListener { view ->
            // Получаем координаты касания относительно канваса
            val x = view.x
            val y = view.y

            // Добавляем прибор на схему
            binding.canvasView.addDeviceToCanvas(device, x, y)

            // Убираем слушатель после добавления
            binding.canvasView.setOnClickListener(null)

            Toast.makeText(
                requireContext(),
                "Прибор добавлен на схему",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun saveScheme() {
        if (args.schemeId == 0) {
            // Создание новой схемы
            showSaveNewSchemeDialog()
        } else {
            // Обновление существующей схемы
            updateExistingScheme()
        }
    }

    private fun showSaveNewSchemeDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Сохранение схемы")
            .setMessage("Введите название для новой схемы:")
            .setView(R.layout.dialog_scheme_name) // Создашь позже
            .setPositiveButton("Сохранить") { dialog, _ ->
                // TODO: Реализовать сохранение с названием
                saveSchemeToDatabase("Новая схема")
                dialog.dismiss()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun saveSchemeToDatabase(schemeName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            // TODO: Реализовать сохранение схемы в БД
            // 1. Создать объект Scheme
            // 2. Сохранить через Repository
            // 3. Сохранить все расположения приборов

            Toast.makeText(
                requireContext(),
                "Схема '$schemeName' сохранена",
                Toast.LENGTH_SHORT
            ).show()

            // Возвращаемся назад
            findNavController().popBackStack()
        }
    }

    private fun updateExistingScheme() {
        viewLifecycleOwner.lifecycleScope.launch {
            // TODO: Обновить существующую схему
            Toast.makeText(
                requireContext(),
                "Изменения сохранены",
                Toast.LENGTH_SHORT
            ).show()

            findNavController().popBackStack()
        }
    }

    private fun showCancelConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Подтверждение")
            .setMessage("Отменить изменения? Все несохраненные данные будут потеряны.")
            .setPositiveButton("Отменить") { _, _ ->
                findNavController().popBackStack()
            }
            .setNegativeButton("Продолжить", null)
            .show()
    }

    private fun showCanvasContextMenu(view: View) {
        // TODO: Реализовать контекстное меню для канваса
        // - Удалить прибор
        // - Повернуть прибор
        // - Изменить свойства
        // - Добавить текст/фигуру
    }

    private fun loadBackgroundImage(imagePath: String? = null) {
        // Загрузка фонового изображения
        val path = imagePath ?: "schemes/default_background.png"
        val imageFile = File(requireContext().filesDir, path)

        if (imageFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
            binding.canvasView.backgroundBitmap = bitmap
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}