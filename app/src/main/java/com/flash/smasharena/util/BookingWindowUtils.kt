package com.flash.smasharena.util

import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.domain.model.SlotStatus

object BookingWindowUtils {

    // Booking windows by tier:
    // ACE → 7 days | SMASH → 6 days | RALLY → 5 days | NONE → 2 days
    private fun maxDaysAhead(tier: MembershipTier): Int = when (tier) {
        MembershipTier.ACE   -> 7
        MembershipTier.SMASH -> 6
        MembershipTier.RALLY -> 5
        MembershipTier.NONE  -> 2
    }

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
        if (daysUntilSlot < 0 || daysUntilSlot > 7) return SlotStatus.LOCKED

        val maxDays = maxDaysAhead(membershipTier)
        return if (daysUntilSlot <= maxDays) serverStatus else SlotStatus.MEMBER_HOLD
    }

    /** Whether a given date is selectable based on the user's membership tier. */
    fun isDateBrowsable(daysUntil: Int, membershipTier: MembershipTier): Boolean =
        daysUntil in 0..maxDaysAhead(membershipTier)
}
