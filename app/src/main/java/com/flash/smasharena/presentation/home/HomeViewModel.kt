package com.flash.smasharena.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.repository.AuthRepository
import com.flash.smasharena.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // One-time fetch to trigger expiry rollover logic before streaming
        viewModelScope.launch { runCatching { userRepository.getOrCreateProfile() } }
        viewModelScope.launch {
            userRepository.observeProfile()
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { profile ->
                    _uiState.update {
                        it.copy(isLoading = false, userProfile = profile, lastSyncedAt = formattedNow())
                    }
                }
        }
    }

    fun refreshProfile() {
        viewModelScope.launch { runCatching { userRepository.getOrCreateProfile() } }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.update { it.copy(navigateToLogin = true) }
    }

    fun onNavigatedToLogin() = _uiState.update { it.copy(navigateToLogin = false) }

    private fun formattedNow(): String =
        SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date())
}
