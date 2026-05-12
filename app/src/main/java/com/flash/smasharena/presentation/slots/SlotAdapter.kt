package com.flash.smasharena.presentation.slots

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flash.smasharena.R
import com.flash.smasharena.databinding.ItemSlotBinding
import com.flash.smasharena.domain.model.SlotStatus

class SlotAdapter(
    private val onSlotClicked: (DisplaySlot) -> Unit,
) : ListAdapter<DisplaySlot, SlotAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemSlotBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DisplaySlot) {
            val ctx = binding.root.context

            binding.tvTime.text = String.format("%02d:00", item.hour)
            binding.tvStatus.text = when {
                item.effectiveStatus == SlotStatus.MY_BOOKING && item.isFreeMembership ->
                    ctx.getString(R.string.slot_membership_booking)
                else -> when (item.effectiveStatus) {
                    SlotStatus.AVAILABLE   -> ctx.getString(R.string.slot_available)
                    SlotStatus.BOOKED      -> ctx.getString(R.string.slot_booked)
                    SlotStatus.MEMBER_HOLD -> ctx.getString(R.string.slot_member_hold)
                    SlotStatus.LOCKED      -> ctx.getString(R.string.slot_locked)
                    SlotStatus.MY_BOOKING  -> ctx.getString(R.string.slot_my_booking)
                    SlotStatus.CANCELLED   -> ctx.getString(R.string.slot_available)
                }
            }

            val bgColor = ContextCompat.getColor(
                ctx, when {
                    item.effectiveStatus == SlotStatus.MY_BOOKING && item.isFreeMembership ->
                        if (item.isSelected) R.color.slot_membership_booking_selected else R.color.slot_membership_booking
                    else -> when (item.effectiveStatus) {
                        SlotStatus.AVAILABLE   -> if (item.isSelected) R.color.slot_selected else R.color.slot_available
                        SlotStatus.BOOKED      -> R.color.slot_booked
                        SlotStatus.MEMBER_HOLD -> R.color.slot_member_hold
                        SlotStatus.LOCKED      -> R.color.slot_locked
                        SlotStatus.MY_BOOKING  -> if (item.isSelected) R.color.slot_my_booking_selected else R.color.slot_my_booking
                        SlotStatus.CANCELLED   -> if (item.isSelected) R.color.slot_selected else R.color.slot_available
                    }
                }
            )
            binding.root.setCardBackgroundColor(bgColor)

            // MEMBER_HOLD slots: gold text + gold border to signal premium-exclusive access
            if (item.effectiveStatus == SlotStatus.MEMBER_HOLD) {
                binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.accent_gold))
                binding.root.strokeWidth = 1
                binding.root.strokeColor = ContextCompat.getColor(ctx, R.color.accent_gold)
            } else {
                binding.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                val strokeWidth = if (item.isSelected) 2 else 0
                binding.root.strokeWidth = strokeWidth
                binding.root.strokeColor = ContextCompat.getColor(ctx, R.color.text_primary)
            }

            val isClickable = item.effectiveStatus == SlotStatus.AVAILABLE || item.effectiveStatus == SlotStatus.MY_BOOKING
            binding.root.isClickable = isClickable
            binding.root.isFocusable = isClickable
            binding.root.setOnClickListener { if (isClickable) onSlotClicked(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSlotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<DisplaySlot>() {
        override fun areItemsTheSame(a: DisplaySlot, b: DisplaySlot) = a.hour == b.hour
        override fun areContentsTheSame(a: DisplaySlot, b: DisplaySlot) = a == b
    }
}
