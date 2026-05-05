package com.flash.smasharena.presentation.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.domain.repository.UserRepository
import com.flash.smasharena.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MembershipUiState())
    val uiState: StateFlow<MembershipUiState> = _uiState.asStateFlow()

    private var currentTier: MembershipTier = MembershipTier.NONE
    private var selectedTier: MembershipTier? = null

    init {
        viewModelScope.launch {
            currentTier = runCatching { userRepository.getOrCreateProfile() }
                .getOrNull()?.membershipTier ?: MembershipTier.NONE
            _uiState.update {
                it.copy(
                    plans = buildPlans(),
                    currentTier = currentTier,
                    showCancelButton = currentTier != MembershipTier.NONE,
                    isLoading = false,
                )
            }
        }
    }

    fun onCardTapped(tier: MembershipTier) {
        if (tier == currentTier) return
        if (tier.ordinal < currentTier.ordinal) return
        selectedTier = if (selectedTier == tier) null else tier
        _uiState.update { it.copy(plans = buildPlans()) }
    }

    fun onGetStarted(item: PlanItem) {
        val isUpgrade = item.upgradePrice != null
        val amount = item.upgradePrice ?: item.tier.priceRupees
        _uiState.update {
            it.copy(
                navigateToPayment = NavigateToMembershipPayment(item.tier, amount, isUpgrade)
            )
        }
    }

    fun onNavigatedToPayment() = _uiState.update { it.copy(navigateToPayment = null) }

    fun cancelMembership() {
        viewModelScope.launch {
            runCatching { userRepository.cancelMembership() }
                .onSuccess { _uiState.update { it.copy(cancelSuccess = true) } }
                .onFailure { e -> _uiState.update { it.copy(error = e.toAppError()) } }
        }
    }

    fun onCancelSuccessHandled() = _uiState.update { it.copy(cancelSuccess = false) }
    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    private fun buildPlans(): List<PlanItem> =
        listOf(MembershipTier.RALLY, MembershipTier.SMASH, MembershipTier.ACE).map { tier ->
            val upgradePrice = if (tier.ordinal > currentTier.ordinal && currentTier != MembershipTier.NONE) {
                tier.priceRupees - currentTier.priceRupees
            } else null
            PlanItem(
                tier = tier,
                price = "₹${"%,d".format(tier.priceRupees)} / month",
                badmintonSessions = tier.sessionsPerMonth,
                cricketSessions = tier.cricketSessionsPerMonth,
                isCurrentPlan = tier == currentTier,
                isRecommended = tier == MembershipTier.SMASH,
                isSelected = tier == selectedTier,
                upgradePrice = upgradePrice,
            )
        }
}
