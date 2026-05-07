package com.flash.smasharena.presentation.mybookings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.model.Facility
import com.flash.smasharena.domain.repository.SlotRepository
import com.flash.smasharena.domain.repository.UserRepository
import com.flash.smasharena.util.DateTimeUtils
import com.flash.smasharena.util.toAppError
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
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
                val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val items = slots.map { slot ->
                    val daysUntil = DateTimeUtils.daysUntil(slot.date)
                    val isExpired = daysUntil < 0 || (daysUntil == 0 && slot.hour <= currentHour)
                    BookingItem(
                        docId = slot.docId,
                        facilityId = slot.facilityId,
                        facilityDisplayName = facilityDisplayName(slot.facilityId),
                        date = slot.date,
                        displayDate = DateTimeUtils.displayDate(slot.date),
                        hour = slot.hour,
                        timeLabel = slot.timeLabel,
                        isFreeMembership = slot.isFreeMembership,
                        isExpired = isExpired,
                    )
                }
                _uiState.update { it.copy(listItems = buildSectionedList(items), isLoading = false) }
            }
        }
    }

    private fun buildSectionedList(items: List<BookingItem>): List<BookingListItem> {
        val upcoming = items.filter { !it.isExpired }.sortedWith(compareBy({ it.date }, { it.hour }))
        val past = items.filter { it.isExpired }.sortedWith(compareByDescending<BookingItem> { it.date }.thenByDescending { it.hour })
        return buildList {
            if (upcoming.isNotEmpty()) {
                add(BookingListItem.Header("Upcoming"))
                upcoming.forEach { add(BookingListItem.Booking(it)) }
            }
            if (past.isNotEmpty()) {
                add(BookingListItem.Header("Past"))
                past.forEach { add(BookingListItem.Booking(it)) }
            }
        }
    }

    fun cancelBooking(docId: String) {
        val item = _uiState.value.listItems
            .filterIsInstance<BookingListItem.Booking>()
            .map { it.item }
            .find { it.docId == docId }
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
