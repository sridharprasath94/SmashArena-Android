package com.flash.smasharena.presentation.membership

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.flash.smasharena.R
import com.flash.smasharena.databinding.FragmentMembershipBinding
import com.flash.smasharena.presentation.slots.ConfirmationDialog
import com.flash.smasharena.util.toUserMessage
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MembershipFragment : Fragment(R.layout.fragment_membership) {

    private val viewModel: MembershipViewModel by viewModels()
    private val binding by viewBinding(FragmentMembershipBinding::bind)

    private lateinit var adapter: PlanAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupCancelButton()
        setupCancelMembershipListener()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun setupRecyclerView() {
        adapter = PlanAdapter(
            onCardTapped = { plan -> viewModel.onCardTapped(plan.tier) },
            onGetStarted = { plan -> viewModel.onGetStarted(plan) },
        )
        binding.rvPlans.apply {
            this.adapter = this@MembershipFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupCancelButton() {
        binding.btnCancelMembership.setOnClickListener {
            val tierName = viewModel.uiState.value.currentTier.displayName
            ConfirmationDialog.cancelMembership(tierName, getString(R.string.dialog_cancel_membership_detail))
                .show(childFragmentManager, "confirm_cancel_membership")
        }
    }

    private fun setupCancelMembershipListener() {
        childFragmentManager.setFragmentResultListener(
            ConfirmationDialog.REQUEST_CANCEL_MEMBERSHIP, viewLifecycleOwner
        ) { _, _ -> viewModel.cancelMembership() }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.rvPlans.isVisible = !state.isLoading
                    binding.btnCancelMembership.isVisible = state.showCancelButton && !state.isLoading

                    adapter.submitList(state.plans)

                    state.navigateToPayment?.let { nav ->
                        viewModel.onNavigatedToPayment()
                        val action = MembershipFragmentDirections
                            .actionMembershipFragmentToMembershipPaymentFragment(
                                tierName = nav.tier.name,
                                amount = nav.amount,
                                isUpgrade = nav.isUpgrade,
                            )
                        findNavController().navigate(action)
                    }

                    if (state.cancelSuccess) {
                        viewModel.onCancelSuccessHandled()
                        findNavController().getBackStackEntry(R.id.homeFragment)
                            .savedStateHandle["membership_updated"] = true
                        findNavController().popBackStack()
                    }

                    state.error?.let { error ->
                        viewModel.onErrorShown()
                        Snackbar.make(requireView(), error.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
