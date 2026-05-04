package com.flash.smasharena.presentation.home

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
import dev.androidbroadcast.vbpd.viewBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val viewModel: HomeViewModel by viewModels()
    private val binding by viewBinding(FragmentHomeBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupFacilityCards()
        observeUiState()
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_logout -> {
                    viewModel.signOut()
                    true
                }
                else -> false
            }
        }
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

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    updateSyncLabel(state.lastSyncedAt)

                    state.userProfile?.let { profile ->
                        binding.membershipBanner.tvMembershipStatus.text = when (profile.membershipTier) {
                            MembershipTier.NONE -> getString(R.string.membership_banner_none)
                            else -> "${profile.membershipTier.displayName} · ${profile.sessionsRemaining} sessions left · View plans →"
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
