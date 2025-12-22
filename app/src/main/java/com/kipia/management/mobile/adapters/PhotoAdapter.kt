package com.kipia.management.mobile.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.kipia.management.mobile.R
import com.kipia.management.mobile.databinding.ItemPhotoBinding

class PhotoAdapter(
    private val onPhotoClick: (String) -> Unit,
    private val onPhotoLongClick: (String) -> Unit
) : ListAdapter<String, PhotoAdapter.PhotoViewHolder>(PhotoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PhotoViewHolder(binding, onPhotoClick, onPhotoLongClick)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photoPath = getItem(position)
        holder.bind(photoPath)
    }

    class PhotoViewHolder(
        private val binding: ItemPhotoBinding,
        private val onPhotoClick: (String) -> Unit,
        private val onPhotoLongClick: (String) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(photoPath: String) {
            Glide.with(binding.root.context)
                .load(photoPath)
                .placeholder(R.drawable.ic_photo)
                .error(R.drawable.ic_broken_image)
                .centerCrop()
                .into(binding.imagePhoto)

            binding.root.setOnClickListener {
                onPhotoClick(photoPath)
            }

            binding.root.setOnLongClickListener {
                onPhotoLongClick(photoPath)
                true
            }
        }
    }

    class PhotoDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}