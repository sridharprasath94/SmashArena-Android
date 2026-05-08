package com.flash.smasharena.presentation.home

import com.flash.smasharena.domain.model.UserProfile

sealed class HomeEvent {
    data object NavigateToLogin : HomeEvent()
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val userProfile: UserProfile? = null,
    val lastSyncedAt: String = "",
)
