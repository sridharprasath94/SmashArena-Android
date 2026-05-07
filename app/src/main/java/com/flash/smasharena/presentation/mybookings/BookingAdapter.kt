package com.flash.smasharena.presentation.mybookings

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flash.smasharena.R
import com.flash.smasharena.databinding.ItemBookingBinding

class BookingAdapter(
    private val onCancelClicked: (BookingItem) -> Unit,
) : ListAdapter<BookingListItem, RecyclerView.ViewHolder>(DiffCallback) {

    var cancellingDocId: String? = null

    inner class BookingViewHolder(private val binding: ItemBookingBinding) :
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

    inner class HeaderViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is BookingListItem.Header  -> TYPE_HEADER
        is BookingListItem.Booking -> TYPE_BOOKING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(inflater.inflate(R.layout.item_booking_header, parent, false) as TextView)
        } else {
            BookingViewHolder(ItemBookingBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is BookingListItem.Header  -> (holder as HeaderViewHolder).textView.text = item.label
            is BookingListItem.Booking -> (holder as BookingViewHolder).bind(item.item)
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<BookingListItem>() {
        override fun areItemsTheSame(a: BookingListItem, b: BookingListItem): Boolean = when {
            a is BookingListItem.Header  && b is BookingListItem.Header  -> a.label == b.label
            a is BookingListItem.Booking && b is BookingListItem.Booking -> a.item.docId == b.item.docId
            else -> false
        }
        override fun areContentsTheSame(a: BookingListItem, b: BookingListItem) = a == b
    }

    companion object {
        private const val TYPE_HEADER  = 0
        private const val TYPE_BOOKING = 1
    }
}
