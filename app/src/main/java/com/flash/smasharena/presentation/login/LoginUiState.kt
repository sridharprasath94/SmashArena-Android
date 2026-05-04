package com.flash.smasharena.presentation.login

data class LoginUiState(
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: String? = null,
    val navigateToHome: Boolean = false,
)
