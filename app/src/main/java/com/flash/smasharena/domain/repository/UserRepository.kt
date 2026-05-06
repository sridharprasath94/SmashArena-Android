package com.flash.smasharena.domain.repository

import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.domain.model.UserProfile

interface UserRepository {
    suspend fun getOrCreateProfile(): UserProfile
    suspend fun purchaseMembership(tier: MembershipTier)
    suspend fun upgradeMembership(newTier: MembershipTier)
    suspend fun cancelMembership()
    suspend fun incrementSessionsUsed(isCricket: Boolean = false)
    suspend fun decrementSessionsUsed(isCricket: Boolean = false)
}
