package com.flash.smasharena.presentation.login

import com.flash.smasharena.domain.model.AppError

data class LoginUiState(
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val generalError: AppError? = null,
    val navigateToHome: Boolean = false,
)
