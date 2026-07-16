package com.flash.smasharena.domain.model

enum class MembershipTier(
    val displayName: String,
    val priceRupees: Int,
    val sessionsPerMonth: Int,
    val bookingDaysAhead: Int,
) {
    NONE("No membership", 0, 0, 3),
    RALLY("Rally", 1_000, 8, 5),
    SMASH("Smash", 1_500, 12, 6),
    ACE("Ace", 2_000, 16, 7),
}
