package com.kipia.management.mobile.ui.photos

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.kipia.management.mobile.R
import com.kipia.management.mobile.databinding.DialogFullscreenPhotoBinding
import com.kipia.management.mobile.utils.PhotoManager

class FullScreenPhotoDialog : DialogFragment() {

    companion object {
        private const val ARG_PHOTO_PATH = "photo_path"
        private const val ARG_DEVICE_ID = "device_id"

        fun newInstance(photoPath: String, deviceId: Long = 0): FullScreenPhotoDialog {
            return FullScreenPhotoDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_PATH, photoPath)
                    putLong(ARG_DEVICE_ID, deviceId)
                }
            }
        }
    }

    private var _binding: DialogFullscreenPhotoBinding? = null
    private val binding get() = _binding!!

    private var photoPath: String = ""
    private var deviceId: Long = 0
    private var currentRotation = 0f

    // Callback для обновления фото в родительском фрагменте
    interface PhotoActionListener {
        fun onPhotoDeleted(photoPath: String)
        fun onPhotoRotated(oldPath: String, newPath: String)
    }

    private var listener: PhotoActionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogFullscreenPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Настройка диалога
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawable(Color.BLACK.toDrawable())
        }

        photoPath = arguments?.getString(ARG_PHOTO_PATH) ?: ""
        deviceId = arguments?.getLong(ARG_DEVICE_ID) ?: 0L

        // Находим родительский фрагмент как слушатель
        parentFragment?.let {
            if (it is PhotoActionListener) {
                listener = it
            }
        }

        loadPhoto()
        setupClickListeners()
    }

    private fun loadPhoto() {
        Glide.with(this)
            .load(photoPath)
            .placeholder(R.drawable.ic_photo)
            .into(binding.fullscreenImage)
    }

    private fun setupClickListeners() {
        // Закрытие
        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.fullscreenImage.setOnClickListener {
            dismiss()
        }

        // Поворот влево (-90 градусов)
        binding.buttonRotateLeft.setOnClickListener {
            rotatePhoto(-90f)
        }

        // Поворот вправо (+90 градусов)
        binding.buttonRotateRight.setOnClickListener {
            rotatePhoto(90f)
        }

        // Удаление
        binding.buttonDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun rotatePhoto(degrees: Float) {
        // Используем PhotoManager для поворота
        val photoManager = PhotoManager(
            fragment = this,
            onPhotoTaken = { /* не используется здесь */ },
            onPhotoSelected = { /* не используется здесь */ }
        )

        val newPath = photoManager.rotatePhoto(requireContext(), photoPath, degrees)
        newPath?.let {
            // Обновляем отображение
            Glide.with(this)
                .load(newPath)
                .into(binding.fullscreenImage)

            // Уведомляем родительский фрагмент
            listener?.onPhotoRotated(photoPath, newPath)

            // Обновляем текущий путь
            photoPath = newPath
            currentRotation += degrees
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_photo_title))
            .setMessage(getString(R.string.delete_photo_confirmation))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                listener?.onPhotoDeleted(photoPath)
                dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}