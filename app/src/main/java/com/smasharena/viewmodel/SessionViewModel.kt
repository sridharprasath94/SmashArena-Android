package com.smasharena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smasharena.data.BookingRepository
import com.smasharena.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Process-wide signed-in user. In a real app this would be backed by encrypted
 * preferences / a token; here it lives in memory because the prototype trusts
 * a single device.
 */
class SessionViewModel(private val repo: BookingRepository) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun signIn(name: String, isPremium: Boolean) {
        viewModelScope.launch {
            val id = repo.createUser(name, isPremium)
            _currentUser.value = repo.findUser(id)
        }
    }

    fun signOut() {
        _currentUser.value = null
    }

    class Factory(private val repo: BookingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SessionViewModel(repo) as T
    }
}
