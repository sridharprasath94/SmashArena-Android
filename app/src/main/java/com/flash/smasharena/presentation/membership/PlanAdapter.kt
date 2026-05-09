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
    private val onScheduleNextCycle: (PlanItem) -> Unit,
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
            binding.tvFeatureEarlyAccess.text =
                ctx.getString(R.string.plan_feature_early_access, item.tier.bookingDaysAhead)

            // Visual hierarchy: selected > current plan > scheduled next > unselected
            val dp = ctx.resources.displayMetrics.density
            when {
                item.isSelected -> {
                    binding.root.strokeColor = ContextCompat.getColor(ctx, R.color.accent_gold)
                    binding.root.strokeWidth = 3
                    binding.root.cardElevation = 16f * dp
                    binding.root.animate().scaleX(1.04f).scaleY(1.04f).setDuration(200).start()
                    binding.root.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.surface_highlighted))
                }
                item.isCurrentPlan || item.isScheduledNext -> {
                    binding.root.strokeColor = ContextCompat.getColor(ctx, R.color.accent_gold)
                    binding.root.strokeWidth = 1
                    binding.root.cardElevation = if (item.isCurrentPlan) 4f * dp else 2f * dp
                    binding.root.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start()
                    binding.root.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.surface_selected))
                }
                else -> {
                    binding.root.strokeColor = ContextCompat.getColor(ctx, R.color.outline)
                    binding.root.strokeWidth = 1
                    binding.root.cardElevation = 0f
                    binding.root.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                    binding.root.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.surface))
                }
            }

            // Card is only tappable when it has an actionable button (not current plan, not already scheduled)
            val isCardTappable = !item.isCurrentPlan && !item.isScheduledNext
            binding.root.isClickable = isCardTappable
            binding.root.isFocusable = isCardTappable
            if (isCardTappable) {
                binding.root.setOnClickListener { onCardTapped(item) }
            } else {
                binding.root.setOnClickListener(null)
            }

            // Primary button visible for current plan, scheduled-next plan, or selected card
            val showButton = item.isSelected || item.isCurrentPlan || item.isScheduledNext
            binding.btnSelect.isVisible = showButton

            // Secondary button: "Upgrade Next Cycle" when selected+upgradeable, or next-cycle management on current plan
            val showUpgradeNextCycle = item.isSelected && item.upgradePrice != null && !item.isScheduledNext
            val showNextCycleManage = item.isCurrentPlan && item.nextCycleCta != null
            binding.btnUpgradeNextCycle.isVisible = showUpgradeNextCycle || showNextCycleManage
            when {
                showNextCycleManage -> {
                    binding.btnUpgradeNextCycle.text = item.nextCycleCta
                    binding.btnUpgradeNextCycle.setOnClickListener { onScheduleNextCycle(item) }
                }
                showUpgradeNextCycle -> {
                    binding.btnUpgradeNextCycle.text = ctx.getString(R.string.plan_upgrade_next_cycle_btn)
                    binding.btnUpgradeNextCycle.setOnClickListener { onScheduleNextCycle(item) }
                }
            }

            when {
                item.isSelected && item.scheduledCta != null -> {
                    binding.btnSelect.text = item.scheduledCta
                    binding.btnSelect.isEnabled = true
                    binding.btnSelect.alpha = 1f
                    binding.btnSelect.setOnClickListener { onGetStarted(item) }
                }
                item.isCurrentPlan && item.isCancelled -> {
                    binding.btnSelect.text = item.expiryText ?: ctx.getString(R.string.plan_current)
                    binding.btnSelect.isEnabled = false
                    binding.btnSelect.alpha = 0.55f
                }
                item.isCurrentPlan -> {
                    binding.btnSelect.text = ctx.getString(R.string.plan_current)
                    binding.btnSelect.isEnabled = false
                    binding.btnSelect.alpha = 0.45f
                }
                item.isScheduledNext -> {
                    binding.btnSelect.text = ctx.getString(R.string.plan_next_cycle)
                    binding.btnSelect.isEnabled = false
                    binding.btnSelect.alpha = 0.65f
                }
                item.isSelected && item.upgradePrice != null -> {
                    binding.btnSelect.text = ctx.getString(R.string.plan_upgrade_btn)
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
