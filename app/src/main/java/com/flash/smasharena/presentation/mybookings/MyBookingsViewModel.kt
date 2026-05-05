package com.flash.smasharena.presentation.mybookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.repository.SlotRepository
import com.flash.smasharena.util.DateTimeUtils
import com.flash.smasharena.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyBookingsViewModel @Inject constructor(
    private val slotRepository: SlotRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyBookingsUiState())
    val uiState: StateFlow<MyBookingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            slotRepository.getMyBookings().collect { slots ->
                val items = slots.map { slot ->
                    BookingItem(
                        docId = slot.docId,
                        facilityId = slot.facilityId,
                        facilityDisplayName = facilityDisplayName(slot.facilityId),
                        date = slot.date,
                        displayDate = DateTimeUtils.displayDate(slot.date),
                        timeLabel = slot.timeLabel,
                    )
                }
                _uiState.update { it.copy(bookings = items, isLoading = false) }
            }
        }
    }

    fun cancelBooking(docId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(cancellingDocId = docId) }
            slotRepository.cancelBooking(docId)
                .onSuccess { _uiState.update { it.copy(cancellingDocId = null) } }
                .onFailure { e -> _uiState.update { it.copy(cancellingDocId = null, error = e.toAppError()) } }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    private fun facilityDisplayName(facilityId: String): String = when (facilityId) {
        "badminton_court_1" -> "Badminton Court"
        "cricket_net_1" -> "Cricket Net"
        else -> facilityId
    }
}
