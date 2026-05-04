package com.flash.smasharena.presentation.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.domain.repository.UserRepository
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
            _uiState.update { it.copy(plans = buildPlans(), isLoading = false) }
        }
    }

    fun onCardTapped(tier: MembershipTier) {
        if (tier == currentTier) return
        selectedTier = if (selectedTier == tier) null else tier
        _uiState.update { it.copy(plans = buildPlans()) }
    }

    fun onGetStarted() {
        _uiState.update { it.copy(snackbarMessage = "Online payments coming soon!") }
    }

    fun onSnackbarShown() = _uiState.update { it.copy(snackbarMessage = null) }

    private fun buildPlans(): List<PlanItem> =
        listOf(MembershipTier.RALLY, MembershipTier.SMASH, MembershipTier.ACE).map { tier ->
            PlanItem(
                tier = tier,
                price = "₹${"%,d".format(tier.priceRupees)} / month",
                badmintonSessions = tier.sessionsPerMonth,
                cricketSessions = tier.cricketSessionsPerMonth,
                isCurrentPlan = tier == currentTier,
                isRecommended = tier == MembershipTier.SMASH,
                isSelected = tier == selectedTier,
            )
        }
}
