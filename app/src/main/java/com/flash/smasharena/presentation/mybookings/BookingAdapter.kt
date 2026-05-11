package com.flash.smasharena.presentation.mybookings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flash.smasharena.databinding.ItemBookingBinding

class BookingAdapter(
    private val onCancelClicked: (BookingItem) -> Unit,
) : ListAdapter<BookingItem, BookingAdapter.ViewHolder>(DiffCallback) {

    var cancellingDocId: String? = null

    inner class ViewHolder(private val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: BookingItem) {
            binding.tvFacilityName.text = item.facilityDisplayName
            binding.tvDate.text = item.displayDate
            binding.tvTime.text = item.timeLabel
            binding.root.alpha = if (item.isExpired) 0.5f else 1f

            val isCancelling = item.docId == cancellingDocId
            binding.layoutActions.isVisible = !item.isExpired
            binding.btnCancel.isEnabled = !isCancelling
            binding.btnCancel.text = if (isCancelling) "Cancelling…" else "Cancel"
            binding.btnCancel.setOnClickListener { onCancelClicked(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<BookingItem>() {
        override fun areItemsTheSame(a: BookingItem, b: BookingItem) = a.docId == b.docId
        override fun areContentsTheSame(a: BookingItem, b: BookingItem) = a == b
    }
}
