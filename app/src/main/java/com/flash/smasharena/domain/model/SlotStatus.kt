package com.flash.smasharena.domain.model

enum class SlotStatus {
    AVAILABLE,    // open for booking
    MEMBER_HOLD,  // reserved for members (days 6–7, not yet taken)
    BOOKED,       // taken by someone else
    LOCKED,       // outside booking window or in the past
    MY_BOOKING,   // booked by the current signed-in user
}
