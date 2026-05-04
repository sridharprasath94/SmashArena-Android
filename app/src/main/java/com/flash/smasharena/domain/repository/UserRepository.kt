package com.flash.smasharena.domain.repository

import com.flash.smasharena.domain.model.UserProfile

interface UserRepository {
    suspend fun getOrCreateProfile(): UserProfile
}
