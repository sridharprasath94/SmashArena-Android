package com.flash.smasharena.data.repository

internal object UserFields {
    const val MEMBERSHIP_TIER = "membershipTier"
    const val MEMBERSHIP_EXPIRY = "membershipExpiry"
    const val MEMBERSHIP_CANCELLED = "membershipCancelled"
    const val SCHEDULED_NEXT_TIER = "scheduledNextTier"
    const val BADMINTON_SESSIONS_USED = "badmintonSessionsUsed"
    const val CRICKET_SESSIONS_USED = "cricketSessionsUsed"
    const val SESSIONS_QUOTA = "sessionsQuota"
    const val DISPLAY_NAME = "displayName"
    const val EMAIL = "email"
}

internal object SlotFields {
    const val FACILITY_ID = "facilityId"
    const val DATE = "date"
    const val HOUR = "hour"
    const val STATUS = "status"
    const val BOOKED_BY = "bookedBy"
    const val BOOKED_AT = "bookedAt"
    const val IS_FREE_MEMBERSHIP = "isFreeMembership"
    const val CANCELLED_AT = "cancelledAt"
    const val STATUS_BOOKED = "booked"
    const val STATUS_CANCELLED = "cancelled"
}
