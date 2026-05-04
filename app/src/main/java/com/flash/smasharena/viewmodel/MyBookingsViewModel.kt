package com.smasharena.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smasharena.data.Booking
import com.smasharena.data.BookingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyBookingsViewModel(private val repo: BookingRepository) : ViewModel() {

    private val userId = MutableStateFlow<Long?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val bookings: StateFlow<List<Booking>> = userId
        .flatMapLatest { id: Long? ->
            if (id == null) flowOf(emptyList()) else repo.observeUpcomingFor(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setUser(id: Long) { userId.value = id }

    fun cancel(booking: Booking) {
        viewModelScope.launch { repo.cancel(booking) }
    }

    class Factory(private val repo: BookingRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            MyBookingsViewModel(repo) as T
    }
}
