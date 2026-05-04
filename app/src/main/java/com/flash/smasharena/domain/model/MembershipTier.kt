package com.flash.smasharena.domain.model

enum class MembershipTier(
    val displayName: String,
    val priceRupees: Int,
    val sessionsPerMonth: Int,
) {
    NONE("No membership", 0, 0),
    RALLY("Rally", 1_000, 8),
    SMASH("Smash", 1_500, 12),
    ACE("Ace", 2_000, 16),
}
