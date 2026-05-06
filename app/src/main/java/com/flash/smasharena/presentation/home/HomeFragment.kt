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
import com.flash.smasharena.R
import com.flash.smasharena.databinding.FragmentHomeBinding
import com.flash.smasharena.domain.model.Facility
import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.presentation.slots.ConfirmationDialog
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupFacilityCards()
        setupMembershipBanner()
        setupLogoutListener()
        observeMembershipUpdates()
        observeSessionUpdates()
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
        binding.cardBadminton.apply {
            tvFacilityName.setText(Facility.BADMINTON_COURT.nameRes)
            tvFacilityDescription.text = Facility.BADMINTON_COURT.description
            ivFacility.setImageResource(Facility.BADMINTON_COURT.imageRes)
            btnBook.setOnClickListener { navigateToSlots(Facility.BADMINTON_COURT) }
        }

        binding.cardCricket.apply {
            tvFacilityName.setText(Facility.CRICKET_NET.nameRes)
            tvFacilityDescription.text = Facility.CRICKET_NET.description
            ivFacility.setImageResource(Facility.CRICKET_NET.imageRes)
            btnBook.setOnClickListener { navigateToSlots(Facility.CRICKET_NET) }
        }
    }

    private fun setupMembershipBanner() {
        binding.membershipBanner.root.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_membershipFragment)
        }
    }

    private fun observeMembershipUpdates() {
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Boolean>("membership_updated")
            ?.observe(viewLifecycleOwner) { updated ->
                if (updated == true) {
                    findNavController().currentBackStackEntry
                        ?.savedStateHandle?.remove<Boolean>("membership_updated")
                    viewModel.refreshProfile()
                }
            }
    }

    private fun observeSessionUpdates() {
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Boolean>("session_updated")
            ?.observe(viewLifecycleOwner) { updated ->
                if (updated == true) {
                    findNavController().currentBackStackEntry
                        ?.savedStateHandle?.remove<Boolean>("session_updated")
                    viewModel.refreshProfile()
                }
            }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    updateSyncLabel(state.lastSyncedAt)

                    state.userProfile?.let { profile ->
                        val tier = profile.membershipTier
                        val isActiveMember = tier != MembershipTier.NONE
                        binding.membershipBanner.layoutNoMembership.isVisible = !isActiveMember
                        binding.membershipBanner.layoutMembershipDetail.isVisible = isActiveMember

                        if (isActiveMember) {
                            binding.membershipBanner.tvPlanName.text = tier.displayName
                            binding.membershipBanner.tvBadmintonRemaining.text =
                                "🏸 ${profile.sessionsRemaining} / ${tier.sessionsPerMonth}"
                            binding.membershipBanner.tvCricketRemaining.text =
                                "🏏 ${profile.cricketSessionsRemaining} / ${tier.cricketSessionsPerMonth}"
                            val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                            binding.membershipBanner.tvValidity.text = profile.membershipExpiry?.let { expiry ->
                                val startCal = Calendar.getInstance().apply { timeInMillis = expiry }
                                startCal.add(Calendar.DAY_OF_MONTH, -30)
                                "${sdf.format(startCal.time)} → ${sdf.format(Date(expiry))}"
                            } ?: ""
                        }

                        // Facility card session hints
                        binding.cardBadminton.tvSessionsRemaining.isVisible = isActiveMember
                        binding.cardCricket.tvSessionsRemaining.isVisible = isActiveMember
                        if (isActiveMember) {
                            val badmintonLeft = profile.sessionsRemaining
                            binding.cardBadminton.tvSessionsRemaining.text =
                                if (badmintonLeft > 0) "🏸 $badmintonLeft free sessions left"
                                else "Badminton quota used this month"
                            val cricketLeft = profile.cricketSessionsRemaining
                            binding.cardCricket.tvSessionsRemaining.text =
                                if (cricketLeft > 0) "🏏 $cricketLeft free sessions left"
                                else "Cricket quota used this month"
                        }
                    }

                    if (state.navigateToLogin) {
                        viewModel.onNavigatedToLogin()
                        findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
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
