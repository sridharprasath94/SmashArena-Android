package com.flash.smasharena.presentation.membership

import com.flash.smasharena.domain.model.AppError
import com.flash.smasharena.domain.model.MembershipTier

data class PlanItem(
    val tier: MembershipTier,
    val price: String,
    val badmintonSessions: Int,
    val cricketSessions: Int,
    val isCurrentPlan: Boolean,
    val isRecommended: Boolean,
    val isSelected: Boolean,
    val upgradePrice: Int? = null,
    val isCancelled: Boolean = false,
    val expiryText: String? = null,
    val isScheduledNext: Boolean = false,
    val scheduledCta: String? = null,
)

data class NavigateToMembershipPayment(
    val tier: MembershipTier,
    val amount: Int,
    val isUpgrade: Boolean,
    val isScheduled: Boolean = false,
)

data class MembershipUiState(
    val plans: List<PlanItem> = emptyList(),
    val currentTier: MembershipTier = MembershipTier.NONE,
    val isLoading: Boolean = true,
    val navigateToPayment: NavigateToMembershipPayment? = null,
    val showCancelButton: Boolean = false,
    val cancelSuccess: Boolean = false,
    val error: AppError? = null,
)
