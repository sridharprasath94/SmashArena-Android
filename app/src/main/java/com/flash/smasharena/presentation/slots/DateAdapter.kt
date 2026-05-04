package com.flash.smasharena.presentation.slots

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flash.smasharena.R
import com.flash.smasharena.databinding.ItemDateChipBinding

class DateAdapter(
    private val onDateSelected: (DateItem) -> Unit,
) : ListAdapter<DateItem, DateAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemDateChipBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DateItem) {
            binding.tvDayLabel.text = item.dayLabel
            binding.tvDayNumber.text = item.dayNumber

            val bgColor = when {
                item.isSelected -> ContextCompat.getColor(binding.root.context, R.color.accent_green)
                !item.isBrowsable -> ContextCompat.getColor(binding.root.context, R.color.surface)
                else -> ContextCompat.getColor(binding.root.context, R.color.surface)
            }
            val textColor = when {
                item.isSelected -> ContextCompat.getColor(binding.root.context, R.color.background)
                !item.isBrowsable -> ContextCompat.getColor(binding.root.context, R.color.text_secondary)
                else -> ContextCompat.getColor(binding.root.context, R.color.text_primary)
            }

            binding.root.setCardBackgroundColor(bgColor)
            binding.tvDayLabel.setTextColor(textColor)
            binding.tvDayNumber.setTextColor(textColor)

            binding.root.alpha = if (item.isBrowsable || item.isSelected) 1f else 0.4f
            binding.root.setOnClickListener { if (item.isBrowsable) onDateSelected(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDateChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<DateItem>() {
        override fun areItemsTheSame(a: DateItem, b: DateItem) = a.dateString == b.dateString
        override fun areContentsTheSame(a: DateItem, b: DateItem) = a == b
    }
}
