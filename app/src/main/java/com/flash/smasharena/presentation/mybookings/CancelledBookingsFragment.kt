package com.flash.smasharena.presentation.mybookings

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.flash.smasharena.R
import com.flash.smasharena.databinding.FragmentCancelledBookingsBinding
import com.flash.smasharena.presentation.slots.ConfirmationDialog
import com.flash.smasharena.util.toUserMessage
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CancelledBookingsFragment : Fragment(R.layout.fragment_cancelled_bookings) {

    private val viewModel: MyBookingsViewModel by viewModels({ requireParentFragment() })
    private val binding by viewBinding(FragmentCancelledBookingsBinding::bind)
    private lateinit var adapter: CancelledBookingsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        setupDialogResults()
        observeUiState()
    }

    private fun setupRecyclerView() {
        adapter = CancelledBookingsAdapter(
            onItemClicked = { item -> viewModel.toggleSelectCancelled(item.docId) },
            onItemLongClicked = { item ->
                if (!adapter.isSelectionMode) {
                    viewModel.toggleSelectCancelled(item.docId)
                }
            },
        )
        binding.rvCancelled.apply {
            this.adapter = this@CancelledBookingsFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.btnCancelSelection.setOnClickListener {
            viewModel.clearCancelledSelection()
        }
        binding.btnTrash.setOnClickListener {
            val selected = viewModel.uiState.value.selectedCancelledIds
            if (selected.size == 1) {
                ConfirmationDialog.deleteCancelled(requireContext(), selected.first())
                    .show(childFragmentManager, "delete_cancelled")
            } else if (selected.size > 1) {
                ConfirmationDialog.deleteMultipleCancelled(requireContext(), selected.size)
                    .show(childFragmentManager, "delete_cancelled_multi")
            }
        }
    }

    private fun setupDialogResults() {
        childFragmentManager.setFragmentResultListener(
            ConfirmationDialog.REQUEST_DELETE_CANCELLED, viewLifecycleOwner
        ) { _, bundle ->
            val docId = bundle.getString(ConfirmationDialog.KEY_DOC_ID, "")
            viewModel.deleteSingleCancelled(docId)
        }
        childFragmentManager.setFragmentResultListener(
            ConfirmationDialog.REQUEST_DELETE_CANCELLED_MULTI, viewLifecycleOwner
        ) { _, _ ->
            viewModel.deleteSelectedCancelled()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val hasItems = state.cancelledItems.isNotEmpty()
                    val isSelectionMode = state.selectedCancelledIds.isNotEmpty()

                    binding.progressBar.isVisible = state.isCancelledLoading
                    binding.rvCancelled.isVisible = !state.isCancelledLoading && hasItems
                    binding.tvEmpty.isVisible = !state.isCancelledLoading && !hasItems

                    binding.layoutSelectionBar.isVisible = isSelectionMode
                    binding.layoutBottomAction.isVisible = isSelectionMode

                    val count = state.selectedCancelledIds.size
                    binding.tvSelectedCount.text = getString(R.string.selected_count, count)

                    if (adapter.isSelectionMode != isSelectionMode) {
                        adapter.isSelectionMode = isSelectionMode
                        adapter.notifyItemRangeChanged(0, adapter.itemCount)
                    }

                    adapter.submitList(state.cancelledItems)

                    state.error?.let { error ->
                        viewModel.onErrorShown()
                        Snackbar.make(requireView(), error.toUserMessage(requireContext()), Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
