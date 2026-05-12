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

    private var isMultiSelectMode = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        setupDialogResults()
        observeUiState()
    }

    private fun setupRecyclerView() {
        adapter = CancelledBookingsAdapter(
            onDeleteClicked = { item ->
                ConfirmationDialog.deleteCancelled(requireContext(), item.docId)
                    .show(childFragmentManager, "delete_cancelled")
            },
            onItemClicked = { item -> viewModel.toggleSelectCancelled(item.docId) },
        )
        binding.rvCancelled.apply {
            this.adapter = this@CancelledBookingsFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.btnSelect.setOnClickListener { enterMultiSelectMode() }
        binding.btnCloseMultiselect.setOnClickListener { exitMultiSelectMode() }
        binding.btnDeleteSelected.setOnClickListener {
            val count = viewModel.uiState.value.selectedCancelledIds.size
            if (count > 0) {
                ConfirmationDialog.deleteMultipleCancelled(requireContext(), count)
                    .show(childFragmentManager, "delete_cancelled_multi")
            }
        }
        binding.cbSelectAll.setOnClickListener {
            if (binding.cbSelectAll.isChecked) viewModel.selectAllCancelled()
            else viewModel.clearCancelledSelection()
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
            exitMultiSelectMode()
        }
    }

    private fun enterMultiSelectMode() {
        isMultiSelectMode = true
        adapter.isMultiSelectMode = true
        adapter.notifyDataSetChanged()
        binding.layoutMultiselectBar.isVisible = true
        binding.layoutNormalBar.isVisible = false
    }

    private fun exitMultiSelectMode() {
        isMultiSelectMode = false
        adapter.isMultiSelectMode = false
        viewModel.clearCancelledSelection()
        adapter.notifyDataSetChanged()
        binding.layoutMultiselectBar.isVisible = false
        binding.layoutNormalBar.isVisible = true
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val hasItems = state.cancelledItems.isNotEmpty()
                    binding.progressBar.isVisible = state.isCancelledLoading
                    binding.rvCancelled.isVisible = !state.isCancelledLoading && hasItems
                    binding.tvEmpty.isVisible = !state.isCancelledLoading && !hasItems

                    if (!isMultiSelectMode) {
                        binding.layoutNormalBar.isVisible = hasItems
                    }

                    binding.tvSelectedCount.text = getString(
                        R.string.selected_count, state.selectedCancelledIds.size
                    )
                    binding.cbSelectAll.isChecked =
                        hasItems && state.selectedCancelledIds.size == state.cancelledItems.size

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
