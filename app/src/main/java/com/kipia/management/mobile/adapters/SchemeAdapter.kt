package com.kipia.management.mobile.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kipia.management.mobile.data.entities.Scheme
import com.kipia.management.mobile.databinding.ItemSchemeBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SchemeAdapter(
    private val onItemClick: (Scheme) -> Unit,
    private val onItemLongClick: (Scheme) -> Boolean
) : ListAdapter<Scheme, SchemeAdapter.SchemeViewHolder>(SchemeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SchemeViewHolder {
        val binding = ItemSchemeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SchemeViewHolder(binding, onItemClick, onItemLongClick)
    }

    override fun onBindViewHolder(holder: SchemeViewHolder, position: Int) {
        val scheme = getItem(position)
        holder.bind(scheme)
    }

    class SchemeViewHolder(
        private val binding: ItemSchemeBinding,
        private val onItemClick: (Scheme) -> Unit,
        private val onItemLongClick: (Scheme) -> Boolean
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(scheme: Scheme) {
            binding.apply {
                textSchemeName.text = scheme.name
                textDescription.text = scheme.description ?: "Нет описания"

                // Форматируем дату создания (если есть в схеме)
                textCreatedDate.text = formatDate(scheme)

                // Клик на элемент
                root.setOnClickListener {
                    onItemClick(scheme)
                }

                // Долгий клик для контекстного меню
                root.setOnLongClickListener {
                    onItemLongClick(scheme)
                }
            }
        }

        private fun formatDate(scheme: Scheme): String {
            // TODO: Добавить поле createdDate в entity Scheme
            // Пока используем текущую дату
            val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            return "Создано: ${dateFormat.format(Date())}"
        }
    }

    class SchemeDiffCallback : DiffUtil.ItemCallback<Scheme>() {
        override fun areItemsTheSame(oldItem: Scheme, newItem: Scheme): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Scheme, newItem: Scheme): Boolean {
            return oldItem == newItem
        }
    }
}