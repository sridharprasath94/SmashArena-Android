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
import com.flash.smasharena.domain.model.Facility
import com.flash.smasharena.domain.model.SlotStatus
import com.flash.smasharena.util.toUserMessage
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
        setupConfirmationListeners()
        observePaymentResult()
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
            when {
                slot.effectiveStatus == SlotStatus.MY_BOOKING -> showCancelConfirmation()
                viewModel.wouldExceedConsecutiveLimit() -> showConsecutiveLimitDialog()
                viewModel.isFreeSession() -> showFreeBookingConfirmation()
                else -> navigateToPayment()
            }
        }
    }

    private fun showFreeBookingConfirmation() {
        val state = viewModel.uiState.value
        val slot = state.selectedSlot ?: return
        val dateItem = state.dates.find { it.isSelected }
        val dateLabel = dateItem?.let { "${it.dayLabel}, ${it.dayNumber}" } ?: ""
        ConfirmationDialog.bookFree(state.facilityName, dateLabel, slot.timeLabel)
            .show(childFragmentManager, "confirm_book_free")
    }

    private fun showConsecutiveLimitDialog() {
        ConfirmationDialog.limitReached(getString(R.string.dialog_consecutive_limit_message))
            .show(childFragmentManager, "consecutive_limit")
    }

    private fun setupConfirmationListeners() {
        childFragmentManager.setFragmentResultListener(
            ConfirmationDialog.REQUEST_CANCEL_SLOT, viewLifecycleOwner
        ) { _, _ -> viewModel.cancelSelectedSlot() }
        childFragmentManager.setFragmentResultListener(
            ConfirmationDialog.REQUEST_BOOK_FREE, viewLifecycleOwner
        ) { _, _ -> viewModel.bookSelectedSlotFree() }
    }

    private fun showCancelConfirmation() {
        val state = viewModel.uiState.value
        val slot = state.selectedSlot ?: return
        val dateItem = state.dates.find { it.isSelected }
        val dateLabel = dateItem?.let { "${it.dayLabel}, ${it.dayNumber}" } ?: ""
        ConfirmationDialog.cancelSlot(state.facilityName, dateLabel, slot.timeLabel)
            .show(childFragmentManager, "confirm_cancel_slot")
    }

    private fun navigateToPayment() {
        val state = viewModel.uiState.value
        val slot = state.selectedSlot ?: return
        val dateItem = state.dates.find { it.isSelected } ?: return
        val amount = Facility.entries.find { it.id == viewModel.facilityId }
            ?.priceForHour(slot.hour) ?: 0
        val action = SlotsFragmentDirections.actionSlotsFragmentToPaymentFragment(
            facilityId   = viewModel.facilityId,
            facilityName = state.facilityName,
            date         = dateItem.dateString,
            dateLabel    = "${dateItem.dayLabel}, ${dateItem.dayNumber}",
            hour         = slot.hour,
            timeLabel    = slot.timeLabel,
            amount       = amount,
        )
        findNavController().navigate(action)
    }

    private fun observePaymentResult() {
        findNavController().currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<Bundle>("booking_result")
            ?.observe(viewLifecycleOwner) { bundle ->
                findNavController().currentBackStackEntry
                    ?.savedStateHandle?.remove<Bundle>("booking_result")
                val facilityName = bundle.getString("facilityName") ?: return@observe
                val dateLabel    = bundle.getString("dateLabel")    ?: return@observe
                val timeLabel    = bundle.getString("timeLabel")    ?: return@observe
                BookingResultDialog.newInstance(
                    BookingResultInfo(ResultType.BOOKED, facilityName, dateLabel, timeLabel)
                ).show(childFragmentManager, "booking_result")
            }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.toolbar.title = state.facilityName

                    dateAdapter.submitList(state.dates)
                    slotAdapter.submitList(state.slots)

                    val busy = state.isCancelling || state.isBookingFree
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

                    state.bookingResultInfo?.let { info ->
                        viewModel.onResultShown()
                        BookingResultDialog.newInstance(info)
                            .show(childFragmentManager, "booking_result")
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
