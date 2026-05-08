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
    private var pendingAction: MembershipPendingAction? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupCancelButton()
        setupCancelMembershipListener()
        setupSelectMembershipListener()
        setupScheduleMembershipListener()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun setupRecyclerView() {
        adapter = PlanAdapter(
            onCardTapped = { plan -> viewModel.onCardTapped(plan.tier) },
            onGetStarted = { plan -> viewModel.onGetStarted(plan) },
            onScheduleNextCycle = { plan -> viewModel.onScheduleNextCycle(plan.tier) },
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

    private fun setupSelectMembershipListener() {
        childFragmentManager.setFragmentResultListener(
            ConfirmationDialog.REQUEST_SELECT_MEMBERSHIP, viewLifecycleOwner
        ) { _, _ ->
            val action = pendingAction as? MembershipPendingAction.SelectPlan ?: return@setFragmentResultListener
            pendingAction = null
            viewModel.confirmSelectPlan(action.item, action.amount, action.isUpgrade)
        }
    }

    private fun setupScheduleMembershipListener() {
        childFragmentManager.setFragmentResultListener(
            ConfirmationDialog.REQUEST_SCHEDULE_MEMBERSHIP, viewLifecycleOwner
        ) { _, _ ->
            val action = pendingAction as? MembershipPendingAction.SchedulePlan ?: return@setFragmentResultListener
            pendingAction = null
            viewModel.confirmScheduleNextCycle(action.tier)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.progressBar.isVisible = state.isLoading
                        binding.rvPlans.isVisible = !state.isLoading
                        binding.btnCancelMembership.isVisible = state.showCancelButton && !state.isLoading

                        adapter.submitList(state.plans)

                        state.pendingAction?.let { action ->
                            viewModel.onPendingActionConsumed()
                            pendingAction = action
                            when (action) {
                                is MembershipPendingAction.SelectPlan -> {
                                    val tierName = action.item.tier.displayName
                                    val amountStr = "%,d".format(action.amount)
                                    val message = if (action.isUpgrade) {
                                        "You'll be charged ₹$amountStr to upgrade to $tierName immediately."
                                    } else {
                                        "You'll be charged ₹$amountStr for $tierName membership."
                                    }
                                    ConfirmationDialog.selectMembership(message)
                                        .show(childFragmentManager, "confirm_select_membership")
                                }
                                is MembershipPendingAction.SchedulePlan -> {
                                    ConfirmationDialog.scheduleMembership(action.tier.displayName)
                                        .show(childFragmentManager, "confirm_schedule_membership")
                                }
                            }
                        }

                        state.error?.let { error ->
                            viewModel.onErrorShown()
                            Snackbar.make(requireView(), error.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            is MembershipEvent.NavigateToPayment -> {
                                val nav = event.nav
                                val action = MembershipFragmentDirections
                                    .actionMembershipFragmentToMembershipPaymentFragment(
                                        tierName = nav.tier.name,
                                        amount = nav.amount,
                                        isUpgrade = nav.isUpgrade,
                                        isScheduled = nav.isScheduled,
                                    )
                                findNavController().navigate(action)
                            }
                            MembershipEvent.CancelSuccess -> {
                                findNavController().getBackStackEntry(R.id.homeFragment)
                                    .savedStateHandle["membership_updated"] = true
                                findNavController().popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}
