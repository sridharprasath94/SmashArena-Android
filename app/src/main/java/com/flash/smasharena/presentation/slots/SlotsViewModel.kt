package com.flash.smasharena.presentation.slots

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flash.smasharena.domain.model.SlotStatus
import com.flash.smasharena.domain.repository.SlotRepository
import com.flash.smasharena.domain.repository.UserRepository
import com.flash.smasharena.domain.model.UserProfile
import com.flash.smasharena.util.BookingWindowUtils
import com.flash.smasharena.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SlotsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val slotRepository: SlotRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val facilityId: String = checkNotNull(savedStateHandle["facilityId"])
    private val facilityName: String = checkNotNull(savedStateHandle["facilityName"])

    private val _uiState = MutableStateFlow(SlotsUiState(facilityName = facilityName))
    val uiState: StateFlow<SlotsUiState> = _uiState.asStateFlow()

    private var userProfile: UserProfile? = null
    private var slotObserverJob: Job? = null

    init {
        viewModelScope.launch {
            userProfile = runCatching { userRepository.getOrCreateProfile() }.getOrNull()
            buildDateStrip()
            selectDate(DateTimeUtils.today())
        }
    }

    private fun buildDateStrip() {
        val isMember = userProfile?.isMember == true
        val dates = (0..13).map { offset ->
            val dateString = DateTimeUtils.dateWithOffset(offset)
            val daysUntil = DateTimeUtils.daysUntil(dateString)
            DateItem(
                dateString = dateString,
                dayLabel = DateTimeUtils.dayLabel(dateString),
                dayNumber = DateTimeUtils.dayNumber(dateString),
                isSelected = false,
                isBrowsable = BookingWindowUtils.isDateBrowsable(daysUntil, isMember),
            )
        }
        _uiState.update { it.copy(dates = dates) }
    }

    fun selectDate(dateString: String) {
        _uiState.update { state ->
            state.copy(
                dates = state.dates.map { it.copy(isSelected = it.dateString == dateString) },
                selectedSlot = null,
                slots = emptyList(),
            )
        }
        observeSlots(dateString)
    }

    private fun observeSlots(date: String) {
        slotObserverJob?.cancel()
        slotObserverJob = viewModelScope.launch {
            val isMember = userProfile?.isMember == true
            val currentUid = userProfile?.uid
            val daysUntil = DateTimeUtils.daysUntil(date)

            slotRepository.observeSlots(facilityId, date).collect { slots ->
                val selectedHour = _uiState.value.selectedSlot?.hour
                val displaySlots = slots.map { slot ->
                    val isMyBooking = slot.bookedBy != null && slot.bookedBy == currentUid
                    val effective = BookingWindowUtils.effectiveStatus(
                        daysUntilSlot = daysUntil,
                        serverStatus = slot.status,
                        isMember = isMember,
                        isMyBooking = isMyBooking,
                    )
                    DisplaySlot(
                        hour = slot.hour,
                        timeLabel = slot.timeLabel,
                        effectiveStatus = effective,
                        isSelected = slot.hour == selectedHour,
                    )
                }
                _uiState.update { it.copy(slots = displaySlots) }
            }
        }
    }

    fun selectSlot(slot: DisplaySlot) {
        if (slot.effectiveStatus != SlotStatus.AVAILABLE) return
        val alreadySelected = _uiState.value.selectedSlot?.hour == slot.hour
        val newSelected = if (alreadySelected) null else slot
        _uiState.update { state ->
            state.copy(
                slots = state.slots.map { it.copy(isSelected = it.hour == newSelected?.hour) },
                selectedSlot = newSelected,
            )
        }
    }

    fun bookSelectedSlot() {
        val slot = _uiState.value.selectedSlot ?: return
        val date = _uiState.value.dates.find { it.isSelected }?.dateString ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBooking = true, error = null) }
            slotRepository.bookSlot(facilityId, date, slot.hour)
                .onSuccess {
                    _uiState.update { it.copy(isBooking = false, bookingSuccess = true, selectedSlot = null) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isBooking = false, error = e.message) }
                }
        }
    }

    fun onErrorShown() = _uiState.update { it.copy(error = null) }
    fun onBookingSuccessShown() = _uiState.update { it.copy(bookingSuccess = false) }
}
