package com.flash.smasharena.presentation.home

import com.flash.smasharena.domain.model.UserProfile

data class HomeUiState(
    val isLoading: Boolean = true,
    val userProfile: UserProfile? = null,
    val lastSyncedAt: String = "",
    val navigateToLogin: Boolean = false,
)
