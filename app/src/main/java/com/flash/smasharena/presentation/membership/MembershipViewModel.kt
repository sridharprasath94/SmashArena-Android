package com.flash.smasharena.presentation.membership

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.domain.repository.UserRepository
import com.flash.smasharena.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MembershipViewModel @Inject constructor(
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MembershipUiState())
    val uiState: StateFlow<MembershipUiState> = _uiState.asStateFlow()

    private val _events = Channel<MembershipEvent>(Channel.BUFFERED)
    val events: Flow<MembershipEvent> = _events.receiveAsFlow()

    private var currentTier: MembershipTier = MembershipTier.NONE
    private var isCancelled: Boolean = false
    private var membershipExpiry: Long? = null
    private var scheduledNextTier: MembershipTier? = null
    private var selectedTier: MembershipTier? = null

    init {
        viewModelScope.launch {
            val profile = runCatching { userRepository.getOrCreateProfile() }.getOrNull()
            currentTier = profile?.membershipTier ?: MembershipTier.NONE
            isCancelled = profile?.membershipCancelled == true
            membershipExpiry = profile?.membershipExpiry
            scheduledNextTier = profile?.scheduledNextTier
            _uiState.update {
                it.copy(
                    plans = buildPlans(),
                    currentTier = currentTier,
                    showCancelButton = computeShowCancelButton(),
                    isLoading = false,
                )
            }
        }
    }

    fun onCardTapped(tier: MembershipTier) {
        if (tier == currentTier) return
        if (scheduledNextTier == tier) return
        selectedTier = if (selectedTier == tier) null else tier
        _uiState.update { it.copy(plans = buildPlans()) }
    }

    fun onGetStarted(item: PlanItem) {
        val isDowngrade = item.tier.ordinal < currentTier.ordinal
        val isUpgrade = item.upgradePrice != null
        val action = if (isDowngrade || (isCancelled && !isUpgrade)) {
            MembershipPendingAction.SchedulePlan(item.tier)
        } else {
            val amount = item.upgradePrice ?: item.tier.priceRupees
            MembershipPendingAction.SelectPlan(item, amount, isUpgrade, resumesAutoRenew = isCancelled && isUpgrade)
        }
        _uiState.update { it.copy(pendingAction = action) }
    }

    fun onScheduleNextCycle(tier: MembershipTier) {
        _uiState.update { it.copy(pendingAction = MembershipPendingAction.SchedulePlan(tier)) }
    }

    fun onPendingActionConsumed() = _uiState.update { it.copy(pendingAction = null) }

    fun confirmSelectPlan(item: PlanItem, amount: Int, isUpgrade: Boolean) {
        _events.trySend(MembershipEvent.NavigateToPayment(NavigateToMembershipPayment(item.tier, amount, isUpgrade)))
    }

    fun confirmScheduleNextCycle(tier: MembershipTier) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            runCatching { userRepository.scheduleNextCycleMembership(tier) }
                .onSuccess {
                    scheduledNextTier = tier
                    selectedTier = null
                    _uiState.update {
                        it.copy(
                            plans = buildPlans(),
                            showCancelButton = computeShowCancelButton(),
                            isLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.toAppError()) }
                }
        }
    }

    fun cancelMembership() {
        viewModelScope.launch {
            runCatching { userRepository.cancelMembership() }
                .onSuccess { _events.trySend(MembershipEvent.CancelSuccess) }
                .onFailure { e -> _uiState.update { it.copy(error = e.toAppError()) } }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    private fun computeShowCancelButton() =
        currentTier != MembershipTier.NONE && (!isCancelled || scheduledNextTier != null)

    private fun buildPlans(): List<PlanItem> {
        val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
        val expiryLabel = membershipExpiry?.let { sdf.format(Date(it)) }
        return listOf(MembershipTier.RALLY, MembershipTier.SMASH, MembershipTier.ACE).map { tier ->
            val isCurrentPlan = tier == currentTier
            val upgradePrice = if (tier.ordinal > currentTier.ordinal && currentTier != MembershipTier.NONE) {
                tier.priceRupees - currentTier.priceRupees
            } else null
            val isSelected = tier == selectedTier
            val isScheduledNext = scheduledNextTier == tier && !isCurrentPlan
            val scheduledCta: String? = when {
                !isSelected || isScheduledNext -> null
                isCancelled && (isCurrentPlan || upgradePrice != null) -> null
                tier.ordinal < currentTier.ordinal -> "Downgrade for Next Cycle"
                else -> null
            }
            PlanItem(
                tier = tier,
                price = "₹${"%,d".format(tier.priceRupees)} / month",
                badmintonSessions = tier.sessionsPerMonth,
                cricketSessions = tier.cricketSessionsPerMonth,
                isCurrentPlan = isCurrentPlan,
                isRecommended = tier == MembershipTier.SMASH,
                isSelected = isSelected,
                upgradePrice = upgradePrice,
                isCancelled = isCancelled && isCurrentPlan,
                expiryText = if (isCancelled && isCurrentPlan && expiryLabel != null && scheduledNextTier == null) "Expires $expiryLabel" else null,
                isScheduledNext = isScheduledNext,
                scheduledCta = scheduledCta,
            )
        }
    }
}
