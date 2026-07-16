package com.flash.smasharena.presentation.home

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.core.content.ContextCompat
import com.flash.smasharena.R
import com.flash.smasharena.databinding.FragmentHomeBinding
import com.flash.smasharena.databinding.ItemFacilityCardBinding
import com.flash.smasharena.domain.model.Facility
import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.presentation.slots.ConfirmationDialog
import com.google.android.material.card.MaterialCardView
import dev.androidbroadcast.vbpd.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()
    private val binding by viewBinding(FragmentHomeBinding::bind)
    private var facilityCards: List<Pair<Facility, ItemFacilityCardBinding>> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupFacilityCards()
        setupMembershipBanner()
        setupLogoutListener()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_my_bookings -> {
                    findNavController().navigate(R.id.action_homeFragment_to_myBookingsFragment)
                    true
                }

                R.id.menu_logout -> {
                    showLogoutConfirmation()
                    true
                }

                else -> false
            }
        }
    }

    private fun setupLogoutListener() {
        childFragmentManager.setFragmentResultListener(
            ConfirmationDialog.REQUEST_LOGOUT, viewLifecycleOwner
        ) { _, _ -> viewModel.signOut() }
    }

    private fun showLogoutConfirmation() {
        ConfirmationDialog.logout(getString(R.string.dialog_logout_message))
            .show(childFragmentManager, "confirm_logout")
    }

    private fun updateSyncLabel(syncedAt: String) {
        binding.toolbar.menu.findItem(R.id.menu_cloud_synced)?.title =
            if (syncedAt.isNotEmpty()) "Cloud Synced · $syncedAt" else getString(R.string.menu_cloud_synced)
    }

    private fun setupFacilityCards() {
        binding.facilityCardsContainer.removeAllViews()
        facilityCards = Facility.entries.map { facility ->
            val cardBinding = ItemFacilityCardBinding.inflate(
                layoutInflater, binding.facilityCardsContainer, true
            )
            cardBinding.apply {
                tvFacilityName.setText(facility.nameRes)
                tvFacilityDescription.text = facility.description
                ivFacility.setImageResource(facility.imageRes)
                btnBook.setOnClickListener { navigateToSlots(facility) }
            }
            facility to cardBinding
        }
    }

    private fun setupMembershipBanner() {
        binding.membershipBanner.root.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_membershipFragment)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.progressBar.isVisible = state.isLoading
                        updateSyncLabel(state.lastSyncedAt)

                        state.userProfile?.let { profile ->
                            val tier = profile.membershipTier
                            val isActiveMember = tier != MembershipTier.NONE
                            binding.membershipBanner.layoutNoMembership.isVisible = !isActiveMember
                            binding.membershipBanner.layoutMembershipDetail.isVisible =
                                isActiveMember

                            binding.membershipBanner.root.let { card ->
                                card.strokeColor = ContextCompat.getColor(
                                    requireContext(),
                                    if (isActiveMember) R.color.accent_gold else R.color.outline,
                                )
                            }

                            if (isActiveMember) {
                                binding.membershipBanner.tvPlanName.text = tier.displayName
                                val scheduled = profile.scheduledNextTier
                                binding.membershipBanner.tvCancelledBadge.isVisible =
                                    profile.membershipCancelled && scheduled == null
                                binding.membershipBanner.tvSessionsRemaining.text =
                                    "🏸 ${profile.sessionsRemaining} / ${tier.sessionsPerMonth}"
                                val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                                binding.membershipBanner.tvValidity.text =
                                    profile.membershipExpiry?.let { expiry ->
                                        if (profile.membershipCancelled && profile.scheduledNextTier == null) {
                                            getString(
                                                R.string.membership_active_until,
                                                sdf.format(Date(expiry))
                                            )
                                        } else {
                                            val startCal = Calendar.getInstance()
                                                .apply { timeInMillis = expiry }
                                            startCal.add(Calendar.DAY_OF_MONTH, -30)
                                            "${sdf.format(startCal.time)} → ${sdf.format(Date(expiry))}"
                                        }
                                    } ?: ""

                                val nextCycleText = when {
                                    scheduled != null && scheduled.ordinal > tier.ordinal ->
                                        getString(
                                            R.string.membership_upgrading_next_cycle,
                                            scheduled.displayName
                                        )

                                    scheduled != null && scheduled.ordinal < tier.ordinal ->
                                        getString(
                                            R.string.membership_downgrading_next_cycle,
                                            scheduled.displayName
                                        )

                                    scheduled != null ->
                                        getString(
                                            R.string.membership_renewing_next_cycle,
                                            scheduled.displayName
                                        )

                                    !profile.membershipCancelled ->
                                        getString(
                                            R.string.membership_renewing_next_cycle,
                                            tier.displayName
                                        )

                                    else -> null
                                }
                                binding.membershipBanner.tvNextCycle.isVisible =
                                    nextCycleText != null
                                binding.membershipBanner.tvNextCycle.text = nextCycleText ?: ""
                            }

                            facilityCards.forEach { (_, cardBinding) ->
                                cardBinding.tvSessionsRemaining.isVisible = isActiveMember
                                if (isActiveMember) {
                                    val remaining = profile.sessionsRemaining
                                    cardBinding.tvSessionsRemaining.text = if (remaining > 0) {
                                        getString(R.string.home_sessions_remaining, remaining)
                                    } else {
                                        getString(R.string.home_quota_used)
                                    }
                                }
                            }
                        }
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            HomeEvent.NavigateToLogin ->
                                findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToSlots(facility: Facility) {
        val action = HomeFragmentDirections.actionHomeFragmentToSlotsFragment(
            facilityId = facility.id,
            facilityName = getString(facility.nameRes),
        )
        findNavController().navigate(action)
    }
}
