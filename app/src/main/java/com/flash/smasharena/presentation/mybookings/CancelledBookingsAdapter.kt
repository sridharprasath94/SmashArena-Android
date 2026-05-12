package com.flash.smasharena.presentation.mybookings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flash.smasharena.R
import com.flash.smasharena.databinding.ItemCancelledBookingBinding

class CancelledBookingsAdapter(
    private val onItemClicked: (CancelledBookingItem) -> Unit,
    private val onItemLongClicked: (CancelledBookingItem) -> Unit,
) : ListAdapter<CancelledBookingItem, CancelledBookingsAdapter.ViewHolder>(DiffCallback) {

    var isSelectionMode: Boolean = false

    inner class ViewHolder(private val binding: ItemCancelledBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CancelledBookingItem) {
            binding.tvFacilityName.text = item.facilityDisplayName
            binding.tvDateTime.text = "${item.displayDate} · ${item.timeLabel}"
            binding.tvCancelledOn.text = "Cancelled ${item.cancelledDisplayDate}"

            val iconRes = if (item.facilityId == "badminton_court_1")
                R.drawable.badminton else R.drawable.cricket
            binding.ivFacility.setImageResource(iconRes)

            binding.cbSelect.isVisible = isSelectionMode
            if (isSelectionMode) {
                binding.cbSelect.isChecked = item.isSelected
            }

            val strokeColor = ContextCompat.getColor(
                binding.root.context,
                if (item.isSelected) R.color.accent_green else R.color.outline,
            )
            binding.root.setStrokeColor(strokeColor)

            binding.root.setOnClickListener {
                if (isSelectionMode) onItemClicked(item)
            }
            binding.root.setOnLongClickListener {
                onItemLongClicked(item)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCancelledBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<CancelledBookingItem>() {
        override fun areItemsTheSame(a: CancelledBookingItem, b: CancelledBookingItem) = a.docId == b.docId
        override fun areContentsTheSame(a: CancelledBookingItem, b: CancelledBookingItem) = a == b
    }
}
