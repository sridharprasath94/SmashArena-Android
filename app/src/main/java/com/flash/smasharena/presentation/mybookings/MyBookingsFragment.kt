package com.flash.smasharena.presentation.mybookings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.flash.smasharena.R
import com.flash.smasharena.databinding.FragmentMyBookingsBinding
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dev.androidbroadcast.vbpd.viewBinding

@AndroidEntryPoint
class MyBookingsFragment : Fragment(R.layout.fragment_my_bookings) {

    private val binding by viewBinding(FragmentMyBookingsBinding::bind)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.viewPager.adapter = MyBookingsPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 2
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_upcoming)
                1 -> getString(R.string.tab_past)
                2 -> getString(R.string.tab_cancelled)
                else -> ""
            }
        }.attach()
    }
}
