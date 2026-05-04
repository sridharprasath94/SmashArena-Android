package com.flash.smasharena.presentation.slots

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.flash.smasharena.R
import com.flash.smasharena.databinding.FragmentSlotsBinding
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SlotsFragment : Fragment(R.layout.fragment_slots) {

    private val viewModel: SlotsViewModel by viewModels()
    private val binding by viewBinding(FragmentSlotsBinding::bind)

    private lateinit var dateAdapter: DateAdapter
    private lateinit var slotAdapter: SlotAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupDateStrip()
        setupSlotGrid()
        setupBookButton()
        observeUiState()
    }

    private fun setupToolbar() {
        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun setupDateStrip() {
        dateAdapter = DateAdapter { dateItem -> viewModel.selectDate(dateItem.dateString) }
        binding.rvDates.apply {
            adapter = dateAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }
    }

    private fun setupSlotGrid() {
        slotAdapter = SlotAdapter { slot -> viewModel.selectSlot(slot) }
        binding.rvSlots.apply {
            adapter = slotAdapter
            layoutManager = GridLayoutManager(requireContext(), 3)
        }
    }

    private fun setupBookButton() {
        binding.btnBook.setOnClickListener { viewModel.bookSelectedSlot() }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.toolbar.title = state.facilityName

                    dateAdapter.submitList(state.dates)
                    slotAdapter.submitList(state.slots)

                    binding.btnBook.isVisible = state.selectedSlot != null && !state.isBooking
                    binding.btnBook.text = state.selectedSlot?.let {
                        "Book ${it.timeLabel}"
                    } ?: getString(R.string.booking_confirm)
                    binding.progressBooking.isVisible = state.isBooking

                    if (state.bookingSuccess) {
                        viewModel.onBookingSuccessShown()
                        Snackbar.make(requireView(), R.string.booking_success, Snackbar.LENGTH_LONG).show()
                    }

                    state.error?.let {
                        viewModel.onErrorShown()
                        Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
