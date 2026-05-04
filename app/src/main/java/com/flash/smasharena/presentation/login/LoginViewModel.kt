package com.flash.smasharena.presentation.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val isAlreadySignedIn: Boolean get() = authRepository.isSignedIn

    fun signIn(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            authRepository.signInWithEmail(email, password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, navigateToHome = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, generalError = friendlyMessage(e)) } }
        }
    }

    fun createAccount(email: String, password: String) {
        if (!validate(email, password)) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            authRepository.createAccountWithEmail(email, password)
                .onSuccess { _uiState.update { it.copy(isLoading = false, navigateToHome = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, generalError = friendlyMessage(e)) } }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            authRepository.signInWithGoogle(idToken)
                .onSuccess { _uiState.update { it.copy(isLoading = false, navigateToHome = true) } }
                .onFailure { e -> _uiState.update { it.copy(isLoading = false, generalError = friendlyMessage(e)) } }
        }
    }

    fun onNavigatedToHome() = _uiState.update { it.copy(navigateToHome = false) }

    private fun validate(email: String, password: String): Boolean {
        val emailError = when {
            email.isBlank() -> "Email is required"
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> "Enter a valid email"
            else -> null
        }
        val passwordError = when {
            password.isBlank() -> "Password is required"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
        _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
        return emailError == null && passwordError == null
    }

    private fun friendlyMessage(e: Throwable): String = when {
        e.message?.contains("no user record") == true -> "No account found with this email."
        e.message?.contains("password is invalid") == true -> "Incorrect password."
        e.message?.contains("email address is already in use") == true -> "An account already exists with this email."
        e.message?.contains("network") == true -> "Check your internet connection and try again."
        else -> "Something went wrong. Please try again."
    }
}
