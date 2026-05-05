package com.flash.smasharena.presentation.slots

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
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
import com.flash.smasharena.domain.model.SlotStatus
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
        binding.btnBook.setOnClickListener {
            val slot = viewModel.uiState.value.selectedSlot ?: return@setOnClickListener
            if (slot.effectiveStatus == SlotStatus.MY_BOOKING) showCancelConfirmation()
            else showBookingConfirmation()
        }
    }

    private fun showCancelConfirmation() {
        val state = viewModel.uiState.value
        val slot = state.selectedSlot ?: return
        val dateItem = state.dates.find { it.isSelected }
        val dateLabel = dateItem?.let { "${it.dayLabel}, ${it.dayNumber}" } ?: ""

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_cancel_booking_title)
            .setMessage(
                getString(
                    R.string.dialog_cancel_booking_message,
                    state.facilityName,
                    dateLabel,
                    slot.timeLabel,
                )
            )
            .setPositiveButton(R.string.dialog_cancel_booking_confirm) { _, _ -> viewModel.cancelSelectedSlot() }
            .setNegativeButton(R.string.dialog_keep, null)
            .show()
    }

    private fun showBookingConfirmation() {
        val state = viewModel.uiState.value
        val slot = state.selectedSlot ?: return
        val dateItem = state.dates.find { it.isSelected }

        val dateLabel = dateItem?.let { "${it.dayLabel}, ${it.dayNumber}" } ?: ""

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dialog_book_title)
            .setMessage(
                getString(
                    R.string.dialog_book_message,
                    state.facilityName,
                    dateLabel,
                    slot.timeLabel,
                )
            )
            .setPositiveButton(R.string.dialog_book_confirm) { _, _ -> viewModel.bookSelectedSlot() }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.toolbar.title = state.facilityName

                    dateAdapter.submitList(state.dates)
                    slotAdapter.submitList(state.slots)

                    val busy = state.isBooking || state.isCancelling
                    val isCancelSelected = state.selectedSlot?.effectiveStatus == SlotStatus.MY_BOOKING
                    binding.btnBook.isVisible = state.selectedSlot != null && !busy
                    binding.btnBook.text = state.selectedSlot?.let {
                        if (isCancelSelected) "Cancel ${it.timeLabel}" else "Book ${it.timeLabel}"
                    } ?: getString(R.string.booking_confirm)
                    binding.btnBook.backgroundTintList = ColorStateList.valueOf(
                        ContextCompat.getColor(
                            requireContext(),
                            if (isCancelSelected) R.color.cancel_action else R.color.accent_green,
                        )
                    )
                    binding.btnBook.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            if (isCancelSelected) R.color.text_primary else R.color.background,
                        )
                    )
                    binding.progressBooking.isVisible = busy

                    if (state.bookingSuccess) {
                        viewModel.onBookingSuccessShown()
                        Snackbar.make(requireView(), R.string.booking_success, Snackbar.LENGTH_LONG).show()
                    }

                    if (state.cancelSuccess) {
                        viewModel.onCancelSuccessShown()
                        Snackbar.make(requireView(), R.string.booking_cancelled, Snackbar.LENGTH_LONG).show()
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
