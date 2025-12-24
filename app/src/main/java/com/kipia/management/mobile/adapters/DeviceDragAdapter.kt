package com.kipia.management.mobile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kipia.management.mobile.R
import com.kipia.management.mobile.data.entities.Device

class DeviceDragAdapter(
    private val devices: List<Device>,
    private val onDeviceDragStart: (Device, View) -> Unit
) : RecyclerView.Adapter<DeviceDragAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.deviceNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device_drag, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.textView.text = "${device.type}: ${device.inventoryNumber}"

        // Настраиваем перетаскивание
        holder.itemView.setOnLongClickListener { view ->
            onDeviceDragStart(device, view)
            true
        }
    }

    override fun getItemCount() = devices.size
}