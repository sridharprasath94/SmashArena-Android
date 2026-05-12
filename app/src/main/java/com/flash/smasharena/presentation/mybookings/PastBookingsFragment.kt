package com.flash.smasharena.presentation.mybookings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.flash.smasharena.R
import com.flash.smasharena.databinding.FragmentPastBookingsBinding
import com.flash.smasharena.util.toUserMessage
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PastBookingsFragment : Fragment(R.layout.fragment_past_bookings) {

    private val viewModel: MyBookingsViewModel by viewModels({ requireParentFragment() })
    private val binding by viewBinding(FragmentPastBookingsBinding::bind)
    private lateinit var adapter: PastBookingsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeUiState()
    }

    private fun setupRecyclerView() {
        adapter = PastBookingsAdapter(
            onYearClicked = { year -> viewModel.toggleYearExpanded(year) },
            onMonthClicked = { year, month -> viewModel.toggleMonthExpanded(year, month) },
            onShowMoreBookingsClicked = { year, month -> viewModel.showMoreBookings(year, month) },
            onShowAllBookingsClicked = { year, month -> viewModel.showAllBookings(year, month) },
            onCollapseBookingsClicked = { year, month -> viewModel.collapseBookings(year, month) },
            onLoadMoreClicked = { viewModel.loadPast(loadMore = true) },
        )
        binding.rvPast.apply {
            this.adapter = this@PastBookingsFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
            (itemAnimator as? DefaultItemAnimator)?.apply {
                supportsChangeAnimations = false
                addDuration = 150
                removeDuration = 120
                moveDuration = 180
            }
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val hasItems = state.pastTimelineItems.isNotEmpty()
                    binding.progressBar.isVisible = state.isPastLoading
                    binding.rvPast.isVisible = !state.isPastLoading && hasItems
                    binding.tvEmpty.isVisible = !state.isPastLoading && !hasItems
                    adapter.submitList(state.pastTimelineItems)

                    state.error?.let { error ->
                        viewModel.onErrorShown()
                        Snackbar.make(requireView(), error.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
