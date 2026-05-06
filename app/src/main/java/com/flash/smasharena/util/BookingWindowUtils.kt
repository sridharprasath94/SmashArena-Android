package com.flash.smasharena.util

import com.flash.smasharena.domain.model.SlotStatus

object BookingWindowUtils {

    /**
     * Resolves what a user actually sees for a slot given:
     * - how many days until the slot
     * - the status stored in Firestore
     * - whether the user is a member
     * - whether this slot belongs to the current user
     *
     * Rules:
     *   past (< 0)   → LOCKED
     *   > 7 days     → LOCKED  (too far ahead)
     *   6–7 days     → member-only window; non-members see LOCKED
     *   0–5 days     → open for everyone; use server status as-is
     */
    fun effectiveStatus(
        daysUntilSlot: Int,
        serverStatus: SlotStatus,
        isMember: Boolean,
        isMyBooking: Boolean,
        slotHour: Int = -1,
        currentHour: Int = -1,
    ): SlotStatus {
        if (isMyBooking) return SlotStatus.MY_BOOKING
        // Slot hour has already passed today
        if (daysUntilSlot == 0 && slotHour >= 0 && slotHour <= currentHour) return SlotStatus.LOCKED
        if (serverStatus == SlotStatus.BOOKED) return SlotStatus.BOOKED

        return when {
            daysUntilSlot < 0  -> SlotStatus.LOCKED
            daysUntilSlot > 7  -> SlotStatus.LOCKED
            daysUntilSlot >= 6 -> if (isMember) serverStatus else SlotStatus.LOCKED
            else               -> serverStatus
        }
    }

    /** Whether a given date is browsable (within the 0–7 day window). */
    fun isDateBrowsable(daysUntil: Int, isMember: Boolean): Boolean = when {
        daysUntil < 0  -> false
        daysUntil > 7  -> false
        daysUntil >= 6 -> isMember
        else           -> true
    }
}
