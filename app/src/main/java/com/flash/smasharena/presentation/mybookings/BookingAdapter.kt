package com.flash.smasharena.presentation.mybookings

import android.animation.ValueAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flash.smasharena.R
import com.flash.smasharena.domain.model.Facility
import com.flash.smasharena.databinding.ItemBookingBinding

class BookingAdapter(
    private val onCancelClicked: (BookingItem) -> Unit,
) : ListAdapter<BookingItem, BookingAdapter.ViewHolder>(DiffCallback) {

    var cancellingDocId: String? = null

    inner class ViewHolder(private val binding: ItemBookingBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var strokeAnimator: ValueAnimator? = null

        fun bind(item: BookingItem) {
            binding.tvFacilityName.text = item.facilityDisplayName
            binding.tvDateTime.text = "${item.displayDate} · ${item.timeLabel}"

            val iconRes = Facility.entries.find { it.id == item.facilityId }?.imageRes
                ?: R.drawable.badminton
            binding.ivFacility.setImageResource(iconRes)

            val isCancelling = item.docId == cancellingDocId

            when {
                item.isOngoing -> bindOngoing()
                item.isExpired -> bindExpired()
                else -> bindNormal(isCancelling, item)
            }
        }

        private fun bindOngoing() {
            binding.root.alpha = 1f
            binding.tvLiveBadge.isVisible = true
            binding.layoutActions.isVisible = false

            val colorGreen = ContextCompat.getColor(binding.root.context, R.color.accent_green)
            val colorDim = Color.argb(60, Color.red(colorGreen), Color.green(colorGreen), Color.blue(colorGreen))

            strokeAnimator?.cancel()
            strokeAnimator = ValueAnimator.ofArgb(colorGreen, colorDim).apply {
                duration = 1200
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
                addUpdateListener { binding.root.strokeColor = it.animatedValue as Int }
                start()
            }
            binding.root.cardElevation = binding.root.context.resources.getDimension(R.dimen.card_elevation_ongoing)
        }

        private fun bindExpired() {
            cancelAnimation()
            binding.root.alpha = 0.45f
            binding.tvLiveBadge.isVisible = false
            binding.layoutActions.isVisible = false
            resetCardStyle()
        }

        private fun bindNormal(isCancelling: Boolean, item: BookingItem) {
            cancelAnimation()
            binding.root.alpha = 1f
            binding.tvLiveBadge.isVisible = false
            binding.layoutActions.isVisible = true
            binding.btnCancel.isEnabled = !isCancelling
            binding.btnCancel.text = if (isCancelling) "Cancelling…" else "Cancel"
            binding.btnCancel.setOnClickListener { onCancelClicked(item) }
            resetCardStyle()
        }

        private fun cancelAnimation() {
            strokeAnimator?.cancel()
            strokeAnimator = null
        }

        private fun resetCardStyle() {
            val colorOutline = ContextCompat.getColor(binding.root.context, R.color.outline)
            binding.root.strokeColor = colorOutline
            binding.root.cardElevation = 0f
        }

        fun recycle() {
            cancelAnimation()
            resetCardStyle()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    override fun onViewRecycled(holder: ViewHolder) {
        holder.recycle()
    }

    private object DiffCallback : DiffUtil.ItemCallback<BookingItem>() {
        override fun areItemsTheSame(a: BookingItem, b: BookingItem) = a.docId == b.docId
        override fun areContentsTheSame(a: BookingItem, b: BookingItem) = a == b
    }
}
