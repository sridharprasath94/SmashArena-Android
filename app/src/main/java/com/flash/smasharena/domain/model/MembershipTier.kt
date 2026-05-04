package com.flash.smasharena.domain.model

enum class MembershipTier(
    val displayName: String,
    val priceRupees: Int,
    val sessionsPerMonth: Int,
    val cricketSessionsPerMonth: Int,
) {
    NONE("No membership", 0, 0, 0),
    RALLY("Rally", 1_000, 8, 2),
    SMASH("Smash", 1_500, 12, 3),
    ACE("Ace", 2_000, 16, 4),
}
