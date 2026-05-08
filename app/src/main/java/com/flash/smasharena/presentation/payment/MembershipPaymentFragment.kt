package com.flash.smasharena.presentation.payment

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.flash.smasharena.R
import com.flash.smasharena.databinding.FragmentPaymentBinding
import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.util.toUserMessage
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MembershipPaymentFragment : Fragment(R.layout.fragment_payment) {

    private val viewModel: MembershipPaymentViewModel by viewModels()
    private val binding by viewBinding(FragmentPaymentBinding::bind)
    private val args by navArgs<MembershipPaymentFragmentArgs>()
    private val tier: MembershipTier by lazy { MembershipTier.valueOf(args.tierName) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        populateOrderSummary()
        setupPaymentMethods()
        setupPayButton()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.title = "${tier.displayName} Plan"
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun populateOrderSummary() {
        binding.tvFacilityName.text = "${tier.displayName} Plan"
        binding.tvDatetime.text = when {
            args.isScheduled -> getString(R.string.membership_schedule_description, tier.sessionsPerMonth)
            args.isUpgrade   -> getString(R.string.membership_upgrade_description, tier.sessionsPerMonth)
            else             -> getString(R.string.membership_purchase_description, tier.sessionsPerMonth)
        }
        binding.tvAmount.text = "₹${"%,d".format(args.amount)}"
        binding.btnPay.text = getString(R.string.payment_btn_pay, args.amount)
    }

    private fun setupPaymentMethods() {
        binding.rgPaymentMethods.setOnCheckedChangeListener { _, checkedId ->
            val method = when (checkedId) {
                R.id.rb_upi         -> PaymentMethod.UPI
                R.id.rb_net_banking -> PaymentMethod.NET_BANKING
                R.id.rb_card        -> PaymentMethod.CARD
                else                -> PaymentMethod.UPI
            }
            viewModel.selectMethod(method)
        }
    }

    private fun setupPayButton() {
        binding.btnPay.setOnClickListener { viewModel.pay() }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        binding.btnPay.isVisible = !state.isProcessing
                        binding.progressPayment.isVisible = state.isProcessing

                        state.error?.let { error ->
                            viewModel.onErrorShown()
                            Snackbar.make(requireView(), error.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        when (event) {
                            PaymentEvent.PaymentSuccess -> {
                                findNavController().getBackStackEntry(R.id.homeFragment)
                                    .savedStateHandle["membership_updated"] = true
                                findNavController().popBackStack(R.id.homeFragment, false)
                            }
                        }
                    }
                }
            }
        }
    }
}
