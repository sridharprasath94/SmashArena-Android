package com.flash.smasharena.presentation.membership

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.flash.smasharena.R
import com.flash.smasharena.databinding.ItemMembershipPlanBinding

class PlanAdapter(
    private val onCardTapped: (PlanItem) -> Unit,
    private val onGetStarted: (PlanItem) -> Unit,
) : ListAdapter<PlanItem, PlanAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(private val binding: ItemMembershipPlanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlanItem) {
            val ctx = binding.root.context

            binding.tvBadgeRecommended.isVisible = item.isRecommended
            binding.tvPlanName.text = item.tier.displayName
            binding.tvPrice.text = item.price
            binding.tvBadmintonSessions.text =
                ctx.getString(R.string.plan_badminton_sessions, item.badmintonSessions)
            binding.tvCricketSessions.text =
                ctx.getString(R.string.plan_cricket_sessions, item.cricketSessions)

            // Card border + premium elevation/scale/background for selected or current plan
            val active = item.isSelected || item.isCurrentPlan
            val (strokeColor, strokeWidth) = if (active) {
                ContextCompat.getColor(ctx, R.color.accent_gold) to 2
            } else {
                ContextCompat.getColor(ctx, R.color.outline) to 1
            }
            binding.root.strokeColor = strokeColor
            binding.root.strokeWidth = strokeWidth

            val dp = ctx.resources.displayMetrics.density
            binding.root.cardElevation = if (active) 8f * dp else 0f
            binding.root.animate().scaleX(if (active) 1.03f else 1f)
                .scaleY(if (active) 1.03f else 1f).setDuration(200).start()
            binding.root.setCardBackgroundColor(
                ContextCompat.getColor(ctx, if (active) R.color.surface_selected else R.color.surface)
            )

            // Whole card is tappable for selection
            binding.root.setOnClickListener { onCardTapped(item) }

            // Button: visible only if this card is selected or it's the current plan
            val showButton = item.isSelected || item.isCurrentPlan
            binding.btnSelect.isVisible = showButton

            when {
                item.isCurrentPlan -> {
                    binding.btnSelect.text = ctx.getString(R.string.plan_current)
                    binding.btnSelect.isEnabled = false
                    binding.btnSelect.alpha = 0.45f
                }
                item.isSelected && item.upgradePrice != null -> {
                    binding.btnSelect.text = ctx.getString(R.string.plan_upgrade_btn, item.upgradePrice)
                    binding.btnSelect.isEnabled = true
                    binding.btnSelect.alpha = 1f
                    binding.btnSelect.setOnClickListener { onGetStarted(item) }
                }
                item.isSelected -> {
                    binding.btnSelect.text = ctx.getString(R.string.plan_get_started)
                    binding.btnSelect.isEnabled = true
                    binding.btnSelect.alpha = 1f
                    binding.btnSelect.setOnClickListener { onGetStarted(item) }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMembershipPlanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    private object DiffCallback : DiffUtil.ItemCallback<PlanItem>() {
        override fun areItemsTheSame(a: PlanItem, b: PlanItem) = a.tier == b.tier
        override fun areContentsTheSame(a: PlanItem, b: PlanItem) = a == b
    }
}
