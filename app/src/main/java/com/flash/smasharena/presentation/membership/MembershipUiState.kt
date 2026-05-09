package com.flash.smasharena.presentation.membership

import com.flash.smasharena.domain.model.AppError
import com.flash.smasharena.domain.model.MembershipTier

enum class ScheduledCtaType { DOWNGRADE_NEXT_CYCLE }
enum class NextCycleCtaType { RENEW_MEMBERSHIP, CONTINUE_NEXT_CYCLE }

sealed class MembershipPendingAction {
    data class SelectPlan(
        val item: PlanItem,
        val amount: Int,
        val isUpgrade: Boolean,
        val resumesAutoRenew: Boolean = false,
    ) : MembershipPendingAction()
    data class SchedulePlan(val tier: MembershipTier) : MembershipPendingAction()
}

data class PlanItem(
    val tier: MembershipTier,
    val badmintonSessions: Int,
    val cricketSessions: Int,
    val isCurrentPlan: Boolean,
    val isRecommended: Boolean,
    val isSelected: Boolean,
    val upgradePrice: Int? = null,
    val isCancelled: Boolean = false,
    val expiryDateLabel: String? = null,
    val isScheduledNext: Boolean = false,
    val scheduledCta: ScheduledCtaType? = null,
    val nextCycleCta: NextCycleCtaType? = null,
)

data class NavigateToMembershipPayment(
    val tier: MembershipTier,
    val amount: Int,
    val isUpgrade: Boolean,
    val isScheduled: Boolean = false,
)

sealed class MembershipEvent {
    data class NavigateToPayment(val nav: NavigateToMembershipPayment) : MembershipEvent()
    data object CancelSuccess : MembershipEvent()
}

data class MembershipUiState(
    val plans: List<PlanItem> = emptyList(),
    val currentTier: MembershipTier = MembershipTier.NONE,
    val isLoading: Boolean = true,
    val pendingAction: MembershipPendingAction? = null,
    val showCancelButton: Boolean = false,
    val error: AppError? = null,
)
