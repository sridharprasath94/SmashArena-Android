package com.flash.smasharena.domain.model

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val membershipTier: MembershipTier,
    val membershipExpiry: Long?,
    val sessionsUsed: Int,
    val sessionsQuota: Int,
    val cricketSessionsUsed: Int = 0,
    val membershipCancelled: Boolean = false,
) {
    val sessionsRemaining: Int get() = (sessionsQuota - sessionsUsed).coerceAtLeast(0)
    val cricketSessionsRemaining: Int
        get() = (membershipTier.cricketSessionsPerMonth - cricketSessionsUsed).coerceAtLeast(0)
    val isMember: Boolean get() = membershipTier != MembershipTier.NONE
}
