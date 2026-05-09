package com.flash.smasharena.util

import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.domain.model.SlotStatus

object BookingWindowUtils {

    private fun maxDaysAhead(tier: MembershipTier): Int = tier.bookingDaysAhead

    fun effectiveStatus(
        daysUntilSlot: Int,
        serverStatus: SlotStatus,
        membershipTier: MembershipTier,
        isMyBooking: Boolean,
        slotHour: Int = -1,
        currentHour: Int = -1,
    ): SlotStatus {
        if (isMyBooking) return SlotStatus.MY_BOOKING
        if (daysUntilSlot == 0 && slotHour >= 0 && slotHour <= currentHour) return SlotStatus.LOCKED
        if (serverStatus == SlotStatus.BOOKED) return SlotStatus.BOOKED
        if (daysUntilSlot !in 0..7) return SlotStatus.LOCKED

        val maxDays = maxDaysAhead(membershipTier)
        return if (daysUntilSlot <= maxDays) serverStatus else SlotStatus.MEMBER_HOLD
    }

    /** Whether a given date is selectable based on the user's membership tier and current hour. */
    fun isDateBrowsable(daysUntil: Int, membershipTier: MembershipTier, currentHour: Int = -1): Boolean {
        // After 22:00 all today's slots are past — hide today entirely
        if (daysUntil == 0 && currentHour >= 22) return false
        return daysUntil in 0..maxDaysAhead(membershipTier)
    }
}
