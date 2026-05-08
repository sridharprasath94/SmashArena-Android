package com.flash.smasharena.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.repository.AuthRepository
import com.flash.smasharena.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
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

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            // Run expiry rollover before streaming so the first snapshot reflects the correct tier
            runCatching { userRepository.getOrCreateProfile() }
            userRepository.observeProfile()
                .catch { _uiState.update { it.copy(isLoading = false) } }
                .collect { profile ->
                    _uiState.update {
                        it.copy(isLoading = false, userProfile = profile, lastSyncedAt = formattedNow())
                    }
                }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _events.trySend(HomeEvent.NavigateToLogin)
    }

    private fun formattedNow(): String =
        SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date())
}
