package com.flash.smasharena.presentation.mybookings

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MyBookingsPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
    override fun getItemCount() = 3
    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> UpcomingBookingsFragment()
        1 -> PastBookingsFragment()
        2 -> CancelledBookingsFragment()
        else -> throw IllegalArgumentException("Unknown tab position: $position")
    }
}
