package com.flash.smasharena.presentation.mybookings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flash.smasharena.databinding.ItemCancelledBookingBinding

class CancelledBookingsAdapter(
    private val onDeleteClicked: (CancelledBookingItem) -> Unit,
    private val onItemClicked: (CancelledBookingItem) -> Unit,
) : ListAdapter<CancelledBookingItem, CancelledBookingsAdapter.ViewHolder>(DiffCallback) {

    var isMultiSelectMode: Boolean = false

    inner class ViewHolder(private val binding: ItemCancelledBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CancelledBookingItem) {
            binding.tvFacilityName.text = item.facilityDisplayName
            binding.tvDateTime.text = "${item.displayDate} · ${item.timeLabel}"
            binding.tvCancelledOn.text = "Cancelled ${item.cancelledDisplayDate}"

            binding.cbSelect.isVisible = isMultiSelectMode
            binding.btnDelete.isVisible = !isMultiSelectMode

            if (isMultiSelectMode) {
                binding.cbSelect.isChecked = item.isSelected
                binding.cbSelect.setOnClickListener { onItemClicked(item) }
                binding.root.setOnClickListener { onItemClicked(item) }
            } else {
                binding.btnDelete.setOnClickListener { onDeleteClicked(item) }
                binding.root.setOnClickListener(null)
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
