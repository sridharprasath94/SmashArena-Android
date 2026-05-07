package com.flash.smasharena.domain.repository

import com.flash.smasharena.domain.model.MembershipTier
import com.flash.smasharena.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getOrCreateProfile(): UserProfile
    fun observeProfile(): Flow<UserProfile>
    suspend fun purchaseMembership(tier: MembershipTier)
    suspend fun upgradeMembership(newTier: MembershipTier)
    suspend fun cancelMembership()
    suspend fun scheduleNextCycleMembership(tier: MembershipTier)
    suspend fun incrementSessionsUsed(isCricket: Boolean = false)
    suspend fun decrementSessionsUsed(isCricket: Boolean = false)
}
