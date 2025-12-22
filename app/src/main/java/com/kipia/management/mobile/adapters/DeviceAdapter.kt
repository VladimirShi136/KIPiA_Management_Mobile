package com.kipia.management.mobile.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kipia.management.mobile.R
import com.kipia.management.mobile.data.entities.Device
import com.kipia.management.mobile.databinding.ItemDeviceBinding

class DeviceAdapter(
    private val onItemClick: (Device) -> Unit
) : ListAdapter<Device, DeviceAdapter.DeviceViewHolder>(DeviceDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = ItemDeviceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DeviceViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = getItem(position)
        holder.bind(device)
    }

    class DeviceViewHolder(
        private val binding: ItemDeviceBinding,
        private val onItemClick: (Device) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val gson = Gson()

        fun bind(device: Device) {
            binding.apply {
                // Основная информация
                textInventoryNumber.text = device.inventoryNumber
                textType.text = device.type
                textLocation.text = device.location
                textManufacturer.text = device.manufacturer ?: "Не указан"

                // Статус
                textStatus.text = device.status
                textStatus.setBackgroundResource(getStatusBackground(device.status))

                // Фотографии
                val photos = getPhotoList(device.photos)
                if (photos.isNotEmpty() || !device.photoPath.isNullOrEmpty()) {
                    layoutPhotos.visibility = ViewGroup.VISIBLE
                    val totalPhotos = photos.size + if (device.photoPath != null) 1 else 0
                    textPhotoCount.text = "$totalPhotos фото"
                } else {
                    layoutPhotos.visibility = ViewGroup.GONE
                }

                // Клик на элемент
                root.setOnClickListener {
                    onItemClick(device)
                }
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

        private fun getPhotoList(photosJson: String?): List<String> {
            return if (photosJson.isNullOrEmpty()) {
                emptyList()
            } else {
                try {
                    val listType = object : TypeToken<List<String>>() {}.type
                    gson.fromJson<List<String>>(photosJson, listType) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
        }
    }

    class DeviceDiffCallback : DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean {
            return oldItem == newItem
        }
    }
}