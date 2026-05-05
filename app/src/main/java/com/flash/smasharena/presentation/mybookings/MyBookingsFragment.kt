package com.flash.smasharena.presentation.mybookings

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
import com.flash.smasharena.databinding.FragmentMyBookingsBinding
import com.flash.smasharena.presentation.slots.ConfirmationDialog
import com.flash.smasharena.util.toUserMessage
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MyBookingsFragment : Fragment(R.layout.fragment_my_bookings) {

    private val viewModel: MyBookingsViewModel by viewModels()
    private val binding by viewBinding(FragmentMyBookingsBinding::bind)

    private lateinit var adapter: BookingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        setupConfirmationListener()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun setupRecyclerView() {
        adapter = BookingAdapter { item -> showCancelConfirmation(item) }
        binding.rvBookings.apply {
            this.adapter = this@MyBookingsFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupConfirmationListener() {
        childFragmentManager.setFragmentResultListener(
            ConfirmationDialog.REQUEST_CANCEL_BOOKING, viewLifecycleOwner
        ) { _, bundle ->
            val docId = bundle.getString(ConfirmationDialog.KEY_DOC_ID, "")
            if (docId.isNotEmpty()) viewModel.cancelBooking(docId)
        }
    }

    private fun showCancelConfirmation(item: BookingItem) {
        ConfirmationDialog.cancelBooking(
            facilityName = item.facilityDisplayName,
            dateLabel = item.displayDate,
            timeLabel = item.timeLabel,
            docId = item.docId,
        ).show(childFragmentManager, "confirm_cancel_booking")
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.rvBookings.isVisible = !state.isLoading && state.bookings.isNotEmpty()
                    binding.tvEmpty.isVisible = !state.isLoading && state.bookings.isEmpty()

                    adapter.cancellingDocId = state.cancellingDocId
                    adapter.submitList(state.bookings)

                    state.error?.let { error ->
                        viewModel.onErrorShown()
                        Snackbar.make(requireView(), error.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
