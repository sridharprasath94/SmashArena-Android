package com.flash.smasharena.presentation.membership

import com.flash.smasharena.domain.model.MembershipTier

data class PlanItem(
    val tier: MembershipTier,
    val price: String,
    val badmintonSessions: Int,
    val cricketSessions: Int,
    val isCurrentPlan: Boolean,
    val isRecommended: Boolean,
    val isSelected: Boolean,
)

data class MembershipUiState(
    val plans: List<PlanItem> = emptyList(),
    val isLoading: Boolean = true,
    val snackbarMessage: String? = null,
)
