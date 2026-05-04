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
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MembershipFragment : Fragment(R.layout.fragment_membership) {

    private val viewModel: MembershipViewModel by viewModels()
    private val binding by viewBinding(FragmentMembershipBinding::bind)

    private lateinit var adapter: PlanAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupToolbar()
        setupRecyclerView()
        observeUiState()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
    }

    private fun setupRecyclerView() {
        adapter = PlanAdapter(
            onCardTapped = { plan -> viewModel.onCardTapped(plan.tier) },
            onGetStarted = { viewModel.onGetStarted() },
        )
        binding.rvPlans.apply {
            this.adapter = this@MembershipFragment.adapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.isVisible = state.isLoading
                    binding.rvPlans.isVisible = !state.isLoading

                    adapter.submitList(state.plans)

                    state.snackbarMessage?.let {
                        viewModel.onSnackbarShown()
                        Snackbar.make(requireView(), it, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
