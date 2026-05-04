package com.flash.smasharena.domain.model

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val membershipTier: MembershipTier,
    val membershipExpiry: Long?,
    val sessionsUsed: Int,
    val sessionsQuota: Int,
) {
    val sessionsRemaining: Int get() = (sessionsQuota - sessionsUsed).coerceAtLeast(0)
    val isMember: Boolean get() = membershipTier != MembershipTier.NONE
}
