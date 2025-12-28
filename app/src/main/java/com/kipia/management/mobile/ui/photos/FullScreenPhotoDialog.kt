package com.kipia.management.mobile.ui.photos

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kipia.management.mobile.databinding.DialogFullscreenPhotoBinding
import java.io.File

class FullScreenPhotoDialog : DialogFragment() {

    private var _binding: DialogFullscreenPhotoBinding? = null
    private val binding get() = _binding!!

    private var photoPath: String? = null
    private var deviceId: Long = 0L
    private var currentRotation = 0

    interface PhotoActionListener {
        fun onPhotoDeleted(photoPath: String)
        fun onPhotoRotated(oldPath: String, newPath: String)
    }

    companion object {
        private const val ARG_PHOTO_PATH = "photo_path"
        private const val ARG_DEVICE_ID = "device_id"

        fun newInstance(photoPath: String, deviceId: Long): FullScreenPhotoDialog {
            return FullScreenPhotoDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_PHOTO_PATH, photoPath)
                    putLong(ARG_DEVICE_ID, deviceId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            photoPath = it.getString(ARG_PHOTO_PATH)
            deviceId = it.getLong(ARG_DEVICE_ID)
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogFullscreenPhotoBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()

        setupPhoto()
        setupListeners()

        return dialog
    }

    private fun setupPhoto() {
        photoPath?.let { path ->
            Glide.with(this)
                .load(File(path))
                .into(binding.fullscreenImage)
        }
    }

    private fun setupListeners() {
        binding.buttonClose.setOnClickListener {
            dismiss()
        }

        binding.fullscreenImage.setOnClickListener {
            // Можно добавить зум при двойном тапе
        }

        binding.buttonRotateLeft.setOnClickListener {
            rotatePhoto(-90)
        }

        binding.buttonRotateRight.setOnClickListener {
            rotatePhoto(90)
        }

        binding.buttonDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun rotatePhoto(degrees: Int) {
        photoPath?.let { path ->
            val file = File(path)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(path)
                val matrix = Matrix()
                matrix.postRotate(degrees.toFloat())

                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap, 0, 0,
                    bitmap.width, bitmap.height,
                    matrix, true
                )

                // Сохраняем повернутое изображение
                val rotatedPath = saveRotatedBitmap(rotatedBitmap, path)

                // Обновляем отображение
                Glide.with(this)
                    .load(File(rotatedPath))
                    .into(binding.fullscreenImage)

                // Уведомляем слушателя
                (parentFragment as? PhotoActionListener)?.onPhotoRotated(path, rotatedPath)

                // Обновляем путь к текущему фото
                photoPath = rotatedPath
                currentRotation = (currentRotation + degrees) % 360

                bitmap.recycle()
                rotatedBitmap.recycle()
            }
        }
    }

    private fun saveRotatedBitmap(bitmap: Bitmap, originalPath: String): String {
        val file = File(originalPath)
        val fileName = file.nameWithoutExtension
        val extension = file.extension
        val timestamp = System.currentTimeMillis()
        val newFileName = "${fileName}_rotated_$timestamp.$extension"
        val newFile = File(file.parent, newFileName)

        try {
            val outputStream = newFile.outputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            return newFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return originalPath
        }
    }

    private fun showDeleteConfirmationDialog() {
        photoPath?.let { path ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Удалить фото")
                .setMessage("Вы уверены, что хотите удалить это фото?")
                .setPositiveButton("Удалить") { _, _ ->
                    deletePhoto(path)
                }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun deletePhoto(path: String) {
        val file = File(path)
        if (file.exists()) {
            file.delete()

            // Уведомляем слушателя
            (parentFragment as? PhotoActionListener)?.onPhotoDeleted(path)

            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}