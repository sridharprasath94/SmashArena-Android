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
import com.flash.smasharena.databinding.FragmentUpcomingBookingsBinding
import com.flash.smasharena.presentation.slots.ConfirmationDialog
import com.flash.smasharena.util.toUserMessage
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class UpcomingBookingsFragment : Fragment(R.layout.fragment_upcoming_bookings) {

    private val viewModel: MyBookingsViewModel by viewModels({ requireParentFragment() })
    private val binding by viewBinding(FragmentUpcomingBookingsBinding::bind)
    private lateinit var adapter: BookingAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupConfirmationListener()
        observeUiState()
    }

    private fun setupRecyclerView() {
        adapter = BookingAdapter { item -> showCancelConfirmation(item) }
        binding.rvUpcoming.apply {
            this.adapter = this@UpcomingBookingsFragment.adapter
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
                    val hasItems = state.upcomingItems.isNotEmpty()
                    binding.progressBar.isVisible = state.isUpcomingLoading
                    binding.rvUpcoming.isVisible = !state.isUpcomingLoading && hasItems
                    binding.tvEmpty.isVisible = !state.isUpcomingLoading && !hasItems

                    adapter.cancellingDocId = state.cancellingDocId
                    adapter.submitList(state.upcomingItems)

                    if (state.cancelSuccess) {
                        viewModel.onCancelSuccessHandled()
                        runCatching {
                            findNavController().getBackStackEntry(R.id.homeFragment)
                                .savedStateHandle["session_updated"] = true
                        }
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
