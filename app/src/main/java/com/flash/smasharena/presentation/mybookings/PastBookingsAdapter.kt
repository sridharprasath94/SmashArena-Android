package com.flash.smasharena.presentation.mybookings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flash.smasharena.R
import com.flash.smasharena.databinding.ItemPastBookingBinding
import com.flash.smasharena.databinding.ItemPastMonthHeaderBinding
import com.flash.smasharena.databinding.ItemPastYearHeaderBinding

class PastBookingsAdapter(
    private val onYearClicked: (year: Int) -> Unit,
    private val onMonthClicked: (year: Int, month: Int) -> Unit,
    private val onShowMoreClicked: () -> Unit,
) : ListAdapter<PastTimelineItem, RecyclerView.ViewHolder>(DiffCallback) {

    inner class YearViewHolder(private val binding: ItemPastYearHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PastTimelineItem.YearHeader) {
            binding.tvYear.text = item.year.toString()
            binding.ivExpand.rotation = if (item.isExpanded) 180f else 0f
            binding.root.setOnClickListener { onYearClicked(item.year) }
        }
    }

    inner class MonthViewHolder(private val binding: ItemPastMonthHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PastTimelineItem.MonthHeader) {
            binding.tvMonth.text = item.label
            binding.ivExpand.rotation = if (item.isExpanded) 180f else 0f
            binding.root.setOnClickListener { onMonthClicked(item.year, item.month) }
        }
    }

    inner class BookingViewHolder(private val binding: ItemPastBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PastTimelineItem.BookingEntry) {
            binding.tvFacilityName.text = item.item.facilityDisplayName
            binding.tvDateTime.text = "${item.item.displayDate} · ${item.item.timeLabel}"
            val iconRes = if (item.item.facilityId == "badminton_court_1")
                R.drawable.badminton else R.drawable.cricket
            binding.ivFacility.setImageResource(iconRes)
        }
    }

    inner class ShowMoreViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        init { view.setOnClickListener { onShowMoreClicked() } }
    }

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is PastTimelineItem.YearHeader   -> TYPE_YEAR
        is PastTimelineItem.MonthHeader  -> TYPE_MONTH
        is PastTimelineItem.BookingEntry -> TYPE_BOOKING
        PastTimelineItem.ShowMore        -> TYPE_SHOW_MORE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_YEAR    -> YearViewHolder(ItemPastYearHeaderBinding.inflate(inflater, parent, false))
            TYPE_MONTH   -> MonthViewHolder(ItemPastMonthHeaderBinding.inflate(inflater, parent, false))
            TYPE_BOOKING -> BookingViewHolder(ItemPastBookingBinding.inflate(inflater, parent, false))
            else -> ShowMoreViewHolder(
                inflater.inflate(R.layout.item_show_more, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is PastTimelineItem.YearHeader   -> (holder as YearViewHolder).bind(item)
            is PastTimelineItem.MonthHeader  -> (holder as MonthViewHolder).bind(item)
            is PastTimelineItem.BookingEntry -> (holder as BookingViewHolder).bind(item)
            PastTimelineItem.ShowMore        -> { /* click handled in ViewHolder init */ }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<PastTimelineItem>() {
        override fun areItemsTheSame(a: PastTimelineItem, b: PastTimelineItem): Boolean = when {
            a is PastTimelineItem.YearHeader && b is PastTimelineItem.YearHeader -> a.year == b.year
            a is PastTimelineItem.MonthHeader && b is PastTimelineItem.MonthHeader -> a.year == b.year && a.month == b.month
            a is PastTimelineItem.BookingEntry && b is PastTimelineItem.BookingEntry -> a.item.docId == b.item.docId
            a is PastTimelineItem.ShowMore && b is PastTimelineItem.ShowMore -> true
            else -> false
        }
        override fun areContentsTheSame(a: PastTimelineItem, b: PastTimelineItem) = a == b
    }

    companion object {
        private const val TYPE_YEAR      = 0
        private const val TYPE_MONTH     = 1
        private const val TYPE_BOOKING   = 2
        private const val TYPE_SHOW_MORE = 3
    }
}
