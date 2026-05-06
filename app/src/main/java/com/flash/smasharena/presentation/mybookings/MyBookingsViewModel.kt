package com.flash.smasharena.presentation.mybookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.model.Facility
import com.flash.smasharena.domain.repository.SlotRepository
import com.flash.smasharena.domain.repository.UserRepository
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
    private val userRepository: UserRepository,
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
                        isFreeMembership = slot.isFreeMembership,
                    )
                }
                _uiState.update { it.copy(bookings = items, isLoading = false) }
            }
        }
    }

    fun cancelBooking(docId: String) {
        val item = _uiState.value.bookings.find { it.docId == docId }
        viewModelScope.launch {
            _uiState.update { it.copy(cancellingDocId = docId) }
            slotRepository.cancelBooking(docId)
                .onSuccess {
                    if (item?.isFreeMembership == true) {
                        runCatching {
                            userRepository.decrementSessionsUsed(
                                isCricket = item.facilityId == Facility.CRICKET_NET.id
                            )
                        }
                    }
                    _uiState.update { it.copy(cancellingDocId = null, cancelSuccess = true) }
                }
                .onFailure { e -> _uiState.update { it.copy(cancellingDocId = null, error = e.toAppError()) } }
        }
    }

    fun onCancelSuccessHandled() = _uiState.update { it.copy(cancelSuccess = false) }
    fun onErrorShown() = _uiState.update { it.copy(error = null) }

    private fun facilityDisplayName(facilityId: String): String = when (facilityId) {
        "badminton_court_1" -> "Badminton Court"
        "cricket_net_1" -> "Cricket Net"
        else -> facilityId
    }
}
