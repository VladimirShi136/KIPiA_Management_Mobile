package com.kipia.management.mobile.ui.devices

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kipia.management.mobile.R
import com.kipia.management.mobile.adapters.PhotoAdapter
import com.kipia.management.mobile.databinding.FragmentDeviceDetailBinding
import com.kipia.management.mobile.ui.photos.FullScreenPhotoDialog
import com.kipia.management.mobile.utils.PhotoManager
import com.kipia.management.mobile.viewmodel.DeviceDetailViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeviceDetailFragment : Fragment(), FullScreenPhotoDialog.PhotoActionListener {

    private var _binding: FragmentDeviceDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DeviceDetailViewModel by viewModels()
    private val args: DeviceDetailFragmentArgs by navArgs()

    private lateinit var photoManager: PhotoManager
    private lateinit var photoAdapter: PhotoAdapter

    // Для обработки разрешений
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val storageGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] ?: false
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false
        }

        if (cameraGranted || storageGranted) {
            showPhotoSourceDialog()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeviceDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPhotoManager()
        setupRecyclerView()
        setupObservers()
        setupListeners()
    }

    private fun setupPhotoManager() {
        photoManager = PhotoManager(
            fragment = this,
            onPhotoTaken = { uri ->
                val photoPath = PhotoManager.copyPhotoToAppStorage(requireContext(), uri)
                photoPath?.let {
                    viewModel.updateDevicePhoto(it)
                    showPhotoAddedMessage()
                }
            },
            onPhotoSelected = { uri ->
                val photoPath = PhotoManager.copyPhotoToAppStorage(requireContext(), uri)
                photoPath?.let {
                    viewModel.addAdditionalPhoto(it)
                    showPhotoAddedMessage()
                }
            }
        )
    }

    private fun setupRecyclerView() {
        photoAdapter = PhotoAdapter(
            onPhotoClick = { photoPath ->
                // TODO: Показать полноэкранный просмотр фото
                showPhotoViewerDialog(photoPath)
            },
            onPhotoLongClick = { photoPath ->
                showDeletePhotoDialog(photoPath)
            }
        )

        binding.recyclerViewPhotos.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = photoAdapter
        }
    }

    private fun setupObservers() {
        viewModel.device.observe(viewLifecycleOwner) { device ->
            device?.let { populateDeviceDetails(it) }
        }

        viewModel.photos.observe(viewLifecycleOwner) { photos ->
            updatePhotoUI(photos)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.isVisible = isLoading
            binding.content.isVisible = !isLoading
        }
    }

    private fun setupListeners() {
        // Клик на область фото
        binding.photoContainer.setOnClickListener {
            showPhotoSourceDialog()
        }

        // Клик на FAB
        binding.fabAddPhoto.setOnClickListener {
            showPhotoSourceDialog()
        }

        // Кнопка редактирования (добавить в toolbar)
        requireActivity().setActionBar(binding.toolbar)
        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    navigateToEdit()
                    true
                }
                else -> false
            }
        }
    }

    private fun populateDeviceDetails(device: com.kipia.management.mobile.data.entities.Device) {
        binding.apply {
            textTitle.text = device.inventoryNumber
            textStatus.text = device.status
            textStatus.setBackgroundResource(getStatusBackground(device.status))

            textLocation.text = device.location
            textType.text = device.type
            textManufacturer.text = device.manufacturer ?: "Не указан"
            textYear.text = device.year?.toString() ?: "Не указан"
            textAdditionalInfo.text = device.additionalInfo ?: "Нет дополнительной информации"

            // Показываем/скрываем поля если они пустые
            textManufacturer.isVisible = device.manufacturer?.isNotBlank() == true
            textYear.isVisible = device.year != null
            textAdditionalInfo.isVisible = device.additionalInfo?.isNotBlank() == true
        }
    }

    private fun updatePhotoUI(photos: List<String>) {
        if (photos.isNotEmpty()) {
            // Показываем первое фото как основное
            val mainPhoto = photos.first()
            Glide.with(this)
                .load(mainPhoto)
                .placeholder(R.drawable.ic_photo)
                .error(R.drawable.ic_broken_image)
                .into(binding.imageDevice)

            binding.layoutNoPhoto.visibility = View.GONE
            binding.imageDevice.visibility = View.VISIBLE

            // Показываем галерею если фото больше 1
            if (photos.size > 1) {
                binding.cardGallery.visibility = View.VISIBLE
                photoAdapter.submitList(photos.drop(1))
            } else {
                binding.cardGallery.visibility = View.GONE
            }
        } else {
            // Нет фото
            binding.layoutNoPhoto.visibility = View.VISIBLE
            binding.imageDevice.visibility = View.GONE
            binding.cardGallery.visibility = View.GONE
        }
    }

    private fun getStatusBackground(status: String): Int {
        return when (status) {
            "В работе" -> R.drawable.status_in_work_bg
            "В ремонте" -> R.drawable.status_repair_bg
            "В резерве" -> R.drawable.status_reserve_bg
            "Списан" -> R.drawable.status_decommissioned_bg
            else -> R.drawable.status_in_work_bg
        }
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf(
            getString(R.string.take_photo),
            getString(R.string.choose_from_gallery)
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.add_photo))
            .setItems(options) { _, which ->
                when (which) {
                    0 -> photoManager.takePhoto()
                    1 -> photoManager.pickPhotoFromGallery()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showDeletePhotoDialog(photoPath: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.delete_photo))
            .setMessage(getString(R.string.delete_photo_confirmation))
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.deletePhoto(photoPath)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPhotoViewerDialog(photoPath: String) {
        val deviceId = viewModel.device.value?.id?.toLong() ?: 0L
        val dialog = FullScreenPhotoDialog.newInstance(photoPath, deviceId)
        dialog.show(parentFragmentManager, "FullScreenPhotoDialog")
    }

    private fun showPhotoAddedMessage() {
        // Можно показать Snackbar или Toast
        // Snackbar.make(binding.root, R.string.photo_added_successfully, Snackbar.LENGTH_SHORT).show()
    }

    private fun navigateToEdit() {
        val device = viewModel.device.value ?: return
        val action = DeviceDetailFragmentDirections.actionDeviceDetailFragmentToDeviceEditFragment(
            deviceId = device.id
        )
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPhotoDeleted(photoPath: String) {
        viewModel.deletePhoto(photoPath)
        Toast.makeText(requireContext(), R.string.photo_deleted, Toast.LENGTH_SHORT).show()
    }

    override fun onPhotoRotated(oldPath: String, newPath: String) {
        // Обновляем путь в базе данных
        if (viewModel.device.value?.photoPath == oldPath) {
            viewModel.updateDevicePhoto(newPath)
        } else {
            // Это дополнительное фото
            viewModel.addAdditionalPhoto(newPath)
            // Удаляем старую версию
            viewModel.deletePhoto(oldPath)
        }
    }

}